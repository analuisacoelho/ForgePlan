package com.example.forgeplan.core.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object SupabaseApi {

    private const val BASE_URL = "https://ywoqflowqzejqijhmlfv.supabase.co/rest/v1/"

    private const val SUPABASE_KEY =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inl3b3FmbG93cXplanFpamhtbGZ2Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzgzMjAxMDEsImV4cCI6MjA5Mzg5NjEwMX0.C8bnIBbSUWZDsFEvBCeZmDRuIQlZDUDaZP_MErunWNA"

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .addHeader("Content-Type", "application/json")
                .build()

            chain.proceed(request)
        }
        .build()

    val service: SupabaseService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SupabaseService::class.java)
    }
}