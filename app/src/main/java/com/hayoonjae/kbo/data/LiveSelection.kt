package com.hayoonjae.kbo.data

/** 영상 탭 최상단 라이브 칸에 무엇을 올릴지 고른다. */
object LiveSelection {

    /** 라이브 칸 상한. */
    const val LIMIT = 3

    /**
     * 유튜브 전체 라이브 중 [myTeam] 의 오늘 경기를 다루는 방송인지.
     *
     * 팀명으로 검색하면 태그만 도배한 다른 경기 방송이 섞여 나온다(실제로 "LG트윈스"
     * 라이브 검색 시청자 1위가 "#lg트윈스" 태그만 붙인 삼성 vs 한화 입중계였다).
     * 그래서 태그를 뗀 제목에서 팀을 읽고,
     *  - 내 팀이 나와야 하고
     *  - 다른 팀이 함께 나온다면 그 팀은 오늘 상대([opponent])여야 한다.
     */
    fun isAboutMyGame(title: String, myTeam: Team, opponent: Team?): Boolean {
        val teams = KboTitleParser.teamsIn(title)
        if (myTeam !in teams) return false
        val others = teams - myTeam
        return others.isEmpty() || (opponent != null && others == setOf(opponent))
    }

    /** 공식 채널 방송을 먼저, 나머지는 시청자 많은 순으로 채워 [limit] 개까지. */
    fun rank(official: List<LiveVideo>, others: List<LiveVideo>, limit: Int = LIMIT): List<LiveVideo> {
        val byViewers = compareByDescending<LiveVideo> { it.viewers ?: -1L }
        return (official.sortedWith(byViewers) + others.sortedWith(byViewers))
            .distinctBy { it.video.id }
            .take(limit)
    }
}
