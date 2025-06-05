package com.example.lotto

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.lotto.databinding.ActivityManualGenerateBinding

class ManualGenerate : AppCompatActivity() {
    private val selectedNumbers = mutableListOf<Int>()
    private val maxSelectionCount = 6

    // 선택된 번호 표시 TextViews
    private lateinit var selectedNumberViews: Array<TextView>
    private lateinit var selectionCountView: TextView

    // 번호 선택 버튼들
    private val numberButtons = mutableMapOf<Int, TextView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val binding=ActivityManualGenerateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 선택된 번호 표시 텍스트 뷰 초기화 메서드
        initViews()
        // 1~45번호 버튼을 매핑하고 이벤트 리스너 설정
        setupNumberButtons()
        // 리셋 버튼 설정
        setupResetButton()

        // 돌아가기 이벤트
        binding.backBtn.setOnClickListener {
            finish()
        }
    }

    private fun initViews() {
        // 선택된 번호 표시 TextViews 배열로 초기화
        selectedNumberViews = arrayOf(
            findViewById(R.id.tv_selected1),
            findViewById(R.id.tv_selected2),
            findViewById(R.id.tv_selected3),
            findViewById(R.id.tv_selected4),
            findViewById(R.id.tv_selected5),
            findViewById(R.id.tv_selected6)
        )

        selectionCountView = findViewById(R.id.tv_selection_count)
    }

    private fun setupNumberButtons() {
        // 1부터 45까지의 번호 버튼들을 찾아서 매핑
        for (i in 1..45) {
            val buttonId = resources.getIdentifier("btn_num_$i", "id", packageName)
            if (buttonId != 0) {
                val button = findViewById<TextView>(buttonId)
                numberButtons[i] = button
                // 각 버튼에 클릭 리스너 설정하여 해당 번호로 onNumberClick 호출
                button.setOnClickListener { onNumberClick(i) }
            }
        }
    }

    // 리셋 버튼에 클릭 리스너 설정하여 resetSelection 호출
    private fun setupResetButton() {
        val resetButton = findViewById<Button>(R.id.btnReset)
        resetButton.setOnClickListener { resetSelection() }
    }

    private fun onNumberClick(number: Int) {
        if (selectedNumbers.contains(number)) {
            // 이미 선택된 번호라면 deselectNumber로 선택 해제
            deselectNumber(number)
        } else {
            // 새로운 번호 선택
            // 선택된 번호 개수가 최대 선택 개수를 넘지 않으면 selectedNumber로 선택된 번호에 할당
            // 최대 선택 개수를 넘으면 토스트 메시지 표시
            if (selectedNumbers.size < maxSelectionCount) {
                selectNumber(number)
            } else {
                Toast.makeText(this, "최대 6개까지만 선택할 수 있습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 위에 초기화된 selectedNumbers(Set)에 추가
    private fun selectNumber(number: Int) {
        selectedNumbers.add(number)
        updateNumberButtonAppearance(number, true)
        updateSelectedNumbersDisplay()
        updateSelectionCount()
    }

    // 위에 초기화된 selectedNumbers(Set)에서 삭제
    private fun deselectNumber(number: Int) {
        selectedNumbers.remove(number)
        updateNumberButtonAppearance(number, false)
        updateSelectedNumbersDisplay()
        updateSelectionCount()
    }

    // 버튼 외관 업데이트
    private fun updateNumberButtonAppearance(number: Int, isSelected: Boolean) {
        val button = numberButtons[number] ?: return

        if (isSelected) {
            // 선택된 상태: 번호에 따른 색상 적용
            // getNumberBackgroundResource로 번호에 맞는 색을 가져옴
            // setBackgroundResource로 해당 색을 설정
            val backgroundRes = getNumberBackgroundResource(number)
            button.setBackgroundResource(backgroundRes)
            button.setTextColor(ContextCompat.getColor(this, R.color.white))
        } else {
            // 선택 해제된 상태: 기본 배경으로 복원
            button.setBackgroundResource(R.drawable.lotto_ball_empty)
            button.setTextColor(ContextCompat.getColor(this, R.color.text_gray))
        }
    }

    // 번호별 배경 리소스 반환
    private fun getNumberBackgroundResource(number: Int): Int {
        return when (number) {
            in 1..10 -> R.drawable.lotto_ball_yellow
            in 11..20 -> R.drawable.lotto_ball_blue
            in 21..30 -> R.drawable.lotto_ball_red
            in 31..40 -> R.drawable.lotto_ball_gray
            in 41..45 -> R.drawable.lotto_ball_green
            else -> R.drawable.lotto_ball_empty
        }
    }

    private fun updateSelectedNumbersDisplay() {
        // 선택된 번호들은 index와 비교하여 selectedNumbers 순서대로 위에 표시
        // 선택 취소된 번호들은 위에 표시가 사라지고 기본으로 복원
        selectedNumberViews.forEachIndexed { index, textView ->
            if (index < selectedNumbers.size) {
                val number = selectedNumbers[index]
                textView.text = number.toString()
                textView.setBackgroundResource(getNumberBackgroundResource(number))
                textView.setTextColor(ContextCompat.getColor(this, R.color.black))

            } else {
                textView.text = "?"
                textView.setBackgroundResource(R.drawable.lotto_ball_empty)
                textView.setTextColor(ContextCompat.getColor(this, R.color.text_gray))
            }
        }
    }

    // 선택된 번호 카운트
    private fun updateSelectionCount() {
        selectionCountView.text = "${selectedNumbers.size}/$maxSelectionCount 선택됨"
    }

    private fun resetSelection() {
        // 모든 선택 해제
        // 선택된 번호를 리스트로 가져옴
        // 반복문으로 각 번호를 false로 선택 해제
        val numbersToRemove = selectedNumbers.toList()
        numbersToRemove.forEach { number ->
            updateNumberButtonAppearance(number, false)
        }
        selectedNumbers.clear()

        // 화면 업데이트
        updateSelectedNumbersDisplay()
        updateSelectionCount()

        Toast.makeText(this, "선택이 초기화되었습니다.", Toast.LENGTH_SHORT).show()
    }
}