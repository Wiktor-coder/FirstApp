package ru.netology.nmedia.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import ru.netology.nmedia.databinding.FragmentNewPostBinding
import ru.netology.nmedia.repository.DraftRepository
import ru.netology.nmedia.utils.Result
import ru.netology.nmedia.utils.StringArg
import ru.netology.nmedia.viewmodel.PostViewModel

class NewPostFragment : Fragment() {

    private var _binding: FragmentNewPostBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewPostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel by activityViewModels<PostViewModel>()

        loadDraft()

        // Текст из share
        arguments?.textArg?.let { sharedText ->
            if (sharedText.isNotBlank()) {
                binding.edit.setText(sharedText)
                binding.edit.setSelection(sharedText.length)
                clearDraft()
            }
        }

        // Наблюдаем за созданием поста
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.postCreated.collect { result ->
                when (result) {
                    is Result.Success -> {
                        clearDraft()
                        findNavController().navigateUp()
                    }
                    is Result.Error -> {
                        showError(result.message)
                        enableButtons(true)
                    }
                    is Result.Loading -> {
                        binding.progress.visibility = View.VISIBLE
                    }
                }
            }
        }

        // Наблюдаем за ошибками
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.postError.collect { errorMessage ->
                showError(errorMessage)
                enableButtons(true)
            }
        }

        binding.ok.setOnClickListener {
            val text = binding.edit.text.toString().trim()
            if (text.isNotEmpty()) {
                enableButtons(false)
                viewModel.createPost(text)
            } else {
                findNavController().navigateUp()
            }
        }

        setupBackPressHandler()
    }

    private fun loadDraft() {
        viewLifecycleOwner.lifecycleScope.launch {
            val draft = DraftRepository.loadDraft(requireContext())
            if (!draft.isNullOrBlank()) {
                binding.edit.setText(draft)
                binding.edit.setSelection(draft.length)
            }
        }
    }

    private fun clearDraft() {
        viewLifecycleOwner.lifecycleScope.launch {
            DraftRepository.clearDraft(requireContext())
        }
    }

    private fun saveDraft() {
        val currentText = binding.edit.text.toString().trim()
        if (currentText.isNotEmpty()) {
            viewLifecycleOwner.lifecycleScope.launch {
                DraftRepository.saveDraft(requireContext(), currentText)
            }
        }
    }

    private fun enableButtons(enabled: Boolean) {
        binding.ok.isEnabled = enabled
        binding.progress.visibility = if (enabled) View.GONE else View.VISIBLE
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAnchorView(binding.ok)
            .setAction("Ok") {
                enableButtons(true)
            }
            .show()
    }

    private fun setupBackPressHandler() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                saveDraft()
                findNavController().navigateUp()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        var Bundle.textArg: String? by StringArg
    }
}