package ru.netology.nmedia.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import ru.netology.nmedia.R
import ru.netology.nmedia.viewmodel.PostViewModel
import ru.netology.nmedia.adapter.PostAdapter
import ru.netology.nmedia.adapter.PostListener
import ru.netology.nmedia.databinding.FragmentFeedBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.ErrorType
import ru.netology.nmedia.model.ErrorType.*


class FeedFragment : Fragment() {

    private val viewModel: PostViewModel by activityViewModels()
    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    private fun showErrorSnackbar(message: String, action: (() -> Unit)? = null) {
        val snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_INDEFINITE)

        // Устанавливаем якорь на кнопку добавления поста
        snackbar.setAnchorView(binding.add)

        if (action != null) {
            snackbar.setAction("Повторить") { action() }
                .setActionTextColor(resources.getColor(R.color.purple_500, null))
        } else {
            snackbar.setAction("OK") { }
        }

        snackbar.show()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- НАСТРОЙКА SWIPE TO REFRESH ---
        binding.swiperefresh.apply {
            // Устанавливаем цвета индикатора
            setColorSchemeResources(
                R.color.purple_500,
                android.R.color.holo_green_dark,
                android.R.color.holo_orange_dark
            )

            setOnRefreshListener {
                viewModel.loadPost()
            }
        }
        // Настройка RecyclerView
        val adapter = PostAdapter(
            object : PostListener {
                override fun onLike(post: Post) {
                    viewModel.likeById(post.id)
                }

                //поделится
//                override fun onShare(post: Post) {
//                    viewModel.shareById(post.id)
//                    val intent = Intent().apply {
//                        action = Intent.ACTION_SEND
//                        type = "text/plain"
//                        putExtra(Intent.EXTRA_TEXT, post.content)
//                    }
//                    val chooser =
//                        Intent.createChooser(intent, getString(R.string.chooser_share_post))
//                    startActivity(chooser)
//                }

                override fun onRemove(post: Post) {
                    viewModel.removeById(post.id)
                }

                override fun onEdit(post: Post) {
                    val bundle = bundleOf("postId" to post.id)
                    findNavController().navigate(R.id.editPostFragment, bundle)
//                    viewModel.edit(post)
//                    editPostLauncher.launch(post.content)
                }

                override fun onPostClick(post: Post) {
                    val bundle = bundleOf("postId" to post.id)
                    findNavController().navigate(R.id.singlePostFragment, bundle)
                }

                override fun hasVideo(post: Post): Boolean {
                    return viewModel.hasVideo(post)
                }

                override fun getVideoUrl(post: Post): String? {
                    return viewModel.getVideoUrl(post)
                }
            }
        )
        binding.container.adapter = adapter

        // Наблюдение за состоянием
        viewModel.data.observe(viewLifecycleOwner) { state ->
            adapter.submitList(state.posts)
            // Управление видимостью элементов
            binding.progress.isVisible = state.loading && state.posts.isEmpty()
            binding.errorGroup.isVisible = state.error
            binding.emptyText.isVisible = state.empty && !state.loading

            if (state.error && state.posts.isEmpty()) {
                binding.errorGroup.isVisible = true
            } else {
                binding.errorGroup.isVisible = false
            }

            if (state.error) {
                val errorMessage = when (state.errorType) {
                    NETWORK -> "Нет подключения к интернету"
                    TIMEOUT -> "Превышено время ожидания"
                    SERVER -> "Ошибка на сервере"
                    CLIENT -> "Ошибка запроса"
                    UNKNOWN -> "Неизвестная ошибка"
                }
                showErrorSnackbar(errorMessage) {
                    viewModel.loadPost()
                } // Теперь передаем String
            }

            //ВАЖНО: скрываем индикатор SwipeRefreshLayout, когда загрузка закончена
            if (!state.loading) {
                binding.swiperefresh.isRefreshing = false
//                binding.swiperefresh.post {
//                    binding.swiperefresh.isRefreshing = false
//                }

            }
        }

        // Отдельно наблюдаем за ошибками постов (они не должны влиять на ленту)
        viewModel.postError.observe(viewLifecycleOwner) { errorMessage ->
            // Показываем в текущем фрагменте, если он активен
            if (this.isVisible) {
                Snackbar.make(
                    binding.root,
                    errorMessage,
                    Snackbar.LENGTH_LONG
                )
                    .setAnchorView(binding.add)
                    .setAction("ОК") { }
                    .show()
            }
        }

        binding.retryButton.setOnClickListener {
            viewModel.loadPost()
        }

        binding.add.setOnClickListener {
            findNavController().navigate(R.id.action_feedFragment_to_newPostFragment2)
        }
        //Применяем системные отступы
//        applyInsets(binding.root)
    }


    //системные отступы в приложении
    private fun applyInsets(root: View) {
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val isImeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            v.setPadding(
                v.paddingLeft,
                if (isImeVisible) imeInsets.top else systemBars.top,
                v.paddingRight,
                if (isImeVisible) imeInsets.bottom else systemBars.bottom
            )
            insets
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}