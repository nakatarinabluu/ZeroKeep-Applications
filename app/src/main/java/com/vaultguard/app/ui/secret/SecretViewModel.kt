package com.vaultguard.app.ui.secret

// import com.vaultguard.app.domain.repository.SecretRepository // Removed
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultguard.app.domain.use_case.DeleteSecretUseCase
import com.vaultguard.app.domain.use_case.GetSecretsUseCase
import com.vaultguard.app.domain.use_case.SaveSecretUseCase
import com.vaultguard.app.security.SecurityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class SecretState {
    object Loading : SecretState()
    data class Success(val secrets: List<SecretUiModel>) : SecretState()
    data class Error(val message: String) : SecretState()
}

@HiltViewModel
class SecretViewModel
@Inject
constructor(
        private val getSecretsUseCase: GetSecretsUseCase,
        private val saveSecretUseCase: SaveSecretUseCase,
        private val deleteSecretUseCase: DeleteSecretUseCase,
        private val securityManager:
                SecurityManager, // Still needed for Wipe/Auth checks? Or move to UseCase? For now
        // kept for Wipe.
        private val prefs: SharedPreferences,
        private val repository:
                com.vaultguard.app.domain.repository.SecretRepository // Kept ONLY for WipeVault
// which isn't refactored yet.
// Ideally WipeVaultUseCase.
) : ViewModel() {

    private val _isBiometricsEnabled =
            MutableStateFlow(prefs.getBoolean("biometrics_enabled", false))
    val isBiometricsEnabled: StateFlow<Boolean> = _isBiometricsEnabled

    fun setBiometricEnabled(enabled: Boolean) {
        _isBiometricsEnabled.value = enabled
        prefs.edit().putBoolean("biometrics_enabled", enabled).apply()
    }

    private val _saveState = MutableStateFlow<Result<Unit>?>(null)
    val saveState: StateFlow<Result<Unit>?> = _saveState

    // UI State Management
    private val _secretState = MutableStateFlow<SecretState>(SecretState.Loading)
    val state: StateFlow<SecretState> = _secretState

    fun loadSecrets() {
        viewModelScope.launch {
            _secretState.value = SecretState.Loading

            val ownerHash =
                    prefs.getString("vault_id", null)
                            ?: run {
                                com.vaultguard.app.security.SecurityUtils.sha256("default_user")
                            }

            // USE CASE CALL
            val result = getSecretsUseCase(ownerHash)

            result
                    .onSuccess { domainSecrets ->
                        // Map Domain -> UI
                        val uiSecrets =
                                domainSecrets.map { secret ->
                                    SecretUiModel(
                                            id = secret.id,
                                            title = secret.title,
                                            username = secret.username,
                                            password = secret.password
                                    )
                                }
                        _secretState.value = SecretState.Success(uiSecrets)
                    }
                    .onFailure { e ->
                        _secretState.value =
                                SecretState.Error("Failed to fetch secrets: ${e.message}")
                    }
        }
    }

    fun saveSecret(title: String, username: String, secret: String) {
        viewModelScope.launch {
            try {
                // Fix: ownerHash logic duplicated in UseCase? No, passed as arg.
                val ownerHash =
                        prefs.getString("vault_id", null)
                                ?: com.vaultguard.app.security.SecurityUtils.sha256("default_user")

                // USE CASE CALL
                val result = saveSecretUseCase(title, username, secret, ownerHash)

                _saveState.value = result
                if (result.isSuccess) loadSecrets()
            } catch (e: Exception) {
                _saveState.value = Result.failure(e)
            }
        }
    }

    fun deleteSecret(id: String) {
        viewModelScope.launch {
            val ownerHash = prefs.getString("vault_id", "default_vault")!!

            // USE CASE CALL
            val result = deleteSecretUseCase(id, ownerHash)

            if (result.isSuccess) {
                loadSecrets()
            } else {
                android.util.Log.e("SecretViewModel", "Delete failed", result.exceptionOrNull())
            }
        }
    }

    fun clearSensitiveData() {
        _secretState.value = SecretState.Success(emptyList())
        _saveState.value = null
    }

    fun wipeVault(token: String) {
        viewModelScope.launch {
            try {
                // Direct Repository call (Technical Debt: Should be WipeVaultUseCase)
                val result = repository.wipeVault(token)
                if (result.isFailure) {
                    android.util.Log.e(
                            "SecretViewModel",
                            "Server Wipe Failed",
                            result.exceptionOrNull()
                    )
                }

                securityManager.deleteKey()
                setBiometricEnabled(false)
                _secretState.value = SecretState.Success(emptyList())
            } catch (e: Exception) {
                android.util.Log.e("SecretViewModel", "Wipe Error", e)
                securityManager.deleteKey()
            }
        }
    }
}

data class SecretUiModel(
        val id: String,
        val title: String,
        val username: String,
        val password: String
)
