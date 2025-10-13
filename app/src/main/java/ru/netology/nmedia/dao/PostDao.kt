package ru.netology.nmedia.dao

import ru.netology.nmedia.dto.Post

interface PostDao {

    fun getAll(): List<Post>

    fun save(post: Post): Post

    fun likeById(id: Long)

    fun shareById(id: Long) //поделится

    fun removeById(id: Long) //удаление
}