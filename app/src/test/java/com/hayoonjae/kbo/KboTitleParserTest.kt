package com.hayoonjae.kbo

import com.hayoonjae.kbo.data.KboTitleParser
import com.hayoonjae.kbo.data.Team
import com.hayoonjae.kbo.data.VideoKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 검증에 쓰는 제목은 전부 KBO 공식 채널(@KBO1982)에 실제로 올라온 것들이다.
 */
class KboTitleParserTest {

    private val highlight =
        "[NC다이노스 vs 두산베어스] 9.12(토) 야구 하이라이트｜2026 KBO 리그｜KBO X TVING"
    private val moment =
        "최고의 타격감! LG 오스틴의 동점 적시타! 그리고 송찬의가 만든 역전까지! | LG vs 삼성 | 9.12 | 크보모먼트 | KBO 야구 하이라이트"
    private val momentEmptyTeams =
        "4번 타자가 보여줘야 할 한 방! 롯데 한동희의 스리런 홈런!!! |  vs  | 9.12 | 크보모먼트 | KBO 야구 하이라이트"
    private val momentDoubleSpace =
        "엄청난 힘! 밀어서 담장을 넘기는 한화 문현빈의 재역전 투런포! | NC  vs 한화 | 9.11 | 크보모먼트 | KBO 야구 하이라이트"
    private val futures =
        "[2026 메디힐 KBO 퓨처스리그 H/L] KIA타이거즈 vs 한화이글스(09.11)"

    @Test
    fun `하이라이트 제목에서 두 팀을 읽는다`() {
        assertEquals(setOf(Team.NC, Team.OB), KboTitleParser.teamsIn(highlight))
        assertEquals(VideoKind.HIGHLIGHT, KboTitleParser.kindOf(highlight))
    }

    @Test
    fun `크보모먼트 제목에서 대진 구간만 읽는다`() {
        assertEquals(setOf(Team.LG, Team.SS), KboTitleParser.teamsIn(moment))
        assertEquals(VideoKind.MOMENT, KboTitleParser.kindOf(moment))
    }

    @Test
    fun `대진 칸이 비어 있으면 제목 전체에서 팀을 찾는다`() {
        assertTrue(Team.LT in KboTitleParser.teamsIn(momentEmptyTeams))
    }

    @Test
    fun `공백이 겹친 대진도 읽는다`() {
        assertEquals(setOf(Team.NC, Team.HH), KboTitleParser.teamsIn(momentDoubleSpace))
    }

    @Test
    fun `퓨처스리그는 1군이 아니다`() {
        assertFalse(KboTitleParser.isFirstTeam(futures))
        assertTrue(KboTitleParser.isFirstTeam(highlight))
    }

    @Test
    fun `풀네임이 약칭보다 먼저 맞춰진다`() {
        // "KIA타이거즈" 를 "KIA" 로 끊어 읽어도 같은 팀이어야 한다.
        assertEquals(setOf(Team.HT, Team.HH), KboTitleParser.teamsIn(futures))
    }

    @Test
    fun `제목에서 날짜를 읽는다`() {
        assertEquals(9 to 12, KboTitleParser.monthDayIn(highlight))
        assertEquals(9 to 11, KboTitleParser.monthDayIn(momentDoubleSpace))
    }

    @Test
    fun `영문 단어 속 약칭은 팀으로 세지 않는다`() {
        // "LANCE" 안의 "NC", "SKY" 안의 "SK" 처럼 우연히 겹치는 경우를 걸러야 한다.
        val title = "LANCE SKYLINE 특집 영상"
        assertTrue(KboTitleParser.teamsIn(title).isEmpty())
    }

    @Test
    fun `일반 영상은 하이라이트로 분류되지 않는다`() {
        val title = "윤석민 코치 레슨 모아보기 | 투수 통합편 | 크보 레전드 레슨"
        assertEquals(VideoKind.OTHER, KboTitleParser.kindOf(title))
    }
}
