package com.kelompok4.eyewise.ui.scan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.kelompok4.eyewise.databinding.FragmentScanBinding
import com.kelompok4.eyewise.ResultActivity
import kotlinx.coroutines.launch
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.classifier.Classifications
import org.tensorflow.lite.task.vision.classifier.ImageClassifier
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScanFragment : Fragment() {
    private var _binding: FragmentScanBinding? = null
    private val binding get() = _binding!!

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var imageClassifier: ImageClassifier
    private var imageCapture: ImageCapture? = null

    // Camera permission request
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()
        initClassifier()

        binding.btnCamera.setOnClickListener {
            takePhotoAndClassify()
        }

        // Check camera permission
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                startCamera()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun initClassifier() {
        lifecycleScope.launch {
            try {
                val options = ImageClassifier.ImageClassifierOptions.builder()
                    .setMaxResults(1)
                    .build()

                imageClassifier = ImageClassifier.createFromFileAndOptions(
                    requireContext(),
                    "eyewise-1.tflite",
                    options
                )
            } catch (e: Exception) {
                Log.e("ScanFragment", "Failed to initialize classifier", e)
                Toast.makeText(context, "Failed to initialize image classifier", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            // ImageCapture for taking photos
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            // Select front camera
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner, cameraSelector, preview, imageCapture
                )
            } catch(ex: Exception) {
                Log.e("ScanFragment", "Use case binding failed", ex)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhotoAndClassify() {
        // Get a stable reference to the ImageCapture use case
        val imageCapture = imageCapture ?: return

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)

                    // Convert ImageProxy to Bitmap
                    val bitmap = imageProxyToBitmap(image)
                    image.close()

                    // Classify the image
                    classifyImage(bitmap)
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e("ScanFragment", "Photo capture failed: ${exc.message}", exc)
                    Toast.makeText(context, "Failed to capture photo", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun classifyImage(bitmap: Bitmap) {
        if (!::imageClassifier.isInitialized) {
            Toast.makeText(context, "Classifier is not initialized", Toast.LENGTH_SHORT).show()
            Log.e("ScanFragment", "Classifier is not initialized")
            return
        }

        try {
            val tensorImage = TensorImage.fromBitmap(bitmap)
            val results = imageClassifier.classify(tensorImage)

            if (results.isNotEmpty() && results[0].categories.isNotEmpty()) {
                val result = results[0]
                val label = result.categories[0].label
                val score = result.categories[0].score

                val intent = Intent(requireContext(), ResultActivity::class.java).apply {
                    putExtra("LABEL", label)
                    putExtra("SCORE", score)
                }
                startActivity(intent)
            } else {
                Toast.makeText(context, "Classification failed - no results", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("ScanFragment", "Classification error", e)
            Toast.makeText(context, "Classification error", Toast.LENGTH_SHORT).show()
        }
    }


    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val planeProxy = image.planes[0]
        val buffer: ByteBuffer = planeProxy.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        val yuvImage = YuvImage(
            bytes,
            ImageFormat.NV21,
            image.width,
            image.height,
            null
        )

        val outputStream = ByteArrayOutputStream()
        yuvImage.compressToJpeg(
            Rect(0, 0, image.width, image.height),
            100,
            outputStream
        )
        val jpegBytes = outputStream.toByteArray()

        val bitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)

        // Rotate the bitmap if needed (front camera images are mirrored)
        val matrix = Matrix().apply {
            postScale(-1f, 1f) // Mirror the image
            postRotate(90f) // Rotate to portrait
        }

        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        _binding = null
    }
}