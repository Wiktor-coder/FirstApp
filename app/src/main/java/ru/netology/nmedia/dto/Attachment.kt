package ru.netology.nmedia.dto

data class Attachment(
    val url: String,
    val type: AttachmentType,
    val description: String?,
)

enum class AttachmentType{
    IMAGE, VIDEO, AUDIO
}