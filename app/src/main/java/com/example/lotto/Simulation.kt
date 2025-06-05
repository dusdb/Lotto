package com.example.lotto

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.lotto.api.LottoApiService
import com.example.lotto.databinding.ActivitySimulationBinding
import com.example.lotto.manager.LottoDataManager
import kotlinx.coroutines.*
import kotlin.random.Random

class Simulation : AppCompatActivity() {

    private lateinit var winningNumbersContainer: LinearLayout
    private lateinit var numberGrid: ScrollView
    private lateinit var numberContainer: LinearLayout
    private lateinit var btnGenerate5: Button
    private lateinit var btnGenerate10: Button
    private lateinit var btnRandomGenerate: Button

    private lateinit var tvFirst: TextView
    private lateinit var tvSecond: TextView
    private lateinit var tvThird: TextView
    private lateinit var tvFourth: TextView
    private lateinit var tvFifth: TextView

    // 당첨 통계
    private var firstCount = 0
    private var secondCount = 0
    private var thirdCount = 0
    private var fourthCount = 0
    private var fifthCount = 0

    // 당첨번호 (예시로 최신 회차 번호 사용)
    private var winningNumbers = listOf(1, 12, 23, 44, 15, 6)
    private var bonusNumber = 33

    // 생성된 번호들 저장
    private val generatedNumbersList = mutableListOf<List<Int>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val binding=ActivitySimulationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        resetStats()
        setupListeners()
        loadLatestWinningNumbers()
        displayWinningNumbers()

        binding.backBtn.setOnClickListener {
            finish()
        }
    }

    private fun initViews() {
        winningNumbersContainer = findViewById(R.id.winningNumbersContainer)
        numberGrid = findViewById(R.id.numberGrid)
        numberContainer = findViewById(R.id.numberContainer)
        btnGenerate5 = findViewById(R.id.btnGenerate5)
        btnGenerate10 = findViewById(R.id.btnGenerate10)
        btnRandomGenerate = findViewById(R.id.btnRandomGenerate)

        tvFirst = findViewById(R.id.tvFirst)
        tvSecond = findViewById(R.id.tvSecond)
        tvThird = findViewById(R.id.tvThird)
        tvFourth = findViewById(R.id.tvFourth)
        tvFifth = findViewById(R.id.tvFifth)
    }

    private fun setupListeners() {
        btnGenerate5.setOnClickListener {
            generateAndCheckNumbers(5)
        }

        btnGenerate10.setOnClickListener {
            generateAndCheckNumbers(10)

        }

        btnRandomGenerate.setOnClickListener {
            // 새로운 당첨번호 로드 후 표시
            generateNewWinningNumbers()
            displayWinningNumbers()
            // 통계 초기화
            resetStats()
            // 생성된 번호들도 초기화
            numberContainer.removeAllViews()
        }
    }

    // 시뮬레이션 당첨번호 생성
    private fun generateNewWinningNumbers() {
        val numbers = mutableSetOf<Int>()
        while (numbers.size < 6) {
            numbers.add(Random.nextInt(1, 46))
        }
        winningNumbers = numbers.toList().sorted()
        bonusNumber = generateBonusNumber(winningNumbers)
    }

    // 시뮬레이션 당첨 보너스 번호 생성
    private fun generateBonusNumber(winningNumbers: List<Int>): Int {
        var bonus: Int
        do {
            bonus = Random.nextInt(1, 46)
        } while (bonus in winningNumbers)
        return bonus
    }

    private fun loadLatestWinningNumbers() {
        // LottoDataManager를 사용하여 실제 당첨번호 가져오기
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val dataManager = LottoDataManager.getInstance()
                val currentRound = dataManager.getCurrentRoundWithTimeThreeTen()
                val latestData = dataManager.getWinningNumber(currentRound)

                latestData?.let { data ->
                    winningNumbers = data.getMainNumbers()
                    bonusNumber = data.bonusNumber
                } ?: run {
                    // API에서 데이터를 가져올 수 없는 경우 랜덤 번호 생성
                    val randomNumbers = (1..45).shuffled().take(6).sorted()
                    winningNumbers = randomNumbers
                    bonusNumber = (1..45).filter { !randomNumbers.contains(it) }.random()
                }
            } catch (e: Exception) {
                // 에러 발생 시 랜덤 번호 생성
                val randomNumbers = (1..45).shuffled().take(6).sorted()
                winningNumbers = randomNumbers
                bonusNumber = (1..45).filter { !randomNumbers.contains(it) }.random()
            }
        }
    }

    private fun displayWinningNumbers() {
        winningNumbersContainer.removeAllViews()

        // 당첨번호 표시
        winningNumbers.forEach { number ->
            val numberView = createNumberView(number)
            winningNumbersContainer.addView(numberView)
        }

        // 플러스 기호 추가
        val plusView = TextView(this).apply {
            text = "+"
            textSize = 16f
            setTextColor(Color.BLACK)
            setPadding(16, 0, 16, 0)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(8, 0, 8, 0)
            }
            layoutParams = params
        }
        winningNumbersContainer.addView(plusView)

        // 보너스번호 표시
        val bonusView = createNumberView(bonusNumber)
        winningNumbersContainer.addView(bonusView)
    }

    // 5,10회 뽑기 메서드
    private fun generateAndCheckNumbers(count: Int) {
        generatedNumbersList.clear()
        numberContainer.removeAllViews()

        repeat(count) {
            val numbers = generateLottoNumbers()
            generatedNumbersList.add(numbers)
            checkWinning(numbers)
        }

        displayGeneratedNumbers()
        updateWinningStats()
    }


    // 당첨번호 기반으로 분석하여 번호 생성(핵심)
    private fun generateLottoNumbers(): List<Int> {
        // 분석 기반 번호 생성 (가중치 적용)
        val weightedNumbers = mutableMapOf<Int, Double>()

        // 기본 가중치 설정
        for (i in 1..45) {
            weightedNumbers[i] = 1.0
        }

        // 최근 당첨번호 주변 숫자에 가중치 부여
        winningNumbers.forEach { num ->
            weightedNumbers[num] = weightedNumbers[num]!! * 1.2
            if (num > 1) weightedNumbers[num - 1] = weightedNumbers[num - 1]!! * 1.1
            if (num < 45) weightedNumbers[num + 1] = weightedNumbers[num + 1]!! * 1.1
        }

        // 가중치 기반 번호 선택
        val selectedNumbers = mutableListOf<Int>()
        val availableNumbers = weightedNumbers.keys.toMutableList()

        while (selectedNumbers.size < 6) {
            val totalWeight = availableNumbers.sumOf { weightedNumbers[it] ?: 0.0 }
            val randomValue = Random.nextDouble() * totalWeight

            var currentWeight = 0.0
            for (number in availableNumbers) {
                currentWeight += weightedNumbers[number] ?: 0.0
                if (randomValue <= currentWeight) {
                    selectedNumbers.add(number)
                    availableNumbers.remove(number)
                    break
                }
            }
        }

        return selectedNumbers
    }

    // 당첨 횟수 체크
    private fun checkWinning(numbers: List<Int>): Int {
        val matchCount = numbers.intersect(winningNumbers.toSet()).size
        val bonusMatch = numbers.contains(bonusNumber)

        return when {
            matchCount == 6 -> {
                firstCount++
                1
            }
            matchCount == 5 && bonusMatch -> {
                secondCount++
                2
            }
            matchCount == 5 -> {
                thirdCount++
                3
            }
            matchCount == 4 -> {
                fourthCount++
                4
            }
            matchCount == 3 -> {
                fifthCount++
                5
            }
            else -> 0
        }
    }

    private fun displayGeneratedNumbers() {
        numberContainer.removeAllViews() // ScrollView가 아닌 내부 컨테이너에서 제거
        generatedNumbersList.forEachIndexed { index, numbers ->
            val rank = checkWinning(numbers)
            addNumberRowToGrid(numbers, rank)
        }
    }

    private fun addNumberRowToGrid(numbers: List<Int>, rank: Int) {
        val rowContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 8)
            }
        }

        // 번호들을 담을 컨테이너 (왼쪽 정렬)
        val numbersContainer = LinearLayout(this).apply {
            id = View.generateViewId()
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                addRule(RelativeLayout.ALIGN_PARENT_START)
                addRule(RelativeLayout.CENTER_VERTICAL)
            }
        }

        // 번호들 추가
        numbers.forEach { number ->
            val isWinning = winningNumbers.contains(number)
            val color = getNumberColor(number) // 항상 해당 번호에 맞는 색상 사용
            val numberView = createSmallNumberView(number, color, isWinning)
            numbersContainer.addView(numberView)
        }

        rowContainer.addView(numbersContainer)

        // 등수 표시 (1~5등일 때만, 오른쪽 정렬)
        if (rank in 1..5) {
            val rankView = TextView(this).apply {
                text = "${rank}등"
                textSize = 14f
                setTextColor(Color.BLACK)
                gravity = Gravity.CENTER
                // 패딩으로 여백 확보
                setPadding(12, 8, 12, 8)
                layoutParams = RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    addRule(RelativeLayout.ALIGN_PARENT_END)
                    addRule(RelativeLayout.CENTER_VERTICAL)
                    marginEnd = (20 * resources.displayMetrics.density).toInt()
                }
            }
            rowContainer.addView(rankView)
        }

        numberContainer.addView(rowContainer)
    }

    // 당첨번호를 담을 뷰 생성
    private fun createNumberView(number: Int): TextView {
        // 당첨 여부 확인 (winningNumbers는 당첨번호 리스트)
        val isWinning = number in winningNumbers || number == bonusNumber

        return TextView(this).apply {
            text = number.toString()
            textSize = 12f
            setTextColor(if (isWinning) Color.WHITE else Color.BLACK)
            gravity = android.view.Gravity.CENTER

            // 텍스트가 잘리지 않도록 패딩 추가
            setPadding(4, 4, 4, 4)

            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                if (isWinning) {
                    // 당첨번호일 때 번호 범위에 맞는 색상 사용
                    setColor(getNumberColor(number))
                } else {
                    setColor(Color.WHITE)
                    setStroke(2, Color.GRAY)
                }
            }
            background = drawable

            // 밀도 독립적인 크기 설정
            val size = (35 * resources.displayMetrics.density).toInt()
            val params = LinearLayout.LayoutParams(size, size).apply {
                setMargins(8, 8, 8, 8)
            }
            layoutParams = params

            // 최소 크기 보장으로 텍스트 짤림 방지
            minWidth = size
            minHeight = size
        }
    }

    // 시뮬레이션 번호를 담을 뷰 생성
    private fun createSmallNumberView(number: Int, color: Int, isWinning: Boolean): TextView {
        return TextView(this).apply {
            text = number.toString()
            textSize = 12f
            setTextColor(if (isWinning) Color.WHITE else Color.BLACK)
            gravity = android.view.Gravity.CENTER

            // 텍스트 짤림 방지를 위한 패딩
            setPadding(2, 2, 2, 2)

            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                if (isWinning) {
                    setColor(getNumberColor(number))
                } else {
                    setColor(Color.WHITE)
                    setStroke(2, Color.GRAY)
                }
            }
            background = drawable

            val size = (35 * resources.displayMetrics.density).toInt()
            val params = LinearLayout.LayoutParams(size, size).apply {
                setMargins(10, 10, 10, 10)
            }
            layoutParams = params
        }
    }

    private fun getNumberColor(number: Int): Int {
        return when {
            number in 1..10 -> Color.parseColor("#FFA726")  // 주황색
            number in 11..20 -> Color.parseColor("#42A5F5") // 파란색
            number in 21..30 -> Color.parseColor("#EF5350") // 빨간색
            number in 31..40 -> Color.parseColor("#66BB6A") // 초록색
            else -> Color.parseColor("#AB47BC")              // 보라색
        }
    }

    private fun updateWinningStats() {
        tvFirst.text = "${firstCount}회 당첨"
        tvSecond.text = "${secondCount}회 당첨"
        tvThird.text = "${thirdCount}회 당첨"
        tvFourth.text = "${fourthCount}회 당첨"
        tvFifth.text = "${fifthCount}회 당첨"
    }

    private fun resetStats() {
        firstCount = 0
        secondCount = 0
        thirdCount = 0
        fourthCount = 0
        fifthCount = 0
        updateWinningStats()
    }
}