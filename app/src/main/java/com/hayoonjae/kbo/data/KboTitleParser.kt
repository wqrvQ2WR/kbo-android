package com.hayoonjae.kbo.data

/**
 * KBO 공식 유튜브 채널의 제목 규칙을 해석한다.
 *
 * 실제 채널에서 쓰이는 형태는 세 가지다.
 *  - 경기 하이라이트: `[NC다이노스 vs 두산베어스] 9.12(토) 야구 하이라이트｜2026 KBO 리그｜KBO X TVING`
 *  - 크보모먼트 클립: `... 역전까지! | LG vs 삼성 | 9.12 | 크보모먼트 | KBO 야구 하이라이트`
 *  - 퓨처스리그(2군): `[2026 메디힐 KBO 퓨처스리그 H/L] KIA타이거즈 vs 한화이글스(09.11)`
 *
 * 구분자가 전각(｜)과 반각(|) 둘 다 쓰이고, `|  vs  |` 처럼 팀 이름이 비어 있거나
 * `NC  vs 한화` 처럼 공백이 겹치는 제목이 실제로 올라오므로 전부 견뎌야 한다.
 */
object KboTitleParser {

    private val BRACKET = Regex("""^\s*\[([^]]*)]""")
    private val VS = Regex("""(.*?)\s+vs\s+(.*)""", RegexOption.IGNORE_CASE)
    private val MONTH_DAY = Regex("""(?<!\d)(\d{1,2})\s*[.]\s*(\d{1,2})(?!\d)""")
    private val HASHTAG = Regex("""#[^\s#]*""")
    private val SPACES = Regex("""\s{2,}""")

    fun kindOf(title: String): VideoKind = when {
        title.contains("크보모먼트") -> VideoKind.MOMENT
        title.contains("하이라이트") && BRACKET.containsMatchIn(title) -> VideoKind.HIGHLIGHT
        else -> VideoKind.OTHER
    }

    /** 1군 KBO 리그 영상인지. 퓨처스(2군)는 제외한다. */
    fun isFirstTeam(title: String): Boolean = !title.contains("퓨처스")

    /**
     * 제목에 등장하는 팀들.
     *
     * 먼저 `A vs B` 로 적힌 대진 구간에서 찾고, 그 구간이 비어 있으면(실제로 있는 경우다)
     * 제목 전체를 훑는다. 전체를 훑을 때 `LG`·`KT` 같은 라틴 약칭은 영문 단어 안에
     * 우연히 들어맞을 수 있으므로 앞뒤가 영문자가 아닐 때만 인정한다.
     */
    fun teamsIn(title: String): Set<Team> {
        // 라이브 방송은 제목 끝에 "#삼성라이온즈#lg트윈스#기아타이거즈" 처럼 모든 팀 태그를
        // 도배하는 일이 흔하다. 태그는 내용이 아니므로 떼고 본다.
        val text = stripHashtags(title)
        matchupSegment(text)?.let { (left, right) ->
            val teams = listOfNotNull(teamOf(left), teamOf(right)).toSet()
            if (teams.isNotEmpty()) return teams
        }
        return Team.entries.filter { team -> team.titleAliases.any { text.containsAlias(it) } }.toSet()
    }

    fun stripHashtags(title: String): String = HASHTAG.replace(title, " ").trim()

    /**
     * 화면에 띄울 제목. 검색 API 가 돌려주는 HTML 엔티티(&amp; 등)를 풀고,
     * 이모지와 장식 기호, 끝에 도배된 해시태그를 걷어낸다.
     */
    fun cleanForDisplay(title: String): String {
        val unescaped = stripHashtags(title)
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
        val sb = StringBuilder(unescaped.length)
        var i = 0
        while (i < unescaped.length) {
            val cp = unescaped.codePointAt(i)
            if (!isDecoration(cp)) sb.appendCodePoint(cp)
            i += Character.charCount(cp)
        }
        return sb.toString().replace(SPACES, " ").trim()
    }

    private fun isDecoration(cp: Int): Boolean {
        val type = Character.getType(cp)
        return type == Character.OTHER_SYMBOL.toInt() ||
            (type == Character.MODIFIER_SYMBOL.toInt() && cp >= 0x1F3FB) ||
            cp == 0xFE0F || cp == 0x200D || cp in 0xE0020..0xE007F
    }

    /** 제목에서 읽어낸 경기 날짜(월, 일). 없으면 null. */
    fun monthDayIn(title: String): Pair<Int, Int>? {
        val m = MONTH_DAY.find(title) ?: return null
        val month = m.groupValues[1].toIntOrNull() ?: return null
        val day = m.groupValues[2].toIntOrNull() ?: return null
        return if (month in 1..12 && day in 1..31) month to day else null
    }

    /** `[A vs B]` 또는 `| A vs B |` 구간을 좌/우로 잘라 돌려준다. */
    private fun matchupSegment(title: String): Pair<String, String>? {
        BRACKET.find(title)?.groupValues?.get(1)?.let { inside ->
            VS.matchEntire(inside.trim())?.let { return it.groupValues[1] to it.groupValues[2] }
        }
        // 크보모먼트는 구분자로 나뉜 칸 중 하나가 대진이다. 전각/반각 둘 다 쓰인다.
        for (part in title.split('|', '｜')) {
            VS.matchEntire(part.trim())?.let { return it.groupValues[1] to it.groupValues[2] }
        }
        return null
    }

    private fun teamOf(raw: String): Team? {
        val s = raw.trim()
        if (s.isEmpty()) return null
        // 긴 표기부터 맞춰야 "KIA타이거즈" 가 "KIA" 로 먼저 끊기지 않는다.
        return Team.entries.firstOrNull { team ->
            team.titleAliases.sortedByDescending { it.length }.any { s.contains(it) }
        }
    }

    private fun String.containsAlias(alias: String): Boolean {
        if (alias.any { it.isKoreanSyllable() }) return contains(alias)
        var from = 0
        while (true) {
            val i = indexOf(alias, from, ignoreCase = true)
            if (i < 0) return false
            val before = getOrNull(i - 1)
            val after = getOrNull(i + alias.length)
            if (!before.isLatinLetter() && !after.isLatinLetter()) return true
            from = i + 1
        }
    }

    private fun Char.isKoreanSyllable(): Boolean = this in '가'..'힣'

    private fun Char?.isLatinLetter(): Boolean = this != null && (this in 'a'..'z' || this in 'A'..'Z')
}
