package ru.netology.nmedia.viewmodel

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

class PostViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PostRepository = PostRepositorySQLiteImpl()
//    private val repository = PostRepositorySQLiteImpl.newInstance(application)

    private val _data = MutableLiveData(FeedModel())

    val data: LiveData<FeedModel>
        get() = _data

    // Приватное изменяемое состояние
    private val _edited = MutableLiveData<Post?>()

    // Публичное неизменяемое состояние для UI
    val edited: LiveData<Post?> = _edited

    //состояние: идёт ли редактирование
    // true, если _edited != null
    val isEditing: LiveData<Boolean> = _edited.map { it != null }

    private val _postCreated = SingleLiveEvent<Unit>()

    val postCreated: LiveData<Unit>
        get() = _postCreated

    init {
        loadPost()
    }

    fun loadPost() {
        thread {
            //начинаем загрузку
            _data.postValue(FeedModel(loading = true))
            try {
                //данные успешно получены

                val posts = repository.get()
                FeedModel(posts = posts, empty = posts.isEmpty())
            } catch (e: IOException) {
                // получена ошибка
                FeedModel(error = true)
            }.also(_data::postValue)
        }
    }


    fun get(): List<Post> = repository.get()

    fun likeById(id: Long) = {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.likeById(id)
                // обновляем данные
                loadPost()
            } catch (e: IOException) {
                FeedModel(error = true)
            }.also { _data::postValue }
        }
    }
    fun shareById(id: Long) = repository.shareById(id)
    fun removeById(id: Long) = repository.removeById(id)

    //Запуск редактирования: передаём пост, который хотим редактировать
    fun edit(post: Post) {
        _edited.value = post
    }

    //Сохранение изменений для редактирования
    fun save(newContent: String) {
        edited.value?.let { post ->
            if (post.content != newContent) {
                repository.save(post.copy(content = newContent))
            }
        }
        //Завершаем редактирование
        cancelEdited()
    }

    //Отмена редактирования
    fun cancelEdited() {
        _edited.value = null
    }

    //новый пост
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
            repository.save(newPost)
            loadPost()
            _postCreated.postValue(Unit)
        }
    }

    //проверка наличия видео
    fun hasVideo(post: Post): Boolean = !post.video.isNullOrBlank()

    //возвращаем URL или null
    fun getVideoUrl(post: Post): String? = post.video?.trim()


}