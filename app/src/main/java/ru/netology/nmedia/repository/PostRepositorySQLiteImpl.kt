package ru.netology.nmedia.repository

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import ru.netology.nmedia.utils.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
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

    // Flow для наблюдения за постами
    override fun observePosts(): Flow<List<Post>> {
        return dao.observeAll().map { entities ->
            entities.map { it.toPost() }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun getAll(): Result<List<Post>> = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            return@withContext Result.Success(getLocalPosts())
        }

        return@withContext try {
            val response = PostApi.service.getAll().execute()
            if (response.isSuccessful) {
                val posts = response.body() ?: emptyList()
                val postEntities = posts.map { PostEntity.fromPost(it) }
                dao.saveAll(postEntities)
                Result.Success(posts)
            } else {
                Result.Error("Ошибка ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Неизвестная ошибка", exception = e)
        }
    }

    override suspend fun likeById(id: Long): Result<Post> = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            return@withContext Result.Error("Нет подключения к интернету")
        }

        return@withContext try {
            val response = PostApi.service.likeById(id).execute()
            if (response.isSuccessful) {
                val post = response.body()
                if (post != null) {
                    dao.save(PostEntity.fromPost(post))
                    Result.Success(post)
                } else {
                    Result.Error("Пустой ответ от сервера")
                }
            } else {
                Result.Error("Ошибка ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Неизвестная ошибка", exception = e)
        }
    }

    override suspend fun unlikeById(id: Long): Result<Post> = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            return@withContext Result.Error("Нет подключения к интернету")
        }

        return@withContext try {
            val response = PostApi.service.unLikeById(id).execute()
            if (response.isSuccessful) {
                val post = response.body()
                if (post != null) {
                    dao.save(PostEntity.fromPost(post))
                    Result.Success(post)
                } else {
                    Result.Error("Пустой ответ от сервера")
                }
            } else {
                Result.Error("Ошибка ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Неизвестная ошибка", exception = e)
        }
    }

    override suspend fun removeById(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            return@withContext Result.Error("Нет подключения к интернету")
        }

        return@withContext try {
            val response = PostApi.service.delete(id).execute()
            if (response.isSuccessful) {
                dao.removeById(id)
                Result.Success(Unit)
            } else {
                Result.Error("Ошибка ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Неизвестная ошибка", exception = e)
        }
    }

    override suspend fun save(post: Post): Result<Post> = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!isNetworkAvailable()) {
                // Сохраняем локально для офлайн режима
                val savedPost = post.copy(id = System.currentTimeMillis()) // Временный ID
                dao.save(PostEntity.fromPost(savedPost))
                Result.Success(savedPost)
            } else {
                val response = PostApi.service.savePost(post).execute()
                if (response.isSuccessful) {
                    val savedPost = response.body()
                    if (savedPost != null) {
                        dao.save(PostEntity.fromPost(savedPost))
                        Result.Success(savedPost)
                    } else {
                        Result.Error("Пустой ответ от сервера")
                    }
                } else {
                    Result.Error("Ошибка ${response.code()}")
                }
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Неизвестная ошибка", exception = e)
        }
    }

    // Вспомогательные функции
    fun getLocalPosts(): List<Post> = dao.getAll().map { it.toPost() }

    fun getAvatarUrl(avatarPath: String?): String? {
        return if (!avatarPath.isNullOrBlank()) {
            "${BuildConfig.BASE_URL}/avatars/${avatarPath}"
        } else null
    }

    fun getAttachmentUrl(attachment: Attachment?): String? {
        return when (attachment?.type) {
            AttachmentType.IMAGE -> {
                // Проверяем, не содержит ли URL уже полный путь
                val url = attachment.url
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    url
                } else {
                    "${BuildConfig.BASE_URL}/media/${url}"
                }
            }
            AttachmentType.VIDEO -> {
                if (attachment.url.startsWith("http://") || attachment.url.startsWith("https://")) {
                    attachment.url
                } else {
                    "${BuildConfig.BASE_URL}/video/${attachment.url}"
                }
            }
            AttachmentType.AUDIO -> {
                if (attachment.url.startsWith("http://") || attachment.url.startsWith("https://")) {
                    attachment.url
                } else {
                    "${BuildConfig.BASE_URL}/audio/${attachment.url}"
                }
            }
            null -> null
        }
    }

    @SuppressLint("ServiceCast")
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

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
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            return networkInfo.isConnectedOrConnecting
        }
    }
}
