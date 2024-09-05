package com.example.qrcodescannerapp

import android.os.Bundle
import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qrcodescannerapp.ui.theme.QRCodeScannerAppTheme
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import okhttp3.*
import java.io.IOException

class MainActivity : ComponentActivity() {
    private var scannedProductDataList = mutableStateListOf<String>()
    private var warehouseCode by mutableStateOf("")
    private var isWarehouseScan by mutableStateOf(false)

    private val qrScannerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val intentResult = IntentIntegrator.parseActivityResult(result.resultCode, result.data)
        if (intentResult != null) {
            if (intentResult.contents == null) {
                // 사용자가 QR 코드 스캔을 취소한 경우
            } else {
                handleQRCodeResult(intentResult)
            }
        } else {
            Toast.makeText(this, "QR 코드 스캔 실패", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QRCodeScannerAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .verticalScroll(rememberScrollState()) // 스크롤 가능하도록 설정
                    ) {

                        // 상단 제목 박스
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1565C0))
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "DONGJU QRcode Scanner V1.0",
                                fontSize = 22.sp,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                        // 추가된 부분: Box와 ProductScanButton 사이에 공간 확보
                        Spacer(modifier = Modifier.height(32.dp))
                        // 기존 내용들
                        ProductScanButton(::startProductScanner)
                        QRCodeList(scannedProductDataList)
                        Spacer(modifier = Modifier.height(16.dp))
                        WarehouseCodeScanButton(::startWarehouseCodeScanner)
                        Text(text = "$warehouseCode")
                        Spacer(modifier = Modifier.height(16.dp))
                        ActionButtons(::sendDataToServer, ::clearList, ::finish)
                    }
                }
            }
        }
    }

    private fun startWarehouseCodeScanner() {
        isWarehouseScan = true
        val integrator = IntentIntegrator(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.setPrompt("QR Code Scanner Product by EZTAKE")
        integrator.setCameraId(0)
        integrator.setBeepEnabled(true)
        integrator.setBarcodeImageEnabled(true)
        qrScannerLauncher.launch(integrator.createScanIntent())
    }

    private fun startProductScanner() {
        isWarehouseScan = false
        val integrator = IntentIntegrator(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.setPrompt("QR Code Scanner Product by EZTAKE")
        integrator.setCameraId(0)
        integrator.setBeepEnabled(true)
        integrator.setBarcodeImageEnabled(true)
        qrScannerLauncher.launch(integrator.createScanIntent())
    }

    private fun handleQRCodeResult(result: IntentResult) {
        if (isWarehouseScan) {
            warehouseCode = result.contents
        } else {
            scannedProductDataList.add(result.contents)
        }
    }

    private fun sendDataToServer() {
        if (scannedProductDataList.isEmpty()) {
            Toast.makeText(this, "데이터값이 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val url = "http://14.63.165.185/V1/qr_update.php"
        val client = OkHttpClient()

        val formBody = FormBody.Builder()
            .add("data", scannedProductDataList.joinToString(","))
            .add("warehouseCode", warehouseCode)
            .build()

        val request = Request.Builder()
            .url(url)
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(applicationContext, "데이터 전송 실패: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(applicationContext, "데이터 전송 완료", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(applicationContext, "데이터 전송 실패: ${response.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    private fun clearList() {
        scannedProductDataList.clear()
        warehouseCode = ""
        Toast.makeText(this, "목록이 초기화되었습니다.", Toast.LENGTH_SHORT).show()
    }

    @Composable
    fun WarehouseCodeScanButton(onClick: () -> Unit) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = onClick,
                modifier = Modifier
                    .width(300.dp)
                    .padding(vertical = 24.dp)
            ) {
                Text(
                    text = "적재위치스캔",
                    fontSize = 22.sp  // 텍스트 폰트 사이즈 조절 (예: 18sp)
                )
            }
        }
    }

    @Composable
    fun ProductScanButton(onClick: () -> Unit) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = onClick,
                modifier = Modifier
                    .width(300.dp)  // 원하는 고정 너비로 설정
                    .padding(vertical = 24.dp)
            ) {
                Text(
                    text = "품목정보스캔",
                    fontSize = 22.sp  // 텍스트 폰트 사이즈 조절
                )
            }
        }
    }


    @Composable
    fun QRCodeList(scannedDataList: List<String>) {
        Column {
            scannedDataList.forEachIndexed { index, data ->
                if (index > 0) {
                    Divider(color = Color.Gray, thickness = 1.dp)
                }
                Text(text = data, modifier = Modifier.padding(4.dp))
            }
        }
    }

    @Composable
    fun ActionButtons(onSendDataClick: () -> Unit, onClearListClick: () -> Unit, onExitClick: () -> Unit) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = onSendDataClick, modifier = Modifier.weight(1f).padding(vertical = 24.dp)) {
                Text(text = "전송", fontSize = 22.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onClearListClick, modifier = Modifier.weight(1f).padding(vertical = 24.dp)) {
                Text(text = "초기화", fontSize = 22.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onExitClick, modifier = Modifier.weight(1f).padding(vertical = 24.dp)) {
                Text(text = "종료", fontSize = 22.sp)
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun DefaultPreview() {
        QRCodeScannerAppTheme {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()) // 스크롤 가능하도록 설정
            ) {
                ProductScanButton(onClick = {})
                QRCodeList(listOf("Sample Product Data 1", "Sample Product Data 2"))
                Spacer(modifier = Modifier.height(16.dp))
                WarehouseCodeScanButton(onClick = {})
                Text(text = "창고 코드: WH1")
                Spacer(modifier = Modifier.height(16.dp))
                ActionButtons({}, {}, {})
            }
        }
    }
}
