package ru.netology.nmedia.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.netology.nmedia.api.PostApi
import ru.netology.nmedia.auth.AppAuth
import java.net.SocketTimeoutException
import java.net.UnknownHostException

// SignInViewModel.kt
class SignInViewModel : ViewModel() {
    private val _authState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    fun authenticate(login: String, password: String) {
        if (login.isBlank() || password.isBlank()) {
            _authState.value = AuthUiState.Error("Заполните все поля")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthUiState.Loading

            try {
                val response = PostApi.service.authenticate(login, password)//.execute()

                if (response.isSuccessful) {
                    val authResponse = response.body()
                    if (authResponse != null) {
                        // Сохраняем в AppAuth
                        AppAuth.getInstance().setAuth(authResponse.id, authResponse.token)
                        _authState.value = AuthUiState.Success
                    } else {
                        _authState.value = AuthUiState.Error("Пустой ответ от сервера")
                    }
                } else {
                    when (response.code()) {
                        401 -> _authState.value = AuthUiState.Error("Неверный логин или пароль")
                        else -> _authState.value = AuthUiState.Error("Ошибка: ${response.code()}")
                    }
                }
            } catch (e: UnknownHostException) {
                _authState.value = AuthUiState.Error("Нет подключения к интернету")
            } catch (e: SocketTimeoutException) {
                _authState.value = AuthUiState.Error("Превышено время ожидания")
            } catch (e: Exception) {
                _authState.value = AuthUiState.Error("Ошибка: ${e.message}")
            }
        }
    }

    sealed class AuthUiState {
        object Idle : AuthUiState()
        object Loading : AuthUiState()
        object Success : AuthUiState()
        data class Error(val message: String) : AuthUiState()
    }
}