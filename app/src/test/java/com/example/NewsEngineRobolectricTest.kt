package com.example

import com.example.data.network.RssXmlParser
import com.example.domain.PersonalizationEngine
import com.example.model.NewsArticle
import com.example.model.NewsSource
import com.example.model.NewsTopic
import com.example.model.SourcePriority
import com.example.model.UserNewsPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NewsEngineRobolectricTest {

  @Test
  fun `rss parser extracts media image, cleans publisher suffix, and classifies topic`() {
    val sampleXml =
      """
      <?xml version="1.0" encoding="UTF-8"?>
      <rss version="2.0" xmlns:media="http://search.yahoo.com/mrss/">
        <channel>
          <item>
            <title>Nvidia shares surge as AI chip demand accelerates - CNBC</title>
            <link>https://www.cnbc.com/2026/11/14/nvidia-ai-demand.html</link>
            <description><![CDATA[<p>Wall Street rallies as chipmakers report strong quarterly earnings.</p>]]></description>
            <pubDate>Sat, 14 Nov 2026 12:00:00 GMT</pubDate>
            <media:content url="https://image.cnbcfm.com/sample.jpg" medium="image" />
            <source>CNBC</source>
          </item>
        </channel>
      </rss>
      """
        .trimIndent()

    val parsed =
      RssXmlParser.parse(
        xmlContent = sampleXml,
        source = NewsSource.CNBC,
        defaultTopic = NewsTopic.TOP_STORIES,
      )

    assertEquals(1, parsed.size)
    val item = parsed.first()
    assertEquals("Nvidia shares surge as AI chip demand accelerates", item.title)
    assertEquals("https://image.cnbcfm.com/sample.jpg", item.imageUrl)
    assertEquals(NewsSource.CNBC, item.source)
    assertEquals(NewsTopic.BUSINESS, item.topic)
  }

  @Test
  fun `personalization engine prioritizes keyword match and boosted sources`() {
    val now = 1_763_120_000_000L
    val articles =
      listOf(
        NewsArticle(
          id = "1",
          title = "City Council approves transit budget",
          summary = "Local transit updates in Austin",
          url = "https://example.com/1",
          imageUrl = null,
          source = NewsSource.LOCAL,
          publisherSubName = "Austin Monitor",
          author = null,
          topic = NewsTopic.POLITICS,
          publishedAtMillis = now - 60 * 60 * 1000L,
          localEditionCity = "Austin, TX",
        ),
        NewsArticle(
          id = "2",
          title = "Federal Reserve signals steady rates",
          summary = "Markets react to inflation report",
          url = "https://example.com/2",
          imageUrl = null,
          source = NewsSource.CNBC,
          publisherSubName = null,
          author = null,
          topic = NewsTopic.BUSINESS,
          publishedAtMillis = now - 2 * 60 * 60 * 1000L,
        ),
        NewsArticle(
          id = "3",
          title = "Breakthrough in fusion energy research",
          summary = "Clean energy milestone announced",
          url = "https://example.com/3",
          imageUrl = null,
          source = NewsSource.CNN,
          publisherSubName = null,
          author = null,
          topic = NewsTopic.HEALTH,
          publishedAtMillis = now - 30 * 60 * 1000L,
        ),
      )

    val prefs =
      UserNewsPreferences(
        enabledSources = setOf(NewsSource.LOCAL, NewsSource.CNBC, NewsSource.CNN),
        sourcePriorities = mapOf(NewsSource.CNBC to SourcePriority.BOOSTED),
        preferredTopics = setOf(NewsTopic.BUSINESS),
        localCity = "Austin, TX",
        keywords = listOf("fusion"),
      )

    val ranked =
      PersonalizationEngine.rankForYouFeed(
        articles = articles,
        preferences = prefs,
        nowMillis = now,
      )

    assertEquals(3, ranked.size)
    assertEquals("3", ranked.first().id)
    assertTrue(ranked.first().personalizationReason?.contains("fusion") == true)
  }
}
