package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.model.NewsSource
import com.example.model.SourcePriority
import com.example.model.UserNewsPreferences
import com.example.ui.NewsUiState
import com.example.ui.components.SourceBadge
import com.example.ui.components.brandAccent
import com.example.ui.theme.LocalEmerald

@Composable
fun SourcesScreen(
  uiState: NewsUiState,
  onToggleSource: (NewsSource, Boolean) -> Unit,
  onSetSourcePriority: (NewsSource, SourcePriority) -> Unit,
  onUpdateLocalCity: (String) -> Unit,
  onDetectCity: () -> Unit,
  onLocationDenied: () -> Unit,
  onOpenChannelFeed: (NewsSource) -> Unit,
  onOpenPersonalize: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  var cityDraft by remember(uiState.preferences.localCity) {
    mutableStateOf(uiState.preferences.localCity)
  }

  val locationPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      if (granted) onDetectCity() else onLocationDenied()
    }

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 760.dp),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text(
              text = "Outlets & Local Edition",
              style = MaterialTheme.typography.headlineMedium,
            )
            Text(
              text = "Manage NBC, CNBC, CNN, and your city's local news feed.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          OutlinedButton(
            onClick = onOpenPersonalize,
            modifier = Modifier.testTag("sources_personalize_button"),
          ) {
            Icon(Icons.Outlined.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Topics")
          }
        }
      }

      // Local Edition Hero Card
      item {
        Card(
          shape = MaterialTheme.shapes.large,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          Column {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 7f)) {
              Image(
                painter = painterResource(id = R.drawable.img_local_banner),
                contentDescription = "Local city skyline banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
              )
              Box(
                modifier =
                  Modifier.fillMaxSize()
                    .background(
                      Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                      )
                    )
              )
              Row(
                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  imageVector = Icons.Outlined.LocationOn,
                  contentDescription = null,
                  tint = Color.White,
                )
                Column {
                  Text(
                    text = "LOCAL EDITION",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.85f),
                  )
                  Text(
                    text = uiState.preferences.localCity,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                  )
                }
              }
            }

            Column(
              modifier = Modifier.padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                OutlinedTextField(
                  value = cityDraft,
                  onValueChange = { cityDraft = it },
                  label = { Text(stringResource(R.string.local_city_hint)) },
                  singleLine = true,
                  modifier = Modifier.weight(1f).testTag("sources_city_input"),
                  shape = RoundedCornerShape(12.dp),
                )
                Button(
                  onClick = { onUpdateLocalCity(cityDraft) },
                  enabled = cityDraft.isNotBlank() && cityDraft.trim() != uiState.preferences.localCity,
                  modifier = Modifier.testTag("sources_city_apply_button"),
                ) {
                  Text("Update")
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                OutlinedButton(
                  onClick = {
                    val granted =
                      ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                      ) == PackageManager.PERMISSION_GRANTED
                    if (granted) {
                      onDetectCity()
                    } else {
                      locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    }
                  },
                  enabled = !uiState.isDetectingCity,
                  modifier = Modifier.weight(1f).testTag("sources_detect_city_button"),
                ) {
                  if (uiState.isDetectingCity) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                  } else {
                    Icon(
                      Icons.Outlined.MyLocation,
                      contentDescription = null,
                      modifier = Modifier.size(16.dp),
                    )
                  }
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(stringResource(R.string.action_detect_city))
                }

                Button(
                  onClick = { onOpenChannelFeed(NewsSource.LOCAL) },
                  modifier = Modifier.testTag("sources_open_local_button"),
                ) {
                  Text("Read Local")
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                UserNewsPreferences.PRESET_CITIES.forEach { preset ->
                  SuggestionChip(
                    onClick = {
                      cityDraft = preset
                      onUpdateLocalCity(preset)
                    },
                    label = { Text(preset) },
                  )
                }
              }
            }
          }
        }
      }

      item {
        Text(
          text = "News Outlets",
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.padding(top = 4.dp),
        )
      }

      items(NewsSource.entries) { source ->
        val enabled = source in uiState.preferences.enabledSources
        val priority = uiState.preferences.sourcePriorities[source] ?: SourcePriority.STANDARD
        val storyCount = uiState.allArticlesCountBySource[source] ?: 0

        Card(
          shape = MaterialTheme.shapes.medium,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  SourceBadge(
                    source = source,
                    localCity =
                      if (source == NewsSource.LOCAL) uiState.preferences.localCity else null,
                  )
                  Text(
                    text = "$storyCount stories",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = source.tagline,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Switch(
                checked = enabled,
                onCheckedChange = { onToggleSource(source, it) },
                modifier = Modifier.testTag("channel_switch_${source.id}"),
              )
            }

            if (enabled) {
              HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                SingleChoiceSegmentedButtonRow {
                  SourcePriority.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                      selected = priority == option,
                      onClick = { onSetSourcePriority(source, option) },
                      shape =
                        SegmentedButtonDefaults.itemShape(
                          index = index,
                          count = SourcePriority.entries.size,
                        ),
                    ) {
                      Text(option.label, style = MaterialTheme.typography.labelMedium)
                    }
                  }
                }

                TextButton(
                  onClick = { onOpenChannelFeed(source) },
                  modifier = Modifier.testTag("open_channel_${source.id}"),
                ) {
                  Text(
                    text = "Open",
                    color = if (source == NewsSource.LOCAL) LocalEmerald else source.brandAccent(),
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (source == NewsSource.LOCAL) LocalEmerald else source.brandAccent(),
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
