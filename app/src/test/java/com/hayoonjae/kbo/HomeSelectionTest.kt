package com.hayoonjae.kbo

import com.hayoonjae.kbo.data.Game
import com.hayoonjae.kbo.data.GameDto
import com.hayoonjae.kbo.data.GameStatus
import com.hayoonjae.kbo.data.HomeSelection
import com.hayoonjae.kbo.data.Team
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeSelectionTest {

    /** statusCode 값은 네이버 스포츠 API 가 실제로 돌려주는 것들이다. */
    private fun game(
        id: String,
        away: Team,
        home: Team,
        statusCode: String,
        time: String = "17:00",
        cancel: Boolean = false,
    ) = Game.from(
        GameDto(
            gameId = id,
            gameDate = "2026-09-13",
            gameDateTime = "2026-09-13T$time:00",
            awayTeamCode = away.code,
            awayTeamName = away.shortName,
            homeTeamCode = home.code,
            homeTeamName = home.shortName,
            statusCode = statusCode,
            cancel = cancel,
        ),
    )

    @Test
    fun `STARTED 만 진행 중이다`() {
        assertEquals(GameStatus.LIVE, game("a", Team.LG, Team.SS, "STARTED").status)
        assertEquals(GameStatus.FINISHED, game("b", Team.LG, Team.SS, "RESULT").status)
        assertEquals(GameStatus.BEFORE, game("c", Team.LG, Team.SS, "BEFORE").status)
    }

    @Test
    fun `READY 는 아직 경기 전이다`() {
        // 개시 직전 상태다. 이걸 진행 중으로 잡으면 시작도 안 한 경기가 라이브 칸에 올라간다.
        assertEquals(GameStatus.BEFORE, game("d", Team.LG, Team.SS, "READY").status)
    }

    @Test
    fun `취소는 상태 코드보다 우선한다`() {
        assertEquals(
            GameStatus.CANCELED,
            game("e", Team.LG, Team.SS, "BEFORE", cancel = true).status,
        )
    }

    @Test
    fun `최상단에는 내 팀 라이브만 올라간다`() {
        val games = listOf(
            game("g1", Team.HT, Team.KT, "STARTED"),
            game("g2", Team.LT, Team.WO, "STARTED"),
            game("g3", Team.LG, Team.SS, "STARTED", time = "18:30"),
            game("g4", Team.NC, Team.OB, "STARTED"),
        )
        val picked = HomeSelection.pickMyLive(games, myTeam = Team.LG)
        assertEquals(listOf("g3"), picked.map { it.id })
    }

    @Test
    fun `내 팀이 원정이어도 잡힌다`() {
        val games = listOf(game("g1", Team.LG, Team.KT, "STARTED"))
        assertEquals(1, HomeSelection.pickMyLive(games, Team.LG).size)
        assertEquals(Team.KT, HomeSelection.liveOpponent(games, Team.LG))
    }

    @Test
    fun `내 팀이 경기 전이거나 끝났으면 비어 있다`() {
        val games = listOf(
            game("g1", Team.LG, Team.SS, "READY"),
            game("g2", Team.LG, Team.SS, "RESULT"),
            game("g3", Team.HT, Team.KT, "STARTED"),
        )
        assertTrue(HomeSelection.pickMyLive(games, Team.LG).isEmpty())
        assertEquals(null, HomeSelection.liveOpponent(games, Team.LG))
    }

    @Test
    fun `팀을 안 골랐으면 비어 있다`() {
        val games = listOf(game("g1", Team.LG, Team.SS, "STARTED"))
        assertTrue(HomeSelection.pickMyLive(games, myTeam = null).isEmpty())
    }
}
