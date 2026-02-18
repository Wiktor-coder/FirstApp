package ru.netology.nmedia.dto

import com.google.gson.annotations.SerializedName

data class Post(
    val id: Long,
    val author: String,
    val authorAvatar: String?,
    val published: Long,
    val content: String,
    @SerializedName("likes") val likeCount: Int,
    val shareCount: Int,
    val likedByMe: Boolean = false,
    val video: String? = null,
    val attachment: Attachment? = null
)