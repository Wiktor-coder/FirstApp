package ru.netology.nmedia.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import ru.netology.nmedia.databinding.FragmentEditPostBinding
import ru.netology.nmedia.viewmodel.PostViewModel

private const val TAG = "EditPostFragment"
class EditPostFragment : Fragment() {

    init {
        Log.d(TAG, "EditPostFragment created")
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "Uncaught exception in thread $thread", throwable)
        }
    }

    private var _binding: FragmentEditPostBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<PostViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView")
        _binding = FragmentEditPostBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Получаем ID поста из аргументов
        val postId = arguments?.getLong("postId")

        if (postId == null) {
            findNavController().navigateUp()
            return
        }

        // Показываем debug информацию
        binding.debugInfo.visibility = View.GONE
        binding.debugInfo.text = "Post ID: $postId\nLoading posts..."

        // Проверяем, есть ли данные
        if (viewModel.data.value.posts.isEmpty()) {
            viewModel.loadPosts()
        }

        // Наблюдаем за данными поста
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.data.collect { feedModel ->

                val postToEdit = feedModel.posts.find { it.id == postId }
                if (postToEdit != null) {
                    binding.edit.setText(postToEdit.content)
                    binding.edit.setSelection(postToEdit.content.length)
                    binding.debugInfo.text = "Editing post: ${postToEdit.id}\nContent: ${postToEdit.content.take(50)}"
                    viewModel.edit(postToEdit)
                } else if (!feedModel.loading && feedModel.posts.isNotEmpty()) {
                    binding.debugInfo.text = "Post not found!\nAvailable: ${feedModel.posts.map { it.id }}"
                }
            }
        }

        // Наблюдаем за успешным сохранением
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.postSuccess.collect {
                findNavController().navigateUp()
            }
        }

        // Наблюдаем за ошибками
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.postError.collect { errorMessage ->
                binding.debugInfo.text = "Error: $errorMessage"
                showErrorSnackbar(errorMessage)
            }
        }

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.ok.setOnClickListener {
            val text = binding.edit.text.toString().trim()
            Log.d(TAG, "Save clicked, text: '$text'")
            if (text.isNotEmpty()) {
                viewModel.save(text)
                binding.progress.visibility = View.VISIBLE
                binding.ok.isEnabled = false
            } else {
                findNavController().navigateUp()
            }
        }

        binding.cancel.setOnClickListener {
            Log.d(TAG, "Cancel clicked")
            viewModel.cancelEdited()
            findNavController().navigateUp()
        }
    }

    private fun showErrorSnackbar(message: String, onAction: (() -> Unit)? = null) {
        Snackbar.make(
            binding.root,
            message,
            Snackbar.LENGTH_LONG
        ).apply {
            setAnchorView(binding.ok)
            setAction("OK") {
                onAction?.invoke()
                dismiss()
            }
            show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView")
        _binding = null
    }
}
