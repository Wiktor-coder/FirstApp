package ru.netology.nmedia.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import ru.netology.nmedia.databinding.FragmentEditPostBinding
import ru.netology.nmedia.viewmodel.PostViewModel
import kotlin.getValue


class EditPostFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentEditPostBinding.inflate(layoutInflater, container, false)
        val viewModel by activityViewModels<PostViewModel>()
        //val viewModel by viewModels<PostViewModel>(::requireParentFragment)

        //Получаем ID поста из аргументов
        val postId = arguments?.getLong("postId") ?: run {
            findNavController().navigateUp()
            return binding.root
        }

        // Наблюдаем за списком постов из LiveData
        viewModel.data.observe(viewLifecycleOwner) { feedModel ->
            val postToEdit = feedModel.posts.find { it.id == postId }
            if (postToEdit != null) {
                viewModel.edit(postToEdit)
                binding.edit.setText(postToEdit.content)
                binding.edit.setSelection(binding.edit.text.length)
            } else if (!feedModel.loading && !feedModel.error) {
                // Пост не найден — возвращаемся
                findNavController().navigateUp()
            }
        }

        // Наблюдаем за успешным сохранением
        viewModel.postSuccess.observe(viewLifecycleOwner) {
            findNavController().navigateUp()
        }

        viewModel.postError.observe(viewLifecycleOwner) { errorMessage ->
            Snackbar.make(
                binding.root,
                errorMessage,
                Snackbar.LENGTH_LONG
            )
                .setAnchorView(binding.ok)
                .setAction("Ok") {
                    // Разблокируем кнопку при ошибке
                    binding.ok.isEnabled = true
                    binding.progress.visibility = View.GONE
                }
                .show()
        }
//        viewModel.postCreated.observe(viewLifecycleOwner) {
//            findNavController().navigateUp()
//        }

        viewModel.error.observe(viewLifecycleOwner) { errorMassage ->
            Snackbar.make(
                binding.root,
                errorMassage ?: "Error",
                Snackbar.LENGTH_LONG
            )
                .setAction("OK") {}
                .show()
        }

        binding.ok.setOnClickListener {
            val text = binding.edit.text.toString().trim()
            if (text.isNotEmpty()) {
                viewModel.save(text) //редактирует существующий пост
                binding.ok.isEnabled = false
                binding.progress.visibility = View.VISIBLE

            } else {
                findNavController().navigateUp()
            }
        }

        binding.cancel.setOnClickListener {
            viewModel.cancelEdited()
            findNavController().navigateUp()
        }

        return binding.root
    }
}
