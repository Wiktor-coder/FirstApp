package ru.netology.nmedia.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.netology.nmedia.dto.Post

@Entity
class PostEntity (
    @ColumnInfo // по необходимости можно здесь поменять имена колонок
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val author: String,
    val published: String,
    val content: String,
    val likeCount: Int,
    val shareCount: Int,
    val likedByMe: Boolean = false,
    val video: String? = null,
) {
    fun toPost(): Post =
        Post(
            id = id,
            author = author,
            published = published,
            content = content,
            likeCount = likeCount,
            shareCount = shareCount,
            likedByMe = likedByMe,
            video = video
        )

    companion object{
        fun fromPost(post: Post): PostEntity = with(post) {
            PostEntity(
                id = id,
                author = author,
                published = published,
                content = content,
                likeCount = likeCount,
                shareCount = shareCount,
                likedByMe = likedByMe,
                video = video
            )
        }
    }

}