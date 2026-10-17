package com.nyaa.aniyaa.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nyaa.aniyaa.BuildConfig
import com.nyaa.aniyaa.data.model.CatalogSite
import com.nyaa.aniyaa.data.model.DarkMode
import com.nyaa.aniyaa.data.model.SortField
import com.nyaa.aniyaa.data.model.SortOrder
import com.nyaa.aniyaa.data.model.categoryByValue
import com.nyaa.aniyaa.data.model.sortFieldByValue
import com.nyaa.aniyaa.data.model.sortOrderByValue
import com.nyaa.aniyaa.util.CodecChoice
import com.nyaa.aniyaa.util.LocalFilter
import com.nyaa.aniyaa.util.ResolutionChoice
import com.nyaa.aniyaa.util.SubtitleChoice
import com.nyaa.aniyaa.ui.theme.APP_THEMES
import com.nyaa.aniyaa.ui.theme.AppTheme
import com.nyaa.aniyaa.ui.theme.rememberThemeSwatch
import com.nyaa.aniyaa.ui.viewmodel.SettingsViewModel
import com.nyaa.aniyaa.util.listTorrentApps
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    currentThemeIndex: Int,
    darkMode: DarkMode,
    onThemeSelected: (Int) -> Unit,
    onDarkModeSelected: (DarkMode) -> Unit,
    onPrivacyFlagsChanged: () -> Unit,
    settingsViewModel: SettingsViewModel = viewModel(),
    bottomPadding: Dp = 0.dp
) {
    val context = LocalContext.current
    val prefs = settingsViewModel.prefs
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val message by settingsViewModel.message.collectAsStateWithLifecycle()
    val update by settingsViewModel.update.collectAsStateWithLifecycle()
    val checkingUpdate by settingsViewModel.checkingUpdate.collectAsStateWithLifecycle()
    val installingUpdate by settingsViewModel.installingUpdate.collectAsStateWithLifecycle()

    var currentSite by remember { mutableStateOf(prefs.currentSite) }
    var nyaaBaseUrl by remember { mutableStateOf(prefs.baseUrl(CatalogSite.NYAA)) }
    var sukebeiBaseUrl by remember { mutableStateOf(prefs.baseUrl(CatalogSite.SUKEBEI)) }
    var showSukebeiWarning by remember { mutableStateOf(false) }
    var sukebeiEnabled by remember { mutableStateOf(prefs.sukebeiEnabled) }
    var lockEnabled by remember { mutableStateOf(prefs.lockEnabled) }
    var hideScreenshots by remember { mutableStateOf(prefs.hideScreenshots) }
    var hideFromRecents by remember { mutableStateOf(prefs.hideFromRecents) }
    var defaultCategory by remember { mutableStateOf(prefs.defaultCategoryValue(currentSite)) }
    var defaultSort by remember { mutableStateOf(prefs.defaultSortFieldValue(currentSite)) }
    var defaultOrder by remember { mutableStateOf(prefs.defaultSortOrderValue(currentSite)) }
    var defaultResolution by remember { mutableStateOf(prefs.defaultResolutionTag) }
    var defaultCodec by remember { mutableStateOf(prefs.defaultCodecTag) }
    var defaultSubtitle by remember { mutableStateOf(prefs.defaultSubtitleTag) }
    var quietEnabled by remember { mutableStateOf(prefs.quietHoursEnabled) }
    var quietStart by remember { mutableStateOf(prefs.quietStartHour) }
    var quietEnd by remember { mutableStateOf(prefs.quietEndHour) }
    var remoteKind by remember { mutableStateOf(prefs.remoteKind) }
    var remoteUrl by remember { mutableStateOf(prefs.remoteBaseUrl) }
    var remoteUser by remember { mutableStateOf(prefs.remoteUsername) }
    var remotePassword by remember { mutableStateOf(prefs.remotePassword) }
    var defaultMinSeeders by remember { mutableStateOf(prefs.defaultMinSeeders) }
    var defaultHideBatches by remember { mutableStateOf(prefs.defaultHideBatches) }
    var hideRules by remember { mutableStateOf(prefs.hideRules()) }
    var hideDraft by remember { mutableStateOf("") }
    var torrentPackage by remember { mutableStateOf(prefs.preferredTorrentPackage) }
    var showPinDialog by remember { mutableStateOf(false) }
    var pinValue by remember { mutableStateOf("") }
    var showResetDialog by remember { mutableStateOf(false) }
    var loadLatestOnStart by remember { mutableStateOf(prefs.loadLatestOnStart) }
    var alertHours by remember { mutableStateOf(prefs.savedSearchIntervalHours) }
    var lockGraceMs by remember { mutableStateOf(prefs.lockGraceMs) }
    var biometricUnlock by remember { mutableStateOf(prefs.biometricUnlockEnabled) }
    var pendingImportJson by remember { mutableStateOf<String?>(null) }
    var backupPassword by remember { mutableStateOf("") }
    var importPassword by remember { mutableStateOf("") }
    var pinConfirm by remember { mutableStateOf("") }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val json = settingsViewModel.exportBackup(backupPassword.takeIf { it.isNotBlank() })
                    context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                    snackbarHostState.showSnackbar("Backup exported")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar(e.message ?: "Export failed")
                }
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                val json = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }.orEmpty()
                pendingImportJson = json
            } catch (e: Exception) {
                scope.launch { snackbarHostState.showSnackbar(e.message ?: "Import failed") }
            }
        }
    }

    LaunchedEffect(message) {
        val text = message
        if (text != null) {
            snackbarHostState.showSnackbar(text)
            settingsViewModel.consumeMessage()
        }
    }

    LaunchedEffect(Unit) {
        val last = prefs.lastUpdateCheckAt
        if (System.currentTimeMillis() - last > 86_400_000L) {
            settingsViewModel.checkForUpdates(manual = false)
        }
    }

    fun openUrl(url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) {
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .padding(bottom = bottomPadding)
        ) {
            SectionLabel("Appearance")
            Text("Color theme", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(8.dp))
            val previewDark = when (darkMode) {
                DarkMode.SYSTEM -> isSystemInDarkTheme()
                DarkMode.LIGHT -> false
                DarkMode.DARK -> true
            }
            val materialYou = APP_THEMES.first()
            ThemeCard(
                theme = materialYou,
                isSelected = currentThemeIndex == 0,
                previewDark = previewDark,
                featured = true,
                onClick = {
                    onThemeSelected(0)
                    settingsViewModel.setThemeIndex(0)
                }
            )
            Spacer(Modifier.height(16.dp))
            Text("Palettes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(8.dp))
            APP_THEMES.drop(1).chunked(2).forEachIndexed { rowIndex, rowThemes ->
                if (rowIndex > 0) Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowThemes.forEachIndexed { columnIndex, theme ->
                        val index = rowIndex * 2 + columnIndex + 1
                        ThemeCard(
                            theme = theme,
                            isSelected = currentThemeIndex == index,
                            previewDark = previewDark,
                            onClick = {
                                onThemeSelected(index)
                                settingsViewModel.setThemeIndex(index)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowThemes.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Dark mode", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DarkMode.entries.forEach { mode ->
                    FilterChip(
                        selected = darkMode == mode,
                        onClick = {
                            onDarkModeSelected(mode)
                            settingsViewModel.setDarkMode(mode)
                        },
                        label = { Text(mode.displayName) }
                    )
                }
            }
            var compactCards by remember { mutableStateOf(prefs.compactCards) }
            SettingsSwitchRow(
                "Compact result cards",
                "Show denser search results with fewer action buttons",
                compactCards
            ) { enabled ->
                compactCards = enabled
                settingsViewModel.setCompactCards(enabled)
            }

            Spacer(Modifier.height(28.dp))
            SectionLabel("Catalog")
            Text("Nyaa is the default. Sukebei is optional adult content.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            SettingsSwitchRow("Enable Sukebei (18+)", "Show the Sukebei catalog on Search", sukebeiEnabled) { enabled ->
                if (enabled && !prefs.sukebeiAcknowledged) {
                    showSukebeiWarning = true
                } else {
                    sukebeiEnabled = enabled
                    settingsViewModel.setSukebeiEnabled(enabled)
                    if (!enabled) {
                        currentSite = CatalogSite.NYAA
                        defaultCategory = prefs.defaultCategoryValue(CatalogSite.NYAA)
                        defaultSort = prefs.defaultSortFieldValue(CatalogSite.NYAA)
                        defaultOrder = prefs.defaultSortOrderValue(CatalogSite.NYAA)
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            SectionLabel("Search defaults")
            SettingsSwitchRow(
                "Load latest on start",
                "Show the newest listings when you open Search",
                loadLatestOnStart
            ) { enabled ->
                loadLatestOnStart = enabled
                settingsViewModel.setLoadLatestOnStart(enabled)
            }
            Spacer(Modifier.height(8.dp))
            Text("Alert interval for saved searches", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                com.nyaa.aniyaa.data.prefs.AppPreferences.ALERT_INTERVAL_HOURS.forEach { hours ->
                    FilterChip(
                        selected = alertHours == hours,
                        onClick = {
                            alertHours = hours
                            settingsViewModel.setSavedSearchIntervalHours(hours)
                        },
                        label = { Text("${hours}h") }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Opening Aniyaa uses this row. Alerts use it too.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.height(8.dp))
            DefaultFilterRow(
                site = currentSite,
                sukebeiEnabled = sukebeiEnabled,
                categoryValue = defaultCategory,
                sortFieldValue = defaultSort,
                sortOrderValue = defaultOrder,
                resolutionTag = defaultResolution,
                codecTag = defaultCodec,
                subtitleTag = defaultSubtitle,
                minSeeders = defaultMinSeeders,
                hideBatches = defaultHideBatches,
                onSite = { site ->
                    settingsViewModel.setCurrentSite(site)
                    currentSite = site
                    defaultCategory = prefs.defaultCategoryValue(site)
                    defaultSort = prefs.defaultSortFieldValue(site)
                    defaultOrder = prefs.defaultSortOrderValue(site)
                },
                onCategory = { value ->
                    defaultCategory = value
                    settingsViewModel.setDefaultCategory(value, currentSite)
                },
                onSort = { field, order ->
                    defaultSort = field
                    defaultOrder = order
                    settingsViewModel.setDefaultSortField(field, currentSite)
                    settingsViewModel.setDefaultSortOrder(order, currentSite)
                },
                onResolution = { tag ->
                    defaultResolution = tag
                    settingsViewModel.setDefaultResolution(tag)
                },
                onCodec = { tag ->
                    defaultCodec = tag
                    settingsViewModel.setDefaultCodec(tag)
                },
                onSubtitle = { tag ->
                    defaultSubtitle = tag
                    prefs.defaultSubtitleTag = tag
                },
                onMinSeeders = { value ->
                    defaultMinSeeders = value
                    settingsViewModel.setDefaultMinSeeders(value)
                },
                onHideBatches = { enabled ->
                    defaultHideBatches = enabled
                    settingsViewModel.setDefaultHideBatches(enabled)
                }
            )

            Spacer(Modifier.height(12.dp))
            SettingsSwitchRow(
                "Quiet hours",
                "Skip saved-search checks overnight. New results wait until the window ends.",
                quietEnabled
            ) { enabled ->
                quietEnabled = enabled
                prefs.quietHoursEnabled = enabled
            }
            if (quietEnabled) {
                Text("From $quietStart:00 to $quietEnd:00", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0, 6, 7, 22, 23).forEach { hour ->
                        FilterChip(
                            selected = quietStart == hour,
                            onClick = {
                                quietStart = hour
                                prefs.quietStartHour = hour
                            },
                            label = { Text("Start $hour") }
                        )
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(6, 7, 8, 9).forEach { hour ->
                        FilterChip(
                            selected = quietEnd == hour,
                            onClick = {
                                quietEnd = hour
                                prefs.quietEndHour = hour
                            },
                            label = { Text("End $hour") }
                        )
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
            SectionLabel("Remote client")
            Text(
                "Optional. Magnets go to qBittorrent or Transmission on your network. Use https when you can. The password stays on this phone.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                com.nyaa.aniyaa.data.prefs.RemoteKind.entries.forEach { kind ->
                    FilterChip(
                        selected = remoteKind == kind,
                        onClick = {
                            remoteKind = kind
                            prefs.remoteKind = kind
                        },
                        label = { Text(kind.label) }
                    )
                }
            }
            if (remoteKind != com.nyaa.aniyaa.data.prefs.RemoteKind.NONE) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = remoteUrl,
                    onValueChange = { remoteUrl = it },
                    label = { Text("Address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = remoteUser,
                    onValueChange = { remoteUser = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = remotePassword,
                    onValueChange = { remotePassword = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    prefs.remoteBaseUrl = remoteUrl
                    prefs.remoteUsername = remoteUser
                    prefs.remotePassword = remotePassword
                }) { Text("Save client") }
            }
            Spacer(Modifier.height(28.dp))
            SectionLabel("Hide rules")
            Text(
                "Listings whose title, uploader, or release group contains one of these are hidden in search and alerts.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = hideDraft,
                onValueChange = { hideDraft = it },
                label = { Text("Hide if it contains") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    val draft = hideDraft.trim()
                    if (draft.length < 2) return@KeyboardActions
                    settingsViewModel.addHideRule(draft)
                    hideRules = prefs.hideRules()
                    if (hideRules.any { it.equals(draft, ignoreCase = true) }) hideDraft = ""
                }),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            val draft = hideDraft.trim()
                            settingsViewModel.addHideRule(draft)
                            hideRules = prefs.hideRules()
                            if (hideRules.any { it.equals(draft, ignoreCase = true) }) hideDraft = ""
                        },
                        enabled = hideDraft.trim().length >= 2
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add hide rule")
                    }
                }
            )
            if (hideRules.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    hideRules.forEach { rule ->
                        InputChip(
                            selected = true,
                            onClick = {
                                settingsViewModel.removeHideRule(rule)
                                hideRules = prefs.hideRules()
                            },
                            label = { Text(rule) },
                            trailingIcon = {
                                Icon(Icons.Default.Close, contentDescription = "Remove $rule", modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            SectionLabel("Mirrors")
            Text("If a site is blocked, paste a working mirror for that catalog.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                com.nyaa.aniyaa.data.network.CatalogMirrors.forSite(CatalogSite.NYAA).forEach { url ->
                    FilterChip(
                        selected = nyaaBaseUrl == url,
                        onClick = {
                            nyaaBaseUrl = url
                            settingsViewModel.setBaseUrl(CatalogSite.NYAA, url)
                        },
                        label = { Text(com.nyaa.aniyaa.data.network.CatalogMirrors.label(url, CatalogSite.NYAA)) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = nyaaBaseUrl,
                onValueChange = { nyaaBaseUrl = it },
                label = { Text("Nyaa URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { settingsViewModel.setBaseUrl(CatalogSite.NYAA, nyaaBaseUrl) }) { Text("Save") }
                OutlinedButton(onClick = { settingsViewModel.testMirror(CatalogSite.NYAA, nyaaBaseUrl) }) { Text("Test") }
                OutlinedButton(onClick = {
                    nyaaBaseUrl = CatalogSite.NYAA.defaultBase
                    settingsViewModel.setBaseUrl(CatalogSite.NYAA, CatalogSite.NYAA.defaultBase)
                }) { Text("Official") }
            }
            if (sukebeiEnabled) {
                Spacer(Modifier.height(16.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    com.nyaa.aniyaa.data.network.CatalogMirrors.forSite(CatalogSite.SUKEBEI).forEach { url ->
                        FilterChip(
                            selected = sukebeiBaseUrl == url,
                            onClick = {
                                sukebeiBaseUrl = url
                                settingsViewModel.setBaseUrl(CatalogSite.SUKEBEI, url)
                            },
                            label = { Text(com.nyaa.aniyaa.data.network.CatalogMirrors.label(url, CatalogSite.SUKEBEI)) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = sukebeiBaseUrl,
                    onValueChange = { sukebeiBaseUrl = it },
                    label = { Text("Sukebei URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { settingsViewModel.setBaseUrl(CatalogSite.SUKEBEI, sukebeiBaseUrl) }) { Text("Save") }
                    OutlinedButton(onClick = { settingsViewModel.testMirror(CatalogSite.SUKEBEI, sukebeiBaseUrl) }) { Text("Test") }
                    OutlinedButton(onClick = {
                        sukebeiBaseUrl = CatalogSite.SUKEBEI.defaultBase
                        settingsViewModel.setBaseUrl(CatalogSite.SUKEBEI, CatalogSite.SUKEBEI.defaultBase)
                    }) { Text("Official") }
                }
            }

            Spacer(Modifier.height(28.dp))
            SectionLabel("Privacy")
            SettingsSwitchRow("App lock", "Require a PIN when you return to the app", lockEnabled) { enabled ->
                if (enabled && !prefs.hasPin) {
                    showPinDialog = true
                } else {
                    lockEnabled = enabled
                    settingsViewModel.setLockEnabled(enabled)
                    onPrivacyFlagsChanged()
                }
            }
            TextButton(onClick = { showPinDialog = true }) { Text(if (prefs.hasPin) "Change PIN" else "Set PIN") }
            if (prefs.hasPin) {
                TextButton(onClick = {
                    settingsViewModel.clearPin()
                    lockEnabled = false
                    onPrivacyFlagsChanged()
                }) { Text("Remove PIN") }
            }
            SettingsSwitchRow(
                "Unlock with biometrics",
                "Use fingerprint or face unlock when the device supports strong biometrics",
                biometricUnlock
            ) { enabled ->
                biometricUnlock = enabled
                settingsViewModel.setBiometricUnlockEnabled(enabled)
            }
            Text("Lock delay", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                com.nyaa.aniyaa.data.prefs.AppPreferences.LOCK_GRACE_OPTIONS.forEach { ms ->
                    FilterChip(
                        selected = lockGraceMs == ms,
                        onClick = {
                            lockGraceMs = ms
                            settingsViewModel.setLockGraceMs(ms)
                        },
                        label = { Text(com.nyaa.aniyaa.data.prefs.AppPreferences.lockGraceLabel(ms)) }
                    )
                }
            }
            SettingsSwitchRow("Hide screenshots", "Block screenshots and recents previews", hideScreenshots) { enabled ->
                hideScreenshots = enabled
                settingsViewModel.setHideScreenshots(enabled)
                onPrivacyFlagsChanged()
            }
            SettingsSwitchRow("Hide from recents", "Remove Aniyaa from the recents list", hideFromRecents) { enabled ->
                hideFromRecents = enabled
                settingsViewModel.setHideFromRecents(enabled)
                onPrivacyFlagsChanged()
            }

            Spacer(Modifier.height(28.dp))
            SectionLabel("Torrent app")
            val apps = remember { listTorrentApps(context) }
            FilterChip(
                selected = torrentPackage.isBlank(),
                onClick = {
                    torrentPackage = ""
                    settingsViewModel.setPreferredTorrentPackage("")
                },
                label = { Text("Always ask") }
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                apps.forEach { app ->
                    FilterChip(
                        selected = torrentPackage == app.packageName,
                        onClick = {
                            torrentPackage = app.packageName
                            settingsViewModel.setPreferredTorrentPackage(app.packageName)
                        },
                        label = { Text(app.label) }
                    )
                }
            }
            if (apps.isEmpty()) {
                Text("No magnet-handling apps found.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }

            Spacer(Modifier.height(28.dp))
            SectionLabel("Backup")
            Text("Export bookmarks, history, saved searches, and settings. Optional password encrypts the file.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = backupPassword,
                onValueChange = { backupPassword = it },
                label = { Text("Export password (optional)") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { exportLauncher.launch("aniyaa-backup.json") }) { Text("Export") }
                OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }) { Text("Import") }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { settingsViewModel.clearNetworkCache() }) { Text("Clear cache") }
                OutlinedButton(onClick = { showResetDialog = true }) { Text("Reset local data") }
            }
            if (showResetDialog) {
                AlertDialog(
                    onDismissRequest = { showResetDialog = false },
                    title = { Text("Reset this device?") },
                    text = { Text("This removes bookmarks, search history, and saved searches on this phone. A backup is not created.") },
                    confirmButton = {
                        TextButton(onClick = {
                            settingsViewModel.resetLocalData()
                            showResetDialog = false
                        }) { Text("Reset") }
                    },
                    dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text("Cancel") } }
                )
            }
            if (pendingImportJson != null) {
                AlertDialog(
                    onDismissRequest = { pendingImportJson = null },
                    title = { Text("Restore backup") },
                    text = {
                        Column {
                            Text("Merge keeps existing bookmarks and searches. Replace overwrites everything in the backup.")
                            if (com.nyaa.aniyaa.data.backup.BackupCrypto.isEncrypted(pendingImportJson.orEmpty())) {
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = importPassword,
                                    onValueChange = { importPassword = it },
                                    label = { Text("Backup password") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    singleLine = true
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val json = pendingImportJson
                            pendingImportJson = null
                            if (json != null) {
                                settingsViewModel.importBackup(
                                    json,
                                    merge = true,
                                    password = importPassword.takeIf { it.isNotBlank() }
                                )
                            }
                            importPassword = ""
                        }) { Text("Merge") }
                    },
                    dismissButton = {
                        Row {
                            TextButton(onClick = {
                                val json = pendingImportJson
                                pendingImportJson = null
                                if (json != null) {
                                    settingsViewModel.importBackup(
                                        json,
                                        merge = false,
                                        password = importPassword.takeIf { it.isNotBlank() }
                                    )
                                }
                                importPassword = ""
                            }) { Text("Replace") }
                            TextButton(onClick = {
                                pendingImportJson = null
                                importPassword = ""
                            }) { Text("Cancel") }
                        }
                    }
                )
            }

            Spacer(Modifier.height(28.dp))
            SectionLabel("Updates")
            if (update != null) {
                Text("Version ${update?.versionName} is available.", color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (update?.apkUrl.isNullOrBlank()) {
                                openUrl(update?.htmlUrl.orEmpty())
                            } else {
                                settingsViewModel.downloadAndInstallUpdate(context)
                            }
                        },
                        enabled = !installingUpdate
                    ) { Text(if (installingUpdate) "Downloading…" else "Install") }
                    OutlinedButton(onClick = { openUrl(update?.htmlUrl.orEmpty()) }) { Text("GitHub") }
                    TextButton(onClick = { settingsViewModel.dismissUpdate() }) { Text("Later") }
                }
            } else {
                Button(onClick = { settingsViewModel.checkForUpdates(true) }, enabled = !checkingUpdate) {
                    Text(if (checkingUpdate) "Checking…" else "Check for updates")
                }
            }

            Spacer(Modifier.height(32.dp))
            SectionLabel("About")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Aniyaa searches nyaa.si and sukebei.nyaa.si from your phone. It finds listings — a torrent app you already have does the downloading.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "This is not an official nyaa.si or sukebei.nyaa.si app. Bookmarks and search history stay on this device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(Modifier.height(12.dp))
                    InfoSettingsRow(label = "Version", value = BuildConfig.VERSION_NAME)
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(Modifier.height(10.dp))
                    InfoSettingsRow(
                        label = "Catalog",
                        value = currentSite.displayName,
                        onClick = { openUrl(prefs.baseUrl(currentSite)) }
                    )
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(Modifier.height(10.dp))
                    InfoSettingsRow(
                        label = "Updates",
                        value = "GitHub Releases",
                        onClick = { openUrl("https://github.com/Gourab0002/Aniyaa/releases/latest") }
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showSukebeiWarning) {
        AlertDialog(
            onDismissRequest = { showSukebeiWarning = false },
            title = { Text("Sukebei is 18+") },
            text = { Text("Sukebei lists adult content. You must be 18 or older to continue.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        settingsViewModel.acknowledgeSukebei()
                        settingsViewModel.setCurrentSite(CatalogSite.SUKEBEI)
                        sukebeiEnabled = true
                        currentSite = CatalogSite.SUKEBEI
                        defaultCategory = prefs.defaultCategoryValue(CatalogSite.SUKEBEI)
                        defaultSort = prefs.defaultSortFieldValue(CatalogSite.SUKEBEI)
                        defaultOrder = prefs.defaultSortOrderValue(CatalogSite.SUKEBEI)
                        showSukebeiWarning = false
                    }
                ) { Text("I am 18+") }
            },
            dismissButton = { TextButton(onClick = { showSukebeiWarning = false }) { Text("Cancel") } }
        )
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Set PIN") },
            text = {
                Column {
                    OutlinedTextField(
                        value = pinValue,
                        onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) pinValue = it },
                        label = { Text("4–8 digit PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pinConfirm,
                        onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) pinConfirm = it },
                        label = { Text("Confirm PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = pinValue.length in 4..8 && pinValue == pinConfirm,
                    onClick = {
                        settingsViewModel.setPin(pinValue)
                        lockEnabled = true
                        settingsViewModel.setLockEnabled(true)
                        onPrivacyFlagsChanged()
                        pinValue = ""
                        pinConfirm = ""
                        showPinDialog = false
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPinDialog = false
                    pinValue = ""
                    pinConfirm = ""
                }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Icon(
            if (text == "About") Icons.Default.Info else Icons.Default.Palette,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(text = text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

private enum class DefaultFilterMenu { Catalog, Category, Quality, Codec, Subtitles, Seeds, Sort }

@Composable
private fun DefaultFilterRow(
    site: CatalogSite,
    sukebeiEnabled: Boolean,
    categoryValue: String,
    sortFieldValue: String,
    sortOrderValue: String,
    resolutionTag: String,
    codecTag: String,
    subtitleTag: String,
    minSeeders: Int,
    hideBatches: Boolean,
    onSite: (CatalogSite) -> Unit,
    onCategory: (String) -> Unit,
    onSort: (String, String) -> Unit,
    onResolution: (String) -> Unit,
    onCodec: (String) -> Unit,
    onSubtitle: (String) -> Unit,
    onMinSeeders: (Int) -> Unit,
    onHideBatches: (Boolean) -> Unit
) {
    val category = categoryByValue(categoryValue, site)
    val sortField = sortFieldByValue(sortFieldValue)
    val sortOrder = sortOrderByValue(sortOrderValue)
    val resolution = ResolutionChoice.fromTag(resolutionTag)
    val codec = CodecChoice.fromTag(codecTag)
    val subtitle = SubtitleChoice.fromTag(subtitleTag)
    var openMenu by remember { mutableStateOf<DefaultFilterMenu?>(null) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (sukebeiEnabled) {
            MenuChip(
                label = if (site.nsfw) "Sukebei" else "Nyaa",
                selected = site.nsfw,
                expanded = openMenu == DefaultFilterMenu.Catalog,
                onExpand = { openMenu = if (openMenu == DefaultFilterMenu.Catalog) null else DefaultFilterMenu.Catalog },
                onDismiss = { openMenu = null }
            ) {
                CatalogSite.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(if (option.nsfw) "Sukebei 18+" else option.displayName) },
                        onClick = {
                            openMenu = null
                            onSite(option)
                        }
                    )
                }
            }
        }
        MenuChip(
            label = category.compactLabel(),
            selected = category.value != "0_0",
            expanded = openMenu == DefaultFilterMenu.Category,
            onExpand = { openMenu = if (openMenu == DefaultFilterMenu.Category) null else DefaultFilterMenu.Category },
            onDismiss = { openMenu = null }
        ) {
            site.categories.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    onClick = {
                        openMenu = null
                        onCategory(option.value)
                    }
                )
            }
        }
        MenuChip(
            label = if (resolution == ResolutionChoice.ANY) "Quality" else resolution.label,
            selected = resolution != ResolutionChoice.ANY,
            expanded = openMenu == DefaultFilterMenu.Quality,
            onExpand = { openMenu = if (openMenu == DefaultFilterMenu.Quality) null else DefaultFilterMenu.Quality },
            onDismiss = { openMenu = null }
        ) {
            ResolutionChoice.entries.forEach { choice ->
                DropdownMenuItem(
                    text = { Text(choice.label) },
                    onClick = {
                        openMenu = null
                        onResolution(choice.tag)
                    }
                )
            }
        }
        MenuChip(
            label = if (codec == CodecChoice.ANY) "Codec" else codec.label,
            selected = codec != CodecChoice.ANY,
            expanded = openMenu == DefaultFilterMenu.Codec,
            onExpand = { openMenu = if (openMenu == DefaultFilterMenu.Codec) null else DefaultFilterMenu.Codec },
            onDismiss = { openMenu = null }
        ) {
            CodecChoice.entries.forEach { choice ->
                DropdownMenuItem(
                    text = { Text(choice.label) },
                    onClick = {
                        openMenu = null
                        onCodec(choice.tag)
                    }
                )
            }
        }
        MenuChip(
            label = if (subtitle == SubtitleChoice.ANY) "Subs" else subtitle.label,
            selected = subtitle != SubtitleChoice.ANY,
            expanded = openMenu == DefaultFilterMenu.Subtitles,
            onExpand = { openMenu = if (openMenu == DefaultFilterMenu.Subtitles) null else DefaultFilterMenu.Subtitles },
            onDismiss = { openMenu = null }
        ) {
            SubtitleChoice.entries.forEach { choice ->
                DropdownMenuItem(
                    text = { Text(choice.label) },
                    onClick = {
                        openMenu = null
                        onSubtitle(choice.tag)
                    }
                )
            }
        }
        MenuChip(
            label = if (minSeeders <= 0) "Seeds" else "$minSeeders+",
            selected = minSeeders > 0,
            expanded = openMenu == DefaultFilterMenu.Seeds,
            onExpand = { openMenu = if (openMenu == DefaultFilterMenu.Seeds) null else DefaultFilterMenu.Seeds },
            onDismiss = { openMenu = null }
        ) {
            LocalFilter.SEED_CHOICES.forEach { value ->
                DropdownMenuItem(
                    text = { Text(if (value <= 0) "Any" else "$value+") },
                    onClick = {
                        openMenu = null
                        onMinSeeders(value)
                    }
                )
            }
        }
        TinyChip(
            text = "No batch",
            selected = hideBatches,
            onClick = { onHideBatches(!hideBatches) }
        )
        MenuChip(
            label = sortChipLabel(sortField, sortOrder),
            selected = sortField != SortField.DATE || sortOrder != SortOrder.DESC,
            expanded = openMenu == DefaultFilterMenu.Sort,
            onExpand = { openMenu = if (openMenu == DefaultFilterMenu.Sort) null else DefaultFilterMenu.Sort },
            onDismiss = { openMenu = null }
        ) {
            SortField.entries.forEach { field ->
                val mark = if (field == sortField) {
                    if (sortOrder == SortOrder.DESC) " \u2193" else " \u2191"
                } else {
                    ""
                }
                DropdownMenuItem(
                    text = { Text(field.displayName + mark) },
                    onClick = {
                        openMenu = null
                        if (field == sortField) {
                            val flipped = if (sortOrder == SortOrder.DESC) SortOrder.ASC else SortOrder.DESC
                            onSort(field.value, flipped.value)
                        } else {
                            onSort(field.value, sortOrder.value)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ThemeCard(
    theme: AppTheme,
    isSelected: Boolean,
    previewDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    featured: Boolean = false
) {
    val swatch = rememberThemeSwatch(theme, previewDark)
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "themeCardColor"
    )
    val caption = when {
        !theme.usesDynamicColor -> null
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> "Colors follow your wallpaper"
        else -> "Uses Aniyaa Purple until Android 12"
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.large)
                else Modifier
            )
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = MaterialTheme.shapes.large
    ) {
        if (featured) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(swatch.primary, swatch.secondary, swatch.tertiary, swatch.primaryContainer)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(swatch.primary))
                        Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(swatch.secondary))
                        Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(swatch.tertiary))
                    }
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = swatch.onPrimary,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 16.dp)
                                .size(22.dp)
                        )
                    }
                }
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Wallpaper,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = theme.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                        if (caption != null) {
                            Text(
                                text = caption,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(28.dp).clip(CircleShape).background(swatch.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = swatch.onPrimary, modifier = Modifier.size(14.dp))
                        }
                    }
                    Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(swatch.secondary))
                    Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(swatch.tertiary))
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = theme.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun InfoSettingsRow(label: String, value: String, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (onClick != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}
