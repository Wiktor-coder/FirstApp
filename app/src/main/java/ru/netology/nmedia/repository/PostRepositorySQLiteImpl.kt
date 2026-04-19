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
import ru.netology.nmedia.api.PostApiService
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dto.Attachment
import ru.netology.nmedia.dto.AttachmentType
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostEntity

class PostRepositorySQLiteImpl(
    private val context: Context,
    private val dao: PostDao,
    private val postApiService: PostApiService,
    private val appAuth: AppAuth,
) : PostRepository {

    // Flow для наблюдения за постами
    override fun observePosts(): Flow<List<Post>> {
        return dao.observeAll().map { entities ->
            val currentUserId = appAuth.authStateFlow.value.id
            entities.map { entity ->
                val post = entity.toPost()
                post.copy(
                    ownedByMe = post.authorId == currentUserId && currentUserId != 0L
                )
            }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun getAll(): Result<List<Post>> = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            return@withContext Result.Success(getLocalPosts())
        }

        return@withContext try {
            val response = postApiService.getAll().execute()
            if (response.isSuccessful) {
                val posts = response.body() ?: emptyList()

                // Получаем текущего пользователя
                val currentUserId = appAuth.authStateFlow.value.id

                // Устанавливаем ownedByMe на основе текущего пользователя
                val postsWithOwnership = posts.map { post ->
                    post.copy(
                        ownedByMe = post.authorId == currentUserId && currentUserId != 0L
                    )
                }

                val postEntities = posts.map { PostEntity.fromPost(it) }
                dao.saveAll(postEntities)
                Result.Success(postsWithOwnership)
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
            val response = postApiService.likeById(id).execute()
            if (response.isSuccessful) {
                val post = response.body()
                if (post != null) {
                    // Сохраняем ownedByMe
                    val currentUserId = appAuth.authStateFlow.value.id
                    val postWithOwnership = post.copy(
                        ownedByMe = post.authorId == currentUserId && currentUserId != 0L
                    )
                    dao.save(PostEntity.fromPost(postWithOwnership))
                    Result.Success(postWithOwnership)
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
            val response = postApiService.unLikeById(id).execute()
            if (response.isSuccessful) {
                val post = response.body()
                if (post != null) {
                    val currentUserId = appAuth.authStateFlow.value.id
                    val postWithOwnership = post.copy(
                        ownedByMe = post.authorId == currentUserId && currentUserId != 0L
                    )
                    dao.save(PostEntity.fromPost(postWithOwnership))
                    Result.Success(postWithOwnership)
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
            val response = postApiService.delete(id).execute()
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
                val currentUserId = appAuth.authStateFlow.value.id
                // Сохраняем локально для офлайн режима
                val savedPost = post.copy(
                    id = System.currentTimeMillis(),
                    ownedByMe = post.authorId == currentUserId && currentUserId != 0L
                    ) // Временный ID
                dao.save(PostEntity.fromPost(savedPost))
                Result.Success(savedPost)
            } else {
                val response =postApiService.savePost(post).execute()
                if (response.isSuccessful) {
                    val savedPost = response.body()
                    if (savedPost != null) {
                        val currentUserId = appAuth.authStateFlow.value.id
                        val postWithOwnership = savedPost.copy(
                            ownedByMe = savedPost.authorId == currentUserId && currentUserId != 0L
                        )
                        dao.save(PostEntity.fromPost(postWithOwnership))
                        Result.Success(postWithOwnership)
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
    override fun getLocalPosts(): List<Post> {
        val currentUserId = appAuth.authStateFlow.value.id
        return dao.getAll().map { entity ->
            val post = entity.toPost()
            post.copy(
                ownedByMe = post.authorId == currentUserId && currentUserId != 0L
            )
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
