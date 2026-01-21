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

class PostRepositorySQLiteImpl: PostRepository {
    // для получения списка
    private val postListType = object : TypeToken<List<Post>>() {}.type
    companion object{
        const val BASE_URL = "http://10.0.2.2:9999"

        val jsonType = "application/json".toMediaType()
    }

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun get(): List<Post> {
        // создаём запрос
        val request = Request.Builder()
            .url("${ BASE_URL}/api/slow/posts")
            .build()

        return client.newCall(request)
            .execute()
            .use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code $response")
                val body = response.body?.string() ?: throw IOException("Empty response body")
                val listType = object : TypeToken<List<Post>>(){}.type
                gson.fromJson(body, listType)
            }
//        val call = client.newCall(request)
//        val response = call.execute()
//        val stringBody = response.body.string()
//        return gson.fromJson(stringBody,typeToken.type)
    }

//    private val posts = MutableLiveData<List<Post>>()
//
//    init {
//        refresh()
//    }

//    override fun get(): List<Post> = dao.getAll().map { posts ->
//        posts.map { postEntity ->
//                postEntity.toPost()
//        }
//    }

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

//        val call = client.newCall(request)
//        val response = call.execute()
//        if (!response.isSuccessful) {
//            throw IOException("Unexpected code $response")
//        }
//        response.close()

        //dao.likeById(id)
//        refresh()
    }

    override fun shareById(id: Long) {
       // dao.shareById(id)
//        refresh()
    }

    override fun removeById(id: Long) {
        // создаём запрос
        val request = Request.Builder()
            .url("${ BASE_URL}/api/slow/posts/$id")
            .delete()
            .build()

        client.newCall(request)
            .execute()
            .use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Unexpected code $response")
                }
            }

        //dao.removeById(id)
//        refresh()
    }

    override fun save(post: Post): Post {
        // создаём запрос
        val request = Request.Builder()
            .url("${ BASE_URL}/api/slow/posts")
            .post(gson.toJson(post).toRequestBody(jsonType))
            .build()

        val call = client.newCall(request)
        val response = call.execute()
        val stringBody = response.body.string()
        return gson.fromJson(stringBody, Post::class.java)

        //dao.save(PostEntity.fromPost(post))
//        refresh()
    }

//    private fun refresh() {
//        val post = dao.getAll()
//        posts.postValue(post)
//    }

//    companion object {
//        fun newInstance(context: Context): PostRepositorySQLiteImpl {
//            val db = AppDb.getInstance(context)
//            return PostRepositorySQLiteImpl(db.postDao)
//        }
//    }

//    //private val preferences = context.getSharedPreferences("posts", Context.MODE_PRIVATE)
//
//
//    //сохранять всё в deaultPost через data.value сохранение не работает, только обновляем
//    private var defaultPosts = readPosts()
//        set(value) {
//            field = value
//            sync()
//        }
//    //находим максимальный id постов и увеличиваем его
//    private var nextId = defaultPosts.maxByOrNull { it.id }?.id?.plus(1) ?: 0L
//    private val data = MutableLiveData(defaultPosts)
//
//    override fun get(): LiveData<List<Post>> = data
//
//    //лайк
//    override fun likeById(id: Long) {
//        defaultPosts = defaultPosts.map { post ->
//            if (post.id == id) {
//                post.copy(
//                    likedByMe = !post.likedByMe,
//                    likeCount = if (post.likedByMe) post.likeCount - 1
//                    else post.likeCount + 1
//                )
//            } else {
//                post
//            }
//
//        }
//        data.value = defaultPosts
//    }
//
//    //поделится
//    override fun shareById(id: Long) {
//        defaultPosts = defaultPosts.map { post ->
//            if (post.id == id) {
//                post.copy(shareCount = post.shareCount + 1)
//            } else {
//                post
//            }
//        }
//        data.value = defaultPosts
//    }
//
//    //удаление
//    override fun removeById(id: Long) {
//        defaultPosts = defaultPosts.filter { it.id != id }
//        data.value = defaultPosts
//            //data.value?.filter { it.id != id }  //.filterNot { it.id == id } как альтернатива
//
//    }
//
//    //создание новго поста, else - редактирование поста
//    override fun save(post: Post) {
//        if (post.id == 0L) {
//            val newPost = post.copy(id = nextId++)
//            defaultPosts = listOf(newPost) + defaultPosts
//            data.value = defaultPosts //listOf(post.copy(id = nextId++)) + data.value.orEmpty()
//        } else {
//            defaultPosts = defaultPosts.map {
//                if (it.id == post.id) {
//                    post
//                } else {
//                    it
//                }
//            }
//            data.value = defaultPosts
//        }
//    }
//
//    //чтение преобразование из строчки в список обьектов
//    //filesDir локальный файл не относится к кэш
//    private fun readPosts(): List<Post> {
//        val file = context.filesDir.resolve(POST_FILE)
//        return if (file.exists()) {
//            //file.reader().buffered
//            context.openFileInput(POST_FILE).bufferedReader().use {
//                gson.fromJson(it, postsType)
//            }
//        } else {
//            emptyList()
//        }
//    }
//
//    //записываем наши посты в gson
//    //MODE_PRIVATE для перезаписывания если был до этого
//    //it.write записываем в наш поток
//    private fun sync() {
//        context.openFileOutput(POST_FILE, Context.MODE_PRIVATE).bufferedWriter().use {
//           it.write(gson.toJson(defaultPosts))
//        }
//    }
//    companion object {
//        const val POST_FILE = "posts.json"
//        val gson = Gson()
//        val postsType: Type = TypeToken.getParameterized(List::class.java, Post::class.java).type
//    }
}