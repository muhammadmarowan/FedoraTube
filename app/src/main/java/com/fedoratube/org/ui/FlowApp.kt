package com.fedoratube.org.ui

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fedoratube.org.MainActivity
import com.fedoratube.org.data.local.PlayerPreferences
import com.fedoratube.org.data.recommendation.FlowNeuroEngine
import com.fedoratube.org.player.DeepFlowManager
import com.fedoratube.org.player.EnhancedMusicPlayerManager
import com.fedoratube.org.player.EnhancedPlayerManager
import com.fedoratube.org.player.GlobalPlayerState
import com.fedoratube.org.player.SleepTimerManager
import com.fedoratube.org.ui.components.donation.DonationPromptHost
import com.fedoratube.org.ui.components.equalizer.LocalEqualizerState
import com.fedoratube.org.ui.components.layout.FlowBottomInsets
import com.fedoratube.org.ui.components.layout.LocalFlowBottomInsets
import com.fedoratube.org.ui.components.layout.navigation.FlowNavigationChrome
import com.fedoratube.org.ui.components.layout.navigation.FlowNavigationDefaults
import com.fedoratube.org.ui.components.layout.navigation.NavigationVisibility
import com.fedoratube.org.ui.components.layout.navigation.flowUsesNavigationRail
import com.fedoratube.org.ui.components.layout.navigation.rememberFlowNavigationScrollState
import com.fedoratube.org.ui.components.layout.navigation.resolveDefaultFlowTab
import com.fedoratube.org.ui.components.layout.navigation.visibleFlowTabs
import com.fedoratube.org.ui.components.layout.topbar.ProvideFlowGlobalActions
import com.fedoratube.org.ui.components.music.common.ProvideMusicPlaybackState
import com.fedoratube.org.ui.components.music.sheet.LocalMusicMenus
import com.fedoratube.org.ui.components.music.sheet.rememberMusicMenus
import com.fedoratube.org.ui.components.musicplayer.common.canTurnLight
import com.fedoratube.org.ui.components.musicplayer.sheet.rememberMusicPlayerSheetState
import com.fedoratube.org.ui.components.shared.LocalMediaOpenOrigins
import com.fedoratube.org.ui.components.shared.MediaMiniBarDefaults
import com.fedoratube.org.ui.components.shared.MediaOpenOrigins
import com.fedoratube.org.ui.components.shared.mediaMiniBarBounds
import com.fedoratube.org.ui.components.videoplayer.PlayerSheetValue
import com.fedoratube.org.ui.components.videoplayer.rememberPlayerDraggableState
import com.fedoratube.org.ui.screens.equalizer.EqualizerViewModel
import com.fedoratube.org.ui.screens.home.HomeViewModel
import com.fedoratube.org.ui.screens.notifications.NotificationViewModel
import com.fedoratube.org.ui.screens.player.VideoPlayerViewModel
import com.fedoratube.org.ui.screens.update.UPDATE_ROUTE
import com.fedoratube.org.ui.screens.update.UpdateLaunchEffect
import com.fedoratube.org.ui.theme.ThemeMode
import com.fedoratube.org.ui.theme.ThemeVariant
import com.fedoratube.org.ui.utils.LocalWindowSizeClass
import com.fedoratube.org.ui.utils.isMediumWidth
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt

@UnstableApi
@Composable
fun FlowApp(
    currentTheme: ThemeMode,
    themeVariant: ThemeVariant,
    systemLightThemeMode: ThemeMode,
    systemDarkThemeMode: ThemeMode,
    deeplinkVideoId: String? = null,
    isShort: Boolean = false,
    isSharedLink: Boolean = false,
    openMusicPlayerRequest: Int = 0,
    onDeeplinkConsumed: () -> Unit = {},
    pendingRoute: String? = null,
    onPendingRouteConsumed: () -> Unit = {},
    onStartDestinationKnown: () -> Unit = {},
) {
    val context = LocalContext.current
    val activity = context as? androidx.activity.ComponentActivity
    val navController = rememberNavController()
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }

    val playerViewModel: VideoPlayerViewModel = hiltViewModel(activity!!)
    // Activity-scoped so the unread badge has exactly one collector for the whole shell.
    val notificationViewModel: NotificationViewModel = hiltViewModel(activity)
    val equalizerViewModel: EqualizerViewModel = hiltViewModel(activity)
    val playerUiStateResult = playerViewModel.uiState.collectAsStateWithLifecycle()
    val playerUiState by playerUiStateResult

    val preferences = remember { PlayerPreferences(context) }
    val isHomeNavigationEnabled by preferences.homeNavigationEnabled.collectAsState(initial = true)
    val isShortsNavigationEnabled by preferences.effectiveShortsNavigationEnabled.collectAsState(initial = true)
    val isMusicNavigationEnabled by preferences.musicNavigationEnabled.collectAsState(initial = true)
    val isSearchNavigationEnabled by preferences.searchNavigationEnabled.collectAsState(initial = false)
    val isCategoriesNavigationEnabled by preferences.categoriesNavigationEnabled.collectAsState(initial = false)
    val disableShortsPlayer by preferences.effectiveDisableShortsPlayer.collectAsState(initial = false)
    val musicPlayerBackgroundStyle by preferences.musicPlayerBackgroundStyle.collectAsState(
        initial = com.fedoratube.org.data.local.MusicPlayerBackgroundStyle.BLUR_GRADIENT,
    )
    val musicPlayerFollowLightTheme by preferences.musicPlayerFollowLightTheme.collectAsState(initial = false)
    val navTabOrder by preferences.navTabOrder.collectAsState(initial = com.fedoratube.org.data.local.DEFAULT_NAV_TAB_ORDER)
    val defaultNavTabIndex by preferences.defaultNavTabIndex.collectAsState(initial = 0)
    val subscriptionRefreshOnStartup by preferences.subscriptionRefreshOnStartup.collectAsState(initial = false)
    val bottomNavHideOnScroll by preferences.bottomNavHideOnScroll.collectAsState(initial = true)
    val isFocusModeEnabled by preferences.focusModeEnabled.collectAsState(initial = false)
    val sleepTimerCloseAppOnExpiry by preferences.sleepTimerCloseAppOnExpiry.collectAsState(
        initial = SleepTimerManager.preferredCloseAppOnExpiry,
    )
    val navigationVisibility =
        NavigationVisibility(
            home = isHomeNavigationEnabled,
            shorts = isShortsNavigationEnabled,
            music = isMusicNavigationEnabled,
            search = isSearchNavigationEnabled,
            categories = isCategoriesNavigationEnabled,
        )
    val navigationTabs = remember(navTabOrder, navigationVisibility) { visibleFlowTabs(navTabOrder, navigationVisibility) }
    val defaultTab = resolveDefaultFlowTab(defaultNavTabIndex, navTabOrder, navigationVisibility)
    val defaultStartRoute = defaultTab.route

    // Mini Player Customizations
    val miniPlayerScale by preferences.miniPlayerScale.collectAsState(initial = 0.45f)
    val miniPlayerShowSkipControls by preferences.miniPlayerShowSkipControls.collectAsState(initial = false)
    val miniPlayerShowNextPrevControls by preferences.miniPlayerShowNextPrevControls.collectAsState(initial = false)
    val showRestoredMusicMiniPlayer by produceState<Boolean?>(initialValue = null, preferences) {
        preferences.showRestoredMusicMiniPlayer.collect { value = it }
    }

    // Offline Monitoring
    val currentRoute = remember { mutableStateOf(defaultStartRoute) }

    // Onboarding check
    var needsOnboarding by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        DeepFlowManager.initialize(context)
        val bypass = activity?.intent?.getBooleanExtra(MainActivity.EXTRA_BENCHMARK_BYPASS_ONBOARDING, false) == true
        // A viewer who is past onboarding never waits for the engine here; everyone else asks it (#1301).
        needsOnboarding =
            when {
                bypass || preferences.onboardingDone.first() -> {
                    false
                }

                else -> {
                    FlowNeuroEngine.initialize(context)
                    FlowNeuroEngine.needsOnboarding().also { preferences.setOnboardingDone(!it) }
                }
            }
        onStartDestinationKnown()
    }

    FlowAppSideEffects(
        snackbarHostState = snackbarHostState,
        sleepTimerCloseAppOnExpiry = sleepTimerCloseAppOnExpiry,
        subscriptionRefreshOnStartup = subscriptionRefreshOnStartup,
    )

    HandleIncomingVideo(deeplinkVideoId, isShort, isSharedLink, navController, preferences, onDeeplinkConsumed)
    OfflineMonitor(context, navController, snackbarHostState, currentRoute)

    val currentEntry by navController.currentBackStackEntryAsState()
    val statusBarHero = remember { StatusBarHeroState() }
    val currentTab = currentEntry?.flowTab()
    val selectedTab by rememberSelectedFlowTab(navController, defaultTab)
    val usesNavigationRail = flowUsesNavigationRail()
    var navigationRailWidth by remember { mutableStateOf(0.dp) }
    var navigationBarHeight by remember { mutableStateOf(FlowNavigationDefaults.BarHeight) }

    FlowStartTabEffects(
        navController = navController,
        currentRoute = currentRoute,
        defaultTab = defaultTab,
        isHomeNavigationEnabled = isHomeNavigationEnabled,
        needsOnboarding = needsOnboarding,
    )

    val isHomeRoute = currentRoute.value == "home"
    val navScrollState =
        rememberFlowNavigationScrollState(
            hideOnScroll = bottomNavHideOnScroll && isHomeRoute && !isFocusModeEnabled,
            routeKey = currentRoute.value,
            locked = (!isHomeRoute) || isFocusModeEnabled || currentRoute.value == SHORTS_ROUTE_KEY,
        )

    val isInPipMode by GlobalPlayerState.isInPipMode.collectAsState()
    val currentVideo by GlobalPlayerState.currentVideo.collectAsState()
    val isShortsPlayerRoute = currentRoute.value == SHORTS_ROUTE_KEY

    LaunchedEffect(isShortsPlayerRoute) {
        if (isShortsPlayerRoute) {
            EnhancedPlayerManager.getInstance().pause()
            GlobalPlayerState.hideMiniPlayer()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val screenHeightPx = constraints.maxHeight.toFloat()

        val navBarBottomInset = WindowInsets.navigationBars.getBottom(density)

        val playerSheetState = rememberPlayerDraggableState()
        val mediaOpenOrigins = remember { MediaOpenOrigins() }
        val playerVisibleState = remember { mutableStateOf(false) }
        var playerVisible by playerVisibleState

        val musicPlayerSheetState = rememberMusicPlayerSheetState()
        val musicMenus = rememberMusicMenus()
        val collapseExpandedPlayers: () -> Unit =
            remember(playerSheetState, musicPlayerSheetState) {
                {
                    if (playerSheetState.currentValue == PlayerSheetValue.Expanded) playerSheetState.collapse()
                    if (musicPlayerSheetState.isExpanded) musicPlayerSheetState.collapse()
                }
            }
        val currentShortsExitRoute by rememberUpdatedState(shortsExitRoute(navigationTabs))
        val mediaNavigator =
            remember(navController, collapseExpandedPlayers) {
                FlowMediaNavigator(navController, collapseExpandedPlayers) { currentShortsExitRoute }
            }
        // A page opened from another app would otherwise land under an expanded player.
        HandlePendingRoute(
            pendingRoute,
            navController,
            defaultStartRoute,
            onPendingRouteConsumed,
            onBeforeNavigate = collapseExpandedPlayers,
        )

        val activeVideo = playerUiState.cachedVideo

        FlowPlayerSessionEffects(
            playerSheetState = playerSheetState,
            playerViewModel = playerViewModel,
            playerUiStateResult = playerUiStateResult,
            playerVisibleState = playerVisibleState,
            isInPipMode = isInPipMode,
            openOrigins = mediaOpenOrigins,
        )

        val currentMusicTrack by EnhancedMusicPlayerManager.currentTrack.collectAsStateWithLifecycle()
        var suppressMusicMiniAfterVideo by remember { mutableStateOf(false) }
        val openMusicPlayerOnPlay = preferences.openMusicPlayerOnPlay.collectAsState(initial = false)
        // Shows the player for a song that just started, without a route: a navigation here used to
        // swap the page out and back for a frame, which the mini player now leaves in view.
        val onMusicStarted: () -> Unit =
            remember(musicPlayerSheetState, playerViewModel) {
                {
                    // One player at a time: a video still cached, even one playing in the
                    // background, would keep the music from showing its own mini player.
                    if (playerViewModel.uiState.value.cachedVideo != null) {
                        playerVisible = false
                        playerViewModel.clearVideo()
                    }
                    suppressMusicMiniAfterVideo = false
                    if (openMusicPlayerOnPlay.value) {
                        musicPlayerSheetState.expand()
                    } else if (musicPlayerSheetState.isDismissed) {
                        musicPlayerSheetState.collapse()
                    }
                }
            }
        var handledMusicPlayerRequest by remember { mutableIntStateOf(0) }

        LaunchedEffect(activeVideo?.id) {
            if (activeVideo != null) {
                suppressMusicMiniAfterVideo = true
            }
        }

        LaunchedEffect(currentMusicTrack?.videoId) {
            if (currentMusicTrack == null) {
                suppressMusicMiniAfterVideo = false
            }
        }

        LaunchedEffect(currentRoute.value) {
            if (currentRoute.value == "musicPlayer") {
                suppressMusicMiniAfterVideo = false
            }
        }

        LaunchedEffect(currentMusicTrack, showRestoredMusicMiniPlayer) {
            if (currentMusicTrack != null &&
                showRestoredMusicMiniPlayer == true &&
                musicPlayerSheetState.isDismissed
            ) {
                musicPlayerSheetState.collapse()
            } else if (currentMusicTrack == null) {
                musicPlayerSheetState.dismiss()
            }
        }

        LaunchedEffect(showRestoredMusicMiniPlayer) {
            if (showRestoredMusicMiniPlayer == false && !musicPlayerSheetState.isExpanded) {
                musicPlayerSheetState.dismiss()
            }
        }

        LaunchedEffect(openMusicPlayerRequest, currentMusicTrack?.videoId) {
            if (openMusicPlayerRequest > handledMusicPlayerRequest && currentMusicTrack != null) {
                handledMusicPlayerRequest = openMusicPlayerRequest
                suppressMusicMiniAfterVideo = false
                if (playerVisible) {
                    playerSheetState.collapse()
                }
                musicPlayerSheetState.expand()
            }
        }

        ApplyStatusBarStyle(
            themeMode = currentTheme,
            themeVariant = themeVariant,
            systemLightThemeMode = systemLightThemeMode,
            systemDarkThemeMode = systemDarkThemeMode,
            isFullscreen = false,
            isMusicPlayerImmersive = currentMusicTrack != null && musicPlayerSheetState.isImmersive,
            musicPlayerFollowsTheme =
                musicPlayerBackgroundStyle == com.fedoratube.org.data.local.MusicPlayerBackgroundStyle.DEFAULT ||
                    (musicPlayerFollowLightTheme && musicPlayerBackgroundStyle.canTurnLight),
            isShortsPlayer = isShortsPlayerRoute,
            heroUnderStatusBar =
                statusBarHero.heroUnderStatusBar &&
                    !musicPlayerSheetState.isExpanded &&
                    playerSheetState.currentValue != PlayerSheetValue.Expanded,
        )

        LaunchedEffect(isInPipMode) {
            if (!isInPipMode) return@LaunchedEffect
            navController.awaitGraph()
            val video = currentVideo
            val route = currentRoute.value
            if (route != SHORTS_ROUTE_KEY && !route.startsWith("player") && video != null) {
                navController.navigate("player/${video.id}")
            }
        }

        val activeMiniPlayer =
            resolveActiveMiniPlayer(
                hasVideo = playerUiState.cachedVideo != null,
                videoVisible = playerVisible,
                videoInBackground = playerUiState.isBackgroundPlaybackMode,
                onShortsPlayer = isShortsPlayerRoute,
                hasMusic = currentMusicTrack != null,
                musicSuppressed = suppressMusicMiniAfterVideo,
            )
        val isMusicSheetShown = activeMiniPlayer == ActiveMiniPlayer.Music
        val isPlayerCoveringContent =
            (playerVisible && playerSheetState.currentValue == PlayerSheetValue.Expanded) ||
                (isMusicSheetShown && musicPlayerSheetState.isExpanded)
        val showBottomNav = !isInPipMode && currentTab.showsNavigationBar() && !isPlayerCoveringContent && !isFocusModeEnabled
        val isBottomNavShown = !usesNavigationRail && showBottomNav && navScrollState.isBarVisible
        // The rail is hidden only where content goes truly full screen; the expanded players cover
        // it instead, so opening them never re-lays out the page beneath.
        val currentDestinationRoute = currentEntry?.destination?.route
        val isNavigationRailVisible =
            !isInPipMode &&
                needsOnboarding != null &&
                currentDestinationRoute != "onboarding" &&
                !(currentDestinationRoute == SHORTS_ROUTE_PATTERN && currentTab == null) &&
                !isFocusModeEnabled
        val showVideoBar = activeMiniPlayer == ActiveMiniPlayer.VideoBar && !isInPipMode
        val isMiniBarObscuringContent =
            (isMusicSheetShown && !musicPlayerSheetState.isDismissed && !musicPlayerSheetState.isExpanded) || showVideoBar
        val motionScheme = MaterialTheme.motionScheme
        val barFraction = remember { Animatable(if (isBottomNavShown) 1f else 0f) }
        LaunchedEffect(isBottomNavShown) {
            // The same springs as the bar's own slide, so whatever rides on it stays glued to it.
            barFraction.animateTo(
                targetValue = if (isBottomNavShown) 1f else 0f,
                animationSpec = if (isBottomNavShown) motionScheme.defaultSpatialSpec() else motionScheme.fastSpatialSpec(),
            )
        }
        val systemBottomState = rememberUpdatedState(with(density) { navBarBottomInset.toDp() })
        val barHeightState = rememberUpdatedState(if (usesNavigationRail) 0.dp else navigationBarHeight)
        val barShownState = rememberUpdatedState(isBottomNavShown)
        val miniPlayerFraction = remember { Animatable(if (isMiniBarObscuringContent) 1f else 0f) }
        LaunchedEffect(isMiniBarObscuringContent) {
            miniPlayerFraction.animateTo(
                targetValue = if (isMiniBarObscuringContent) 1f else 0f,
                animationSpec = motionScheme.defaultSpatialSpec(),
            )
        }
        val miniPlayerShownState = rememberUpdatedState(isMiniBarObscuringContent)
        val miniPlayerHeightState = rememberUpdatedState(MediaMiniBarDefaults.Height + MediaMiniBarDefaults.BottomSpacer)
        val miniPlayerBounds =
            with(density) {
                mediaMiniBarBounds(
                    containerWidthPx = constraints.maxWidth.toFloat(),
                    startInsetPx = (if (usesNavigationRail && isNavigationRailVisible) navigationRailWidth else 0.dp).toPx(),
                    isCompactWidth = !LocalWindowSizeClass.current.isMediumWidth,
                    compactMarginPx = MediaMiniBarDefaults.CompactMargin.toPx(),
                    largeMarginPx = MediaMiniBarDefaults.LargeMargin.toPx(),
                    maxWidthPx = MediaMiniBarDefaults.MaxWidth.toPx(),
                )
            }
        val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        val miniPlayerSpanState =
            rememberUpdatedState(
                miniPlayerBounds.let { bounds ->
                    val left = if (isRtl) constraints.maxWidth - bounds.start - bounds.width else bounds.start
                    left..(left + bounds.width)
                },
            )
        val bottomInsets =
            remember {
                FlowBottomInsets(
                    systemInset = systemBottomState,
                    barHeight = barHeightState,
                    barShown = barShownState,
                    miniPlayerHeight = miniPlayerHeightState,
                    miniPlayerShown = miniPlayerShownState,
                    barFraction = { barFraction.value },
                    miniPlayerFraction = { miniPlayerFraction.value },
                    miniPlayerSpanPx = miniPlayerSpanState,
                )
            }
        FlowNavigationChrome(
            tabs = navigationTabs,
            selectedTab = selectedTab,
            onTabSelected = { tab ->
                if (currentTab == tab) {
                    TabScrollEventBus.emitScrollToTop(tab.route)
                } else {
                    currentRoute.value = tab.route
                    navController.navigateToTab(tab, defaultStartRoute)
                }
            },
            barVisible = showBottomNav && navScrollState.isBarVisible,
            railVisible = isNavigationRailVisible,
            onBarHeightChanged = { navigationBarHeight = it },
            onRailWidthChanged = { navigationRailWidth = it },
        ) {
            ProvideMusicPlaybackState(
                surfacesVisible = !musicPlayerSheetState.isExpanded && playerSheetState.currentValue != PlayerSheetValue.Expanded,
            ) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor =
                        if (isInPipMode) {
                            androidx.compose.ui.graphics.Color.Black
                        } else {
                            androidx.compose.material3.MaterialTheme.colorScheme.background
                        },
                    // Pages run under the bottom chrome and clear it themselves through LocalFlowBottomInsets.
                    contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                ) { paddingValues ->
                    val layoutDirection = LocalLayoutDirection.current
                    val contentPadding =
                        if (
                            isShortsPlayerRoute
                        ) {
                            PaddingValues(
                                start = paddingValues.calculateStartPadding(layoutDirection),
                                top = 0.dp,
                                end = paddingValues.calculateEndPadding(layoutDirection),
                                bottom = bottomInsets.systemBottom,
                            )
                        } else {
                            paddingValues
                        }
                    Box(
                        modifier =
                            Modifier
                                .padding(if (isInPipMode) PaddingValues(0.dp) else contentPadding)
                                .nestedScroll(navScrollState.nestedScrollConnection),
                    ) {
                        if (needsOnboarding != null) {
                            val homeViewModel: HomeViewModel = hiltViewModel(activity!!)
                            LaunchedEffect(homeViewModel) {
                                homeViewModel.initialize(context.applicationContext)
                            }

                            ProvideFlowGlobalActions(
                                unreadNotifications = notificationViewModel.unreadCount,
                                onOpenNotifications = { navController.navigate("notifications") },
                                onOpenSettings = { navController.navigate("settings") },
                            ) {
                                CompositionLocalProvider(
                                    *mediaNavigationLocals(mediaNavigator),
                                    LocalMediaOpenOrigins provides mediaOpenOrigins,
                                    LocalMusicMenus provides musicMenus,
                                    LocalEqualizerState provides equalizerViewModel.state,
                                    LocalFlowBottomInsets provides bottomInsets,
                                    LocalStatusBarHero provides statusBarHero,
                                ) {
                                    NavHost(
                                        navController = navController,
                                        startDestination = if (needsOnboarding == true) "onboarding" else defaultStartRoute,
                                        enterTransition = FlowNavTransitions.enter,
                                        exitTransition = FlowNavTransitions.exit,
                                        popEnterTransition = FlowNavTransitions.popEnter,
                                        popExitTransition = FlowNavTransitions.popExit,
                                        predictivePopEnterTransition = FlowNavTransitions.predictivePopEnter,
                                        predictivePopExitTransition = FlowNavTransitions.predictivePopExit,
                                    ) {
                                        flowAppGraph(
                                            navController = navController,
                                            mediaNavigator = mediaNavigator,
                                            currentRoute = currentRoute,
                                            playerSheetState = playerSheetState,
                                            musicPlayerSheetState = musicPlayerSheetState,
                                            homeViewModel = homeViewModel,
                                            playerViewModel = playerViewModel,
                                            playerUiStateResult = playerUiStateResult,
                                            playerVisibleState = playerVisibleState,
                                            disableShortsPlayer = disableShortsPlayer,
                                            defaultStartRoute = defaultStartRoute,
                                            onMusicStarted = onMusicStarted,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        val bottomPaddingTarget =
            if (isBottomNavShown) {
                navigationBarHeight + with(density) { navBarBottomInset.toDp() }
            } else {
                with(density) { navBarBottomInset.toDp() }
            }
        FlowPlayerOverlays(
            navController = navController,
            mediaNavigator = mediaNavigator,
            playerViewModel = playerViewModel,
            playerUiStateResult = playerUiStateResult,
            playerVisibleState = playerVisibleState,
            playerSheetState = playerSheetState,
            activeVideo = activeVideo,
            isShortsPlayerRoute = isShortsPlayerRoute,
            bottomPadding = bottomPaddingTarget,
            startInset = if (usesNavigationRail && isNavigationRailVisible) navigationRailWidth else 0.dp,
            miniPlayerScale = miniPlayerScale,
            miniPlayerShowSkipControls = miniPlayerShowSkipControls,
            miniPlayerShowNextPrevControls = miniPlayerShowNextPrevControls,
            showMusicSheet = isMusicSheetShown,
            showVideoBar = showVideoBar,
            musicPlayerSheetState = musicPlayerSheetState,
            containerWidth = maxWidth,
            containerHeight = with(density) { screenHeightPx.toDp() },
            miniBarBounds = miniPlayerBounds,
            musicMenus = musicMenus,
            equalizerState = equalizerViewModel.state,
            bottomInsets = bottomInsets,
            openOrigins = mediaOpenOrigins,
            snackbarHostState = snackbarHostState,
        )

        androidx.compose.material3.SnackbarHost(
            hostState = snackbarHostState,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp)
                    .offset {
                        val lift = bottomInsets.floatingBottomPx(this) + 12.dp.toPx()
                        IntOffset(0, -lift.roundToInt())
                    },
        )

        UpdateLaunchEffect(needsOnboarding = needsOnboarding, onOpenUpdate = { navController.navigate(UPDATE_ROUTE) })

        DonationPromptHost(
            enabled = needsOnboarding == false && !isInPipMode && !playerVisible,
            onNavigateToDonations = { navController.navigate("donations") },
        )
    }
}
