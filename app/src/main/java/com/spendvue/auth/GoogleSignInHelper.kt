package com.spendvue.auth

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

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
    val credentialManager = CredentialManager.create(activity)

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(webClientId)
        .setNonce(null) // Not needed for standard login
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    val response = credentialManager.getCredential(activity, request)
    val credential = response.credential
    if (credential is CustomCredential &&
        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
    ) {
        val googleIdTokenCredential = GoogleIdTokenCredential
            .createFrom(credential.data)
        return googleIdTokenCredential.idToken
    } else {
        throw IllegalStateException("Unexpected credential type")
    }
}