package com.example.forgeplan.core.network

import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface SupabaseStorageService {

    @POST("object/{bucket}/{filePath}")
    fun uploadFile(
        @Path("bucket") bucket: String,
        @Path("filePath", encoded = true) filePath: String,
        @Header("Content-Type") contentType: String,
        @Body file: RequestBody
    ): Call<Unit>
}