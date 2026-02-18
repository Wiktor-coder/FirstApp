package ru.netology.nmedia.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.netology.nmedia.dto.Attachment
import ru.netology.nmedia.dto.AttachmentType
import ru.netology.nmedia.dto.Post

@Entity(tableName = "PostEntity")
data class PostEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "author")
    val author: String,

    @ColumnInfo(name = "authorAvatar")
    val authorAvatar: String?,

    @ColumnInfo(name = "published")
    val published: Long,

    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "likeCount")
    val likeCount: Int,

    @ColumnInfo(name = "shareCount")
    val shareCount: Int,

    @ColumnInfo(name = "likedByMe")
    val likedByMe: Boolean = false,

    @ColumnInfo(name = "video")
    val video: String? = null,

    @ColumnInfo(name = "attachmentUrl")
    val attachmentUrl: String? = null,

    @ColumnInfo(name = "attachmentType")
    val attachmentType: String? = null,

    @ColumnInfo(name = "attachmentDescription")
    val attachmentDescription: String? = null
) {
    fun toPost(): Post = Post(
        id = id,
        author = author,
        authorAvatar = authorAvatar,
        published = published,
        content = content,
        likeCount = likeCount,
        shareCount = shareCount,
        likedByMe = likedByMe,
        video = video,
        attachment = createAttachment()
    )

    private fun createAttachment(): Attachment? {
        if (attachmentUrl == null || attachmentType == null) return null

        val type = when (attachmentType) {
            "IMAGE" -> AttachmentType.IMAGE
            "VIDEO" -> AttachmentType.VIDEO
            "AUDIO" -> AttachmentType.AUDIO
            else -> return null
        }

        return Attachment(
            url = attachmentUrl,
            type = type,
            description = attachmentDescription
        )
    }

    companion object {
        fun fromPost(post: Post): PostEntity = with(post) {
            PostEntity(
                id = id,
                author = author,
                authorAvatar = authorAvatar,
                published = published,
                content = content,
                likeCount = likeCount,
                shareCount = shareCount,
                likedByMe = likedByMe,
                video = video,
                attachmentUrl = attachment?.url,
                attachmentType = attachment?.type?.name,
                attachmentDescription = attachment?.description
            )
        }
    }
}