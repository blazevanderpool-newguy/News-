package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.NewsArticle
import com.example.ui.components.CompactStoryCard

@Composable
fun SavedScreen(
  bookmarkedArticles: List<NewsArticle>,
  onOpenArticle: (NewsArticle) -> Unit,
  onToggleBookmark: (NewsArticle) -> Unit,
  onShareArticle: (NewsArticle) -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(modifier = Modifier.fillMaxSize().widthIn(max = 760.dp)) {
      Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
        Text(
          text = "Reading List",
          style = MaterialTheme.typography.headlineMedium,
        )
        Text(
          text =
            if (bookmarkedArticles.isEmpty()) "Saved stories stay available offline"
            else "${bookmarkedArticles.size} saved ${if (bookmarkedArticles.size == 1) "story" else "stories"}",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

      if (bookmarkedArticles.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.widthIn(max = 380.dp).padding(28.dp),
          ) {
            Icon(
              imageVector = Icons.Outlined.Bookmarks,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(48.dp),
            )
            Text(
              text = stringResource(R.string.empty_saved_title),
              style = MaterialTheme.typography.titleLarge,
              textAlign = TextAlign.Center,
            )
            Text(
              text = stringResource(R.string.empty_saved_subtitle),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
            )
          }
        }
      } else {
        LazyColumn(
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.fillMaxSize().testTag("saved_articles_list"),
        ) {
          items(items = bookmarkedArticles, key = { it.id }) { article ->
            CompactStoryCard(
              article = article,
              onClick = { onOpenArticle(article) },
              onToggleBookmark = { onToggleBookmark(article) },
              onShare = { onShareArticle(article) },
            )
          }
          item { Spacer(modifier = Modifier.height(12.dp)) }
        }
      }
    }
  }
}
