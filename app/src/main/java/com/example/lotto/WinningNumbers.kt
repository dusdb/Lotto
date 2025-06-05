package com.example.lotto

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.lotto.data.LottoWinningNumber
import com.example.lotto.databinding.ActivityLottoResultBinding
import com.example.lotto.databinding.ActivityWinningNumbersBinding
import com.example.lotto.manager.LottoDataManager
import kotlinx.coroutines.launch

class WinningNumbers : AppCompatActivity() {

    private lateinit var etRound: EditText
    private lateinit var etEndRound: EditText
    private lateinit var btnSearch: ImageButton
    private lateinit var layoutWinningNumbers: LinearLayout
    private lateinit var progressBar: ProgressBar

    private val dataManager = LottoDataManager.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding=ActivityWinningNumbersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        setupClickListeners()

        // 초기 데이터 로드 (최근 5회차)
        loadRecentWinningNumbers(5)

        binding.backBtn.setOnClickListener {
            finish()
        }
    }

    private fun initViews() {
        etRound = findViewById(R.id.etRound)
        etEndRound = findViewById(R.id.etEndRound)
        btnSearch = findViewById(R.id.btnSearch)
        layoutWinningNumbers = findViewById(R.id.layoutWinningNumbers)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupClickListeners() {
        btnSearch.setOnClickListener {
            val fromRound = etRound.text.toString().toIntOrNull()
            val toRound = etEndRound.text.toString().toIntOrNull()

            if (fromRound == null || toRound == null) {
                Toast.makeText(this, "회차를 정확히 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (fromRound > toRound) {
                Toast.makeText(this, "시작 회차가 끝 회차보다 클 수 없습니다", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (toRound - fromRound > 50) {
                Toast.makeText(this, "한 번에 최대 50회차까지만 조회할 수 있습니다", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loadWinningNumbers(fromRound, toRound)
        }
    }

    // 최근 N회차 당첨번호 로드
    private fun loadRecentWinningNumbers(count: Int) {
        showLoading(true)

        lifecycleScope.launch {
            try {
                val winningNumbers = dataManager.getRecentWinningNumbers(count)
                displayWinningNumbers(winningNumbers)
            } catch (e: Exception) {
                showError("데이터를 불러오는 중 오류가 발생했습니다: ${e.message}")
            } finally {
                showLoading(false)
            }
        }
    }

    // 지정 범위 당첨번호 로드
    private fun loadWinningNumbers(fromRound: Int, toRound: Int) {
        showLoading(true)

        lifecycleScope.launch {
            try {
                val winningNumbers = dataManager.getWinningNumbers(fromRound, toRound)
                displayWinningNumbers(winningNumbers)

                if (winningNumbers.isNotEmpty()) {
                    Toast.makeText(
                        this@WinningNumbers,
                        "${winningNumbers.size}개 회차 데이터를 불러왔습니다",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                showError("데이터를 불러오는 중 오류가 발생했습니다: ${e.message}")
            } finally {
                showLoading(false)
            }
        }
    }

    // 당첨번호 화면에 표시
    private fun displayWinningNumbers(winningNumbers: List<LottoWinningNumber>) {
        layoutWinningNumbers.removeAllViews()

        if (winningNumbers.isEmpty()) {
            showNoData()
            return
        }

        winningNumbers.forEach { winningNumber ->
            addWinningNumberItem(winningNumber)
        }
    }

    // 당첨번호 아이템 추가
    private fun addWinningNumberItem(winningNumber: LottoWinningNumber) {
        val itemView = LayoutInflater.from(this)
            .inflate(R.layout.activity_lotto_result, layoutWinningNumbers, false)

        val tvRoundInfo = itemView.findViewById<TextView>(R.id.tvRoundInfo)
        val numberViews = listOf(
            itemView.findViewById<TextView>(R.id.tvNum1),
            itemView.findViewById<TextView>(R.id.tvNum2),
            itemView.findViewById<TextView>(R.id.tvNum3),
            itemView.findViewById<TextView>(R.id.tvNum4),
            itemView.findViewById<TextView>(R.id.tvNum5),
            itemView.findViewById<TextView>(R.id.tvNum6)
        )
        val tvBonus = itemView.findViewById<TextView>(R.id.tvBonus)

        // 회차 정보 설정
        tvRoundInfo.text = "${winningNumber.round}회 당첨번호"

        // 일반 번호 설정
        winningNumber.getMainNumbers().forEachIndexed { index, number ->
            numberViews[index].text = number.toString()
            numberViews[index].setBackgroundResource(getColorByNumber(number))
        }

        // 보너스 번호 설정
        tvBonus.text = winningNumber.bonusNumber.toString()
        tvBonus.setBackgroundResource(getColorByNumber(winningNumber.bonusNumber))

        layoutWinningNumbers.addView(itemView)
    }


    private fun getColorByNumber(number: Int): Int {
        return when (number) {
            in 1..10 -> R.drawable.lotto_ball_yellow
            in 11..20 -> R.drawable.lotto_ball_blue
            in 21..30 -> R.drawable.lotto_ball_red
            in 31..40 -> R.drawable.lotto_ball_gray
            in 41..45 -> R.drawable.lotto_ball_green
            else -> R.drawable.lotto_ball_gray
        }
    }

    // 로딩 상태 표시
    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnSearch.isEnabled = !show
    }

    // 데이터 없음 표시
    private fun showNoData() {
        val noDataText = TextView(this).apply {
            text = "해당 회차의 당첨번호가 없습니다"
            textSize = 16f
            setTextColor(resources.getColor(android.R.color.darker_gray, null))
            setPadding(16, 32, 16, 32)
            gravity = android.view.Gravity.CENTER
        }
        layoutWinningNumbers.addView(noDataText)
    }

    // 에러 메시지 표시
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        showNoData()
    }
}