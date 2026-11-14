package com.hayoonjae.kbo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hayoonjae.kbo.R
import com.hayoonjae.kbo.data.Game
import com.hayoonjae.kbo.data.GameStatus
import com.hayoonjae.kbo.data.GameSummary
import com.hayoonjae.kbo.data.ScoreBoard
import com.hayoonjae.kbo.data.Standings
import com.hayoonjae.kbo.data.Team
import com.hayoonjae.kbo.data.Video
import com.hayoonjae.kbo.ui.theme.CardSurface
import com.hayoonjae.kbo.ui.theme.Hairline
import com.hayoonjae.kbo.ui.theme.Ink
import com.hayoonjae.kbo.ui.theme.InkMuted
import com.hayoonjae.kbo.ui.theme.ScoreStyle
import com.hayoonjae.kbo.ui.theme.headerBrush

@Composable
fun GameDetailScreen(
    viewModel: KboViewModel,
    myTeam: Team?,
    onBack: () -> Unit,
    onOpenVideo: (Video) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.detail.collectAsStateWithLifecycle()

    // 하단 탭이 없는 화면이라 제스처 바 높이만큼 직접 비운다.
    LazyColumn(modifier.fillMaxSize().navigationBarsPadding()) {
        when (val s = state) {
            is Load.Loading -> {
                item { DetailHeader(null, myTeam, onBack) }
                item { LoadingBox() }
            }

            is Load.Err -> {
                item { DetailHeader(null, myTeam, onBack) }
                item { MessageBox("${stringResource(R.string.error_generic)}\n${s.message}") }
            }

            is Load.Ok -> {
                val summary = s.value.summary
                item { DetailHeader(summary.game, myTeam, onBack) }

                summary.scoreBoard?.let { board ->
                    item { SectionTitle(stringResource(R.string.scoreboard)) }
                    item { ScoreBoardTable(summary.game, board) }
                }

                item { SectionTitle(stringResource(R.string.summary_title)) }
                item { SummaryCard(summary) }

                if (s.value.videos.isNotEmpty()) {
                    item { SectionTitle(stringResource(R.string.related_videos)) }
                    items(s.value.videos, key = { it.id }) { video ->
                        VideoRow(video) { onOpenVideo(video) }
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun DetailHeader(game: Game?, myTeam: Team?, onBack: () -> Unit) {
    // 헤더 그라디언트는 내 팀이 뛰는 경기면 내 팀 색, 아니면 홈팀 색으로 깐다.
    val tintTeam = when {
        game == null -> myTeam
        myTeam != null && game.involves(myTeam) -> myTeam
        else -> game.home
    }
    Column(
        Modifier
            .fillMaxWidth()
            .background(headerBrush(tintTeam))
            .statusBarsPadding()
            .padding(bottom = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, top = 4.dp)) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.tab_today),
                    tint = Color.White,
                )
            }
            if (game != null) {
                Text(
                    "${game.date}  ${game.stadium}",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
        }

        if (game == null) {
            Spacer(Modifier.height(24.dp))
            return@Column
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeaderSide(game.away, game.awayName, Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (game.status == GameStatus.FINISHED) {
                    Text(
                        "${game.awayScore} : ${game.homeScore}",
                        style = ScoreStyle,
                        color = Color.White,
                    )
                } else {
                    Text(game.startTime, style = ScoreStyle, color = Color.White)
                }
                Text(
                    when (game.status) {
                        GameStatus.FINISHED -> stringResource(R.string.status_finished)
                        GameStatus.CANCELED -> stringResource(R.string.status_canceled)
                        GameStatus.LIVE -> game.statusInfo.ifBlank { stringResource(R.string.status_live) }
                        GameStatus.BEFORE -> stringResource(R.string.status_before)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
            HeaderSide(game.home, game.homeName, Modifier.weight(1f))
        }
    }
}

@Composable
private fun HeaderSide(team: Team?, name: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (team != null) TeamEmblem(team, size = 44.dp)
        Spacer(Modifier.height(6.dp))
        Text(
            name.ifBlank { "-" },
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
    }
}

/** 이닝별 점수 + R/H/E/B. 연장전이면 가로로 스크롤된다. */
@Composable
private fun ScoreBoardTable(game: Game, board: ScoreBoard, modifier: Modifier = Modifier) {
    val away = board.inn?.away.orEmpty()
    val home = board.inn?.home.orEmpty()
    val innings = maxOf(away.size, home.size)
    if (innings == 0) return

    Column(
        modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CardSurface)
            .padding(vertical = 10.dp)
            .horizontalScroll(rememberScrollState()),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Cell("", width = 64.dp)
            repeat(innings) { Cell("${it + 1}", muted = true) }
            Spacer(Modifier.width(8.dp))
            Cell(stringResource(R.string.scoreboard_runs), muted = true)
            Cell(stringResource(R.string.scoreboard_hits), muted = true)
            Cell(stringResource(R.string.scoreboard_errors), muted = true)
            Cell(stringResource(R.string.scoreboard_walks), muted = true)
        }
        Divider(Modifier.padding(vertical = 6.dp))
        ScoreRow(game.awayName, away, innings, board.rheb?.away)
        Spacer(Modifier.height(6.dp))
        ScoreRow(game.homeName, home, innings, board.rheb?.home)
    }
}

@Composable
private fun ScoreRow(
    name: String,
    innings: List<Int>,
    columns: Int,
    totals: com.hayoonjae.kbo.data.Rheb?,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            name.ifBlank { "-" },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(64.dp).padding(start = 10.dp),
        )
        repeat(columns) { i ->
            // 아직 치르지 않은 이닝은 0 이 아니라 빈칸으로 둔다.
            Cell(innings.getOrNull(i)?.toString() ?: "")
        }
        Spacer(Modifier.width(8.dp))
        Cell(totals?.r?.toString() ?: "", bold = true)
        Cell(totals?.h?.toString() ?: "")
        Cell(totals?.e?.toString() ?: "")
        Cell(totals?.b?.toString() ?: "")
    }
}

@Composable
private fun Cell(
    text: String,
    width: androidx.compose.ui.unit.Dp = 28.dp,
    muted: Boolean = false,
    bold: Boolean = false,
) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (muted) InkMuted else Ink,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        textAlign = TextAlign.Center,
        modifier = Modifier.width(width),
    )
}

/**
 * 경기 요약. 네이버 기록에 있는 항목만 보여준다.
 * 값이 없는 항목은 비워 두는 대신 줄 자체를 그리지 않는다.
 */
@Composable
private fun SummaryCard(summary: GameSummary, modifier: Modifier = Modifier) {
    val rows = buildList {
        summary.winningPitcher?.let { add(stringResource(R.string.label_win_pitcher) to it) }
        summary.losingPitcher?.let { add(stringResource(R.string.label_lose_pitcher) to it) }
        summary.save?.let { add(stringResource(R.string.label_save) to it) }
        summary.decisiveHit?.let { add(stringResource(R.string.label_decisive) to it) }
        summary.homeRuns?.let { add(stringResource(R.string.label_homerun) to it) }
    }

    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CardSurface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (rows.isEmpty()) {
            Text(
                stringResource(R.string.summary_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = InkMuted,
            )
        } else {
            rows.forEach { (label, value) -> SummaryLine(label, value) }
        }

        val standings = listOfNotNull(summary.awayStandings, summary.homeStandings)
        if (standings.isNotEmpty()) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(Hairline))
            standings.forEach { StandingsLine(it) }
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = InkMuted,
            modifier = Modifier.width(72.dp),
        )
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StandingsLine(standings: Standings) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            standings.name,
            style = MaterialTheme.typography.bodyMedium,
            color = InkMuted,
            modifier = Modifier.width(72.dp),
        )
        Text(
            stringResource(
                R.string.standings_value,
                standings.rank,
                standings.w,
                standings.l,
                standings.d,
            ),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
    }
}
