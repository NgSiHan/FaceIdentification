package com.ml.shubham0204.facenet_android.data.remote

import com.ml.shubham0204.facenet_android.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import org.koin.core.annotation.Single

@Single
class SupabaseProvider {
    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
    ) {
        install(Postgrest)
    }
}
