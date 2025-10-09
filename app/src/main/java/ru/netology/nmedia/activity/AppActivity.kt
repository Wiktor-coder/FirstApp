package ru.netology.nmedia.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.snackbar.Snackbar
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.ActivityAppBinding
import ru.netology.nmedia.fragment.NewPostFragment.Companion.textArg


class AppActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val binding = ActivityAppBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //setContentView(R.layout.activity_intent_handler)
        ViewCompat.setOnApplyWindowInsetsListener(binding.navController) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        intent?.let {
            if (it.action != Intent.ACTION_SEND) {
                return@let
            }

            val text = it.getStringExtra(Intent.EXTRA_TEXT) //?: ""
            if (text.isNullOrBlank()) {
                // Snackbar.LENGTH_INDEFINITE показывает сообщение пока пользователь не нажмёт окей
                Snackbar.make(
                    binding.root,
                    R.string.error_empty_content,
                    Snackbar.LENGTH_INDEFINITE
                )
                    // resId показываем кнопку "ок" finish() завершает activity
                    .setAction(android.R.string.ok) { finish() }
                    .show()
            }
            //findNavController(R.id.nav_controller)
            binding.navController.getFragment<NavHostFragment>().navController
                .navigate(
                    R.id.action_feedFragment_to_newPostFragment2,
                    Bundle().apply { textArg = text }
                )
        }
    }
}