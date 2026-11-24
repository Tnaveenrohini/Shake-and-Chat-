package com.flikk.shakechat.data

/**
 * Stranger chat safety:
 *  - Bad words -> ****
 *  - Phone numbers / @handles / links -> hidden (friends ayyaka matrame share cheyyochu)
 */
object Filter {
    private val badWords = listOf(
        // English
        "fuck", "fucker", "fucking", "shit", "bitch", "bastard", "asshole", "dick", "pussy",
        "slut", "whore", "nude", "nudes", "boobs", "cock",
        // Hindi (roman)
        "chutiya", "chutiye", "madarchod", "bhenchod", "behenchod", "bhosdi", "bhosdike",
        "randi", "gandu", "lund", "harami", "kutta", "kutti",
        // Telugu (roman)
        "dengu", "dengey", "lanja", "lanjakoduku", "modda", "puku", "gudda", "nakodaka",
        // Tamil (roman)
        "punda", "pundai", "otha", "thevidiya", "sunni",
    )

    private val badRegex = Regex(
        "\\b(" + badWords.joinToString("|") { Regex.escape(it) } + ")\\b",
        RegexOption.IGNORE_CASE,
    )

    // 7+ digits (spaces / - / . madhyalo unna kuda)
    private val phoneRegex = Regex("(?:\\+?\\d[\\s\\-.]?){7,}")
    private val handleRegex = Regex("@[A-Za-z0-9_.]{3,}")
    private val linkRegex = Regex("(https?://\\S+|www\\.\\S+|\\b\\S+\\.(com|in|me|io|net|org|link)\\b\\S*)", RegexOption.IGNORE_CASE)

    fun clean(text: String, stranger: Boolean): String {
        var t = badRegex.replace(text) { "*".repeat(it.value.length) }
        if (stranger) {
            t = phoneRegex.replace(t) { "📵" }
            t = handleRegex.replace(t) { "📵" }
            t = linkRegex.replace(t) { "🔗❌" }
        }
        return t
    }

    fun hadPrivateInfo(text: String): Boolean =
        phoneRegex.containsMatchIn(text) || handleRegex.containsMatchIn(text) || linkRegex.containsMatchIn(text)
}
