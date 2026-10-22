package com.whatsforlunch.app.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whatsforlunch.app.data.scanner.CameraXScanImageSource
import org.koin.androidx.compose.koinViewModel

@Composable
fun ScannerRoute(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onOpenRecipe: (String) -> Unit,
    onSuggestWithAi: () -> Unit,
    viewModel: ScannerViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current
    val language = configuration.locales[0].language
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val previewView = remember(context) { PreviewView(context) }
    val imageSource = remember(context, lifecycleOwner) { CameraXScanImageSource(context, lifecycleOwner) }

    val bindPreview = remember(imageSource, previewView, viewModel) {
        {
            imageSource.bindPreview(
                previewView = previewView,
                onReady = viewModel::onCameraPreviewReady,
                onFailure = viewModel::onCameraPreviewFailed,
            )
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            viewModel.onCameraPermissionGranted()
            bindPreview()
        } else {
            viewModel.onCameraPermissionDenied()
        }
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            viewModel.onCameraPermissionGranted()
            bindPreview()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    DisposableEffect(imageSource) {
        onDispose { imageSource.unbindPreview() }
    }
    LaunchedEffect(uiState.saveCompleted) {
        if (uiState.saveCompleted) {
            viewModel.consumeSaveCompleted()
            onSaved()
        }
    }

    ScannerScreen(
        uiState = uiState,
        onBack = onBack,
        previewContent = {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxWidth().height(300.dp).testTag(ScannerTestTags.CAMERA_PREVIEW),
            )
        },
        onCapture = { viewModel.captureAndAnalyze(imageSource, language) },
        onRescan = { viewModel.requestRescan() },
        onRequestExit = viewModel::requestExit,
        onConfirmExit = { if (!viewModel.confirmExit()) onBack() },
        onCancelExit = viewModel::cancelExitConfirmation,
        onAddManualItem = viewModel::addManualItem,
        onRenameItem = viewModel::renameItem,
        onQuantityChange = viewModel::updateQuantity,
        onRemoveItem = viewModel::removeItem,
        onSave = viewModel::saveToPantry,
        onClearNotice = viewModel::clearNotice,
        onOpenRecipe = onOpenRecipe,
        onSuggestWithAi = onSuggestWithAi,
    )
}
