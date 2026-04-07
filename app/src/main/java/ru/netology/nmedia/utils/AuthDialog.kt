package ru.netology.nmedia.utils

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import ru.netology.nmedia.R

class AuthDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
            .setTitle("Требуется авторизация")
            .setMessage("Для выполнения этого действия необходимо войти в систему. Перейти на экран входа?")
            .setPositiveButton("Войти") { _, _ ->
                // Переход на экран аутентификации
                findNavController().navigate(R.id.action_feedFragment_to_signInFragment)
            }
            .setNegativeButton("Отмена", null)
            .create()
    }

    companion object {
        const val TAG = "AuthDialog"
    }
}