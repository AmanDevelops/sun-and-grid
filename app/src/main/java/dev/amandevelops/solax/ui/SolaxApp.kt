package dev.amandevelops.solax.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.amandevelops.solax.ui.components.AppIcons
import dev.amandevelops.solax.ui.components.tap
import dev.amandevelops.solax.ui.screens.DeviceScreen
import dev.amandevelops.solax.ui.screens.HistoryScreen
import dev.amandevelops.solax.ui.screens.SettingsScreen
import dev.amandevelops.solax.ui.screens.SolarScreen
import dev.amandevelops.solax.ui.screens.TodayScreen
import dev.amandevelops.solax.ui.screens.UsageScreen
import dev.amandevelops.solax.ui.theme.AppText
import dev.amandevelops.solax.ui.theme.LocalAppColors

private enum class AppTab(
    val route: AppRoute,
    val label: String,
    val icon: ImageVector,
) {
    TODAY(AppRoute.TODAY, "Today", AppIcons.DockToday),
    SOLAR(AppRoute.SOLAR, "Solar", AppIcons.DockSolar),
    USAGE(AppRoute.USAGE, "Usage", AppIcons.DockUsage),
    DEVICE(AppRoute.DEVICE, "Device", AppIcons.DockDevice),
}

private enum class AppRoute {
    TODAY,
    SOLAR,
    USAGE,
    DEVICE,
    SETTINGS,
    HISTORY,
}

@Composable
fun SolaxApp(
    viewModel: MainViewModel,
    state: MonitorUiState,
) {
    val colors = LocalAppColors.current
    var tab by rememberSaveable { mutableStateOf(AppTab.TODAY) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    val route = when {
        showSettings -> AppRoute.SETTINGS
        showHistory -> AppRoute.HISTORY
        else -> tab.route
    }
    val overlayOpen = showSettings || showHistory
    val stateHolder = rememberSaveableStateHolder()

    val usage = remember(state.settings.demoMode, state.uppcl.monthData) {
        when {
            state.settings.demoMode -> demoUsageMonth()
            state.uppcl.monthData != null -> state.uppcl.monthData.toUsageMonth()
            else -> UsageMonth.empty()
        }
    }

    BackHandler(enabled = overlayOpen || tab != AppTab.TODAY) {
        when {
            showSettings -> showSettings = false
            showHistory -> showHistory = false
            else -> tab = AppTab.TODAY
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Top,
                    ),
                ),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = 520.dp)
                    .fillMaxSize()
                    .imePadding(),
            ) {
                AnimatedContent(
                    targetState = route,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(190)) togetherWith
                            fadeOut(animationSpec = tween(110))
                    },
                    label = "app-route",
                ) { destination ->
                    stateHolder.SaveableStateProvider(destination.name) {
                        when (destination) {
                            AppRoute.TODAY -> TodayScreen(
                                state = state,
                                usage = usage,
                                usageIsDemo = state.settings.demoMode,
                                onOpenSettings = { showSettings = true },
                            )

                            AppRoute.SOLAR -> SolarScreen(
                                state = state,
                                onOpenSettings = { showSettings = true },
                            )

                            AppRoute.USAGE -> UsageScreen(
                                state = state,
                                usage = usage,
                                onOpenSettings = { showSettings = true },
                            )

                            AppRoute.DEVICE -> DeviceScreen(
                                state = state,
                                onOpenSettings = { showSettings = true },
                                onOpenHistory = { showHistory = true },
                            )

                            AppRoute.HISTORY -> HistoryScreen(
                                state = state,
                                onBack = { showHistory = false },
                            )

                            AppRoute.SETTINGS -> SettingsScreen(
                                state = state,
                                onBack = { showSettings = false },
                                onSaveConnection = viewModel::saveConnection,
                                onTestConnection = viewModel::testConnection,
                                onClearConnectionTest = viewModel::clearConnectionTest,
                                onSetDemoMode = viewModel::setDemoMode,
                                onSetTheme = viewModel::setTheme,
                                onSetInvertGridSign = viewModel::setInvertGridSign,
                                onSetBackgroundSync = viewModel::setBackgroundSync,
                                onClearHistory = viewModel::clearHistory,
                                onSaveUppcl = viewModel::saveUppclConnection,
                                onTestUppcl = viewModel::testUppclConnection,
                                onClearUppclTest = viewModel::clearUppclConnectionTest,
                                onDisconnectUppcl = viewModel::disconnectUppcl,
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = !overlayOpen,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(animationSpec = tween(180)),
            exit = fadeOut(animationSpec = tween(110)),
        ) {
            AppDock(
                selected = tab,
                onSelect = {
                    if (tab != it || overlayOpen) {
                        showSettings = false
                        showHistory = false
                        tab = it
                    }
                },
            )
        }
    }
}

@Composable
private fun AppDock(
    selected: AppTab,
    onSelect: (AppTab) -> Unit,
) {
    val colors = LocalAppColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 14.dp)
                .widthIn(max = 340.dp)
                .fillMaxWidth()
                .clip(CircleShape)
                .background(colors.ink)
                .padding(5.dp),
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                val slot = maxWidth / 4
                val indicatorOffset by animateDpAsState(
                    targetValue = slot * AppTab.entries.indexOf(selected),
                    animationSpec = spring(
                        dampingRatio = 0.68f,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                    label = "dock-indicator",
                )

                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffset)
                        .width(slot)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(colors.sun),
                )

                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    AppTab.entries.forEach { entry ->
                        DockButton(
                            tab = entry,
                            selected = entry == selected,
                            onClick = { onSelect(entry) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DockButton(
    tab: AppTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val tint = if (selected) colors.onSun else colors.background
    Column(
        modifier = modifier
            .clip(CircleShape)
            .tap(onClick)
            .alpha(if (selected) 1f else 0.65f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = tint,
        )
        Text(
            text = tab.label,
            style = AppText.dock.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            ),
            color = tint,
            maxLines = 1,
        )
    }
}
