package com.hayoonjae.kbo.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hayoonjae.kbo.R
import com.hayoonjae.kbo.data.Game
import com.hayoonjae.kbo.data.Team
import com.hayoonjae.kbo.ui.theme.ScoreStyle

/**
 * 최상단 라이브 카드.
 *
 * 내 팀이 뛰는 경기면 팀 색 그라디언트로, 아니면 홈팀 색으로 깐다.
 * 점수를 크게 두고 statusInfo("5회말")를 붙인다.
 */
@Composable
fun LiveGameCard(
    game: Game,
    myTeam: Team?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val tintTeam = when {
        myTeam != null && game.involves(myTeam) -> myTeam
        else -> game.home ?: game.away
    }
    val colors = if (tintTeam != null) {
        listOf(tintTeam.primary, tintTeam.accent)
    } else {
        listOf(com.hayoonjae.kbo.ui.theme.DefaultPrimary, com.hayoonjae.kbo.ui.theme.DefaultPrimary)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(colors))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LiveBadge()
            Spacer(Modifier.width(8.dp))
            Text(
                game.statusInfo.ifBlank { stringResource(R.string.status_live) },
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
            )
            Spacer(Modifier.weight(1f))
            Text(
                game.stadium.ifBlank { "-" },
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.85f),
            )
        }

        Spacer(Modifier.size(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            LiveSide(game.away, game.awayName, Modifier.weight(1f))
            Text(
                "${game.awayScore} : ${game.homeScore}",
                style = ScoreStyle,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            LiveSide(game.home, game.homeName, Modifier.weight(1f))
        }
    }
}

@Composable
private fun LiveSide(team: Team?, name: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (team != null) TeamEmblem(team, size = 34.dp)
        Spacer(Modifier.size(5.dp))
        Text(
            name.ifBlank { "-" },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
    }
}

/** 천천히 깜빡이는 점 + LIVE 글자. 이모지 대신 도형으로 그린다. */
@Composable
private fun LiveBadge() {
    val transition = rememberInfiniteTransition(label = "live")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White.copy(alpha = 0.22f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Box(
            Modifier
                .size(7.dp)
                .alpha(pulse)
                .clip(CircleShape)
                .background(Color.White),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            stringResource(R.string.live_badge),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}

@Composable
private fun stringResource(id: Int): String = androidx.compose.ui.res.stringResource(id)
