package ru.netology.nmedia.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.netology.nmedia.databinding.CardPostBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.viewmodel.PostViewModel

typealias likeClickListener = (post: Post) -> Unit
typealias shareClickListener = (post: Post) -> Unit

class PostAdapter(
    private val likeClickListener: likeClickListener,
    private val shareClickListener: shareClickListener
) : ListAdapter<Post, PostViewHolder>(PostDiffItemCallback()) {
//    var data: List<Post> = emptyList()
//        set(value) {
//            field = value
//            notifyDataSetChanged()
//        }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PostViewHolder {

        val binding = CardPostBinding.inflate(LayoutInflater.from(parent.context),
            parent,
            false)
        return PostViewHolder(binding, likeClickListener, shareClickListener)
    }

    override fun onBindViewHolder(
        holder: PostViewHolder,
        position: Int
    ) {
        holder.bind(getItem(position))    //data[position])
    }

//    override fun getItemCount(): Int = data.size
}