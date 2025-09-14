package ru.netology.nmedia

data class Post(
    val id: Long,
    val author: String,
    val published: String,
    val content: String,
    var likeCount: Int,
    var shareCount: Int,
    var likedByMe: Boolean = false,
    )