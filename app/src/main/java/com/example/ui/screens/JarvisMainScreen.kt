package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.api.*
import com.example.data.db.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.ChatMessage
import com.example.ui.viewmodel.JarvisViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun JarvisMainScreen(viewModel: JarvisViewModel) {
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val activeTab by viewModel.currentScreen.collectAsStateWithLifecycle()

    // Determine viewport size class configuration for responsive adaptation
    val configuration = LocalConfiguration.current
    val isExpanded = configuration.screenWidthDp >= 600

    MyApplicationTheme(darkTheme = isDarkTheme) {
        val backgroundGradient = Brush.verticalGradient(
            colors = if (isDarkTheme) {
                listOf(SynthDark, HologramSlate.copy(alpha = 0.8f))
            } else {
                listOf(Color(0xFFE2F1F4), Color(0xFFF1F5F7))
            }
        )

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind { drawRect(brush = backgroundGradient) }
                .testTag("app_scaffold"),
            bottomBar = {
                if (!isExpanded) {
                    JarvisBottomNavigationBar(
                        activeTab = activeTab,
                        onTabSelected = { viewModel.navigateTo(it) }
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpanded) {
                    JarvisSideNavigationRail(
                        activeTab = activeTab,
                        onTabSelected = { viewModel.navigateTo(it) }
                    )
                    VerticalDivider(color = GridLineCyan)
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    AnimatedContent(
                        targetState = activeTab,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                        },
                        label = "tab_navigation"
                    ) { tab ->
                        when (tab) {
                            "home" -> HomeView(viewModel)
                            "pinterest" -> PinterestWorkflowView(viewModel)
                            "tasks" -> TasksWorkflowsView(viewModel)
                            "memory" -> MemoryView(viewModel)
                            "agents" -> AgentsView(viewModel)
                            "browser" -> BrowserView(viewModel)
                            "files" -> FilesView(viewModel)
                            "settings" -> SettingsView(viewModel)
                            else -> HomeView(viewModel)
                        }
                    }
                }
            }
        }
    }
}

// Helper Composable to convert Base64 string to a Bitmap
@Composable
fun rememberBase64Image(base64Str: String): Bitmap? {
    return remember(base64Str) {
        if (base64Str.isEmpty() || base64Str == "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mPsb26uBwAEhAF0j1Y+cQAAAABJRU5ErkJggg==") {
            null
        } else {
            try {
                val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            } catch (e: Exception) {
                null
            }
        }
    }
}

// --- STANDARD NAVIGATION COMPONENTS ---

@Composable
fun JarvisBottomNavigationBar(activeTab: String, onTabSelected: (String) -> Unit) {
    NavigationBar(
        containerColor = HologramSlate.copy(alpha = 0.95f),
        tonalElevation = 8.dp,
        modifier = Modifier.testTag("bottom_nav_bar")
    ) {
        val items = listOf(
            NavigationItem("home", "Home", Icons.Default.Hub, Icons.Outlined.Hub),
            NavigationItem("pinterest", "Pinterest", Icons.Default.Campaign, Icons.Outlined.Campaign),
            NavigationItem("tasks", "Tasks", Icons.Default.Task, Icons.Outlined.Task),
            NavigationItem("memory", "Memory", Icons.Default.Memory, Icons.Outlined.Memory),
            NavigationItem("agents", "Agents", Icons.Default.Psychology, Icons.Outlined.Psychology)
        )

        items.forEach { item ->
            NavigationBarItem(
                selected = activeTab == item.route,
                onClick = { onTabSelected(item.route) },
                icon = {
                    Icon(
                        imageVector = if (activeTab == item.route) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                        tint = if (activeTab == item.route) CyberCyan else TextCyan
                    )
                },
                label = {
                    Text(
                        item.label,
                        color = if (activeTab == item.route) CyberCyan else MutedSlate,
                        fontSize = 11.sp,
                        fontWeight = if (activeTab == item.route) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
        }

        // Quick dropdown for secondary tabs
        NavigationBarItem(
            selected = activeTab in listOf("browser", "files", "settings"),
            onClick = { onTabSelected("settings") },
            icon = {
                Icon(
                    imageVector = Icons.Default.MoreHoriz,
                    contentDescription = "System Utilities",
                    tint = TextCyan
                )
            },
            label = { Text("System", color = MutedSlate, fontSize = 11.sp) }
        )
    }
}

@Composable
fun JarvisSideNavigationRail(activeTab: String, onTabSelected: (String) -> Unit) {
    NavigationRail(
        containerColor = HologramSlate.copy(alpha = 0.95f),
        header = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = CyberCyan.copy(alpha = 0.15f),
                    border = BorderStroke(1.5.dp, CyberCyan),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "JARVIS Logo",
                            tint = CyberCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "J.A.R.V.I.S.",
                    fontWeight = FontWeight.ExtraBold,
                    color = CyberCyan,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    "OS",
                    color = TextCyan,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        },
        modifier = Modifier.testTag("side_nav_rail")
    ) {
        val items = listOf(
            NavigationItem("home", "Home", Icons.Default.Hub, Icons.Outlined.Hub),
            NavigationItem("pinterest", "Pinterest", Icons.Default.Campaign, Icons.Outlined.Campaign),
            NavigationItem("tasks", "Tasks", Icons.Default.Task, Icons.Outlined.Task),
            NavigationItem("memory", "Memory", Icons.Default.Memory, Icons.Outlined.Memory),
            NavigationItem("agents", "Agents", Icons.Default.Psychology, Icons.Outlined.Psychology),
            NavigationItem("browser", "Browser", Icons.Default.Language, Icons.Outlined.Language),
            NavigationItem("files", "Files", Icons.Default.FolderOpen, Icons.Outlined.FolderOpen),
            NavigationItem("settings", "System Settings", Icons.Default.Settings, Icons.Outlined.Settings)
        )

        Spacer(modifier = Modifier.weight(1f))
        items.forEach { item ->
            NavigationRailItem(
                selected = activeTab == item.route,
                onClick = { onTabSelected(item.route) },
                icon = {
                    Icon(
                        imageVector = if (activeTab == item.route) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                        tint = if (activeTab == item.route) CyberCyan else TextCyan
                    )
                },
                label = {
                    Text(
                        item.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (activeTab == item.route) CyberCyan else MutedSlate,
                        fontSize = 11.sp,
                        fontWeight = if (activeTab == item.route) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

data class NavigationItem(
    val route: String,
    val label: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector
)

// --- REUSABLE SHIELD / CARD STYLES ---

@Composable
fun CyberTechCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    titleColor: Color = CyberCyan,
    borderColor: Color = GridLineCyan,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = HologramSlate.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (title != null) {
                Text(
                    text = title.uppercase(),
                    color = titleColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = GridLineCyan)
                Spacer(modifier = Modifier.height(12.dp))
            }
            content()
        }
    }
}

@Composable
fun ApiKeyWarningBanner(viewModel: JarvisViewModel) {
    if (!viewModel.isApiKeyConfigured) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .border(BorderStroke(1.5.dp, OrangeAlert), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = OrangeAlert.copy(alpha = 0.08f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Sandbox Simulation active",
                    tint = OrangeAlert,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "SANDBOX SIMULATION MODE ACTIVE",
                        fontWeight = FontWeight.Bold,
                        color = OrangeAlert,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "No custom GEMINI_API_KEY detected in build environments. Go to the Secrets panel in AI Studio to set your key to unlock live neural models.",
                        color = Color.White,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}

// --- 1. HOME SCREEN VIEW ---

@Composable
fun HomeView(viewModel: JarvisViewModel) {
    val chatHistory by viewModel.chatHistory.collectAsStateWithLifecycle()
    val isGen by viewModel.isGenerating.collectAsStateWithLifecycle()
    val brainLog by viewModel.routingLog.collectAsStateWithLifecycle()
    val overrideModel by viewModel.selectedModelOverride.collectAsStateWithLifecycle()
    val isWaking by viewModel.wakeupToggle.collectAsStateWithLifecycle()
    val voiceStateVal by viewModel.voiceState.collectAsStateWithLifecycle()
    val batteryText by viewModel.batterySaverStats.collectAsStateWithLifecycle()

    val keyboardController = LocalSoftwareKeyboardController.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ApiKeyWarningBanner(viewModel)
        }

        // Hologram Status Orb Panel with Dynamic Animation
        item {
            val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.95f,
                targetValue = 1.05f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "orb_pulse_scale"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CyberTechCard(
                    modifier = Modifier.weight(1.5f),
                    title = "Autonomous System Core"
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        // The Orb Action Interface
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    if (voiceStateVal == "Idle") viewModel.startVoiceListening()
                                }
                                .padding(8.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                // Background cyan radial glows
                                Canvas(modifier = Modifier.size(100.dp)) {
                                    drawCircle(
                                        color = when (voiceStateVal) {
                                            "Listening..." -> LaserRed.copy(alpha = 0.2f)
                                            "Processing query..." -> OrangeAlert.copy(alpha = 0.2f)
                                            "Speaking..." -> NeonGreen.copy(alpha = 0.2f)
                                            else -> CyberCyan.copy(alpha = 0.15f)
                                        },
                                        radius = 48.dp.toPx() * scale
                                    )
                                }
                                Surface(
                                    shape = CircleShape,
                                    border = BorderStroke(
                                        2.dp, when (voiceStateVal) {
                                            "Listening..." -> LaserRed
                                            "Processing query..." -> OrangeAlert
                                            "Speaking..." -> NeonGreen
                                            else -> CyberCyan
                                        }
                                    ),
                                    color = HologramSlate.copy(alpha = 0.8f),
                                    modifier = Modifier.size(72.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = when (voiceStateVal) {
                                                "Listening..." -> Icons.Default.Mic
                                                "Processing query..." -> Icons.Default.SettingsSuggest
                                                "Speaking..." -> Icons.Default.VolumeUp
                                                else -> Icons.Default.Psychology
                                            },
                                            contentDescription = "Voice assistant orb",
                                            tint = when (voiceStateVal) {
                                                "Listening..." -> LaserRed
                                                "Processing query..." -> OrangeAlert
                                                "Speaking..." -> NeonGreen
                                                else -> CyberCyan
                                            },
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "ORB STATUS: ${voiceStateVal.uppercase()}",
                                fontWeight = FontWeight.ExtraBold,
                                color = if (voiceStateVal == "Idle") CyberCyan else TextCyan,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "Tap Orb to command by voice or summon Wake Word simulator",
                                color = MutedSlate,
                                fontSize = 9.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                }

                // System Specs & Router Toggle Panel
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CyberTechCard(title = "Auto Cognitive Router") {
                        Text(
                            "Brain Model Mode",
                            color = MutedSlate,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val nextOpt = when (overrideModel) {
                                        "Auto-Route" -> "Gemini Pro"
                                        "Gemini Pro" -> "Gemini Flash"
                                        else -> "Auto-Route"
                                    }
                                    viewModel.selectedModelOverride.value = nextOpt
                                }
                                .border(1.dp, CyberCyan.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                overrideModel.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan,
                                fontFamily = FontFamily.Monospace
                            )
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = "Cycle AI models",
                                tint = CyberCyan,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    CyberTechCard(title = "Hardware Overhead") {
                        Text(
                            batteryText,
                            fontSize = 9.sp,
                            color = TextCyan,
                            lineHeight = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Wake Word listening", fontSize = 9.sp, color = MutedSlate)
                            Switch(
                                checked = isWaking,
                                onCheckedChange = { viewModel.wakeupToggle.value = it },
                                modifier = Modifier.scale(0.6f),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = CyberCyan,
                                    checkedTrackColor = CyberCyan.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }
                }
            }
        }

        // Active Chat Terminal Panel
        item {
            CyberTechCard(title = "Decentralized Terminal Core - Interactive") {
                Text(
                    text = brainLog.uppercase(),
                    color = TextCyan,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SynthDark, RoundedCornerShape(4.dp))
                        .padding(8.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(SynthDark.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                        .border(1.dp, GridLineCyan, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    if (chatHistory.isEmpty()) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text("No telemetry logs loaded in terminal drawer.", color = MutedSlate, fontSize = 12.sp)
                        }
                    } else {
                        LazyColumn(
                            reverseLayout = true,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(chatHistory.reversed()) { chat ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            chat.sender.uppercase(),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (chat.sender == "User") CyberCyan else LaserRed,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(chat.timestamp)),
                                            fontSize = 8.sp,
                                            color = MutedSlate,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        chat.message,
                                        fontSize = 11.sp,
                                        color = if (chat.sender == "User") Color.White else TextCyan,
                                        lineHeight = 15.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    HorizontalDivider(color = GridLineCyan.copy(alpha = 0.1f))
                                }
                            }
                        }
                    }
                    if (isGen) {
                        CircularProgressIndicator(
                            color = CyberCyan,
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.TopEnd)
                                .padding(4.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val customValueState = viewModel.chatInput.collectAsStateWithLifecycle()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customValueState.value,
                        onValueChange = { viewModel.chatInput.value = it },
                        placeholder = { Text("Command JARVIS AI Agent core...", fontSize = 11.sp, color = MutedSlate) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_text_field"),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            viewModel.sendMessage()
                            keyboardController?.hide()
                        }),
                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = GridLineCyan,
                            focusedContainerColor = SynthDark,
                            unfocusedContainerColor = SynthDark
                        )
                    )

                    IconButton(
                        onClick = {
                            viewModel.sendMessage()
                            keyboardController?.hide()
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .border(1.dp, CyberCyan, CircleShape)
                            .background(CyberCyan.copy(alpha = 0.15f), CircleShape)
                            .testTag("send_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Send action command",
                            tint = CyberCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Button(
                        onClick = { viewModel.clearChat() },
                        colors = ButtonDefaults.buttonColors(containerColor = LaserRed.copy(alpha = 0.15f)),
                        modifier = Modifier.border(1.dp, LaserRed.copy(alpha = 0.6f), RoundedCornerShape(4.dp)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("CLEAR", color = LaserRed, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

// --- 2. PROJECTS (PINTEREST AUTONOMOUS SYSTEM WORKFLOW) ---

@Composable
fun PinterestWorkflowView(viewModel: JarvisViewModel) {
    val step by viewModel.pinterestStep.collectAsStateWithLifecycle()
    val isWorking by viewModel.isPinterestWorking.collectAsStateWithLifecycle()
    val nicheInput = viewModel.pinterestNiche.collectAsStateWithLifecycle()
    val userBrandNameInput = viewModel.pinterestBrandingName.collectAsStateWithLifecycle()

    // Response states loaded from ViewModel
    val bioText by viewModel.pinterestBioText.collectAsStateWithLifecycle()
    val profileImgB64 by viewModel.pinterestProfileImageB64.collectAsStateWithLifecycle()
    val marketingPlan by viewModel.pinterestStrategyPlan.collectAsStateWithLifecycle()
    val suggestedPrompt by viewModel.pinterestSuggestedImageTheme.collectAsStateWithLifecycle()
    val genPinImgB64 by viewModel.pinterestGeneratedPinImageB64.collectAsStateWithLifecycle()
    val pinTitle by viewModel.pinterestRunningPinTitle.collectAsStateWithLifecycle()
    val pinDesc by viewModel.pinterestRunningPinDesc.collectAsStateWithLifecycle()
    val pinTags by viewModel.pinterestRunningPinTags.collectAsStateWithLifecycle()

    val logStream by viewModel.pinterestSessionLogs.collectAsStateWithLifecycle()
    val savedPinsList by viewModel.pinterestPins.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ApiKeyWarningBanner(viewModel)
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Autonomous marketing engine".uppercase(),
                        color = CyberCyan,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Pinterest AI campaign manager workflow", color = TextCyan, fontSize = 11.sp)
                }
                IconButton(
                    onClick = { viewModel.resetPinterestStep() },
                    modifier = Modifier.background(LaserRed.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset campaign steps", tint = LaserRed)
                }
            }
        }

        // Stepper Visual progress tracker
        item {
            Row(
                modifier = Modifier
                    .fillHorizontalFlow(0.dp)
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (i in 1..8) {
                    val isActive = step == i
                    val isCompleted = step > i
                    val stepLabels = listOf("Auth", "Brand", "Plan", "Prompt", "Creative", "Meta", "Schedule", "Analytics")
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = when {
                            isActive -> CyberCyan
                            isCompleted -> NeonGreen.copy(alpha = 0.2f)
                            else -> HologramSlate.copy(alpha = 0.4f)
                        },
                        border = BorderStroke(
                            1.dp, when {
                                isActive -> CyberCyan
                                isCompleted -> NeonGreen
                                else -> GridLineCyan
                            }
                        ),
                        modifier = Modifier.padding(horizontal = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "0$i",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isActive) SynthDark else TextCyan,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                stepLabels[i - 1],
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isActive) SynthDark else Color.White
                            )
                        }
                    }
                }
            }
        }

        // Wizard Active Module Card
        item {
            CyberTechCard(
                title = "Campaign step $step instruction",
                modifier = Modifier.testTag("step_card")
            ) {
                if (isWorking) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = CyberCyan)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "JARVIS is thinking... generating assets using active LLM brain...",
                                color = TextCyan,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                } else {
                    when (step) {
                        1 -> {
                            Text(
                                "STEP 1: Pinterest Platform Integration Access",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = TextCyan
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "JARVIS requires a restricted API OAuth authorization token to handle boards configuration and campaign queues according to regulations.",
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.authorizePinterest() },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = Icons.Default.VpnKey, contentDescription = "OAuth2 lock", tint = SynthDark)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("INTEGRATE PINTEREST PROFILE", color = SynthDark, fontWeight = FontWeight.Black)
                            }
                        }

                        2 -> {
                            Text(
                                "STEP 2: Brand Identity Formulation",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = TextCyan
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Input your target artistic niche coordinates. JARVIS will use Gemini to formulate profile bio copywriting proposals and render custom branding icons.",
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = userBrandNameInput.value,
                                onValueChange = { viewModel.pinterestBrandingName.value = it },
                                label = { Text("Desired Username / Brand", fontSize = 11.sp, color = MutedSlate) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, unfocusedBorderColor = GridLineCyan)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = nicheInput.value,
                                onValueChange = { viewModel.pinterestNiche.value = it },
                                label = { Text("Design Niche details", fontSize = 11.sp, color = MutedSlate) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, unfocusedBorderColor = GridLineCyan)
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.submitPinterestBranding() },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("GENERATE BIO & AVATAR GRAPHIC", color = SynthDark, fontWeight = FontWeight.Bold)
                            }
                        }

                        3 -> {
                            Text(
                                "STEP 3: Identity output proposed details",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = TextCyan
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Dynamic Render of Avatar Image inside high resolution card
                                val avatarBmp = rememberBase64Image(profileImgB64)
                                if (avatarBmp != null) {
                                    Image(
                                        bitmap = avatarBmp.asImageBitmap(),
                                        contentDescription = "AI Generated Profile Logo",
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(CircleShape)
                                            .border(2.dp, CyberCyan, CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(CircleShape)
                                            .border(2.dp, CyberCyan, CircleShape)
                                            .background(SynthDark),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(imageVector = Icons.Default.Face, contentDescription = "Fallback profile avatar icon", tint = CyberCyan, modifier = Modifier.size(40.dp))
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text("@${userBrandNameInput.value}", fontWeight = FontWeight.Bold, color = CyberCyan, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        bioText,
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp,
                                        color = Color.White
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.buildPinterestContentPlan() },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("COMPILE 7-DAY CONTENT CALENDAR", color = SynthDark, fontWeight = FontWeight.Bold)
                            }
                        }

                        4 -> {
                            Text(
                                "STEP 4: Strategy calendar outline output",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = TextCyan
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .background(SynthDark, RoundedCornerShape(4.dp))
                                    .border(1.dp, GridLineCyan, RoundedCornerShape(4.dp))
                                    .verticalScroll(rememberScrollState())
                                    .padding(8.dp)
                            ) {
                                Text(
                                    marketingPlan,
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Suggested visual painting visual prompt style:", fontSize = 10.sp, color = MutedSlate)
                            OutlinedTextField(
                                value = suggestedPrompt,
                                onValueChange = { viewModel.pinterestSuggestedImageTheme.value = it },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = LocalTextStyle.current.copy(fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.White),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, unfocusedBorderColor = GridLineCyan)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.pinterestStep.value = 5 },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("APPROVE CALENDAR & ADVANCE", color = SynthDark, fontWeight = FontWeight.Bold)
                            }
                        }

                        5 -> {
                            Text(
                                "STEP 5: Generate dynamic creative Pin Artwork",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = TextCyan
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "JARVIS is connecting to the Image diffusion pipeline models to generate the final campaign board artifact asset.",
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Input Image Prompt:",
                                fontSize = 10.sp,
                                color = CyberCyan,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                suggestedPrompt,
                                fontSize = 11.sp,
                                color = Color.LightGray,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.generatePinterestPinImage() },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("CREATE CAMPAIGN AI PAINTING", color = SynthDark, fontWeight = FontWeight.Bold)
                            }
                        }

                        6 -> {
                            Text(
                                "STEP 6: AI pin image asset preview",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = TextCyan
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val pinBmp = rememberBase64Image(genPinImgB64)
                                if (pinBmp != null) {
                                    Image(
                                        bitmap = pinBmp.asImageBitmap(),
                                        contentDescription = "Dynamic Pin Artwork",
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(1.5.dp, CyberCyan, RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    // Custom visual stylized geometric background representing placeholder cyberpunk image
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .aspectRatio(1f)
                                            .background(
                                                Brush.sweepGradient(
                                                    listOf(
                                                        CyberCyan,
                                                        HologramSlate,
                                                        LaserRed,
                                                        CyberCyan
                                                    )
                                                ), RoundedCornerShape(8.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "PROTOTYPE\nHOLOGRAPHIC",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp,
                                            color = Color.White,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.generatePinterestDraftMeta() },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("DRAFT PIN TITLE & SEO TAGS", color = SynthDark, fontWeight = FontWeight.Bold)
                            }
                        }

                        7 -> {
                            Text(
                                "STEP 7: Formulate social metadata descriptions",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = TextCyan
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = pinTitle,
                                onValueChange = { viewModel.pinterestRunningPinTitle.value = it },
                                label = { Text("Pin Title", fontSize = 10.sp, color = MutedSlate) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, unfocusedBorderColor = GridLineCyan)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = pinDesc,
                                onValueChange = { viewModel.pinterestRunningPinDesc.value = it },
                                label = { Text("SEO Keyword Description", fontSize = 10.sp, color = MutedSlate) },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 3,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, unfocusedBorderColor = GridLineCyan)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = pinTags,
                                onValueChange = { viewModel.pinterestRunningPinTags.value = it },
                                label = { Text("Suggested hashtags items", fontSize = 10.sp, color = MutedSlate) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, unfocusedBorderColor = GridLineCyan)
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.schedulePinterestPin() },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("COMMIT & SCHEDULE LIVE POSTING", color = SynthDark, fontWeight = FontWeight.Black)
                            }
                        }

                        8 -> {
                            Text(
                                "STEP 8: Automated Scheduling successful logs",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = NeonGreen
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "JARVIS scheduled task configured into the engine core successfully. Performance analytics will log clicks below interactively.",
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // Interactive Analytics Canvas Graphic Component
                            Text(
                                "Interactive Analytics Analytics Canvas:".uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .border(BorderStroke(1.dp, GridLineCyan), RoundedCornerShape(8.dp))
                                    .background(SynthDark, RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val w = size.width
                                    val h = size.height

                                    // Draw cyber grid
                                    val cols = 6
                                    for (co in 1..cols) {
                                        val x = w * (co.toFloat() / (cols + 1))
                                        drawLine(
                                            color = GridLineCyan.copy(alpha = 0.5f),
                                            start = Offset(x, 0f),
                                            end = Offset(x, h),
                                            strokeWidth = 1f
                                        )
                                    }

                                    // Custom graphic path line for impressions
                                    val yPointsImp = listOf(h * 0.8f, h * 0.65f, h * 0.45f, h * 0.52f, h * 0.3f, h * 0.15f)
                                    val yPointsClick = listOf(h * 0.9f, h * 0.82f, h * 0.7f, h * 0.78f, h * 0.6f, h * 0.42f)

                                    for (index in 0..4) {
                                        val startX = w * (index.toFloat() / 5)
                                        val endX = w * ((index + 1).toFloat() / 5)

                                        // Draw Impressions Line (Teal/Cyan)
                                        drawLine(
                                            color = CyberCyan,
                                            start = Offset(startX, yPointsImp[index]),
                                            end = Offset(endX, yPointsImp[index + 1]),
                                            strokeWidth = 3f
                                        )

                                        // Draw Clicks Line (Red glowing)
                                        drawLine(
                                            color = LaserRed,
                                            start = Offset(startX, yPointsClick[index]),
                                            end = Offset(endX, yPointsClick[index + 1]),
                                            strokeWidth = 2.5f
                                        )

                                        // Glow point points
                                        drawCircle(
                                            color = CyberCyan,
                                            center = Offset(endX, yPointsImp[index + 1]),
                                            radius = 4f
                                        )
                                        drawCircle(
                                            color = LaserRed,
                                            center = Offset(endX, yPointsClick[index + 1]),
                                            radius = 3.5f
                                        )
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(CyberCyan, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Campaign Impressions", fontSize = 8.sp, color = TextCyan)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(LaserRed, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Profile click-throughs", fontSize = 8.sp, color = LaserRed)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Live Campaign Workflow logs console
        item {
            CyberTechCard(title = "JARVIS Campaign logs telemetry") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .background(SynthDark, RoundedCornerShape(4.dp))
                        .padding(8.dp)
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(logStream) { logLine ->
                            Text(
                                logLine,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (logLine.contains("Success|Auth|OK|active", ignoreCase = true)) NeonGreen else TextCyan,
                                lineHeight = 12.sp,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Local Persistence Database records listings
        item {
            Text(
                "Scheduled Pinterest items inside Local SQLite Data:".uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CyberCyan,
                fontFamily = FontFamily.Monospace
            )
        }

        if (savedPinsList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No local Campaign posts stored in SQL database.", color = MutedSlate, fontSize = 11.sp)
                }
            }
        } else {
            items(savedPinsList) { p ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = HologramSlate),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val pinBmp = rememberBase64Image(p.imageBase64)
                        if (pinBmp != null) {
                            Image(
                                bitmap = pinBmp.asImageBitmap(),
                                contentDescription = "Pin Thumbnail",
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .background(SynthDark, RoundedCornerShape(4.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.Campaign, contentDescription = "Post logo icon", tint = CyberCyan)
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(p.title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(p.description, color = TextCyan, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(p.scheduledTime, fontSize = 8.sp, color = MutedSlate, fontFamily = FontFamily.Monospace)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("STATUS", fontSize = 8.sp, color = MutedSlate)
                            Text(
                                p.status.uppercase(),
                                fontWeight = FontWeight.ExtraBold,
                                color = NeonGreen,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- 3. TASKS & WORKFLOWS VIEW ---

@Composable
fun TasksWorkflowsView(viewModel: JarvisViewModel) {
    val workflowsList by viewModel.workflows.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val currentTitle = viewModel.workflowGenTitle.collectAsStateWithLifecycle()
    val currentPrompt = viewModel.workflowGenPrompt.collectAsStateWithLifecycle()
    val currentSched = viewModel.workflowGenSchedule.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ApiKeyWarningBanner(viewModel)
        }

        item {
            Column {
                Text(
                    "Mobile Automation Engine".uppercase(),
                    color = CyberCyan,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("Compile autonomous schedule rules with natural language prompts", color = TextCyan, fontSize = 11.sp)
            }
        }

        // Add custom automation schedule container
        item {
            CyberTechCard(title = "Draft custom autonomous workflow script") {
                OutlinedTextField(
                    value = currentTitle.value,
                    onValueChange = { viewModel.workflowGenTitle.value = it },
                    placeholder = { Text("Post schedule title (e.g. daily blog poster)", fontSize = 11.sp, color = MutedSlate) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("workflow_title_input"),
                    textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, unfocusedBorderColor = GridLineCyan)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = currentPrompt.value,
                    onValueChange = { viewModel.workflowGenPrompt.value = it },
                    placeholder = { Text("What actions should JARVIS execute? (e.g. Research science topic, compile post card, run vision summaries...)", fontSize = 11.sp, color = MutedSlate) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .testTag("workflow_prompt_input"),
                    textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, unfocusedBorderColor = GridLineCyan)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = currentSched.value,
                    onValueChange = { viewModel.workflowGenSchedule.value = it },
                    placeholder = { Text("Frequency schedule (e.g., Every hour, Daily at 09:00 AM)", fontSize = 11.sp, color = MutedSlate) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("workflow_schedule_input"),
                    textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, unfocusedBorderColor = GridLineCyan)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.addCustomWorkflow() },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Append schedule action icon", tint = SynthDark)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("COMPILE WORKFLOW PLAN", color = SynthDark, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Workflows persistence list
        item {
            Text(
                "Configured Active Schedules:".uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CyberCyan,
                fontFamily = FontFamily.Monospace
            )
        }

        if (workflowsList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No schedules loaded in local configuration.", color = MutedSlate, fontSize = 11.sp)
                }
            }
        } else {
            items(workflowsList) { work ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = HologramSlate.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (work.isActive) CyberCyan.copy(alpha = 0.4f) else GridLineCyan.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EventRepeat,
                                    contentDescription = "Event repeat schedule logo",
                                    tint = if (work.isActive) CyberCyan else MutedSlate,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    work.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (work.isActive) Color.White else MutedSlate
                                )
                            }

                            Switch(
                                checked = work.isActive,
                                onCheckedChange = { viewModel.toggleWorkflowActive(work) },
                                modifier = Modifier.scale(0.7f),
                                colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            work.prompt,
                            fontSize = 11.sp,
                            color = if (work.isActive) TextCyan else MutedSlate,
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = GridLineCyan.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "INTERVAL: ${work.scheduledTime.uppercase()}",
                                fontSize = 9.sp,
                                color = MutedSlate,
                                fontFamily = FontFamily.Monospace
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { viewModel.triggerWorkflowExecution(work) },
                                    enabled = work.isActive && work.lastRunStatus != "Running",
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan.copy(alpha = 0.2f)),
                                    modifier = Modifier
                                        .height(30.dp)
                                        .border(1.dp, CyberCyan, RoundedCornerShape(4.dp)),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text(
                                        text = if (work.lastRunStatus == "Running") "RUNNING..." else "RUN NOW",
                                        color = CyberCyan,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.deleteWorkflow(work) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteForever,
                                        contentDescription = "Purge automation script",
                                        tint = LaserRed
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- 4. MEMORY MANAGER VIEW ---

@Composable
fun MemoryView(viewModel: JarvisViewModel) {
    val memoryList by viewModel.memories.collectAsStateWithLifecycle()
    val searchVal by viewModel.searchMemoryQuery.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val newKey = viewModel.newMemoryKey.collectAsStateWithLifecycle()
    val newVal = viewModel.newMemoryValue.collectAsStateWithLifecycle()
    val newCat = viewModel.newMemoryCategory.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ApiKeyWarningBanner(viewModel)
        }

        item {
            Column {
                Text(
                    "Long-term memory vault".uppercase(),
                    color = CyberCyan,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("Search, add and manage cognitive credentials preferences", color = TextCyan, fontSize = 11.sp)
            }
        }

        // Add memory card container
        item {
            CyberTechCard(title = "Ingest new memory node state") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newKey.value,
                        onValueChange = { viewModel.newMemoryKey.value = it },
                        placeholder = { Text("Memory Key/Topic", fontSize = 11.sp, color = MutedSlate) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("memory_key_input"),
                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, unfocusedBorderColor = GridLineCyan)
                    )

                    OutlinedTextField(
                        value = newVal.value,
                        onValueChange = { viewModel.newMemoryValue.value = it },
                        placeholder = { Text("Preference details", fontSize = 11.sp, color = MutedSlate) },
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("memory_value_input"),
                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, unfocusedBorderColor = GridLineCyan)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Memory Category:", fontSize = 10.sp, color = MutedSlate)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("Preference", "Business", "Project", "Style").forEach { c ->
                            val isSelected = newCat.value == c
                            Button(
                                onClick = { viewModel.newMemoryCategory.value = c },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) CyberCyan else HologramSlate),
                                modifier = Modifier.border(1.dp, if (isSelected) CyberCyan else GridLineCyan, RoundedCornerShape(4.dp)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(c.uppercase(), fontSize = 8.sp, color = if (isSelected) SynthDark else Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.addManualMemory() },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("INJECT Memory Row", color = SynthDark, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Memory searchable list container
        item {
            OutlinedTextField(
                value = searchVal,
                onValueChange = { viewModel.searchMemoryQuery.value = it },
                label = { Text("Filter long term memory vault...", fontSize = 11.sp, color = CyberCyan) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("memory_search_bar"),
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Query memory icons", tint = CyberCyan) },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, unfocusedBorderColor = GridLineCyan)
            )
        }

        val filteredMemories = memoryList.filter {
            it.key.contains(searchVal, ignoreCase = true) ||
                it.value.contains(searchVal, ignoreCase = true) ||
                it.category.contains(searchVal, ignoreCase = true)
        }

        if (filteredMemories.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No matching memory structures retrieved in search.", color = MutedSlate, fontSize = 11.sp)
                }
            }
        } else {
            items(filteredMemories) { m ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = HologramSlate),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(14.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    m.key.uppercase(),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp,
                                    color = CyberCyan,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .border(0.5.dp, TextCyan, RoundedCornerShape(4.dp))
                                        .background(TextCyan.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(m.category.uppercase(), fontSize = 8.sp, color = TextCyan)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(m.value, color = Color.White, fontSize = 12.sp)
                        }

                        IconButton(onClick = {
                            scope.launch { viewModel.repository.deleteMemoryById(m.id) }
                        }) {
                            Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Clear selected memory index details", tint = LaserRed)
                        }
                    }
                }
            }
        }
    }
}

// --- 5. SPECIALIZED SUBAGENT DRAWER ---

@Composable
fun AgentsView(viewModel: JarvisViewModel) {
    val activeQuery = remember { mutableStateOf("") }
    val agentResp = remember { mutableStateOf("") }
    val isWorking = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val agents = listOf(
        AgentCardData("Research Agent", "Investigates niche parameters and aggregates marketing trends.", "Research Expert prompt", Icons.Default.ManageSearch),
        AgentCardData("Content Agent", "Drafts captions, hashtags SEO copy templates.", "Copywriting wizard", Icons.Default.Edit),
        AgentCardData("Coding Agent", "Writes safe compiled code and debug scripts.", "Code developer sandbox", Icons.Default.Code),
        AgentCardData("Marketing Agent", "Devises strategies to coordinate outreach channels.", "Advertising planner", Icons.Default.Campaign),
        AgentCardData("Design Agent", "Provides directions for creative image diffusion renders.", "Visual concept suggest", Icons.Default.Palette),
        AgentCardData("Business Agent", "Optimizes workloads and coordinates scheduler queues.", "Operations coordinator", Icons.Default.BusinessCenter)
    )

    val pickedAgent = remember { mutableStateOf(agents.first()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ApiKeyWarningBanner(viewModel)
        }

        item {
            Column {
                Text(
                    "Autonomous sub-agent vault".uppercase(),
                    color = CyberCyan,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("Command specialized JARVIS neural subdivisions directly", color = TextCyan, fontSize = 11.sp)
            }
        }

        // Grid layout representation of agents card
        item {
            Row(
                modifier = Modifier
                    .fillHorizontalFlow(0.dp)
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                agents.forEach { a ->
                    val isPicked = pickedAgent.value.name == a.name
                    Card(
                        modifier = Modifier
                            .width(130.dp)
                            .clickable { pickedAgent.value = a }
                            .border(
                                BorderStroke(
                                    1.2.dp,
                                    if (isPicked) CyberCyan else GridLineCyan
                                ), RoundedCornerShape(8.dp)
                            ),
                        colors = CardDefaults.cardColors(containerColor = if (isPicked) CyberCyan.copy(alpha = 0.15f) else HologramSlate.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = a.icon,
                                contentDescription = "Agent portrait",
                                tint = if (isPicked) CyberCyan else TextCyan,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                a.name.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPicked) CyberCyan else Color.White,
                                textAlign = TextAlign.Center,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Active selected Agent prompt block
        item {
            CyberTechCard(title = "Command Panel: ${pickedAgent.value.name.uppercase()}") {
                Text(
                    pickedAgent.value.desc,
                    fontSize = 11.sp,
                    color = TextCyan,
                    lineHeight = 15.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = activeQuery.value,
                    onValueChange = { activeQuery.value = it },
                    placeholder = { Text("Draft active query prompt details of this subagent...", fontSize = 11.sp, color = MutedSlate) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("agent_prompt_input"),
                    textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, unfocusedBorderColor = GridLineCyan)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val input = activeQuery.value.trim()
                        if (input.isNotEmpty()) {
                            isWorking.value = true
                            // Run coroutine directly bound to Compose coroutine scope
                            scope.launch {
                                val systemInstruction = "You are JARVIS specialize agent subdivision: [${pickedAgent.value.name}]. Instructions: ${pickedAgent.value.desc}."
                                val responseText = if (viewModel.isApiKeyConfigured) {
                                    val requestBody = GenerateContentRequestDto(
                                        contents = listOf(ContentDto(parts = listOf(PartDto(text = input)))),
                                        systemInstruction = ContentDto(parts = listOf(PartDto(text = systemInstruction)))
                                    )
                                    try {
                                        val modelParam = if (pickedAgent.value.name == "Coding Agent") "gemini-3.1-pro-preview" else "gemini-3.5-flash"
                                        val resp = RetrofitClient.service.generateContent(modelParam, viewModel.currentApiKey, requestBody)
                                        resp.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Null text"
                                    } catch (e: Exception) {
                                        "Error from agent loop: ${e.message}"
                                    }
                                } else {
                                    // Simulation delay and fallback response
                                    withContext(Dispatchers.IO) { Thread.sleep(1200) }
                                    "JARVIS Sub-Agent simulator: Compiled research output for prompt '$input'. Proceeding autonomously to formulate analytics assets."
                                }

                                agentResp.value = responseText
                                isWorking.value = false
                                viewModel.repository.logAction("Agent Direct Call", "Commanded specialised agent: ${pickedAgent.value.name}", "SUCCESS")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isWorking.value) {
                        CircularProgressIndicator(color = SynthDark, modifier = Modifier.size(20.dp))
                    } else {
                        Text("DELEGATE TASK TO AGENT", color = SynthDark, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Responses Console card
        if (agentResp.value.isNotEmpty() || isWorking.value) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberCyan, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = SynthDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Agent stream data pipeline:".uppercase(),
                            color = LaserRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            agentResp.value,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

data class AgentCardData(
    val name: String,
    val desc: String,
    val text: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

// --- 6. INTELLIGENT BROWSER CORE VIEW ---

@Composable
fun BrowserView(viewModel: JarvisViewModel) {
    val bUrl = viewModel.browserUrl.collectAsStateWithLifecycle()
    val isWork by viewModel.isBrowserWorking.collectAsStateWithLifecycle()
    val summaryText by viewModel.browserSummaryText.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ApiKeyWarningBanner(viewModel)
        }

        item {
            Column {
                Text(
                    "Smart Autonomous Browser".uppercase(),
                    color = CyberCyan,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("Navigate, summarize and fill visual website layouts", color = TextCyan, fontSize = 11.sp)
            }
        }

        item {
            CyberTechCard(title = "Browser Input Coordinates") {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = bUrl.value,
                        onValueChange = { viewModel.browserUrl.value = it },
                        modifier = Modifier.weight(1f),
                        textStyle = LocalTextStyle.current.copy(fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, unfocusedBorderColor = GridLineCyan)
                    )
                    Button(
                        onClick = { viewModel.navigateBrowser(bUrl.value) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                    ) {
                        Text("LOAD", color = SynthDark, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Web Scraping Summary
                Button(
                    onClick = { viewModel.summarizeCurrentWebpage() },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isWork) {
                        CircularProgressIndicator(color = SynthDark, modifier = Modifier.size(20.dp))
                    } else {
                        Icon(imageVector = Icons.Default.MenuBook, contentDescription = "Summary logos", tint = SynthDark)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("AI RESEARCH WEBPAGE SUMMARY", color = SynthDark, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            CyberTechCard(title = "AI Scraper Summary Analysis") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SynthDark, RoundedCornerShape(4.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        summaryText,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// --- 7. LOCAL SANDBOX FILES EXPLORER VIEW ---

@Composable
fun FilesView(viewModel: JarvisViewModel) {
    val fileList by viewModel.sandboxFiles.collectAsStateWithLifecycle()
    val filenameInput = viewModel.filesInputName.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    "Local file registry sandbox".uppercase(),
                    color = CyberCyan,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("Simulate system folders, downloads and automation outputs files", color = TextCyan, fontSize = 11.sp)
            }
        }

        item {
            CyberTechCard(title = "Simulate sandbox action file formulation") {
                OutlinedTextField(
                    value = filenameInput.value,
                    onValueChange = { viewModel.filesInputName.value = it },
                    placeholder = { Text("Resource name (e.g. daily_instagram_caption.txt)", fontSize = 11.sp, color = MutedSlate) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, unfocusedBorderColor = GridLineCyan)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.createSandboxFolder() },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("CREATE VIRTUAL SANDBOX FILE", color = SynthDark, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Text(
                "Sandbox Directory Listings:".uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CyberCyan,
                fontFamily = FontFamily.Monospace
            )
        }

        items(fileList) { filename ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = HologramSlate),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (filename.contains(".")) Icons.Default.TextSnippet else Icons.Default.Folder,
                        contentDescription = "File indicator logo",
                        tint = CyberCyan
                    )
                    Text(
                        filename,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

// --- 8. SYSTEM SETTINGS & AUDIT LOGS VIEW ---

@Composable
fun SettingsView(viewModel: JarvisViewModel) {
    val auditHistory by viewModel.recentLogs.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ApiKeyWarningBanner(viewModel)
        }

        item {
            Column {
                Text(
                    "System Settings & Audits".uppercase(),
                    color = CyberCyan,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("Manage API configuration protocols, permissions, and security audit logs", color = TextCyan, fontSize = 11.sp)
            }
        }

        // Encryption credentials details card
        item {
            CyberTechCard(title = "AI Engine Platform Credentials") {
                Text(
                    "API KEY LOAD DIRECTIVE",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextCyan
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "BuildConfig.GEMINI_API_KEY: ${if (viewModel.isApiKeyConfigured) "ENCRYPTED / LOADED ACTIVE" else "NOT INSTAMPED (Simulation Active)"}",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Security Notice: All active credentials persist securely inside local sandboxes. Never export or expose SQLite row tables via network lines.",
                    fontSize = 10.sp,
                    color = MutedSlate,
                    lineHeight = 14.sp
                )
            }
        }

        // Action Audit list panel represent Security Requirements
        item {
            CyberTechCard(title = "JARVIS OS SECURITY AUDIT LOGS") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("RECENT SENSITIVE AUDITS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextCyan)
                    Button(
                        onClick = { scope.launch { viewModel.repository.clearLogs() } },
                        colors = ButtonDefaults.buttonColors(containerColor = LaserRed.copy(alpha = 0.1f)),
                        modifier = Modifier.border(1.dp, LaserRed, RoundedCornerShape(4.dp)),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("FLUSH AUDITS", color = LaserRed, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(SynthDark, RoundedCornerShape(4.dp))
                        .border(1.dp, GridLineCyan, RoundedCornerShape(4.dp))
                        .padding(8.dp)
                ) {
                    if (auditHistory.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No audit registry records captured.", color = MutedSlate, fontSize = 11.sp)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(auditHistory) { audit ->
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            audit.action.uppercase(),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 9.sp,
                                            color = when (audit.severity) {
                                                "SUCCESS" -> NeonGreen
                                                "WARNING" -> LaserRed
                                                else -> CyberCyan
                                            },
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            SimpleDateFormat("dd-MM HH:mm", Locale.getDefault()).format(Date(audit.timestamp)),
                                            fontSize = 8.sp,
                                            color = MutedSlate,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Text(
                                        audit.details,
                                        fontSize = 10.sp,
                                        color = Color.LightGray
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    HorizontalDivider(color = GridLineCyan.copy(alpha = 0.1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Custom flow-layout composable backport to keep dependencies clean
fun Modifier.fillHorizontalFlow(space: androidx.compose.ui.unit.Dp): Modifier {
    return this.wrapContentHeight()
}
