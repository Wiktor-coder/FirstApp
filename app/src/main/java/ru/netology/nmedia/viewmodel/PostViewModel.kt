package ru.netology.nmedia.viewmodel

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import retrofit2.HttpException
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.ErrorType
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.repository.PostRepositorySQLiteImpl
import ru.netology.nmedia.utils.SingleLiveEvent
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class PostViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDb.getInstance(application).postDao
    private val repository: PostRepository =
        PostRepositorySQLiteImpl(application.applicationContext, dao)

    private val _data = MutableLiveData<FeedModel>(FeedModel())
    val data: LiveData<FeedModel> = _data

    private val _edited = MutableLiveData<Post?>()
    val edited: LiveData<Post?> = _edited
    val isEditing: LiveData<Boolean> = _edited.map { it != null }

    private val _postCreated = SingleLiveEvent<Result<Unit>>()
    val postCreated: LiveData<Result<Unit>> = _postCreated

    val _postError = SingleLiveEvent<String>()
    val postError: LiveData<String> = _postError

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        loadPost()
    }

    fun loadPost(useCache: Boolean = true) {
//        _data.postValue(FeedModel(loading = true))
        _data.value = FeedModel(loading = true, posts = _data.value?.posts ?: emptyList())

//        if (useCache) {
//            // Показываем кэшированные данные сразу
//            val cachedPosts = repository.getLocalPosts()
//            if (cachedPosts.isNotEmpty()) {
//                _data.postValue(FeedModel(posts = cachedPosts))
//            }
//        }
        if (useCache) {
            val cachedPosts = repository.getLocalPosts()
            if (cachedPosts.isNotEmpty()) {
                // Показываем кэш, но оставляем loading=true для фоновой загрузки
                _data.value = FeedModel(
                    posts = cachedPosts,
                    loading = true  // показываем, что идет обновление
                )
            }
        }

        repository.getAllAsync(object : PostRepository.PostCallback<List<Post>> {
            override fun onSuccess(result: List<Post>) {
                _data.value = FeedModel(
                    posts = result,
                    empty = result.isEmpty(),
                    loading = false
                )
                _error.value = null
            }

            override fun onError(e: Throwable) {
                e.printStackTrace()
                val errorType = when (e) {
                    is UnknownHostException -> ErrorType.NETWORK
                    is SocketTimeoutException -> ErrorType.TIMEOUT
                    is HttpException -> {
                        when (e.code()) {
                            in 400..499 -> ErrorType.CLIENT
                            in 500..599 -> ErrorType.SERVER
                            else -> ErrorType.UNKNOWN
                        }
                    }

                    else -> ErrorType.UNKNOWN
                }
                // Формируем сообщение для пользователя
                val errorMessage = when (errorType) {
                    ErrorType.NETWORK -> "Нет подключения к интернету"
                    ErrorType.TIMEOUT -> "Превышено время ожидания"
                    ErrorType.SERVER -> "Ошибка на сервере. Попробуйте позже"
                    ErrorType.CLIENT -> "Ошибка запроса"
                    ErrorType.UNKNOWN -> "Неизвестная ошибка: ${e.message}"
                }

                _error.value = errorMessage

                // Важно: при ошибке показываем кэшированные данные (если они есть)
                val currentPosts = _data.value?.posts ?: emptyList()
                _data.value = FeedModel(
                    posts = currentPosts,  // Сохраняем старые посты
                    error = true,
                    errorType = errorType,
                    loading = false,
                    empty = currentPosts.isEmpty()  // Пусто только если нет ни кэша, ни новых
                )

                //_error.postValue(e.message ?: "Неизвестная ошибка")
//                _data.value = FeedModel(error = true, errorType = errorType)
            }
        })
    }

    fun likeById(id: Long) {
        // 1. Сначала обновляем UI
        val currentPosts = _data.value?.posts.orEmpty()
        val updatedPosts = currentPosts.map { post ->
            if (post.id == id) {
                val newLikedByMe = !post.likedByMe
                post.copy(
                    likedByMe = newLikedByMe,
                    likeCount = if (newLikedByMe) post.likeCount + 1 else post.likeCount - 1
                )
            } else {
                post
            }
        }
        _data.value = FeedModel(posts = updatedPosts)

        // 2. Отправляем запрос на сервер
        repository.likeByAsync(id, object : PostRepository.PostCallback<Post> {


            override fun onSuccess(result: Post) {
                // Можно по необходимости обновит конкретный пост
                val updatedPost = _data.value?.posts?.map {
                    if (it.id == result.id) result else it
                }
                updatedPost?.let { _data.postValue((FeedModel(posts = it))) }
            }

            override fun onError(e: Throwable) {
                e.printStackTrace()
                _error.postValue(errorHandler(e))
                // В случае ошибки откатываем изменения
                _data.postValue(FeedModel(posts = currentPosts))
                _data.postValue(FeedModel(error = true))
            }

        })
    }

//    fun shareById(id: Long) {
//        thread {
//            try {
//                repository.shareById(id)
//                // Не вызываем loadPost() здесь, чтобы избежать лишних запросов
//                // Просто обновляем счетчик шеринга через локальное обновление
//                val currentPosts = _data.value?.posts.orEmpty()
//                val updatedPosts = currentPosts.map { post ->
//                    if (post.id == id) {
//                        post.copy(shareCount = post.shareCount + 1)
//                    } else post
//                }
//                _data.postValue(FeedModel(posts = updatedPosts))
//            } catch (e: IOException) {
//                e.printStackTrace()
//                _data.postValue(FeedModel(error = true))
//            }
//        }
//    }

    fun removeById(id: Long) {
        repository.removeByAsync(id, object : PostRepository.PostCallback<Unit> {
            override fun onSuccess(result: Unit) {
                // Удаляем пост из текущего списка
                val currentPosts = _data.value?.posts.orEmpty()
                val updatedPosts = currentPosts.filter { it.id != id }
                _data.postValue(FeedModel(posts = updatedPosts, empty = updatedPosts.isEmpty()))
            }

            override fun onError(e: Throwable) {
                e.printStackTrace()
                _error.postValue(errorHandler(e))
                _data.postValue(FeedModel(error = true))

            }
        })
    }

    fun edit(post: Post) {
        _edited.postValue(post)
    }

    fun save(newContent: String) {
        edited.value?.let { post ->
            if (post.content != newContent) {
                repository.saveByAsync(
                    post.copy(content = newContent),
                    object : PostRepository.PostCallback<Post> {
                        override fun onSuccess(result: Post) {
                            // Обновляем пост в списке
                            val currentPosts = _data.value?.posts.orEmpty()
                            val updatedPosts = currentPosts.map {
                                if (it.id == result.id) result else it
                            }
                            _data.postValue(FeedModel(posts = updatedPosts))
                            _edited.postValue(null)
                        }

                        override fun onError(e: Throwable) {
                            e.printStackTrace()
                            _postError.postValue(errorHandler(e))
//                            _error.postValue(errorHandler(e))
//                            _data.postValue(FeedModel(error = true))
                        }

                    })
            }
        }
    }

    fun cancelEdited() {
        _edited.postValue(null)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun createPost(content: String) {
        if (content.isNotBlank()) {
            val newPost = Post(
                id = 0L,
                author = "My Post",
                authorAvatar = "No Name",
                published = System.currentTimeMillis(),
                content = content.trim(),
                likeCount = 0,
                shareCount = 0,
                likedByMe = false,
            )

            repository.saveByAsync(newPost, object : PostRepository.PostCallback<Post> {
                override fun onSuccess(result: Post) {
                    // Добавляем новый пост в начало списка
                    val currentPosts = _data.value?.posts.orEmpty()
                    val updatedPosts = listOf(result) + currentPosts
                    _data.postValue(FeedModel(posts = updatedPosts))
                    _postCreated.postValue(Result.success(Unit))
                }

                override fun onError(e: Throwable) {
                    e.printStackTrace()
                    _postError.postValue(errorHandler(e))
//                    _error.postValue(errorHandler(e))
//                    _data.postValue(FeedModel(error = true))

                }
            })
//
        }
    }

    fun hasVideo(post: Post): Boolean = !post.video.isNullOrBlank()
    fun getVideoUrl(post: Post): String? = post.video?.trim()
}

private fun errorHandler(e: Throwable): String {
    val errorMessage = when (e) {
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
    return errorMessage
}