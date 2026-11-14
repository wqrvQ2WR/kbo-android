package com.hayoonjae.kbo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.hayoonjae.kbo.data.KboTitleParser
import com.hayoonjae.kbo.data.LiveVideo
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.hayoonjae.kbo.R
import com.hayoonjae.kbo.data.Team
import com.hayoonjae.kbo.data.Video
import com.hayoonjae.kbo.ui.theme.CardSurface
import com.hayoonjae.kbo.ui.theme.InkMuted

@Composable
fun VideoScreen(
    viewModel: KboViewModel,
    myTeam: Team?,
    onOpenVideo: (Video) -> Unit,
    onNeedApiKey: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.videos.collectAsStateWithLifecycle()
    val lives by viewModel.lives.collectAsStateWithLifecycle()
    val home by viewModel.home.collectAsStateWithLifecycle()
    // 홈 화면이 이미 알고 있는 "내 팀이 지금 경기 중인가" 를 그대로 쓴다.
    val myTeamPlaying = (home as? Load.Ok)?.value?.live?.isNotEmpty() == true
    var filter by remember { mutableStateOf(VideoFilter.ALL) }

    LazyColumn(modifier.fillMaxSize()) {
        item {
            GradientHeader(
                team = myTeam,
                title = stringResource(
                    R.string.videos_title,
                    myTeam?.shortName ?: stringResource(R.string.videos_all),
                ),
                subtitle = "youtube.com/@KBO1982",
                trailing = {
                    IconButton(onClick = {
                        viewModel.refreshVideos()
                        viewModel.refreshLives(force = true)
                    }) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.retry),
                            tint = Color.White,
                        )
                    }
                },
            )
        }

        // 지금 방송 중인 것이 가장 위. 없으면 칸 자체를 그리지 않는다.
        when (val l = lives) {
            is Load.Ok -> if (l.value.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.videos_live)) }
                items(l.value, key = { "live-${it.video.id}" }) { live ->
                    LiveVideoRow(live) { onOpenVideo(live.video) }
                }
            } else if (myTeamPlaying && myTeam != null) {
                // 경기는 하는데 유튜브에 중계가 없는 경우. 빈 칸이면 고장으로 보이므로 알려 준다.
                item { SectionTitle(stringResource(R.string.videos_live)) }
                item {
                    Text(
                        stringResource(R.string.live_none_for_game, myTeam.shortName),
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkMuted,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    )
                }
            }
            is Load.Loading -> item { LoadingBox() }
            // 키가 없는 경우는 아래 목록 쪽 안내가 대신한다.
            is Load.Err -> if (!l.needsApiKey) item {
                Text(
                    "${stringResource(R.string.live_error)}: ${l.message}",
                    style = MaterialTheme.typography.labelMedium,
                    color = InkMuted,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                VideoFilter.entries.forEach { option ->
                    FilterChip(
                        selected = filter == option,
                        onClick = { filter = option },
                        label = { Text(option.label()) },
                    )
                }
            }
        }

        when (val s = state) {
            is Load.Loading -> item { LoadingBox() }

            is Load.Err -> item {
                if (s.needsApiKey) {
                    MessageBox(
                        message = stringResource(R.string.need_api_key_title) + "\n\n" +
                            stringResource(R.string.need_api_key_desc),
                        actionLabel = stringResource(R.string.go_to_settings),
                        onAction = onNeedApiKey,
                    )
                } else {
                    MessageBox(
                        message = "${stringResource(R.string.error_generic)}\n${s.message}",
                        actionLabel = stringResource(R.string.retry),
                        onAction = { viewModel.refreshVideos() },
                    )
                }
            }

            is Load.Ok -> {
                val shown = s.value.videos.filter { filter.kind == null || it.kind == filter.kind }
                if (shown.isEmpty()) {
                    item { MessageBox(stringResource(R.string.no_videos)) }
                } else {
                    items(shown, key = { it.id }) { video ->
                        VideoRow(video) { onOpenVideo(video) }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
fun VideoRow(video: Video, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CardSurface)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = video.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 108.dp, height = 61.dp)
                .clip(RoundedCornerShape(8.dp)),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                KboTitleParser.cleanForDisplay(video.title),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                // publishedAt 은 "2026-09-12T11:00:05Z" 형태다. 날짜만 쓴다.
                video.publishedAt.take(10),
                style = MaterialTheme.typography.labelMedium,
                color = InkMuted,
            )
        }
    }
}

/** 방송 중인 영상 한 줄. 썸네일 위에 LIVE 표시, 아래에 채널과 동시 시청자. */
@Composable
fun LiveVideoRow(live: LiveVideo, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CardSurface)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            AsyncImage(
                model = live.video.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 108.dp, height = 61.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
            Text(
                stringResource(R.string.live_badge),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(LiveRed)
                    .padding(horizontal = 5.dp, vertical = 1.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                KboTitleParser.cleanForDisplay(live.video.title),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (live.official) {
                    Text(
                        stringResource(R.string.live_official),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    buildString {
                        append(KboTitleParser.cleanForDisplay(live.channelTitle))
                        live.viewers?.let {
                            append(" · ")
                            append(stringResource(R.string.live_viewers, "%,d".format(it)))
                        }
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = InkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private val LiveRed = Color(0xFFE62117)

@Composable
private fun VideoFilter.label(): String = when (this) {
    VideoFilter.ALL -> stringResource(R.string.videos_all)
    VideoFilter.HIGHLIGHT -> stringResource(R.string.videos_highlight)
    VideoFilter.MOMENT -> stringResource(R.string.videos_moment)
}
