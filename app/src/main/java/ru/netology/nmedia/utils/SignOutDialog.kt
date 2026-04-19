package ru.netology.nmedia.utils

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nmedia.auth.AppAuth
import javax.inject.Inject
@AndroidEntryPoint
class SignOutDialog : DialogFragment() {

    @Inject
    lateinit var appAuth: AppAuth

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
            .setTitle("Выход из аккаунта")
            .setMessage("Вы уверены, что хотите выйти?")
            .setPositiveButton("Выйти") { _, _ ->
                appAuth.removeAuth()
                // После выхода остаемся на текущем фрагменте
            }
            .setNegativeButton("Отмена", null)
            .create()
    }

    companion object {
        const val TAG = "SignOutDialog"
    }
}