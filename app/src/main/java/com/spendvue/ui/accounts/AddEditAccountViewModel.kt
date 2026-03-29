package com.spendvue.ui.accounts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendvue.data.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import javax.inject.Inject

@HiltViewModel
class AddEditAccountViewModel @Inject constructor(
    private val repository: AccountRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private val POPULAR_BANKS = listOf(
            "HDFC Bank", "ICICI Bank", "SBI", "Axis Bank", "Kotak Mahindra Bank",
            "IDFC First Bank", "Yes Bank", "IndusInd Bank", "Bank of Baroda",
            "Punjab National Bank", "Canara Bank", "Other"
        )
    }

    private val accountId: Int = savedStateHandle.get<Int>("accountId") ?: -1
    val isEditing = accountId != -1

    private val _accountName = MutableStateFlow("")
    val accountName: StateFlow<String> = _accountName.asStateFlow()

    private val _bankSelection = MutableStateFlow("HDFC Bank")
    val bankSelection: StateFlow<String> = _bankSelection.asStateFlow()
    
    private val _customBankName = MutableStateFlow("")
    val customBankName: StateFlow<String> = _customBankName.asStateFlow()
    
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
                    if (POPULAR_BANKS.contains(account.bankName) && account.bankName != "Other") {
                        _bankSelection.value = account.bankName
                        _customBankName.value = ""
                    } else {
                        _bankSelection.value = "Other"
                        _customBankName.value = account.bankName
                    }
                    _accountType.value = account.accountType
                    _last4Digits.value = account.last4Digits
                    _currentBalance.value = if (account.accountType == "CREDIT_CARD" && account.currentBalance < 0) 
                        (-account.currentBalance).toString() 
                    else 
                        account.currentBalance.toString()
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
                _bankSelection.value = event.name
                clearError("bankName")
                if (event.name != "Other") {
                    _bankName.value = event.name
                } else {
                    // Keep current custom bank name if any
                    _bankName.value = _customBankName.value
                }
            }
            is AccountFormEvent.CustomBankNameChanged -> {
                _customBankName.value = event.name
                clearError("bankName")
                if (_bankSelection.value == "Other") {
                    _bankName.value = event.name
                }
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
            val balance = _currentBalance.value.toDoubleOrNull()
            if (balance == null) {
                errors["balance"] = "Enter a valid balance"
            } else {
                if (_accountType.value == "CREDIT_CARD" && balance < 0) {
                    errors["balance"] = "Outstanding amount cannot be negative"
                }
            }
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
                    currentBalance = if (_accountType.value == "CREDIT_CARD") 
                        -abs(_currentBalance.value.toDouble())
                    else 
                        _currentBalance.value.toDouble()
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
    data class CustomBankNameChanged(val name: String) : AccountFormEvent()
    data class AccountTypeChanged(val type: String) : AccountFormEvent()
    data class Last4DigitsChanged(val digits: String) : AccountFormEvent()
    data class CurrentBalanceChanged(val balance: String) : AccountFormEvent()
    object SaveAccount : AccountFormEvent()
}

sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
    object NavigateBack : UiEvent()
}
