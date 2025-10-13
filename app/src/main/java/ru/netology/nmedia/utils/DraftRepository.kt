package ru.netology.nmedia.utils

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.content.edit

object DraftRepository {

    private const val PREF_NAME = "draft_prefs"
    private const val DRAFT_KEY = "new_post_draft"

    fun saveDraft(context: Context, text: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit {
                putString(DRAFT_KEY, text)
            }
    }

    fun loadDraft(context: Context): String? {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(DRAFT_KEY, null)
    }

    @SuppressLint("UseKtx")
    fun clearDraft(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(DRAFT_KEY)
            .apply()
    }
}