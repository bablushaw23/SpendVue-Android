package com.spendsense.ui.accounts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendsense.data.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditAccountViewModel @Inject constructor(
    private val repository: AccountRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val accountId: Int = savedStateHandle.get<Int>("accountId") ?: -1
    val isEditing = accountId != -1

    private val _accountName = MutableStateFlow("")
    val accountName: StateFlow<String> = _accountName.asStateFlow()

    private val _bankName = MutableStateFlow("HDFC Bank")
    val bankName: StateFlow<String> = _bankName.asStateFlow()

    private val _accountType = MutableStateFlow("SALARY")
    val accountType: StateFlow<String> = _accountType.asStateFlow()

    private val _last4Digits = MutableStateFlow("")
    val last4Digits: StateFlow<String> = _last4Digits.asStateFlow()

    private val _currentBalance = MutableStateFlow("")
    val currentBalance: StateFlow<String> = _currentBalance.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** Per-field validation errors keyed by field name. */
    private val _formErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val formErrors: StateFlow<Map<String, String>> = _formErrors.asStateFlow()

    private val _uiEvent = MutableStateFlow<UiEvent?>(null)
    val uiEvent: StateFlow<UiEvent?> = _uiEvent.asStateFlow()

    init {
        if (isEditing) {
            viewModelScope.launch {
                repository.getAccountById(accountId)?.let { account ->
                    _accountName.value = account.accountName
                    _bankName.value = account.bankName
                    _accountType.value = account.accountType
                    _last4Digits.value = account.last4Digits
                    _currentBalance.value = account.currentBalance.toString()
                }
            }
        }
    }

    fun onEvent(event: AccountFormEvent) {
        when (event) {
            is AccountFormEvent.AccountNameChanged -> {
                _accountName.value = event.name
                clearError("accountName")
            }
            is AccountFormEvent.BankNameChanged -> {
                _bankName.value = event.name
                clearError("bankName")
            }
            is AccountFormEvent.AccountTypeChanged -> _accountType.value = event.type
            is AccountFormEvent.Last4DigitsChanged -> {
                if (event.digits.length <= 4 && event.digits.all { it.isDigit() }) {
                    _last4Digits.value = event.digits
                    clearError("last4Digits")
                }
            }
            is AccountFormEvent.CurrentBalanceChanged -> {
                _currentBalance.value = event.balance
                clearError("balance")
            }
            is AccountFormEvent.SaveAccount -> saveAccount()
        }
    }

    private fun saveAccount() {
        val errors = mutableMapOf<String, String>()

        if (_accountName.value.isBlank()) errors["accountName"] = "Account name is required"
        if (_bankName.value.isBlank()) errors["bankName"] = "Please select a bank"
        if (!isEditing) {
            if (_last4Digits.value.length != 4) errors["last4Digits"] = "Enter exactly 4 digits"
            if (_currentBalance.value.toDoubleOrNull() == null) errors["balance"] = "Enter a valid balance"
        }

        if (errors.isNotEmpty()) {
            _formErrors.value = errors
            return
        }

        _isLoading.value = true

        if (isEditing) {
            viewModelScope.launch {
                val result = repository.updateAccountNameAndBank(
                    accountId = accountId,
                    newName = _accountName.value.trim(),
                    newBank = _bankName.value
                )
                _isLoading.value = false
                _uiEvent.value = if (result.isSuccess) {
                    UiEvent.NavigateBack
                } else {
                    UiEvent.ShowSnackbar(result.exceptionOrNull()?.message ?: "Failed to update")
                }
            }
        } else {
            viewModelScope.launch {
                val result = repository.createAccount(
                    accountName = _accountName.value.trim(),
                    bankName = _bankName.value,
                    accountType = _accountType.value,
                    last4Digits = _last4Digits.value,
                    currentBalance = _currentBalance.value.toDouble()
                )
                _isLoading.value = false
                _uiEvent.value = if (result.isSuccess) {
                    UiEvent.ShowSnackbar("✅ Account added!")
                } else {
                    UiEvent.ShowSnackbar(result.exceptionOrNull()?.message ?: "Failed to add account")
                }
                if (result.isSuccess) {
                    _uiEvent.value = UiEvent.NavigateBack
                }
            }
        }
    }

    fun archiveCurrentAccount() {
        if (!isEditing) return
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.archiveAccount(accountId)
            _isLoading.value = false
            _uiEvent.value = if (result.isSuccess) {
                UiEvent.NavigateBack
            } else {
                UiEvent.ShowSnackbar(result.exceptionOrNull()?.message ?: "Failed to archive")
            }
        }
    }

    fun onEventConsumed() {
        _uiEvent.value = null
    }

    private fun clearError(field: String) {
        if (_formErrors.value.containsKey(field)) {
            _formErrors.value = _formErrors.value - field
        }
    }
}

sealed class AccountFormEvent {
    data class AccountNameChanged(val name: String) : AccountFormEvent()
    data class BankNameChanged(val name: String) : AccountFormEvent()
    data class AccountTypeChanged(val type: String) : AccountFormEvent()
    data class Last4DigitsChanged(val digits: String) : AccountFormEvent()
    data class CurrentBalanceChanged(val balance: String) : AccountFormEvent()
    object SaveAccount : AccountFormEvent()
}

sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
    object NavigateBack : UiEvent()
}
