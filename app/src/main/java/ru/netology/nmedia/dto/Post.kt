package ru.netology.nmedia.dto

data class Post(
    val id: Long,
    val author: String,
    val published: String,
    val content: String,
    val likeCount: Int,
    val shareCount: Int,
    val likedByMe: Boolean = false,
    val video: String? = null,
)