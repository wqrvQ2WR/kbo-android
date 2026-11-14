package com.hayoonjae.kbo

import com.hayoonjae.kbo.data.KboTitleParser
import com.hayoonjae.kbo.data.LiveSelection
import com.hayoonjae.kbo.data.LiveVideo
import com.hayoonjae.kbo.data.Team
import com.hayoonjae.kbo.data.Video
import com.hayoonjae.kbo.data.VideoKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 2026-09-18 LG vs KT, NC vs 롯데 경기 중 유튜브 라이브 검색에 실제로 걸린 제목들.
 */
class LiveSelectionTest {

    @Test
    fun `LG 경기 입중계는 통과한다`() {
        listOf(
            "🔴 톨허스트 vs 로건｜5연승 들어가보자｜KT와 시즌 마지막 경기｜LG 편파 입중계 LIVE (vs KT)",
            "[9.18 엘지편파 입중계!] 톨허스트 파이팅!   #엘지트윈스 #삼성라이온즈 #롯데자이언츠  #두산베어스",
            "LG트윈스 편파입중계 이제 KT상대로 좀 이기자 LG vs KT 9/18",
            "엘지입중계 9월18일ㅣ[엘지편파입중계] ㅣ톨허스트VS로건ㅣ이주헌선발!!ㅣ#엘지트윈스#엘튜브#LG#LG트윈스#",
            "🔴 KBO LIVE LG Twins vs KT Wiz (September 18th)",
        ).forEach { title ->
            assertTrue("걸러지면 안 되는 제목: $title", LiveSelection.isAboutMyGame(title, Team.LG, Team.KT))
        }
    }

    @Test
    fun `태그만 붙인 다른 경기 방송은 걸러진다`() {
        // "LG트윈스" 라이브 검색 시청자 1위(1,168명)였던 방송. 실제 내용은 삼성 vs 한화다.
        val trap = "[ 입중계 라이브 ] 페라자 선발제외? 장규현 포수출전! KBO리그 삼성 vs 한화#한화이글스#lg트윈스#기아타이거즈"
        assertFalse(LiveSelection.isAboutMyGame(trap, Team.LG, Team.KT))
    }

    @Test
    fun `다른 팀 편파 방송은 걸러진다`() {
        val title = "🔥 삼성편파 🔥 삼성 vs 한화 입중계!! 특타좀 하고 왔나? 잘 좀 하자 제발!!!!!"
        assertFalse(LiveSelection.isAboutMyGame(title, Team.LG, Team.KT))
        assertFalse(LiveSelection.isAboutMyGame(title, Team.OB, null))
    }

    @Test
    fun `표기가 제각각인 롯데 경기 중계도 잡는다`() {
        listOf(
            "🔴LIVE NC다이노스 vs 롯데 자이언츠 보이는 라디오 중계 / 26.09.18 (금) / KNN",
            "[스클 LIVE] 2026 KBO리그 NC 다이노스 : 롯데 자이언츠 / 2026.09.18 경기 (이재학 vs 로드",
            "롯데자이언츠:2026년9월18일 롯데 VS NC(부산)!",
        ).forEach { title ->
            assertTrue("걸러지면 안 되는 제목: $title", LiveSelection.isAboutMyGame(title, Team.LT, Team.NC))
        }
    }

    @Test
    fun `상대가 오늘 상대와 다르면 걸러진다`() {
        // 롯데 팬인데 지난 경기(롯데 vs 삼성) 제목이 걸린 경우.
        assertFalse(LiveSelection.isAboutMyGame("롯데 vs 삼성 입중계", Team.LT, Team.NC))
    }

    @Test
    fun `공식 채널이 먼저, 나머지는 시청자 순으로 세 개까지`() {
        fun live(id: String, viewers: Long?, official: Boolean) =
            LiveVideo(Video(id, id, "", null, VideoKind.OTHER), "ch", viewers, official)
        val ranked = LiveSelection.rank(
            official = listOf(live("o1", 30, true)),
            others = listOf(live("a", 156, false), live("b", 500, false), live("c", null, false), live("d", 177, false)),
        )
        assertEquals(listOf("o1", "b", "d"), ranked.map { it.video.id })
    }

    @Test
    fun `같은 방송이 양쪽에 있어도 한 번만 나온다`() {
        fun live(id: String, official: Boolean) =
            LiveVideo(Video(id, id, "", null, VideoKind.OTHER), "ch", 10, official)
        val ranked = LiveSelection.rank(listOf(live("x", true)), listOf(live("x", false), live("y", false)))
        assertEquals(listOf("x", "y"), ranked.map { it.video.id })
        assertTrue(ranked.first().official)
    }

    @Test
    fun `화면용 제목에서 이모지와 HTML 엔티티를 걷어낸다`() {
        assertEquals(
            "톨허스트 vs 로건｜LG 편파 입중계 LIVE (vs KT)",
            KboTitleParser.cleanForDisplay("🔴 톨허스트 vs 로건｜LG 편파 입중계 LIVE (vs KT)"),
        )
        assertEquals(
            "김동현 캐스터 & 신본기",
            KboTitleParser.cleanForDisplay("김동현 캐스터 &amp; 신본기"),
        )
        assertEquals(
            "누리라이브 ep.17 오늘은 아라데이",
            KboTitleParser.cleanForDisplay("누리라이브 ep.17 오늘은 아라데이♡"),
        )
        assertEquals(
            "[9.18 엘지편파 입중계!] 톨허스트 파이팅!",
            KboTitleParser.cleanForDisplay(
                "[9.18 엘지편파 입중계!] 톨허스트 파이팅!   #엘지트윈스 #삼성라이온즈 #롯데자이언츠  #두산베어스",
            ),
        )
        assertEquals(
            "엘지입중계 9월18일ㅣ[엘지편파입중계] ㅣ톨허스트VS로건ㅣ이주헌선발!!ㅣ",
            KboTitleParser.cleanForDisplay("엘지입중계 9월18일ㅣ[엘지편파입중계] ㅣ톨허스트VS로건ㅣ이주헌선발!!ㅣ#엘지트윈스#엘튜브#LG#LG트윈스#"),
        )
        assertEquals(
            "THE GLOW GOES ON 영원히 빛날 김성현 은퇴식 라이브",
            KboTitleParser.cleanForDisplay("THE GLOW GOES ON✨ 영원히 빛날 김성현 은퇴식 라이브🎥"),
        )
    }
}
