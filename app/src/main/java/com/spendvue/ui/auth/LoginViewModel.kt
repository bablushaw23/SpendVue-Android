package com.spendvue.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendvue.auth.AuthManager
import com.spendvue.data.remote.AuthApi
import com.spendvue.data.remote.DummyAuthRequest
import com.spendvue.data.remote.GoogleAuthRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    object Success : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authApi: AuthApi,
    private val authManager: AuthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /**
     * Dummy login — generates a user identity from the provided name/email
     * without any real Google authentication.
     *
     * FUTURE: Replace this with real Google Sign-In flow:
     *   1. Launch CredentialManager to get Google ID token
     *   2. Call loginWithGoogle(idToken) instead
     *   3. Remove displayName/email parameters (those come from ID token)
     */
    fun loginDummy(displayName: String, email: String) {
        if (displayName.isBlank() || email.isBlank()) {
            _uiState.value = LoginUiState.Error("Please enter your name and email")
            return
        }
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                val response = authApi.loginDummy(
                    DummyAuthRequest(displayName = displayName, email = email)
                )
                authManager.saveToken(response.token)
                _uiState.value = LoginUiState.Success
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(
                    "Connection failed. Is the backend running?\n${e.message}"
                )
            }
        }
    }

    fun loginWithGoogle(idToken: String) {
        if (idToken.isBlank()) {
            _uiState.value = LoginUiState.Error("Invalid Google ID token")
            return
        }
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                val response = authApi.loginWithGoogle(
                    GoogleAuthRequest(idToken = idToken)
                )
                authManager.saveToken(response.token)
                _uiState.value = LoginUiState.Success
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(
                    "Google authentication failed.\n${e.message}"
                )
            }
        }
    }

    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }
}
