package ru.netology.nmedia.repository

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DraftRepository {
    private const val PREFS_NAME = "draft_prefs"
    private const val KEY_DRAFT = "draft"

    suspend fun loadDraft(context: Context): String? = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return@withContext prefs.getString(KEY_DRAFT, null)
    }

    suspend fun saveDraft(context: Context, draft: String) = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_DRAFT, draft).apply()
    }

    suspend fun clearDraft(context: Context) = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_DRAFT).apply()
    }
}