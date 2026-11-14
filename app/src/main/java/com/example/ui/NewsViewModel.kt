package com.example.ui

import android.app.Application
import android.text.format.DateUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.NewsRepository
import com.example.data.local.PulseWireDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.domain.PersonalizationEngine
import com.example.model.NewsArticle
import com.example.model.NewsSource
import com.example.model.NewsTopic
import com.example.model.SourcePriority
import com.example.model.UserNewsPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface FeedSyncState {
  data object InitialLoading : FeedSyncState

  data object Ready : FeedSyncState

  data class PartialOrOffline(val failedSources: Set<NewsSource>) : FeedSyncState
}

data class NewsUiState(
  val feedArticles: List<NewsArticle> = emptyList(),
  val allArticlesCountBySource: Map<NewsSource, Int> = emptyMap(),
  val bookmarkedArticles: List<NewsArticle> = emptyList(),
  val preferences: UserNewsPreferences = UserNewsPreferences(),
  val selectedSourceFilter: NewsSource? = null,
  val selectedTopicFilter: NewsTopic? = null,
  val searchQuery: String = "",
  val isSearchExpanded: Boolean = false,
  val isRefreshing: Boolean = false,
  val syncState: FeedSyncState = FeedSyncState.InitialLoading,
  val selectedArticle: NewsArticle? = null,
  val statusMessage: String? = null,
  val isDetectingCity: Boolean = false,
)

private data class FilterState(
  val selectedSource: NewsSource? = null,
  val selectedTopic: NewsTopic? = null,
  val searchQuery: String = "",
  val isSearchExpanded: Boolean = false,
  val isRefreshing: Boolean = false,
  val syncState: FeedSyncState = FeedSyncState.InitialLoading,
  val selectedArticleId: String? = null,
  val statusMessage: String? = null,
  val isDetectingCity: Boolean = false,
)

class NewsViewModel(application: Application) : AndroidViewModel(application) {

  private val repository: NewsRepository =
    NewsRepository(
      context = application,
      articleDao = PulseWireDatabase.getInstance(application).articleDao(),
      preferencesRepository = UserPreferencesRepository(application),
    )

  private val filterState = MutableStateFlow(FilterState())

  val uiState: StateFlow<NewsUiState> =
    combine(
        repository.allArticlesFlow,
        repository.bookmarkedArticlesFlow,
        repository.preferencesFlow,
        filterState,
      ) { allArticles, bookmarked, prefs, filters ->
        val ranked =
          PersonalizationEngine.rankForYouFeed(
            articles = allArticles,
            preferences = prefs,
            sourceFilter = filters.selectedSource,
            topicFilter = filters.selectedTopic,
            searchQuery = filters.searchQuery,
          )

        val countsBySource =
          NewsSource.entries.associateWith { src ->
            allArticles.count { art ->
              art.source == src &&
                (src != NewsSource.LOCAL ||
                  art.localEditionCity.isNullOrBlank() ||
                  art.localEditionCity.equals(prefs.localCity, ignoreCase = true))
            }
          }

        val selectedArticle =
          filters.selectedArticleId?.let { id ->
            ranked.firstOrNull { it.id == id }
              ?: allArticles.firstOrNull { it.id == id }
              ?: bookmarked.firstOrNull { it.id == id }
          }

        val effectiveSyncState =
          if (allArticles.isNotEmpty() && filters.syncState == FeedSyncState.InitialLoading) {
            FeedSyncState.Ready
          } else {
            filters.syncState
          }

        NewsUiState(
          feedArticles = ranked,
          allArticlesCountBySource = countsBySource,
          bookmarkedArticles = bookmarked,
          preferences = prefs,
          selectedSourceFilter = filters.selectedSource,
          selectedTopicFilter = filters.selectedTopic,
          searchQuery = filters.searchQuery,
          isSearchExpanded = filters.isSearchExpanded,
          isRefreshing = filters.isRefreshing,
          syncState = effectiveSyncState,
          selectedArticle = selectedArticle,
          statusMessage = filters.statusMessage,
          isDetectingCity = filters.isDetectingCity,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NewsUiState(),
      )

  init {
    refreshFeeds(forceAllSources = true)
  }

  fun refreshFeeds(forceAllSources: Boolean = false) {
    if (filterState.value.isRefreshing) return
    viewModelScope.launch {
      filterState.update { it.copy(isRefreshing = true) }
      val result = repository.refreshFeeds(forceAllSources = forceAllSources)
      val nextSync =
        if (result.failedSources.isEmpty()) {
          FeedSyncState.Ready
        } else {
          FeedSyncState.PartialOrOffline(result.failedSources)
        }
      filterState.update {
        it.copy(
          isRefreshing = false,
          syncState = nextSync,
          statusMessage =
            if (result.articles.isEmpty() && result.failedSources.isNotEmpty()) {
              "Could not reach live feeds. Check connection and pull to retry."
            } else {
              null
            },
        )
      }
    }
  }

  fun selectSourceFilter(source: NewsSource?) {
    filterState.update { it.copy(selectedSource = source) }
  }

  fun selectTopicFilter(topic: NewsTopic?) {
    filterState.update { it.copy(selectedTopic = topic) }
  }

  fun toggleSearchExpanded(expanded: Boolean) {
    filterState.update {
      it.copy(
        isSearchExpanded = expanded,
        searchQuery = if (!expanded) "" else it.searchQuery,
      )
    }
  }

  fun updateSearchQuery(query: String) {
    filterState.update { it.copy(searchQuery = query) }
  }

  fun openArticle(article: NewsArticle) {
    filterState.update { it.copy(selectedArticleId = article.id) }
    viewModelScope.launch { repository.markAsRead(article.id) }
  }

  fun closeArticle() {
    filterState.update { it.copy(selectedArticleId = null) }
  }

  fun toggleBookmark(article: NewsArticle) {
    viewModelScope.launch { repository.toggleBookmark(article) }
  }

  fun toggleSource(source: NewsSource, enabled: Boolean) {
    viewModelScope.launch {
      repository.toggleSource(source, enabled)
      if (enabled) {
        refreshFeeds(forceAllSources = false)
      }
    }
  }

  fun setSourcePriority(source: NewsSource, priority: SourcePriority) {
    viewModelScope.launch { repository.setSourcePriority(source, priority) }
  }

  fun toggleTopic(topic: NewsTopic) {
    viewModelScope.launch { repository.toggleTopic(topic) }
  }

  fun updateLocalCity(city: String) {
    val clean = city.trim()
    if (clean.isEmpty()) return
    viewModelScope.launch {
      filterState.update { it.copy(isRefreshing = true, statusMessage = "Updating local edition for $clean…") }
      repository.updateLocalCity(clean)
      filterState.update { it.copy(isRefreshing = false, statusMessage = "Local edition set to $clean") }
    }
  }

  fun addKeyword(keyword: String) {
    viewModelScope.launch { repository.addKeyword(keyword) }
  }

  fun removeKeyword(keyword: String) {
    viewModelScope.launch { repository.removeKeyword(keyword) }
  }

  fun detectCityFromLocation() {
    viewModelScope.launch {
      filterState.update { it.copy(isDetectingCity = true) }
      val detected = repository.detectCurrentCityName()
      filterState.update { it.copy(isDetectingCity = false) }
      if (detected != null) {
        updateLocalCity(detected)
      } else {
        filterState.update {
          it.copy(
            statusMessage =
              "Couldn't resolve city automatically. Pick or type your city below."
          )
        }
      }
    }
  }

  fun showMessage(message: String) {
    filterState.update { it.copy(statusMessage = message) }
  }

  fun clearStatusMessage() {
    filterState.update { it.copy(statusMessage = null) }
  }

  companion object {
    fun formatRelativeTime(publishedAtMillis: Long): String {
      val now = System.currentTimeMillis()
      val diff = (now - publishedAtMillis).coerceAtLeast(0L)
      return when {
        diff < DateUtils.MINUTE_IN_MILLIS -> "Just now"
        diff < DateUtils.HOUR_IN_MILLIS -> "${diff / DateUtils.MINUTE_IN_MILLIS}m ago"
        diff < DateUtils.DAY_IN_MILLIS -> "${diff / DateUtils.HOUR_IN_MILLIS}h ago"
        else ->
          DateUtils.getRelativeTimeSpanString(
              publishedAtMillis,
              now,
              DateUtils.DAY_IN_MILLIS,
              DateUtils.FORMAT_ABBREV_RELATIVE,
            )
            .toString()
      }
    }
  }
}
