package ru.netology.nmedia.viewmodel

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.repository.PostRepositorySQLiteImpl
import ru.netology.nmedia.utils.SingleLiveEvent

class PostViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDb.getInstance(application).postDao
    private val repository: PostRepository = PostRepositorySQLiteImpl(application.applicationContext, dao)

    private val _data = MutableLiveData<FeedModel>(FeedModel())
    val data: LiveData<FeedModel> = _data

    private val _edited = MutableLiveData<Post?>()
    val edited: LiveData<Post?> = _edited
    val isEditing: LiveData<Boolean> = _edited.map { it != null }

    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit> = _postCreated

    init {
        loadPost()
    }

    fun loadPost(useCache: Boolean = true) {
        _data.postValue(FeedModel(loading = true))

        if (useCache) {
            // Показываем кэшированные данные сразу
            val cachedPosts = repository.getLocalPosts()
            if (cachedPosts.isNotEmpty()) {
                _data.postValue(FeedModel(posts = cachedPosts))
            }
        }

        repository.getAllAsync(object : PostRepository.PostCallback<List<Post>> {
            override fun onSuccess(result: List<Post>) {
                _data.postValue(FeedModel(posts = result, empty = result.isEmpty()))
            }

            override fun onError(e: Exception) {
                e.printStackTrace()
                _data.postValue(FeedModel(error = true))
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

            override fun onError(e: Exception) {
                e.printStackTrace()
                // В случае ошибки откатываем изменения
                _data.postValue(FeedModel(posts = currentPosts))
                _data.postValue(FeedModel(error = true))
            }
        })
//        thread {
//            try {
//                repository.likeById(id)
//                // Можно ничего не делать, если сервер ответил успешно
//            } catch (e: IOException) {
//                e.printStackTrace()
//                // В случае ошибки - откатываем изменения
//                _data.postValue(FeedModel(posts = currentPosts))
//                _data.postValue(FeedModel(error = true))
//            }
//        }
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

            override fun onError(e: Exception) {
                e.printStackTrace()
                _data.postValue(FeedModel(error = true))

            }
        })
//        thread {
//            try {
//                repository.removeById(id)
//                // Удаляем пост из текущего списка
//                val currentPosts = _data.value?.posts.orEmpty()
//                val updatedPosts = currentPosts.filter { it.id != id }
//                _data.postValue(FeedModel(posts = updatedPosts, empty = updatedPosts.isEmpty()))
//            } catch (e: IOException) {
//                e.printStackTrace()
//                _data.postValue(FeedModel(error = true))
//            }
//        }
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

                        override fun onError(e: Exception) {
                            e.printStackTrace()
                            _data.postValue(FeedModel(error = true))
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
                    _postCreated.postValue(Unit)
                }

                override fun onError(e: Exception) {
                    e.printStackTrace()
                    _data.postValue(FeedModel(error = true))
                }
            })
//            thread {
//                try {
//                    val savedPost = repository.save(newPost)
//                    // Добавляем новый пост в начало списка
//                    val currentPosts = _data.value?.posts.orEmpty()
//                    val updatedPosts = listOf(savedPost) + currentPosts
//                    _data.postValue(FeedModel(posts = updatedPosts))
//                    _postCreated.postValue(Unit)
//                } catch (e: IOException) {
//                    e.printStackTrace()
//                    _data.postValue(FeedModel(error = true))
//                }
//            }
        }
    }

    fun hasVideo(post: Post): Boolean = !post.video.isNullOrBlank()
    fun getVideoUrl(post: Post): String? = post.video?.trim()
}