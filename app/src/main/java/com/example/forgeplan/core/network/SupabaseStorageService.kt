package com.example.forgeplan.core.network

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.PUT
import retrofit2.http.Path

interface SupabaseStorageService {

    @Headers("x-upsert: true")
    @PUT("object/{bucket}/{filePath}")
    fun uploadFile(
        @Path("bucket") bucket: String,
        @Path("filePath", encoded = true) filePath: String,
        @Header("Content-Type") contentType: String,
        @Body file: RequestBody
    ): Call<ResponseBody>
}