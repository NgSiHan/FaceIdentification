package com.ml.shubham0204.facenet_android.presentation.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.FrameLayout
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toRectF
import androidx.core.view.doOnLayout
import androidx.lifecycle.LifecycleOwner
import com.ml.shubham0204.facenet_android.presentation.screens.live_enroll.LiveEnrollScreenViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

@SuppressLint("ViewConstructor")
@ExperimentalGetImage
class LiveEnrollCameraView(
    private val lifecycleOwner: LifecycleOwner,
    private val context: Context,
    private val viewModel: LiveEnrollScreenViewModel,
) : FrameLayout(context) {
    private var overlayWidth: Int = 0
    private var overlayHeight: Int = 0

    private var imageTransform: Matrix = Matrix()
    private var boundingBoxTransform: Matrix = Matrix()
    private var isImageTransformInitialized = false
    private var isBoundingBoxTransformInitialized = false

    private lateinit var frameBitmap: Bitmap
    private var isProcessing = false
    private var cameraFacing: Int? = null
    private lateinit var boundingBoxOverlay: BoundingBoxOverlay
    private lateinit var previewView: PreviewView

    private var detectionBoxes: Array<RectF> = arrayOf()

    init {
        doOnLayout {
            overlayHeight = it.measuredHeight
            overlayWidth = it.measuredWidth
        }
    }

    fun initializeCamera(cameraFacing: Int) {
        this.cameraFacing = cameraFacing
        this.isImageTransformInitialized = false
        this.isBoundingBoxTransformInitialized = false
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val previewView = PreviewView(context)
        val executor = ContextCompat.getMainExecutor(context)
        cameraProviderFuture.addListener(
            {
                val cameraProvider = cameraProviderFuture.get()
                val preview =
                    Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                val cameraSelector =
                    CameraSelector.Builder().requireLensFacing(cameraFacing).build()
                val frameAnalyzer =
                    ImageAnalysis
                        .Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .build()
                frameAnalyzer.setAnalyzer(Executors.newSingleThreadExecutor(), analyzer)
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    frameAnalyzer,
                )
            },
            executor,
        )
        if (childCount == 2) {
            removeView(this.previewView)
            removeView(this.boundingBoxOverlay)
        }
        this.previewView = previewView
        addView(this.previewView)

        val overlayParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        this.boundingBoxOverlay = BoundingBoxOverlay(context)
        this.boundingBoxOverlay.setWillNotDraw(false)
        this.boundingBoxOverlay.setZOrderOnTop(true)
        addView(this.boundingBoxOverlay, overlayParams)
    }

    private val analyzer =
        ImageAnalysis.Analyzer { image ->
            if (isProcessing || viewModel.isFrozen.value) {
                image.close()
                return@Analyzer
            }
            isProcessing = true

            frameBitmap = createBitmap(image.image!!.width, image.image!!.height)
            frameBitmap.copyPixelsFromBuffer(image.planes[0].buffer)

            if (!isImageTransformInitialized) {
                imageTransform = Matrix()
                imageTransform.postRotate(image.imageInfo.rotationDegrees.toFloat())
                isImageTransformInitialized = true
            }
            frameBitmap =
                Bitmap.createBitmap(
                    frameBitmap,
                    0,
                    0,
                    frameBitmap.width,
                    frameBitmap.height,
                    imageTransform,
                    false,
                )

            if (!isBoundingBoxTransformInitialized) {
                boundingBoxTransform = Matrix()
                boundingBoxTransform.setScale(
                    overlayWidth / frameBitmap.width.toFloat(),
                    overlayHeight / frameBitmap.height.toFloat(),
                )
                if (cameraFacing == CameraSelector.LENS_FACING_FRONT) {
                    boundingBoxTransform.postScale(
                        -1f,
                        1f,
                        overlayWidth.toFloat() / 2.0f,
                        overlayHeight.toFloat() / 2.0f,
                    )
                }
                isBoundingBoxTransformInitialized = true
            }

            CoroutineScope(Dispatchers.Default).launch {
                val results = viewModel.faceDetector.getAllCroppedFaces(frameBitmap)
                val boxes = ArrayList<RectF>()
                val firstDetection = results.firstOrNull()

                results.forEach { (_, boundingBox) ->
                    val box = boundingBox.toRectF()
                    boundingBoxTransform.mapRect(box)
                    boxes.add(box)
                }

                withContext(Dispatchers.Main) {
                    viewModel.latestDetection.value = firstDetection
                    detectionBoxes = boxes.toTypedArray()
                    boundingBoxOverlay.invalidate()
                    isProcessing = false
                }
            }
            image.close()
        }

    inner class BoundingBoxOverlay(
        context: Context,
    ) : SurfaceView(context),
        SurfaceHolder.Callback {
        private val boxPaint =
            Paint().apply {
                color = Color.parseColor("#4D90caf9")
                style = Paint.Style.FILL
            }
        private val textPaint =
            Paint().apply {
                strokeWidth = 2.0f
                textSize = 36f
                color = Color.WHITE
            }

        override fun surfaceCreated(holder: SurfaceHolder) {}

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

        override fun surfaceDestroyed(holder: SurfaceHolder) {}

        override fun onDraw(canvas: Canvas) {
            detectionBoxes.forEach { box ->
                canvas.drawRoundRect(box, 16f, 16f, boxPaint)
                canvas.drawText("Face detected", box.centerX(), box.centerY(), textPaint)
            }
        }
    }
}
