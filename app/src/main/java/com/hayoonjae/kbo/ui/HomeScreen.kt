package com.hayoonjae.kbo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hayoonjae.kbo.R
import com.hayoonjae.kbo.data.Game
import com.hayoonjae.kbo.data.Team

@Composable
fun HomeScreen(
    viewModel: KboViewModel,
    myTeam: Team?,
    onGameClick: (Game) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.home.collectAsStateWithLifecycle()

    LazyColumn(modifier.fillMaxSize()) {
        item {
            GradientHeader(
                team = myTeam,
                title = myTeam?.fullName ?: stringResource(R.string.app_name),
                subtitle = stringResource(R.string.today_games),
                trailing = {
                    IconButton(onClick = { viewModel.refreshHome() }) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.retry),
                            tint = Color.White,
                        )
                    }
                },
            )
        }

        when (val s = state) {
            is Load.Loading -> item { LoadingBox() }

            is Load.Err -> item {
                MessageBox(
                    message = "${stringResource(R.string.error_generic)}\n${s.message}",
                    actionLabel = stringResource(R.string.retry),
                    onAction = { viewModel.refreshHome() },
                )
            }

            is Load.Ok -> {
                val data = s.value

                // 진행 중인 경기는 다른 무엇보다 위에 온다.
                if (data.live.isNotEmpty()) {
                    item { SectionTitle(stringResource(R.string.live_now)) }
                    items(data.live, key = { "live-${it.id}" }) { game ->
                        LiveGameCard(game, myTeam) { onGameClick(game) }
                    }
                }

                if (myTeam != null && data.myRecent.isNotEmpty()) {
                    item { SectionTitle(stringResource(R.string.recent_results)) }
                    items(data.myRecent, key = { "recent-${it.id}" }) { game ->
                        MyGameRow(game, myTeam) { onGameClick(game) }
                    }
                }

                if (myTeam != null && data.myUpcoming.isNotEmpty()) {
                    item { SectionTitle(stringResource(R.string.upcoming)) }
                    items(data.myUpcoming, key = { "next-${it.id}" }) { game ->
                        MyGameRow(game, myTeam) { onGameClick(game) }
                    }
                }

                // 오늘 경기가 전부 라이브 칸으로 올라갔으면 이 칸 자체를 그리지 않는다.
                if (data.today.isNotEmpty() || data.live.isEmpty()) {
                    item { SectionTitle(stringResource(R.string.today_games)) }
                    if (data.today.isEmpty()) {
                        item { MessageBox(stringResource(R.string.no_games_today)) }
                    } else {
                        items(data.today, key = { "today-${it.id}" }) { game ->
                            GameRow(game, myTeam) { onGameClick(game) }
                        }
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

/** 팀을 아직 안 골랐을 때 가장 먼저 보여주는 화면. */
@Composable
fun TeamPickerScreen(onPick: (Team) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier.fillMaxSize().navigationBarsPadding()) {
        item {
            GradientHeader(
                team = null,
                title = stringResource(R.string.pick_team_title),
                subtitle = stringResource(R.string.pick_team_desc),
            )
            Spacer(Modifier.height(8.dp))
        }
        items(Team.entries.toList(), key = { it.code }) { team ->
            TeamPickRow(team) { onPick(team) }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun TeamPickRow(team: Team, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(listOf(team.primary, team.accent)))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TeamEmblem(team, size = 36.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(team.fullName, style = MaterialTheme.typography.titleMedium, color = Color.White)
            Text(
                team.shortName,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.85f),
            )
        }
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.9f),
        )
    }
}
