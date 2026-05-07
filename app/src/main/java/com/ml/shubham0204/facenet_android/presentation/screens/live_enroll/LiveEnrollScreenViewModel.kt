package com.ml.shubham0204.facenet_android.presentation.screens.live_enroll

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.camera.core.CameraSelector
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.ml.shubham0204.facenet_android.domain.ImageVectorUseCase
import com.ml.shubham0204.facenet_android.domain.PersonUseCase
import com.ml.shubham0204.facenet_android.domain.face_detection.BaseFaceDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class LiveEnrollScreenViewModel(
    val faceDetector: BaseFaceDetector,
    private val personUseCase: PersonUseCase,
    private val imageVectorUseCase: ImageVectorUseCase,
) : ViewModel() {
    val latestDetection: MutableState<Pair<Bitmap, Rect>?> = mutableStateOf(null)
    val isFrozen: MutableState<Boolean> = mutableStateOf(false)
    val showNameDialog: MutableState<Boolean> = mutableStateOf(false)
    val personNameInput: MutableState<String> = mutableStateOf("")
    val isSaving: MutableState<Boolean> = mutableStateOf(false)
    val snackbarMessage: MutableState<String?> = mutableStateOf(null)
    val cameraFacing = mutableIntStateOf(CameraSelector.LENS_FACING_BACK)

    fun onCaptureTapped() {
        if (latestDetection.value == null) {
            snackbarMessage.value = "No face detected. Please position your face in frame."
        } else {
            isFrozen.value = true
            showNameDialog.value = true
        }
    }

    fun onSaveEnrolment() {
        val faceBitmap = latestDetection.value?.first ?: return
        val name = personNameInput.value.trim()
        if (name.isEmpty()) return
        isSaving.value = true
        CoroutineScope(Dispatchers.Default).launch {
            val personID = personUseCase.addPerson(name, 1L)
            imageVectorUseCase.addImageFromBitmap(personID, name, faceBitmap)
            withContext(Dispatchers.Main) {
                isSaving.value = false
                showNameDialog.value = false
                personNameInput.value = ""
                isFrozen.value = false
                latestDetection.value = null
                snackbarMessage.value = "Enrolled \"$name\" successfully"
            }
        }
    }

    fun onDismissDialog() {
        showNameDialog.value = false
        personNameInput.value = ""
        isFrozen.value = false
    }

    fun changeCameraFacing() {
        cameraFacing.intValue =
            if (cameraFacing.intValue == CameraSelector.LENS_FACING_FRONT) {
                CameraSelector.LENS_FACING_BACK
            } else {
                CameraSelector.LENS_FACING_FRONT
            }
    }
}
