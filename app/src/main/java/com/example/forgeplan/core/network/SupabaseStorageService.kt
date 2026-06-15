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

    //Header fixo enviado em todos os uploads.
    // cria o ficheiro se não existir
    // substitui o ficheiro se já existir
    @Headers("x-upsert: true")
    // pedido HTTP PUT para o endpoint do Storage.
    @PUT("object/{bucket}/{filePath}")
    fun uploadFile(
        // nome do bucket do Supabase
        @Path("bucket") bucket: String,
        // caminho completo do ficheiro dentro do bucket
        @Path("filePath", encoded = true) filePath: String,
        // Tipo MIME do ficheiro (jpeg, png, pdf, etc.)
        @Header("Content-Type") contentType: String,
        //Contém os bytes da imagem, PDF, etc.
        @Body file: RequestBody
    ): Call<ResponseBody> // resposta devolvida pelo servidor
}