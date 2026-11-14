package com.hayoonjae.kbo.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hayoonjae.kbo.data.Game
import com.hayoonjae.kbo.data.GameStatus
import com.hayoonjae.kbo.data.GameSummary
import com.hayoonjae.kbo.data.HomeSelection
import com.hayoonjae.kbo.data.KboTitleParser
import com.hayoonjae.kbo.data.LiveVideo
import com.hayoonjae.kbo.data.Repository
import com.hayoonjae.kbo.data.Settings
import com.hayoonjae.kbo.data.Team
import com.hayoonjae.kbo.data.Video
import com.hayoonjae.kbo.data.VideoKind
import com.hayoonjae.kbo.data.YouTubeApi
import com.hayoonjae.kbo.notify.GameCheckWorker
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** 화면에 그대로 대응하는 로딩 상태. */
sealed interface Load<out T> {
    data object Loading : Load<Nothing>
    data class Ok<T>(val value: T) : Load<T>
    data class Err(val message: String, val needsApiKey: Boolean = false) : Load<Nothing>
}

data class HomeData(
    /** 최상단에 고정되는 내 팀의 진행 중 경기. */
    val live: List<Game>,
    val today: List<Game>,
    val myRecent: List<Game>,
    val myUpcoming: List<Game>,
)

data class VideoData(val videos: List<Video>)

data class DetailData(val summary: GameSummary, val videos: List<Video>)

class KboViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repository(app)

    val settings: StateFlow<Settings?> = repo.prefs.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _home = MutableStateFlow<Load<HomeData>>(Load.Loading)
    val home: StateFlow<Load<HomeData>> = _home.asStateFlow()

    private val _videos = MutableStateFlow<Load<VideoData>>(Load.Loading)
    val videos: StateFlow<Load<VideoData>> = _videos.asStateFlow()

    /** 영상 탭 최상단 라이브. 비어 있으면 칸을 그리지 않는다. */
    private val _lives = MutableStateFlow<Load<List<LiveVideo>>>(Load.Ok(emptyList()))
    val lives: StateFlow<Load<List<LiveVideo>>> = _lives.asStateFlow()

    private val _detail = MutableStateFlow<Load<DetailData>>(Load.Loading)
    val detail: StateFlow<Load<DetailData>> = _detail.asStateFlow()

    /** 영상 탭에서 쓰는, 내 팀이 아닌 팀을 잠깐 둘러볼 때의 선택. null 이면 내 팀. */
    private val _videoTeamOverride = MutableStateFlow<Team?>(null)

    private var homeJob: Job? = null
    private var videoJob: Job? = null
    private var detailJob: Job? = null
    private var liveJob: Job? = null

    /** 라이브 마지막 조회 시각. 검색이 100 유닛이라 탭을 오갈 때마다 부르지 않는다. */
    private var livesFetchedAt = 0L

    /** 홈에서 누른 경기를 상세 화면으로 넘기기 위해 잠시 들고 있는다. */
    private var lastGames: List<Game> = emptyList()

    fun refreshHome() {
        homeJob?.cancel()
        homeJob = viewModelScope.launch {
            _home.value = Load.Loading
            val team = repo.prefs.current().myTeam
            val today = Repository.today()
            val result = runCatching { repo.gamesAround(today, span = 7) }
            val games = result.getOrElse {
                _home.value = Load.Err(it.readableMessage())
                return@launch
            }
            lastGames = games
            val todayKey = Repository.formatDate(today)
            val mine = team?.let { t -> games.filter { it.involves(t) } }.orEmpty()
            val todayGames = games.filter { it.date == todayKey }
            val live = HomeSelection.pickMyLive(todayGames, team)
            val pinned = live.mapTo(mutableSetOf()) { it.id }
            _home.value = Load.Ok(
                HomeData(
                    live = live,
                    // 위에 고정한 내 팀 경기는 아래에서 한 번 더 보여주지 않는다.
                    // 다른 팀 라이브는 여기 이닝 표시와 함께 남는다.
                    today = todayGames.filterNot { it.id in pinned },
                    myRecent = mine
                        .filter { it.status == GameStatus.FINISHED && it.date <= todayKey }
                        .sortedByDescending { it.date }
                        .take(5),
                    myUpcoming = mine
                        .filter { it.status == GameStatus.BEFORE && it.date >= todayKey }
                        .sortedBy { it.date }
                        .take(5),
                ),
            )
        }
    }

    fun refreshVideos() {
        videoJob?.cancel()
        videoJob = viewModelScope.launch {
            _videos.value = Load.Loading
            val team = _videoTeamOverride.value ?: repo.prefs.current().myTeam
            runCatching { repo.videos(team) }
                .onSuccess { _videos.value = Load.Ok(VideoData(it)) }
                .onFailure {
                    _videos.value = Load.Err(
                        it.readableMessage(),
                        needsApiKey = it is YouTubeApi.MissingKeyException,
                    )
                }
        }
    }

    /**
     * 라이브를 다시 가져온다. [force] 가 아니면 마지막 조회 후 [LIVE_TTL_MS] 가 지났을 때만.
     */
    fun refreshLives(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - livesFetchedAt < LIVE_TTL_MS) return
        liveJob?.cancel()
        liveJob = viewModelScope.launch {
            val team = _videoTeamOverride.value ?: repo.prefs.current().myTeam
            if (team == null) {
                _lives.value = Load.Ok(emptyList())
                return@launch
            }
            _lives.value = Load.Loading
            runCatching { repo.liveVideos(team) }
                .onSuccess {
                    livesFetchedAt = System.currentTimeMillis()
                    _lives.value = Load.Ok(it)
                }
                .onFailure {
                    _lives.value = Load.Err(
                        it.readableMessage(),
                        needsApiKey = it is YouTubeApi.MissingKeyException,
                    )
                }
        }
    }

    fun loadDetail(gameId: String) {
        detailJob?.cancel()
        detailJob = viewModelScope.launch {
            _detail.value = Load.Loading
            val game = lastGames.firstOrNull { it.id == gameId } ?: findGameById(gameId)
            if (game == null) {
                _detail.value = Load.Err(GAME_NOT_FOUND)
                return@launch
            }
            val summary = runCatching { repo.summary(game) }.getOrElse {
                _detail.value = Load.Err(it.readableMessage())
                return@launch
            }
            // 영상은 있으면 좋고 없어도 그만이다. 키가 없거나 실패하면 빈 목록으로 둔다.
            val related = runCatching { repo.videos(null) }.getOrNull().orEmpty()
                .filter { it.matchesGame(game) }
            _detail.value = Load.Ok(DetailData(summary, related))
        }
    }

    /** 알림에서 바로 들어온 경우처럼 목록을 아직 안 불러온 상황을 위한 조회. */
    private suspend fun findGameById(gameId: String): Game? {
        val date = gameId.take(8)
        if (date.length != 8 || date.any { !it.isDigit() }) return null
        val formatted = "${date.take(4)}-${date.substring(4, 6)}-${date.substring(6, 8)}"
        return runCatching { repo.gamesOn(LocalDate.parse(formatted)) }
            .getOrNull()
            ?.firstOrNull { it.id == gameId }
    }

    fun setVideoTeam(team: Team?) {
        _videoTeamOverride.value = team
        refreshVideos()
        refreshLives(force = true)
    }

    fun selectTeam(team: Team) {
        viewModelScope.launch {
            repo.prefs.setMyTeam(team)
            _videoTeamOverride.value = null
            refreshHome()
            refreshVideos()
            refreshLives(force = true)
        }
    }

    fun setNotifyResult(on: Boolean) = viewModelScope.launch {
        repo.prefs.setNotifyResult(on)
        syncWorker()
    }

    fun setNotifyVideo(on: Boolean) = viewModelScope.launch {
        repo.prefs.setNotifyVideo(on)
        syncWorker()
    }

    fun setApiKey(key: String) = viewModelScope.launch {
        repo.prefs.setApiKey(key)
        refreshVideos()
        refreshLives(force = true)
    }

    /** 알림을 하나라도 켜 뒀으면 주기 작업을 걸고, 둘 다 껐으면 내린다. */
    fun syncWorker() = viewModelScope.launch {
        val s = repo.prefs.current()
        val context = getApplication<Application>()
        if (s.myTeam != null && (s.notifyResult || s.notifyVideo)) {
            GameCheckWorker.schedule(context)
        } else {
            GameCheckWorker.cancel(context)
        }
    }

    private fun Video.matchesGame(game: Game): Boolean {
        val teams = KboTitleParser.teamsIn(title)
        val home = game.home ?: return false
        val away = game.away ?: return false
        if (home !in teams || away !in teams) return false
        val (month, day) = KboTitleParser.monthDayIn(title) ?: return false
        val gameMonth = game.date.substring(5, 7).toIntOrNull()
        val gameDay = game.date.substring(8, 10).toIntOrNull()
        return month == gameMonth && day == gameDay
    }

    private fun Throwable.readableMessage(): String = when (this) {
        is YouTubeApi.MissingKeyException -> message.orEmpty()
        is YouTubeApi.ApiException -> "YouTube API ($code): $message"
        else -> message ?: this::class.simpleName.orEmpty()
    }

    companion object {
        const val GAME_NOT_FOUND = "경기를 찾을 수 없습니다"
        private const val LIVE_TTL_MS = 60_000L
    }
}

/** 영상 탭의 종류 필터. */
enum class VideoFilter(val kind: VideoKind?) {
    ALL(null),
    HIGHLIGHT(VideoKind.HIGHLIGHT),
    MOMENT(VideoKind.MOMENT),
}
