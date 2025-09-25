package ru.netology.nmedia.repository

import androidx.lifecycle.LiveData
import ru.netology.nmedia.dto.Post

interface PostRepository {
    fun get(): LiveData<List<Post>> //получить список

    fun likeById(id: Long) //лайк

    fun shareById(id: Long) //поделится

    fun removeById(id: Long) //удаление

    fun save(post: Post) //редактирование, создоние нового поста
}