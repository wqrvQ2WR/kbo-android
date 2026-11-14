package com.hayoonjae.kbo.notify

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.hayoonjae.kbo.data.GameStatus
import com.hayoonjae.kbo.data.Prefs
import com.hayoonjae.kbo.data.Repository
import com.hayoonjae.kbo.data.Team
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * 주기적으로 내 팀의 경기 결과와 새 영상을 확인해 알림을 띄운다.
 *
 * 한 번 알린 경기·영상은 [Prefs.markNotifiedOnce] 로 걸러 다시 울리지 않는다.
 */
class GameCheckWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repo = Repository(applicationContext)
        val settings = runCatching { repo.prefs.current() }.getOrNull() ?: return Result.success()
        val team = settings.myTeam ?: return Result.success()
        if (!Notifier.canPost(applicationContext)) return Result.success()

        if (settings.notifyResult) {
            checkResults(repo, team)
        }
        if (settings.notifyVideo && settings.hasApiKey) {
            checkVideos(repo, team)
        }
        return Result.success()
    }

    private suspend fun checkResults(repo: Repository, team: Team) {
        // 심야에 끝나는 경기가 있어 어제까지 같이 본다.
        val today = Repository.today()
        val games = runCatching { repo.gamesAround(today, span = 1) }.getOrNull().orEmpty()
        val finished = games.filter { it.involves(team) && it.status == GameStatus.FINISHED }
        for (game in finished) {
            if (!repo.prefs.markNotifiedOnce(Prefs.NotifyKind.GAME, game.id)) continue
            val summary = runCatching { repo.summary(game) }.getOrNull() ?: continue
            Notifier.postResult(applicationContext, team, summary)
        }
    }

    private suspend fun checkVideos(repo: Repository, team: Team) {
        val latest = runCatching { repo.videos(team) }.getOrNull().orEmpty().take(10)
        if (latest.isEmpty()) return
        val fresh = latest.filter { repo.prefs.markNotifiedOnce(Prefs.NotifyKind.VIDEO, it.id) }
        // 처음 켠 회차에는 최신 영상이 전부 "새 영상"으로 잡힌다. 그때 한꺼번에 울리지
        // 않도록 기록만 남기고 넘어가고, 다음 회차부터 실제로 추가된 것만 알린다.
        if (fresh.size == latest.size) return
        fresh.take(3).forEach { Notifier.postVideo(applicationContext, team, it) }
    }

    companion object {
        private const val UNIQUE_NAME = "kbo_game_check"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<GameCheckWorker>(30, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_NAME)
        }
    }
}
