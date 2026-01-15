package com.vaultguard.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultguard.app.security.SecurityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val securityManager: SecurityManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private var attempts = 0
    private val MAX_ATTEMPTS = 7

    fun attemptUnlock(password: String) {
        val prefs = context.getSharedPreferences("vault_guard_prefs", Context.MODE_PRIVATE)
        // Default to "password123" only if not set (backward compatibility)
        val validPassword = prefs.getString("master_password", "password123") ?: "password123"
        val duressPassword = prefs.getString("duress_password", null)

        if (password == validPassword) {
            attempts = 0
            _authState.value = AuthState.Success
        } else if (duressPassword != null && password == duressPassword) {
            // DURESS MODE TRIGGERED: SILENT DESTRUCTION
            // 1. Delete Crypto Key (Data becomes permanently unreadable)
            securityManager.deleteKey()
            
            // 2. Do NOT clear prefs or crash. 
            // We want the app to look NORMAL.
            // Navigate to Dashboard.
            
            // 3. Set Success (Unlock)
            // The Dashboard will try to load secrets, fail to decrypt (no key), and show empty list.
            _authState.value = AuthState.Success
        } else {
            attempts++
            if (attempts >= MAX_ATTEMPTS) {
                securityManager.deleteKey()
                _authState.value = AuthState.Wiped("Self-Destruct Triggered: Master Key Deleted.")
            } else {
                _authState.value = AuthState.Error(attempts)
            }
        }
    }
    
    fun resetState() {
        _authState.value = AuthState.Idle
    }

    fun biometricUnlockSuccess() {
        _authState.value = AuthState.Success
    }

    /**
     * User-initiated Factory Reset / Logout.
     * Clears all keys and preferences.
     */
    fun wipeLocalData() {
        // 1. Destroy Crypto Keys
        securityManager.deleteKey()
        
        // 2. Clear App Preferences (Auth, Flags, etc.)
        context.getSharedPreferences("vault_guard_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
            
        resetState()
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Success : AuthState()
    data class Error(val attemptsUsed: Int) : AuthState()
    data class Wiped(val reason: String) : AuthState()
}
