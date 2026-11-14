package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.NewsArticle
import com.example.model.NewsSource
import com.example.model.NewsTopic
import com.example.ui.FeedSyncState
import com.example.ui.NewsUiState
import com.example.ui.components.CompactStoryCard
import com.example.ui.components.EditorialMasthead
import com.example.ui.components.LeadHeroStoryCard
import com.example.ui.components.SourceFilterBar
import com.example.ui.components.TopicFilterBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForYouScreen(
  uiState: NewsUiState,
  onSelectSourceFilter: (NewsSource?) -> Unit,
  onSelectTopicFilter: (NewsTopic?) -> Unit,
  onToggleSearch: () -> Unit,
  onSearchQueryChange: (String) -> Unit,
  onOpenPersonalize: () -> Unit,
  onRefresh: () -> Unit,
  onOpenArticle: (NewsArticle) -> Unit,
  onToggleBookmark: (NewsArticle) -> Unit,
  onShareArticle: (NewsArticle) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxSize()) {
    EditorialMasthead(
      localCity = uiState.preferences.localCity,
      isSearchExpanded = uiState.isSearchExpanded,
      searchQuery = uiState.searchQuery,
      isRefreshing = uiState.isRefreshing,
      onToggleSearch = onToggleSearch,
      onSearchQueryChange = onSearchQueryChange,
      onOpenPersonalize = onOpenPersonalize,
      onRefresh = onRefresh,
    )

    SourceFilterBar(
      selectedSource = uiState.selectedSourceFilter,
      enabledSources = uiState.preferences.enabledSources,
      localCity = uiState.preferences.localCity,
      onSelectSource = onSelectSourceFilter,
    )

    TopicFilterBar(
      selectedTopic = uiState.selectedTopicFilter,
      onSelectTopic = onSelectTopicFilter,
    )

    Spacer(modifier = Modifier.height(6.dp))

    AnimatedVisibility(visible = uiState.syncState is FeedSyncState.PartialOrOffline) {
      val failed = (uiState.syncState as? FeedSyncState.PartialOrOffline)?.failedSources.orEmpty()
      if (failed.isNotEmpty()) {
        Surface(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
          shape = MaterialTheme.shapes.small,
          color = MaterialTheme.colorScheme.secondaryContainer,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Outlined.CloudOff,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSecondaryContainer,
              modifier = Modifier.size(16.dp),
            )
            Text(
              text =
                "Sync issue for ${failed.joinToString(", ") { it.shortTag }}. Showing available stories.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
          }
        }
      }
    }

    PullToRefreshBox(
      isRefreshing = uiState.isRefreshing,
      onRefresh = onRefresh,
      modifier = Modifier.weight(1f).fillMaxWidth(),
    ) {
      when {
        uiState.feedArticles.isEmpty() && uiState.isRefreshing -> {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(12.dp),
              modifier = Modifier.padding(24.dp),
            ) {
              CircularProgressIndicator()
              Text(
                text = stringResource(R.string.loading_wire),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
              )
            }
          }
        }

        uiState.feedArticles.isEmpty() -> {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.widthIn(max = 420.dp).padding(28.dp),
            ) {
              Icon(
                imageVector = Icons.Outlined.Newspaper,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp),
              )
              Text(
                text = stringResource(R.string.empty_feed_title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
              )
              Text(
                text = stringResource(R.string.empty_feed_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
              )
              Spacer(modifier = Modifier.height(6.dp))
              Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                  onClick = onOpenPersonalize,
                  modifier = Modifier.testTag("empty_personalize_button"),
                ) {
                  Icon(Icons.Outlined.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(stringResource(R.string.action_personalize))
                }
                Button(
                  onClick = onRefresh,
                  modifier = Modifier.testTag("empty_refresh_button"),
                ) {
                  Text(stringResource(R.string.action_retry))
                }
              }
            }
          }
        }

        else -> {
          val leadStory = uiState.feedArticles.first()
          val remainingStories = uiState.feedArticles.drop(1)

          BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useTwoColumns = maxWidth >= 660.dp

            if (useTwoColumns) {
              LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize().testTag("feed_grid"),
              ) {
                item(span = { GridItemSpan(2) }, key = leadStory.id) {
                  LeadHeroStoryCard(
                    article = leadStory,
                    onClick = { onOpenArticle(leadStory) },
                    onToggleBookmark = { onToggleBookmark(leadStory) },
                    onShare = { onShareArticle(leadStory) },
                  )
                }
                items(items = remainingStories, key = { it.id }) { article ->
                  CompactStoryCard(
                    article = article,
                    onClick = { onOpenArticle(article) },
                    onToggleBookmark = { onToggleBookmark(article) },
                    onShare = { onShareArticle(article) },
                  )
                }
              }
            } else {
              LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize().testTag("feed_list"),
              ) {
                item(key = leadStory.id) {
                  LeadHeroStoryCard(
                    article = leadStory,
                    onClick = { onOpenArticle(leadStory) },
                    onToggleBookmark = { onToggleBookmark(leadStory) },
                    onShare = { onShareArticle(leadStory) },
                  )
                }
                items(items = remainingStories, key = { it.id }) { article ->
                  CompactStoryCard(
                    article = article,
                    onClick = { onOpenArticle(article) },
                    onToggleBookmark = { onToggleBookmark(article) },
                    onShare = { onShareArticle(article) },
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}
