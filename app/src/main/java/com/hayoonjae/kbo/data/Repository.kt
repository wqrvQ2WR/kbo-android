package com.hayoonjae.kbo.data

import android.content.Context
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class Repository(context: Context) {

    val prefs = Prefs(context.applicationContext)

    suspend fun gamesOn(date: LocalDate): List<Game> {
        val d = date.format(DATE)
        return NaverSportsApi.games(d, d)
    }

    /** [date] 기준으로 앞뒤 [span] 일을 함께 가져온다. 최근 결과와 다음 경기를 한 번에 보여주기 위함이다. */
    suspend fun gamesAround(date: LocalDate, span: Long = 3): List<Game> =
        NaverSportsApi.games(date.minusDays(span).format(DATE), date.plusDays(span).format(DATE))

    suspend fun summary(game: Game): GameSummary = NaverSportsApi.summary(game)

    suspend fun videos(team: Team?): List<Video> {
        val settings = prefs.current()
        return YouTubeApi.recentVideos(settings.apiKey, team)
    }

    /** [team] 관련 라이브. 내 팀 경기가 진행 중인지는 네이버 일정으로 판단한다. */
    suspend fun liveVideos(team: Team): List<LiveVideo> {
        val settings = prefs.current()
        val todayGames = runCatching { gamesOn(today()) }.getOrDefault(emptyList())
        val opponent = HomeSelection.liveOpponent(todayGames, team)
        return YouTubeApi.liveVideos(settings.apiKey, team, opponent)
    }

    companion object {
        private val DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        private val KST: ZoneId = ZoneId.of("Asia/Seoul")

        /** KBO 일정은 한국 날짜 기준이다. 폰 시간대와 상관없이 한국 날짜를 쓴다. */
        fun today(): LocalDate = LocalDate.now(KST)

        fun formatDate(date: LocalDate): String = date.format(DATE)
    }
}
