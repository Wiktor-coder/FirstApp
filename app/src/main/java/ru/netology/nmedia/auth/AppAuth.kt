package ru.netology.nmedia.auth

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import ru.netology.nmedia.api.PostApiService
import ru.netology.nmedia.dto.PushToken
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppAuth @Inject constructor(
    @ApplicationContext private val context: Context,
    private val postApiService: PostApiService
    ) {
    private val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
    private val idKey = "id"
    private val tokenKey = "token"

    private val _authStateFlow: MutableStateFlow<AuthState>

    init {
        val id = prefs.getLong(idKey, 0)
        val token = prefs.getString(tokenKey, null)

        if (id == 0L || token == null) {
            _authStateFlow = MutableStateFlow(AuthState())
            with(prefs.edit()) {
                clear()
                apply()
            }
        } else {
            _authStateFlow = MutableStateFlow(AuthState(id, token))
        }

        sendPushToken(token)
    }

    val authStateFlow: StateFlow<AuthState> = _authStateFlow.asStateFlow()

    @Synchronized
    fun setAuth(id: Long, token: String) {
        _authStateFlow.value = AuthState(id, token)
        with(prefs.edit()) {
            putLong(idKey, id)
            putString(tokenKey, token)
            apply()
        }

        sendPushToken()
    }

    @Synchronized
    fun removeAuth() {
        _authStateFlow.value = AuthState()
        with(prefs.edit()) {
            clear()
            commit()
        }

        sendPushToken()
    }
    fun sendPushToken(token: String? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val pushToken = token ?: FirebaseMessaging.getInstance().token.await()
                Log.d("AppAuth", "Sending push token: $pushToken")
                val response = postApiService.sendPushToken(PushToken(pushToken))
                if (response.isSuccessful) {
                    Log.d("AppAuth", "Push token sent successfully")
                } else {
                    Log.e("AppAuth", "Failed to send push token: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("AppAuth", "Error sending push token", e)
            }
        }
    }
//    fun sendPushToken(token: String? = null) {
//        CoroutineScope(EmptyCoroutineContext).launch {
//            runCatching {
//                PostApi.service.sendPushToken(
//                    PushToken(
//                        token ?: FirebaseMessaging.getInstance().token.await()
//                    )
//                )
//            }
//                .onFailure { it.printStackTrace() }
//        }
//    }

//    companion object {
//        @Volatile
//        private var instance: AppAuth? = null
//
//        fun getInstance(): AppAuth = synchronized(this) {
//            instance ?: throw IllegalStateException(
//                "AppAuth is not initialized, you must call AppAuth.initializeApp(Context context) first."
//            )
//        }
//
//        fun initApp(context: Context): AppAuth = instance ?: synchronized(this) {
//            instance ?: buildAuth(context).also { instance = it }
//        }
//
//        private fun buildAuth(context: Context): AppAuth = AppAuth(context)
//    }
}

data class AuthState(val id: Long = 0, val token: String? = null)