package com.example.harish331.data.model

data class HistoryItem(
    val id: String,
    val question: String,
    val timestamp: String,
    val documentFilename: String = "Document.pdf"
)
