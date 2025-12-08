package ru.netology.nmedia.adapter

import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.CardPostBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.utils.formatNumberCompact

class PostViewHolder(
    private val binding: CardPostBinding,

    private val listener: PostListener
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(post: Post) {
        with(binding) {
            content.text = post.content
            author.text = post.author
            published.text = post.published.toString()
            Like.text = post.likeCount.formatNumberCompact()
            Share.text = post.shareCount.formatNumberCompact()

            //при использовании MaterialCheckBox
            Like.isChecked = post.likedByMe


            Like.setOnClickListener {
                listener.onLike(post)
            }

            Share.setOnClickListener {
                listener.onShare(post)
            }

            menu.setOnClickListener {
                showPopupMenu(it, post)
            }

            root.setOnClickListener {
                listener.onPostClick(post)
            }

            // обработка видео
            val hasVideo = listener.hasVideo(post)
            videoContainer.isVisible = hasVideo

            if (hasVideo) {
                videoContainer.setOnClickListener {
                    val url = listener.getVideoUrl(post)
                    if (url != null) {
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
                }
            }
        }
    }

    private fun showPopupMenu(view: View, post: Post) {
        PopupMenu(view.context, view).apply {
            inflate(R.menu.post_menu)

            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.remove -> {
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
}