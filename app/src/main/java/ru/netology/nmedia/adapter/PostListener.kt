package ru.netology.nmedia.adapter

import ru.netology.nmedia.dto.Post

interface PostListener {
    fun onLike(post: Post) //лайк

    //    fun onShare(post: Post) //поделится
    fun onRemove(post: Post) //удалить
    fun onEdit(post: Post) //редактировать

    fun onPostClick(post: Post)
    fun hasVideo(post: Post): Boolean
    fun getVideoUrl(post: Post): String?

}