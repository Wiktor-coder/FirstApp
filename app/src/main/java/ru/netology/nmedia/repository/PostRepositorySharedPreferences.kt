package ru.netology.nmedia.repository

import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ru.netology.nmedia.dto.Post
import java.lang.reflect.Type

class PostRepositorySharedPreferences(context: Context) : PostRepository {

    private val preferences = context.getSharedPreferences("posts", Context.MODE_PRIVATE)


    //сохранять всё в deaultPost через data.value сохранение не работает, только обновляем
    private var defaultPosts = readPosts()
        set(value) {
            field = value
            sync()
        }
    //находим максимальный id постов и увеличиваем его
    private var nextId = defaultPosts.maxByOrNull { it.id }?.id?.plus(1) ?: 1L
    private val data = MutableLiveData(defaultPosts)

    override fun get(): LiveData<List<Post>> = data

    //лайк
    override fun likeById(id: Long) {
        defaultPosts = defaultPosts.map { post ->
            if (post.id == id) {
                post.copy(
                    likedByMe = !post.likedByMe,
                    likeCount = if (post.likedByMe) post.likeCount - 1
                    else post.likeCount + 1
                )
            } else {
                post
            }

        }
        data.value = defaultPosts
    }

    //поделится
    override fun shareById(id: Long) {
        defaultPosts = defaultPosts.map { post ->
            if (post.id == id) {
                post.copy(shareCount = post.shareCount + 1)
            } else {
                post
            }
        }
        data.value = defaultPosts
    }

    //удаление
    override fun removeById(id: Long) {
        defaultPosts = defaultPosts.filter { it.id != id }
        data.value = defaultPosts
            //data.value?.filter { it.id != id }  //.filterNot { it.id == id } как альтернатива

    }

    //создание новго поста, else - редактирование поста
    override fun save(post: Post) {
        if (post.id == 0L) {
            val newPost = post.copy(id = nextId++)
            defaultPosts = listOf(newPost) + defaultPosts
            data.value = defaultPosts //listOf(post.copy(id = nextId++)) + data.value.orEmpty()
        } else {
            defaultPosts = defaultPosts.map {
                if (it.id == post.id) {
                    post
                } else {
                    it
                }
            }
            data.value = defaultPosts
        }
    }

    //чтение преобразование из строчки в список обьектов
    private fun readPosts(): List<Post> {
        val serialized = preferences.getString(POST_KEY, null)
        return if (serialized != null) {
            gson.fromJson(serialized, postsType)
        } else {
            emptyList()
        }
    }

    //записываем наши посты в gson
    private fun sync() {
        preferences.edit {
            putString(POST_KEY, gson.toJson(defaultPosts))
        }
    }
    companion object {
        const val POST_KEY = "POST_KEY"
        val gson = Gson()
        val postsType: Type = TypeToken.getParameterized(List::class.java, Post::class.java).type
    }
}