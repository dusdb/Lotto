package com.example.lotto.manager

import android.app.Application
import com.example.lotto.api.LottoApiService
import com.example.lotto.data.LottoWinningNumber
import com.jakewharton.threetenabp.AndroidThreeTen
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import org.threeten.bp.temporal.ChronoUnit
import kotlinx.coroutines.*

class LottoDataManager : Application() {

    private val apiService = LottoApiService.getInstance()
    private val cachedData = mutableMapOf<Int, LottoWinningNumber>()


    override fun onCreate() {
        super.onCreate()
        AndroidThreeTen.init(this)
    }


    companion object {
        @Volatile
        private var INSTANCE: LottoDataManager? = null

        fun getInstance(): LottoDataManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LottoDataManager().also { INSTANCE = it }
            }
        }
    }

    // 단일 회차 데이터 가져오기 (캐시 우선)
    suspend fun getWinningNumber(round: Int): LottoWinningNumber? {
        // 캐시에 있으면 캐시에서 반환
        cachedData[round]?.let { return it }

        // 캐시에 없으면 API에서 가져와서 캐시에 저장
        val data = apiService.getWinningNumber(round)
        data?.let { cachedData[round] = it }

        return data
    }

    // 범위 내 데이터 가져오기
    suspend fun getWinningNumbers(fromRound: Int, toRound: Int): List<LottoWinningNumber> {
        val results = mutableListOf<LottoWinningNumber>()
        val roundsToFetch = mutableListOf<Int>()

        // 캐시에서 먼저 찾기
        for (round in fromRound..toRound) {
            cachedData[round]?.let {
                results.add(it)
            } ?: run {
                roundsToFetch.add(round)
            }
        }

        // 캐시에 없는 데이터만 API에서 가져오기
        if (roundsToFetch.isNotEmpty()) {
            val newData = apiService.getWinningNumbers(roundsToFetch.min(), roundsToFetch.max())
            newData.forEach { data ->
                cachedData[data.round] = data
                results.add(data)
            }
        }

        // 결과를 회차 순서대로 정렬 (최신순)
        return results.filter { it.round in fromRound..toRound }
            .sortedByDescending { it.round }
    }

    // 최근 N회차 데이터 가져오기
    suspend fun getRecentWinningNumbers(count: Int): List<LottoWinningNumber> {
        val latestRound = getCurrentRoundWithTimeThreeTen()
        val fromRound = latestRound - count + 1
        return getWinningNumbers(fromRound, latestRound)
    }

    fun getCurrentRoundThreeTen(): Int {
        val today = LocalDate.now()
        // 로또 1회차 추첨일: 2002년 12월 7일 (토요일) -> 실제로는 12월 2일이었음
        val firstLottoDate = LocalDate.of(2002, 12, 2)
        val weeksBetween = ChronoUnit.WEEKS.between(firstLottoDate, today)
        return (weeksBetween + 1).toInt()
    }

    fun getCurrentRoundWithTimeThreeTen(): Int {
        val today = LocalDate.now()
        val thisWeekDrawDate = getThisWeekDrawDateThreeTen()
        val baseRound = getCurrentRoundThreeTen()

        return if (today.isAfter(thisWeekDrawDate) ||
            (today.isEqual(thisWeekDrawDate) && isAfterDrawTimeThreeTen())) {
            baseRound
        } else {
            baseRound - 1
        }
    }

    private fun getThisWeekDrawDateThreeTen(): LocalDate {
        val today = LocalDate.now()
        val dayOfWeek = today.dayOfWeek.value

        return when {
            dayOfWeek == 6 -> today
            dayOfWeek == 7 -> today.minusDays(1)
            else -> today.plusDays((6 - dayOfWeek).toLong())
        }
    }

    private fun isAfterDrawTimeThreeTen(): Boolean {
        val now = LocalTime.now()
        val drawTime = LocalTime.of(20, 45)
        return now.isAfter(drawTime)
    }

    // 캐시 초기화
    fun clearCache() {
        cachedData.clear()
    }

    // 캐시된 데이터 개수 반환
    fun getCacheSize(): Int = cachedData.size

    // 특정 번호가 나온 회차들 찾기
    suspend fun findRoundsWithNumber(number: Int, fromRound: Int, toRound: Int): List<LottoWinningNumber> {
        val allData = getWinningNumbers(fromRound, toRound)
        return allData.filter { data ->
            data.getAllNumbers().contains(number)
        }
    }

    // 번호별 출현 빈도 계산
    suspend fun getNumberFrequency(fromRound: Int, toRound: Int): Map<Int, Int> {
        val allData = getWinningNumbers(fromRound, toRound)
        val frequency = mutableMapOf<Int, Int>()

        allData.forEach { data ->
            data.getMainNumbers().forEach { number ->
                frequency[number] = frequency.getOrDefault(number, 0) + 1
            }
        }

        return frequency.toSortedMap()
    }

    // 보너스 번호별 출현 빈도 계산
    suspend fun getBonusNumberFrequency(fromRound: Int, toRound: Int): Map<Int, Int> {
        val allData = getWinningNumbers(fromRound, toRound)
        val frequency = mutableMapOf<Int, Int>()

        allData.forEach { data ->
            val bonusNumber = data.bonusNumber
            frequency[bonusNumber] = frequency.getOrDefault(bonusNumber, 0) + 1
        }

        return frequency.toSortedMap()
    }
}