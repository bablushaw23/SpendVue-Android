package com.spendsense.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages authentication state locally.
 *
 * Stores JWT in EncryptedSharedPreferences.
 * Provides [getUserId], [getEmail], [getName] by decoding the JWT payload.
 *
 * FUTURE: When real backend is integrated, this class needs NO changes —
 * only the token acquisition flow (LoginViewModel + AuthApi) will change.
 * The JWT format (sub/user_id claim) must match what the real backend issues.
 */
@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_FILE = "spendsense_auth_prefs"
        private const val KEY_JWT = "jwt_token"
    }

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveToken(jwt: String) {
        prefs.edit().putString(KEY_JWT, jwt).apply()
    }

    fun clearToken() {
        prefs.edit().remove(KEY_JWT).apply()
    }

    fun getToken(): String? = prefs.getString(KEY_JWT, null)

    fun isLoggedIn(): Boolean = getToken() != null

    /** Extract user_id from JWT payload (uses 'sub' or 'user_id' claim). */
    fun getUserId(): String {
        val payload = decodePayload() ?: error("Not authenticated")
        return payload.optString("user_id").ifBlank {
            payload.optString("sub").ifBlank { error("JWT missing user_id") }
        }
    }

    fun getEmail(): String? = decodePayload()?.optString("email")

    fun getName(): String? = decodePayload()?.optString("name")

    private fun decodePayload(): JSONObject? {
        val token = getToken() ?: return null
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return null
            // Pad base64 if needed
            var padded = parts[1].replace('-', '+').replace('_', '/')
            while (padded.length % 4 != 0) padded += "="
            val decoded = Base64.getDecoder().decode(padded)
            JSONObject(String(decoded))
        } catch (e: Exception) {
            null
        }
    }
}
