package com.example.lotto

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.lotto.databinding.ActivityQrscanBinding
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView

class QRScan : AppCompatActivity() {

    private lateinit var barcodeView: DecoratedBarcodeView
    private val CAMERA_PERMISSION_REQUEST = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding=ActivityQrscanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        barcodeView = findViewById(R.id.barcode_scanner)

        // 카메라 권한을 사용하기 전에 실행
        if (checkCameraPermission()) {
            initializeScanner()
        } else {
            requestCameraPermission()
        }


        binding.backBtn.setOnClickListener {
            finish()
        }
    }

    private fun initializeScanner() {
        // 연속적으로 QR코드 스캔
        // QR코드가 인식되면 barcodeResult 콜백함수 호출하여 result값(QR 코드 내용) processQRCode로 전달
        barcodeView.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult?) {
                result?.let {
                    val qrContent = it.text
                    processQRCode(qrContent)
                }
            }
        })

        // 스캔 영역 가이드 표시
        barcodeView.setStatusText("QR 코드를 스캔 영역에 맞춰주세요")
    }

    private fun processQRCode(qrContent: String) {
        try {
            // 로또 QR 코드 형식 분석
            // 일반적으로 로또 QR 코드는 특정 형식을 가지고 있습니다
            // 예: "https://dhlottery.co.kr/gameResult.do?method=byWin&drwNo=1234"

            // QR코드값 추출해서 값이 나오면 사이트로 이동
            // 추출이 안되면 에러 메시지나오고 스캔 재시작
            val drawNumber = extractDrawNumber(qrContent)

            if (drawNumber != null) {
                // 로또 결과 확인 사이트로 이동
                openLottoResultSite(drawNumber)
            } else {
                Toast.makeText(this, "올바른 로또 QR 코드가 아닙니다.", Toast.LENGTH_SHORT).show()
                // 스캔 재시작
                barcodeView.resume()
            }

        } catch (e: Exception) {
            Toast.makeText(this, "QR 코드 처리 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            barcodeView.resume()
        }
    }

    private fun extractDrawNumber(qrContent: String): String? {
        return try {
            // 다양한 형식에 대응하여 QR 코드에서 회차 번호 추출
            when {
                qrContent.contains("drwNo=") -> {
                    val regex = "drwNo=(\\d+)".toRegex()
                    regex.find(qrContent)?.groupValues?.get(1)
                }
                qrContent.contains("draw=") -> {
                    val regex = "draw=(\\d+)".toRegex()
                    regex.find(qrContent)?.groupValues?.get(1)
                }
                qrContent.matches("\\d+".toRegex()) -> {
                    // 숫자만 있는 경우
                    qrContent
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun openLottoResultSite(drawNumber: String) {
        try {
            // 추출한 회차 번호를 추가하여 url 열기
            val url = "https://dhlottery.co.kr/gameResult.do?method=byWin&drwNo=$drawNumber"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)

            Toast.makeText(this, "${drawNumber}회차 결과를 확인합니다.", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Toast.makeText(this, "웹 브라우저를 열 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }


    // 현재 액티비티에서 카메라 권한이 부여됐는지 확인
    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    // 카메라 권한을 요청하는 다이얼로그 표시
    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST
        )
    }


    // 사용자에게 권한 요청 다이얼로그를 보여준 후, 사용자가 권한 허용 또는 거부를 선택했을 때 시스템에 의해 호출
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // 먼저 카메라 권한 요청에 대한 결과임을 확인
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            // 결과가 허용되었다면 initializeScanner 함수 호출
            // 거부되었다면 토스트 메시지 표시 및 현재 액티비티 종료
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeScanner()
            } else {
                Toast.makeText(this, "카메라 권한이 필요합니다.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (checkCameraPermission()) {
            barcodeView.resume()
        }
    }

    override fun onPause() {
        super.onPause()
        barcodeView.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        barcodeView.pause()
    }
}