package ru.netology.nmedia.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.FragmentFeedBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.viewmodel.PostViewModel
import ru.netology.nmedia.adapter.PostAdapter
import ru.netology.nmedia.adapter.PostListener
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.model.ErrorType
import ru.netology.nmedia.utils.AuthDialog
import ru.netology.nmedia.utils.SignOutDialog
import javax.inject.Inject

@AndroidEntryPoint
@OptIn(ExperimentalCoroutinesApi::class)
class FeedFragment : Fragment() {

    val viewModel: PostViewModel by activityViewModels()

    @Inject
    lateinit var appAuth: AppAuth
    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Проверяем, есть ли данные
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.data.collect { state ->
                Log.d("FeedFragment", "Posts count: ${state.posts.size}")
                state.posts.forEach { post ->
                    Log.d("FeedFragment", "Post: id=${post.id}, content=${post.content.take(30)}")
                }
            }
        }

        setupSwipeRefresh()
        setupRecyclerView()
        observeData()
        observeErrors()
        observeAuthEvents()

        binding.retryButton.setOnClickListener {
            viewModel.loadPosts()
        }

        binding.add.setOnClickListener {
            // Используем метод с проверкой аутентификации
            viewModel.createPostWithAuthCheck("")
        }
//        binding.add.setOnClickListener {
//            findNavController().navigate(R.id.action_feedFragment_to_newPostFragment2)
//        }
    }

    private fun setupSwipeRefresh() {
        binding.swiperefresh.apply {
            setColorSchemeResources(
                R.color.purple_500,
                android.R.color.holo_green_dark,
                android.R.color.holo_orange_dark
            )
            setOnRefreshListener {
                viewModel.loadPosts()
            }
        }
    }

    private fun setupRecyclerView() {
        val adapter = PostAdapter(
            object : PostListener {
                override fun onLike(post: Post) {
                    // Используем метод с проверкой аутентификации
                    viewModel.likeByIdWithAuthCheck(post.id)

//                    viewModel.likeById(post.id)
                }

                override fun onRemove(post: Post) {
                    viewModel.removeById(post.id)
                }

                override fun onEdit(post: Post) {
                    Log.d("FeedFragment", "=== EDIT CLICKED ===")
                    Log.d("FeedFragment", "Post id: ${post.id}")
                    Log.d("FeedFragment", "Post content: ${post.content}")

                    try {
                        val bundle = Bundle().apply {
                            putLong("postId", post.id)
                        }
                        Log.d("FeedFragment", "Navigating to editPostFragment with bundle: $bundle")
                        findNavController().navigate(R.id.editPostFragment, bundle)
                        Log.d("FeedFragment", "Navigation call completed")
                    } catch (e: Exception) {
                        Log.e("FeedFragment", "Navigation failed", e)
                    }
                }

                override fun onPostClick(post: Post) {
                    val bundle = Bundle().apply {
                        putLong("postId", post.id)
                    }
                    findNavController().navigate(R.id.singlePostFragment, bundle)
                }

                override fun onImageClick(imageUrl: String) {
                    // Используем прямой Bundle вместо Safe Args
                    val bundle = Bundle().apply {
                        putString("imageUrl", imageUrl)
                    }
                    findNavController().navigate(R.id.fullScreenImageFragment, bundle)
                }

                override fun hasVideo(post: Post): Boolean {
                    return viewModel.hasVideo(post)
                }

                override fun getVideoUrl(post: Post): String? {
                    return viewModel.getVideoUrl(post)
                }

            }, appAuth
        )

        binding.container.adapter = adapter
    }

    // Наблюдение за событиями аутентификации
    private fun observeAuthEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.authRequired.collect {
                // Показываем диалог авторизации
                AuthDialog().show(parentFragmentManager, AuthDialog.TAG)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.signOutRequested.collect {
                // Показываем диалог подтверждения выхода
                SignOutDialog().show(parentFragmentManager, SignOutDialog.TAG)
            }
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.data.collect { state ->
                val adapter = binding.container.adapter as? PostAdapter
                adapter?.submitList(state.posts)
                updateUI(state)
            }
        }
    }

    private fun updateUI(state: FeedModel) {
        with(binding) {
            progress.isVisible = state.loading && state.posts.isEmpty()
            errorGroup.isVisible = state.error
            emptyText.isVisible = state.empty && !state.loading

            // Показываем текст ошибки если есть
            if (state.error) {
                retryTitle.text = when (state.errorType) {
                    ErrorType.NETWORK -> "Нет подключения к интернету"
                    ErrorType.TIMEOUT -> "Превышено время ожидания"
                    ErrorType.SERVER -> "Ошибка на сервере"
                    ErrorType.CLIENT -> "Ошибка запроса"
                    ErrorType.UNKNOWN -> "Неизвестная ошибка"
                }
            }

            if (!state.loading) {
                swiperefresh.isRefreshing = false
            }
        }
    }

    private fun observeErrors() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.postError.collect { errorMessage ->
                if (isVisible) {
                    showErrorSnackbar(errorMessage)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collect { errorMessage ->
                if (isVisible && errorMessage.isNotBlank()) {
                    showErrorSnackbar(errorMessage)
                }
            }
        }
    }

    private fun showErrorSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAnchorView(binding.add)
            .setAction("OK") { }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}