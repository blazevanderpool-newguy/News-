package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {
  @Query("SELECT * FROM articles ORDER BY publishedAtMillis DESC")
  fun observeAllArticles(): Flow<List<ArticleEntity>>

  @Query("SELECT * FROM articles WHERE isBookmarked = 1 ORDER BY bookmarkedAtMillis DESC")
  fun observeBookmarkedArticles(): Flow<List<ArticleEntity>>

  @Query("SELECT * FROM articles WHERE id = :id LIMIT 1")
  fun observeArticleById(id: String): Flow<ArticleEntity?>

  @Query("SELECT * FROM articles WHERE id IN (:ids)")
  suspend fun getByIds(ids: List<String>): List<ArticleEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(articles: List<ArticleEntity>)

  @Query(
    "UPDATE articles SET isBookmarked = :bookmarked, bookmarkedAtMillis = :bookmarkedAt WHERE id = :id"
  )
  suspend fun setBookmarked(id: String, bookmarked: Boolean, bookmarkedAt: Long?)

  @Query("UPDATE articles SET isRead = 1 WHERE id = :id")
  suspend fun markAsRead(id: String)

  @Query("DELETE FROM articles WHERE sourceId = :sourceId AND isBookmarked = 0")
  suspend fun clearUnbookmarkedBySource(sourceId: String)

  @Query("DELETE FROM articles WHERE isBookmarked = 0 AND fetchedAtMillis < :olderThanMillis")
  suspend fun pruneStaleUnbookmarked(olderThanMillis: Long)

  @Transaction
  suspend fun upsertPreservingUserState(incoming: List<ArticleEntity>) {
    if (incoming.isEmpty()) return
    val existingMap =
      incoming
        .chunked(500)
        .flatMap { chunk -> getByIds(chunk.map { it.id }) }
        .associateBy { it.id }

    val merged =
      incoming.map { fresh ->
        val existing = existingMap[fresh.id]
        if (existing != null) {
          fresh.copy(
            imageUrl = fresh.imageUrl ?: existing.imageUrl,
            isBookmarked = existing.isBookmarked,
            isRead = existing.isRead,
            bookmarkedAtMillis = existing.bookmarkedAtMillis,
          )
        } else {
          fresh
        }
      }
    insertAll(merged)
  }
}
