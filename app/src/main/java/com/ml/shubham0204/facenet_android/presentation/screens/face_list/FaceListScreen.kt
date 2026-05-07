package com.ml.shubham0204.facenet_android.presentation.screens.face_list

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ml.shubham0204.facenet_android.data.PersonRecord
import com.ml.shubham0204.facenet_android.presentation.components.AppAlertDialog
import com.ml.shubham0204.facenet_android.presentation.components.createAlertDialog
import com.ml.shubham0204.facenet_android.presentation.theme.FaceNetAndroidTheme
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceListScreen(
    onNavigateBack: (() -> Unit),
    onAddFaceClick: (() -> Unit),
    onEnrolFromCameraClick: (() -> Unit),
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    FaceNetAndroidTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(text = "Face List", style = MaterialTheme.typography.headlineSmall)
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                contentDescription = "Navigate Back",
                            )
                        }
                    },

                )
            },
            floatingActionButton = {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AnimatedVisibility(
                        visible = isMenuExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            SmallFloatingActionButton(
                                onClick = {
                                    onAddFaceClick()
                                    isMenuExpanded = false
                                },
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = "Add from image")
                            }
                            SmallFloatingActionButton(
                                onClick = {
                                    onEnrolFromCameraClick()
                                    isMenuExpanded = false
                                },
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = "Enrol from camera")
                            }
                        }
                    }
                    FloatingActionButton(onClick = { isMenuExpanded = !isMenuExpanded }) {
                        Icon(
                            imageVector = if (isMenuExpanded) Icons.Default.Clear else Icons.Default.Add,
                            contentDescription = "Add a new face",
                        )
                    }
                }
            },
        ) { innerPadding ->
            val viewModel: FaceListScreenViewModel = koinViewModel()
            Column(modifier = Modifier.padding(innerPadding)) {
                ScreenUI(viewModel)
                AppAlertDialog()
            }
        }
    }
}

@Composable
private fun ScreenUI(viewModel: FaceListScreenViewModel) {
    val faces by viewModel.personFlow.collectAsState(emptyList())
    LazyColumn { items(faces) { FaceListItem(it) { viewModel.removeFace(it.personID) } } }
}

@Composable
private fun FaceListItem(
    personRecord: PersonRecord,
    onRemoveFaceClick: (() -> Unit),
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Color.White).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Text(
                text = personRecord.personName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = DateUtils.getRelativeTimeSpanString(personRecord.addTime).toString(),
                style = MaterialTheme.typography.labelSmall,
                color = Color.DarkGray,
            )
        }
        Icon(
            modifier =
                Modifier.clickable {
                    createAlertDialog(
                        dialogTitle = "Remove person",
                        dialogText =
                            "Are you sure to remove this person from the database. The face for this person will not " +
                                "be detected in realtime",
                        dialogPositiveButtonText = "Remove",
                        onPositiveButtonClick = onRemoveFaceClick,
                        dialogNegativeButtonText = "Cancel",
                        onNegativeButtonClick = {},
                    )
                },
            imageVector = Icons.Default.Clear,
            contentDescription = "Remove face",
        )
        Spacer(modifier = Modifier.width(2.dp))
    }
}
