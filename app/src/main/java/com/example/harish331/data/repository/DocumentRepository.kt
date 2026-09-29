package com.example.harish331.data.repository

import com.example.harish331.data.model.SearchRequest
import com.example.harish331.data.model.SearchResult
import android.content.Context
import android.net.Uri
import com.example.harish331.data.api.ApiClient
import com.example.harish331.data.model.UploadResponse
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class DocumentRepository {

    suspend fun uploadDocument(
        context: Context,
        uri: Uri,
        filename: String
    ): UploadResponse {

        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw Exception("Unable to open selected PDF")

        val fileBytes = inputStream.use { it.readBytes() }

        val requestBody = fileBytes.toRequestBody(
            "application/pdf".toMediaType()
        )

        val filePart = MultipartBody.Part.createFormData(
            "file",
            filename,
            requestBody
        )

        return ApiClient.apiService.uploadDocument(filePart)
    }
    suspend fun searchDocument(
        query: String,
        topK: Int = 3
    ): SearchResult {
        return ApiClient.apiService.searchDocument(
            SearchRequest(
                query = query
            )
        )
    }
}