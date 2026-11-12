package com.fedoratube.org.ui.screens.onboarding

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fedoratube.org.data.backup.BackupOperation
import com.fedoratube.org.data.backup.ImportKind
import com.fedoratube.org.ui.components.shared.FedoraThumbnailsWallpaper
import com.fedoratube.org.ui.components.shared.FedoraWallpaperRowPitch
import com.fedoratube.org.ui.components.shared.FlowMaxContentWidth
import com.fedoratube.org.ui.utils.LocalWindowSizeClass
import com.fedoratube.org.ui.utils.isExpandedWidth
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val importOperation by viewModel.importOperation.collectAsStateWithLifecycle()
    val newVideoAlerts by viewModel.newVideoAlerts.collectAsStateWithLifecycle()
    val isFocusMode by viewModel.isFocusMode.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var interestsRevealed by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.completed) { if (state.completed) onComplete() }
    BackHandler(enabled = state.step.index > 0) { viewModel.back() }

    LaunchedEffect(importOperation) {
        val message =
            when (val operation = importOperation) {
                is BackupOperation.Succeeded -> operation.message
                is BackupOperation.Failed -> operation.message
                else -> return@LaunchedEffect
            }
        viewModel.dismissImport()
        // Dismissing changes this effect's key, which would cancel a snackbar shown from here.
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    var pendingImport by rememberSaveable { mutableStateOf<ImportKind?>(null) }
    val importPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            val kind = pendingImport
            pendingImport = null
            if (uri != null && kind != null) viewModel.startImport(kind, listOf(uri))
        }
    val multiImportPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            val kind = pendingImport
            pendingImport = null
            if (uris.isNotEmpty() && kind != null) viewModel.startImport(kind, uris)
        }

    fun pick(kind: ImportKind) {
        pendingImport = kind
        if (kind.takesSeveralFiles) multiImportPicker.launch(kind.mimeTypes) else importPicker.launch(kind.mimeTypes)
    }

    // The step the backdrop and hero last settled on; a new step morphs out of it.
    var shownStep by remember { mutableStateOf(state.step) }
    var backdropFrom by remember { mutableStateOf(state.step) }
    val backdrop = remember { Animatable(1f) }
    val backdropSpec = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
    val heroFrom = shownStep
    LaunchedEffect(state.step) {
        if (state.step == shownStep) return@LaunchedEffect
        backdropFrom = shownStep
        shownStep = state.step
        backdrop.snapTo(0f)
        backdrop.animateTo(1f, backdropSpec)
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // The step's own wallpaper starts below this screen's top-bar band, so this strip
        // continues the same grid upward — offset by one row pitch, so the rows land exactly
        // where the step's wall takes over below. It stays composed through the first steps
        // (the backdrop behind the scaffold covers it), and the scrim matches the welcome
        // gradient's top stop so the seam reads as one wall.
        val bandVisible = state.step == OnboardingStep.WELCOME || shownStep == OnboardingStep.WELCOME
        AnimatedVisibility(
            visible = bandVisible,
            enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec<Float>()),
            exit = fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec<Float>()),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().height(TopBarHeight).clipToBounds(),
                contentAlignment = Alignment.TopCenter,
            ) {
                // Two pitches of wall, pushed up two rows: the band shows the bottom of one row
                // plus its gap, and the next row lands exactly at the band's edge, where the
                // step's own wallpaper (also starting at the top-bar height) takes over.
                Box(
                    modifier =
                        Modifier
                            .widthIn(max = FlowMaxContentWidth)
                            .fillMaxWidth()
                            .requiredHeight(TopBarHeight + FedoraWallpaperRowPitch * 2)
                            .offset(y = TopBarHeight - FedoraWallpaperRowPitch * 2),
                ) {
                    FedoraThumbnailsWallpaper(Modifier.fillMaxSize())
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.20f)),
                    )
                }
            }
        }
        if (shownStep != OnboardingStep.WELCOME) {
            Spacer(
                Modifier
                    .fillMaxSize()
                    .onboardingBackdrop(backdropFrom, shownStep, MaterialTheme.colorScheme.surfaceContainer) { backdrop.value },
            )
        }
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0.dp),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = { OnboardingTopBar(step = state.step, onSkip = viewModel::next) },
            bottomBar = {
                val besideList = LocalWindowSizeClass.current.isExpandedWidth && state.step.showsProgress
                Box(Modifier.fillMaxWidth(), contentAlignment = if (besideList) Alignment.CenterEnd else Alignment.Center) {
                    OnboardingBottomBar(
                        modifier = Modifier.widthIn(max = FlowMaxContentWidth),
                        state = state,
                        onBack = { viewModel.back() },
                        onNext = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            viewModel.next()
                        },
                        onRestore = {},
                    )
                }
            },
        ) { innerPadding ->
            SharedTransitionLayout(Modifier.fillMaxSize().padding(innerPadding)) {
                OnboardingSteps(
                    state = state,
                    heroFrom = heroFrom,
                    importOperation = importOperation,
                    newVideoAlerts = newVideoAlerts,
                    isFocusMode = isFocusMode,
                    interestsRevealed = interestsRevealed,
                    actions =
                        StepActions(
                            viewModel = viewModel,
                            onPick = ::pick,
                            onTopicToggle = { topic ->
                                val selecting = topic !in state.topics
                                haptic.performHapticFeedback(if (selecting) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                                viewModel.toggleTopic(topic)
                            },
                            onInterestsRevealed = { interestsRevealed = true },
                        ),
                )
            }
        }
    }
}
