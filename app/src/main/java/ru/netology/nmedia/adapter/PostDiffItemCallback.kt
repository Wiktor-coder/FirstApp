package ru.netology.nmedia.adapter

import androidx.recyclerview.widget.DiffUtil
import ru.netology.nmedia.dto.Post

class PostDiffItemCallback : DiffUtil.ItemCallback<Post>() {
    override fun areItemsTheSame(
        oldItem: Post,
        newItem: Post
    ): Boolean = oldItem.id == newItem.id

    override fun areContentsTheSame(
        oldItem: Post,
        newItem: Post
    ): Boolean = oldItem == newItem

}