package ru.netology.nmedia.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import ru.netology.nmedia.R
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
        print(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {

        super.onMessageReceived(message)

        message.data[action]?.let { actionStr ->
            val action = Action.entries.find { it.name == actionStr }
            val json = message.data[content] ?: return
            when (action) {
                Action.LIKE -> {
                    val like = gson.fromJson(json, Like::class.java)
                    val title = getString(
                        R.string.notification_user_liked,
                        like.userName,
                        like.postAuthor)
                    showNotification(title)

//                    val json = message.data[content] ?: return
//                    val likeObj = gson.fromJson(json, Like::class.java)
//                    handleLike(likeObj)
                }
//                    handleLike(
//                    gson.fromJson(
//                        message.data[content],
//                        Like::class.java
//                    )
//                )

                Action.SHARE -> {
                    val share = gson.fromJson(json, Share::class.java)
                    val title = getString(
                        R.string.notification_user_shared,
                        share.userName
                    )
                    showNotification(title)

//                    val json = message.data[content] ?: return
//                    val sharePost = gson.fromJson(json, Share::class.java)
//                    handleShare(sharePost)
                }

                Action.NEW_POST -> {
                    val post = gson.fromJson(json, NewPost::class.java)
                    val title = "${post.authorName} опубликовал новый пост:"
                    val trimmedContent = if (post.content.length > MAX_NOTIFICATION_CONTENT_LENGTH) {
                        post.content.take(MAX_NOTIFICATION_CONTENT_LENGTH) + "…"
                    } else {
                        post.content
                    }
                    showNotification(title, trimmedContent)

//                    val json = message.data[content] ?: return
//                    val newPost = gson.fromJson(json, NewPost::class.java)
//                    handleNewPost(newPost)
                }
                null -> {
                    Log.w("FCM", "Unknown action: $actionStr")
                }
            }
        }
    }

    private fun showNotification(title: String, text: String? = null) {
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle(title)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        if (!text.isNullOrBlank()) {
            builder
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
        }
        notify(builder.build())
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
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("FCM", "Notification permission denied")
            return
        }
        NotificationManagerCompat.from(this)
            .notify(Random.nextInt(100_000), notification)
    }

//    private fun notify(notification: Notification) {
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
//            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
//            PackageManager.PERMISSION_DENIED
//        )
//            NotificationManagerCompat.from(this).notify(
//                Random.nextInt(100_000), notification
//            )
//    }
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