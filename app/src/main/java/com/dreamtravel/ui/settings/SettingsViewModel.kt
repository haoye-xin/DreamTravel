package com.dreamtravel.ui.settings

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dreamtravel.data.remote.AccountManager
import com.dreamtravel.data.remote.AuthResult
import com.dreamtravel.analytics.AnalyticsEvent
import com.dreamtravel.analytics.AnalyticsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountState(
    val isSignedIn: Boolean = false,
    val isAnonymous: Boolean = true,
    val email: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val accountManager: AccountManager,
    private val analytics: AnalyticsManager
) : ViewModel() {

    private val _state = MutableStateFlow(AccountState())
    val state: StateFlow<AccountState> = _state.asStateFlow()

    init {
        refreshState()
    }

    fun refreshState() {
        _state.update {
            it.copy(
                isSignedIn = accountManager.isSignedIn(),
                isAnonymous = accountManager.isAnonymous(),
                email = accountManager.getUserEmail(),
                error = null
            )
        }
    }

    fun upgradeAccount(email: String, password: String) {
        val validationError = validateInput(email, password)
        if (validationError != null) {
            _state.update { it.copy(error = validationError) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = accountManager.linkAnonymousToEmail(email, password)) {
                is AuthResult.Success -> {
                    analytics.logEvent(AnalyticsEvent.ACCOUNT_UPGRADE)
                    refreshState()
                }
                is AuthResult.Error -> _state.update { it.copy(error = result.message) }
            }
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun signUp(email: String, password: String) {
        val validationError = validateInput(email, password)
        if (validationError != null) {
            _state.update { it.copy(error = validationError) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = accountManager.signUpWithEmail(email, password)) {
                is AuthResult.Success -> {
                    analytics.logEvent(AnalyticsEvent.ACCOUNT_SIGN_UP)
                    refreshState()
                }
                is AuthResult.Error -> _state.update { it.copy(error = result.message) }
            }
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun signIn(email: String, password: String) {
        val validationError = validateInput(email, password)
        if (validationError != null) {
            _state.update { it.copy(error = validationError) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = accountManager.signInWithEmail(email, password)) {
                is AuthResult.Success -> {
                    analytics.logEvent(AnalyticsEvent.ACCOUNT_SIGN_IN)
                    refreshState()
                }
                is AuthResult.Error -> _state.update { it.copy(error = result.message) }
            }
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (accountManager.signOut()) {
                is AuthResult.Success -> {
                    analytics.logEvent(AnalyticsEvent.ACCOUNT_SIGN_OUT)
                    refreshState()
                }
                is AuthResult.Error -> _state.update { it.copy(error = "退出登录失败") }
            }
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private fun validateInput(email: String, password: String): String? {
        if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return "请输入有效的邮箱地址"
        }
        if (password.length < 6) {
            return "密码至少需要6位"
        }
        if (password.length > 128) {
            return "密码不能超过128位"
        }
        return null
    }
}
