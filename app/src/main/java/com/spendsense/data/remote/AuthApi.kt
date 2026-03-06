package com.spendsense.data.remote

import retrofit2.http.Body
import retrofit2.http.POST

// ---------------------------------------------------------------------------
// Auth API - the ONLY backend communication for account management feature.
// All account data stays on device (SQLite).
//
// FUTURE: When real backend is ready:
//   - Add `POST /auth/google` that accepts Google ID token
//   - Remove `/auth/google/dummy` endpoint usage
//   - Update LoginViewModel to pass the real Google ID token here
// ---------------------------------------------------------------------------

data class DummyAuthRequest(
    val displayName: String,
    val email: String
)

// FUTURE: data class GoogleAuthRequest(val idToken: String)

data class AuthResponse(
    val token: String,
    val userId: String,
    val email: String,
    val name: String
)

interface AuthApi {
    /**
     * Dummy login — no real Google validation.
     * FUTURE: Replace with POST /auth/google that accepts a real Google ID token.
     */
    @POST("auth/google/dummy")
    suspend fun loginDummy(@Body request: DummyAuthRequest): AuthResponse

    // FUTURE:
    // @POST("auth/google")
    // suspend fun loginWithGoogle(@Body request: GoogleAuthRequest): AuthResponse
}
