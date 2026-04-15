package ru.netology.nmedia.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONException
import org.json.JSONObject
import ru.netology.nmedia.R
import ru.netology.nmedia.activity.AppActivity
import ru.netology.nmedia.api.PostApi
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.dto.PushToken
import kotlin.random.Random

private const val MAX_NOTIFICATION_CONTENT_LENGTH = 1000

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val action = "action"
    private val content = "content"
    private val channelId = "remote"
    private val gson = Gson()

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_remote_name) //"Server notifications"
            val descriptionText =
                getString(R.string.channel_remote_description) //"Notification from remote server"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token: $token")
        // Отправляем новый токен на сервер
        sendPushTokenToServer(token)

//        AppAuth.getInstance().sendPushToken(token)
//        print(token)

    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d("FCM", "=== PUSH RECEIVED ===")
        Log.d("FCM", "All data: ${message.data}")

        // Пробуем получить recipientId напрямую
        var recipientId = message.data["recipientId"]?.toLongOrNull()
        var notificationContent = message.data["content"] ?: return
        var action = message.data["action"]
        var postId = message.data["postId"]?.toLongOrNull()

        // Если recipientId не найден напрямую, парсим из content (так как сервер кладет JSON туда)
        if (recipientId == null) {
            try {
                // Парсим JSON из строки content
                val jsonObject = JSONObject(notificationContent)
                recipientId = if (jsonObject.has("recipientId")) {
                    val id = jsonObject.getLong("recipientId")
                    // Если recipientId найден, обновляем content, убирая его из JSON
                    notificationContent = jsonObject.optString("content", notificationContent)
                    action = jsonObject.optString("action", action)
                    postId = if (jsonObject.has("postId")) jsonObject.getLong("postId") else null
                    id
                } else {
                    null
                }
                Log.d("FCM", "Parsed recipientId from content JSON: $recipientId")
            } catch (e: JSONException) {
                Log.d("FCM", "Content is not JSON, using as is")
            }
        }

        val currentUserId = AppAuth.getInstance().authStateFlow.value.id

        Log.d("FCM", "RecipientId from server: $recipientId")
        Log.d("FCM", "Current User ID: $currentUserId")
        Log.d("FCM", "Content: $notificationContent")
        Log.d("FCM", "Action: $action")
        Log.d("FCM", "PostId: $postId")

        // СТРОГАЯ ПРОВЕРКА
        val shouldShow = when {
            // Массовая рассылка
            recipientId == null -> {
                Log.d("FCM", "✅ Mass notification - SHOWING")
                true
            }
            // Уведомление для текущего пользователя
            recipientId == currentUserId && currentUserId != 0L -> {
                Log.d("FCM", "✅ Notification for current user - SHOWING")
                true
            }
            // Анонимный пользователь
            recipientId == 0L -> {
                Log.d("FCM", "⚠️ Anonymous auth detected - IGNORING")
                resendPushToken()
                false
            }
            // НЕПРАВИЛЬНЫЙ recipientId - НЕ ПОКАЗЫВАЕМ!
            recipientId != currentUserId -> {
                Log.w("FCM", "❌ WRONG RECIPIENT! Expected: $currentUserId, Got: $recipientId - IGNORING")
                resendPushToken()
                false
            }
            else -> {
                Log.d("FCM", "Unknown case - IGNORING")
                false
            }
        }

        if (shouldShow) {
            showNotification(notificationContent, action, postId)
        } else {
            Log.d("FCM", "❌ Notification BLOCKED by client-side check")
        }

//        message.data[action]?.let { actionStr ->
//            val action = Action.entries.find { it.name == actionStr }
//            val json = message.data[content] ?: return
//            when (action) {
//                Action.LIKE -> {
//                    val like = gson.fromJson(json, Like::class.java)
//                    val title = getString(
//                        R.string.notification_user_liked,
//                        like.userName,
//                        like.postAuthor
//                    )
//                    showNotification(title)
//
//                }
//
//                Action.SHARE -> {
//                    val share = gson.fromJson(json, Share::class.java)
//                    val title = getString(
//                        R.string.notification_user_shared,
//                        share.userName
//                    )
//                    showNotification(title)
//                }
//
//                Action.NEW_POST -> {
//                    val post = gson.fromJson(json, NewPost::class.java)
//                    val title = "${post.authorName} опубликовал новый пост:"
//                    val trimmedContent =
//                        if (post.content.length > MAX_NOTIFICATION_CONTENT_LENGTH) {
//                            post.content.take(MAX_NOTIFICATION_CONTENT_LENGTH) + "…"
//                        } else {
//                            post.content
//                        }
//                    showNotification(title, trimmedContent)
//                }
//
//                null -> {
//                    Log.w("FCM", "Unknown action: $actionStr")
//                }
//            }
//        }
    }

    private fun showNotification(content: String, actionType: String?, postId: Long?) {
        val intent = Intent(this, AppActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

            // Если есть postId, можно добавить его в Intent для навигации
            postId?.let {
                putExtra("postId", it)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when (actionType) {
            "LIKE" -> "👍 Новый лайк!"
            "SHARE" -> "🔄 Новый репост!"
            "NEW_POST" -> "📝 Новый пост!"
            else -> "Уведомление"
        }

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(pendingIntent) // Добавьте intent
            .setAutoCancel(true) // Закрывать при клике
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))

        // Ограничиваем длину текста для уведомления
        val trimmedContent = if (content.length > MAX_NOTIFICATION_CONTENT_LENGTH) {
            content.take(MAX_NOTIFICATION_CONTENT_LENGTH) + "…"
        } else {
            content
        }
        builder.setContentText(trimmedContent)

        notify(builder.build())
    }

    private fun resendPushToken() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Получаем текущий push-токен
                val token = FirebaseMessaging.getInstance().token.await()
                Log.d("FCM", "Re-sending push token: $token")

                // Отправляем токен на сервер
                val response = PostApi.service.sendPushToken(PushToken(token))
                if (response.isSuccessful) {
                    Log.d("FCM", "Push token re-sent successfully")
                } else {
                    Log.e("FCM", "Failed to re-send push token: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("FCM", "Error re-sending push token", e)
            }
        }
    }

    private fun sendPushTokenToServer(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = PostApi.service.sendPushToken(PushToken(token))
                if (response.isSuccessful) {
                    Log.d("FCM", "Push token sent successfully")
                } else {
                    Log.e("FCM", "Failed to send push token: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("FCM", "Error sending push token", e)
            }
        }
    }

        private fun handleLike(content: Like) {
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle(
                getString(
                    R.string.notification_user_liked,
                    content.userName,
                    content.postAuthor
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notify(notification)

    }

    private fun handleShare(content: Share) {
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle(
                getString(
                    R.string.notification_user_shared,
                    content.userName
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notify(notification)
    }

    private fun handleNewPost(post: NewPost) {
        val title = "${post.authorName} опубликовал новый пост: "

        // Ограничиваем длину текста до 1000 символов
        val trimmedContent = if (post.content.length > MAX_NOTIFICATION_CONTENT_LENGTH) {
            post.content.take(MAX_NOTIFICATION_CONTENT_LENGTH) + "..."
        } else {
            post.content
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle(title)
            .setContentText(trimmedContent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(trimmedContent))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notify(notification)
    }

    private fun notify(notification: Notification) {
        // Проверяем разрешение ТОЛЬКО на Android 13 (API 33) и выше
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w("FCM", "Notification permission denied")
                return
            }
        }
        // На Android < 13 — просто показываем уведомление
        NotificationManagerCompat.from(this).notify(
            Random.nextInt(100_000),
            notification
        )
    }

}

enum class Action {
    LIKE,
    SHARE,
    NEW_POST,
}

data class Like(
    val userId: Long,
    val userName: String,
    val postId: Long,
    val postAuthor: String,
)

data class Share(
    val userId: Long,
    val userName: String,
    val postId: Long,
    val postAuthor: String,
)

data class NewPost(
    val authorId: Long,
    val authorName: String,
    val postId: Long,
    val content: String,
)