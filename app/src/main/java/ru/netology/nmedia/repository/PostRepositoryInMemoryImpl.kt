package ru.netology.nmedia.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ru.netology.nmedia.dto.Post

class PostRepositoryInMemoryImpl: PostRepository {
    private var defaultPost = Post(
        1,
        "Нетология. Университет интернет-профессий будущего",
        "21 мая в 18:36",
        "Привет, это новая Нетология! Когда-то Нетология начиналась с интенсивов по онлайн-маркетингу. Затем появились курсы по дизайну, разработке, аналитике и управлению. Мы растём сами и помогаем расти студентам: от новичков до уверенных профессионалов. Но самое важное остаётся с нами: мы верим, что в каждом уже есть сила, которая заставляет хотеть больше, целиться выше, бежать быстрее. Наша миссия — помочь встать на путь роста и начать цепочку перемен → http://netolo.gy/fyb",
        85,
        8,
        likedByMe = false,
    )

    private val data = MutableLiveData(defaultPost)

    override fun get(): LiveData<Post> = data

    override fun like() {
        defaultPost = defaultPost.copy(likedByMe = !defaultPost.likedByMe,
            likeCount = if (defaultPost.likedByMe) defaultPost.likeCount - 1
            else defaultPost.likeCount + 1)
        data.value = defaultPost
    }

    override fun share() {
        defaultPost = defaultPost.copy(shareCount = defaultPost.shareCount + 1)
        data.value = defaultPost
    }

}