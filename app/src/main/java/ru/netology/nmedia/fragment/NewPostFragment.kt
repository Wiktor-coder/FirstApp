package ru.netology.nmedia.fragment

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import ru.netology.nmedia.databinding.FragmentNewPostBinding
import ru.netology.nmedia.utils.StringArg
import ru.netology.nmedia.viewmodel.PostViewModel
import kotlin.getValue


class NewPostFragment : androidx.fragment.app.Fragment() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentNewPostBinding.inflate(layoutInflater, container, false)
        val viewModel by activityViewModels<PostViewModel>()
        //val viewModel by viewModels<PostViewModel>(::requireParentFragment)

        arguments?.textArg.let(binding.edit::setText)


//        обработчик нажатия на кнопку добавления
        binding.ok.setOnClickListener {
            val text = binding.edit.text.toString()
            if (text.isBlank()) {
                activity?.setResult(Activity.RESULT_CANCELED)
            } else {
                //val intent = Intent().apply { putExtra(Intent.EXTRA_TEXT, text) }
                //activity?.setResult(Activity.RESULT_OK, intent)
                viewModel.createPost(binding.edit.text.toString())
            }
            //закрытие активити
//            activity?.finish()
            findNavController().navigateUp()


            //Альтернатива без вызова ошибки
//            if (!binding.edit.text.isNullOrBlank()) {
//                val text = binding.edit.text.toString()
//                viewModel.createPost(text)
//                findNavController().navigateUp()
//            }
        }
        return binding.root
    }

    companion object {
        var Bundle.textArg: String? by StringArg
    }
}