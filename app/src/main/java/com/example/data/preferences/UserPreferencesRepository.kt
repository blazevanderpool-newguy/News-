package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.NewsSource
import com.example.model.NewsTopic
import com.example.model.SourcePriority
import com.example.model.UserNewsPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.pulseWireDataStore: DataStore<Preferences> by
  preferencesDataStore(name = "pulsewire_preferences")

class UserPreferencesRepository(private val context: Context) {

  private object Keys {
    val ENABLED_SOURCES = stringSetPreferencesKey("enabled_sources")
    val PREFERRED_TOPICS = stringSetPreferencesKey("preferred_topics")
    val LOCAL_CITY = stringPreferencesKey("local_city")
    val KEYWORDS = stringPreferencesKey("keywords")
    val PRIORITY_NBC = stringPreferencesKey("priority_nbc")
    val PRIORITY_CNBC = stringPreferencesKey("priority_cnbc")
    val PRIORITY_CNN = stringPreferencesKey("priority_cnn")
    val PRIORITY_LOCAL = stringPreferencesKey("priority_local")
  }

  val preferencesFlow: Flow<UserNewsPreferences> =
    context.pulseWireDataStore.data.map { prefs ->
      val enabledSourceIds =
        prefs[Keys.ENABLED_SOURCES] ?: NewsSource.entries.map { it.id }.toSet()
      val enabledSources =
        enabledSourceIds.mapNotNull { id -> NewsSource.entries.firstOrNull { it.id == id } }.toSet()
          .ifEmpty { setOf(NewsSource.NBC) }

      val preferredTopicIds =
        prefs[Keys.PREFERRED_TOPICS]
          ?: setOf(
            NewsTopic.TOP_STORIES.id,
            NewsTopic.BUSINESS.id,
            NewsTopic.TECHNOLOGY.id,
          )
      val preferredTopics =
        preferredTopicIds
          .mapNotNull { id -> NewsTopic.entries.firstOrNull { it.id == id } }
          .toSet()

      val priorities =
        mapOf(
          NewsSource.NBC to SourcePriority.fromName(prefs[Keys.PRIORITY_NBC]),
          NewsSource.CNBC to SourcePriority.fromName(prefs[Keys.PRIORITY_CNBC]),
          NewsSource.CNN to SourcePriority.fromName(prefs[Keys.PRIORITY_CNN]),
          NewsSource.LOCAL to SourcePriority.fromName(prefs[Keys.PRIORITY_LOCAL]),
        )

      val localCity =
        prefs[Keys.LOCAL_CITY]?.takeIf { it.isNotBlank() }
          ?: UserNewsPreferences.DEFAULT_LOCAL_CITY

      val keywords =
        prefs[Keys.KEYWORDS]
          ?.split("|")
          ?.map { it.trim() }
          ?.filter { it.isNotEmpty() }
          ?.distinct()
          ?: emptyList()

      UserNewsPreferences(
        enabledSources = enabledSources,
        sourcePriorities = priorities,
        preferredTopics = preferredTopics,
        localCity = localCity,
        keywords = keywords,
      )
    }

  suspend fun toggleSource(source: NewsSource, enabled: Boolean) {
    context.pulseWireDataStore.edit { prefs ->
      val current =
        (prefs[Keys.ENABLED_SOURCES] ?: NewsSource.entries.map { it.id }.toSet()).toMutableSet()
      if (enabled) {
        current.add(source.id)
      } else if (current.size > 1) {
        current.remove(source.id)
      }
      prefs[Keys.ENABLED_SOURCES] = current
    }
  }

  suspend fun setSourcePriority(source: NewsSource, priority: SourcePriority) {
    context.pulseWireDataStore.edit { prefs ->
      val key =
        when (source) {
          NewsSource.NBC -> Keys.PRIORITY_NBC
          NewsSource.CNBC -> Keys.PRIORITY_CNBC
          NewsSource.CNN -> Keys.PRIORITY_CNN
          NewsSource.LOCAL -> Keys.PRIORITY_LOCAL
        }
      prefs[key] = priority.name
    }
  }

  suspend fun toggleTopic(topic: NewsTopic) {
    context.pulseWireDataStore.edit { prefs ->
      val current =
        (prefs[Keys.PREFERRED_TOPICS]
            ?: setOf(
              NewsTopic.TOP_STORIES.id,
              NewsTopic.BUSINESS.id,
              NewsTopic.TECHNOLOGY.id,
            ))
          .toMutableSet()
      if (current.contains(topic.id)) {
        if (current.size > 1) current.remove(topic.id)
      } else {
        current.add(topic.id)
      }
      prefs[Keys.PREFERRED_TOPICS] = current
    }
  }

  suspend fun setLocalCity(city: String) {
    val sanitized = city.trim()
    if (sanitized.isEmpty()) return
    context.pulseWireDataStore.edit { prefs -> prefs[Keys.LOCAL_CITY] = sanitized }
  }

  suspend fun addKeyword(keyword: String) {
    val clean = keyword.trim().replace("|", "")
    if (clean.isEmpty()) return
    context.pulseWireDataStore.edit { prefs ->
      val existing =
        prefs[Keys.KEYWORDS]
          ?.split("|")
          ?.map { it.trim() }
          ?.filter { it.isNotEmpty() }
          ?.toMutableList() ?: mutableListOf()
      if (existing.none { it.equals(clean, ignoreCase = true) }) {
        existing.add(clean)
        prefs[Keys.KEYWORDS] = existing.joinToString("|")
      }
    }
  }

  suspend fun removeKeyword(keyword: String) {
    context.pulseWireDataStore.edit { prefs ->
      val existing =
        prefs[Keys.KEYWORDS]
          ?.split("|")
          ?.map { it.trim() }
          ?.filter { it.isNotEmpty() && !it.equals(keyword, ignoreCase = true) }
          ?: emptyList()
      prefs[Keys.KEYWORDS] = existing.joinToString("|")
    }
  }
}
