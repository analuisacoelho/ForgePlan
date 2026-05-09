package com.example.forgeplan.core.network

import com.example.forgeplan.core.model.User
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Headers

interface SupabaseService {

    @Headers(
        "apikey: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inl3b3FmbG93cXplanFpamhtbGZ2Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzgzMjAxMDEsImV4cCI6MjA5Mzg5NjEwMX0.C8bnIBbSUWZDsFEvBCeZmDRuIQlZDUDaZP_MErunWNA",
        "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inl3b3FmbG93cXplanFpamhtbGZ2Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzgzMjAxMDEsImV4cCI6MjA5Mzg5NjEwMX0.C8bnIBbSUWZDsFEvBCeZmDRuIQlZDUDaZP_MErunWNA"
    )
    @GET("users")
    fun getUsers(): Call<List<User>>
}