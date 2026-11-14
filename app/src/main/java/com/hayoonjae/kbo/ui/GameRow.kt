package com.hayoonjae.kbo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hayoonjae.kbo.data.Game
import com.hayoonjae.kbo.data.GameStatus
import com.hayoonjae.kbo.data.Team
import com.hayoonjae.kbo.ui.theme.CardSurface
import com.hayoonjae.kbo.ui.theme.Ink
import com.hayoonjae.kbo.ui.theme.InkMuted

/**
 * 경기 한 줄. 원정팀을 위, 홈팀을 아래에 두고 이긴 쪽 점수를 굵게 쓴다.
 * 아직 안 끝난 경기는 점수 대신 시작 시각을 보여준다.
 */
@Composable
fun GameRow(
    game: Game,
    myTeam: Team?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val finished = game.status == GameStatus.FINISHED
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CardSurface)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusChip(game.status, game.statusInfo)
            Spacer(Modifier.width(8.dp))
            Text(
                game.stadium.ifBlank { "-" },
                style = MaterialTheme.typography.labelMedium,
                color = InkMuted,
            )
            Spacer(Modifier.weight(1f))
            Text(
                if (finished) game.date else "${game.date}  ${game.startTime}",
                style = MaterialTheme.typography.labelMedium,
                color = InkMuted,
            )
        }

        SideLine(
            team = game.away,
            name = game.awayName,
            score = game.awayScore,
            showScore = finished,
            winner = finished && game.awayScore > game.homeScore,
            highlight = myTeam != null && game.away == myTeam,
        )
        SideLine(
            team = game.home,
            name = game.homeName,
            score = game.homeScore,
            showScore = finished,
            winner = finished && game.homeScore > game.awayScore,
            highlight = myTeam != null && game.home == myTeam,
        )
    }
}

@Composable
private fun SideLine(
    team: Team?,
    name: String,
    score: Int,
    showScore: Boolean,
    winner: Boolean,
    highlight: Boolean,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        if (team != null) {
            TeamEmblem(team, size = 26.dp)
        } else {
            Spacer(Modifier.width(26.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(
            name.ifBlank { "-" },
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
            color = if (highlight) (team?.primary ?: Ink) else Ink,
            modifier = Modifier.weight(1f),
        )
        if (showScore) {
            Text(
                score.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (winner) FontWeight.Bold else FontWeight.Normal,
                color = if (winner) Ink else InkMuted,
                textAlign = TextAlign.End,
                modifier = Modifier.width(32.dp),
            )
        }
    }
}

/** 내 팀 최근 경기를 승패 배지와 함께 간단히 보여주는 줄. */
@Composable
fun MyGameRow(game: Game, myTeam: Team, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(CardSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutcomeBadge(game.outcomeFor(myTeam))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                buildString {
                    append(if (game.isHomeGameFor(myTeam)) "vs " else "@ ")
                    append(game.opponentOf(myTeam))
                },
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                "${game.date}  ${game.stadium}",
                style = MaterialTheme.typography.labelMedium,
                color = InkMuted,
            )
        }
        if (game.status == GameStatus.FINISHED) {
            Text(
                "${game.scoreOf(myTeam)} : ${game.scoreAgainst(myTeam)}",
                style = MaterialTheme.typography.titleMedium,
            )
        } else {
            Text(game.startTime, style = MaterialTheme.typography.titleMedium, color = InkMuted)
        }
    }
}
