package com.ml.shubham0204.facenet_android.presentation.screens.live_enroll

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import com.ml.shubham0204.facenet_android.presentation.components.AppAlertDialog
import com.ml.shubham0204.facenet_android.presentation.components.DelayedVisibility
import com.ml.shubham0204.facenet_android.presentation.components.LiveEnrollCameraView
import com.ml.shubham0204.facenet_android.presentation.components.createAlertDialog
import com.ml.shubham0204.facenet_android.presentation.theme.FaceNetAndroidTheme
import org.koin.androidx.compose.koinViewModel

private val liveEnrollCameraPermissionStatus = mutableStateOf(false)
private lateinit var liveEnrollCameraPermissionLauncher: ManagedActivityResultLauncher<String, Boolean>

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveEnrollScreen(onNavigateBack: () -> Unit) {
    val viewModel: LiveEnrollScreenViewModel = koinViewModel()
    val snackbarHostState = remember { SnackbarHostState() }

    val snackbarMessage by remember { viewModel.snackbarMessage }
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.snackbarMessage.value = null
        }
    }

    FaceNetAndroidTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(text = "Enrol Face", style = MaterialTheme.typography.headlineSmall)
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                contentDescription = "Navigate Back",
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.changeCameraFacing() }) {
                            Icon(
                                imageVector = Icons.Default.Cameraswitch,
                                contentDescription = "Switch Camera",
                            )
                        }
                    },
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                ScreenUI(viewModel)
                AppAlertDialog()
            }
        }
    }

    if (viewModel.showNameDialog.value) {
        NameInputDialog(viewModel)
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun ScreenUI(viewModel: LiveEnrollScreenViewModel) {
    val context = LocalContext.current
    liveEnrollCameraPermissionStatus.value =
        ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
    val cameraFacing by remember { viewModel.cameraFacing }
    val lifecycleOwner = LocalLifecycleOwner.current
    val isFrozen by remember { viewModel.isFrozen }
    val isSaving by remember { viewModel.isSaving }

    liveEnrollCameraPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                liveEnrollCameraPermissionStatus.value = true
            } else {
                createAlertDialog(
                    "Camera Permission",
                    "The app needs the camera permission to enrol faces.",
                    "ALLOW",
                    "CLOSE",
                    onPositiveButtonClick = {
                        liveEnrollCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    onNegativeButtonClick = {},
                )
            }
        }

    Box(modifier = Modifier.fillMaxSize()) {
        DelayedVisibility(liveEnrollCameraPermissionStatus.value) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { LiveEnrollCameraView(lifecycleOwner, context, viewModel) },
                update = { it.initializeCamera(cameraFacing) },
            )
        }
        DelayedVisibility(!liveEnrollCameraPermissionStatus.value) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Allow Camera Permissions\nThe app cannot enrol faces without the camera permission.",
                    textAlign = TextAlign.Center,
                )
                Button(
                    onClick = {
                        liveEnrollCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text(text = "Allow")
                }
            }
        }
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DelayedVisibility(isFrozen) {
                Text(
                    text = "Face captured — enter a name below",
                    color = androidx.compose.ui.graphics.Color.White,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            Button(
                onClick = { viewModel.onCaptureTapped() },
                enabled = !isSaving,
            ) {
                Text(text = "Capture")
            }
        }
    }
}

@Composable
private fun NameInputDialog(viewModel: LiveEnrollScreenViewModel) {
    var name by remember { viewModel.personNameInput }
    val isSaving by remember { viewModel.isSaving }
    AlertDialog(
        onDismissRequest = { viewModel.onDismissDialog() },
        title = { Text("Enter person's name") },
        text = {
            TextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { viewModel.onSaveEnrolment() },
                enabled = name.isNotBlank() && !isSaving,
            ) {
                Text(if (isSaving) "Saving…" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.onDismissDialog() }) {
                Text("Cancel")
            }
        },
    )
}
