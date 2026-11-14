package com.hayoonjae.kbo.data

import androidx.compose.ui.graphics.Color

/**
 * KBO 10개 구단.
 *
 * [code] 는 네이버 스포츠 KBO API 의 팀 코드이고, [titleAliases] 는 유튜브 영상 제목에
 * 실제로 등장하는 표기들이다. 공식 채널은 "[NC다이노스 vs 두산베어스]" 처럼 붙여 쓴
 * 풀네임과 "| NC vs 두산 |" 같은 약칭을 쓰고, 경기 중 라이브(입중계)는 "엘지편파",
 * "트윈스" 처럼 팬들이 부르는 이름을 쓰므로 그것까지 담는다.
 *
 * [channelId] 는 구단 공식 유튜브 채널이다. 채널 설명에 공식 채널임이 적혀 있는 것만
 * 넣었다 (팀명 검색 상위에 팬 채널이 섞여 나오므로 검색 결과를 그대로 쓰지 않는다).
 */
enum class Team(
    val code: String,
    val shortName: String,
    val fullName: String,
    val primary: Color,
    val channelId: String,
    val titleAliases: List<String>,
) {
    LG("LG", "LG", "LG 트윈스", Color(0xFFC30452), "UCL6QZZxb-HR4hCh_eFAnQWA", listOf("LG트윈스", "LG", "엘지", "트윈스")),
    OB("OB", "두산", "두산 베어스", Color(0xFF1A1748), "UCsebzRfMhwYfjeBIxNX1brg", listOf("두산베어스", "두산", "베어스")),
    SS("SS", "삼성", "삼성 라이온즈", Color(0xFF074CA1), "UCMWAku3a3h65QpLm63Jf2pw", listOf("삼성라이온즈", "삼성", "라이온즈")),
    HT("HT", "KIA", "KIA 타이거즈", Color(0xFFEA0029), "UCKp8knO8a6tSI1oaLjfd9XA", listOf("KIA타이거즈", "KIA", "기아", "타이거즈")),
    LT("LT", "롯데", "롯데 자이언츠", Color(0xFF041E42), "UCAZQZdSY5_YrziMPqXi-Zfw", listOf("롯데자이언츠", "롯데", "자이언츠")),
    SK("SK", "SSG", "SSG 랜더스", Color(0xFFCE0E2D), "UCt8iRtgjVqm5rJHNl1TUojg", listOf("SSG랜더스", "SSG", "랜더스")),
    HH("HH", "한화", "한화 이글스", Color(0xFFFC4E00), "UCdq4Ji3772xudYRUatdzRrg", listOf("한화이글스", "한화", "이글스")),
    NC("NC", "NC", "NC 다이노스", Color(0xFF315288), "UC8_FRgynMX8wlGsU6Jh3zKg", listOf("NC다이노스", "NC", "엔씨", "다이노스")),
    KT("KT", "KT", "kt wiz", Color(0xFFE8112D), "UCvScyjGkBUx2CJDMNAi9Twg", listOf("KT위즈", "KT", "케이티", "위즈")),
    WO("WO", "키움", "키움 히어로즈", Color(0xFF820024), "UC_MA8-XEaVmvyayPzG66IKg", listOf("키움히어로즈", "키움", "히어로즈"));

    /** 팀 컬러를 흰색 쪽으로 당겨 만든 그라디언트 끝 색. 남색 계열도 어둡게 깔리지 않게 한다. */
    val accent: Color get() = primary.lightened(0.42f)

    val emblemUrl: String get() = "https://sports-phinf.pstatic.net/team/kbo/default/$code.png"

    companion object {
        fun byCode(code: String?): Team? = entries.firstOrNull { it.code == code }
    }
}

private fun Color.lightened(amount: Float): Color = Color(
    red = red + (1f - red) * amount,
    green = green + (1f - green) * amount,
    blue = blue + (1f - blue) * amount,
    alpha = alpha,
)
