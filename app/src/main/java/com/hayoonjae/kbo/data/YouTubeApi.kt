package com.hayoonjae.kbo.data

/**
 * YouTube Data API v3.
 *
 * 검색(search.list)은 호출당 100 유닛이라 주기적으로 두드리기엔 비싸서, 채널의 업로드
 * 재생목록을 playlistItems.list(호출당 1 유닛)로 읽고 팀 필터는 앱에서 건다.
 * 업로드 재생목록 ID 는 채널 ID 의 접두사 UC 를 UU 로 바꾼 값이다.
 */
object YouTubeApi {

    /** KBO 공식 채널(youtube.com/@KBO1982). */
    const val KBO_CHANNEL_ID = "UCoVz66yWHzVsXAFG8WhJK9g"

    private val UPLOADS_PLAYLIST_ID = "UU" + KBO_CHANNEL_ID.removePrefix("UC")
    private const val BASE = "https://www.googleapis.com/youtube/v3"

    class MissingKeyException : Exception("YouTube API 키가 없습니다")

    class ApiException(val code: Int, override val message: String) : Exception(message)

    /**
     * 공식 채널 최신 업로드에서 [team] 관련 1군 영상만 추린다.
     *
     * @param pages 훑을 페이지 수. 한 페이지가 50개이므로 기본 2페이지면 최근 100개를 본다.
     */
    suspend fun recentVideos(apiKey: String, team: Team?, pages: Int = 2): List<Video> {
        if (apiKey.isBlank()) throw MissingKeyException()
        val collected = mutableListOf<Video>()
        var pageToken: String? = null
        repeat(pages) {
            val url = buildString {
                append("$BASE/playlistItems?part=snippet&maxResults=50")
                append("&playlistId=$UPLOADS_PLAYLIST_ID")
                append("&key=$apiKey")
                if (pageToken != null) append("&pageToken=$pageToken")
            }
            val body = Http.get(url)
            val page = Http.json.decodeFromString(PlaylistItemsResponse.serializer(), body)
            page.error?.let { throw ApiException(it.code, it.message) }
            page.items.mapNotNullTo(collected) { it.toVideo() }
            pageToken = page.nextPageToken
            if (pageToken == null) return@repeat
        }
        return collected
            .filter { KboTitleParser.isFirstTeam(it.title) }
            .filter { team == null || team in KboTitleParser.teamsIn(it.title) }
    }

    /**
     * 지금 방송 중인 [team] 관련 라이브.
     *
     * 1. 구단 공식 채널의 최신 업로드를 videos.list 로 확인해 방송 중인 것(2 유닛).
     *    공식 채널은 퓨처스 생중계, 경기 전 프리뷰, 행사 라이브를 한다.
     * 2. [opponent] 가 있으면(= 내 팀 경기가 진행 중이면) 유튜브 전체에서 라이브를 검색한다(100 유닛).
     *    1군 경기 중계권은 유튜브에 없어서 경기 중 라이브는 거의 입중계·라디오 중계다.
     *    검색은 비싸서 내 팀이 실제로 경기 중일 때만 한다.
     */
    suspend fun liveVideos(apiKey: String, team: Team, opponent: Team?): List<LiveVideo> {
        if (apiKey.isBlank()) throw MissingKeyException()

        val official = latestVideoIds(apiKey, team.channelId, count = 15)
            .let { details(apiKey, it) }
            .filter { it.snippet?.liveBroadcastContent == "live" }
            .mapNotNull { it.toLive(official = true) }

        val others = if (opponent == null) {
            emptyList()
        } else {
            searchLiveIds(apiKey, query = team.titleAliases.first())
                .filterNot { id -> official.any { it.video.id == id } }
                .let { details(apiKey, it) }
                .filter { it.snippet?.liveBroadcastContent == "live" }
                .filter { LiveSelection.isAboutMyGame(it.snippet?.title.orEmpty(), team, opponent) }
                .mapNotNull { it.toLive(official = false) }
        }
        return LiveSelection.rank(official, others)
    }

    private suspend fun latestVideoIds(apiKey: String, channelId: String, count: Int): List<String> {
        val playlist = "UU" + channelId.removePrefix("UC")
        val body = Http.get(
            "$BASE/playlistItems?part=snippet&maxResults=$count&playlistId=$playlist&key=$apiKey",
        )
        val page = Http.json.decodeFromString(PlaylistItemsResponse.serializer(), body)
        page.error?.let { throw ApiException(it.code, it.message) }
        return page.items.mapNotNull { it.snippet?.resourceId?.videoId?.takeIf(String::isNotBlank) }
    }

    private suspend fun searchLiveIds(apiKey: String, query: String): List<String> {
        val q = java.net.URLEncoder.encode(query, "UTF-8")
        val body = Http.get(
            "$BASE/search?part=id&type=video&eventType=live&maxResults=25" +
                "&regionCode=KR&relevanceLanguage=ko&q=$q&key=$apiKey",
        )
        val result = Http.json.decodeFromString(SearchResponse.serializer(), body)
        result.error?.let { throw ApiException(it.code, it.message) }
        return result.items
            .filter { it.id?.kind == "youtube#video" }
            .mapNotNull { it.id?.videoId }
    }

    /** 영상 50개까지 한 번에(1 유닛) 방송 상태와 동시 시청자를 가져온다. */
    private suspend fun details(apiKey: String, ids: List<String>): List<VideoItem> {
        if (ids.isEmpty()) return emptyList()
        val body = Http.get(
            "$BASE/videos?part=snippet,liveStreamingDetails&id=${ids.take(50).joinToString(",")}&key=$apiKey",
        )
        val result = Http.json.decodeFromString(VideosResponse.serializer(), body)
        result.error?.let { throw ApiException(it.code, it.message) }
        return result.items
    }

    private fun VideoItem.toLive(official: Boolean): LiveVideo? {
        val s = snippet ?: return null
        if (id.isBlank()) return null
        return LiveVideo(
            video = Video(
                id = id,
                title = s.title,
                publishedAt = liveStreamingDetails?.actualStartTime ?: s.publishedAt,
                thumbnailUrl = s.thumbnails?.best,
                kind = VideoKind.OTHER,
            ),
            channelTitle = s.channelTitle,
            viewers = liveStreamingDetails?.concurrentViewers?.toLongOrNull(),
            official = official,
        )
    }

    private fun PlaylistItem.toVideo(): Video? {
        val s = snippet ?: return null
        val id = s.resourceId?.videoId?.takeIf { it.isNotBlank() } ?: return null
        return Video(
            id = id,
            title = s.title,
            publishedAt = s.publishedAt,
            thumbnailUrl = s.thumbnails?.best,
            kind = KboTitleParser.kindOf(s.title),
        )
    }
}
