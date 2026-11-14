package com.hayoonjae.kbo

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsBaseball
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hayoonjae.kbo.data.Video
import com.hayoonjae.kbo.notify.Notifier
import com.hayoonjae.kbo.ui.GameDetailScreen
import com.hayoonjae.kbo.ui.HomeScreen
import com.hayoonjae.kbo.ui.KboViewModel
import com.hayoonjae.kbo.ui.SettingsScreen
import com.hayoonjae.kbo.ui.TeamPickerScreen
import com.hayoonjae.kbo.ui.VideoScreen
import com.hayoonjae.kbo.ui.theme.KboTheme

class MainActivity : ComponentActivity() {

    private var notificationsAllowed by mutableStateOf(true)

    private val requestNotifications = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> notificationsAllowed = granted }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 헤더 그라디언트가 상태바 밑까지 이어지므로 상태바 아이콘은 흰색으로 둔다.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        notificationsAllowed = Notifier.canPost(this)

        setContent {
            val viewModel: KboViewModel = viewModel()
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val myTeam = settings?.myTeam

            KboTheme(team = myTeam) {
                val current = settings
                when {
                    current == null -> Unit
                    current.myTeam == null -> TeamPickerScreen(onPick = viewModel::selectTeam)
                    else -> MainScaffold(
                        viewModel = viewModel,
                        settings = current,
                        notificationsAllowed = notificationsAllowed,
                        onRequestNotificationPermission = ::requestNotificationPermission,
                        onOpenVideo = ::openVideo,
                        initialGameId = intent?.getStringExtra(EXTRA_GAME_ID),
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun openVideo(video: Video) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(video.watchUrl)))
    }

    companion object {
        const val EXTRA_GAME_ID = "game_id"
    }
}

private enum class Tab(val labelRes: Int, val icon: ImageVector) {
    TODAY(R.string.tab_today, Icons.Filled.SportsBaseball),
    VIDEOS(R.string.tab_videos, Icons.Filled.PlayCircle),
    SETTINGS(R.string.tab_settings, Icons.Filled.Settings),
}

@Composable
private fun MainScaffold(
    viewModel: KboViewModel,
    settings: com.hayoonjae.kbo.data.Settings,
    notificationsAllowed: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onOpenVideo: (Video) -> Unit,
    initialGameId: String?,
) {
    var tab by remember { mutableStateOf(Tab.TODAY) }
    var openGameId by remember { mutableStateOf(initialGameId) }
    var pickingTeam by remember { mutableStateOf(false) }

    // 첫 진입과 팀 변경 시 한 번씩 불러온다.
    LaunchedEffect(settings.myTeam) {
        viewModel.refreshHome()
        viewModel.refreshVideos()
        viewModel.syncWorker()
    }

    // 라이브는 금방 바뀌므로 영상 탭에 들어올 때마다 확인한다(1분 안에 다시 오면 건너뜀).
    LaunchedEffect(tab) {
        if (tab == Tab.VIDEOS) viewModel.refreshLives()
    }

    LaunchedEffect(openGameId) {
        openGameId?.let { viewModel.loadDetail(it) }
    }

    if (pickingTeam) {
        TeamPickerScreen(onPick = {
            viewModel.selectTeam(it)
            pickingTeam = false
        })
        return
    }

    val gameId = openGameId
    if (gameId != null) {
        GameDetailScreen(
            viewModel = viewModel,
            myTeam = settings.myTeam,
            onBack = { openGameId = null },
            onOpenVideo = onOpenVideo,
        )
        return
    }

    Scaffold(
        // 위쪽 여백은 각 화면 헤더가 상태바 높이만큼 직접 잡는다.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { entry ->
                    NavigationBarItem(
                        selected = tab == entry,
                        onClick = { tab = entry },
                        icon = { Icon(entry.icon, contentDescription = null) },
                        label = { Text(stringResource(entry.labelRes)) },
                    )
                }
            }
        },
    ) { padding ->
        val content = Modifier.fillMaxSize().padding(padding)
        when (tab) {
            Tab.TODAY -> HomeScreen(
                viewModel = viewModel,
                myTeam = settings.myTeam,
                onGameClick = { openGameId = it.id },
                modifier = content,
            )

            Tab.VIDEOS -> VideoScreen(
                viewModel = viewModel,
                myTeam = settings.myTeam,
                onOpenVideo = onOpenVideo,
                onNeedApiKey = { tab = Tab.SETTINGS },
                modifier = content,
            )

            Tab.SETTINGS -> SettingsScreen(
                viewModel = viewModel,
                settings = settings,
                notificationsAllowed = notificationsAllowed,
                onRequestNotificationPermission = onRequestNotificationPermission,
                onChangeTeam = { pickingTeam = true },
                modifier = content,
            )
        }
    }
}
