package com.example.data.network

import android.util.Xml
import androidx.core.text.HtmlCompat
import com.example.model.NewsArticle
import com.example.model.NewsSource
import com.example.model.NewsTopic
import java.io.StringReader
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import org.xmlpull.v1.XmlPullParser

object RssXmlParser {

  private val imgRegex = Regex("""<img[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
  private val datePatterns =
    listOf(
      "EEE, dd MMM yyyy HH:mm:ss Z",
      "EEE, dd MMM yyyy HH:mm:ss zzz",
      "EEE, d MMM yyyy HH:mm:ss Z",
      "EEE, d MMM yyyy HH:mm:ss zzz",
      "yyyy-MM-dd'T'HH:mm:ss'Z'",
      "yyyy-MM-dd'T'HH:mm:ssXXX",
      "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
      "yyyy-MM-dd HH:mm:ss",
    )

  fun parse(
    xmlContent: String,
    source: NewsSource,
    defaultTopic: NewsTopic,
    localCity: String? = null,
    maxItems: Int = 18,
  ): List<NewsArticle> {
    if (xmlContent.isBlank()) return emptyList()
    val results = mutableListOf<NewsArticle>()
    val parser = Xml.newPullParser()
    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
    parser.setInput(StringReader(xmlContent))

    var inItem = false
    var isAtomEntry = false
    var title = ""
    var link = ""
    var description = ""
    var contentEncoded = ""
    var pubDateRaw = ""
    var imageUrl: String? = null
    var author: String? = null
    var publisherSubName: String? = null
    val categories = mutableListOf<String>()

    var eventType = parser.eventType
    while (eventType != XmlPullParser.END_DOCUMENT && results.size < maxItems) {
      when (eventType) {
        XmlPullParser.START_TAG -> {
          val tagName = parser.name?.lowercase(Locale.US).orEmpty()
          if (tagName == "item" || tagName == "entry") {
            inItem = true
            isAtomEntry = tagName == "entry"
            title = ""
            link = ""
            description = ""
            contentEncoded = ""
            pubDateRaw = ""
            imageUrl = null
            author = null
            publisherSubName = null
            categories.clear()
          } else if (inItem) {
            when (tagName) {
              "title" -> title = readTextSafe(parser)
              "link" -> {
                val href = parser.getAttributeValue(null, "href")
                val textLink = readTextSafe(parser)
                if (link.isBlank()) {
                  link = if (!href.isNullOrBlank()) href else textLink
                }
              }
              "description",
              "summary" -> {
                val raw = readTextSafe(parser)
                if (description.isBlank()) description = raw
                if (imageUrl == null) imageUrl = extractImgFromHtml(raw)
              }
              "content:encoded",
              "content" -> {
                val raw = readTextSafe(parser)
                if (contentEncoded.isBlank()) contentEncoded = raw
                if (imageUrl == null) imageUrl = extractImgFromHtml(raw)
              }
              "pubdate",
              "published",
              "updated",
              "dc:date" -> {
                val raw = readTextSafe(parser)
                if (pubDateRaw.isBlank()) pubDateRaw = raw
              }
              "author",
              "dc:creator" -> {
                val raw = readTextSafe(parser)
                if (raw.isNotBlank() && author == null) author = cleanPlainText(raw)
              }
              "source" -> {
                val raw = readTextSafe(parser)
                if (raw.isNotBlank()) publisherSubName = cleanPlainText(raw)
              }
              "category" -> {
                val term = parser.getAttributeValue(null, "term")
                val text = readTextSafe(parser)
                val cat = (term ?: text).trim()
                if (cat.isNotEmpty()) categories.add(cat)
              }
              "media:content",
              "media:thumbnail" -> {
                val url = parser.getAttributeValue(null, "url")
                val medium = parser.getAttributeValue(null, "medium")
                if (
                  !url.isNullOrBlank() &&
                    (medium == null || medium == "image" || url.looksLikeImage()) &&
                    imageUrl == null
                ) {
                  imageUrl = normalizeImageUrl(url)
                }
              }
              "enclosure" -> {
                val url = parser.getAttributeValue(null, "url")
                val type = parser.getAttributeValue(null, "type")
                if (
                  !url.isNullOrBlank() &&
                    (type?.startsWith("image") == true || url.looksLikeImage()) &&
                    imageUrl == null
                ) {
                  imageUrl = normalizeImageUrl(url)
                }
              }
            }
          }
        }
        XmlPullParser.END_TAG -> {
          val tagName = parser.name?.lowercase(Locale.US).orEmpty()
          if ((tagName == "item" && !isAtomEntry) || (tagName == "entry" && isAtomEntry)) {
            inItem = false
            val cleanedTitle = cleanHeadline(title, publisherSubName)
            val cleanedUrl = link.trim()
            if (cleanedTitle.isNotBlank() && cleanedUrl.startsWith("http")) {
              val rawSummary = if (description.isNotBlank()) description else contentEncoded
              val cleanedSummary = buildSummary(rawSummary, cleanedTitle, publisherSubName)
              val publishedMillis = parseDateMillis(pubDateRaw)
              val topic =
                classifyTopic(
                  title = cleanedTitle,
                  summary = cleanedSummary,
                  categories = categories,
                  defaultTopic = defaultTopic,
                )
              results.add(
                NewsArticle(
                  id = hashId("${source.id}|$cleanedUrl|$cleanedTitle"),
                  title = cleanedTitle,
                  summary = cleanedSummary,
                  url = cleanedUrl,
                  imageUrl = imageUrl,
                  source = source,
                  publisherSubName = publisherSubName,
                  author = author,
                  topic = topic,
                  publishedAtMillis = publishedMillis,
                  localEditionCity = if (source == NewsSource.LOCAL) localCity else null,
                )
              )
            }
          }
        }
      }
      eventType = parser.next()
    }
    return results
  }

  private fun readTextSafe(parser: XmlPullParser): String {
    var result = ""
    if (parser.next() == XmlPullParser.TEXT) {
      result = parser.text ?: ""
      parser.nextTag()
    }
    return result.trim()
  }

  private fun cleanHeadline(rawTitle: String, subPublisher: String?): String {
    var text = cleanPlainText(rawTitle)
    if (!subPublisher.isNullOrBlank()) {
      val suffix = " - $subPublisher"
      if (text.endsWith(suffix, ignoreCase = true)) {
        text = text.removeSuffix(suffix).trim()
      }
    }
    listOf(" - CNN", " - CNBC", " - NBC News", " | NBC News", " | CNN").forEach { suffix ->
      if (text.endsWith(suffix, ignoreCase = true)) {
        text = text.dropLast(suffix.length).trim()
      }
    }
    return text
  }

  private fun buildSummary(rawHtml: String, title: String, subPublisher: String?): String {
    val plain = cleanPlainText(rawHtml)
    if (plain.isBlank() || plain.equals(title, ignoreCase = true)) {
      return if (!subPublisher.isNullOrBlank()) {
        "Coverage reported by $subPublisher. Tap to read the full story."
      } else {
        "Tap to read the complete report and analysis."
      }
    }
    // Google News RSS descriptions often repeat the headline followed by publisher name
    val withoutTitle =
      if (plain.startsWith(title, ignoreCase = true)) {
        plain.drop(title.length).trim(' ', '-', '—', '|', '\u00A0')
      } else {
        plain
      }
    val finalSummary =
      if (withoutTitle.length < 24 && !subPublisher.isNullOrBlank()) {
        "Reported by $subPublisher. Tap to open full coverage."
      } else {
        withoutTitle.ifBlank { plain }
      }
    return if (finalSummary.length > 260) {
      finalSummary.take(257).trimEnd() + "…"
    } else {
      finalSummary
    }
  }

  private fun cleanPlainText(raw: String): String {
    if (raw.isBlank()) return ""
    val decoded =
      HtmlCompat.fromHtml(raw, HtmlCompat.FROM_HTML_MODE_LEGACY)
        .toString()
        .replace('\uFFFC', ' ')
        .replace('\u00A0', ' ')
        .replace(Regex("""\s+"""), " ")
        .trim()
    return decoded
  }

  private fun extractImgFromHtml(html: String): String? {
    val match = imgRegex.find(html) ?: return null
    return normalizeImageUrl(match.groupValues[1])
  }

  private fun normalizeImageUrl(url: String): String? {
    val clean = url.trim().replace("&amp;", "&")
    if (!clean.startsWith("http://") && !clean.startsWith("https://")) return null
    if (clean.contains("1x1") || clean.contains("pixel", ignoreCase = true)) return null
    return clean.replaceFirst("http://", "https://")
  }

  private fun String.looksLikeImage(): Boolean {
    val lower = lowercase(Locale.US)
    return lower.contains(".jpg") ||
      lower.contains(".jpeg") ||
      lower.contains(".png") ||
      lower.contains(".webp") ||
      lower.contains("image") ||
      lower.contains("photo")
  }

  private fun parseDateMillis(raw: String): Long {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return System.currentTimeMillis()
    for (pattern in datePatterns) {
      try {
        val sdf = SimpleDateFormat(pattern, Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val parsed = sdf.parse(trimmed)
        if (parsed != null) return parsed.time
      } catch (_: Exception) {
        // try next pattern
      }
    }
    return System.currentTimeMillis()
  }

  private fun classifyTopic(
    title: String,
    summary: String,
    categories: List<String>,
    defaultTopic: NewsTopic,
  ): NewsTopic {
    val haystack = "${categories.joinToString(" ")} $title $summary".lowercase(Locale.US)
    return when {
      haystack.containsAny(
        "stock",
        "market",
        "wall street",
        "nasdaq",
        "dow",
        "s&p",
        "fed",
        "inflation",
        "earnings",
        "economy",
        "tariff",
        "gdp",
        "mortgage",
        "investor",
      ) -> NewsTopic.BUSINESS
      haystack.containsAny(
        "ai ",
        "artificial intelligence",
        "tech",
        "apple",
        "google",
        "microsoft",
        "nvidia",
        "meta",
        "cyber",
        "software",
        "iphone",
        "android",
        "chip",
        "robot",
      ) -> NewsTopic.TECHNOLOGY
      haystack.containsAny(
        "white house",
        "senate",
        "congress",
        "election",
        "president",
        "supreme court",
        "governor",
        "mayor",
        "legislation",
        "bipartisan",
        "democrat",
        "republican",
      ) -> NewsTopic.POLITICS
      haystack.containsAny(
        "ukraine",
        "gaza",
        "israel",
        "china",
        "europe",
        "nato",
        "russia",
        "middle east",
        "united nations",
        "tokyo",
        "london",
        "paris",
      ) -> NewsTopic.WORLD
      haystack.containsAny(
        "health",
        "fda",
        "medical",
        "vaccine",
        "cancer",
        "nasa",
        "space",
        "climate",
        "science",
        "hospital",
      ) -> NewsTopic.HEALTH
      haystack.containsAny(
        "nfl",
        "nba",
        "mlb",
        "nhl",
        "olympic",
        "soccer",
        "quarterback",
        "championship",
        "playoff",
      ) -> NewsTopic.SPORTS
      haystack.containsAny(
        "movie",
        "oscar",
        "grammy",
        "netflix",
        "music",
        "box office",
        "hollywood",
        "broadway",
      ) -> NewsTopic.ENTERTAINMENT
      else -> defaultTopic
    }
  }

  private fun String.containsAny(vararg needles: String): Boolean =
    needles.any { this.contains(it) }

  private fun hashId(input: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
    return digest.take(12).joinToString("") { "%02x".format(it) }
  }
}
