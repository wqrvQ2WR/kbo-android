package com.hayoonjae.kbo.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hayoonjae.kbo.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("kbo_settings")

data class Settings(
    val myTeam: Team?,
    val notifyResult: Boolean,
    val notifyVideo: Boolean,
    /** 사용자가 설정 화면에서 직접 넣은 키. 비어 있으면 빌드에 심어진 키를 쓴다. */
    val userApiKey: String,
) {
    val apiKey: String get() = userApiKey.ifBlank { BuildConfig.YOUTUBE_API_KEY }
    val hasApiKey: Boolean get() = apiKey.isNotBlank()
}

class Prefs(private val context: Context) {

    private object Keys {
        val myTeam = stringPreferencesKey("my_team")
        val notifyResult = booleanPreferencesKey("notify_result")
        val notifyVideo = booleanPreferencesKey("notify_video")
        val apiKey = stringPreferencesKey("youtube_api_key")
        val notifiedGames = stringSetPreferencesKey("notified_games")
        val notifiedVideos = stringSetPreferencesKey("notified_videos")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            myTeam = Team.byCode(p[Keys.myTeam]),
            notifyResult = p[Keys.notifyResult] ?: true,
            notifyVideo = p[Keys.notifyVideo] ?: true,
            userApiKey = p[Keys.apiKey].orEmpty(),
        )
    }

    suspend fun current(): Settings = settings.first()

    suspend fun setMyTeam(team: Team?) = context.dataStore.edit { p ->
        if (team == null) p.remove(Keys.myTeam) else p[Keys.myTeam] = team.code
    }

    suspend fun setNotifyResult(on: Boolean) = context.dataStore.edit { it[Keys.notifyResult] = on }

    suspend fun setNotifyVideo(on: Boolean) = context.dataStore.edit { it[Keys.notifyVideo] = on }

    suspend fun setApiKey(key: String) = context.dataStore.edit { it[Keys.apiKey] = key.trim() }

    /**
     * [id] 를 아직 알리지 않았다면 알림 기록에 남기고 true 를 돌려준다.
     * 같은 경기·영상으로 두 번 울리지 않게 하는 장치다.
     */
    suspend fun markNotifiedOnce(kind: NotifyKind, id: String): Boolean {
        val key = when (kind) {
            NotifyKind.GAME -> Keys.notifiedGames
            NotifyKind.VIDEO -> Keys.notifiedVideos
        }
        var fresh = false
        context.dataStore.edit { p ->
            val seen = p[key].orEmpty()
            if (id !in seen) {
                fresh = true
                // 무한정 쌓이지 않게 최근 200개만 남긴다.
                p[key] = (seen + id).toList().takeLast(200).toSet()
            }
        }
        return fresh
    }

    enum class NotifyKind { GAME, VIDEO }
}
