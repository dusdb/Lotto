package com.example.lotto.api

import com.example.lotto.data.LottoApiResponse
import com.example.lotto.data.LottoWinningNumber
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class LottoApiService {

    companion object {
        private const val BASE_URL = "https://www.dhlottery.co.kr/common.do"

        @Volatile
        private var INSTANCE: LottoApiService? = null

        fun getInstance(): LottoApiService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LottoApiService().also { INSTANCE = it }
            }
        }
    }

    // 단일 회차 당첨번호 가져오기
    suspend fun getWinningNumber(round: Int): LottoWinningNumber? {
        return withContext(Dispatchers.IO) {
            try {
                val urlString="$BASE_URL?method=getLottoNumber&drwNo=$round"
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = reader.readText()
                    reader.close()

                    parseApiResponse(response)
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    // 범위 내 당첨번호들 가져오기
    suspend fun getWinningNumbers(fromRound: Int, toRound: Int): List<LottoWinningNumber> {
        val results = mutableListOf<LottoWinningNumber>()

        // 병렬 처리로 여러 회차 동시 요청
        val jobs = mutableListOf<Deferred<LottoWinningNumber?>>()

        for (round in fromRound..toRound) {
            val job = CoroutineScope(Dispatchers.IO).async {
                getWinningNumber(round)
            }
            jobs.add(job)
        }

        // 모든 요청 완료 대기
        jobs.forEach { job ->
            job.await()?.let { winningNumber ->
                results.add(winningNumber)
            }
        }

        // 회차 순서대로 정렬 (최신순)
        return results.sortedByDescending { it.round }
    }

    // JSON 응답을 파싱하여 LottoWinningNumber로 변환
    private fun parseApiResponse(jsonString: String): LottoWinningNumber? {
        return try {
            val jsonObject = JSONObject(jsonString)

            // API에서 에러 응답인지 확인
            val returnValue = jsonObject.getString("returnValue")
            if (returnValue != "success") {
                return null
            }

            val round = jsonObject.getInt("drwNo")
            val date = jsonObject.getString("drwNoDate")

            val numbers = listOf(
                jsonObject.getInt("drwtNo1"),
                jsonObject.getInt("drwtNo2"),
                jsonObject.getInt("drwtNo3"),
                jsonObject.getInt("drwtNo4"),
                jsonObject.getInt("drwtNo5"),
                jsonObject.getInt("drwtNo6")
            )

            val bonusNumber = jsonObject.getInt("bnusNo")
            val prizeMoney = jsonObject.optLong("firstWinamnt", 0L)
            val winnerCount = jsonObject.optInt("firstPrzwnerCo", 0)

            LottoWinningNumber(
                round = round,
                date = date,
                numbers = numbers,
                bonusNumber = bonusNumber,
                prizeMoney = prizeMoney,
                winnerCount = winnerCount
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}