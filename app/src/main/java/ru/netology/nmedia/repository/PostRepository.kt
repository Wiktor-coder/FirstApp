package ru.netology.nmedia.repository

import androidx.lifecycle.LiveData
import ru.netology.nmedia.adapter.PostDiffItemCallback
import ru.netology.nmedia.dto.Post

interface PostRepository {
    fun get(): List<Post> //получить список

    fun likeById(id: Long): Post //лайк

//    fun shareById(id: Long) //поделится

    fun removeById(id: Long) //удаление

    fun save(post: Post): Post //редактирование, создоние нового поста

    fun getAllAsync(callback: PostCallback<List<Post>>)
    fun likeByAsync(id: Long, callback: PostCallback<Post>)
    fun removeByAsync(id: Long, callback: PostCallback<Unit>)
    fun saveByAsync(post: Post, callback: PostCallback<Post>)
    fun getLocalPosts(): List<Post>
    interface PostCallback<T> {
        fun onSuccess(result: T)
        fun onError(e: Exception)
    }
}