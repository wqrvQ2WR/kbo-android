package com.hayoonjae.kbo

import com.hayoonjae.kbo.data.Game
import com.hayoonjae.kbo.data.GameStatus
import com.hayoonjae.kbo.data.ScheduleEnvelope
import com.hayoonjae.kbo.data.Team
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 네이버 스포츠 일정 응답을 그대로 넣고 역직렬화 경로를 확인한다.
 * 아래 JSON 은 API 가 실제로 돌려준 응답에서 가져온 것이다.
 */
class ScheduleParsingTest {

    // 앱이 쓰는 것과 같은 설정.
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    private val liveResponse = """
        {"code":200,"success":true,"result":{"games":[
        {"gameId":"20260913LGSS02026","categoryId":"kbo","gameDate":"2026-09-13",
         "gameDateTime":"2026-09-13T17:00:00","stadium":"대구",
         "homeTeamCode":"SS","homeTeamName":"삼성","homeTeamScore":2,
         "awayTeamCode":"LG","awayTeamName":"LG","awayTeamScore":5,
         "winner":"AWAY","statusCode":"STARTED","statusInfo":"3회초",
         "cancel":false,"suspended":false,"reversedHomeAway":true,
         "homeTeamEmblemUrl":"https://sports-phinf.pstatic.net/team/kbo/default/SS.png",
         "awayTeamEmblemUrl":"https://sports-phinf.pstatic.net/team/kbo/default/LG.png",
         "widgetEnable":false}
        ],"gameTotalCount":1}}
    """.trimIndent()

    @Test
    fun `진행 중 응답이 라이브로 해석된다`() {
        val games = json.decodeFromString(ScheduleEnvelope.serializer(), liveResponse)
            .result!!.games.map(Game::from)
        assertEquals(1, games.size)
        val game = games.first()
        assertEquals(GameStatus.LIVE, game.status)
        assertEquals("3회초", game.statusInfo)
        assertEquals(Team.LG, game.away)
        assertEquals(Team.SS, game.home)
        assertEquals(5, game.scoreOf(Team.LG))
        assertEquals(2, game.scoreAgainst(Team.LG))
        assertEquals("17:00", game.startTime)
        assertEquals("대구", game.stadium)
    }

    @Test
    fun `응답에 없는 필드가 있어도 깨지지 않는다`() {
        // statusInfo, stadium 이 빠지고 모르는 키가 섞인 응답.
        val sparse = """
            {"result":{"games":[
            {"gameId":"20260913NCOB02026","gameDate":"2026-09-13",
             "gameDateTime":"2026-09-13T18:30:00",
             "homeTeamCode":"OB","homeTeamName":"두산",
             "awayTeamCode":"NC","awayTeamName":"NC",
             "statusCode":"BEFORE","someNewFieldNaverAdded":123}
            ]}}
        """.trimIndent()
        val game = json.decodeFromString(ScheduleEnvelope.serializer(), sparse)
            .result!!.games.map(Game::from).first()
        assertEquals(GameStatus.BEFORE, game.status)
        assertEquals("18:30", game.startTime)
        assertEquals("", game.stadium)
        assertEquals(0, game.homeScore)
    }

    @Test
    fun `KBO 팀이 아닌 코드는 팀으로 해석하지 않는다`() {
        // 같은 엔드포인트가 올스타전(드림·나눔)이나 다른 리그도 같은 모양으로 돌려준다.
        assertNull(Team.byCode("EA"))
        assertNull(Team.byCode("WE"))
        assertNull(Team.byCode(null))
        assertEquals(Team.HT, Team.byCode("HT"))
    }
}
