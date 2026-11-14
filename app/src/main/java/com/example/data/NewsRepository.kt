package com.example.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import com.example.data.local.ArticleDao
import com.example.data.local.ArticleEntity
import com.example.data.network.FeedFetchResult
import com.example.data.network.RssFeedService
import com.example.data.preferences.UserPreferencesRepository
import com.example.model.NewsArticle
import com.example.model.NewsSource
import com.example.model.NewsTopic
import com.example.model.SourcePriority
import com.example.model.UserNewsPreferences
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

class NewsRepository(
  private val context: Context,
  private val articleDao: ArticleDao,
  private val preferencesRepository: UserPreferencesRepository,
  private val rssFeedService: RssFeedService = RssFeedService(),
) {

  val preferencesFlow: Flow<UserNewsPreferences> = preferencesRepository.preferencesFlow

  val allArticlesFlow: Flow<List<NewsArticle>> =
    articleDao.observeAllArticles().map { list -> list.map { it.toDomain() } }

  val bookmarkedArticlesFlow: Flow<List<NewsArticle>> =
    articleDao.observeBookmarkedArticles().map { list -> list.map { it.toDomain() } }

  fun observeArticleById(id: String): Flow<NewsArticle?> =
    articleDao.observeArticleById(id).map { it?.toDomain() }

  suspend fun refreshFeeds(forceAllSources: Boolean = false): FeedFetchResult {
    val prefs = preferencesRepository.preferencesFlow.first()
    val sourcesToFetch =
      if (forceAllSources) NewsSource.entries.toSet() else prefs.enabledSources
    val fetchResult =
      rssFeedService.fetchAllFeeds(
        enabledSources = sourcesToFetch,
        localCity = prefs.localCity,
        preferredTopics = prefs.preferredTopics,
      )

    if (fetchResult.articles.isNotEmpty()) {
      val now = System.currentTimeMillis()
      val entities =
        fetchResult.articles.map { article -> ArticleEntity.fromDomain(article, now) }
      articleDao.upsertPreservingUserState(entities)
      // Prune unbookmarked stories older than 5 days
      articleDao.pruneStaleUnbookmarked(now - 5L * 24 * 60 * 60 * 1000)
    }
    return fetchResult
  }

  suspend fun toggleBookmark(article: NewsArticle) {
    val newState = !article.isBookmarked
    val timestamp = if (newState) System.currentTimeMillis() else null
    articleDao.setBookmarked(article.id, newState, timestamp)
  }

  suspend fun markAsRead(articleId: String) {
    articleDao.markAsRead(articleId)
  }

  suspend fun toggleSource(source: NewsSource, enabled: Boolean) {
    preferencesRepository.toggleSource(source, enabled)
  }

  suspend fun setSourcePriority(source: NewsSource, priority: SourcePriority) {
    preferencesRepository.setSourcePriority(source, priority)
  }

  suspend fun toggleTopic(topic: NewsTopic) {
    preferencesRepository.toggleTopic(topic)
  }

  suspend fun updateLocalCity(city: String) {
    val clean = city.trim()
    if (clean.isEmpty()) return
    preferencesRepository.setLocalCity(clean)
    articleDao.clearUnbookmarkedBySource(NewsSource.LOCAL.id)
    val localResult =
      rssFeedService.fetchAllFeeds(
        enabledSources = setOf(NewsSource.LOCAL),
        localCity = clean,
        preferredTopics = emptySet(),
      )
    if (localResult.articles.isNotEmpty()) {
      val now = System.currentTimeMillis()
      articleDao.upsertPreservingUserState(
        localResult.articles.map { ArticleEntity.fromDomain(it, now) }
      )
    }
  }

  suspend fun addKeyword(keyword: String) = preferencesRepository.addKeyword(keyword)

  suspend fun removeKeyword(keyword: String) = preferencesRepository.removeKeyword(keyword)

  @SuppressLint("MissingPermission")
  suspend fun detectCurrentCityName(): String? =
    withContext(Dispatchers.IO) {
      try {
        val locationManager =
          context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return@withContext null
        val providers = locationManager.getProviders(true)
        var bestLocation: android.location.Location? = null
        for (provider in providers) {
          val loc = locationManager.getLastKnownLocation(provider) ?: continue
          if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
            bestLocation = loc
          }
        }
        val location = bestLocation ?: return@withContext null
        val geocoder = Geocoder(context, Locale.US)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          suspendCancellableCoroutine { cont ->
            geocoder.getFromLocation(location.latitude, location.longitude, 1) { addresses ->
              val addr = addresses.firstOrNull()
              val formatted =
                listOfNotNull(addr?.locality ?: addr?.subAdminArea, addr?.adminArea)
                  .joinToString(", ")
                  .ifBlank { null }
              if (cont.isActive) cont.resume(formatted)
            }
          }
        } else {
          @Suppress("DEPRECATION")
          val addr = geocoder.getFromLocation(location.latitude, location.longitude, 1)?.firstOrNull()
          listOfNotNull(addr?.locality ?: addr?.subAdminArea, addr?.adminArea)
            .joinToString(", ")
            .ifBlank { null }
        }
      } catch (_: Exception) {
        null
      }
    }
}
