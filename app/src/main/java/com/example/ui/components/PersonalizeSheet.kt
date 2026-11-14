package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.model.NewsSource
import com.example.model.NewsTopic
import com.example.model.SourcePriority
import com.example.model.UserNewsPreferences

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PersonalizeBottomSheet(
  preferences: UserNewsPreferences,
  isDetectingCity: Boolean,
  onDismiss: () -> Unit,
  onToggleSource: (NewsSource, Boolean) -> Unit,
  onSetSourcePriority: (NewsSource, SourcePriority) -> Unit,
  onToggleTopic: (NewsTopic) -> Unit,
  onUpdateLocalCity: (String) -> Unit,
  onDetectCity: () -> Unit,
  onLocationDenied: () -> Unit,
  onAddKeyword: (String) -> Unit,
  onRemoveKeyword: (String) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val context = LocalContext.current

  var cityDraft by remember(preferences.localCity) { mutableStateOf(preferences.localCity) }
  var keywordDraft by remember { mutableStateOf("") }

  val locationPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      if (granted) {
        onDetectCity()
      } else {
        onLocationDenied()
      }
    }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp)
          .padding(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      Column {
        Text(
          text = stringResource(R.string.personalize_title),
          style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = stringResource(R.string.personalize_subtitle),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      // Section 1: Outlets & Priority Boost
      Text(
        text = stringResource(R.string.sources_section_title),
        style = MaterialTheme.typography.titleMedium,
      )

      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        NewsSource.entries.forEach { source ->
          val enabled = source in preferences.enabledSources
          val priority = preferences.sourcePriorities[source] ?: SourcePriority.STANDARD

          Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          ) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  SourceBadge(
                    source = source,
                    localCity = if (source == NewsSource.LOCAL) preferences.localCity else null,
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = source.tagline,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Switch(
                  checked = enabled,
                  onCheckedChange = { onToggleSource(source, it) },
                  modifier = Modifier.testTag("sheet_toggle_${source.id}"),
                )
              }
              if (enabled) {
                Spacer(modifier = Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                  SourcePriority.entries.forEachIndexed { idx, option ->
                    SegmentedButton(
                      selected = priority == option,
                      onClick = { onSetSourcePriority(source, option) },
                      shape =
                        SegmentedButtonDefaults.itemShape(
                          index = idx,
                          count = SourcePriority.entries.size,
                        ),
                    ) {
                      Text(option.label, style = MaterialTheme.typography.labelMedium)
                    }
                  }
                }
              }
            }
          }
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

      // Section 2: Local City Edition
      Text(
        text = stringResource(R.string.local_edition_title),
        style = MaterialTheme.typography.titleMedium,
      )

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
          modifier = Modifier.weight(1f).testTag("sheet_city_input"),
          shape = RoundedCornerShape(12.dp),
        )
        Button(
          onClick = { onUpdateLocalCity(cityDraft) },
          enabled = cityDraft.isNotBlank() && cityDraft.trim() != preferences.localCity,
          modifier = Modifier.testTag("sheet_city_apply_button"),
        ) {
          Text("Set")
        }
      }

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
        enabled = !isDetectingCity,
        modifier = Modifier.fillMaxWidth().testTag("sheet_detect_city_button"),
      ) {
        if (isDetectingCity) {
          CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
          Spacer(modifier = Modifier.width(8.dp))
        } else {
          Icon(
            Icons.Outlined.MyLocation,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
        }
        Text(stringResource(R.string.action_detect_city))
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

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

      // Section 3: Topics
      Text(
        text = stringResource(R.string.topics_section_title),
        style = MaterialTheme.typography.titleMedium,
      )

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        NewsTopic.entries.forEach { topic ->
          val selected = topic in preferences.preferredTopics
          FilterChip(
            selected = selected,
            onClick = { onToggleTopic(topic) },
            label = { Text(topic.label) },
          )
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

      // Section 4: Custom Tracked Keywords
      Text(
        text = stringResource(R.string.keywords_section_title),
        style = MaterialTheme.typography.titleMedium,
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedTextField(
          value = keywordDraft,
          onValueChange = { keywordDraft = it },
          placeholder = { Text(stringResource(R.string.keyword_hint)) },
          singleLine = true,
          modifier = Modifier.weight(1f).testTag("sheet_keyword_input"),
          shape = RoundedCornerShape(12.dp),
        )
        IconButton(
          onClick = {
            if (keywordDraft.isNotBlank()) {
              onAddKeyword(keywordDraft)
              keywordDraft = ""
            }
          },
          modifier = Modifier.testTag("sheet_keyword_add_button"),
        ) {
          Icon(Icons.Outlined.Add, contentDescription = "Add keyword")
        }
      }

      if (preferences.keywords.isNotEmpty()) {
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          preferences.keywords.forEach { kw ->
            InputChip(
              selected = true,
              onClick = { onRemoveKeyword(kw) },
              label = { Text(kw) },
              trailingIcon = {
                Icon(
                  Icons.Outlined.Close,
                  contentDescription = "Remove $kw",
                  modifier = Modifier.size(16.dp),
                )
              },
            )
          }
        }
      }

      Button(
        onClick = onDismiss,
        modifier = Modifier.fillMaxWidth().testTag("sheet_done_button"),
      ) {
        Text("Done")
      }
    }
  }
}
