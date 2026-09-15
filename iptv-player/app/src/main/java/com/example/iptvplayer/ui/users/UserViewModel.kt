package com.example.iptvplayer.ui.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.iptvplayer.domain.model.User
import com.example.iptvplayer.domain.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UserViewModel(
    private val userRepository: UserRepository,
    coroutineScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope = coroutineScope ?: viewModelScope

    val users: StateFlow<List<User>> = userRepository.getAllUsers()
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val activeUser: StateFlow<User?> = userRepository.activeUser

    private val _selectedUserForPin = MutableStateFlow<User?>(null)
    val selectedUserForPin: StateFlow<User?> = _selectedUserForPin.asStateFlow()

    private val _pinInput = MutableStateFlow("")
    val pinInput: StateFlow<String> = _pinInput.asStateFlow()

    private val _pinError = MutableStateFlow<String?>(null)
    val pinError: StateFlow<String?> = _pinError.asStateFlow()

    private val _actionFeedback = MutableStateFlow<String?>(null)
    val actionFeedback: StateFlow<String?> = _actionFeedback.asStateFlow()

    fun selectUserToLogin(user: User, onLoginSuccess: (User) -> Unit) {
        if (!user.isPinProtected) {
            // Direct login without PIN
            scope.launch {
                val result = userRepository.login(user.id, null)
                result.onSuccess { loggedIn ->
                    onLoginSuccess(loggedIn)
                }.onFailure { error ->
                    _pinError.value = error.message ?: "Erro ao iniciar sessão"
                }
            }
        } else {
            // Prompt for PIN
            _selectedUserForPin.value = user
            _pinInput.value = ""
            _pinError.value = null
        }
    }

    fun onPinDigitEntered(digit: Char) {
        if (_pinInput.value.length < 8) {
            _pinInput.value += digit
            _pinError.value = null
        }
    }

    fun onPinBackspace() {
        if (_pinInput.value.isNotEmpty()) {
            _pinInput.value = _pinInput.value.dropLast(1)
            _pinError.value = null
        }
    }

    fun onPinClear() {
        _pinInput.value = ""
        _pinError.value = null
    }

    fun setPinInput(text: String) {
        _pinInput.value = text.filter { it.isDigit() }.take(8)
        _pinError.value = null
    }

    fun submitPin(onLoginSuccess: (User) -> Unit) {
        val user = _selectedUserForPin.value ?: return
        scope.launch {
            val result = userRepository.login(user.id, _pinInput.value)
            result.onSuccess { loggedIn ->
                _selectedUserForPin.value = null
                _pinInput.value = ""
                _pinError.value = null
                onLoginSuccess(loggedIn)
            }.onFailure { error ->
                _pinError.value = if (error is SecurityException) {
                    "PIN incorreto. Tente novamente."
                } else {
                    error.message ?: "Falha na autenticação"
                }
            }
        }
    }

    fun dismissPinDialog() {
        _selectedUserForPin.value = null
        _pinInput.value = ""
        _pinError.value = null
    }

    fun logout(onLoggedOut: () -> Unit) {
        userRepository.logout()
        onLoggedOut()
    }

    fun createUser(
        username: String,
        displayName: String?,
        pin: String?,
        avatarColor: Long,
        onSuccess: (User) -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            val result = userRepository.createUser(
                username = username,
                displayName = displayName,
                pinOrPassword = pin,
                avatarColor = avatarColor
            )
            result.onSuccess { newUser ->
                _actionFeedback.value = "Utilizador criado com sucesso"
                onSuccess(newUser)
            }.onFailure { error ->
                onError(error.message ?: "Erro ao criar utilizador")
            }
        }
    }

    fun updateUser(
        userId: String,
        displayName: String?,
        newPin: String?,
        avatarColor: Long?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            val result = userRepository.updateUser(userId, displayName, newPin, avatarColor)
            result.onSuccess {
                _actionFeedback.value = "Perfil atualizado com sucesso"
                onSuccess()
            }.onFailure { error ->
                onError(error.message ?: "Erro ao atualizar perfil")
            }
        }
    }

    fun deleteUser(
        userId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            val result = userRepository.deleteUser(userId)
            result.onSuccess {
                _actionFeedback.value = "Utilizador eliminado com sucesso"
                onSuccess()
            }.onFailure { error ->
                onError(error.message ?: "Não foi possível eliminar o utilizador")
            }
        }
    }

    fun clearFeedback() {
        _actionFeedback.value = null
    }

    companion object {
        fun provideFactory(userRepository: UserRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return UserViewModel(userRepository) as T
                }
            }
    }
}
