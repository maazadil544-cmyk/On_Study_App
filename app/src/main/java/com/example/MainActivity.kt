package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.MoreBottomSheet
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.StudyViewModel
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private val viewModel: StudyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                StudyApp(viewModel = viewModel, onFinish = { finish() })
            }
        }
    }
}

@Composable
fun StudyApp(viewModel: StudyViewModel, onFinish: () -> Unit) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val showMoreSheet by viewModel.showMoreBottomSheet.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Collect snackbar / toast notifications
    LaunchedEffect(Unit) {
        viewModel.uiMessage.collectLatest { msg ->
            snackbarHostState.showSnackbar(
                message = msg.text,
                duration = SnackbarDuration.Short
            )
        }
    }

    // Handle system hardware/gesture back press
    BackHandler(enabled = true) {
        if (showMoreSheet) {
            viewModel.showMoreBottomSheet.value = false
            return@BackHandler
        }
        val handled = viewModel.navigateBack()
        if (!handled) {
            if (currentScreen != Screen.HOME && currentScreen != Screen.SPLASH && currentScreen != Screen.LOGIN) {
                if (viewModel.isUserLoggedIn.value) {
                    viewModel.navigateTo(Screen.HOME, addToBackStack = false)
                } else {
                    viewModel.navigateTo(Screen.LOGIN, addToBackStack = false)
                }
            } else {
                onFinish()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        modifier = Modifier.fillMaxSize()
    ) { _ ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    Screen.SPLASH -> SplashScreen(viewModel = viewModel)
                    Screen.LOGIN -> LoginScreen(viewModel = viewModel)
                    Screen.HOME -> HomeScreen(viewModel = viewModel)
                    Screen.CLASS_SELECT -> ClassSelectScreen(viewModel = viewModel)
                    Screen.BOOK_LIST -> BookListScreen(viewModel = viewModel)
                    Screen.READER -> StudyReaderScreen(viewModel = viewModel)
                    Screen.BOOKMARKS -> BookmarksScreen(viewModel = viewModel)
                    Screen.NOTES -> NotesScreen(viewModel = viewModel)
                    Screen.NEWS -> NewsScreen(viewModel = viewModel)
                    Screen.SETTINGS -> SettingsScreen(viewModel = viewModel)
                    Screen.ADMIN_PANEL -> AdminPanelScreen(viewModel = viewModel)
                }
            }

            // More Bottom Sheet modal
            if (showMoreSheet) {
                MoreBottomSheet(
                    viewModel = viewModel,
                    onDismiss = { viewModel.showMoreBottomSheet.value = false }
                )
            }
        }
    }
}

