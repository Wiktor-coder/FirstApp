package ru.netology.nmedia.repository

import java.util.concurrent.TimeUnit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import ru.netology.nmedia.dto.Post

class PostRepositorySQLiteImpl : PostRepository {
    // для получения списка
    private val postListType = object : TypeToken<List<Post>>() {}.type

    companion object {
        const val BASE_URL = "http://10.0.2.2:9999"

        val jsonType = "application/json".toMediaType()
    }

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()

    override  fun get(): List<Post> {
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

    override  fun likeById(id: Long): Post {
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

    override  fun removeById(id: Long) {
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

    override  fun save(post: Post): Post {
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