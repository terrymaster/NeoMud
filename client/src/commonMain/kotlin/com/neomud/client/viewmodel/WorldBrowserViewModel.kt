package com.neomud.client.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neomud.client.model.platform.WorldDetail
import com.neomud.client.model.platform.WorldSummary
import com.neomud.client.network.PlatformApiClient
import com.neomud.client.platform.PlatformLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WorldBrowserViewModel(
    private val apiClient: PlatformApiClient
) : ViewModel() {

    private val _worlds = MutableStateFlow<List<WorldSummary>>(emptyList())
    val worlds: StateFlow<List<WorldSummary>> = _worlds

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _selectedWorld = MutableStateFlow<WorldDetail?>(null)
    val selectedWorld: StateFlow<WorldDetail?> = _selectedWorld

    private val _isLoadingDetail = MutableStateFlow(false)
    val isLoadingDetail: StateFlow<Boolean> = _isLoadingDetail

    private var currentPage = 1
    private var totalPages = 1

    fun loadWorlds(search: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = apiClient.getWorlds(page = 1, search = search)
                _worlds.value = response.worlds
                currentPage = response.pagination.page
                totalPages = response.pagination.totalPages
            } catch (e: Exception) {
                PlatformLogger.e("WorldBrowser", "Failed to load worlds", e)
                _error.value = "Failed to load worlds. Check your connection."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadWorldDetail(slug: String) {
        viewModelScope.launch {
            _isLoadingDetail.value = true
            _error.value = null
            try {
                _selectedWorld.value = apiClient.getWorldDetail(slug)
            } catch (e: Exception) {
                PlatformLogger.e("WorldBrowser", "Failed to load world detail", e)
                _error.value = "Failed to load world details."
            } finally {
                _isLoadingDetail.value = false
            }
        }
    }

    fun clearSelection() {
        _selectedWorld.value = null
    }

    fun clearError() {
        _error.value = null
    }
}
