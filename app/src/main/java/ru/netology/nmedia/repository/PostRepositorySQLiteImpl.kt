package ru.netology.nmedia.repository

import android.annotation.SuppressLint
import android.content.Context
import android.net.NetworkCapabilities
import android.net.ConnectivityManager
import android.os.Build
import android.util.Log
import java.util.concurrent.TimeUnit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.IOException
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.dto.Attachment
import ru.netology.nmedia.dto.AttachmentType
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostEntity

class PostRepositorySQLiteImpl(
    private val context: Context,
    private val dao: PostDao = AppDb.getInstance(context).postDao
) : PostRepository {
    // для получения списка
    private val postListType = object : TypeToken<List<Post>>() {}.type

    companion object {
        const val BASE_URL = "http://10.0.2.2:9999"

        val jsonType = "application/json".toMediaType()
    }

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)  // Добавьте readTimeout
        .writeTimeout(30, TimeUnit.SECONDS) // Добавьте writeTimeout
        .build()

    // Вспомогательная функция для получения полного URL аватарки
    fun getAvatarUrl(avatarPath: String?): String? {
        return if (!avatarPath.isNullOrBlank()) {
            "${BASE_URL}/avatars/${avatarPath}"
        } else null
    }

    override fun getLocalPosts(): List<Post> {
        return dao.getAll().map { it.toPost() }
    }

    // Вспомогательная функция для получения URL вложения
    fun getAttachmentUrl(attachment: Attachment?): String? {
        val url = when(attachment?.type) {
            AttachmentType.IMAGE -> "${BASE_URL}/images/${attachment.url}"
            AttachmentType.VIDEO -> "${BASE_URL}/video/${attachment.url}"
            AttachmentType.AUDIO -> "${BASE_URL}/audio/${attachment.url}"
            null -> null
        }
        Log.d("ATTACHMENT_DEBUG", "getAttachmentUrl: type=${attachment?.type}, input=${attachment?.url}, output=$url")
        return url
    }

    override fun getAllAsync(callback: PostRepository.PostCallback<List<Post>>) {
        if (!isNetworkAvailable()) {
            // Если нет сети, возвращаем кэшированные данные
            callback.onSuccess(getLocalPosts())
            return
        }

        val request = Request.Builder()
            .url("${BASE_URL}/api/slow/posts")
            .build()

        client.newCall(request)
            .enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    callback.onError(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        if (!response.isSuccessful) {
                            callback.onError(IOException("Неожиданный код $response"))
                            return
                        }
                        val body = response.body?.string() ?: throw IOException("Пустой ответ")

                        // Логируем весь ответ для отладки
                        Log.d("ATTACHMENT_DEBUG", "Server response: $body")

                        val posts = gson.fromJson<List<Post>>(body, postListType)

                        posts.forEach { post ->
                            Log.d("ATTACHMENT_DEBUG",
                                "Post ${post.id}: attachment=${post.attachment?.url}, type=${post.attachment?.type}")
                        }

                        // Сохраняем в БД через saveAll
                        val postEntities = posts.map { PostEntity.fromPost(it) }
                        dao.saveAll(postEntities)
                        callback.onSuccess(posts)
                    } catch (e: Exception) {
                        callback.onError(e)
                    } finally {
                        response.close()
                    }
                }
            })
    }

    @SuppressLint("ServiceCast")
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Для Android 6.0 (API 23) и выше
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

            return when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else {
            // Для старых версий Android (до 6.0)
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            return networkInfo.isConnectedOrConnecting
        }
    }

    override fun get(): List<Post> {
        // создаём запрос
        val request = Request.Builder()
            .url("${BASE_URL}/api/slow/posts")
            .build()

        return client.newCall(request)
            .execute()
            .use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code $response")
                val body = response.body?.string() ?: throw IOException("Empty response body")
                val listType = object : TypeToken<List<Post>>() {}.type
                gson.fromJson(body, listType)
            }
    }

    override fun likeByAsync(id: Long, callback: PostRepository.PostCallback<Post>) {
        val request = Request.Builder()
            .url("${BASE_URL}/api/slow/posts/$id/likes")
            .post(RequestBody.EMPTY)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onError(e)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    if (!response.isSuccessful) {
                        callback.onError(IOException("Неожиданный код $response"))
                        return
                    }
                    val body = response.body?.string() ?: throw IOException("Пустой ответ")
                    val post = gson.fromJson(body, Post::class.java)
                    dao.save(PostEntity.fromPost(post))
                    callback.onSuccess(post)
                } catch (e: Exception) {
                    callback.onError(e)
                } finally {
                    response.close()
                }
            }

        })
    }

    override fun likeById(id: Long): Post {
        val request = Request.Builder()
            .url("${BASE_URL}/api/slow/posts/$id/likes")
            .post(RequestBody.EMPTY)
            .build()

        //Используем пост с сервера для частичного обновления
        return client.newCall(request)
            .execute()
            .use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Unexpected code $response")
                }
                val body = response.body?.string() ?: throw IOException("Empty response body")
                gson.fromJson(body, Post::class.java)
            }
    }

//    override  fun shareById(id: Long) {
//        val request = Request.Builder()
//            .url("${BASE_URL}/api/slow/posts/$id/shares")
//            .post(RequestBody.EMPTY)
//            .build()
//
//        client.newCall(request)
//            .execute()
//            .use { response ->
//                if (!response.isSuccessful) {
//                    throw IOException("Unexpected code $response")
//                }
//            }
//    }

    override fun removeByAsync(id: Long, callback: PostRepository.PostCallback<Unit>) {
        val request = Request.Builder()
            .url("${BASE_URL}/api/slow/posts/$id")
            .delete()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onError(e)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    if (!response.isSuccessful) {
                        callback.onError(IOException("Неожиданный код $response"))
                        return
                    }
                    dao.removeById(id)
                    callback.onSuccess(Unit)
                } catch (e: Exception) {
                    callback.onError(e)
                } finally {
                    response.close()
                }
            }

        })
    }

    override fun removeById(id: Long) {
        // создаём запрос
        val request = Request.Builder()
            .url("${BASE_URL}/api/slow/posts/$id")
            .delete()
            .build()

        client.newCall(request)
            .execute()
            .use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Unexpected code $response")
                }
            }
    }

    override fun saveByAsync(post: Post, callback: PostRepository.PostCallback<Post>) {
        val request = Request.Builder()
            .url("${BASE_URL}/api/slow/posts")
            .post(gson.toJson(post).toRequestBody(jsonType))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onError(e)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    if (!response.isSuccessful) {
                        callback.onError(IOException("Неожиданный код $response"))
                        return
                    }
                    val body = response.body?.string() ?: throw IOException("Пустой ответ")
                    val savePost = gson.fromJson(body, Post::class.java)
                    dao.save(PostEntity.fromPost(savePost))
                    callback.onSuccess(savePost)
                } catch (e: Exception) {
                    callback.onError(e)
                } finally {
                    response.close()
                }
            }

        })
    }

    override fun save(post: Post): Post {
        // создаём запрос
        val request = Request.Builder()
            .url("${BASE_URL}/api/slow/posts")
            .post(gson.toJson(post).toRequestBody(jsonType))
            .build()

        return client.newCall(request)
            .execute()
            .use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Unexpected code $response")
                }
                val body = response.body?.string() ?: throw IOException("Empty response body")
                gson.fromJson(body, Post::class.java)
            }
    }
}