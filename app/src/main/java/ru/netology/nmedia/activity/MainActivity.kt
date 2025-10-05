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

                //поделится
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

                override fun hasVideo(post: Post): Boolean {
                    return viewModel.hasVideo(post)
                }

                override fun getVideoUrl(post: Post): String? {
                    return viewModel.getVideoUrl(post)
                }
            }
        )
        binding.container.adapter = adapter

        // Подписка на список постов
        viewModel.get().observe(this) { posts ->
            adapter.submitList(posts)
        }

        binding.add.setOnClickListener {
            newPostLauncher.launch()
        }
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