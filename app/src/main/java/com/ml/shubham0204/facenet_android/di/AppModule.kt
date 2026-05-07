package com.ml.shubham0204.facenet_android.di

import android.content.Context
import com.ml.shubham0204.facenet_android.data.remote.SupabaseProvider
import com.ml.shubham0204.facenet_android.domain.face_detection.BaseFaceDetector
import com.ml.shubham0204.facenet_android.domain.face_detection.MLKitFaceDetector
import com.ml.shubham0204.facenet_android.domain.face_detection.MediapipeFaceDetector
import io.github.jan.supabase.SupabaseClient
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@ComponentScan("com.ml.shubham0204.facenet_android")
class AppModule {

    private var isMLKit = false

    @Single
    fun provideFaceDetector(context: Context): BaseFaceDetector = if (isMLKit) {
        MLKitFaceDetector(context)
    } else {
        MediapipeFaceDetector(context)
    }

    @Single
    fun provideSupabaseClient(supabaseProvider: SupabaseProvider): SupabaseClient =
        supabaseProvider.client
}
