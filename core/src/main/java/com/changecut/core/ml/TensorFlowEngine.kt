package com.changecut.core.ml

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TensorFlowEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var tfliteAvailable = false
    private var interpreter: Any? = null
    private var gpuDelegate: Any? = null

    sealed class InferenceResult {
        data class Segmentation(
            val mask: ByteBuffer,
            val width: Int,
            val height: Int
        ) : InferenceResult()

        data class FaceMesh(
            val landmarks: List<Pair<Float, Float>>,
            val confidence: Float
        ) : InferenceResult()

        data class ObjectDetections(
            val boxes: List<FloatArray>,
            val scores: List<Float>,
            val classes: List<Int>
        ) : InferenceResult()

        data class Embedding(
            val vector: FloatArray
        ) : InferenceResult()
    }

    fun isAvailable(): Boolean = tfliteAvailable

    fun initialize() {
        try {
            val tfliteClass = Class.forName("org.tensorflow.lite.Interpreter")
            val optionsClass = Class.forName("org.tensorflow.lite.Interpreter\$Options")
            val options = optionsClass.getDeclaredConstructor().newInstance()

            try {
                val gpuDelegateClass = Class.forName("org.tensorflow.lite.gpu.GpuDelegate")
                val gpuOptionsClass = Class.forName("org.tensorflow.lite.gpu.GpuDelegate\$Options")
                val gpuOptions = gpuOptionsClass.getDeclaredConstructor().newInstance()
                gpuDelegate = gpuDelegateClass.getDeclaredConstructor(gpuOptionsClass).newInstance(gpuOptions)
                optionsClass.getMethod("addDelegate", Class.forName("org.tensorflow.lite.Delegate"))
                    .invoke(options, gpuDelegate)
            } catch (_: Exception) { }

            // Try loading each model; continue if one fails.
            // Only valid TFLite flatbuffers should be passed to the interpreter.
            val modelNames = listOf(
                "models/deeplabv3_257_mv_gpu.tflite",
                "models/face_landmark.tflite",
                "models/movenet.tflite"
            )
            for (name in modelNames) {
                try {
                    val modelBuffer = loadModelFile(name)
                    val interpreter = tfliteClass.getDeclaredConstructor(ByteBuffer::class.java, optionsClass)
                        .newInstance(modelBuffer, options)
                    interpreter?.let { this.interpreter = it }
                    break // Use first successfully loaded model
                } catch (_: Exception) { }
            }

            tfliteAvailable = this.interpreter != null
        } catch (e: Exception) {
            tfliteAvailable = false
        }
    }

    fun runPortraitSegmentation(inputBuffer: FloatBuffer, width: Int, height: Int): InferenceResult.Segmentation? {
        if (!tfliteAvailable) return null
        return try {
            val inputShape = intArrayOf(1, height, width, 3)
            val outputShape = intArrayOf(1, height, width, 1)
            val outputBuffer = ByteBuffer.allocateDirect(height * width * 4).order(ByteOrder.nativeOrder())

            val interpreter = interpreter ?: return null
            val runMethod = interpreter::class.java.getMethod("run", Any::class.java, Any::class.java)
            runMethod.invoke(interpreter, inputBuffer, outputBuffer)

            InferenceResult.Segmentation(mask = outputBuffer, width = width, height = height)
        } catch (_: Exception) { null }
    }

    fun detectFaceLandmarks(inputBuffer: FloatBuffer, width: Int, height: Int): InferenceResult.FaceMesh? {
        if (!tfliteAvailable) return null
        return try {
            val interpreter = interpreter ?: return null
            val landmarks = mutableListOf<Pair<Float, Float>>()
            // 468 face landmarks from MediaPipe
            val output = Array(1) { Array(468) { FloatArray(3) } }
            val runMethod = interpreter::class.java.getMethod("run", Any::class.java, Any::class.java)
            runMethod.invoke(interpreter, inputBuffer, output)

            for (i in 0 until 468) {
                landmarks.add(Pair(output[0][i][0], output[0][i][1]))
            }
            InferenceResult.FaceMesh(landmarks = landmarks, confidence = 1.0f)
        } catch (_: Exception) { null }
    }

    private fun loadModelFile(modelName: String): ByteBuffer {
        return try {
            val bytes = context.assets.open(modelName).use { it.readBytes() }
            if (!isValidTflite(bytes)) {
                throw IllegalArgumentException("Model $modelName is not a valid TFLite flatbuffer")
            }
            ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder()).put(bytes).apply { position(0) }
        } catch (e: IOException) {
            throw RuntimeException("Model $modelName not found in assets", e)
        }
    }

    private fun isValidTflite(bytes: ByteArray): Boolean {
        if (bytes.size < 8) return false
        return bytes[4] == 'T'.code.toByte() &&
            bytes[5] == 'F'.code.toByte() &&
            bytes[6] == 'L'.code.toByte() &&
            bytes[7] == '3'.code.toByte()
    }

    fun release() {
        try {
            interpreter?.let {
                it::class.java.getMethod("close").invoke(it)
            }
            gpuDelegate?.let {
                it::class.java.getMethod("close").invoke(it)
            }
        } catch (_: Exception) { }
    }
}
