package ru.netology.nmedia.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.dto.Post

class PostRepositorySQLiteImpl(private val dao: PostDao) : PostRepository {

    private val posts = MutableLiveData<List<Post>>()

    init {
        refresh()
    }

    override fun get(): LiveData<List<Post>> = posts

    override fun likeById(id: Long) {
        dao.likeById(id)
        refresh()
    }

    override fun shareById(id: Long) {
        dao.shareById(id)
        refresh()
    }

    override fun removeById(id: Long) {
        dao.removeById(id)
        refresh()
    }

    override fun save(post: Post) {
        dao.save(post)
        refresh()
    }

    private fun refresh() {
        val post = dao.getAll()
        posts.postValue(post)
    }

    companion object {
        fun newInstance(context: Context): PostRepositorySQLiteImpl {
            val db = AppDb.getInstance(context)
            return PostRepositorySQLiteImpl(db.postDao)
        }
    }

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