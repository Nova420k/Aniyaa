package com.nyaa.aniyaa

import android.app.ActivityManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nyaa.aniyaa.data.model.CatalogDeepLink
import com.nyaa.aniyaa.data.model.CatalogDeepLinks
import com.nyaa.aniyaa.data.model.CatalogSite
import com.nyaa.aniyaa.data.model.DarkMode
import com.nyaa.aniyaa.data.model.SearchParams
import com.nyaa.aniyaa.data.model.Torrent
import com.nyaa.aniyaa.data.model.stubTorrent
import com.nyaa.aniyaa.data.network.AppHttpClient
import com.nyaa.aniyaa.ui.lock.LockScreen
import com.nyaa.aniyaa.ui.screens.BookmarksScreen
import com.nyaa.aniyaa.ui.screens.OnboardingScreen
import com.nyaa.aniyaa.ui.screens.SearchHistoryScreen
import com.nyaa.aniyaa.ui.screens.SearchScreen
import com.nyaa.aniyaa.ui.screens.SettingsScreen
import com.nyaa.aniyaa.ui.screens.TorrentDetailScreen
import com.nyaa.aniyaa.ui.theme.AniyaaTheme
import com.nyaa.aniyaa.ui.viewmodel.BookmarkViewModel
import com.nyaa.aniyaa.ui.viewmodel.SearchHistoryViewModel
import com.nyaa.aniyaa.ui.viewmodel.SearchViewModel
import com.nyaa.aniyaa.util.HighRefreshRate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : AppCompatActivity() {

    private lateinit var app: AniyaaApplication
    private val incomingLink = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app = application as AniyaaApplication
        AppHttpClient.configure(cacheDir)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        HighRefreshRate.apply(this)
        applyPrivacyFlags()
        incomingLink.value = intent?.data
        app.lockController.prepare(app.prefs.lockEnabled, app.prefs.hasPin)
        setContent {
            val prefs = app.prefs
            var themeIndex by remember { mutableIntStateOf(prefs.themeIndex) }
            var darkMode by remember { mutableStateOf(prefs.darkMode) }
            val locked by app.lockController.locked.collectAsStateWithLifecycle()
            var pinError by remember { mutableStateOf<String?>(null) }
            var lockRemainingMs by remember { mutableStateOf(0L) }
            val biometricAvailable = remember(prefs.biometricUnlockEnabled) {
                prefs.biometricUnlockEnabled &&
                    BiometricManager.from(this).canAuthenticate(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG
                    ) == BiometricManager.BIOMETRIC_SUCCESS
            }

            AniyaaTheme(darkMode = darkMode, themeIndex = themeIndex) {
                Box(modifier = Modifier.fillMaxSize()) {
                    var showOnboarding by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        if (prefs.onboardingComplete) return@LaunchedEffect
                        val hasData = withContext(Dispatchers.IO) {
                            app.bookmarkRepository.getAll().isNotEmpty() ||
                                app.historyRepository.getAll().isNotEmpty()
                        }
                        if (hasData) {
                            prefs.onboardingComplete = true
                        } else {
                            showOnboarding = true
                        }
                    }
                    if (locked && prefs.lockEnabled && prefs.hasPin) {
                        LaunchedEffect(locked, lockRemainingMs) {
                            while (lockRemainingMs > 0L) {
                                delay(1000)
                                lockRemainingMs = prefs.pinLockRemainingMs()
                            }
                        }
                        LockScreen(
                            error = pinError,
                            lockRemainingMs = lockRemainingMs,
                            biometricAvailable = biometricAvailable,
                            onUnlockWithPin = { pin ->
                                lifecycleScope.launch {
                                    val remaining = withContext(Dispatchers.Default) { prefs.pinLockRemainingMs() }
                                    if (remaining > 0L) {
                                        lockRemainingMs = remaining
                                        pinError = "Too many attempts"
                                        return@launch
                                    }
                                    val ok = withContext(Dispatchers.Default) { prefs.verifyPin(pin) }
                                    if (ok) {
                                        pinError = null
                                        lockRemainingMs = 0L
                                        app.lockController.unlock()
                                    } else {
                                        lockRemainingMs = prefs.pinLockRemainingMs()
                                        pinError = if (lockRemainingMs > 0L) "Too many attempts" else "Wrong PIN"
                                    }
                                }
                            },
                            onUnlockWithBiometric = { promptBiometric { pinError = null; lockRemainingMs = 0L } }
                        )
                    } else {
                        AniyaaApp(
                            currentThemeIndex = themeIndex,
                            darkMode = darkMode,
                            pendingDeepLink = incomingLink.value,
                            onDeepLinkConsumed = { incomingLink.value = null },
                            onThemeSelected = { index ->
                                themeIndex = index
                                prefs.themeIndex = index
                            },
                            onDarkModeSelected = { mode ->
                                darkMode = mode
                                prefs.darkMode = mode
                            },
                            onPrivacyFlagsChanged = { applyPrivacyFlags() }
                        )
                        if (showOnboarding) {
                            OnboardingScreen(
                                preferredTorrentPackage = prefs.preferredTorrentPackage,
                                onPreferredTorrentPackage = { prefs.preferredTorrentPackage = it },
                                onFinished = {
                                    prefs.onboardingComplete = true
                                    showOnboarding = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingLink.value = intent.data
    }

    override fun onResume() {
        super.onResume()
        HighRefreshRate.apply(this)
        applyPrivacyFlags()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) HighRefreshRate.apply(this)
    }

    private fun applyPrivacyFlags() {
        if (app.prefs.hideScreenshots) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        val am = getSystemService(ActivityManager::class.java)
        am?.appTasks?.forEach { it.setExcludeFromRecents(app.prefs.hideFromRecents) }
    }

    private fun promptBiometric(onSuccess: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    app.lockController.unlock()
                    onSuccess()
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Aniyaa")
            .setNegativeButtonText("Use PIN")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
        prompt.authenticate(info)
    }
}

private val navFadeSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMedium
)

private val navSlideSpring = spring<IntOffset>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMedium
)

private data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem("search", "Search", Icons.Filled.Search, Icons.Outlined.Search),
    BottomNavItem("history", "History", Icons.Filled.History, Icons.Outlined.History),
    BottomNavItem("bookmarks", "Bookmarks", Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder),
    BottomNavItem("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
)

private fun encodeNavId(id: String): String =
    URLEncoder.encode(id, StandardCharsets.UTF_8.toString())

private fun decodeNavId(id: String): String =
    URLDecoder.decode(id, StandardCharsets.UTF_8.toString())

@Composable
fun AniyaaApp(
    currentThemeIndex: Int,
    darkMode: DarkMode,
    pendingDeepLink: Uri?,
    onDeepLinkConsumed: () -> Unit,
    onThemeSelected: (Int) -> Unit,
    onDarkModeSelected: (DarkMode) -> Unit,
    onPrivacyFlagsChanged: () -> Unit
) {
    val navController = rememberNavController()
    var selectedTorrent by rememberSaveable { mutableStateOf<Torrent?>(null) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val searchHistoryViewModel: SearchHistoryViewModel = viewModel()
    val searchViewModel: SearchViewModel = viewModel()
    val bookmarkViewModel: BookmarkViewModel = viewModel()
    val expanded = LocalConfiguration.current.screenWidthDp >= 700
    val showBottomBar = currentRoute?.startsWith("detail") != true
    val prefs = AniyaaApplication.instance.prefs
    var pendingNsfwLink by remember { mutableStateOf<CatalogDeepLink?>(null) }
    val pendingNsfwParams by searchViewModel.pendingNsfwParams.collectAsStateWithLifecycle()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var detailBackProgress by remember { mutableFloatStateOf(0f) }
    PredictiveBackHandler(enabled = expanded && selectedTorrent != null) { progress ->
        try {
            progress.collect { event ->
                detailBackProgress = event.progress
            }
            selectedTorrent = null
            detailBackProgress = 0f
        } catch (_: kotlinx.coroutines.CancellationException) {
            detailBackProgress = 0f
        }
    }

    LaunchedEffect(expanded) {
        val torrent = selectedTorrent
        val onDetail = currentRoute?.startsWith("detail") == true
        if (expanded && onDetail) {
            navController.navigateUp()
        } else if (!expanded && torrent != null && !onDetail) {
            navController.navigate("detail/${torrent.site.id}/${encodeNavId(torrent.navId())}") {
                launchSingleTop = true
            }
        }
    }

    fun openSearch(query: String, site: CatalogSite) {
        searchViewModel.applyParams(SearchParams(query = query, site = site))
        selectedTorrent = null
        navController.navigate("search") {
            popUpTo("search") { inclusive = true }
            launchSingleTop = true
        }
    }

    fun openUser(username: String, site: CatalogSite) {
        openSearch("user:$username", site)
    }

    fun goToSearch() {
        navController.navigate("search") {
            popUpTo("search") { inclusive = true }
            launchSingleTop = true
        }
    }

    val openTorrent = remember(navController, expanded) {
        { torrent: Torrent ->
            selectedTorrent = torrent
            searchHistoryViewModel.recordViewed(torrent)
            if (!expanded) {
                navController.navigate("detail/${torrent.site.id}/${encodeNavId(torrent.navId())}")
            }
        }
    }

    fun applyCatalogLink(link: CatalogDeepLink) {
        if (link.requiresNsfw && !prefs.sukebeiEnabled) {
            pendingNsfwLink = link
            return
        }
        if (link.viewId != null) {
            searchViewModel.switchSite(link.site)
            selectedTorrent = searchViewModel.torrentByNavId(link.viewId, link.site)
                ?: bookmarkViewModel.torrentByNavId(link.viewId, link.site)
                ?: stubTorrent(link.viewId, link.site)
            searchHistoryViewModel.recordViewed(selectedTorrent!!)
            if (!expanded) {
                navController.navigate("detail/${link.site.id}/${encodeNavId(link.viewId)}")
            }
        } else if (link.savedSearchId != null) {
            // loaded asynchronously below
        } else if (link.searchParams != null) {
            searchViewModel.applyParams(link.searchParams)
            goToSearch()
        } else {
            searchViewModel.switchSite(link.site)
            goToSearch()
        }
    }

    LaunchedEffect(pendingDeepLink) {
        val data = pendingDeepLink ?: return@LaunchedEffect
        onDeepLinkConsumed()
        val link = CatalogDeepLinks.parse(data.toString()) ?: return@LaunchedEffect
        if (link.requiresNsfw && !prefs.sukebeiEnabled) {
            pendingNsfwLink = link
            return@LaunchedEffect
        }
        if (link.savedSearchId != null) {
            val saved = searchHistoryViewModel.savedSearchById(link.savedSearchId)
            if (saved != null) {
                searchViewModel.applySavedSearch(saved)
                goToSearch()
            }
            return@LaunchedEffect
        }
        applyCatalogLink(link)
    }

    if (pendingNsfwLink != null || pendingNsfwParams != null) {
        AlertDialog(
            onDismissRequest = {
                pendingNsfwLink = null
                searchViewModel.dismissPendingNsfw()
            },
            title = { Text("Sukebei is 18+") },
            text = { Text("This opens Sukebei, which lists adult content. You must be 18 or older to continue.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val link = pendingNsfwLink
                        pendingNsfwLink = null
                        if (link != null) {
                            prefs.sukebeiEnabled = true
                            applyCatalogLink(link.copy(requiresNsfw = false))
                        } else {
                            searchViewModel.confirmPendingNsfw()
                            goToSearch()
                        }
                    }
                ) { Text("I am 18+") }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingNsfwLink = null
                    searchViewModel.dismissPendingNsfw()
                }) { Text("Cancel") }
            }
        )
    }

    fun navigateTab(route: String) {
        selectedTorrent = null
        if (currentRoute != route) {
            navController.navigate(route) {
                popUpTo("search") { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar && !expanded,
                enter = slideInVertically(
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ) { it },
                exit = slideOutVertically(
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ) { it }
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 0.dp
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = {
                                Text(
                                    item.label,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            selected = selected,
                            onClick = { navigateTab(item.route) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (showBottomBar && !expanded) innerPadding.calculateBottomPadding() else 0.dp)
        ) {
            if (expanded && showBottomBar) {
                NavigationRail(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigateTab(item.route) },
                            icon = {
                                Icon(
                                    if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
            NavHost(
                navController = navController,
                startDestination = "search",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                enterTransition = {
                    fadeIn(navFadeSpring) + slideInHorizontally(navSlideSpring) { it / 8 }
                },
                exitTransition = { fadeOut(navFadeSpring) },
                popEnterTransition = {
                    fadeIn(navFadeSpring) + slideInHorizontally(navSlideSpring) { -it / 8 }
                },
                popExitTransition = {
                    fadeOut(navFadeSpring) + slideOutHorizontally(navSlideSpring) { it / 8 }
                }
            ) {
                composable("search") {
                    SearchScreen(
                        onTorrentClick = openTorrent,
                        onOpenSettings = { navigateTab("settings") },
                        searchViewModel = searchViewModel,
                        searchHistoryViewModel = searchHistoryViewModel,
                        bookmarkViewModel = bookmarkViewModel
                    )
                }
                composable("history") {
                    SearchHistoryScreen(
                        onHistoryItemClick = { entry ->
                            searchViewModel.applyHistory(entry)
                            goToSearch()
                        },
                        onSavedSearchClick = { saved ->
                            searchViewModel.applySavedSearch(saved)
                            goToSearch()
                        },
                        onTorrentClick = openTorrent,
                        searchHistoryViewModel = searchHistoryViewModel
                    )
                }
                composable("bookmarks") {
                    BookmarksScreen(
                        onTorrentClick = openTorrent,
                        bookmarkViewModel = bookmarkViewModel
                    )
                }
                composable("settings") {
                    SettingsScreen(
                        currentThemeIndex = currentThemeIndex,
                        darkMode = darkMode,
                        onThemeSelected = onThemeSelected,
                        onDarkModeSelected = onDarkModeSelected,
                        onPrivacyFlagsChanged = onPrivacyFlagsChanged
                    )
                }
                composable(
                    route = "detail/{site}/{torrentId}",
                    arguments = listOf(
                        navArgument("site") { type = NavType.StringType },
                        navArgument("torrentId") { type = NavType.StringType }
                    )
                ) { entry ->
                    val navId = decodeNavId(entry.arguments?.getString("torrentId").orEmpty())
                    val site = CatalogSite.fromId(entry.arguments?.getString("site").orEmpty())
                    val cached = selectedTorrent?.takeIf { it.matchesNavId(navId) && it.site == site }
                        ?: searchViewModel.torrentByNavId(navId, site)
                        ?: bookmarkViewModel.torrentByNavId(navId, site)
                    TorrentDetailGate(
                        navId = navId,
                        site = site,
                        cached = cached,
                        onNavigateBack = { navController.navigateUp() },
                        onOpenUser = ::openUser,
                        onOpenCatalogLink = { applyCatalogLink(it) },
                        onFollow = { name, query, followSite ->
                            scope.launch {
                                searchViewModel.saveSearch(
                                    SearchParams(query = query, site = followSite),
                                    name,
                                    notify = false
                                )
                            }
                        },
                        onSearch = { query, followSite -> openSearch(query, followSite) },
                        bookmarkViewModel = bookmarkViewModel,
                        searchHistoryViewModel = searchHistoryViewModel
                    )
                }
            }
            if (expanded && showBottomBar) {
                Box(
                    modifier = Modifier
                        .weight(1.15f)
                        .fillMaxHeight()
                        .graphicsLayer {
                            translationX = size.width * detailBackProgress
                            alpha = 1f - (detailBackProgress * 0.25f)
                        }
                ) {
                    val torrent = selectedTorrent
                    if (torrent != null) {
                        TorrentDetailGate(
                            navId = torrent.navId(),
                            site = torrent.site,
                            cached = torrent,
                            onNavigateBack = { selectedTorrent = null },
                            onOpenUser = ::openUser,
                            onOpenCatalogLink = { applyCatalogLink(it) },
                            onFollow = { name, query, followSite ->
                                scope.launch {
                                    searchViewModel.saveSearch(
                                        SearchParams(query = query, site = followSite),
                                        name,
                                        notify = false
                                    )
                                }
                            },
                            onSearch = { query, followSite -> openSearch(query, followSite) },
                            bookmarkViewModel = bookmarkViewModel,
                            searchHistoryViewModel = searchHistoryViewModel
                        )
                    } else {
                        EmptyDetailPane()
                    }
                }
            }
        }
    }
}

@Composable
private fun TorrentDetailGate(
    navId: String,
    site: CatalogSite,
    cached: Torrent?,
    onNavigateBack: () -> Unit,
    onOpenUser: (String, CatalogSite) -> Unit,
    onOpenCatalogLink: (CatalogDeepLink) -> Unit,
    onFollow: (String, String, CatalogSite) -> Unit = { _, _, _ -> },
    onSearch: (String, CatalogSite) -> Unit = { _, _ -> },
    bookmarkViewModel: BookmarkViewModel,
    searchHistoryViewModel: SearchHistoryViewModel
) {
    var torrent by remember(navId, site) { mutableStateOf(cached) }
    var loading by remember(navId, site) { mutableStateOf(cached == null && navId.isNotBlank() && navId != "unknown") }
    var failed by remember(navId, site) { mutableStateOf(false) }
    var retryTick by remember(navId, site) { mutableIntStateOf(0) }

    LaunchedEffect(navId, site, cached, retryTick) {
        if (cached != null && retryTick == 0) {
            torrent = cached
            loading = false
            return@LaunchedEffect
        }
        if (navId.isBlank() || navId == "unknown") {
            failed = true
            loading = false
            return@LaunchedEffect
        }
        loading = true
        failed = false
        AniyaaApplication.instance.nyaaRepository.fetchTorrent(navId, site = site)
            .onSuccess {
                torrent = it
                loading = false
                searchHistoryViewModel.recordViewed(it)
            }
            .onFailure {
                failed = true
                loading = false
            }
    }

    when {
        torrent != null -> {
            LaunchedEffect(torrent!!.bookmarkKey()) {
                searchHistoryViewModel.recordViewed(torrent!!)
            }
            TorrentDetailScreen(
                torrent = torrent!!,
                onNavigateBack = onNavigateBack,
                onOpenUser = onOpenUser,
                onOpenCatalogLink = onOpenCatalogLink,
                onFollow = { name, query -> onFollow(name, query, torrent!!.site) },
                onSearch = { query -> onSearch(query, torrent!!.site) },
                bookmarkViewModel = bookmarkViewModel
            )
        }
        loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        else -> MissingTorrentScreen(
            onNavigateBack = onNavigateBack,
            onRetry = { retryTick++ }
        )
    }
}

@Composable
private fun EmptyDetailPane() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Select a listing",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MissingTorrentScreen(onNavigateBack: () -> Unit, onRetry: () -> Unit = {}) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "This torrent is no longer available",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onRetry) { Text("Retry") }
            }
        }
    }
}
