package ru.netology.nmedia.repository

import ru.netology.nmedia.utils.Result
import ru.netology.nmedia.dto.Post

interface PostRepository {
    fun getLocalPosts(): List<Post>
    fun getAllAsync(callback: PostCallback<List<Post>>)
    fun get(): Result<List<Post>> //получить список

    fun likeByAsync(id: Long, callback: PostCallback<Post>)
    fun unlikeByAsync(id: Long, callback: PostCallback<Post>)
    fun likeById(id: Long): Result<Post> //лайк
    fun unlikeById(id: Long): Result<Post>

//    fun shareById(id: Long) //поделится

    fun removeByAsync(id: Long, callback: PostCallback<Unit>)
    fun removeById(id: Long): Result<Unit> //удаление

    fun saveByAsync(post: Post, callback: PostCallback<Post>)
    fun save(post: Post): Result<Post> //редактирование, создоние нового поста

    interface PostCallback<T> {
        fun onSuccess(result: T)
        fun onError(error: Throwable)
    }
}