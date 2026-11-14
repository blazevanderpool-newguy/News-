package com.example.data.network

import com.example.model.NewsArticle
import com.example.model.NewsSource
import com.example.model.NewsTopic
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class FeedFetchResult(
  val articles: List<NewsArticle>,
  val succeededSources: Set<NewsSource>,
  val failedSources: Set<NewsSource>,
)

class RssFeedService {

  private val client =
    OkHttpClient.Builder()
      .connectTimeout(10, TimeUnit.SECONDS)
      .readTimeout(12, TimeUnit.SECONDS)
      .followRedirects(true)
      .followSslRedirects(true)
      .build()

  suspend fun fetchAllFeeds(
    enabledSources: Set<NewsSource>,
    localCity: String,
    preferredTopics: Set<NewsTopic>,
  ): FeedFetchResult = coroutineScope {
    val jobs =
      enabledSources.map { source ->
        async(Dispatchers.IO) {
          val articles =
            when (source) {
              NewsSource.NBC -> fetchNbc(preferredTopics)
              NewsSource.CNBC -> fetchCnbc(preferredTopics)
              NewsSource.CNN -> fetchCnn(preferredTopics)
              NewsSource.LOCAL -> fetchLocalNews(localCity)
            }
          source to articles
        }
      }

    val results = jobs.awaitAll()
    val allArticles = mutableListOf<NewsArticle>()
    val succeeded = mutableSetOf<NewsSource>()
    val failed = mutableSetOf<NewsSource>()

    for ((source, items) in results) {
      if (items.isNotEmpty()) {
        succeeded.add(source)
        allArticles.addAll(items)
      } else {
        failed.add(source)
      }
    }

    FeedFetchResult(
      articles = allArticles.distinctBy { it.id },
      succeededSources = succeeded,
      failedSources = failed,
    )
  }

  private suspend fun fetchNbc(topics: Set<NewsTopic>): List<NewsArticle> = coroutineScope {
    val endpoints = buildList {
      add("https://feeds.nbcnews.com/nbcnews/public/news" to NewsTopic.TOP_STORIES)
      if (NewsTopic.POLITICS in topics) {
        add("https://feeds.nbcnews.com/nbcnews/public/politics" to NewsTopic.POLITICS)
      }
      if (NewsTopic.TECHNOLOGY in topics) {
        add("https://feeds.nbcnews.com/nbcnews/public/tech" to NewsTopic.TECHNOLOGY)
      }
      if (NewsTopic.WORLD in topics) {
        add("https://feeds.nbcnews.com/nbcnews/public/world" to NewsTopic.WORLD)
      }
      if (NewsTopic.BUSINESS in topics) {
        add("https://feeds.nbcnews.com/nbcnews/public/business" to NewsTopic.BUSINESS)
      }
    }

    val primary =
      endpoints
        .map { (url, topic) ->
          async { fetchSingleRss(url, NewsSource.NBC, topic, maxItems = 12) }
        }
        .awaitAll()
        .flatten()

    if (primary.isNotEmpty()) {
      primary
    } else {
      fetchSingleRss(
        url = "https://news.google.com/rss/search?q=when:2d+site:nbcnews.com&hl=en-US&gl=US&ceid=US:en",
        source = NewsSource.NBC,
        defaultTopic = NewsTopic.TOP_STORIES,
        maxItems = 18,
      )
    }
  }

  private suspend fun fetchCnbc(topics: Set<NewsTopic>): List<NewsArticle> = coroutineScope {
    val endpoints = buildList {
      add(
        "https://search.cnbc.com/rs/search/combinedcms/view.xml?partnerId=wrss01&id=100003114" to
          NewsTopic.TOP_STORIES
      )
      if (NewsTopic.BUSINESS in topics || NewsTopic.TOP_STORIES in topics) {
        add(
          "https://search.cnbc.com/rs/search/combinedcms/view.xml?partnerId=wrss01&id=10000664" to
            NewsTopic.BUSINESS
        )
      }
      if (NewsTopic.TECHNOLOGY in topics) {
        add(
          "https://search.cnbc.com/rs/search/combinedcms/view.xml?partnerId=wrss01&id=19854910" to
            NewsTopic.TECHNOLOGY
        )
      }
      if (NewsTopic.POLITICS in topics) {
        add(
          "https://search.cnbc.com/rs/search/combinedcms/view.xml?partnerId=wrss01&id=10000113" to
            NewsTopic.POLITICS
        )
      }
    }

    val primary =
      endpoints
        .map { (url, topic) ->
          async { fetchSingleRss(url, NewsSource.CNBC, topic, maxItems = 12) }
        }
        .awaitAll()
        .flatten()

    if (primary.isNotEmpty()) {
      primary
    } else {
      fetchSingleRss(
        url = "https://news.google.com/rss/search?q=when:2d+site:cnbc.com&hl=en-US&gl=US&ceid=US:en",
        source = NewsSource.CNBC,
        defaultTopic = NewsTopic.BUSINESS,
        maxItems = 18,
      )
    }
  }

  private suspend fun fetchCnn(topics: Set<NewsTopic>): List<NewsArticle> = coroutineScope {
    val endpoints = buildList {
      add(
        "https://news.google.com/rss/search?q=when:2d+site:cnn.com&hl=en-US&gl=US&ceid=US:en" to
          NewsTopic.TOP_STORIES
      )
      if (NewsTopic.BUSINESS in topics) {
        add(
          "https://news.google.com/rss/search?q=when:3d+site:cnn.com/business&hl=en-US&gl=US&ceid=US:en" to
            NewsTopic.BUSINESS
        )
      }
      if (NewsTopic.POLITICS in topics) {
        add(
          "https://news.google.com/rss/search?q=when:3d+site:cnn.com/politics&hl=en-US&gl=US&ceid=US:en" to
            NewsTopic.POLITICS
        )
      }
      if (NewsTopic.TECHNOLOGY in topics) {
        add(
          "https://news.google.com/rss/search?q=when:4d+site:cnn.com+technology+OR+AI&hl=en-US&gl=US&ceid=US:en" to
            NewsTopic.TECHNOLOGY
        )
      }
      if (NewsTopic.WORLD in topics) {
        add(
          "https://news.google.com/rss/search?q=when:3d+site:cnn.com/world&hl=en-US&gl=US&ceid=US:en" to
            NewsTopic.WORLD
        )
      }
    }

    val googleCnn =
      endpoints
        .map { (url, topic) ->
          async { fetchSingleRss(url, NewsSource.CNN, topic, maxItems = 10) }
        }
        .awaitAll()
        .flatten()

    if (googleCnn.isNotEmpty()) {
      googleCnn
    } else {
      fetchSingleRss(
        url = "https://rss.cnn.com/rss/cnn_topstories.rss",
        source = NewsSource.CNN,
        defaultTopic = NewsTopic.TOP_STORIES,
        maxItems = 18,
      )
    }
  }

  private suspend fun fetchLocalNews(localCity: String): List<NewsArticle> = coroutineScope {
    val encodedCity = withContext(Dispatchers.IO) { URLEncoder.encode(localCity, "UTF-8") }
    val encodedQuery =
      withContext(Dispatchers.IO) { URLEncoder.encode("\"$localCity\" local news", "UTF-8") }

    val geoFeed = async {
      fetchSingleRss(
        url =
          "https://news.google.com/rss/headlines/section/geo/$encodedCity?hl=en-US&gl=US&ceid=US:en",
        source = NewsSource.LOCAL,
        defaultTopic = NewsTopic.TOP_STORIES,
        localCity = localCity,
        maxItems = 14,
      )
    }
    val queryFeed = async {
      fetchSingleRss(
        url = "https://news.google.com/rss/search?q=$encodedQuery&hl=en-US&gl=US&ceid=US:en",
        source = NewsSource.LOCAL,
        defaultTopic = NewsTopic.TOP_STORIES,
        localCity = localCity,
        maxItems = 12,
      )
    }
    (geoFeed.await() + queryFeed.await()).distinctBy { it.title.lowercase() }
  }

  private suspend fun fetchSingleRss(
    url: String,
    source: NewsSource,
    defaultTopic: NewsTopic,
    localCity: String? = null,
    maxItems: Int = 15,
  ): List<NewsArticle> =
    withContext(Dispatchers.IO) {
      try {
        val request =
          Request.Builder()
            .url(url)
            .header(
              "User-Agent",
              "Mozilla/5.0 (Linux; Android 15) PulseWireNews/1.0",
            )
            .header("Accept", "application/rss+xml, application/xml, text/xml;q=0.9, */*;q=0.8")
            .build()

        client.newCall(request).execute().use { response ->
          if (!response.isSuccessful) return@withContext emptyList()
          val body = response.body?.string().orEmpty()
          if (body.isBlank()) return@withContext emptyList()
          RssXmlParser.parse(
            xmlContent = body,
            source = source,
            defaultTopic = defaultTopic,
            localCity = localCity,
            maxItems = maxItems,
          )
        }
      } catch (_: Exception) {
        emptyList()
      }
    }
}
