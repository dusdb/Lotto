package com.example.lotto.data

// API 응답 데이터 모델
data class LottoApiResponse(
    val returnValue: String,
    val drwNoDate: String,
    val drwNo: Int,
    val drwtNo1: Int,
    val drwtNo2: Int,
    val drwtNo3: Int,
    val drwtNo4: Int,
    val drwtNo5: Int,
    val drwtNo6: Int,
    val bnusNo: Int,
    val firstAccumamnt: Long,
    val firstPrzwnerCo: Int,
    val firstWinamnt: Long
)

// 가공된 당첨번호 데이터 모델
data class LottoWinningNumber(
    val round: Int,
    val date: String,
    val numbers: List<Int>,
    val bonusNumber: Int,
    val prizeMoney: Long = 0L,
    val winnerCount: Int = 0
) {
    // 일반 번호만 반환
    fun getMainNumbers(): List<Int> = numbers

    // 모든 번호 (일반 + 보너스) 반환
    fun getAllNumbers(): List<Int> = numbers + bonusNumber

    // 번호를 문자열로 반환
    fun getNumbersAsString(): String = numbers.joinToString(", ")

    // 날짜를 포맷팅해서 반환
    fun getFormattedDate(): String {
        return try {
            val parts = date.split("-")
            "${parts[0]}년 ${parts[1]}월 ${parts[2]}일"
        } catch (e: Exception) {
            date
        }
    }
}