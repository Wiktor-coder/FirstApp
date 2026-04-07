package ru.netology.nmedia.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.repository.PostRepositorySQLiteImpl
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import retrofit2.HttpException
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.model.ErrorType
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.utils.Result
import ru.netology.nmedia.utils.Result.*

@ExperimentalCoroutinesApi
class PostViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDb.getInstance(application).postDao
    private val repository = PostRepositorySQLiteImpl(application.applicationContext, dao)

    // Состояние UI с использованием StateFlow
    private val _data = MutableStateFlow(FeedModel())
    val data: StateFlow<FeedModel> = _data.asStateFlow()

    private val _edited = MutableStateFlow<Post?>(null)
    val edited: StateFlow<Post?> = _edited.asStateFlow()

    private val _postCreated = MutableSharedFlow<Result<Unit>>()
    val postCreated: SharedFlow<Result<Unit>> = _postCreated.asSharedFlow()

    private val _postSuccess = MutableSharedFlow<Unit>()
    val postSuccess: SharedFlow<Unit> = _postSuccess.asSharedFlow()

    private val _postError = MutableSharedFlow<String>()
    val postError: SharedFlow<String> = _postError.asSharedFlow()

    private val _error = MutableSharedFlow<String>()
    val error: SharedFlow<String> = _error.asSharedFlow()

    init {
        observePosts()
        loadPosts()

        // Обновляем посты при изменении аутентификации
        viewModelScope.launch {
            AppAuth.getInstance().authStateFlow.collect {
                loadPosts(useCache = false) // Перезагружаем посты
            }
        }
    }

    private fun observePosts() {
        repository.observePosts()
            .onEach { posts: List<Post> ->
                _data.update { currentState: FeedModel ->
                    currentState.copy(
                        posts = posts,
                        empty = posts.isEmpty(),
                        loading = false
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun loadPosts(useCache: Boolean = true) {
        viewModelScope.launch {
            _data.update { it.copy(loading = true) }

            if (useCache) {
                val cachedPosts = repository.getLocalPosts()
                if (cachedPosts.isNotEmpty()) {
                    _data.update { currentState ->
                        currentState.copy(
                            posts = cachedPosts,
                            loading = true
                        )
                    }
                }
            }

            val result = repository.getAll()
            when (result) {
                is Success<List<Post>> -> {
                    _data.update { currentState ->
                        currentState.copy(
                            loading = false,
                            error = false
                        )
                    }
                }

                is Error -> {
                    val errorType = determineErrorType(result.exception)
                    _data.update { currentState ->
                        currentState.copy(
                            error = true,
                            loading = false,
                            errorType = errorType
                        )
                    }
                    result.exception?.let { handleError(it) }
                }

                is Loading -> {
                    _data.update { currentState ->
                        currentState.copy(
                            loading = true
                        )
                    }
                }
            }
        }
    }

    fun likeById(id: Long) {
        viewModelScope.launch {
            val currentPosts = _data.value.posts
            val currentPost = currentPosts.find { it.id == id } ?: return@launch

            val isLiking = !currentPost.likedByMe

            // Оптимистичное обновление
            _data.update { state ->
                state.copy(
                    posts = state.posts.map { post ->
                        if (post.id == id) {
                            post.copy(
                                likedByMe = isLiking,
                                likeCount = if (isLiking) post.likeCount + 1 else post.likeCount - 1
                            )
                        } else {
                            post
                        }
                    }
                )
            }

            val result = if (isLiking) {
                repository.likeById(id)
            } else {
                repository.unlikeById(id)
            }

            when (result) {
                is Success<Post> -> {
                    // Обновление уже произошло через observePosts
                }

                is Error -> {
                    // Откат при ошибке
                    _data.update { state ->
                        state.copy(posts = currentPosts)
                    }
                    result.message.let { _postError.emit(it) }
                }

                is Loading -> {}
            }
        }
    }

    fun removeById(id: Long) {
        viewModelScope.launch {
            val result = repository.removeById(id)
            when (result) {
                is Success<Unit> -> {
                    // Удаление через observePosts
                }

                is Error -> {
                    result.message.let { _postError.emit(it) }
                }

                is Loading -> {}
            }
        }
    }

    fun createPost(content: String) {
        viewModelScope.launch {
            if (content.isNotBlank()) {
                val newPost = Post(
                    id = 0L,
                    authorId = 0L,
                    author = "My Post",
                    authorAvatar = null,
                    published = System.currentTimeMillis(),
                    content = content.trim(),
                    likeCount = 0,
                    shareCount = 0,
                    likedByMe = true,
                    ownedByMe = true,
                    video = null,
                    attachment = null,
                )

                val result = repository.save(newPost)
                when (result) {
                    is Success -> {
                        _postCreated.emit(Success(Unit))
                    }

                    is Error -> {
                        _postCreated.emit(Error(result.message))
                    }

                    is Loading -> {
                        // Может быть промежуточное состояние
                    }
                }
            }
        }
    }

    fun save(newContent: String) {
        viewModelScope.launch {
            Log.d("PostViewModel", "save() called with content: '$newContent'")

            val currentPost = _edited.value
            if (currentPost != null) {
                if (currentPost.content != newContent) {
                    Log.d("PostViewModel", "Content changed, updating post")

                    val updatedPost = currentPost.copy(content = newContent)
                    val result = repository.save(updatedPost)

                    when (result) {
                        is Success<Post> -> {
                            Log.d("PostViewModel", "Post saved successfully")
                            _edited.value = null
                            _postSuccess.emit(Unit)
                        }
                        is Error -> {
                            Log.e("PostViewModel", "Error saving post: ${result.message}")
                            result.message.let { _postError.emit(it) }
                        }
                        is Loading -> {
                            Log.d("PostViewModel", "Saving post...")
                        }
                    }
                } else {
                    Log.d("PostViewModel", "Content not changed, closing editor")
                    _edited.value = null
                    _postSuccess.emit(Unit)
                }
            } else {
                Log.e("PostViewModel", "No post to edit")
                _postError.emit("Ошибка: пост не найден")
            }
        }
    }

    fun edit(post: Post) {
        _edited.value = post
    }

    fun cancelEdited() {
        _edited.value = null
    }

    private fun handleError(exception: Throwable) {
        viewModelScope.launch {
            _error.emit(errorHandler(exception))
        }
    }

    private fun determineErrorType(exception: Throwable?): ErrorType {
        return when (exception) {
            is UnknownHostException -> ErrorType.NETWORK
            is SocketTimeoutException -> ErrorType.TIMEOUT
            is HttpException -> {
                when (exception.code()) {
                    in 400..499 -> ErrorType.CLIENT
                    in 500..599 -> ErrorType.SERVER
                    else -> ErrorType.UNKNOWN
                }
            }

            else -> ErrorType.UNKNOWN
        }
    }

    fun hasVideo(post: Post): Boolean = !post.video.isNullOrBlank()
    fun getVideoUrl(post: Post): String? = post.video?.trim()
}

private fun errorHandler(e: Throwable): String {
    return when (e) {
        is UnknownHostException -> "Нет подключения к интернету"
        is SocketTimeoutException -> "Превышено время ожидания"
        is HttpException -> {
            when (e.code()) {
                404 -> "Пост не найден"
                500 -> "Ошибка сервера"
                else -> "Ошибка ${e.code()}"
            }
        }

        else -> "Неизвестная ошибка: ${e.message}"
    }
}