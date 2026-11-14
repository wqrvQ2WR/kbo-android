package com.hayoonjae.kbo.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.hayoonjae.kbo.MainActivity
import com.hayoonjae.kbo.R
import com.hayoonjae.kbo.data.GameSummary
import com.hayoonjae.kbo.data.Outcome
import com.hayoonjae.kbo.data.Team
import com.hayoonjae.kbo.data.Video

object Notifier {

    const val CHANNEL_RESULT = "game_result"
    const val CHANNEL_VIDEO = "new_video"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_RESULT,
                context.getString(R.string.channel_result),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = context.getString(R.string.channel_result_desc) },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_VIDEO,
                context.getString(R.string.channel_video),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = context.getString(R.string.channel_video_desc) },
        )
    }

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /** 내 팀 경기가 끝났을 때. 제목에 승패와 스코어, 본문에 한 줄 요약을 담는다. */
    fun postResult(context: Context, team: Team, summary: GameSummary) {
        val game = summary.game
        val mine = game.scoreOf(team)
        val theirs = game.scoreAgainst(team)
        val verdict = when (game.outcomeFor(team)) {
            Outcome.WIN -> context.getString(R.string.outcome_win)
            Outcome.LOSE -> context.getString(R.string.outcome_lose)
            Outcome.DRAW -> context.getString(R.string.outcome_draw)
            Outcome.NONE -> context.getString(R.string.outcome_none)
        }
        val title = context.getString(
            R.string.notif_result_title,
            team.shortName, verdict, mine, theirs, game.opponentOf(team),
        )
        post(
            context = context,
            channel = CHANNEL_RESULT,
            id = game.id.hashCode(),
            title = title,
            text = summaryLine(context, summary),
            intent = openAppIntent(context, game.id),
        )
    }

    /** 내 팀 관련 새 영상이 올라왔을 때. 누르면 유튜브로 바로 간다. */
    fun postVideo(context: Context, team: Team, video: Video) {
        post(
            context = context,
            channel = CHANNEL_VIDEO,
            id = video.id.hashCode(),
            title = context.getString(R.string.notif_video_title, team.shortName),
            text = com.hayoonjae.kbo.data.KboTitleParser.cleanForDisplay(video.title),
            intent = PendingIntent.getActivity(
                context,
                video.id.hashCode(),
                Intent(Intent.ACTION_VIEW, Uri.parse(video.watchUrl)),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            ),
        )
    }

    /** 승리투수·결승타·홈런을 있는 것만 이어 붙인다. 없는 항목은 지어내지 않고 그냥 뺀다. */
    fun summaryLine(context: Context, summary: GameSummary): String {
        val parts = buildList {
            summary.winningPitcher?.let { add(context.getString(R.string.label_win_pitcher_inline, it)) }
            summary.losingPitcher?.let { add(context.getString(R.string.label_lose_pitcher_inline, it)) }
            summary.save?.let { add(context.getString(R.string.label_save_inline, it)) }
            summary.decisiveHit?.let { add(context.getString(R.string.label_decisive_inline, it)) }
        }
        return if (parts.isEmpty()) context.getString(R.string.summary_unavailable) else parts.joinToString(" · ")
    }

    private fun openAppIntent(context: Context, gameId: String): PendingIntent =
        PendingIntent.getActivity(
            context,
            gameId.hashCode(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(MainActivity.EXTRA_GAME_ID, gameId)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun post(
        context: Context,
        channel: String,
        id: Int,
        title: String,
        text: String,
        intent: PendingIntent,
    ) {
        if (!canPost(context)) return
        ensureChannels(context)
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_ball)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(intent)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
    }
}
