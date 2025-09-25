package ru.netology.nmedia.activity

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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

        // Настройка RecyclerView
        val adapter = PostAdapter(
            object : PostListener {
                override fun onLike(post: Post) {
                    viewModel.likeById(post.id)
                }

                override fun onShare(post: Post) {
                    viewModel.shareById(post.id)
                }

                override fun onRemove(post: Post) {
                    viewModel.removeById(post.id)
                }

                override fun onEdit(post: Post) {
                    viewModel.edit(post)
                }
            }
        )
        binding.container.adapter = adapter

        viewModel.get().observe(this) { posts ->
            adapter.submitList(posts)
        }
        // Подписка на список постов
        viewModel.get().observe(this) { posts ->
            adapter.submitList(posts)
        }

        // отмена редактирования должна быть здесь
        viewModel.edited.observe(this) { edited ->
            binding.content.setText(edited?.content ?: "")
            if (edited != null) {
                binding.content.requestFocus()
                AndroidUtils.showKeyboard(binding.content)
            }
        }

        //наблюдаем за состоянием редактирования
        viewModel.isEditing.observe(this) { isEditing ->
            binding.group.visibility = if (isEditing) View.VISIBLE else View.GONE
        }

        // Заполняем EditText при начале редактирования
        viewModel.edited.observe(this) { post ->
            binding.content.setText(post?.content ?: "")
        }

        binding.save.setOnClickListener {
            //читаем ввод и обрезаем пробелы
            val currentText =
                binding.content.text?.trim().toString()

            if (viewModel.isEditing.value == true) {
                //Режим редактирования
                viewModel.save(currentText)
            } else {
                // Режим создания нового поста
                viewModel.createPost(currentText)
            }

            binding.content.setText("") //
            binding.content.clearFocus()
            //скрыть клавиатуру
            AndroidUtils.hideKeyboard(binding.content)
        }

        //Кнопка "Отмена" (крестик)
        binding.closed.setOnClickListener {
            viewModel.cancelEdited()
            binding.content.setText("")
            binding.content.clearFocus()
            AndroidUtils.hideKeyboard(binding.content)
        }
    }

    private fun applyInsets(root: View) {
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                v.paddingLeft,
                v.paddingTop + systemBars.top,
                v.paddingRight,
                v.paddingBottom + systemBars.bottom
            )
            insets
        }
    }
}