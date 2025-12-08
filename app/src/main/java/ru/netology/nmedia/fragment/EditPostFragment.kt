package ru.netology.nmedia.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
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

        // Подписываемся на список постов и ищем нужный
//        viewModel.get().observe(viewLifecycleOwner) { posts ->
//            val postToEdit = posts.find { it.id == postId }
//            if (postToEdit != null) {
//                viewModel.edit(postToEdit) // устанавливаем редактируемый пост
//                binding.edit.setText(postToEdit.content)
//                binding.edit.setSelection(binding.edit.text.length)
//            } else {
//                //Пост не найден — возвращаемся
//                findNavController().navigateUp()
//            }
//        }
        // НЕТ .observe() — просто вызов функции
        val posts = viewModel.get() // List<Post>
        val postToEdit = posts.find { it.id == postId }
        if (postToEdit != null) {
            viewModel.edit(postToEdit)
            binding.edit.setText(postToEdit.content)
            binding.edit.setSelection(binding.edit.text.length)
        } else {
            findNavController().navigateUp()
        }

        binding.ok.setOnClickListener {
            val text = binding.edit.text.toString().trim()
            if (text.isNotEmpty()) {
                viewModel.save(text) //редактирует существующий пост
            }
            findNavController().navigateUp()
        }

        binding.cancel.setOnClickListener {
            viewModel.cancelEdited()
            findNavController().navigateUp()
        }

        return binding.root
    }
}
