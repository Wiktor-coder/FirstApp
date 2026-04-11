package ru.netology.nmedia.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import ru.netology.nmedia.api.PostApi
import ru.netology.nmedia.auth.AppAuth
import java.io.File
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class SignUpViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<SignUpUiState>(SignUpUiState.Idle)
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    fun register(
        name: String,
        login: String,
        password: String,
        confirmPassword: String,
        avatarFile: File? = null
    ) {
        // Валидация
        if (name.isBlank()) {
            _uiState.value = SignUpUiState.Error("Введите имя")
            return
        }

        if (login.isBlank()) {
            _uiState.value = SignUpUiState.Error("Введите логин")
            return
        }

        if (password.isBlank()) {
            _uiState.value = SignUpUiState.Error("Введите пароль")
            return
        }

        if (password != confirmPassword) {
            _uiState.value = SignUpUiState.Error("Пароли не совпадают")
            return
        }

        if (password.length < 4) {
            _uiState.value = SignUpUiState.Error("Пароль должен содержать минимум 4 символа")
            return
        }

        viewModelScope.launch {
            _uiState.value = SignUpUiState.Loading

            try {
                val response = if (avatarFile != null && avatarFile.exists()) {
                    // Регистрация с аватаркой
                    val loginBody = login.toRequestBody("text/plain".toMediaType())
                    val passBody = password.toRequestBody("text/plain".toMediaType())
                    val nameBody = name.toRequestBody("text/plain".toMediaType())

                    val filePart = MultipartBody.Part.createFormData(
                        "file",
                        avatarFile.name,
                        avatarFile.asRequestBody("image/*".toMediaType())
                    )

                    PostApi.service.registerUserWithPhoto(loginBody, passBody, nameBody, filePart)
                } else {
                    // Обычная регистрация
                    PostApi.service.registerUser(login, password, name)
                }

                if (response.isSuccessful) {
                    val authResponse = response.body()
                    if (authResponse != null) {
                        AppAuth.getInstance().setAuth(authResponse.id, authResponse.token)
                        _uiState.value = SignUpUiState.Success
                    } else {
                        _uiState.value = SignUpUiState.Error("Пустой ответ от сервера")
                    }
                } else {
                    when (response.code()) {
                        409 -> _uiState.value = SignUpUiState.Error("Пользователь с таким логином уже существует")
                        400 -> _uiState.value = SignUpUiState.Error("Неверные данные")
                        else -> _uiState.value = SignUpUiState.Error("Ошибка: ${response.code()}")
                    }
                }
            } catch (e: UnknownHostException) {
                _uiState.value = SignUpUiState.Error("Нет подключения к интернету")
            } catch (e: SocketTimeoutException) {
                _uiState.value = SignUpUiState.Error("Превышено время ожидания")
            } catch (e: Exception) {
                _uiState.value = SignUpUiState.Error("Ошибка: ${e.message}")
            }
        }
    }

    fun resetState() {
        _uiState.value = SignUpUiState.Idle
    }

    sealed class SignUpUiState {
        object Idle : SignUpUiState()
        object Loading : SignUpUiState()
        object Success : SignUpUiState()
        data class Error(val message: String) : SignUpUiState()
    }
}