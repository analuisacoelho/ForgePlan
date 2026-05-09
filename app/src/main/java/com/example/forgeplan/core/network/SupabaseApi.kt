package com.example.forgeplan.core.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object SupabaseApi {

    private const val BASE_URL = "https://ywoqflowqzejqijhmlfv.supabase.co/rest/v1/"

    val service: SupabaseService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SupabaseService::class.java)
    }
}