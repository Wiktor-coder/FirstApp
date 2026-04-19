package ru.netology.nmedia.fragment

import android.content.Intent
import android.graphics.drawable.Drawable
import android.widget.PopupMenu
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import kotlinx.coroutines.launch
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.FragmentSinglePostBinding
import ru.netology.nmedia.dto.AttachmentType
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.utils.toFormattedDate
import ru.netology.nmedia.viewmodel.PostViewModel
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.dto.Attachment
import ru.netology.nmedia.utils.MediaUtils.getAttachmentUrl
import ru.netology.nmedia.utils.MediaUtils.getAvatarUrl
import javax.inject.Inject
@AndroidEntryPoint
class SinglePostFragment : Fragment() {

    private var _binding: FragmentSinglePostBinding? = null
    private val binding get() = _binding!!
//    private val repository: PostRepositorySQLiteImpl by lazy {
//        PostRepositorySQLiteImpl(requireContext())
//    }
    private val viewModel by activityViewModels<PostViewModel>()
    @Inject
    lateinit var appAuth: AppAuth

    // ДОБАВЛЯЕМ onCreateView - это обязательный метод!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("SinglePostFragment", "onCreateView")
        _binding = FragmentSinglePostBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("SinglePostFragment", "onViewCreated")

        val postId = arguments?.getLong("postId")
        Log.d("SinglePostFragment", "Received postId: $postId")

        if (postId == null) {
            Log.e("SinglePostFragment", "postId is null")
            findNavController().navigateUp()
            return
        }

        // Добавляем debug TextView для отладки
        binding.debugInfo.visibility = View.VISIBLE
        binding.debugInfo.text = "Post ID: $postId\nLoading..."

        // Проверяем, есть ли данные
        if (viewModel.data.value.posts.isEmpty()) {
            Log.d("SinglePostFragment", "No posts, loading...")
            viewModel.loadPosts()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.data.collect { feedModel ->
                Log.d("SinglePostFragment", "Data collected: ${feedModel.posts.size} posts")

                val post = feedModel.posts.find { it.id == postId }
                if (post != null) {
                    Log.d("SinglePostFragment", "Found post: ${post.content.take(50)}")
                    bind(post)
                    binding.debugInfo.visibility = View.GONE // Скрываем debug после загрузки
                } else if (!feedModel.loading && feedModel.posts.isNotEmpty()) {
                    Log.w("SinglePostFragment", "Post not found")
                    binding.debugInfo.text = "Post not found!\nAvailable: ${feedModel.posts.map { it.id }}"
                }
            }
        }
    }

    private fun bind(post: Post) {
        with(binding) {
            author.text = post.author
            published.text = post.published.toFormattedDate()
            content.text = post.content
            Like.text = post.likeCount.toString()
            Like.isChecked = post.likedByMe
            Share.text = post.shareCount.toString()
            loadAvatar(post.authorAvatar)
            handleAttachments(post)

            Like.setOnClickListener {
                viewModel.likeById(post.id)
            }

            menu.setOnClickListener {
                showMenu(post)
            }
        }
    }

    private fun loadAvatar(avatarPath: String?) {
        try {
            val avatarUrl = getAvatarUrl(avatarPath)

            Glide.with(binding.avatar.context)
                .load(avatarUrl)
                .placeholder(R.drawable.downloading_24)
                .error(R.drawable.info_outline_24)
                .circleCrop()
                .timeout(10_000)
                .into(binding.avatar)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showMenu(post: Post) {
        // Проверка аутентификации через appAuth
        val isAuthenticated = appAuth.authStateFlow.value.id != 0L
        val canEdit = isAuthenticated && post.ownedByMe
        val canRemove = isAuthenticated && post.ownedByMe

        if (!canEdit && !canRemove) return

        PopupMenu(requireContext(), binding.menu).apply {
            inflate(R.menu.post_menu)
            menu.findItem(R.id.edit).isVisible = canEdit
            menu.findItem(R.id.remove).isVisible = canRemove

            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.edit -> {
                        val bundle = Bundle().apply {
                            putLong("postId", post.id)
                        }
                        findNavController().navigate(R.id.editPostFragment, bundle)
                        true
                    }
                    R.id.remove -> {
                        viewModel.removeById(post.id)
                        findNavController().navigateUp()
                        true
                    }
                    else -> false
                }
            }
            show()
        }
    }

    private fun handleAttachments(post: Post) {
        with(binding) {
            attachmentContainer.visibility = View.GONE
            videoContainer.visibility = View.GONE

            post.attachment?.let { attachment ->
                val attachmentUrl = getAttachmentUrl(attachment)
                val uri = attachmentUrl?.toUri()

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
                                    Log.e("SinglePostFragment", "Glide load failed", e)
                                    return false
                                }

                                override fun onResourceReady(
                                    resource: Drawable,
                                    model: Any?,
                                    target: Target<Drawable>,
                                    dataSource: DataSource,
                                    isFirstResource: Boolean
                                ): Boolean {
                                    Log.d("SinglePostFragment", "Image loaded successfully")
                                    return false
                                }
                            })
                            .into(attachmentImage)

                        attachmentContainer.setOnClickListener {
                            attachmentUrl?.let { url ->
                                val bundle = Bundle().apply {
                                    putString("imageUrl", url)
                                }
                                findNavController().navigate(
                                    R.id.fullScreenImageFragment,
                                    bundle
                                )
                            }
                        }
                    }

                    AttachmentType.VIDEO -> {
                        // ... существующий код ...
                        videoContainer.setOnClickListener {
                            try {
                                uri?.let {
                                    val intent = Intent(Intent.ACTION_VIEW, it)
                                    val pm = requireContext().packageManager
                                    if (intent.resolveActivity(pm) != null) {
                                        startActivity(intent)
                                    } else {
                                        Toast.makeText(
                                            requireContext(),
                                            R.string.no_app_to_open_video,
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            } catch (e: Exception) {
                                Toast.makeText(
                                    requireContext(),
                                    R.string.invalid_video_url,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }

                    // ... остальные типы ...
                    AttachmentType.AUDIO -> TODO()
                }
            }
        }
    }
}