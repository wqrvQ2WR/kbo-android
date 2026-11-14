package com.hayoonjae.kbo.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------------------------------------------------------------------------
// 네이버 스포츠 KBO 일정/결과
// ---------------------------------------------------------------------------

@Serializable
data class ScheduleEnvelope(val result: ScheduleResult? = null)

@Serializable
data class ScheduleResult(val games: List<GameDto> = emptyList())

@Serializable
data class GameDto(
    val gameId: String,
    val gameDate: String = "",
    val gameDateTime: String = "",
    val stadium: String = "",
    val homeTeamCode: String = "",
    val homeTeamName: String = "",
    val homeTeamScore: Int = 0,
    val awayTeamCode: String = "",
    val awayTeamName: String = "",
    val awayTeamScore: Int = 0,
    val winner: String = "",
    val statusCode: String = "",
    val statusInfo: String = "",
    val cancel: Boolean = false,
    val suspended: Boolean = false,
)

/**
 * 경기 상태.
 *
 * 네이버가 주는 statusCode 는 실제로 네 가지다.
 * `BEFORE`(경기 전) · `READY`(개시 직전) · `STARTED`(진행 중, statusInfo 에 "2회말") · `RESULT`(종료).
 * `READY` 는 아직 시작한 게 아니므로 [BEFORE] 로 묶는다.
 */
enum class GameStatus { BEFORE, LIVE, FINISHED, CANCELED }

/** 내 팀 관점의 경기 결과. [NONE] 은 아직 결과가 없는 경우다. */
enum class Outcome { WIN, LOSE, DRAW, NONE }

data class Game(
    val id: String,
    val date: String,
    val startTime: String,
    val stadium: String,
    val home: Team?,
    val away: Team?,
    val homeName: String,
    val awayName: String,
    val homeScore: Int,
    val awayScore: Int,
    val status: GameStatus,
    val statusInfo: String,
) {
    fun involves(team: Team): Boolean = home == team || away == team

    fun opponentOf(team: Team): String = if (home == team) awayName else homeName

    fun isHomeGameFor(team: Team): Boolean = home == team

    fun scoreOf(team: Team): Int = if (home == team) homeScore else awayScore

    fun scoreAgainst(team: Team): Int = if (home == team) awayScore else homeScore

    fun outcomeFor(team: Team): Outcome {
        if (status != GameStatus.FINISHED) return Outcome.NONE
        val mine = scoreOf(team)
        val theirs = scoreAgainst(team)
        return when {
            mine > theirs -> Outcome.WIN
            mine < theirs -> Outcome.LOSE
            else -> Outcome.DRAW
        }
    }

    companion object {
        fun from(dto: GameDto): Game = Game(
            id = dto.gameId,
            date = dto.gameDate,
            // gameDateTime 은 "2026-09-12T17:00:00" 형태다. 시:분만 떼어 쓴다.
            startTime = dto.gameDateTime.substringAfter('T', "").take(5),
            stadium = dto.stadium,
            home = Team.byCode(dto.homeTeamCode),
            away = Team.byCode(dto.awayTeamCode),
            homeName = dto.homeTeamName,
            awayName = dto.awayTeamName,
            homeScore = dto.homeTeamScore,
            awayScore = dto.awayTeamScore,
            status = when {
                dto.cancel -> GameStatus.CANCELED
                dto.statusCode == "RESULT" -> GameStatus.FINISHED
                dto.statusCode == "STARTED" -> GameStatus.LIVE
                // BEFORE, READY, 그리고 모르는 코드는 경기 전으로 둔다. 확신 없는 값을
                // 진행 중으로 올리면 라이브 칸에 시작도 안 한 경기가 올라온다.
                else -> GameStatus.BEFORE
            },
            statusInfo = dto.statusInfo,
        )
    }
}

// ---------------------------------------------------------------------------
// 네이버 스포츠 경기 기록 (결과 요약용)
// ---------------------------------------------------------------------------

@Serializable
data class RecordEnvelope(val result: RecordResult? = null)

@Serializable
data class RecordResult(val recordData: RecordData? = null)

@Serializable
data class RecordData(
    val etcRecords: List<EtcRecord> = emptyList(),
    val pitchingResult: List<PitcherResult> = emptyList(),
    val scoreBoard: ScoreBoard? = null,
    val todayKeyStats: KeyStatsPair? = null,
    val homeStandings: Standings? = null,
    val awayStandings: Standings? = null,
)

/** "결승타", "홈런" 같은 항목([how])과 그 내용([result]). */
@Serializable
data class EtcRecord(val how: String = "", val result: String = "")

/** [wls] 는 W(승) / L(패) / S(세이브) / H(홀드). */
@Serializable
data class PitcherResult(val name: String = "", val wls: String = "", val w: Int = 0, val l: Int = 0, val s: Int = 0)

@Serializable
data class ScoreBoard(val inn: InningPair? = null, val rheb: RhebPair? = null)

@Serializable
data class InningPair(val home: List<Int> = emptyList(), val away: List<Int> = emptyList())

@Serializable
data class RhebPair(val home: Rheb? = null, val away: Rheb? = null)

/** 득점 / 안타 / 실책 / 볼넷. */
@Serializable
data class Rheb(val r: Int = 0, val h: Int = 0, val e: Int = 0, val b: Int = 0)

@Serializable
data class KeyStatsPair(val home: KeyStats? = null, val away: KeyStats? = null)

@Serializable
data class KeyStats(val hit: Int = 0, val hr: Int = 0, val err: Int = 0, val kk: Int = 0, val sb: Int = 0)

@Serializable
data class Standings(val name: String = "", val rank: Int = 0, val w: Int = 0, val l: Int = 0, val d: Int = 0, val wra: Double = 0.0)

/** 화면과 알림에서 함께 쓰는, 한 경기의 요약. */
data class GameSummary(
    val game: Game,
    val winningPitcher: String?,
    val losingPitcher: String?,
    val save: String?,
    val decisiveHit: String?,
    val homeRuns: String?,
    val scoreBoard: ScoreBoard?,
    val homeStandings: Standings?,
    val awayStandings: Standings?,
)

// ---------------------------------------------------------------------------
// YouTube Data API v3
// ---------------------------------------------------------------------------

@Serializable
data class PlaylistItemsResponse(
    val items: List<PlaylistItem> = emptyList(),
    val nextPageToken: String? = null,
    val error: YouTubeError? = null,
)

@Serializable
data class YouTubeError(val code: Int = 0, val message: String = "")

@Serializable
data class PlaylistItem(val snippet: PlaylistSnippet? = null)

@Serializable
data class PlaylistSnippet(
    val title: String = "",
    val publishedAt: String = "",
    val thumbnails: Thumbnails? = null,
    val resourceId: ResourceId? = null,
)

@Serializable
data class ResourceId(val videoId: String = "")

@Serializable
data class Thumbnails(
    val medium: Thumbnail? = null,
    val high: Thumbnail? = null,
    @SerialName("default") val standard: Thumbnail? = null,
) {
    val best: String? get() = (high ?: medium ?: standard)?.url
}

@Serializable
data class Thumbnail(val url: String = "")

/** 영상 종류. 공식 채널 제목 규칙으로 구분한다. */
enum class VideoKind { HIGHLIGHT, MOMENT, OTHER }

data class Video(
    val id: String,
    val title: String,
    val publishedAt: String,
    val thumbnailUrl: String?,
    val kind: VideoKind,
) {
    val watchUrl: String get() = "https://www.youtube.com/watch?v=$id"
}

// --- 라이브 ---------------------------------------------------------------

@Serializable
data class SearchResponse(
    val items: List<SearchItem> = emptyList(),
    val error: YouTubeError? = null,
)

@Serializable
data class SearchItem(val id: SearchId? = null)

/** 검색 결과에는 영상 말고 채널도 섞여 나온다. [kind] 로 거른다. */
@Serializable
data class SearchId(val kind: String = "", val videoId: String? = null)

@Serializable
data class VideosResponse(
    val items: List<VideoItem> = emptyList(),
    val error: YouTubeError? = null,
)

@Serializable
data class VideoItem(
    val id: String = "",
    val snippet: VideoSnippet? = null,
    val liveStreamingDetails: LiveStreamingDetails? = null,
)

/** [liveBroadcastContent] 는 "live" / "upcoming" / "none". */
@Serializable
data class VideoSnippet(
    val title: String = "",
    val channelId: String = "",
    val channelTitle: String = "",
    val publishedAt: String = "",
    val thumbnails: Thumbnails? = null,
    val liveBroadcastContent: String = "none",
)

/** API 가 [concurrentViewers] 를 숫자가 아닌 문자열로 준다. */
@Serializable
data class LiveStreamingDetails(
    val concurrentViewers: String? = null,
    val actualStartTime: String? = null,
)

/** 지금 방송 중인 영상 하나. [official] 은 구단 공식 채널에서 하는 방송인지. */
data class LiveVideo(
    val video: Video,
    val channelTitle: String,
    val viewers: Long?,
    val official: Boolean,
)
