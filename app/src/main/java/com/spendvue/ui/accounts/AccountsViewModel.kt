package com.spendvue.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendvue.data.local.entity.Account
import com.spendvue.data.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs
import javax.inject.Inject

sealed class AccountsUiEvent {
    data class ShowSnackbar(val message: String) : AccountsUiEvent()
}

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val repository: AccountRepository
) : ViewModel() {

    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()

    private val _archivedAccounts = MutableStateFlow<List<Account>>(emptyList())
    val archivedAccounts: StateFlow<List<Account>> = _archivedAccounts.asStateFlow()

    private val _totalBalance = MutableStateFlow(0.0)
    val totalBalance: StateFlow<Double> = _totalBalance.asStateFlow()

    private val _activeCount = MutableStateFlow(0)
    val activeCount: StateFlow<Int> = _activeCount.asStateFlow()

    private val _showArchived = MutableStateFlow(false)
    val showArchived: StateFlow<Boolean> = _showArchived.asStateFlow()

    private val _uiEvent = MutableStateFlow<AccountsUiEvent?>(null)
    val uiEvent: StateFlow<AccountsUiEvent?> = _uiEvent.asStateFlow()

    init {
        fetchAccounts()
        fetchArchivedAccounts()
    }

    private fun fetchAccounts() {
        viewModelScope.launch {
            repository.getActiveAccounts().collectLatest { list ->
                _accounts.value = list
                _activeCount.value = list.size
                _totalBalance.value = list.sumOf { account ->
                    if (account.accountType == "CREDIT_CARD") -abs(account.currentBalance)
                    else account.currentBalance
                }
            }
        }
    }

    private fun fetchArchivedAccounts() {
        viewModelScope.launch {
            repository.getArchivedAccounts().collectLatest { list ->
                _archivedAccounts.value = list
            }
        }
    }

    fun toggleShowArchived() {
        _showArchived.value = !_showArchived.value
    }

    fun archiveAccount(accountId: Int) {
        viewModelScope.launch {
            val result = repository.archiveAccount(accountId)
            if (result.isFailure) {
                _uiEvent.value = AccountsUiEvent.ShowSnackbar(
                    result.exceptionOrNull()?.message ?: "Failed to archive"
                )
            }
        }
    }

    fun unarchiveAccount(accountId: Int) {
        viewModelScope.launch {
            val result = repository.unarchiveAccount(accountId)
            if (result.isFailure) {
                _uiEvent.value = AccountsUiEvent.ShowSnackbar(
                    result.exceptionOrNull()?.message ?: "Failed to restore"
                )
            }
        }
    }

    fun clearEvent() {
        _uiEvent.value = null
    }
}
