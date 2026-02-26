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
import com.google.android.material.snackbar.Snackbar
import ru.netology.nmedia.databinding.FragmentNewPostBinding
import ru.netology.nmedia.utils.DraftRepository
import ru.netology.nmedia.utils.StringArg
import ru.netology.nmedia.viewmodel.PostViewModel
import kotlin.concurrent.thread


class NewPostFragment : Fragment() {

//    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentNewPostBinding.inflate(layoutInflater, container, false)
        val viewModel by activityViewModels<PostViewModel>()
        val context = requireContext()

        // Загружаем черновик
        DraftRepository.loadDraft(context)?.let { draft ->
            if (draft.isNotBlank()) {
                binding.edit.setText(draft)
                binding.edit.setSelection(draft.length)
            }
        }

        // Текст из share
        arguments?.textArg?.let { sharedText ->
            if (sharedText.isNotBlank()) {
                binding.edit.setText(sharedText)
                binding.edit.setSelection(sharedText.length)
                DraftRepository.clearDraft(context)
            }
        }

        viewModel.postCreated.observe(viewLifecycleOwner) { result ->
            if (result.isSuccess) {
                DraftRepository.clearDraft(requireContext())
                findNavController().navigateUp()
            }

        }

        viewModel.postError.observe(viewLifecycleOwner) { errorMassage ->
            Snackbar.make(
                binding.root,
                errorMassage ?: "Error",
                Snackbar.LENGTH_LONG
            )
                .setAnchorView(binding.ok)
                .setAction("Ok") {
                    binding.ok.isEnabled = true
                    binding.progress.visibility = View.GONE
                }
                .show()
        }

        binding.ok.setOnClickListener {
            val text = binding.edit.text.toString().trim()
            if (text.isNotEmpty()) {
                binding.ok.isEnabled = false
                binding.progress.visibility = View.VISIBLE // Показываем прогрес
                // Создаем пост и сразу закрываем
                viewModel.createPost(text)
//                DraftRepository.clearDraft(context)
//                findNavController().navigateUp() // Закрываем сразу, не ждем ответа
            } else {
                findNavController().navigateUp()
            }
        }

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