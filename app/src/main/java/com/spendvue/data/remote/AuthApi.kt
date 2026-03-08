package com.spendvue.data.remote

import retrofit2.http.Body
import retrofit2.http.POST

// ---------------------------------------------------------------------------
// Auth API - the ONLY backend communication for account management feature.
// All account data stays on device (SQLite).
// ---------------------------------------------------------------------------

data class DummyAuthRequest(
    val displayName: String,
    val email: String
)

data class GoogleAuthRequest(
    val idToken: String
)

data class AuthResponse(
    val token: String,
    val userId: String,
    val email: String,
    val name: String
)

interface AuthApi {
    /**
     * Dummy login — no real Google validation.
     * TODO: Remove after migrating to real Google Sign-In.
     */
    @POST("auth/google/dummy")
    suspend fun loginDummy(@Body request: DummyAuthRequest): AuthResponse

    /**
     * Real Google authentication with ID token validation.
     */
    @POST("auth/google")
    suspend fun loginWithGoogle(@Body request: GoogleAuthRequest): AuthResponse
}
