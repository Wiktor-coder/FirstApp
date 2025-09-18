package ru.netology.nmedia.activity

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import ru.netology.nmedia.viewmodel.PostViewModel
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.ActivityMainBinding
import ru.netology.nmedia.viewmodel.formatNumberCompact

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsets(binding.root)

        val viewModel by viewModels<PostViewModel>()
        viewModel.get().observe(this) { post ->
            with(binding) {
                content.text = post.content
                author.text = post.author
                published.text = post.published
                numberOfLikes.text = post.likeCount.formatNumberCompact()
                numberOfShare.text = post.shareCount.formatNumberCompact()
                Like.setImageResource(
                    if (post.likedByMe) R.drawable.ic_baseline_favorite_24 else
                        R.drawable.outline_favorite_24
                )

                Like.setOnClickListener {
                    viewModel.like()
                    numberOfLikes.text = viewModel.get().value?.likeCount?.formatNumberCompact()
                }

                Share.setOnClickListener {
                    viewModel.share()
                    numberOfShare.text = viewModel.get().value?.shareCount?.formatNumberCompact()
                }
            }
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