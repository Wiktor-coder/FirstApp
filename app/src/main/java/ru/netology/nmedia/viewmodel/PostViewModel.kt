package ru.netology.nmedia.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.repository.PostRepositoryInMemoryImpl

class PostViewModel : ViewModel() {
    private val repository: PostRepository = PostRepositoryInMemoryImpl()

    fun get(): LiveData<List<Post>> = repository.get()

    fun likeById(id: Long) {
        repository.likeById(id)
    }

    fun shareById(id: Long) {
        repository.shareById(id)
    }
}