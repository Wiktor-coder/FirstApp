package ru.netology.nmedia.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ru.netology.nmedia.dto.Post

class PostRepositoryInMemoryImpl : PostRepository {
    private var defaultPosts = List(10_000) { counter ->
        Post(
            id = +1L,
            author = "Нетология. Университет интернет-профессий будущего",
            published = "22 мая в 18:36",
            content = "Пост № ${counter} Привет, это новая Нетология! Когда-то Нетология начиналась с интенсивов по онлайн-маркетингу. Затем появились курсы по дизайну, разработке, аналитике и управлению. Мы растём сами и помогаем расти студентам: от новичков до уверенных профессионалов. Но самое важное остаётся с нами: мы верим, что в каждом уже есть сила, которая заставляет хотеть больше, целиться выше, бежать быстрее. Наша миссия — помочь встать на путь роста и начать цепочку перемен → http://netolo.gy/fyb",
            likeCount = 4,
            shareCount = 5,
            likedByMe = false,
        )
    }

    private val data = MutableLiveData(defaultPosts)

    override fun get(): LiveData<List<Post>> = data

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

}