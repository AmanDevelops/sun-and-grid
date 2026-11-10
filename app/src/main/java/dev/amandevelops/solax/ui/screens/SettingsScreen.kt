package dev.amandevelops.solax.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.amandevelops.solax.BuildConfig
import dev.amandevelops.solax.data.ConnectionTest
import dev.amandevelops.solax.data.ThemeMode
import dev.amandevelops.solax.data.UppclConnectionTest
import dev.amandevelops.solax.data.UppclErrorKind
import dev.amandevelops.solax.ui.MonitorStatus
import dev.amandevelops.solax.ui.MonitorUiState
import dev.amandevelops.solax.ui.UppclStatus
import dev.amandevelops.solax.ui.components.AppIcons
import dev.amandevelops.solax.ui.components.SunGridCard
import dev.amandevelops.solax.ui.components.tap
import dev.amandevelops.solax.ui.energyText
import dev.amandevelops.solax.ui.powerText
import dev.amandevelops.solax.ui.theme.AppText
import dev.amandevelops.solax.ui.theme.LocalAppColors

@Composable
fun SettingsScreen(
    state: MonitorUiState,
    onBack: () -> Unit,
    onSaveConnection: (String, String) -> Unit,
    onTestConnection: (String, String) -> Unit,
    onClearConnectionTest: () -> Unit,
    onSetDemoMode: (Boolean) -> Unit,
    onSetTheme: (ThemeMode) -> Unit,
    onSetInvertGridSign: (Boolean) -> Unit,
    onSetBackgroundSync: (Boolean) -> Unit,
    onClearHistory: () -> Unit,
    onSaveUppcl: (String, String, String) -> Unit,
    onTestUppcl: (String, String, String) -> Unit,
    onClearUppclTest: () -> Unit,
    onDisconnectUppcl: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val settings = state.settings
    var host by remember(settings.host) { mutableStateOf(settings.host) }
    var dongleSerial by remember(settings.dongleSerial) { mutableStateOf(settings.dongleSerial) }
    var showClearDialog by rememberSaveable { mutableStateOf(false) }
    var advancedOpen by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val serialFocus = remember { FocusRequester() }
    val configured = host.isNotBlank() && dongleSerial.isNotBlank()

    fun saveConnection() {
        if (configured) {
            focusManager.clearFocus()
            onSaveConnection(host, dongleSerial)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { SettingsHeader(onBack = onBack) }

        item {
            SunGridCard(radius = 26.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle(
                        title = "Inverter connection",
                        subtitle = when (state.status) {
                            MonitorStatus.DEMO -> "Example data"
                            MonitorStatus.ONLINE -> "Connected"
                            MonitorStatus.OFFLINE -> "Offline"
                            MonitorStatus.NEEDS_SETUP -> "Not set up"
                            MonitorStatus.LOADING -> "Connecting"
                        },
                        subtitleColor = when (state.status) {
                            MonitorStatus.ONLINE, MonitorStatus.DEMO -> colors.ok
                            MonitorStatus.OFFLINE -> colors.bad
                            MonitorStatus.NEEDS_SETUP -> colors.sun
                            MonitorStatus.LOADING -> colors.mute
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Enter the IP address and the serial number printed on the Wi-Fi dongle.",
                    style = AppText.sub,
                    color = colors.mute,
                )
                Spacer(Modifier.height(16.dp))

                FieldLabel("Inverter IP address")
                Spacer(Modifier.height(6.dp))
                SettingsField(
                    value = host,
                    onValueChange = {
                        host = it
                        onClearConnectionTest()
                    },
                    placeholder = "192.168.10.10",
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next,
                    onImeAction = { serialFocus.requestFocus() },
                )

                Spacer(Modifier.height(12.dp))
                FieldLabel("Dongle serial number")
                Spacer(Modifier.height(6.dp))
                SettingsField(
                    value = dongleSerial,
                    onValueChange = {
                        dongleSerial = it
                        onClearConnectionTest()
                    },
                    placeholder = "SRXXXXXXXXX",
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Done,
                    onImeAction = { saveConnection() },
                    modifier = Modifier.focusRequester(serialFocus),
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = "Also used as the local password.",
                    style = AppText.sub,
                    color = colors.mute,
                )

                state.connectionTest?.let { result ->
                    Spacer(Modifier.height(14.dp))
                    ConnectionResult(result)
                }

                Spacer(Modifier.height(16.dp))
                GridButton(
                    text = if (state.isTesting) "Testing connection…" else "Test connection",
                    onClick = {
                        focusManager.clearFocus()
                        onTestConnection(host, dongleSerial)
                    },
                    enabled = configured && !state.isTesting,
                    loading = state.isTesting,
                    primary = false,
                )
                Spacer(Modifier.height(10.dp))
                GridButton(
                    text = "Save and connect",
                    onClick = { saveConnection() },
                    enabled = configured && !state.isTesting,
                    primary = true,
                )
                if (settings.demoMode) {
                    Spacer(Modifier.height(9.dp))
                    Text(
                        text = "Saving a connection turns off example data.",
                        style = AppText.sub,
                        color = colors.mute,
                    )
                }
            }
        }

        item {
            UppclSettingsCard(
                state = state,
                onSave = onSaveUppcl,
                onTest = onTestUppcl,
                onClearTest = onClearUppclTest,
                onDisconnect = onDisconnectUppcl,
            )
        }

        item {
            SunGridCard(radius = 26.dp) {
                ToggleRow(
                    title = "Example data",
                    subtitle = "Show sample readings so you can explore without an inverter.",
                    checked = settings.demoMode,
                    onCheckedChange = onSetDemoMode,
                )
            }
        }

        item {
            SunGridCard(radius = 26.dp) {
                Text(text = "Appearance", style = AppText.h2, color = colors.ink)
                Spacer(Modifier.height(12.dp))
                ThemePicker(
                    selected = settings.theme,
                    onSelect = onSetTheme,
                )
            }
        }

        item {
            SunGridCard(radius = 26.dp, padding = PaddingValues(horizontal = 18.dp, vertical = 6.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .tap { advancedOpen = !advancedOpen }
                        .padding(vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "More options",
                        style = AppText.h2,
                        color = colors.ink,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = AppIcons.ChevronDown,
                        contentDescription = if (advancedOpen) "Hide options" else "Show options",
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(if (advancedOpen) 180f else 0f),
                        tint = colors.mute,
                    )
                }
                if (advancedOpen) {
                    SolidDivider()
                    ToggleRow(
                        title = "Collect in background",
                        subtitle = "Save a reading every 15 minutes while the app is closed.",
                        checked = settings.backgroundSync,
                        onCheckedChange = onSetBackgroundSync,
                    )
                    SolidDivider()
                    ToggleRow(
                        title = "Swap grid direction",
                        subtitle = "Turn on only if export and import are shown the wrong way.",
                        checked = settings.invertGridSign,
                        onCheckedChange = onSetInvertGridSign,
                    )
                    SolidDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .tap { showClearDialog = true }
                            .padding(vertical = 17.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Clear history",
                            style = AppText.body.copy(fontWeight = FontWeight.Bold),
                            color = colors.bad,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "Delete",
                            style = AppText.button,
                            color = colors.bad,
                        )
                    }
                }
            }
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
                Text(
                    text = "Sun & Grid ${BuildConfig.VERSION_NAME}",
                    style = AppText.sub,
                    color = colors.mute,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Everything stays on your local network.",
                    style = AppText.sub,
                    color = colors.mute.copy(alpha = 0.8f),
                )
            }
        }
    }

    if (showClearDialog) {
        ConfirmDialog(
            title = "Clear all history?",
            message = "This deletes every saved reading on this phone. It cannot be undone.",
            confirmLabel = "Clear history",
            confirmColor = colors.bad,
            onConfirm = {
                onClearHistory()
                showClearDialog = false
            },
            onDismiss = { showClearDialog = false },
        )
    }
}

@Composable
private fun SettingsHeader(onBack: () -> Unit) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(colors.card)
                .tap(onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = AppIcons.Back,
                contentDescription = "Back",
                modifier = Modifier.size(20.dp),
                tint = colors.ink,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Sun & Grid", style = AppText.sub, color = colors.mute)
            Spacer(Modifier.height(4.dp))
            Text(text = "Settings", style = AppText.header, color = colors.ink)
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    subtitle: String? = null,
    subtitleColor: Color = LocalAppColors.current.mute,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    Column(modifier = modifier) {
        Text(text = title, style = AppText.h2, color = colors.ink)
        if (subtitle != null) {
            Text(text = subtitle, style = AppText.sub, color = subtitleColor)
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = AppText.sub.copy(fontWeight = FontWeight.SemiBold),
        color = LocalAppColors.current.mute,
    )
}

@Composable
private fun SettingsField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = LocalAppColors.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth().heightIn(min = 54.dp),
        placeholder = { Text(placeholder, style = AppText.body, color = colors.mute) },
        singleLine = true,
        textStyle = AppText.body,
        shape = RoundedCornerShape(15.dp),
        visualTransformation = visualTransformation,
        trailingIcon = trailing,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction,
        ),
        keyboardActions = KeyboardActions(
            onNext = { onImeAction() },
            onDone = { onImeAction() },
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = colors.ink,
            unfocusedTextColor = colors.ink,
            focusedContainerColor = colors.background.copy(alpha = 0.72f),
            unfocusedContainerColor = colors.background.copy(alpha = 0.72f),
            cursorColor = colors.grid,
            focusedBorderColor = colors.grid,
            unfocusedBorderColor = colors.line,
            focusedPlaceholderColor = colors.mute,
            unfocusedPlaceholderColor = colors.mute,
        ),
    )
}

@Composable
private fun GridButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    primary: Boolean,
    loading: Boolean = false,
) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .alpha(if (enabled) 1f else 0.5f)
            .clip(shape)
            .background(if (primary) colors.sun else colors.card)
            .then(if (primary) Modifier else Modifier.border(1.dp, colors.line, shape))
            .then(if (enabled && !loading) Modifier.tap(onClick) else Modifier)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = if (primary) colors.onSun else colors.mute,
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = AppText.button,
            color = if (primary) colors.onSun else colors.ink,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = AppText.body.copy(fontWeight = FontWeight.Bold),
                color = colors.ink,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = AppText.sub,
                color = colors.mute,
            )
        }
        Spacer(Modifier.width(14.dp))
        GridSwitch(checked = checked)
    }
}

@Composable
private fun GridSwitch(checked: Boolean) {
    val colors = LocalAppColors.current
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 18.dp else 0.dp,
        label = "switch",
    )
    Box(
        modifier = Modifier
            .size(width = 44.dp, height = 26.dp)
            .clip(CircleShape)
            .background(if (checked) colors.sun else colors.line),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .padding(3.dp)
                .offset(x = thumbOffset)
                .size(20.dp)
                .clip(CircleShape)
                .background(if (checked) colors.onSun else colors.mute),
        )
    }
}

@Composable
private fun ThemePicker(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.background)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        ThemeMode.entries.forEach { mode ->
            val active = selected == mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .then(
                        if (active) {
                            Modifier
                                .background(colors.card)
                                .border(1.dp, colors.line, RoundedCornerShape(14.dp))
                        } else {
                            Modifier
                        },
                    )
                    .tap { onSelect(mode) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = when (mode) {
                        ThemeMode.SYSTEM -> "Auto"
                        ThemeMode.LIGHT -> "Light"
                        ThemeMode.DARK -> "Dark"
                    },
                    style = AppText.button,
                    color = if (active) colors.ink else colors.mute,
                )
            }
        }
    }
}

@Composable
private fun ConnectionResult(result: ConnectionTest) {
    val colors = LocalAppColors.current
    val success = result is ConnectionTest.Success
    val tint = if (success) colors.ok else colors.bad
    val title = if (success) "Connection works" else "Couldn't connect"
    val message = when (result) {
        is ConnectionTest.Success -> {
            val model = result.reading.model.ifBlank { "Inverter" }
            "$model · ${powerText(result.reading.powerW)} now"
        }
        is ConnectionTest.Failure -> result.message
    }
    ResultBanner(
        success = success,
        title = title,
        message = message,
        tint = tint,
    )
}

@Composable
private fun ResultBanner(
    success: Boolean,
    title: String,
    message: String,
    tint: Color,
    hint: String? = null,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(tint.copy(alpha = 0.15f))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (success) AppIcons.CheckCircle else AppIcons.Alert,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = tint,
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                style = AppText.body.copy(fontWeight = FontWeight.Bold),
                color = colors.ink,
            )
            Text(text = message, style = AppText.sub, color = colors.mute)
            if (hint != null) {
                Spacer(Modifier.height(3.dp))
                Text(text = hint, style = AppText.sub, color = colors.mute)
            }
        }
    }
}

@Composable
private fun UppclSettingsCard(
    state: MonitorUiState,
    onSave: (String, String, String) -> Unit,
    onTest: (String, String, String) -> Unit,
    onClearTest: () -> Unit,
    onDisconnect: () -> Unit,
) {
    val colors = LocalAppColors.current
    val settings = state.settings
    val uppcl = state.uppcl
    var username by remember(settings.uppclUsername) { mutableStateOf(settings.uppclUsername) }
    var password by remember(settings.uppclPassword) { mutableStateOf(settings.uppclPassword) }
    var accountNumber by remember(settings.uppclAccountNumber) {
        mutableStateOf(settings.uppclAccountNumber)
    }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmDisconnect by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val passwordFocus = remember { FocusRequester() }
    val accountFocus = remember { FocusRequester() }
    val complete = username.isNotBlank() && password.isNotBlank() && accountNumber.isNotBlank()

    fun save() {
        if (!complete) return
        focusManager.clearFocus()
        onSave(username, password, accountNumber)
    }

    SunGridCard(radius = 26.dp) {
        SectionTitle(
            title = "UPPCL usage",
            subtitle = when {
                uppcl.configured && uppcl.status == UppclStatus.ERROR -> "Couldn't refresh"
                uppcl.configured -> "Connected"
                else -> "Not connected"
            },
            subtitleColor = when {
                uppcl.configured && uppcl.status == UppclStatus.ERROR -> colors.bad
                uppcl.configured -> colors.ok
                else -> colors.mute
            },
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Sign in with your UPPCL Smart Consumers account to show daily meter usage.",
            style = AppText.sub,
            color = colors.mute,
        )

        Spacer(Modifier.height(16.dp))
        FieldLabel("UPPCL username")
        Spacer(Modifier.height(6.dp))
        SettingsField(
            value = username,
            onValueChange = {
                username = it
                onClearTest()
            },
            placeholder = "Your UPPCL user name",
            keyboardType = KeyboardType.Ascii,
            imeAction = ImeAction.Next,
            onImeAction = { passwordFocus.requestFocus() },
        )

        Spacer(Modifier.height(12.dp))
        FieldLabel("UPPCL password")
        Spacer(Modifier.height(6.dp))
        SettingsField(
            value = password,
            onValueChange = {
                password = it
                onClearTest()
            },
            placeholder = "Your UPPCL password",
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Next,
            onImeAction = { accountFocus.requestFocus() },
            modifier = Modifier.focusRequester(passwordFocus),
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailing = {
                Text(
                    text = if (passwordVisible) "Hide" else "Show",
                    style = AppText.chip,
                    color = colors.grid,
                    modifier = Modifier
                        .padding(horizontal = 10.dp)
                        .tap { passwordVisible = !passwordVisible },
                )
            },
        )

        Spacer(Modifier.height(12.dp))
        FieldLabel("UPPCL account number")
        Spacer(Modifier.height(6.dp))
        SettingsField(
            value = accountNumber,
            onValueChange = {
                accountNumber = it
                onClearTest()
            },
            placeholder = "Account number",
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done,
            onImeAction = { save() },
            modifier = Modifier.focusRequester(accountFocus),
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = "Use the account number shown in the UPPCL app.",
            style = AppText.sub,
            color = colors.mute,
        )

        uppcl.testResult?.let { result ->
            Spacer(Modifier.height(14.dp))
            UppclTestResult(result)
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                GridButton(
                    text = if (uppcl.isTesting) "Checking…" else "Test",
                    onClick = {
                        focusManager.clearFocus()
                        onTest(username, password, accountNumber)
                    },
                    enabled = complete && !uppcl.isTesting,
                    loading = uppcl.isTesting,
                    primary = false,
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                GridButton(
                    text = "Save",
                    onClick = { save() },
                    enabled = complete && !uppcl.isTesting,
                    loading = uppcl.isTesting,
                    primary = true,
                )
            }
        }

        if (uppcl.configured) {
            Spacer(Modifier.height(5.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .tap { confirmDisconnect = true }
                    .padding(vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Disconnect UPPCL",
                    style = AppText.body.copy(fontWeight = FontWeight.Bold),
                    color = colors.bad,
                    modifier = Modifier.weight(1f),
                )
                Text(text = "Remove", style = AppText.button, color = colors.bad)
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            text = "Credentials are stored on this phone and sent only to uppcl.eeslsmartmeter.in. " +
                "Too many failed sign-ins can lock your UPPCL account.",
            style = AppText.sub,
            color = colors.mute.copy(alpha = 0.85f),
        )
    }

    if (confirmDisconnect) {
        ConfirmDialog(
            title = "Disconnect UPPCL?",
            message = "This removes the UPPCL username, password and account number from this phone. " +
                "Solar monitoring keeps working.",
            confirmLabel = "Disconnect",
            confirmColor = colors.bad,
            onConfirm = {
                onDisconnect()
                confirmDisconnect = false
            },
            onDismiss = { confirmDisconnect = false },
        )
    }
}

@Composable
private fun UppclTestResult(result: UppclConnectionTest) {
    val colors = LocalAppColors.current
    val success = result is UppclConnectionTest.Success
    val tint = if (success) colors.ok else colors.bad
    val title = if (success) "UPPCL connected" else "Couldn't connect"
    val message = when (result) {
        is UppclConnectionTest.Success -> {
            val who = result.username?.takeIf { it.isNotBlank() } ?: "Signed in"
            "$who · ${energyText(result.monthData.totalKwh)} this month"
        }
        is UppclConnectionTest.Failure -> result.message
    }
    val hint = (result as? UppclConnectionTest.Failure)?.let {
        when (it.kind) {
            UppclErrorKind.AUTH -> "Check your username and password. Repeated failures can " +
                "temporarily lock your UPPCL account."
            UppclErrorKind.NETWORK -> "Check your internet connection and try again."
            else -> null
        }
    }
    ResultBanner(
        success = success,
        title = title,
        message = message,
        tint = tint,
        hint = hint,
    )
}

@Composable
private fun SolidDivider() {
    val colors = LocalAppColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(colors.line),
    )
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    confirmColor: Color,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalAppColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.card,
        shape = RoundedCornerShape(26.dp),
        titleContentColor = colors.ink,
        textContentColor = colors.mute,
        title = { Text(text = title, style = AppText.value20) },
        text = { Text(text = message, style = AppText.body) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = confirmLabel, style = AppText.button, color = confirmColor)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Keep it", style = AppText.button, color = colors.ink)
            }
        },
    )
}
