package com.ml.shubham0204.facenet_android.data

import android.util.Log
import com.ml.shubham0204.facenet_android.data.remote.RemoteFaceDataSource
import org.koin.core.annotation.Single

@Single
class FaceRepository(
    private val personDB: PersonDB,
    private val imagesVectorDB: ImagesVectorDB,
    private val remoteDataSource: RemoteFaceDataSource,
) {
    /**
     * Remote-first enrolment.
     * Writes to Supabase first to obtain UUIDs, then writes to ObjectBox with those UUIDs.
     * Returns Result.failure if the remote write fails. No ObjectBox write occurs in that case.
     */
    suspend fun enrol(name: String, embedding: FloatArray): Result<Unit> = try {
        val existingPerson = personDB.getByName(name)
        val remotePersonId = existingPerson?.remotePersonId
        if (remotePersonId != null) {
            val embeddingId = remoteDataSource.addEmbedding(remotePersonId, embedding)
            imagesVectorDB.addFaceImageRecord(
                FaceImageRecord(
                    personID = existingPerson.personID,
                    personName = name,
                    faceEmbedding = embedding,
                    remoteId = embeddingId,
                ),
            )
        } else {
            val enrolResult = remoteDataSource.enrollPerson(name, embedding)
            val personID = personDB.addPerson(
                PersonRecord(
                    personName = name,
                    numImages = 1L,
                    addTime = enrolResult.personCreatedAt,
                    remotePersonId = enrolResult.personId,
                ),
            )
            imagesVectorDB.addFaceImageRecord(
                FaceImageRecord(
                    personID = personID,
                    personName = name,
                    faceEmbedding = embedding,
                    remoteId = enrolResult.embeddingId,
                ),
            )
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Pure pass-through to the local ObjectBox HNSW nearest-neighbour search.
     * No network call, identification is always local.
     */
    fun identify(embedding: FloatArray, flatSearch: Boolean): FaceImageRecord? =
        imagesVectorDB.getNearestEmbeddingPersonName(embedding, flatSearch)

    /**
     * Remote-first deletion.
     * Deletes from Supabase first; only removes from ObjectBox if remote succeeds (or person
     * has no remotePersonId, meaning it was enrolled locally without Supabase).
     * Returns Result.failure if the remote delete fails. ObjectBox is NOT touched in that case.
     */
    suspend fun remove(personID: Long): Result<Unit> = try {
        val person = personDB.getById(personID)
        val remotePersonId = person?.remotePersonId
        if (remotePersonId != null) {
            val embeddingIds = imagesVectorDB.getRemoteIdsByPersonId(personID)
            remoteDataSource.deletePerson(remotePersonId, embeddingIds)
        }
        personDB.removePerson(personID)
        imagesVectorDB.removeFaceRecordsWithPersonID(personID)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Fetches all embeddings from Supabase and inserts locally any whose remoteId is not already
     * present in ObjectBox (deduplicated by remoteId).
     * Errors are logged and swallowed so the app continues to work offline.
     */
    suspend fun syncFromRemote() {
        try {
            val remote = remoteDataSource.fetchAllEmbeddings()
            val existing = imagesVectorDB.getAllRemoteIds()
            for (record in remote) {
                if (record.id in existing) continue
                val personID = personDB.getByName(record.personName)?.personID
                    ?: personDB.addPerson(
                        PersonRecord(
                            personName = record.personName,
                            numImages = 1L,
                            addTime = record.personCreatedAt,
                            remotePersonId = record.personId,
                        ),
                    )
                imagesVectorDB.addFaceImageRecord(
                    FaceImageRecord(
                        personID = personID,
                        personName = record.personName,
                        faceEmbedding = record.toFloatArray(),
                        remoteId = record.id,
                    ),
                )
            }
        } catch (e: Exception) {
            Log.e("FaceRepository", "syncFromRemote failed: ${e.message}")
        }
    }
}
