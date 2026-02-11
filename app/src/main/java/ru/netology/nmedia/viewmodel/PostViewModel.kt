package ru.netology.nmedia.viewmodel

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.*
import okhttp3.Dispatcher
import okio.IOException
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.repository.PostRepositorySQLiteImpl
import ru.netology.nmedia.utils.SingleLiveEvent
import java.time.LocalDate
import kotlin.concurrent.thread
import kotlin.time.Clock

class PostViewModel : ViewModel() {
    private val repository: PostRepository = PostRepositorySQLiteImpl()

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

    fun loadPost() {
        // Просто создаем и запускаем новый поток каждый раз
        Thread {
            try {
                _data.postValue(FeedModel(loading = true))
                val posts = repository.get()
                _data.postValue(FeedModel(posts = posts, empty = posts.isEmpty()))
            } catch (e: IOException) {
                e.printStackTrace()
                _data.postValue(FeedModel(error = true))
            } catch (e: Exception) {
                e.printStackTrace()
                _data.postValue(FeedModel(error = true))
            }
        }.start()
    }

    // Удалите этот метод полностью - он вызывает NetworkOnMainThreadException!
    // fun get(): List<Post> = repository.get()

    fun likeById(id: Long) {
        Thread {
            try {
                val likedPost = repository.likeById(id)
                val currentPosts = _data.value?.posts.orEmpty()
                val updatedPosts = currentPosts.map {
                    if (it.id == id) likedPost else it
                }
                _data.postValue(FeedModel(posts = updatedPosts))
            } catch (e: IOException) {
                e.printStackTrace()
                _data.postValue(FeedModel(error = true))
            }
        }.start()
    }

    fun shareById(id: Long) {
        Thread {
            try {
                repository.shareById(id)
                // Не вызываем loadPost() здесь, чтобы избежать лишних запросов
                // Просто обновляем счетчик шеринга через локальное обновление
                val currentPosts = _data.value?.posts.orEmpty()
                val updatedPosts = currentPosts.map { post ->
                    if (post.id == id) {
                        post.copy(shareCount = post.shareCount + 1)
                    } else post
                }
                _data.postValue(FeedModel(posts = updatedPosts))
            } catch (e: IOException) {
                e.printStackTrace()
                _data.postValue(FeedModel(error = true))
            }
        }.start()
    }

    fun removeById(id: Long) {
        Thread {
            try {
                repository.removeById(id)
                // Удаляем пост из текущего списка
                val currentPosts = _data.value?.posts.orEmpty()
                val updatedPosts = currentPosts.filter { it.id != id }
                _data.postValue(FeedModel(posts = updatedPosts, empty = updatedPosts.isEmpty()))
            } catch (e: IOException) {
                e.printStackTrace()
                _data.postValue(FeedModel(error = true))
            }
        }.start()
    }

    fun edit(post: Post) {
        _edited.postValue(post)
    }

    fun save(newContent: String) {
        edited.value?.let { post ->
            if (post.content != newContent) {
                Thread {
                    try {
                        val savedPost = repository.save(post.copy(content = newContent))
                        // Обновляем пост в списке
                        val currentPosts = _data.value?.posts.orEmpty()
                        val updatedPosts = currentPosts.map {
                            if (it.id == savedPost.id) savedPost else it
                        }
                        _data.postValue(FeedModel(posts = updatedPosts))
                        _edited.postValue(null)
                    } catch (e: IOException) {
                        e.printStackTrace()
                        _data.postValue(FeedModel(error = true))
                    }
                }.start()
            } else {
                cancelEdited()
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
                published = System.currentTimeMillis(),
                content = content.trim(),
                likeCount = 0,
                shareCount = 0,
                likedByMe = false,
            )
            Thread {
                try {
                    val savedPost = repository.save(newPost)
                    // Добавляем новый пост в начало списка
                    val currentPosts = _data.value?.posts.orEmpty()
                    val updatedPosts = listOf(savedPost) + currentPosts
                    _data.postValue(FeedModel(posts = updatedPosts))
                    _postCreated.postValue(Unit)
                } catch (e: IOException) {
                    e.printStackTrace()
                    _data.postValue(FeedModel(error = true))
                }
            }.start()
        }
    }

    fun hasVideo(post: Post): Boolean = !post.video.isNullOrBlank()
    fun getVideoUrl(post: Post): String? = post.video?.trim()
}