package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.model.NewsArticle
import com.example.ui.components.ArticleThumbnail
import com.example.ui.components.SourceBadge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleDetailScreen(
  article: NewsArticle,
  showBackButton: Boolean,
  onBack: () -> Unit,
  onToggleBookmark: () -> Unit,
  onShare: () -> Unit,
  onOpenExternalBrowser: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var showFullWebPage by rememberSaveable(article.id) { mutableStateOf(false) }
  var webProgress by remember { mutableIntStateOf(0) }

  val publishedFormatted = remember(article.publishedAtMillis) {
    SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.US)
      .format(Date(article.publishedAtMillis))
  }

  Surface(
    modifier = modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background,
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      TopAppBar(
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
          ),
        navigationIcon = {
          if (showBackButton) {
            IconButton(
              onClick = onBack,
              modifier = Modifier.minimumInteractiveComponentSize().testTag("detail_back_button"),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(R.string.action_back),
              )
            }
          }
        },
        title = {
          SourceBadge(
            source = article.source,
            subPublisher = article.publisherSubName,
            localCity = article.localEditionCity,
          )
        },
        actions = {
          IconButton(
            onClick = onToggleBookmark,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("detail_bookmark_button"),
          ) {
            Icon(
              imageVector =
                if (article.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
              contentDescription =
                if (article.isBookmarked) stringResource(R.string.action_remove_bookmark)
                else stringResource(R.string.action_bookmark),
              tint =
                if (article.isBookmarked) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.onSurface,
            )
          }
          IconButton(
            onClick = onShare,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("detail_share_button"),
          ) {
            Icon(
              imageVector = Icons.Outlined.Share,
              contentDescription = stringResource(R.string.action_share),
            )
          }
          IconButton(
            onClick = onOpenExternalBrowser,
            modifier =
              Modifier.minimumInteractiveComponentSize().testTag("detail_open_browser_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
              contentDescription = stringResource(R.string.action_open_browser),
            )
          }
        },
      )

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

      // Reader Mode Toggle: Briefing vs Live Publisher WebView
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
      ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth()) {
          SegmentedButton(
            selected = !showFullWebPage,
            onClick = { showFullWebPage = false },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            icon = { Icon(Icons.AutoMirrored.Outlined.Article, contentDescription = null) },
          ) {
            Text(stringResource(R.string.action_read_summary))
          }
          SegmentedButton(
            selected = showFullWebPage,
            onClick = { showFullWebPage = true },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            icon = { Icon(Icons.Outlined.Public, contentDescription = null) },
          ) {
            Text(stringResource(R.string.action_read_full_story))
          }
        }
      }

      if (showFullWebPage) {
        if (webProgress in 1..99) {
          LinearProgressIndicator(
            progress = { webProgress / 100f },
            modifier = Modifier.fillMaxWidth(),
          )
        }
        AndroidView(
          factory = { ctx ->
            WebView(ctx).apply {
              settings.javaScriptEnabled = true
              settings.domStorageEnabled = true
              settings.allowFileAccess = false
              settings.allowContentAccess = false
              settings.loadWithOverviewMode = true
              settings.useWideViewPort = true
              webViewClient =
                object : WebViewClient() {
                  override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?,
                  ): Boolean {
                    val uri = request?.url ?: return false
                    val scheme = uri.scheme?.lowercase(Locale.US)
                    if (scheme == "https" || scheme == "http") {
                      return false
                    }
                    return try {
                      ctx.startActivity(Intent(Intent.ACTION_VIEW, uri))
                      true
                    } catch (_: Exception) {
                      true
                    }
                  }
                }
              webChromeClient =
                object : WebChromeClient() {
                  override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    webProgress = newProgress
                  }
                }
              if (article.url.startsWith("https://") || article.url.startsWith("http://")) {
                loadUrl(article.url)
              }
            }
          },
          update = { webView ->
            if (
              (article.url.startsWith("https://") || article.url.startsWith("http://")) &&
                webView.url != article.url
            ) {
              webView.loadUrl(article.url)
            }
          },
          modifier = Modifier.weight(1f).fillMaxWidth().testTag("detail_webview"),
        )
      } else {
        Column(
          modifier =
            Modifier.weight(1f)
              .fillMaxWidth()
              .verticalScroll(rememberScrollState())
              .padding(horizontal = 20.dp, vertical = 16.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Column(modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth()) {
            article.personalizationReason?.let { reason ->
              Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.secondaryContainer,
              ) {
                Text(
                  text = reason,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSecondaryContainer,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
              }
              Spacer(modifier = Modifier.height(10.dp))
            }

            Text(
              text = article.title,
              style = MaterialTheme.typography.headlineMedium,
              color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
              text =
                buildString {
                  append(publishedFormatted)
                  if (!article.author.isNullOrBlank()) {
                    append("  •  By ")
                    append(article.author)
                  }
                },
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp))

            ArticleThumbnail(
              imageUrl = article.imageUrl,
              contentDescription = article.title,
              modifier =
                Modifier.fillMaxWidth()
                  .aspectRatio(16f / 9f)
                  .clip(MaterialTheme.shapes.large),
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
              text = article.summary,
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Button(
                onClick = { showFullWebPage = true },
                modifier = Modifier.weight(1f).testTag("detail_read_full_button"),
              ) {
                Icon(Icons.Outlined.Public, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.action_read_full_story))
              }
              OutlinedButton(
                onClick = onOpenExternalBrowser,
                modifier = Modifier.weight(1f).testTag("detail_browser_button"),
              ) {
                Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.action_open_browser))
              }
            }
          }
        }
      }
    }
  }
}
