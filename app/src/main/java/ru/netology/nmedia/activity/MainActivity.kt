package ru.netology.nmedia.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.launch
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import ru.netology.nmedia.R
import ru.netology.nmedia.viewmodel.PostViewModel
import ru.netology.nmedia.adapter.PostAdapter
import ru.netology.nmedia.adapter.PostListener
import ru.netology.nmedia.databinding.ActivityMainBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.utils.AndroidUtils

class MainActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsets(binding.root)

        val viewModel by viewModels<PostViewModel>()

        //регистрируем контракт на создание нового поста, передаём наш контракт
        val newPostLauncher = registerForActivityResult(NewPostContract) { result ->
            //обработчик, проверяем на null если true выходим
            result ?: return@registerForActivityResult
            // иначе вызываем метод сохранить, пердаём result
            viewModel.createPost(result)
        }

        //регистрируем контракт на редактирование поста
        val editPostLauncher = registerForActivityResult(EditPostContract()) { newText ->
            newText ?: return@registerForActivityResult
            viewModel.edited.value?.let { post ->
                viewModel.save( newText)
            }
        }

        // Настройка RecyclerView
        val adapter = PostAdapter(
            object : PostListener {
                override fun onLike(post: Post) {
                    viewModel.likeById(post.id)
                }

                //подробнее об реализации? возможно ли переместить реализацию в PostRepositoryInMemoryImpl из activity?
                override fun onShare(post: Post) {
                    viewModel.shareById(post.id)
                    val intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, post.content)
                    }
                    val chooser = Intent.createChooser(intent, getString(R.string.chooser_share_post))
                    startActivity(chooser)
                }

                override fun onRemove(post: Post) {
                    viewModel.removeById(post.id)
                }

                override fun onEdit(post: Post) {
                    viewModel.edit(post)
                    editPostLauncher.launch(post.content)
                }
            }
        )
        binding.container.adapter = adapter

        // Подписка на список постов
        viewModel.get().observe(this) { posts ->
            adapter.submitList(posts)
        }

        // !!нужно сделать!!
        viewModel.edited.observe(this) { edited ->
//            binding.content.setText(edited?.content ?: "")
//            if (edited != null) {
//                binding.content.requestFocus()
//                AndroidUtils.showKeyboard(binding.content)
//            }
        }

        //наблюдаем за состоянием редактирования
//        viewModel.isEditing.observe(this) { isEditing ->
//            binding.group.visibility = if (isEditing) View.VISIBLE else View.GONE
//        }

        // Заполняем EditText при начале редактирования
//        viewModel.edited.observe(this) { post ->
//            binding.content.setText(post?.content ?: "")
//        }

        binding.add.setOnClickListener {
            //import androidx.activity.result.launch чтобы не передовать Unit в метод
            newPostLauncher.launch()

            //читаем ввод и обрезаем пробелы
//            val currentText =
//                binding.content.text?.trim().toString()
//
//            if (viewModel.isEditing.value == true) {
//                //Режим редактирования
//                viewModel.save(currentText)
//            } else {
//                // Режим создания нового поста
//                viewModel.createPost(currentText)
//            }
//
//            binding.content.setText("") //
//            binding.content.clearFocus()
//            //скрыть клавиатуру
//            AndroidUtils.hideKeyboard(binding.content)
        }

        //Кнопка "Отмена" (крестик)
//        binding.closed.setOnClickListener {
//            viewModel.cancelEdited()
//            binding.content.setText("")
//            binding.content.clearFocus()
//            AndroidUtils.hideKeyboard(binding.content)
//        }
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
                //v.paddingTop + systemBars.top,
                v.paddingRight,
                if (isImeVisible) imeInsets.bottom else systemBars.bottom
                //v.paddingBottom + systemBars.bottom
            )
            insets
        }
    }
}