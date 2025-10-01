package ru.netology.nmedia.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.ActivityEditPostBinding

class EditPostActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val binding = ActivityEditPostBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //получаем исходный текст
        val initialText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
        binding.edit.setText(initialText)
        //установка курсора в конец
        binding.edit.setSelection(binding.edit.text.length)

        //обработчик нажатия на кнопку
        binding.ok.setOnClickListener {
            val text = binding.edit.text.toString().trim()

            if (text.isEmpty()) {
                setResult(RESULT_CANCELED)
            } else {
                val resultIntent = Intent().apply { putExtra(Intent.EXTRA_TEXT, text) }
                setResult(RESULT_OK, resultIntent)
            }
            //закрытие активити
            finish()
        }

        binding.cancel.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }

    }
}

class EditPostContract : ActivityResultContract<String, String?>() {
    override fun createIntent(
        context: Context,
        input: String
    ) = Intent(context, EditPostActivity::class.java).apply {
        putExtra(Intent.EXTRA_TEXT, input)
    }

    override fun parseResult(
        resultCode: Int,
        intent: Intent?
    ) = if (resultCode == android.app.Activity.RESULT_OK) {
        intent?.getStringExtra(Intent.EXTRA_TEXT)
    } else {
        null
    }

}