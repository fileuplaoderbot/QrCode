package com.example

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.GeneratorScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LockScreen
import com.example.ui.screens.ResultSheet
import com.example.ui.screens.ScannerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.Screen

class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val lockState by viewModel.lockState.collectAsStateWithLifecycle()
            val activePayload by viewModel.activePayload.collectAsStateWithLifecycle()
            val activeScanEntityId by viewModel.activeScanEntityId.collectAsStateWithLifecycle()
            val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
            val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

            val snackbarHostState = remember { SnackbarHostState() }

            LaunchedEffect(snackbarMessage) {
                snackbarMessage?.let { msg ->
                    snackbarHostState.showSnackbar(msg)
                    viewModel.clearMessage()
                }
            }

            MyApplicationTheme(themePreference = themeMode) {
                if (lockState.isLocked && lockState.lockType != com.example.data.preferences.LockType.NONE) {
                    LockScreen(viewModel = viewModel)
                } else {
                    MainAppScaffold(
                        viewModel = viewModel,
                        currentScreen = currentScreen,
                        snackbarHostState = snackbarHostState
                    )

                    // Active Scan Result Sheet (Smart Preview)
                    activePayload?.let { payload ->
                        ResultSheet(
                            payload = payload,
                            existingEntityId = activeScanEntityId,
                            viewModel = viewModel,
                            onDismiss = {
                                viewModel.dismissPayloadPreview()
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) {
            viewModel.lockApp()
        }
    }

    override fun startActivityForResult(intent: android.content.Intent, requestCode: Int) {
        val safeCode = if (requestCode != -1) (requestCode and 0xFFFF) else -1
        try {
            super.startActivityForResult(intent, safeCode)
        } catch (e: Exception) {
            super.startActivityForResult(intent, -1)
        }
    }

    override fun startActivityForResult(intent: android.content.Intent, requestCode: Int, options: android.os.Bundle?) {
        val safeCode = if (requestCode != -1) (requestCode and 0xFFFF) else -1
        try {
            super.startActivityForResult(intent, safeCode, options)
        } catch (e: Exception) {
            super.startActivityForResult(intent, -1, options)
        }
    }
}

@Composable
fun MainAppScaffold(
    viewModel: MainViewModel,
    currentScreen: Screen,
    snackbarHostState: SnackbarHostState
) {
    // BackHandler to return to Home from other tabs
    BackHandler(enabled = currentScreen != Screen.HOME) {
        viewModel.navigateTo(Screen.HOME)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = currentScreen == Screen.HOME,
                    onClick = { viewModel.navigateTo(Screen.HOME) },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home", fontSize = 11.sp, fontWeight = if (currentScreen == Screen.HOME) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("nav_home")
                )

                NavigationBarItem(
                    selected = currentScreen == Screen.SCANNER,
                    onClick = { viewModel.navigateTo(Screen.SCANNER) },
                    icon = { Icon(Icons.Default.CenterFocusStrong, contentDescription = "Scan") },
                    label = { Text("Scan", fontSize = 11.sp, fontWeight = if (currentScreen == Screen.SCANNER) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("nav_scan")
                )

                NavigationBarItem(
                    selected = currentScreen == Screen.GENERATOR,
                    onClick = { viewModel.navigateTo(Screen.GENERATOR) },
                    icon = { Icon(Icons.Default.QrCode, contentDescription = "Generate") },
                    label = { Text("Generate", fontSize = 11.sp, fontWeight = if (currentScreen == Screen.GENERATOR) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("nav_generate")
                )

                NavigationBarItem(
                    selected = currentScreen == Screen.HISTORY,
                    onClick = { viewModel.navigateTo(Screen.HISTORY) },
                    icon = { Icon(Icons.Default.History, contentDescription = "History") },
                    label = { Text("History", fontSize = 11.sp, fontWeight = if (currentScreen == Screen.HISTORY) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("nav_history")
                )

                NavigationBarItem(
                    selected = currentScreen == Screen.SETTINGS,
                    onClick = { viewModel.navigateTo(Screen.SETTINGS) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings", fontSize = 11.sp, fontWeight = if (currentScreen == Screen.SETTINGS) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("nav_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            Crossfade(targetState = currentScreen, label = "screen_crossfade") { screen ->
                when (screen) {
                    Screen.HOME -> HomeScreen(viewModel = viewModel)
                    Screen.SCANNER -> ScannerScreen(viewModel = viewModel)
                    Screen.GENERATOR -> GeneratorScreen(viewModel = viewModel)
                    Screen.HISTORY -> HistoryScreen(viewModel = viewModel)
                    Screen.FAVORITES -> FavoritesScreen(viewModel = viewModel)
                    Screen.SETTINGS -> SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }
}
