package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.model.NewsArticle
import com.example.ui.NewsViewModel
import com.example.ui.components.PersonalizeBottomSheet
import com.example.ui.screens.ArticleDetailScreen
import com.example.ui.screens.ForYouScreen
import com.example.ui.screens.SavedScreen
import com.example.ui.screens.SourcesScreen
import com.example.ui.theme.PulseWireTheme

private const val ROUTE_FOR_YOU = "for_you"
private const val ROUTE_SOURCES = "sources"
private const val ROUTE_SAVED = "saved"

private data class TopDestination(
  val route: String,
  val labelRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
)

private val TOP_DESTINATIONS =
  listOf(
    TopDestination(
      route = ROUTE_FOR_YOU,
      labelRes = R.string.nav_for_you,
      selectedIcon = Icons.Filled.Newspaper,
      unselectedIcon = Icons.Outlined.Newspaper,
    ),
    TopDestination(
      route = ROUTE_SOURCES,
      labelRes = R.string.nav_sources,
      selectedIcon = Icons.Filled.Public,
      unselectedIcon = Icons.Outlined.Public,
    ),
    TopDestination(
      route = ROUTE_SAVED,
      labelRes = R.string.nav_saved,
      selectedIcon = Icons.Filled.Bookmarks,
      unselectedIcon = Icons.Outlined.Bookmarks,
    ),
  )

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent { PulseWireTheme { PulseWireApp() } }
  }
}

@Composable
fun PulseWireApp(viewModel: NewsViewModel = viewModel()) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val navController = rememberNavController()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_FOR_YOU
  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }

  var showPersonalizeSheet by rememberSaveable { mutableStateOf(false) }

  LaunchedEffect(uiState.statusMessage) {
    val msg = uiState.statusMessage
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearStatusMessage()
    }
  }

  val navigateToTab: (String) -> Unit = { route ->
    navController.navigate(route) {
      popUpTo(navController.graph.findStartDestination().id) { saveState = true }
      launchSingleTop = true
      restoreState = true
    }
  }

  val locationDeniedText = stringResource(R.string.location_denied_msg)

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useNavigationRail = maxWidth >= 600.dp
    val useSideBySideDetail = maxWidth >= 900.dp

    if (!useSideBySideDetail && uiState.selectedArticle != null) {
      BackHandler { viewModel.closeArticle() }
    }

    Scaffold(
      contentWindowInsets = WindowInsets.safeDrawing,
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      bottomBar = {
        if (!useNavigationRail && uiState.selectedArticle == null) {
          NavigationBar {
            TOP_DESTINATIONS.forEach { dest ->
              val selected = currentRoute == dest.route
              val label = stringResource(dest.labelRes)
              NavigationBarItem(
                selected = selected,
                onClick = { navigateToTab(dest.route) },
                icon = {
                  if (dest.route == ROUTE_SAVED && uiState.bookmarkedArticles.isNotEmpty()) {
                    BadgedBox(
                      badge = { Badge { Text("${uiState.bookmarkedArticles.size}") } }
                    ) {
                      Icon(
                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                        contentDescription = label,
                      )
                    }
                  } else {
                    Icon(
                      imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                      contentDescription = label,
                    )
                  }
                },
                label = { Text(label) },
                modifier = Modifier.testTag("nav_${dest.route}"),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        if (useNavigationRail) {
          NavigationRail {
            TOP_DESTINATIONS.forEach { dest ->
              val selected = currentRoute == dest.route
              val label = stringResource(dest.labelRes)
              NavigationRailItem(
                selected = selected,
                onClick = { navigateToTab(dest.route) },
                icon = {
                  if (dest.route == ROUTE_SAVED && uiState.bookmarkedArticles.isNotEmpty()) {
                    BadgedBox(
                      badge = { Badge { Text("${uiState.bookmarkedArticles.size}") } }
                    ) {
                      Icon(
                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                        contentDescription = label,
                      )
                    }
                  } else {
                    Icon(
                      imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                      contentDescription = label,
                    )
                  }
                },
                label = { Text(label) },
                modifier = Modifier.testTag("rail_${dest.route}"),
              )
            }
          }
          VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }

        val mainPaneModifier =
          if (useSideBySideDetail && uiState.selectedArticle != null) {
            Modifier.weight(1f).fillMaxHeight()
          } else {
            Modifier.fillMaxSize()
          }

        Box(modifier = mainPaneModifier) {
          NavHost(
            navController = navController,
            startDestination = ROUTE_FOR_YOU,
            modifier = Modifier.fillMaxSize(),
          ) {
            composable(ROUTE_FOR_YOU) {
              ForYouScreen(
                uiState = uiState,
                onSelectSourceFilter = viewModel::selectSourceFilter,
                onSelectTopicFilter = viewModel::selectTopicFilter,
                onToggleSearch = {
                  viewModel.toggleSearchExpanded(!uiState.isSearchExpanded)
                },
                onSearchQueryChange = viewModel::updateSearchQuery,
                onOpenPersonalize = { showPersonalizeSheet = true },
                onRefresh = { viewModel.refreshFeeds(forceAllSources = false) },
                onOpenArticle = viewModel::openArticle,
                onToggleBookmark = viewModel::toggleBookmark,
                onShareArticle = { article -> shareArticle(context, article) },
              )
            }

            composable(ROUTE_SOURCES) {
              SourcesScreen(
                uiState = uiState,
                onToggleSource = viewModel::toggleSource,
                onSetSourcePriority = viewModel::setSourcePriority,
                onUpdateLocalCity = viewModel::updateLocalCity,
                onDetectCity = viewModel::detectCityFromLocation,
                onLocationDenied = { viewModel.showMessage(locationDeniedText) },
                onOpenChannelFeed = { source ->
                  viewModel.selectSourceFilter(source)
                  navigateToTab(ROUTE_FOR_YOU)
                },
                onOpenPersonalize = { showPersonalizeSheet = true },
              )
            }

            composable(ROUTE_SAVED) {
              SavedScreen(
                bookmarkedArticles = uiState.bookmarkedArticles,
                onOpenArticle = viewModel::openArticle,
                onToggleBookmark = viewModel::toggleBookmark,
                onShareArticle = { article -> shareArticle(context, article) },
              )
            }
          }

          if (!useSideBySideDetail) {
            androidx.compose.animation.AnimatedVisibility(
              visible = uiState.selectedArticle != null,
              enter = slideInHorizontally { it / 3 } + fadeIn(),
              exit = slideOutHorizontally { it / 3 } + fadeOut(),
            ) {
              uiState.selectedArticle?.let { article ->
                ArticleDetailScreen(
                  article = article,
                  showBackButton = true,
                  onBack = viewModel::closeArticle,
                  onToggleBookmark = { viewModel.toggleBookmark(article) },
                  onShare = { shareArticle(context, article) },
                  onOpenExternalBrowser = { openExternalUrl(context, article.url) },
                )
              }
            }
          }
        }

        if (useSideBySideDetail && uiState.selectedArticle != null) {
          VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          uiState.selectedArticle?.let { article ->
            ArticleDetailScreen(
              article = article,
              showBackButton = true,
              onBack = viewModel::closeArticle,
              onToggleBookmark = { viewModel.toggleBookmark(article) },
              onShare = { shareArticle(context, article) },
              onOpenExternalBrowser = { openExternalUrl(context, article.url) },
              modifier = Modifier.width(460.dp).fillMaxHeight(),
            )
          }
        }
      }
    }

    if (showPersonalizeSheet) {
      PersonalizeBottomSheet(
        preferences = uiState.preferences,
        isDetectingCity = uiState.isDetectingCity,
        onDismiss = { showPersonalizeSheet = false },
        onToggleSource = viewModel::toggleSource,
        onSetSourcePriority = viewModel::setSourcePriority,
        onToggleTopic = viewModel::toggleTopic,
        onUpdateLocalCity = viewModel::updateLocalCity,
        onDetectCity = viewModel::detectCityFromLocation,
        onLocationDenied = { viewModel.showMessage(locationDeniedText) },
        onAddKeyword = viewModel::addKeyword,
        onRemoveKeyword = viewModel::removeKeyword,
      )
    }
  }
}

private fun shareArticle(context: Context, article: NewsArticle) {
  val sendIntent =
    Intent(Intent.ACTION_SEND).apply {
      type = "text/plain"
      putExtra(Intent.EXTRA_SUBJECT, article.title)
      putExtra(
        Intent.EXTRA_TEXT,
        "${article.title} (${article.source.displayName})\n${article.url}",
      )
    }
  context.startActivity(Intent.createChooser(sendIntent, "Share story"))
}

private fun openExternalUrl(context: Context, url: String) {
  val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
  context.startActivity(viewIntent)
}
