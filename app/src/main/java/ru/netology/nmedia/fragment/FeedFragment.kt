package ru.netology.nmedia.fragment

import android.content.Intent
import android.os.Bundle
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
import ru.netology.nmedia.R
import ru.netology.nmedia.viewmodel.PostViewModel
import ru.netology.nmedia.adapter.PostAdapter
import ru.netology.nmedia.adapter.PostListener
import ru.netology.nmedia.databinding.FragmentFeedBinding
import ru.netology.nmedia.dto.Post


class FeedFragment : Fragment() {

    private val viewModel: PostViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentFeedBinding.inflate(inflater, container, false)

        //val viewModel by viewModels<PostViewModel>(::requireParentFragment)
        //val viewModel by activityViewModels<PostViewModel>()


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

        viewModel.data.observe(viewLifecycleOwner) { state ->
            adapter.submitList(state.posts)
            binding.progress.isVisible = state.loading
            binding.errorGroup.isVisible = state.error
            binding.emptyText.isVisible = state.empty
        }

        binding.retryButton.setOnClickListener {
            viewModel.loadPost()
        }
        // Подписка на список постов
//        viewModel.get().observe(viewLifecycleOwner) { posts ->
//            adapter.submitList(posts)
//        }

        binding.add.setOnClickListener {
            findNavController().navigate(R.id.action_feedFragment_to_newPostFragment2)
        }

        return binding.root
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
}