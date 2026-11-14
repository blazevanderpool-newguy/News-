package com.example.model

enum class NewsSource(
  val id: String,
  val displayName: String,
  val shortTag: String,
  val tagline: String,
) {
  NBC(
    id = "nbc",
    displayName = "NBC News",
    shortTag = "NBC",
    tagline = "National & breaking broadcast coverage",
  ),
  CNBC(
    id = "cnbc",
    displayName = "CNBC",
    shortTag = "CNBC",
    tagline = "Markets, business & tech economy",
  ),
  CNN(
    id = "cnn",
    displayName = "CNN",
    shortTag = "CNN",
    tagline = "U.S., world & political reporting",
  ),
  LOCAL(
    id = "local",
    displayName = "Local News",
    shortTag = "LOCAL",
    tagline = "Regional & citywide headlines near you",
  );

  companion object {
    fun fromId(id: String): NewsSource = entries.firstOrNull { it.id == id } ?: NBC
  }
}

enum class NewsTopic(val id: String, val label: String) {
  TOP_STORIES("top", "Top Stories"),
  BUSINESS("business", "Markets & Business"),
  TECHNOLOGY("tech", "Technology"),
  POLITICS("politics", "Politics"),
  WORLD("world", "World"),
  HEALTH("health", "Health & Science"),
  SPORTS("sports", "Sports"),
  ENTERTAINMENT("entertainment", "Culture");

  companion object {
    fun fromId(id: String): NewsTopic = entries.firstOrNull { it.id == id } ?: TOP_STORIES
  }
}

enum class SourcePriority(val weight: Double, val label: String) {
  STANDARD(1.0, "Standard"),
  BOOSTED(1.45, "Boosted");

  companion object {
    fun fromName(name: String?): SourcePriority =
      entries.firstOrNull { it.name == name } ?: STANDARD
  }
}

data class UserNewsPreferences(
  val enabledSources: Set<NewsSource> = NewsSource.entries.toSet(),
  val sourcePriorities: Map<NewsSource, SourcePriority> =
    NewsSource.entries.associateWith { SourcePriority.STANDARD },
  val preferredTopics: Set<NewsTopic> =
    setOf(NewsTopic.TOP_STORIES, NewsTopic.BUSINESS, NewsTopic.TECHNOLOGY),
  val localCity: String = DEFAULT_LOCAL_CITY,
  val keywords: List<String> = emptyList(),
) {
  companion object {
    const val DEFAULT_LOCAL_CITY = "San Francisco, CA"
    val PRESET_CITIES =
      listOf(
        "New York, NY",
        "Los Angeles, CA",
        "Chicago, IL",
        "San Francisco, CA",
        "Austin, TX",
        "Seattle, WA",
        "Miami, FL",
        "Boston, MA",
        "Denver, CO",
        "Atlanta, GA",
      )
  }
}

data class NewsArticle(
  val id: String,
  val title: String,
  val summary: String,
  val url: String,
  val imageUrl: String?,
  val source: NewsSource,
  val publisherSubName: String?,
  val author: String?,
  val topic: NewsTopic,
  val publishedAtMillis: Long,
  val isBookmarked: Boolean = false,
  val isRead: Boolean = false,
  val bookmarkedAtMillis: Long? = null,
  val localEditionCity: String? = null,
  val personalizationReason: String? = null,
  val relevanceScore: Double = 0.0,
)
