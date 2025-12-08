package ru.netology.nmedia.fragment

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import ru.netology.nmedia.databinding.FragmentNewPostBinding
import ru.netology.nmedia.utils.DraftRepository
import ru.netology.nmedia.utils.StringArg
import ru.netology.nmedia.viewmodel.PostViewModel
import kotlin.concurrent.thread


class NewPostFragment : Fragment() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentNewPostBinding.inflate(layoutInflater, container, false)
        val viewModel by activityViewModels<PostViewModel>()
        //val viewModel by viewModels<PostViewModel>(::requireParentFragment)

        arguments?.textArg.let(binding.edit::setText) // ?

        val context = requireContext()

        // 1. Загружаем черновик (если есть)
        //Пользователь открывает "Новый пост" -> видит черновик (если был)
        DraftRepository.loadDraft(context)?.let { draft ->
            if (draft.isNotBlank()) {
                binding.edit.setText(draft)
                binding.edit.setSelection(draft.length)
            }
        }

        // 2. Если передан текст через share (например, извне), он имеет приоритет над черновиком
        // Пользователь нажимает «Назад» -> текст сохраняется как черновик.
        arguments?.textArg?.let { sharedText ->
            if (sharedText.isNotBlank()) {
                binding.edit.setText(sharedText)
                binding.edit.setSelection(sharedText.length)
                // Очищаем черновик, так как пользователь явно поделился текстом
                DraftRepository.clearDraft(context)
            }
        }

        // 3. обработчик нажатия на кнопку добавления
        // Пользователь нажимает «ОК» -> пост создаётся, черновик удаляется.
        binding.ok.setOnClickListener {
            thread {
                val text = binding.edit.text.toString().trim()
                if (text.isNotEmpty()) {
                    viewModel.createPost(text)
                    // Успешно сохранили — очищаем черновик
                    DraftRepository.clearDraft(context)
                }
                findNavController().navigateUp()
            }
        }

        // 4. Перехватываем системную кнопку "Назад"
        // Пользователь делится текстом извне (через share)
        // -> черновик игнорируется, текст подставляется, черновик очищается.
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentText = binding.edit.text.toString().trim()
                if (currentText.isNotEmpty()) {
                    DraftRepository.saveDraft(requireContext(), currentText)
                }
                findNavController().navigateUp()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)

        return binding.root
    }

    companion object {
        var Bundle.textArg: String? by StringArg
    }
}