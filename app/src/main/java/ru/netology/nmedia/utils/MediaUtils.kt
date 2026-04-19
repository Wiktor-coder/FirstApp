package ru.netology.nmedia.utils

import ru.netology.nmedia.BuildConfig
import ru.netology.nmedia.dto.Attachment
import ru.netology.nmedia.dto.AttachmentType

object MediaUtils {

    /**
     * Получение полного URL аватарки
     */
    fun getAvatarUrl(avatarPath: String?): String? {
        return if (!avatarPath.isNullOrBlank()) {
            "${BuildConfig.BASE_URL}/avatars/${avatarPath}"
        } else null
    }

    /**
     * Получение полного URL вложения (изображение, видео, аудио)
     */
    fun getAttachmentUrl(attachment: Attachment?): String? {
        return when (attachment?.type) {
            AttachmentType.IMAGE -> {
                val url = attachment.url
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    url
                } else {
                    "${BuildConfig.BASE_URL}/media/${url}"
                }
            }
            AttachmentType.VIDEO -> {
                if (attachment.url.startsWith("http://") || attachment.url.startsWith("https://")) {
                    attachment.url
                } else {
                    "${BuildConfig.BASE_URL}/video/${attachment.url}"
                }
            }
            AttachmentType.AUDIO -> {
                if (attachment.url.startsWith("http://") || attachment.url.startsWith("https://")) {
                    attachment.url
                } else {
                    "${BuildConfig.BASE_URL}/audio/${attachment.url}"
                }
            }
            null -> null
        }
    }
}