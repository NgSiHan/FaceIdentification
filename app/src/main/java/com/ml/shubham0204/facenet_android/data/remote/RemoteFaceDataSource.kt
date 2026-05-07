package com.ml.shubham0204.facenet_android.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koin.core.annotation.Single

@Serializable
private data class PersonInsert(val name: String)

@Serializable
private data class PersonResponse(val id: String)

@Serializable
private data class EmbeddingInsert(
    @SerialName("person_id") val personId: String,
    // pgvector expects a string in the form "[f1,f2,...]"
    val embedding: String,
)

@Serializable
private data class EmbeddingResponse(val id: String)

@Serializable
data class RemoteEmbeddingRecord(
    val id: String,
    @SerialName("person_id") val personId: String,
    // pgvector is returned by Supabase REST as a string "[f1,f2,...]"
    val embedding: String,
    val persons: PersonName,
) {
    @Serializable
    data class PersonName(val name: String)

    val personName: String get() = persons.name

    fun toFloatArray(): FloatArray =
        embedding
            .trim('[', ']')
            .split(',')
            .map { it.trim().toFloat() }
            .toFloatArray()
}

data class EnrolResult(val personId: String, val embeddingId: String)

@Single
class RemoteFaceDataSource(private val supabaseClient: SupabaseClient) {

    /**
     * Inserts a person row, then a face_embedding row.
     * Returns EnrolResult with both the persons UUID and face_embeddings UUID.
     * Throws on any network or server error — caller should wrap in try/catch.
     */
    suspend fun enrollPerson(name: String, embedding: FloatArray): EnrolResult {
        val person = supabaseClient
            .from("persons")
            .insert(PersonInsert(name)) { select() }
            .decodeSingle<PersonResponse>()

        val embeddingStr = embedding.joinToString(separator = ",", prefix = "[", postfix = "]")

        val record = supabaseClient
            .from("face_embeddings")
            .insert(EmbeddingInsert(personId = person.id, embedding = embeddingStr)) { select() }
            .decodeSingle<EmbeddingResponse>()

        return EnrolResult(personId = person.id, embeddingId = record.id)
    }

    /**
     * Deletes all face_embedding rows by their UUIDs, then deletes the person row.
     * Embeddings are deleted first to avoid FK constraint violations.
     * Throws on any network or server error — caller should wrap in try/catch.
     */
    suspend fun deletePerson(remotePersonId: String, embeddingIds: List<String>) {
        for (id in embeddingIds) {
            supabaseClient
                .from("face_embeddings")
                .delete { filter { eq("id", id) } }
        }
        supabaseClient
            .from("persons")
            .delete { filter { eq("id", remotePersonId) } }
    }

    /**
     * Fetches all face_embeddings joined with the persons table for the person name.
     * Uses PostgREST foreign key embedding syntax: persons(name).
     */
    suspend fun fetchAllEmbeddings(): List<RemoteEmbeddingRecord> =
        supabaseClient
            .from("face_embeddings")
            .select(Columns.raw("id, person_id, embedding, persons(name)"))
            .decodeList()
}
