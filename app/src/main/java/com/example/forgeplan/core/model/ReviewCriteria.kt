package com.example.forgeplan.core.model

data class ReviewCriteria(
    val id: Long,
    val name: String?,
    val description: String?,
    val max_score: Int?
)