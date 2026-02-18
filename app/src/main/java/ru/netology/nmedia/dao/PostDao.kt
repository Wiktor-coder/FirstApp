package ru.netology.nmedia.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ru.netology.nmedia.entity.PostEntity

@Dao
interface PostDao {
    @Query("SELECT * FROM PostEntity ORDER BY id DESC")
    fun getAllLive(): LiveData<List<PostEntity>>

    @Query("SELECT * FROM PostEntity ORDER BY id DESC")
    fun getAll():List<PostEntity> //сразу возвращаем LiveData, подписку на изменения таблицы

    @Upsert //
    fun save(post: PostEntity): Long

    @Upsert
    fun saveAll(posts: List<PostEntity>)

    @Query(
        """ UPDATE PostEntity SET
             likeCount = likeCount + CASE WHEN likedByMe THEN -1 ELSE 1 END,
             likedByMe = CASE WHEN likedByMe THEN 0 ELSE 1 END
             WHERE id = :id;
             """)
    fun likeById(id: Long)

    @Query(""" UPDATE PostEntity SET
        shareCount = shareCount + 1
        WHERE id = :id;
    """)
    fun shareById(id: Long) //поделится

    @Query("DELETE FROM PostEntity WHERE id = :id")
    fun removeById(id: Long) //удаление
}