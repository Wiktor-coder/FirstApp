package ru.netology.nmedia.adapter

import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.CardPostBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.repository.PostRepositorySQLiteImpl
import ru.netology.nmedia.utils.formatNumberCompact
import ru.netology.nmedia.utils.toFormattedDate
import com.bumptech.glide.request.target.Target
import ru.netology.nmedia.dto.AttachmentType

class PostViewHolder(
    private val binding: CardPostBinding,

    private val listener: PostListener
) : RecyclerView.ViewHolder(binding.root) {

    // Получаем репозиторий для формирования URL
    private val repository = PostRepositorySQLiteImpl(binding.root.context)

    fun bind(post: Post) {
        with(binding) {
            content.text = post.content
            author.text = post.author
            published.text = post.published.toFormattedDate()
            Like.text = post.likeCount.formatNumberCompact()
            Share.text = post.shareCount.formatNumberCompact()

            // Загрузка аватарки через Glide
            loadAvatar(post.authorAvatar)

            //при использовании MaterialCheckBox
            Like.isChecked = post.likedByMe

            Like.setOnClickListener {
                listener.onLike(post)
            }

//            Share.setOnClickListener {
//                listener.onShare(post)
//            }

            // Меню button
            menu.setOnClickListener {
                showPopupMenu(it, post)
            }

            // Click on post
            root.setOnClickListener {
                listener.onPostClick(post)
            }

        handleAttachments(post)
        }
    }
    private fun handleAttachments(post: Post) {
        with(binding) {
            attachmentContainer.visibility = View.GONE
            videoContainer.visibility = View.GONE

            post.attachment?.let { attachment ->
                Log.d("ATTACHMENT_DEBUG", "=== Attachment Debug ===")
                Log.d("ATTACHMENT_DEBUG", "Post ID: ${post.id}")
                Log.d("ATTACHMENT_DEBUG", "Attachment type: ${attachment.type}")
                Log.d("ATTACHMENT_DEBUG", "Attachment URL: ${attachment.url}")
                Log.d("ATTACHMENT_DEBUG", "Attachment description: ${attachment.description}")

                val attachmentUrl = repository.getAttachmentUrl(attachment)
                Log.d("ATTACHMENT_DEBUG", "Full attachment URL: $attachmentUrl")

                when (attachment.type) {
                    AttachmentType.IMAGE -> {
                        Log.d("ATTACHMENT_DEBUG", "Loading IMAGE from: $attachmentUrl")
                        attachmentContainer.visibility = View.VISIBLE

                        Glide.with(attachmentImage.context)
                            .load(attachmentUrl)
                            .placeholder(R.drawable.downloading_24)
                            .error(R.drawable.info_outline_24)
                            .centerCrop()
                            .timeout(10_000)
                            .listener(object : RequestListener<Drawable> {
                                override fun onLoadFailed(
                                    e: GlideException?,
                                    model: Any?,
                                    target: Target<Drawable>,
                                    isFirstResource: Boolean
                                ): Boolean {
                                    Log.e("ATTACHMENT_DEBUG", "Failed to load image: $attachmentUrl", e)
                                    return false
                                }

                                override fun onResourceReady(
                                    resource: Drawable,
                                    model: Any?,
                                    target: Target<Drawable>,
                                    dataSource: DataSource,
                                    isFirstResource: Boolean
                                ): Boolean {
                                    Log.d("ATTACHMENT_DEBUG", "Successfully loaded image: $attachmentUrl")
                                    return false
                                }
                            })
                            .into(attachmentImage)

                        attachmentContainer.setOnClickListener {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(attachmentUrl))
                            it.context.startActivity(intent)
                        }
                    }

                    AttachmentType.VIDEO -> {
                        Log.d("ATTACHMENT_DEBUG", "Loading VIDEO from: $attachmentUrl")
                        videoContainer.visibility = View.VISIBLE
                        loadVideoThumbnail(attachmentUrl)

                        videoContainer.setOnClickListener {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(attachmentUrl))
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
                                Log.e("ATTACHMENT_DEBUG", "Error opening video", e)
                                Toast.makeText(
                                    it.context,
                                    R.string.invalid_video_url,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }

                    AttachmentType.AUDIO -> {
                        Log.d("ATTACHMENT_DEBUG", "Loading AUDIO from: $attachmentUrl")
                        attachmentContainer.visibility = View.VISIBLE
                        attachmentImage.setImageResource(R.drawable.audio_file_24)
                    }
                }
            } ?: run {
                Log.d("ATTACHMENT_DEBUG", "No attachment for post ${post.id}")
            }
        }
    }

    private fun loadAvatar(avatarPath: String?) {
        val avatarUrl = repository.getAvatarUrl(avatarPath)

        Log.d("Avatar", "Loading from: $avatarUrl")

        Glide.with(binding.avatar.context)
            .load(avatarUrl)
            .placeholder(R.drawable.downloading_24)
            .error(R.drawable.info_outline_24)
            .circleCrop()
            .timeout(10000)
            .skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    Log.e("Glide", "Failed to load: $avatarUrl", e)
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any?,
                    target: Target<Drawable>,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    Log.d("Glide", "Success: $avatarUrl")
                    return false
                }
            })
            .into(binding.avatar)
    }

    private fun loadVideoThumbnail(videoThumbnailUrl: String?) {
        if (!videoThumbnailUrl.isNullOrBlank()) {
            Glide.with(binding.videoContainer.context)
                .load(videoThumbnailUrl)
                .placeholder(R.drawable.downloading_24)
                .error(R.drawable.info_outline_24)
                .centerCrop()
                .into(binding.videoThumbnail)
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