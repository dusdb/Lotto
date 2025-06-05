package com.example.lotto

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.lotto.databinding.ActivityAutoGenerateBinding
import kotlin.random.Random

class AutoGenerate : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val binding= ActivityAutoGenerateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        val Num:Array<Int> = Array(6){0}

        // 자동 번호 생성 이벤트
        binding.btnGenerate.setOnClickListener {
            for(i in 0..5){
                val numbers = Random.nextInt(1, 46)
                Num[i]=numbers
            }

            // 로또 공 번호 랜덤 설정
            binding.firstBall.setText(Num[0].toString())
            binding.secondBall.setText(Num[1].toString())
            binding.thirdBall.setText(Num[2].toString())
            binding.fourthBall.setText(Num[3].toString())
            binding.fifthBall.setText(Num[4].toString())
            binding.sixthBall.setText(Num[5].toString())

            // 로또 공 번호별 색상 변경 함수
            // 로또 번호를 매개변수로 받아 조건으로 색상을 설정하여 적용
            fun TextView.setLottoNumber(number: Int) {
                this.text = number.toString()
                val backgroundResource = when (number) {
                    in 1..10 -> R.drawable.lotto_ball_yellow
                    in 11..20 -> R.drawable.lotto_ball_blue
                    in 21..30 -> R.drawable.lotto_ball_red
                    in 31..40 -> R.drawable.lotto_ball_gray
                    in 41..45 -> R.drawable.lotto_ball_green
                    else -> R.drawable.lotto_ball_orange
                }
                this.setBackgroundResource(backgroundResource)
            }

            binding.firstBall.setLottoNumber(Num[0])
            binding.secondBall.setLottoNumber(Num[1])
            binding.thirdBall.setLottoNumber(Num[2])
            binding.fourthBall.setLottoNumber(Num[3])
            binding.fifthBall.setLottoNumber(Num[4])
            binding.sixthBall.setLottoNumber(Num[5])

        }

        // 돌아가기 이벤트
        binding.backBtn.setOnClickListener {
            finish()
        }
    }
}