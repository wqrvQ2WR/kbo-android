package com.hayoonjae.kbo

import com.hayoonjae.kbo.data.KboTitleParser
import com.hayoonjae.kbo.data.Team
import com.hayoonjae.kbo.data.VideoKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * KBO 공식 채널 업로드 재생목록에서 실제로 받아온 최신 100개 제목을 그대로 넣고
 * 파서를 통째로 돌려 본다. 채널이 제목 규칙을 바꾸면 여기서 먼저 깨진다.
 */
class ChannelFixtureTest {

    private val titles: List<String> = checkNotNull(
        javaClass.classLoader?.getResourceAsStream("kbo_channel_titles.txt"),
    ) { "fixture 를 찾을 수 없습니다" }
        .bufferedReader()
        .readLines()
        .filter { it.isNotBlank() }

    @Test
    fun `fixture 가 비어 있지 않다`() {
        assertEquals(100, titles.size)
    }

    @Test
    fun `1군 경기 하이라이트는 정확히 두 팀으로 해석된다`() {
        val highlights = titles.filter {
            KboTitleParser.kindOf(it) == VideoKind.HIGHLIGHT && KboTitleParser.isFirstTeam(it)
        }
        assertTrue("하이라이트 제목이 하나도 잡히지 않았다", highlights.isNotEmpty())
        highlights.forEach { title ->
            assertEquals("두 팀으로 안 나뉜 제목: $title", 2, KboTitleParser.teamsIn(title).size)
        }
    }

    @Test
    fun `1군 경기 하이라이트에는 날짜가 들어 있다`() {
        titles.filter {
            KboTitleParser.kindOf(it) == VideoKind.HIGHLIGHT && KboTitleParser.isFirstTeam(it)
        }.forEach { title ->
            assertTrue("날짜를 못 읽은 제목: $title", KboTitleParser.monthDayIn(title) != null)
        }
    }

    @Test
    fun `대진이 적힌 크보모먼트는 팀으로 해석된다`() {
        val moments = titles.filter { KboTitleParser.kindOf(it) == VideoKind.MOMENT }
        assertTrue("크보모먼트가 하나도 잡히지 않았다", moments.isNotEmpty())
        // "| LG vs 삼성 |" 처럼 대진이 적힌 클립만 팀이 나와야 한다.
        moments.filter { it.contains(" vs ") }.forEach { title ->
            assertTrue("팀을 못 읽은 제목: $title", KboTitleParser.teamsIn(title).isNotEmpty())
        }
    }

    @Test
    fun `팀과 무관한 클립은 어느 팀에도 붙지 않는다`() {
        // 시구 모음처럼 대진이 없는 영상이 실제로 올라온다. 이런 건 팀을 추측해 붙이는
        // 대신 비워 둬야 팀 필터에서 엉뚱하게 섞이지 않는다.
        val title = "미소 짓게 만드는 귀여운 토요일 시구 모음! | 9.12 | 크보모먼트 | KBO 야구 하이라이트"
        assertTrue(titles.contains(title))
        assertTrue(KboTitleParser.teamsIn(title).isEmpty())
    }

    @Test
    fun `퓨처스리그는 전부 걸러진다`() {
        val futures = titles.filter { it.contains("퓨처스") }
        assertTrue("퓨처스 제목이 fixture 에 없다", futures.isNotEmpty())
        futures.forEach { assertTrue(!KboTitleParser.isFirstTeam(it)) }
    }

    @Test
    fun `10개 구단 모두 최근 영상에서 걸린다`() {
        // 영상 탭의 팀 필터가 실제로 무엇을 건지는지 본다. 시즌 중 최신 100개면
        // 열 팀이 모두 한 번씩은 나와야 한다. 한 팀이라도 0이면 별칭 표가 틀린 것이다.
        val counts = Team.entries.associateWith { team ->
            titles.count { KboTitleParser.isFirstTeam(it) && team in KboTitleParser.teamsIn(it) }
        }
        println("팀별 영상 수: " + counts.entries.joinToString { "${it.key.shortName}=${it.value}" })
        val missing = counts.filterValues { it == 0 }.keys
        assertTrue("최근 영상에서 안 걸린 팀: $missing", missing.isEmpty())
    }
}
