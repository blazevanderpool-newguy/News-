package com.example.domain

import com.example.model.NewsArticle
import com.example.model.NewsSource
import com.example.model.NewsTopic
import com.example.model.SourcePriority
import com.example.model.UserNewsPreferences
import kotlin.math.exp

object PersonalizationEngine {

  fun rankForYouFeed(
    articles: List<NewsArticle>,
    preferences: UserNewsPreferences,
    sourceFilter: NewsSource? = null,
    topicFilter: NewsTopic? = null,
    searchQuery: String = "",
    nowMillis: Long = System.currentTimeMillis(),
  ): List<NewsArticle> {
    val normalizedQuery = searchQuery.trim().lowercase()

    val filtered =
      articles.filter { article ->
        val matchesEnabled =
          if (sourceFilter != null) {
            article.source == sourceFilter
          } else {
            article.source in preferences.enabledSources
          }
        val matchesTopic = topicFilter == null || article.topic == topicFilter
        val matchesLocalCity =
          article.source != NewsSource.LOCAL ||
            article.localEditionCity.isNullOrBlank() ||
            article.localEditionCity.equals(preferences.localCity, ignoreCase = true)
        val matchesSearch =
          normalizedQuery.isEmpty() ||
            article.title.lowercase().contains(normalizedQuery) ||
            article.summary.lowercase().contains(normalizedQuery) ||
            article.source.displayName.lowercase().contains(normalizedQuery) ||
            (article.publisherSubName?.lowercase()?.contains(normalizedQuery) == true)

        matchesEnabled && matchesTopic && matchesLocalCity && matchesSearch
      }

    if (filtered.isEmpty()) return emptyList()

    val scored =
      filtered.map { article ->
        val ageHours = ((nowMillis - article.publishedAtMillis).coerceAtLeast(0L)) / 3_600_000.0
        val recencyScore = exp(-ageHours / 22.0) // Fresh within 24h stays high

        val priority = preferences.sourcePriorities[article.source] ?: SourcePriority.STANDARD
        val topicMatch = article.topic in preferences.preferredTopics
        val matchedKeyword =
          preferences.keywords.firstOrNull { kw ->
            article.title.contains(kw, ignoreCase = true) ||
              article.summary.contains(kw, ignoreCase = true)
          }

        var score = recencyScore * priority.weight
        if (topicMatch) score += 0.35
        if (matchedKeyword != null) score += 0.85
        if (article.source == NewsSource.LOCAL) score += 0.22
        if (!article.isRead) score += 0.12
        if (article.imageUrl != null) score += 0.08

        val reason =
          when {
            matchedKeyword != null -> "Keyword: $matchedKeyword"
            article.source == NewsSource.LOCAL -> "Local • ${preferences.localCity.substringBefore(",")}"
            priority == SourcePriority.BOOSTED -> "Boosted • ${article.source.shortTag}"
            topicMatch -> "For You • ${article.topic.label}"
            ageHours < 3.0 -> "Just In"
            else -> article.topic.label
          }

        article.copy(personalizationReason = reason, relevanceScore = score)
      }

    // If filtering to a single outlet, preserve pure score/recency order
    if (sourceFilter != null) {
      return scored.sortedByDescending { it.relevanceScore }
    }

    // Otherwise blend outlets naturally so NBC, CNBC, CNN, and Local stories interleave cleanly
    return interleaveBySource(scored.sortedByDescending { it.relevanceScore })
  }

  private fun interleaveBySource(sorted: List<NewsArticle>): List<NewsArticle> {
    if (sorted.size <= 4) return sorted
    val buckets = sorted.groupBy { it.source }.mapValues { it.value.toMutableList() }.toMutableMap()
    val result = mutableListOf<NewsArticle>()
    var lastSource: NewsSource? = null

    while (buckets.values.any { it.isNotEmpty() }) {
      val candidateEntry =
        buckets.entries
          .filter { it.value.isNotEmpty() }
          .sortedByDescending { entry ->
            val topScore = entry.value.first().relevanceScore
            // Gently penalize repeating the exact same outlet back-to-back
            if (entry.key == lastSource) topScore * 0.72 else topScore
          }
          .firstOrNull() ?: break

      val picked = candidateEntry.value.removeAt(0)
      result.add(picked)
      lastSource = picked.source
    }
    return result
  }
}
