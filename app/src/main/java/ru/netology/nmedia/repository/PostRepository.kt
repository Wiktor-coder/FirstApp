package ru.netology.nmedia.repository

import androidx.lifecycle.LiveData
import ru.netology.nmedia.adapter.PostDiffItemCallback
import ru.netology.nmedia.dto.Post

interface PostRepository {
    fun getLocalPosts(): List<Post>
    fun getAllAsync(callback: PostCallback<List<Post>>)
    fun get(): List<Post> //получить список

    fun likeByAsync(id: Long, callback: PostCallback<Post>)
    fun likeById(id: Long): Post? //лайк

//    fun shareById(id: Long) //поделится

    fun removeByAsync(id: Long, callback: PostCallback<Unit>)
    fun removeById(id: Long) //удаление

    fun saveByAsync(post: Post, callback: PostCallback<Post>)
    fun save(post: Post): Post? //редактирование, создоние нового поста

    interface PostCallback<T> {
        fun onSuccess(result: T)
        fun onError(e: Throwable)
    }
}