package com.fedoratube.org.ui.screens.settings.home

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fedoratube.org.R
import com.fedoratube.org.data.local.PlayerPreferences
import com.fedoratube.org.ui.components.settings.SettingEntry
import com.fedoratube.org.ui.components.settings.SettingsDestination
import com.fedoratube.org.ui.components.settings.SettingsPage
import com.fedoratube.org.ui.components.settings.SettingsTarget
import com.fedoratube.org.ui.components.shared.FlowChoice
import com.fedoratube.org.ui.components.shared.FlowChoiceDialog
import com.fedoratube.org.ui.components.shared.FlowSearchField
import com.fedoratube.org.ui.screens.settings.index.DestinationIndex
import com.fedoratube.org.ui.screens.settings.index.HomeIndex
import com.fedoratube.org.ui.screens.settings.index.SettingsSearch
import kotlinx.coroutines.launch

private val SearchFieldBottomPadding = 8.dp

@Composable
internal fun SettingsHomeScreen(
    selected: SettingsDestination?,
    onOpen: (SettingsTarget) -> Unit,
    onOpenUpdate: () -> Unit,
    onBack: () -> Unit,
    viewModel: SettingsHomeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val preferences = remember(context) { PlayerPreferences(context) }
    val isFocusModeEnabled by preferences.focusModeEnabled.collectAsStateWithLifecycle(initialValue = false)
    val autoClearCacheHours by preferences.autoClearCacheHours.collectAsStateWithLifecycle(initialValue = 12)
    val coroutineScope = rememberCoroutineScope()

    val deepFlow by viewModel.deepFlow.collectAsStateWithLifecycle()
    val persona by viewModel.persona.collectAsStateWithLifecycle()
    val updateCheck by viewModel.updateCheck.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var query by rememberSaveable { mutableStateOf("") }
    var highlight by remember { mutableStateOf<String?>(null) }
    var showDurationDialog by rememberSaveable { mutableStateOf(false) }
    var showAutoClearDialog by rememberSaveable { mutableStateOf(false) }

    val checkingLabel = stringResource(R.string.checking_for_updates)
    val searchable = rememberSearchableSettings()
    LifecycleResumeEffect(selected) {
        viewModel.refreshPersona()
        onPauseOrDispose {}
    }
    val results = remember(query, searchable) { SettingsSearch.search(query, searchable) }

    UpdateCheckFeedback(
        state = updateCheck,
        snackbarHostState = snackbarHostState,
        onConsumed = viewModel::consumeUpdateCheck,
    )

    val onResultClick: (SettingEntry) -> Unit = { entry ->
        query = ""
        val page = DestinationIndex.destinationOf(entry)
        when {
            entry.key == HomeIndex.checkForUpdates.key -> {
                viewModel.checkForUpdates()
                onOpenUpdate()
            }
            page != null -> onOpen(SettingsTarget(page))
            entry.destination == SettingsDestination.HOME -> highlight = entry.key
            else -> onOpen(entry.target)
        }
    }

    SettingsPage(
        title = stringResource(R.string.settings_title),
        onBack = onBack,
        highlight = highlight,
        snackbarHostState = snackbarHostState,
        header = {
            FlowSearchField(
                query = query,
                onQueryChange = { query = it },
                placeholder = stringResource(R.string.ui_search_settings),
                onClear = { query = "" },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = SearchFieldBottomPadding),
            )
        },
    ) {
        if (query.isNotBlank()) {
            searchResults(query = query, results = results, onResultClick = onResultClick)
        } else {
            homeContent(
                state =
                    SettingsHomeState(
                        selected = selected,
                        persona = persona,
                        deepFlow = deepFlow,
                        updateCheckingLabel = checkingLabel.takeIf { updateCheck == UpdateCheckState.Checking },
                        isFocusModeEnabled = isFocusModeEnabled,
                        autoClearCacheHours = autoClearCacheHours,
                    ),
                actions =
                    SettingsHomeActions(
                        onOpen = onOpen,
                        onDeepFlowChange = viewModel::setDeepFlowEnabled,
                        onDurationClick = { showDurationDialog = true },
                        onSaveHistoryChange = viewModel::setDeepFlowSaveToHistory,
                        onScrobbleChange = viewModel::setDeepFlowScrobble,
                        onShowOnSearchChange = viewModel::setDeepFlowOnSearch,
                        onCheckForUpdates = {
                            viewModel.checkForUpdates()
                            onOpenUpdate()
                        },
                        onFocusModeChange = { enabled ->
                            coroutineScope.launch { preferences.setFocusModeEnabled(enabled) }
                        },
                        onAutoClearCacheClick = { showAutoClearDialog = true },
                    ),
            )
        }
    }

    if (showAutoClearDialog) {
        val options =
            listOf(
                FlowChoice(0, stringResource(R.string.auto_clear_cache_disabled)),
                FlowChoice(3, stringResource(R.string.auto_clear_cache_3h)),
                FlowChoice(6, stringResource(R.string.auto_clear_cache_6h)),
                FlowChoice(12, stringResource(R.string.auto_clear_cache_12h)),
                FlowChoice(24, stringResource(R.string.auto_clear_cache_24h)),
                FlowChoice(72, stringResource(R.string.auto_clear_cache_3d)),
                FlowChoice(168, stringResource(R.string.auto_clear_cache_7d)),
            )
        FlowChoiceDialog(
            title = stringResource(R.string.auto_clear_cache_title),
            description = stringResource(R.string.auto_clear_cache_subtitle),
            icon = Icons.Outlined.CleaningServices,
            options = options,
            selected = autoClearCacheHours,
            onSelect = { hours ->
                coroutineScope.launch { preferences.setAutoClearCacheHours(hours) }
                showAutoClearDialog = false
            },
            onDismiss = { showAutoClearDialog = false },
        )
    }

    if (showDurationDialog) {
        FlowChoiceDialog(
            title = stringResource(R.string.deep_flow_dialog_title),
            description = stringResource(R.string.deep_flow_dialog_body),
            icon = Icons.Outlined.Timer,
            options = DeepFlowDurations.map { FlowChoice(it, deepFlowDurationLabel(it)) },
            selected = deepFlow.expireHours,
            onSelect = viewModel::setDeepFlowExpireHours,
            onDismiss = { showDurationDialog = false },
        )
    }

    LaunchedEffect(updateCheck) {
        if (updateCheck is UpdateCheckState.Available) {
            viewModel.consumeUpdateCheck()
            onOpenUpdate()
        }
    }
}

@Composable
private fun UpdateCheckFeedback(
    state: UpdateCheckState,
    snackbarHostState: SnackbarHostState,
    onConsumed: () -> Unit,
) {
    val upToDate = stringResource(R.string.flow_is_up_to_date)
    val failed = stringResource(R.string.update_check_failed)
    LaunchedEffect(state) {
        val message =
            when (state) {
                UpdateCheckState.UpToDate -> upToDate
                UpdateCheckState.Failed -> failed
                else -> return@LaunchedEffect
            }
        // Consuming first would change this effect's key and cancel it before the snackbar shows.
        try {
            snackbarHostState.showSnackbar(message)
        } finally {
            onConsumed()
        }
    }
}
