package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.NewsArticle
import com.example.model.NewsSource
import com.example.model.NewsTopic

@Entity(tableName = "articles")
data class ArticleEntity(
  @PrimaryKey val id: String,
  val title: String,
  val summary: String,
  val url: String,
  val imageUrl: String?,
  val sourceId: String,
  val publisherSubName: String?,
  val author: String?,
  val topicId: String,
  val publishedAtMillis: Long,
  val isBookmarked: Boolean = false,
  val isRead: Boolean = false,
  val bookmarkedAtMillis: Long? = null,
  val localEditionCity: String? = null,
  val fetchedAtMillis: Long = System.currentTimeMillis(),
) {
  fun toDomain(): NewsArticle =
    NewsArticle(
      id = id,
      title = title,
      summary = summary,
      url = url,
      imageUrl = imageUrl,
      source = NewsSource.fromId(sourceId),
      publisherSubName = publisherSubName,
      author = author,
      topic = NewsTopic.fromId(topicId),
      publishedAtMillis = publishedAtMillis,
      isBookmarked = isBookmarked,
      isRead = isRead,
      bookmarkedAtMillis = bookmarkedAtMillis,
      localEditionCity = localEditionCity,
    )

  companion object {
    fun fromDomain(article: NewsArticle, fetchedAtMillis: Long): ArticleEntity =
      ArticleEntity(
        id = article.id,
        title = article.title,
        summary = article.summary,
        url = article.url,
        imageUrl = article.imageUrl,
        sourceId = article.source.id,
        publisherSubName = article.publisherSubName,
        author = article.author,
        topicId = article.topic.id,
        publishedAtMillis = article.publishedAtMillis,
        isBookmarked = article.isBookmarked,
        isRead = article.isRead,
        bookmarkedAtMillis = article.bookmarkedAtMillis,
        localEditionCity = article.localEditionCity,
        fetchedAtMillis = fetchedAtMillis,
      )
  }
}
