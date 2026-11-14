package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.R
import com.example.model.NewsArticle
import com.example.model.NewsSource
import com.example.model.NewsTopic
import com.example.ui.NewsViewModel
import com.example.ui.theme.CnbcTeal
import com.example.ui.theme.CnbcTealSoft
import com.example.ui.theme.CnnRed
import com.example.ui.theme.CnnRedSoft
import com.example.ui.theme.LocalEmerald
import com.example.ui.theme.LocalEmeraldSoft
import com.example.ui.theme.NbcBlue
import com.example.ui.theme.NbcBlueSoft
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun NewsSource.brandAccent(): Color =
  when (this) {
    NewsSource.NBC -> NbcBlue
    NewsSource.CNBC -> CnbcTeal
    NewsSource.CNN -> CnnRed
    NewsSource.LOCAL -> LocalEmerald
  }

fun NewsSource.brandContainer(): Color =
  when (this) {
    NewsSource.NBC -> NbcBlueSoft
    NewsSource.CNBC -> CnbcTealSoft
    NewsSource.CNN -> CnnRedSoft
    NewsSource.LOCAL -> LocalEmeraldSoft
  }

@Composable
fun SourceBadge(
  source: NewsSource,
  subPublisher: String? = null,
  localCity: String? = null,
  modifier: Modifier = Modifier,
) {
  val label =
    when {
      source == NewsSource.LOCAL && !subPublisher.isNullOrBlank() ->
        "LOCAL • ${subPublisher.uppercase(Locale.US)}"
      source == NewsSource.LOCAL && !localCity.isNullOrBlank() ->
        "LOCAL • ${localCity.substringBefore(",").uppercase(Locale.US)}"
      else -> source.displayName.uppercase(Locale.US)
    }

  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(6.dp),
    color = source.brandContainer(),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Box(
        modifier = Modifier.size(6.dp).clip(CircleShape).background(source.brandAccent())
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = source.brandAccent(),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
fun EditorialMasthead(
  localCity: String,
  isSearchExpanded: Boolean,
  searchQuery: String,
  isRefreshing: Boolean,
  onToggleSearch: () -> Unit,
  onSearchQueryChange: (String) -> Unit,
  onOpenPersonalize: () -> Unit,
  onRefresh: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val formattedDate = remember {
    SimpleDateFormat("EEEE, MMM d", Locale.US).format(Date()).uppercase(Locale.US)
  }

  Column(modifier = modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = stringResource(R.string.app_name),
          style = MaterialTheme.typography.displaySmall,
          color = MaterialTheme.colorScheme.onBackground,
        )
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
            text = formattedDate,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = "•",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Icon(
            imageVector = Icons.Outlined.LocationOn,
            contentDescription = null,
            tint = LocalEmerald,
            modifier = Modifier.size(13.dp),
          )
          Text(
            text = localCity,
            style = MaterialTheme.typography.labelSmall,
            color = LocalEmerald,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = onToggleSearch,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("masthead_search_button"),
        ) {
          Icon(
            imageVector = if (isSearchExpanded) Icons.Outlined.Close else Icons.Outlined.Search,
            contentDescription = stringResource(R.string.action_search),
          )
        }
        IconButton(
          onClick = onRefresh,
          enabled = !isRefreshing,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("masthead_refresh_button"),
        ) {
          Icon(
            imageVector = Icons.Outlined.Refresh,
            contentDescription = stringResource(R.string.action_refresh),
          )
        }
        IconButton(
          onClick = onOpenPersonalize,
          modifier =
            Modifier.minimumInteractiveComponentSize().testTag("masthead_personalize_button"),
        ) {
          Icon(
            imageVector = Icons.Outlined.Tune,
            contentDescription = stringResource(R.string.action_personalize),
          )
        }
      }
    }

    AnimatedVisibility(visible = isSearchExpanded) {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        modifier =
          Modifier.fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 10.dp)
            .testTag("feed_search_input"),
        placeholder = { Text(stringResource(R.string.action_search)) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { onSearchQueryChange("") }) {
              Icon(
                Icons.Outlined.Close,
                contentDescription = stringResource(R.string.action_clear_search),
              )
            }
          }
        },
      )
    }

    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
  }
}

@Composable
fun SourceFilterBar(
  selectedSource: NewsSource?,
  enabledSources: Set<NewsSource>,
  localCity: String,
  onSelectSource: (NewsSource?) -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 10.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    FilterChip(
      selected = selectedSource == null,
      onClick = { onSelectSource(null) },
      label = { Text(stringResource(R.string.source_all)) },
      modifier = Modifier.testTag("filter_source_all"),
      colors =
        FilterChipDefaults.filterChipColors(
          selectedContainerColor = MaterialTheme.colorScheme.primary,
          selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        ),
    )

    NewsSource.entries
      .filter { it in enabledSources }
      .forEach { source ->
        val selected = selectedSource == source
        val label =
          if (source == NewsSource.LOCAL) {
            "Local (${localCity.substringBefore(",")})"
          } else {
            source.displayName
          }
        FilterChip(
          selected = selected,
          onClick = { onSelectSource(if (selected) null else source) },
          label = { Text(label) },
          leadingIcon = {
            Box(
              modifier =
                Modifier.size(8.dp)
                  .clip(CircleShape)
                  .background(if (selected) Color.White else source.brandAccent())
            )
          },
          modifier = Modifier.testTag("filter_source_${source.id}"),
          colors =
            FilterChipDefaults.filterChipColors(
              selectedContainerColor = source.brandAccent(),
              selectedLabelColor = Color.White,
            ),
        )
      }
  }
}

@Composable
fun TopicFilterBar(
  selectedTopic: NewsTopic?,
  onSelectTopic: (NewsTopic?) -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyRow(
    modifier = modifier.fillMaxWidth(),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    item {
      TopicPill(
        label = "All Beats",
        selected = selectedTopic == null,
        onClick = { onSelectTopic(null) },
      )
    }
    items(NewsTopic.entries) { topic ->
      val isSelected = selectedTopic == topic
      TopicPill(
        label = topic.label,
        selected = isSelected,
        onClick = { onSelectTopic(if (isSelected) null else topic) },
      )
    }
  }
}

@Composable
private fun TopicPill(label: String, selected: Boolean, onClick: () -> Unit) {
  Surface(
    shape = RoundedCornerShape(20.dp),
    color =
      if (selected) MaterialTheme.colorScheme.secondaryContainer
      else MaterialTheme.colorScheme.surfaceVariant,
    border =
      if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
      else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = Modifier.clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color =
        if (selected) MaterialTheme.colorScheme.onSecondaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
    )
  }
}

@Composable
fun ArticleThumbnail(
  imageUrl: String?,
  contentDescription: String,
  modifier: Modifier = Modifier,
) {
  if (imageUrl.isNullOrBlank()) {
    Image(
      painter = painterResource(id = R.drawable.img_editorial_fallback),
      contentDescription = contentDescription,
      contentScale = ContentScale.Crop,
      modifier = modifier,
    )
  } else {
    SubcomposeAsyncImage(
      model = imageUrl,
      contentDescription = contentDescription,
      contentScale = ContentScale.Crop,
      modifier = modifier,
      loading = {
        Box(
          modifier =
            Modifier.fillMaxSize()
              .background(
                Brush.linearGradient(
                  listOf(
                    MaterialTheme.colorScheme.surfaceVariant,
                    MaterialTheme.colorScheme.surface,
                  )
                )
              )
        )
      },
      error = {
        Image(
          painter = painterResource(id = R.drawable.img_editorial_fallback),
          contentDescription = contentDescription,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
      },
    )
  }
}

@Composable
fun LeadHeroStoryCard(
  article: NewsArticle,
  onClick: () -> Unit,
  onToggleBookmark: () -> Unit,
  onShare: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier =
      modifier
        .fillMaxWidth()
        .clip(MaterialTheme.shapes.large)
        .clickable(onClick = onClick)
        .testTag("hero_article_card"),
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
  ) {
    Column {
      Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
        ArticleThumbnail(
          imageUrl = article.imageUrl,
          contentDescription = article.title,
          modifier = Modifier.fillMaxSize(),
        )
        Box(
          modifier =
            Modifier.fillMaxSize()
              .background(
                Brush.verticalGradient(
                  colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                  startY = 180f,
                )
              )
        )
        Row(
          modifier =
            Modifier.align(Alignment.TopStart).padding(12.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          SourceBadge(
            source = article.source,
            subPublisher = article.publisherSubName,
            localCity = article.localEditionCity,
          )
          article.personalizationReason?.let { reason ->
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            ) {
              Text(
                text = reason,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              )
            }
          }
        }
      }

      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = article.title,
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 3,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = article.summary,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 3,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text =
              buildString {
                append(NewsViewModel.formatRelativeTime(article.publishedAtMillis))
                if (!article.author.isNullOrBlank()) {
                  append(" • ")
                  append(article.author)
                }
              },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
          )
          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
              onClick = onShare,
              modifier = Modifier.minimumInteractiveComponentSize().testTag("hero_share_button"),
            ) {
              Icon(
                imageVector = Icons.Outlined.Share,
                contentDescription = stringResource(R.string.action_share),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            IconButton(
              onClick = onToggleBookmark,
              modifier = Modifier.minimumInteractiveComponentSize().testTag("hero_bookmark_button"),
            ) {
              Icon(
                imageVector =
                  if (article.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                contentDescription =
                  if (article.isBookmarked) stringResource(R.string.action_remove_bookmark)
                  else stringResource(R.string.action_bookmark),
                tint =
                  if (article.isBookmarked) MaterialTheme.colorScheme.secondary
                  else MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun CompactStoryCard(
  article: NewsArticle,
  onClick: () -> Unit,
  onToggleBookmark: () -> Unit,
  onShare: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier =
      modifier
        .fillMaxWidth()
        .clip(MaterialTheme.shapes.medium)
        .clickable(onClick = onClick)
        .alpha(if (article.isRead) 0.82f else 1f)
        .testTag("story_card_${article.id}"),
    shape = MaterialTheme.shapes.medium,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            SourceBadge(
              source = article.source,
              subPublisher = article.publisherSubName,
              localCity = article.localEditionCity,
            )
            article.personalizationReason?.let { reason ->
              Text(
                text = reason,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = article.title,
            style =
              MaterialTheme.typography.titleLarge.copy(
                fontWeight = if (article.isRead) FontWeight.Normal else FontWeight.SemiBold
              ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
          )
        }

        ArticleThumbnail(
          imageUrl = article.imageUrl,
          contentDescription = article.title,
          modifier = Modifier.size(92.dp).clip(RoundedCornerShape(10.dp)),
        )
      }

      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = article.summary,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(modifier = Modifier.height(6.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text =
            "${NewsViewModel.formatRelativeTime(article.publishedAtMillis)} • ${article.topic.label}",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = onShare,
            modifier = Modifier.minimumInteractiveComponentSize(),
          ) {
            Icon(
              imageVector = Icons.Outlined.Share,
              contentDescription = stringResource(R.string.action_share),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(20.dp),
            )
          }
          IconButton(
            onClick = onToggleBookmark,
            modifier = Modifier.minimumInteractiveComponentSize(),
          ) {
            Icon(
              imageVector =
                if (article.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
              contentDescription =
                if (article.isBookmarked) stringResource(R.string.action_remove_bookmark)
                else stringResource(R.string.action_bookmark),
              tint =
                if (article.isBookmarked) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(20.dp),
            )
          }
        }
      }
    }
  }
}
