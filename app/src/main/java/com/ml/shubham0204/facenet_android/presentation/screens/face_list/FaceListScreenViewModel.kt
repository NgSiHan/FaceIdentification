package com.ml.shubham0204.facenet_android.presentation.screens.face_list

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ml.shubham0204.facenet_android.data.FaceRepository
import com.ml.shubham0204.facenet_android.domain.ImageVectorUseCase
import com.ml.shubham0204.facenet_android.domain.PersonUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class FaceListScreenViewModel(
    val imageVectorUseCase: ImageVectorUseCase,
    val personUseCase: PersonUseCase,
    private val faceRepository: FaceRepository,
) : ViewModel() {
    val personFlow = personUseCase.getAll()
    val removeError = mutableStateOf<String?>(null)

    fun removeFace(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            faceRepository.remove(id)
                .onFailure { removeError.value = "Delete failed: ${it.message}" }
        }
    }
}
