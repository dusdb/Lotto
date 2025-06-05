package com.example.lotto

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.setMargins
import androidx.lifecycle.lifecycleScope
import com.example.lotto.data.LottoWinningNumber
import com.example.lotto.databinding.ActivityStatisticsAnalysisBinding
import com.example.lotto.manager.LottoDataManager
import kotlinx.coroutines.launch
import kotlin.random.Random

class StatisticsAnalysis : AppCompatActivity() {

    private lateinit var etFromRound: EditText
    private lateinit var etToRound: EditText
    private lateinit var btnAnalyze: Button
    private lateinit var btnGenerate: Button
    private lateinit var progressBar: ProgressBar

    private lateinit var cardStatistics: CardView
    private lateinit var cardRecommendedNumbers: CardView
    private lateinit var cardFrequencyAnalysis: CardView

    private lateinit var tvAnalysisInfo: TextView
    private lateinit var layoutRecommendedNumbers: LinearLayout
    private lateinit var layoutFrequentNumbers: LinearLayout
    private lateinit var layoutRareNumbers: LinearLayout

    private val dataManager = LottoDataManager.getInstance()
    private var analysisData: List<LottoWinningNumber> = emptyList()
    private var numberFrequency: Map<Int, Int> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding=ActivityStatisticsAnalysisBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        setupClickListeners()
        setInitialValues()

        binding.backBtn.setOnClickListener {
            finish()
        }
    }

    private fun initViews() {
        etFromRound = findViewById(R.id.etFromRound)
        etToRound = findViewById(R.id.etToRound)
        btnAnalyze = findViewById(R.id.btnAnalyze)
        btnGenerate = findViewById(R.id.btnGenerate)
        progressBar = findViewById(R.id.progressBar)

        cardStatistics = findViewById(R.id.cardStatistics)
        cardRecommendedNumbers = findViewById(R.id.cardRecommendedNumbers)
        cardFrequencyAnalysis = findViewById(R.id.cardFrequencyAnalysis)

        tvAnalysisInfo = findViewById(R.id.tvAnalysisInfo)
        layoutRecommendedNumbers = findViewById(R.id.layoutRecommendedNumbers)
        layoutFrequentNumbers = findViewById(R.id.layoutFrequentNumbers)
        layoutRareNumbers = findViewById(R.id.layoutRareNumbers)
    }

    // 분석/생성 버튼 리스너 연결
    private fun setupClickListeners() {
        btnAnalyze.setOnClickListener {
            performAnalysis()
        }

        btnGenerate.setOnClickListener {
            generateRecommendedNumbers()
        }
    }

    // 회차 텍스트뷰에 초기값 설정
    private fun setInitialValues() {
        val currentRound = dataManager.getCurrentRoundWithTimeThreeTen()
        etFromRound.setText("1")
        etToRound.setText(currentRound.toString())
    }

    // 분석 클릭 메서드
    private fun performAnalysis() {
        val fromRoundStr = etFromRound.text.toString()
        val toRoundStr = etToRound.text.toString()

        if (fromRoundStr.isBlank() || toRoundStr.isBlank()) {
            Toast.makeText(this, "회차를 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val fromRound = fromRoundStr.toIntOrNull()
        val toRound = toRoundStr.toIntOrNull()

        if (fromRound == null || toRound == null) {
            Toast.makeText(this, "올바른 회차를 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        if (fromRound > toRound) {
            Toast.makeText(this, "시작 회차가 끝 회차보다 클 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        if (toRound > dataManager.getCurrentRoundWithTimeThreeTen()) {
            Toast.makeText(this, "현재 회차를 초과할 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                showLoading(true)

                // 데이터 가져오기(당첨번호, 번호별 빈도)
                analysisData = dataManager.getWinningNumbers(fromRound, toRound)
                numberFrequency = dataManager.getNumberFrequency(fromRound, toRound)

                if (analysisData.isEmpty()) {
                    Toast.makeText(this@StatisticsAnalysis, "분석할 데이터가 없습니다.", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // 분석 결과와 출현빈도 표시
                displayAnalysisResults(fromRound, toRound)
                displayFrequencyAnalysis()

                // 카드들 보이기
                cardStatistics.visibility = View.VISIBLE
                cardRecommendedNumbers.visibility = View.VISIBLE
                cardFrequencyAnalysis.visibility = View.VISIBLE

            } catch (e: Exception) {
                Toast.makeText(this@StatisticsAnalysis, "분석 중 오류가 발생했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    // 분석 결과 조회 메서드
    private fun displayAnalysisResults(fromRound: Int, toRound: Int) {
        val totalRounds = analysisData.size
        val expectedRounds = toRound - fromRound + 1
        val analysisInfo = """
            분석 구간: ${fromRound}회차 ~ ${toRound}회차
            분석된 회차 수: ${totalRounds}회차 (총 ${expectedRounds}회차 중)
        """.trimIndent()

        tvAnalysisInfo.text = analysisInfo
    }

    // 번호별 출현빈도 조회 메서드
    private fun displayFrequencyAnalysis() {
        // 출현빈도 정렬 후 많이 나온 번호 10개와 적게 나온 번호 10개 각각 저장
        val sortedByFrequency = numberFrequency.toList().sortedByDescending { it.second }
        val frequentNumbers = sortedByFrequency.take(10)
        val rareNumbers = sortedByFrequency.takeLast(10).reversed()

        displayFrequencyNumbers(layoutFrequentNumbers, frequentNumbers)
        displayFrequencyNumbers(layoutRareNumbers, rareNumbers)
    }

    // 빈도 번호 구조 생성 메서드 (핵심)
    private fun displayFrequencyNumbers(container: LinearLayout, numbers: List<Pair<Int, Int>>) {
        // 기존 컨테이너에 있던 데이터 삭제
        container.removeAllViews()

        // GridLayout 스타일로 구현
        val ballSize = (36 * resources.displayMetrics.density).toInt() // 36dp로 크기 축소
        val maxItemsPerRow = 5 // 한 줄에 최대 5개씩

        var currentRow: LinearLayout? = null
        var itemsInCurrentRow = 0

        numbers.forEachIndexed { index, (number, frequency) ->
            // 새 행이 필요한 경우
            if (currentRow == null || itemsInCurrentRow >= maxItemsPerRow) {
                currentRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.START
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        if (index > 0) topMargin = (8 * resources.displayMetrics.density).toInt()
                    }
                }
                container.addView(currentRow)
                itemsInCurrentRow = 0
            }

            val itemView = createFrequencyItemView(number, frequency, ballSize)
            currentRow?.addView(itemView)
            itemsInCurrentRow++
        }
    }

    // 빈도 아이템 뷰 생성 메서드
    private fun createFrequencyItemView(number: Int, frequency: Int, ballSize: Int): View {
        // 메인 컨테이너 생성
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            val margin = (4 * resources.displayMetrics.density).toInt()
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(margin, margin, margin, margin)
            }
            layoutParams = params
        }

        // 로또공 컨테이너 생성
        val ballContainer = RelativeLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(ballSize, ballSize)
        }

        // 배경 View (로또공) 생성
        val ballBackground = View(this).apply {
            layoutParams = RelativeLayout.LayoutParams(ballSize, ballSize)
        }

        // 번호 텍스트 생성
        val tvNumber = TextView(this).apply {
            val params = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            )
            params.addRule(RelativeLayout.CENTER_IN_PARENT)
            layoutParams = params
            text = number.toString()
            setTextColor(getColor(android.R.color.white))
            textSize = 12f // 크기에 맞게 텍스트 크기 조정
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        // 빈도 텍스트 생성
        val tvFrequency = TextView(this).apply {
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = (2 * resources.displayMetrics.density).toInt()
            layoutParams = params
            text = "${frequency}회"
            setTextColor(getColor(android.R.color.darker_gray))
            textSize = 10f // 더 작은 텍스트 크기
            gravity = android.view.Gravity.CENTER
        }

        // 번호에 따른 공 색상 설정
        val colorResource = when (number) {
            in 1..10 -> R.drawable.lotto_ball_yellow
            in 11..20 -> R.drawable.lotto_ball_blue
            in 21..30 -> R.drawable.lotto_ball_red
            in 31..40 -> R.drawable.lotto_ball_gray
            else -> R.drawable.lotto_ball_green
        }
        ballBackground.setBackgroundResource(colorResource)

        // 뷰들 조립
        ballContainer.addView(ballBackground)
        ballContainer.addView(tvNumber)
        container.addView(ballContainer)
        container.addView(tvFrequency)

        return container
    }

    // 추천번호 생성하기 메서드
    private fun generateRecommendedNumbers() {
        if (numberFrequency.isEmpty()) {
            Toast.makeText(this, "먼저 분석을 실행해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val recommendedNumbers = generateSmartNumbers()
        displayRecommendedNumbers(recommendedNumbers)
    }

    // 추천 번호 통계 분석 메서드(핵심)
    // 고빈도 50%, 중빈도 30%, 저빈도 20%
    private fun generateSmartNumbers(): List<Int> {
        val numbers = mutableListOf<Int>()
        val sortedByFrequency = numberFrequency.toList().sortedByDescending { it.second }

        // 상위 빈도 번호들 (가중치 높음)
        val highFreqNumbers = sortedByFrequency.take(15).map { it.first }
        // 중간 빈도 번호들
        val midFreqNumbers = sortedByFrequency.drop(15).take(15).map { it.first }
        // 하위 빈도 번호들 (가중치 낮음)
        val lowFreqNumbers = sortedByFrequency.takeLast(15).map { it.first }

        // 가중 확률로 번호 선택
        repeat(6) {
            var selectedNumber: Int
            do {
                selectedNumber = when (Random.nextInt(100)) {
                    in 0..49 -> highFreqNumbers.random() // 50% 확률로 고빈도 번호
                    in 50..79 -> midFreqNumbers.random() // 30% 확률로 중빈도 번호
                    else -> lowFreqNumbers.random() // 20% 확률로 저빈도 번호
                }
            } while (numbers.contains(selectedNumber))

            numbers.add(selectedNumber)
        }

        return numbers
    }

    // 추천 번호 생성 시 화면에 추가하는 메서드
    private fun displayRecommendedNumbers(numbers: List<Int>) {
        layoutRecommendedNumbers.removeAllViews()

        numbers.forEach { number ->
            val ballView = createLottoBallView(number)
            layoutRecommendedNumbers.addView(ballView)
        }
    }

    // 추천 번호 공을 만들어주는 메서드
    private fun createLottoBallView(number: Int): View {
        // RelativeLayout 생성 (컨테이너)
        val container = RelativeLayout(this)
        val ballSize = (40 * resources.displayMetrics.density).toInt() // 40dp
        val horizontalMargin  = (6 * resources.displayMetrics.density).toInt() // 6dp

        // 배경 View (로또공) 생성
        val ballBackground = View(this)
        val ballParams = RelativeLayout.LayoutParams(ballSize, ballSize)
        ballBackground.layoutParams = ballParams

        // 번호에 따른 공 색상 설정
        val colorResource = when (number) {
            in 1..10 -> R.drawable.lotto_ball_yellow
            in 11..20 -> R.drawable.lotto_ball_blue
            in 21..30 -> R.drawable.lotto_ball_red
            in 31..40 -> R.drawable.lotto_ball_gray
            else -> R.drawable.lotto_ball_green
        }
        ballBackground.setBackgroundResource(colorResource)

        // 번호 텍스트 생성
        val tvNumber = TextView(this)
        val textParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        textParams.addRule(RelativeLayout.CENTER_IN_PARENT)
        tvNumber.layoutParams = textParams
        tvNumber.text = number.toString()
        tvNumber.setTextColor(getColor(android.R.color.white))
        tvNumber.textSize = 16f
        tvNumber.setTypeface(null, android.graphics.Typeface.BOLD)

        // 컨테이너에 뷰들 추가
        container.addView(ballBackground)
        container.addView(tvNumber)

        // 컨테이너 레이아웃 파라미터 설정
        val containerParams = LinearLayout.LayoutParams(ballSize, ballSize)
        containerParams.setMargins(horizontalMargin, 0, horizontalMargin, 0)
        container.layoutParams = containerParams

        return container
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnAnalyze.isEnabled = !show
        btnGenerate.isEnabled = !show && numberFrequency.isNotEmpty()
    }
}