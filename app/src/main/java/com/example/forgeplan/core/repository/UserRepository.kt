package com.example.forgeplan.core.repository

import com.example.forgeplan.core.network.SupabaseApi

class UserRepository {

    fun getUsers() = SupabaseApi.service.getUsers()
}