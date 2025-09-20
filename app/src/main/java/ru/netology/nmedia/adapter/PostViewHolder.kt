package ru.netology.nmedia.adapter

import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.CardPostBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.viewmodel.formatNumberCompact

class PostViewHolder(
    private val binding: CardPostBinding,
    private val likeClickListener: likeClickListener,
    private val shareClickListener: shareClickListener,
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(post: Post) {
        with(binding) {
            content.text = post.content
            author.text = post.author
            published.text = post.published
            numberOfLikes.text = post.likeCount.formatNumberCompact()
            numberOfShare.text = post.shareCount.formatNumberCompact()

            Like.setImageResource(
                if (post.likedByMe) R.drawable.ic_baseline_favorite_24 else
                    R.drawable.outline_favorite_24
            )

//            numberOfLikes.isVisible = post.likeCount > 0 //если ноль то не будет отображаться
//            numberOfShare.isVisible = post.shareCount > 0

            Like.setOnClickListener {
                likeClickListener(post) //viewModel.likeById(post.id)
                numberOfLikes.text =
                    post.likeCount.formatNumberCompact() //viewModel.get().value?.likeCount?.formatNumberCompact()
            }

            Share.setOnClickListener {
                shareClickListener(post) //viewModel.shareById(post.id)
                numberOfShare.text =
                    post.shareCount.formatNumberCompact() //viewModel.get().value?.shareCount?.formatNumberCompact()
            }
        }
    }
}