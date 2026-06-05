package com.example.forgeplan.core.network

import com.example.forgeplan.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object SupabaseApi {

    private const val PROJECT_URL = BuildConfig.SUPABASE_URL
    private const val SUPABASE_KEY = BuildConfig.SUPABASE_ANON_KEY
    private const val REST_BASE_URL = "$PROJECT_URL/rest/v1/"
    private const val STORAGE_BASE_URL = "$PROJECT_URL/storage/v1/"


    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .build()

            chain.proceed(request)
        }
        .build()

    val service: SupabaseService by lazy {
        Retrofit.Builder()
            .baseUrl(REST_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SupabaseService::class.java)
    }

    val storageService: SupabaseStorageService by lazy {
        Retrofit.Builder()
            .baseUrl(STORAGE_BASE_URL)
            .client(client)
            .build()
            .create(SupabaseStorageService::class.java)
    }

    fun publicStorageUrl(
        bucket: String,
        filePath: String
    ): String {
        return "$STORAGE_BASE_URL/object/public/$bucket/$filePath"
    }
}