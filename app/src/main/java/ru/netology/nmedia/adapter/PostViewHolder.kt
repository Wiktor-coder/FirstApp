package ru.netology.nmedia.adapter

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.CardPostBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.utils.formatNumberCompact
import kotlin.time.Instant

class PostViewHolder(
    private val binding: CardPostBinding,

    private val listener: PostListener
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(post: Post) {
        with(binding) {
            content.text = post.content
            author.text = post.author
            published.text = post.published
            Like.text = post.likeCount.formatNumberCompact()
            Share.text = post.shareCount.formatNumberCompact()
//            numberOfShare.text = post.shareCount.formatNumberCompact()

            //при использовании MaterialCheckBox
            Like.isChecked = post.likedByMe
//            Like.setImageResource(
//                if (post.likedByMe) R.drawable.ic_baseline_favorite_24 else
//                    R.drawable.outline_favorite_24
//            )


            Like.setOnClickListener {
                listener.onLike(post)
            }

            Share.setOnClickListener {
                listener.onShare(post)
            }

            menu.setOnClickListener {
                PopupMenu(it.context,it).apply {
                    inflate(R.menu.post_menu)

                    setOnMenuItemClickListener { item ->
                        when(item.itemId) {
                            R.id.remove -> {
//                                removeClickListener(post)
                                listener.onRemove(post)
                                true
                            }
                            R.id.edit -> {
                                listener.onEdit(post)
                                true
                            }
                            else -> false
                        }
                    }
                }.show()
            }
            // обработка видео
            if (!post.video.isNullOrBlank()) {
                videoContainer.isVisible = true

                videoContainer.setOnClickListener {
                    val url = post.video.trim()
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        val pm = it.context.packageManager
                        if (intent.resolveActivity(pm) != null) {
                            it.context.startActivity(intent)
                        } else {
                            Toast.makeText(
                                it.context,
                                R.string.no_app_to_open_video,
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(
                            it.context,
                            R.string.invalid_video_url,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } else {
                videoContainer.isVisible = false
            }
        }
    }
}