package com.example.harish331.data.model

data class Document(
    val id: String = "doc_01",
    val filename: String = "",
    val filesize: String = "",
    val pageCount: Int = 1,
    val chunkCount: Int = 1,
    val status: String = "Active",
    val uploadDate: String = "",
    val uriString: String? = null
)