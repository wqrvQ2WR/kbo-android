package com.hayoonjae.kbo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.hayoonjae.kbo.R
import com.hayoonjae.kbo.data.GameStatus
import com.hayoonjae.kbo.data.Outcome
import com.hayoonjae.kbo.data.Team
import com.hayoonjae.kbo.ui.theme.DrawColor
import com.hayoonjae.kbo.ui.theme.Hairline
import com.hayoonjae.kbo.ui.theme.InkMuted
import com.hayoonjae.kbo.ui.theme.LoseColor
import com.hayoonjae.kbo.ui.theme.WinColor
import com.hayoonjae.kbo.ui.theme.headerBrush

/** 팀 색 그라디언트를 깐 화면 상단. */
@Composable
fun GradientHeader(
    team: Team?,
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(headerBrush(team))
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (team != null) {
                TeamEmblem(team, size = 40.dp)
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = Color.White)
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
            }
            trailing?.invoke()
        }
    }
}

@Composable
fun TeamEmblem(team: Team, size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    AsyncImage(
        model = team.emblemUrl,
        contentDescription = team.fullName,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(percent = 50))
            .background(Color.White)
            .padding(3.dp),
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier.padding(horizontal = 20.dp, vertical = 10.dp),
    )
}

@Composable
fun StatusChip(status: GameStatus, statusInfo: String, modifier: Modifier = Modifier) {
    val (label, tint) = when (status) {
        GameStatus.BEFORE -> stringOf(R.string.status_before) to InkMuted
        GameStatus.LIVE -> (statusInfo.ifBlank { stringOf(R.string.status_live) }) to WinColor
        GameStatus.FINISHED -> stringOf(R.string.status_finished) to InkMuted
        GameStatus.CANCELED -> stringOf(R.string.status_canceled) to LoseColor
    }
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        color = tint,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(tint.copy(alpha = 0.10f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
fun OutcomeBadge(outcome: Outcome, modifier: Modifier = Modifier) {
    if (outcome == Outcome.NONE) return
    val (label, tint) = when (outcome) {
        Outcome.WIN -> stringOf(R.string.outcome_win) to WinColor
        Outcome.LOSE -> stringOf(R.string.outcome_lose) to LoseColor
        else -> stringOf(R.string.outcome_draw) to DrawColor
    }
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = Color.White,
        textAlign = TextAlign.Center,
        modifier = modifier
            .size(24.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(tint)
            .padding(top = 2.dp),
    )
}

@Composable
fun Divider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Hairline),
    )
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun MessageBox(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = InkMuted,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            OutlinedButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
private fun stringOf(id: Int): String = androidx.compose.ui.res.stringResource(id)
