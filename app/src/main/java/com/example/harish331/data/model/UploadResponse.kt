package com.example.harish331.data.model

data class UploadResponse(
    val message: String,
    val filename: String,
    val path: String,
    val chunks_indexed: Int
)