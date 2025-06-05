package com.example.lotto

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.lotto.data.LottoWinningNumber
import com.example.lotto.databinding.ActivityMainBinding
import com.example.lotto.manager.LottoDataManager
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val dataManager = LottoDataManager.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val binding=ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val autoGenerate= Intent(this, AutoGenerate::class.java)
        binding.AutoGenerate.setOnClickListener{
            startActivity(autoGenerate)
        }

        val manualGenerate= Intent(this, ManualGenerate::class.java)
        binding.ManualGenerate.setOnClickListener{
            startActivity(manualGenerate)
        }

        val winGenerate= Intent(this, WinningNumbers::class.java)
        binding.WinNumbers.setOnClickListener{
            startActivity(winGenerate)
        }

        val simulation= Intent(this, Simulation::class.java)
        binding.cardSimulation.setOnClickListener{
            startActivity(simulation)
        }

        val statistics= Intent(this, StatisticsAnalysis::class.java)
        binding.StatisticsNumbers.setOnClickListener{
            startActivity(statistics)
        }

        val qrscan=Intent(this,QRScan::class.java)
        binding.cardQRCode.setOnClickListener{
            startActivity(qrscan)
        }

        val currRound=dataManager.getCurrentRoundWithTimeThreeTen()

        loadRecentWinningNumbers(currRound)
    }

    // 당첨번호 아이템 추가
    private fun addWinningNumberItem(winningNumber: LottoWinningNumber) {
        val info=findViewById<TextView>(R.id.tvRoundInfo)
        val num1=findViewById<TextView>(R.id.tvNum1)
        val num2=findViewById<TextView>(R.id.tvNum2)
        val num3=findViewById<TextView>(R.id.tvNum3)
        val num4=findViewById<TextView>(R.id.tvNum4)
        val num5=findViewById<TextView>(R.id.tvNum5)
        val num6=findViewById<TextView>(R.id.tvNum6)
        val bonusNum=findViewById<TextView>(R.id.tvBonus)


        info.setText("${winningNumber.round}회 당첨번호")


        val Num = winningNumber.getAllNumbers().toMutableList()
        num1.setText(Num[0].toString())
        num2.setText(Num[1].toString())
        num3.setText(Num[2].toString())
        num4.setText(Num[3].toString())
        num5.setText(Num[4].toString())
        num6.setText(Num[5].toString())
        bonusNum.setText(Num[6].toString())


        num1.setBackgroundResource(getColorByNumber(Integer.parseInt(num1.text.toString())))
        num2.setBackgroundResource(getColorByNumber(Integer.parseInt(num2.text.toString())))
        num3.setBackgroundResource(getColorByNumber(Integer.parseInt(num3.text.toString())))
        num4.setBackgroundResource(getColorByNumber(Integer.parseInt(num4.text.toString())))
        num5.setBackgroundResource(getColorByNumber(Integer.parseInt(num5.text.toString())))
        num6.setBackgroundResource(getColorByNumber(Integer.parseInt(num6.text.toString())))
        bonusNum.setBackgroundResource(getColorByNumber(Integer.parseInt(bonusNum.text.toString())))

    }


    // 최근 N회차 당첨번호 로드(핵심)
    private fun loadRecentWinningNumbers(count: Int) {
        lifecycleScope.launch {
            try {
                val winningNumbers = dataManager.getWinningNumber(count)
                // 당첨번호가 null이 아니면 해당 당첨번호를 추가
                winningNumbers?.let { addWinningNumberItem(winningNumbers) }
            } catch (e: Exception) {
                showError("${count}회차 데이터를 불러오는 중 오류: ${e.message}")
            }
        }
    }

    private fun getColorByNumber(number: Int): Int {
        return when (number) {
            in 1..10 -> R.drawable.lotto_ball_yellow
            in 11..20 -> R.drawable.lotto_ball_blue
            in 21..30 -> R.drawable.lotto_ball_red
            in 31..40 -> R.drawable.lotto_ball_gray
            in 41..45 -> R.drawable.lotto_ball_green
            else -> R.drawable.lotto_ball_empty
        }
    }

    // 에러 메시지 표시
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}