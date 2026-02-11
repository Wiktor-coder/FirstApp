package ru.netology.nmedia.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.FragmentSinglePostBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.utils.toFormattedDate
import ru.netology.nmedia.viewmodel.PostViewModel

class SinglePostFragment : Fragment() {

    //private val args: SinglePostFragmentArgs by navArgs() // ← если используете Safe Args
    private var _binding: FragmentSinglePostBinding? = null
    private val binding get() = _binding!!
    val viewModel by activityViewModels<PostViewModel>()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSinglePostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Получаем postId (если используете Safe Args)
        val postId = arguments?.getLong("postId") ?: run {
            findNavController().navigateUp()
            return
        }

        // Подписываемся на список постов
//        viewModel.get().observe(viewLifecycleOwner) { posts ->
//            val post = posts.find { it.id == postId }
//            if (post != null) {
//                bind(post)
//            } else {
//                findNavController().navigateUp()
//            }
//        }
//        val posts = viewModel.get()
//        val post = posts.find { it.id == postId }
//        if (post != null) {
//            bind(post)
//        } else {
//            findNavController().navigateUp()
//        }
        viewModel.data.observe(viewLifecycleOwner) { feedModel ->
            val post = feedModel.posts.find { it.id == postId }
            if (post != null) {
                bind(post)
            } else if (!feedModel.loading && !feedModel.error) {
                findNavController().navigateUp()
            }
        }

        // Кнопка "Назад" — системная, но можно добавить toolbar, если нужно
    }

    private fun bind(post: Post) {
        with(binding) {
            author.text = post.author
            published.text = post.published.toFormattedDate()
            content.text = post.content
            Like.text = post.likeCount.toString()
            Like.isChecked = post.likedByMe
            Share.text = post.shareCount.toString()

            // Видео
            if (post.video.isNullOrBlank()) {
                videoContainer.visibility = View.GONE
            } else {
                videoContainer.visibility = View.VISIBLE
                // Можно загрузить превью, но пока оставим заглушку
            }

            // Обработчики
//            postRoot.setOnClickListener {
//                // Игнорируем — клик по всему посту не должен ничего делать
//                // (но на самом деле он и не нужен, т.к. мы уже в SinglePost)
//            }

            _binding?.Like?.setOnClickListener {
                viewModel.likeById(post.id)
            }

            Share.setOnClickListener {
                viewModel.shareById(post.id)
                // Share intent
                val intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, post.content)
                }
                val chooser = Intent.createChooser(intent, getString(R.string.chooser_share_post))
                startActivity(chooser)
            }

            menu.setOnClickListener {
                showMenu(post)
            }
        }
    }

    private fun showMenu(post: Post) {
        // Создайте PopupMenu или AlertDialog с "Изменить" и "Удалить"
        val popup = PopupMenu(requireContext(), binding.menu)
        popup.menuInflater.inflate(R.menu.post_menu, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.edit -> {
                    viewModel.edit(post)
                    val bundle = bundleOf("postId" to post.id)
                    findNavController().navigate(
                        R.id.editPostFragment, bundle
                    )
                    true
                }

                R.id.remove -> {
                    viewModel.removeById(post.id)
                    findNavController().navigateUp() // возврат в ленту
                    true
                }

                else -> false
            }
        }
        popup.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}