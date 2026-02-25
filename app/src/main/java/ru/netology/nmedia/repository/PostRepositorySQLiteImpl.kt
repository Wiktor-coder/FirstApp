package ru.netology.nmedia.repository

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import okio.IOException
import retrofit2.*
import ru.netology.nmedia.BuildConfig
import ru.netology.nmedia.api.PostApi
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

    // Вспомогательная функция для получения полного URL аватарки
    fun getAvatarUrl(avatarPath: String?): String? {
        return if (!avatarPath.isNullOrBlank()) {
            "${BuildConfig.BASE_URL}/avatars/${avatarPath}"
        } else null
    }

    override fun getLocalPosts(): List<Post> {
        return dao.getAll().map { it.toPost() }
    }

    // Вспомогательная функция для получения URL вложения
    fun getAttachmentUrl(attachment: Attachment?): String? {
        val url = when (attachment?.type) {
            AttachmentType.IMAGE -> "${BuildConfig.BASE_URL}/images/${attachment.url}"
            AttachmentType.VIDEO -> "${BuildConfig.BASE_URL}/video/${attachment.url}"
            AttachmentType.AUDIO -> "${BuildConfig.BASE_URL}/audio/${attachment.url}"
            null -> null
        }
        Log.d(
            "ATTACHMENT_DEBUG",
            "getAttachmentUrl: type=${attachment?.type}, input=${attachment?.url}, output=$url"
        )
        return url
    }

    override fun getAllAsync(callback: PostRepository.PostCallback<List<Post>>) {
        if (!isNetworkAvailable()) {
            // Если нет сети, возвращаем кэшированные данные
            callback.onSuccess(getLocalPosts())
            return
        }

        PostApi.service.getAll()
            .enqueue(object : Callback<List<Post>> {

                override fun onResponse(
                    call: Call<List<Post>?>,
                    response: Response<List<Post>?>
                ) {
                    if (response.isSuccessful) {
                        val posts = response.body() ?: emptyList()

                        // Логируем для отладки
                        Log.d("API_RESPONSE", "Posts response: $posts")

                        // Сохраняем в БД
                        val postEntities = posts.map { PostEntity.fromPost(it) }
                        dao.saveAll(postEntities)

                        callback.onSuccess(posts)
//                        callback.onSuccess(response.body().orEmpty())
                    } else {
                        val error = response.errorBody()?.string() ?: "Unknown error"
                        callback.onError(IOException("Неожиданный код ${response.code()}: $error"))

//                        callback.onError(RuntimeException(response.errorBody()?.string().orEmpty()))
                    }
                }

                override fun onFailure(
                    call: Call<List<Post>>,
                    t: Throwable
                ) {
                    callback.onError(t)
                }
            })
    }

    @SuppressLint("ServiceCast")
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

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
        return try {
            val response = PostApi.service.getAll().execute()
            if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                emptyList()
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            emptyList()
        }
    }

    override fun likeByAsync(id: Long, callback: PostRepository.PostCallback<Post>) {
        PostApi.service.likeById(id).enqueue(object : Callback<Post> {
            override fun onResponse(
                call: Call<Post?>,
                response: Response<Post?>
            ) {
                if (response.isSuccessful) {
                    val post = response.body()
                    if (post != null) {
                        dao.save(PostEntity.fromPost(post))
                        callback.onSuccess(post)
                    } else {
                        callback.onError(IOException("Empty response body"))
                    }
                } else {
                    val error = response.errorBody()?.string() ?: "Unknown error"
                    callback.onError(IOException("Неожиданный код ${response.code()}: $error"))
                }
            }

            override fun onFailure(
                call: Call<Post?>,
                t: Throwable
            ) {
                callback.onError(t)
            }

        })
    }

    override fun likeById(id: Long): Post? {
        return try {
            val response = PostApi.service.likeById(id).execute()
            if (response.isSuccessful) {
                val post = response.body()
                post?.let { dao.save(PostEntity.fromPost(it)) }
                post
            } else {
                null
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            null
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
        PostApi.service.delete(id).enqueue(object : Callback<Unit> {
            override fun onResponse(
                call: Call<Unit?>,
                response: Response<Unit?>
            ) {
                if (response.isSuccessful) {
                    dao.removeById(id)
                    callback.onSuccess(Unit)
                } else {
                    val error = response.errorBody()?.string() ?: "Unknown error"
                    callback.onError(IOException("Неожиданный код ${response.code()}: $error"))
                }
            }

            override fun onFailure(call: Call<Unit?>, t: Throwable) {
                callback.onError(t)
            }

        })
    }

    override fun removeById(id: Long) {
        try {
            val response = PostApi.service.delete(id).execute()
            if (response.isSuccessful) {
                dao.removeById(id)
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }

    }

    override fun saveByAsync(post: Post, callback: PostRepository.PostCallback<Post>) {
        PostApi.service.savePost(post).enqueue(object : Callback<Post> {
            override fun onResponse(
                call: Call<Post?>,
                response: Response<Post?>
            ) {
                if (response.isSuccessful) {
                    val savePost = response.body()
                    if (savePost != null) {
                        dao.save(PostEntity.fromPost(savePost))
                        callback.onSuccess(savePost)
                    } else {
                        callback.onError(IOException("Empty response body"))
                    }
                } else {
                    val error = response.errorBody()?.string() ?: "Unknown error"
                    callback.onError(IOException("Неожиданный код ${response.code()}: $error"))
                }
            }

            override fun onFailure(
                call: Call<Post?>,
                t: Throwable
            ) {
                callback.onError(t)
            }

        })
    }

    override fun save(post: Post): Post? {
        return try {
            val response = PostApi.service.savePost(post) //return client.newCall(request)
                .execute()
            if (response.isSuccessful) {
                val savePost = response.body()
                savePost?.let { dao.save(PostEntity.fromPost(it)) }
                savePost
            } else {
                null
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        }
    }
}