package com.example.harish331.data.model

data class SearchApiResult(
    val document: String = "",
    val distance: Double = 0.0
)

data class SearchResult(
    val query: String = "",
    val results: List<SearchApiResult> = emptyList()
)

typealias SearchResultResponse = SearchResult