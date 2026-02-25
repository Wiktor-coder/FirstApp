package ru.netology.nmedia.model

import ru.netology.nmedia.dto.Post

data class FeedModel(
    val posts: List<Post> = emptyList(),
    val loading: Boolean = false,
    val error: Boolean = false,
    val errorType: ErrorType = ErrorType.UNKNOWN,
    val empty: Boolean = false,
    val refreshing: Boolean = false,
)

enum class ErrorType {
    NETWORK,  // Ошибка сети
    SERVER,   // Ошибка сервера (5хх)
    CLIENT,   // Ошибка клиента (4хх)
    TIMEOUT,  // Таймаут
    UNKNOWN   //Неизвестная ошибка
}