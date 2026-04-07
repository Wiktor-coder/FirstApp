package ru.netology.nmedia.adapter

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.net.toUri
import androidx.navigation.Navigation.findNavController
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
import ru.netology.nmedia.auth.AppAuth
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

            // Управляем видимостью кнопки меню
            val isAuthenticated = AppAuth.getInstance().authStateFlow.value.id != 0L
            val showMenu = isAuthenticated && post.ownedByMe

            menu.visibility = if (showMenu) View.VISIBLE else View.GONE

            // Если меню видимо, устанавливаем слушатель
            if (showMenu) {
                menu.setOnClickListener {
                    showPopupMenu(it, post)
                }
            } else {
                menu.setOnClickListener(null) // Убираем слушатель, если меню скрыто
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
                val attachmentUrl = repository.getAttachmentUrl(attachment)

                when (attachment.type) {
                    AttachmentType.IMAGE -> {
                        attachmentContainer.visibility = View.VISIBLE

                        Glide.with(attachmentImage.context)
                            .load(attachmentUrl)
                            .placeholder(R.drawable.downloading_24)
                            .error(R.drawable.info_outline_24)
                            .timeout(30000)
                            .listener(object : RequestListener<Drawable> {
                                override fun onLoadFailed(
                                    e: GlideException?,
                                    model: Any?,
                                    target: Target<Drawable>,
                                    isFirstResource: Boolean
                                ): Boolean {
                                    Log.e("ATTACHMENT_DEBUG", "Glide load failed", e)
                                    return false
                                }

                                override fun onResourceReady(
                                    resource:Drawable,
                                    model: Any?,
                                    target: Target<Drawable>,
                                    dataSource: DataSource,
                                    isFirstResource: Boolean
                                ): Boolean {
                                    Log.d("ATTACHMENT_DEBUG", "Image loaded successfully")
                                    return false
                                }
                            })
                            .into(attachmentImage)

                        attachmentContainer.setOnClickListener { view ->
                            attachmentUrl?.let { url ->
                                listener.onImageClick(url)
                            }
                        }
                    }

                    AttachmentType.VIDEO -> {
                        Log.d("ATTACHMENT_DEBUG", "Loading VIDEO from: $attachmentUrl")
                        videoContainer.visibility = View.VISIBLE
                        loadVideoThumbnail(attachmentUrl)

                        videoContainer.setOnClickListener { view ->
                            try {
                                attachmentUrl?.let { url ->
                                    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                                    val packageManager = view.context.packageManager
                                    if (intent.resolveActivity(packageManager) != null) {
                                        view.context.startActivity(intent)
                                    } else {
                                        Toast.makeText(
                                            view.context,
                                            R.string.no_app_to_open_video,
                                            Toast.LENGTH_LONG,
                                        ).show()
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("ATTACHMENT_DEBUG", "Error opening video", e)
                                Toast.makeText(
                                    view.context,
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
        // Проверяем, может ли пользователь редактировать/удалять этот пост
        val canEdit = listener.canEdit(post)
        val canRemove = listener.canRemove(post)

        // Если нельзя ни редактировать, ни удалять - не показываем меню
        if (!canEdit && !canRemove) return

        PopupMenu(view.context, view).apply {
            inflate(R.menu.post_menu)

            // Скрываем пункты меню, если недоступны
            menu.findItem(R.id.edit).isVisible = canEdit
            menu.findItem(R.id.remove).isVisible = canRemove

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