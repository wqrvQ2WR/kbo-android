package com.hayoonjae.kbo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.hayoonjae.kbo.data.Team

/** 팀을 안 골랐을 때 쓰는 기본 색. KBO 로고 계열의 선명한 파랑이다. */
val DefaultPrimary = Color(0xFF1B6FE8)
private val DefaultAccent = Color(0xFF5AA2FF)

val Surface = Color(0xFFFAFBFF)
val CardSurface = Color(0xFFFFFFFF)
val Ink = Color(0xFF15181F)
val InkMuted = Color(0xFF6B7280)
val Hairline = Color(0xFFE6E9F0)

val WinColor = Color(0xFF0E9F6E)
val LoseColor = Color(0xFFE02424)
val DrawColor = Color(0xFF8A8F9A)

/** 팀 색에서 헤더용 그라디언트를 만든다. 남색 계열 팀도 밝게 깔리도록 끝 색을 띄운다. */
fun headerBrush(team: Team?): Brush = Brush.linearGradient(
    colors = if (team == null) {
        listOf(DefaultPrimary, DefaultAccent)
    } else {
        listOf(team.primary, team.accent)
    },
)

private val AppTypography = Typography().let { base ->
    // 한글 자막이 많이 들어가므로 줄간격을 조금 넉넉하게 잡는다.
    base.copy(
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

/** 스코어처럼 자리수가 흔들리면 안 되는 숫자에 쓴다. */
val ScoreStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Bold,
    fontSize = 28.sp,
)

@Composable
fun KboTheme(team: Team?, content: @Composable () -> Unit) {
    val primary = team?.primary ?: DefaultPrimary
    // 밝은 배경 한 벌만 쓴다. 시스템이 다크 모드여도 색이 뒤집히지 않는다.
    val scheme = lightColorScheme(
        primary = primary,
        onPrimary = Color.White,
        secondary = team?.accent ?: DefaultAccent,
        background = Surface,
        onBackground = Ink,
        surface = CardSurface,
        onSurface = Ink,
        surfaceVariant = Color(0xFFF1F4FA),
        onSurfaceVariant = InkMuted,
        outline = Hairline,
    )
    MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
}
