package com.hayoonjae.kbo.data

/** 홈 화면 상단에 무엇을 올릴지 고르는 순수 함수 모음. 화면과 떼어 놓아 테스트할 수 있게 둔다. */
object HomeSelection {

    /**
     * 내 팀의 진행 중인 경기. 한 팀은 동시에 한 경기만 하므로 보통 0개나 1개다.
     * 다른 팀 라이브는 여기 올리지 않고 아래 "오늘 경기" 목록에 이닝과 함께 남는다.
     */
    fun pickMyLive(games: List<Game>, myTeam: Team?): List<Game> {
        if (myTeam == null) return emptyList()
        return games
            .filter { it.status == GameStatus.LIVE && it.involves(myTeam) }
            .sortedBy { it.startTime }
    }

    /** 내 팀 오늘 경기의 상대. 진행 중인 경기가 없으면 null. */
    fun liveOpponent(games: List<Game>, myTeam: Team): Team? =
        pickMyLive(games, myTeam).firstOrNull()?.let { if (it.home == myTeam) it.away else it.home }
}
