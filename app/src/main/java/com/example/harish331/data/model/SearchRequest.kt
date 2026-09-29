package com.example.harish331.data.model

data class SearchRequest(
    val query: String = "",
    val top_k: Int = 3
)