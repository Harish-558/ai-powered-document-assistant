package com.example.harish331.data.api

import com.example.harish331.data.model.SearchRequest
import com.example.harish331.data.model.SearchResult
import com.example.harish331.data.model.UploadResponse
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {

    @Multipart
    @POST("documents/upload")
    suspend fun uploadDocument(
        @Part file: MultipartBody.Part
    ): UploadResponse

    @POST("search/")
    suspend fun searchDocument(
        @Body request: SearchRequest
    ): SearchResult
}