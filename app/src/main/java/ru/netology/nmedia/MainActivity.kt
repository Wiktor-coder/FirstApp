package ru.netology.nmedia

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import ru.netology.nmedia.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsets(binding.root)

        val post = Post(
            1,
            "Нетология. Университет интернет-профессий будущего",
            "21 мая в 18:36",
            "Привет, это новая Нетология! Когда-то Нетология начиналась с интенсивов по онлайн-маркетингу. Затем появились курсы по дизайну, разработке, аналитике и управлению. Мы растём сами и помогаем расти студентам: от новичков до уверенных профессионалов. Но самое важное остаётся с нами: мы верим, что в каждом уже есть сила, которая заставляет хотеть больше, целиться выше, бежать быстрее. Наша миссия — помочь встать на путь роста и начать цепочку перемен → http://netolo.gy/fyb",
            85,
            8,
        )

        with(binding) {
            content.text = post.content
            author.text = post.author
            published.text = post.published
            numberOfLikes.text = formatNumberCompact(post.likeCount)
            numberOfShare.text = formatNumberCompact(post.shareCount)


            Like.setOnClickListener {
                post.likedByMe = !post.likedByMe

                Like.setImageResource(
                    if (post.likedByMe) {
                        R.drawable.ic_baseline_favorite_24
                    } else {
                        R.drawable.outline_favorite_24
                    }
                )

                if (post.likedByMe) {
                    post.likeCount++
                } else {
                    post.likeCount--
                }
                numberOfLikes.text = formatNumberCompact(post.likeCount)
            }

            Share.setOnClickListener {
                post.shareCount++
                numberOfShare.text = formatNumberCompact(post.shareCount)
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

private fun formatNumberCompact(number: Int): String {
    return when {
        number < 1_000 -> number.toString()
        number < 10_000 -> {
            val hundreds = number / 100
            val wholePart = hundreds / 10
            val decimalPart = hundreds % 10
            val str = if (decimalPart == 0) "$wholePart" else "$wholePart.$decimalPart"
            "$str.K"
        }number < 1_000_000 -> "${number / 1_000}K"
        number < 10_000_000 -> {
            val hundredThousands = number / 100_000 // 1_350_000 → 13
            val wholePart = hundredThousands / 10 // 13 → 1
            val decimalPart = hundredThousands % 10 // 13 → 3
            val str = if (decimalPart == 0) "$wholePart" else "$wholePart.$decimalPart"
            "$str.M"
        }

        else -> "${number / 1_000_000}M"
    }
}