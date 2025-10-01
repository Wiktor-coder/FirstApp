package ru.netology.nmedia.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ru.netology.nmedia.dto.Post

class PostRepositoryInMemoryImpl : PostRepository {
    private var nextId = 1L
    private var defaultPosts = listOf(
        Post(
            id = nextId++,
            author = "Нетология. Университет интернет-профессий будущего",
            published = "22 мая в 18:36",
            content = "Пост № ${nextId - 1} Привет, это новая Нетология! Когда-то Нетология начиналась с интенсивов по онлайн-маркетингу. Затем появились курсы по дизайну, разработке, аналитике и управлению. Мы растём сами и помогаем расти студентам: от новичков до уверенных профессионалов. Но самое важное остаётся с нами: мы верим, что в каждом уже есть сила, которая заставляет хотеть больше, целиться выше, бежать быстрее. Наша миссия — помочь встать на путь роста и начать цепочку перемен → http://netolo.gy/fyb",
            likeCount = 4,
            shareCount = 5,
            likedByMe = false,

        ),
        Post(
            id = nextId++,
            author = "Нетология. Университет интернет-профессий будущего",
            published = "22 мая в 18:36",
            content = "Пост № ${nextId - 1} Привет, это новая Нетология! Когда-то Нетология начиналась с интенсивов по онлайн-маркетингу. Затем появились курсы по дизайну, разработке, аналитике и управлению. Мы растём сами и помогаем расти студентам: от новичков до уверенных профессионалов. Но самое важное остаётся с нами: мы верим, что в каждом уже есть сила, которая заставляет хотеть больше, целиться выше, бежать быстрее. Наша миссия — помочь встать на путь роста и начать цепочку перемен → http://netolo.gy/fyb",
            likeCount = 4,
            shareCount = 5,
            likedByMe = false,
            video = "https://rutube.ru/video/6550a91e7e523f9503bed47e4c46d0cb"
        )
    ).reversed()


    //id для новых постов
//    private var nextId = defaultPosts.first().id + 1

    private val data = MutableLiveData(defaultPosts)

    override fun get(): LiveData<List<Post>> = data

    //лайк
    override fun likeById(id: Long) {
        defaultPosts = defaultPosts.map { post ->
            if (post.id == id) {
                post.copy(
                    likedByMe = !post.likedByMe,
                    likeCount = if (post.likedByMe) post.likeCount - 1
                    else post.likeCount + 1
                )
            } else {
                post
            }

        }
        data.value = defaultPosts
    }

    //поделится
    override fun shareById(id: Long) {
        defaultPosts = defaultPosts.map { post ->
            if (post.id == id) {
                post.copy(shareCount = post.shareCount + 1)
            } else {
                post
            }
        }
        data.value = defaultPosts
    }

    //удаление
    override fun removeById(id: Long) {
        data.value =
            data.value?.filter { it.id != id }  //.filterNot { it.id == id } как альтернатива

    }

    //создание новго поста, else - редактирование поста
    override fun save(post: Post) {
        if (post.id == 0L) {
            data.value = listOf(post.copy(id = nextId++)) + data.value.orEmpty()
        } else {
            data.value = data.value?.map {
                if (it.id == post.id) {
                    post
                } else {
                    it
                }
            }
        }
    }
}