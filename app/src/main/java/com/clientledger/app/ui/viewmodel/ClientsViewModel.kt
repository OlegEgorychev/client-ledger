package com.clientledger.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clientledger.app.data.entity.ClientEntity
import com.clientledger.app.data.repository.LedgerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ClientsUiState(
    val clients: List<ClientEntity> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

class ClientsViewModel(private val repository: LedgerRepository) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _uiState = MutableStateFlow(ClientsUiState())
    val uiState: StateFlow<ClientsUiState> = _uiState.asStateFlow()

    init {
        loadClients()
    }

    private fun loadClients() {
        viewModelScope.launch {
            _searchQuery.collect { query ->
                if (query.isBlank()) {
                    repository.getAllClients().collect { clients ->
                        _uiState.value = _uiState.value.copy(clients = clients)
                    }
                } else {
                    repository.searchClients(query).collect { clients ->
                        _uiState.value = _uiState.value.copy(clients = clients)
                    }
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    suspend fun deleteClient(client: ClientEntity) {
        repository.deleteClient(client)
    }

    suspend fun getClientById(id: Long): ClientEntity? {
        return repository.getClientById(id)
    }
}


