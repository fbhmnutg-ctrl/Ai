package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.BenchmarkScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.ModelHubScreen
import com.example.ui.screens.OllamaHostScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.UploadedTemplatesScreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonCyanSubtle
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.BenchmarkViewModel
import com.example.ui.viewmodel.ChatViewModel
import com.example.ui.viewmodel.ModelHubViewModel
import com.example.ui.viewmodel.OllamaHostViewModel

enum class AppDestination(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    CHAT("Chat", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline, "tab_chat"),
    MODELS("GGUF Hub", Icons.Filled.Memory, Icons.Outlined.Memory, "tab_models"),
    UPLOADED("Templates", Icons.Filled.CloudUpload, Icons.Outlined.CloudUpload, "tab_uploaded"),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings, "tab_settings"),
    OLLAMA("Ollama", Icons.Filled.Dns, Icons.Outlined.Dns, "tab_ollama"),
    BENCHMARK("Speed", Icons.Filled.Speed, Icons.Outlined.Speed, "tab_benchmark")
}

class MainActivity : ComponentActivity() {

    private val chatViewModel: ChatViewModel by viewModels()
    private val modelHubViewModel: ModelHubViewModel by viewModels()
    private val ollamaHostViewModel: OllamaHostViewModel by viewModels()
    private val benchmarkViewModel: BenchmarkViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                var currentTab by rememberSaveable { mutableStateOf(AppDestination.CHAT) }

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ObsidianBg),
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier
                                .border(1.dp, ObsidianBorder)
                                .windowInsetsPadding(WindowInsets.navigationBars),
                            containerColor = ObsidianSurface,
                            contentColor = TextPrimary
                        ) {
                            val navItems = listOf(
                                AppDestination.CHAT,
                                AppDestination.MODELS,
                                AppDestination.UPLOADED,
                                AppDestination.SETTINGS,
                                AppDestination.OLLAMA
                            )
                            navItems.forEach { destination ->
                                val isSelected = currentTab == destination || (destination == AppDestination.SETTINGS && currentTab == AppDestination.BENCHMARK)
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { currentTab = destination },
                                    icon = {
                                        Icon(
                                            imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                                            contentDescription = destination.title,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = destination.title,
                                            fontSize = 11.sp
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = NeonCyan,
                                        selectedTextColor = NeonCyan,
                                        indicatorColor = NeonCyanSubtle,
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted
                                    ),
                                    modifier = Modifier.testTag(destination.testTag)
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentTab) {
                            AppDestination.CHAT -> {
                                ChatScreen(
                                    viewModel = chatViewModel,
                                    onNavigateToModels = { currentTab = AppDestination.MODELS },
                                    onNavigateToOllama = { currentTab = AppDestination.OLLAMA },
                                    onNavigateToUploaded = { currentTab = AppDestination.UPLOADED }
                                )
                            }
                            AppDestination.MODELS -> {
                                ModelHubScreen(
                                    viewModel = modelHubViewModel,
                                    onModelSelectedForChat = { model ->
                                        chatViewModel.setActiveModel(model)
                                        currentTab = AppDestination.CHAT
                                    }
                                )
                            }
                            AppDestination.UPLOADED -> {
                                UploadedTemplatesScreen(
                                    viewModel = modelHubViewModel,
                                    onModelSelectedForChat = { model ->
                                        chatViewModel.setActiveModel(model)
                                        currentTab = AppDestination.CHAT
                                    }
                                )
                            }
                            AppDestination.SETTINGS -> {
                                SettingsScreen(
                                    chatViewModel = chatViewModel,
                                    onNavigateToBenchmark = { currentTab = AppDestination.BENCHMARK }
                                )
                            }
                            AppDestination.OLLAMA -> {
                                OllamaHostScreen(
                                    viewModel = ollamaHostViewModel,
                                    onSelectModelForChat = { model ->
                                        chatViewModel.setActiveModel(model)
                                        currentTab = AppDestination.CHAT
                                    }
                                )
                            }
                            AppDestination.BENCHMARK -> {
                                BenchmarkScreen(
                                    viewModel = benchmarkViewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
