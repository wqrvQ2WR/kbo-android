package com.hayoonjae.kbo.data

/**
 * 네이버 스포츠의 공개 KBO 일정/기록 엔드포인트.
 *
 * 유튜브 영상 제목과 설명에는 점수가 들어 있지 않기 때문에(공식 채널 하이라이트는
 * `[A vs B] 9.12(토) 야구 하이라이트` 형식이 전부다) 스코어와 경기 요약은 여기서 가져온다.
 * 별도의 키가 필요 없다.
 */
object NaverSportsApi {

    private const val BASE = "https://api-gw.sports.naver.com/schedule"
    private val HEADERS = mapOf(
        "User-Agent" to "Mozilla/5.0 (Linux; Android 14) KboApp",
        "Accept" to "application/json",
    )

    /** [fromDate]~[toDate] (yyyy-MM-dd) 사이의 KBO 1군 경기. */
    suspend fun games(fromDate: String, toDate: String): List<Game> {
        // fields=basic 에는 구장(stadium)이 빠져 있어 따로 요청해야 한다.
        val url = "$BASE/games?fields=basic,stadium&categoryId=kbo" +
            "&fromDate=$fromDate&toDate=$toDate&size=1000"
        val body = Http.get(url, HEADERS)
        val envelope = Http.json.decodeFromString(ScheduleEnvelope.serializer(), body)
        return envelope.result?.games.orEmpty()
            .map(Game::from)
            .sortedBy { it.date + it.startTime }
    }

    /** 한 경기의 상세 기록. 끝나지 않은 경기는 비어 있을 수 있다. */
    suspend fun record(gameId: String): RecordData? {
        val body = Http.get("$BASE/games/$gameId/record", HEADERS)
        return Http.json.decodeFromString(RecordEnvelope.serializer(), body).result?.recordData
    }

    /** 경기 + 기록을 합친 요약. 기록을 못 가져오면 점수만 담긴 요약을 돌려준다. */
    suspend fun summary(game: Game): GameSummary {
        val record = runCatching { record(game.id) }.getOrNull()
        val pitchers = record?.pitchingResult.orEmpty()
        fun pitcher(mark: String) = pitchers.firstOrNull { it.wls.equals(mark, ignoreCase = true) }?.name
        fun etc(how: String) = record?.etcRecords?.firstOrNull { it.how == how }?.result?.takeIf { it.isNotBlank() }
        return GameSummary(
            game = game,
            winningPitcher = pitcher("W"),
            losingPitcher = pitcher("L"),
            save = pitcher("S"),
            decisiveHit = etc("결승타"),
            homeRuns = etc("홈런"),
            scoreBoard = record?.scoreBoard,
            homeStandings = record?.homeStandings,
            awayStandings = record?.awayStandings,
        )
    }
}
