package com.company.velogworkmanager

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.work.Constraints
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import coil.compose.AsyncImage
import com.company.velogworkmanager.ui.theme.VelogWorkManagerTheme

class MainActivity : ComponentActivity() {

    private lateinit var workManager: WorkManager
    private val viewModel by viewModels<PhotoViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        workManager = WorkManager.getInstance(applicationContext)
        setContent {
            VelogWorkManagerTheme {
                val workerResult = viewModel.workId?.let { id ->
                    workManager.getWorkInfoByIdLiveData(id).observeAsState().value
                }
                LaunchedEffect(key1 = workerResult?.outputData) {
                    if(workerResult?.outputData != null) {
                        val filePath = workerResult.outputData.getString(
                            PhotoCompressionWorker.KEY_RESULT_PATH
                        )
                        filePath?.let {
                            val bitmap = BitmapFactory.decodeFile(it)
                            viewModel.updateCompressedBitmap(bitmap)
                        }
                    }
                }


                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    viewModel.uncompressedUri?.let {
                        Text(text = "압축 전:")
                        AsyncImage(model = it, contentDescription = null)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    viewModel.compressedBitmap?.let {
                        Text(text = "압축 후:")
                        Image(bitmap = it.asImageBitmap(), contentDescription = null)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        // intent-filter에 적용된 인텐트를 통해서 받은 intent에 있는
        // Intent.EXTRA_STREAM(상수임)이라는 키를 가진 데이터를 Uri 타입으로 가져온다.
        // -> Intent.EXTRA_STREAM은 파일 또는 데이터의 URI를 공유할 때 사용된다.
        // getParcelableExtra()는 parameter에 있는 데이터를 직렬화시켜서 가져온다.
        // 결론적으로 uri 변수에 Intent를 통해서 넘어온 데이터를 직렬화 시켜서 Uri 타입으로 가져온다.
        val uri = if(Build.VERSION.SDK_INT >=  Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            intent?.getParcelableExtra(Intent.EXTRA_STREAM)
        } ?: return

        // PhotoViewModel의 uncompressedUri 변수에 uri 변수에 들어있는 값을 넣어준다.
        viewModel.updateUncompressUri(uri)


        // WorkManager에서는 작업을 요청할 때 두가지의 방법을 가지고 있다.
        // 단일 실행 작업 요청 : OneTimeWorkRequestBuilder를 사용한다.
        // OneTimeWorkRequestBuilder<CoroutineWorker를 상속받은 클래스>()
        //  .setInputData( workDataOf() ) : 작업을 실행하는데 필요한 입력 데이터 정의 / 작업 실행에 필요한 입력 데이터를 CoroutineWorker를 상속받은 클래스의 params에 전달한다.
        //  .build() : WorkRequest 객체 생성



        // 주기적으로 반복되는 작업 요청 : PeriodicWorkRequestBuilder를 사용한다.
        // PeriodicWorkRequestBuilder<<CoroutineWorker를 상속받은 클래스>(interval 숫자 , TimeUnit.종류(ex:HOUR , MINUTES , DAYS..))
        //  .setInputData( workDataOf() ) : 작업을 실행하는데 필요한 입력 데이터 정의 / 작업 실행에 필요한 입력 데이터를 CoroutineWorker를 상속받은 클래스의 params에 전달한다.
        //  .build() : WorkRequest 객체 생성



        val request = OneTimeWorkRequestBuilder<PhotoCompressionWorker>()
            .setInputData( // setInputData는 작업을 실행하는데 필요한 입력 데이터를 정의한다.
                workDataOf(// workDataOf()는 parameter 키-값 형태로 구성되고 , 작업 실행에 필요한 입력 데이터를 PhotoCompressionWorker의 params에 전달한다.
                    // to는 좌와 우를 키와 값 쌍으로 묶어주는 역할을 한다.
                    PhotoCompressionWorker.KEY_CONTENT_URI to uri.toString(),
                    PhotoCompressionWorker.KEY_COMPRESSION_THRESHOLD to 1024 * 20L
                )
            )
            .build() // WorkRequest 객체 생성해서 request 변수에 저장한다.

        // WorkRequest의 식별자를 업데이트 해준다.
        viewModel.updateWorkId(request.id)

        // enqueue는 요청된 작업을 큐에 추가해서 작업이 실행 될 수 있는 조건이 만족되었을 때 큐에 있는 작업을 시작한다.
        workManager.enqueue(request)
    }
}

