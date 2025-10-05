package ru.netology.nmedia.viewmodel

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.map
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.repository.PostRepositorySharedPreferences
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class PostViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PostRepository = PostRepositorySharedPreferences(application)

    // Приватное изменяемое состояние
    private val _edited = MutableLiveData<Post?>()

    // Публичное неизменяемое состояние для UI
    val edited: LiveData<Post?> = _edited

    //состояние: идёт ли редактирование
    // true, если _edited != null
    val isEditing: LiveData<Boolean> = _edited.map { it != null }


    fun get(): LiveData<List<Post>> = repository.get()

    fun likeById(id: Long) = repository.likeById(id)
    fun shareById(id: Long) = repository.shareById(id)
    fun removeById(id: Long) = repository.removeById(id)

    //Запуск редактирования: передаём пост, который хотим редактировать
    fun edit(post: Post) {
        _edited.value = post
    }

    //Сохранение изменений
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

    @RequiresApi(Build.VERSION_CODES.O)
    fun createPost(content: String) {
        if (content.isNotBlank()) {
            val newPost = Post(
                id = 0L,
                author = "My Post",
                published = "${LocalDate.now()} ${LocalTime.now(ZoneId.of("Europe/Moscow")).truncatedTo(ChronoUnit.SECONDS)}",
                content = content.trim(),
                likeCount = 0,
                shareCount = 0,
                likedByMe = false,
            )
            repository.save(newPost)
        }
    }
}