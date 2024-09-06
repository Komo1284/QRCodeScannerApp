package com.example.qrcodescannerapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qrcodescannerapp.ui.theme.QRCodeScannerAppTheme
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class MainActivity : ComponentActivity() {
    private var scannedProductDataList = mutableStateListOf<String>()
    private var warehouseCode by mutableStateOf("")
    private var isWarehouseScan by mutableStateOf(false)
    private var isScanning by mutableStateOf(false)

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

                        // 스캔 버튼
                        ProductScanButton(::startProductScanner)
                        if (isScanning && !isWarehouseScan) {
                            ScanningStatus(isScanning)
                            ProductScanInput(::handleQRCodeResult)
                        }

                        QRCodeList(scannedProductDataList)

                        Spacer(modifier = Modifier.height(16.dp))

                        WarehouseCodeScanButton(::startWarehouseCodeScanner)
                        if (isScanning && isWarehouseScan) {
                            ScanningStatus(isScanning)
                            WarehouseCodeInput(::handleQRCodeResult)
                        }

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
        isScanning = true
    }

    private fun startProductScanner() {
        isWarehouseScan = false
        isScanning = true
    }

    private fun handleQRCodeResult(scannedData: String) {
        if (isWarehouseScan) {
            warehouseCode = scannedData
        } else {
            scannedProductDataList.add(scannedData)
        }
        // 스캔이 완료된 후에도 다시 스캔 대기 상태로 돌아가도록 설정
        isScanning = true
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
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(applicationContext, "데이터 전송 실패: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            override fun onResponse(call: okhttp3.Call, response: Response) {
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
    fun ScanningStatus(isScanning: Boolean) {
        if (isScanning) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x80000000), shape = RoundedCornerShape(8.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "스캔 대기 중...",
                    color = Color.White,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }


    @Composable
    fun ProductScanInput(onProductScanned: (String) -> Unit) {
        var scannedData by remember { mutableStateOf("") }
        val focusRequester = remember { FocusRequester() }  // FocusRequester 생성

        // 스캔 버튼을 눌렀을 때 텍스트 필드에 포커스 자동으로 이동
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        TextField(
            value = scannedData,
            onValueChange = { newValue ->
                scannedData = newValue
                if (newValue.isNotBlank()) {
                    onProductScanned(newValue)
                    scannedData = ""  // 입력 필드 초기화
                    // 입력 완료 후 다시 포커스 요청해 다음 스캔 대기
                    focusRequester.requestFocus()
                }
            },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Text
            ),
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent)
                .focusRequester(focusRequester),  // 포커스 설정
            singleLine = true,
            placeholder = { Text(text = "스캔된 제품 정보를 입력하세요") }
        )
    }

    @Composable
    fun WarehouseCodeInput(onWarehouseScanned: (String) -> Unit) {
        var scannedData by remember { mutableStateOf("") }
        val focusRequester = remember { FocusRequester() }

        // 스캔 버튼을 눌렀을 때 텍스트 필드에 포커스 자동으로 이동
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        TextField(
            value = scannedData,
            onValueChange = { newValue ->
                scannedData = newValue
                if (newValue.isNotBlank()) {
                    onWarehouseScanned(newValue)
                    scannedData = ""  // 입력 필드 초기화
                    // 입력 완료 후 다시 포커스 요청해 다음 스캔 대기
                    focusRequester.requestFocus()
                }
            },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Text
            ),
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent)
                .focusRequester(focusRequester),  // 포커스 설정
            singleLine = true,
            placeholder = { Text(text = "스캔된 창고 코드를 입력하세요") }
        )
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
                    .width(300.dp)
                    .padding(vertical = 24.dp)
            ) {
                Text(
                    text = "품목정보스캔",
                    fontSize = 22.sp
                )
            }
        }
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
                    fontSize = 22.sp
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
