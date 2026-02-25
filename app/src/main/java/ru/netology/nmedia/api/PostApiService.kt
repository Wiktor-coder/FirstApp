package ru.netology.nmedia.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import ru.netology.nmedia.BuildConfig
import ru.netology.nmedia.dto.Post
import java.util.concurrent.TimeUnit

const val BASE_URL = "${BuildConfig.BASE_URL}/api/slow/"

private val client = OkHttpClient.Builder()
    .addInterceptor(HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY // включёные логи для дэбаг
        } else {
            HttpLoggingInterceptor.Level.NONE // выключаем логи для релиза
        }
    }) // перехватчик логер
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)  // Добавьте readTimeout
    .writeTimeout(30, TimeUnit.SECONDS) // Добавьте writeTimeout
    .build()

// интегрируем client in retrofit
private val retrofit = Retrofit.Builder()
    .baseUrl(BASE_URL)
    .addConverterFactory(GsonConverterFactory.create())
    .client(client)
    .build()

interface PostApiService {
    @GET("posts")
    fun getAll(): Call<List<Post>>

    @POST("posts/{id}/likes")
    fun likeById(@Path("id") id: Long): Call<Post>

    @POST("posts")
    fun savePost(@Body post: Post): Call<Post>

    @DELETE("posts/{id}")
    fun delete(@Path("id") postId: Long): Call<Unit>

}

// по котлиновски
object PostApi {
    val service by lazy {
        retrofit.create<PostApiService>()
    }
}
// альтернатива
//object PostApi {
//    val service: PostApiService by lazy {
//        retrofit.create(PostApiService::class.java)
//    }
//}