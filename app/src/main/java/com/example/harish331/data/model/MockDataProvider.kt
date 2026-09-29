package com.example.harish331.data.model

object MockDataProvider {
    fun getSampleDocument() = Document(
        id = "doc_01",
        filename = "Resume.pdf",
        filesize = "2.4 MB",
        pageCount = 5,
        chunkCount = 12,
        status = "Active",
        uploadDate = "2026-09-23"
    )

    fun getSampleSearchResults(query: String = "What are the person's skills and technical experience?"): SearchResultResponse {
        return SearchResultResponse(
            query = query,
            results = listOf(
                SearchApiResult(
                    document = "TECHNICAL SKILLS SUMMARY:\n• Languages: Python, Java, Kotlin, C++, SQL\n• Frameworks: Android SDK, Jetpack Compose, FastAPI, PyTorch",
                    distance = 0.12
                ),
                SearchApiResult(
                    document = "WORK EXPERIENCE:\nSoftware Developer — ABC Technologies (2022 - 2024)\n• Led the development of native Android applications using Kotlin.",
                    distance = 0.25
                ),
                SearchApiResult(
                    document = "EDUCATION:\nBachelor of Technology in Computer Science & Engineering\nXYZ University (2020 - 2024)",
                    distance = 0.38
                )
            )
        )
    }

    fun getSampleHistory(): List<HistoryItem> {
        return listOf(
            HistoryItem("h1", "What are the person's skills and technical experience?", "Today, 10:24 AM"),
            HistoryItem("h2", "What is the education background?", "Today, 09:15 AM"),
            HistoryItem("h3", "What are the key achievements?", "Yesterday, 04:32 PM"),
            HistoryItem("h4", "Tell me about the work experience", "Yesterday, 02:18 PM")
        )
    }
}
