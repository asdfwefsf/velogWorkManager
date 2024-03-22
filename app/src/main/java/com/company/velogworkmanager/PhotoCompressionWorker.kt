package com.company.velogworkmanager

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.roundToInt

// CoroutineWorker를 상속받은 클래스를 생성하여서 doWork()를 구현해서 비동기적으로 수행 할 작업을 정의해준다.
class PhotoCompressionWorker(
    private val appContext: Context,
    private val params: WorkerParameters
): CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        // 해당 doWork 메서드는 실제 비동기작업이 작업이 수행된다.
        return withContext(Dispatchers.IO) {
            // 코루틴의 Dispatchers.IO에서 실행 할 수 있도록 withContext()를 사용하였다.

            // RequestBuilder(작업 요청 빌더)에서 WorkRequest 객체(작업에 필요한 데이터 가지고 있음)를 params에서 받아온거야.
            // params의 inputData에서 KEY_CONTENT_URI를 키로 가지는 값을 받아온다.
            val stringUri = params.inputData.getString(KEY_CONTENT_URI)

            val compressionTresholdInBytes = params.inputData.getLong(
                KEY_COMPRESSION_THRESHOLD,
                0L
            )

            // 문자열인 stringUri를 Uri 객체로 변환해준다.
            val uri = Uri.parse(stringUri)

            // uri에서 이미지 데이터를 바이트 배열로 읽는다.
            val bytes = appContext.contentResolver.openInputStream(uri)?.use {
                it.readBytes()
            } ?: return@withContext Result.failure()

            // bitmap 변수에 바이트 배열을 비트맵으로 변환한다.
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

            var outputBytes: ByteArray

            var quality = 100

            do {
                val outputStream = ByteArrayOutputStream()
                outputStream.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                    outputBytes = outputStream.toByteArray()
                    quality -= (quality * 0.1).roundToInt()
                }
            } while(outputBytes.size > compressionTresholdInBytes && quality > 5)

            val file = File(appContext.cacheDir, "${params.id}.jpg")

            file.writeBytes(outputBytes)

            Result.success(
                workDataOf(
                    KEY_RESULT_PATH to file.absolutePath
                )
            )
        }
    }

    companion object {
        const val KEY_CONTENT_URI = "KEY_CONTENT_URI"
        const val KEY_COMPRESSION_THRESHOLD = "KEY_COMPRESSION_THRESHOLD"
        const val KEY_RESULT_PATH = "KEY_RESULT_PATH"
    }
}