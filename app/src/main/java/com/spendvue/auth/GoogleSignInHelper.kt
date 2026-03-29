package com.spendvue.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.*
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * Checks if Google Play Services is available and up to date.
 * Returns true if available, false otherwise.
 */
private fun isGooglePlayServicesAvailable(context: Context): Boolean {
    val googleApiAvailability = GoogleApiAvailability.getInstance()
    val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context)
    val version = googleApiAvailability.getApkVersion(context)
    Log.d("GoogleSignInHelper", "Google Play Services check: resultCode=$resultCode, version=$version")
    
    if (resultCode != ConnectionResult.SUCCESS) {
        val errorString = googleApiAvailability.getErrorString(resultCode)
        Log.e("GoogleSignInHelper", "Google Play Services not available: $resultCode - $errorString")
        return false
    }
    Log.d("GoogleSignInHelper", "Google Play Services available, version: $version")
    return true
}

/**
 * Helper for Google Sign‑In using CredentialManager.
 * Returns a Google ID token string on success.
 *
 * @param activity The host activity for launching the credential picker.
 * @param webClientId OAuth 2.0 web client ID (from Firebase console).
 * @return The Google ID token string.
 * @throws GetCredentialException if the flow fails or is canceled.
 */
suspend fun getGoogleIdToken(activity: Activity, webClientId: String): String {
    Log.d("GoogleSignInHelper", "Starting Google Sign-In with webClientId: $webClientId")
    
    // Check Google Play Services availability
    if (!isGooglePlayServicesAvailable(activity)) {
        Log.e("GoogleSignInHelper", "Google Play Services not available")
        throw GetCredentialProviderConfigurationException("Google Play Services not available or outdated")
    }
    
    val credentialManager = CredentialManager.create(activity)

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(webClientId)
        .setNonce(null) // Not needed for standard login
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    try {
        Log.d("GoogleSignInHelper", "Requesting credential from CredentialManager")
        val response = credentialManager.getCredential(activity, request)
        Log.d("GoogleSignInHelper", "CredentialManager response received")
        val credential = response.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdTokenCredential = GoogleIdTokenCredential
                .createFrom(credential.data)
            Log.d("GoogleSignInHelper", "Google ID token obtained successfully")
            return googleIdTokenCredential.idToken
        } else {
            Log.e("GoogleSignInHelper", "Unexpected credential type: ${credential::class.simpleName}")
            throw IllegalStateException("Unexpected credential type: ${credential::class.simpleName}")
        }
    } catch (e: GetCredentialException) {
        Log.e("GoogleSignInHelper", "CredentialManager error: ${e.message}", e)
        
        // Provide more specific error messages
        val userMessage = when (e) {
            is NoCredentialException -> "No Google accounts found on device. Please add a Google account in Settings."
            is GetCredentialCancellationException -> "Sign-in cancelled"
            is GetCredentialInterruptedException -> "Sign-in interrupted. Please try again."
            is GetCredentialProviderConfigurationException -> "Google Sign-In not configured properly. Check app configuration."
            is GetCredentialUnknownException -> "Unknown error during sign-in"
            else -> "Google Sign-In failed: ${e.message}"
        }
        
        throw GetCredentialUnknownException(userMessage)
    } catch (e: Exception) {
        Log.e("GoogleSignInHelper", "Unexpected error: ${e.message}", e)
        throw GetCredentialUnknownException("Google Sign-In failed: ${e.message}")
    }
}