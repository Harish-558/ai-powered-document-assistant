package com.example.harish331.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.harish331.data.model.UploadResponse
import com.example.harish331.data.model.SearchResult
import com.example.harish331.data.repository.DocumentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


class DocumentViewModel : ViewModel() {

    private val repository = DocumentRepository()

    private val _uploadResult = MutableStateFlow<UploadResponse?>(null)
    val uploadResult: StateFlow<UploadResponse?> = _uploadResult

    private val _uploadError = MutableStateFlow<String?>(null)
    val uploadError: StateFlow<String?> = _uploadError

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading

    private val _searchResult = MutableStateFlow<SearchResult?>(null)
    val searchResult: StateFlow<SearchResult?> = _searchResult

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    fun uploadDocument(
        context: Context,
        uri: Uri,
        filename: String
    ) {
        viewModelScope.launch {
            try {
                _isUploading.value = true
                _uploadError.value = null
                _uploadResult.value = null

                val response = repository.uploadDocument(
                    context,
                    uri,
                    filename
                )

                _uploadResult.value = response

            } catch (e: Exception) {
                _uploadError.value = e.message ?: "Upload failed"
            } finally {
                _isUploading.value = false
            }
        }
    }

    fun searchDocument(query: String) {
        viewModelScope.launch {
            try {
                _isSearching.value = true
                _searchError.value = null
                _searchResult.value = null

                val response = repository.searchDocument(query)

                _searchResult.value = response

            } catch (e: Exception) {
                _searchError.value = e.message ?: "Search failed"
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun resetUploadState() {
        _uploadResult.value = null
        _uploadError.value = null
    }

    fun resetSearchState() {
        _searchResult.value = null
        _searchError.value = null
    }
}
