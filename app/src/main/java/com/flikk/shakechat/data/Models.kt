package com.flikk.shakechat.data

import java.util.Calendar

data class Profile(
    val uid: String,
    val name: String,
    val birthYear: Int,
    val gender: String,
    val lang: String,
    val interests: List<String>,
    val blocked: List<String> = emptyList(),
) {
    val age: Int get() = Calendar.getInstance().get(Calendar.YEAR) - birthYear

    fun toMap(): Map<String, Any> = mapOf(
        "name" to name,
        "birthYear" to birthYear,
        "gender" to gender,
        "lang" to lang,
        "interests" to interests,
        "blocked" to blocked,
    )

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun from(uid: String, m: Map<String, Any?>): Profile? {
            val name = m["name"] as? String ?: return null
            return Profile(
                uid = uid,
                name = name,
                birthYear = (m["birthYear"] as? Number)?.toInt() ?: 2000,
                gender = m["gender"] as? String ?: "",
                lang = m["lang"] as? String ?: "Telugu",
                interests = (m["interests"] as? List<String>) ?: emptyList(),
                blocked = (m["blocked"] as? List<String>) ?: emptyList(),
            )
        }
    }
}

data class Msg(val id: String, val from: String, val text: String, val cts: Long)

data class Friend(val uid: String, val name: String, val age: Int, val roomId: String, val since: Long)

/** Room lo unna inko person gurinchi chinna info (stranger ki full profile chupinchamu). */
data class Peer(val uid: String, val name: String, val age: Int, val interests: List<String>)

object Options {
    val languages = listOf("Telugu", "Hindi", "Tamil", "Kannada", "Malayalam", "Marathi", "Bengali", "English")
    val genders = listOf("Male", "Female", "Other", "Prefer not to say")

    // emoji + name
    val interests = listOf(
        "🎬" to "Movies", "🎵" to "Music", "🏏" to "Cricket", "🎮" to "Gaming",
        "😂" to "Memes", "💻" to "Tech", "✈️" to "Travel", "🍛" to "Food",
        "💪" to "Fitness", "📚" to "Study", "🎨" to "Art", "🙏" to "Devotional",
        "📸" to "Photography", "💃" to "Dance", "🚗" to "Bikes & Cars", "📈" to "Business",
    )

    fun emoji(interest: String): String = interests.firstOrNull { it.second == interest }?.first ?: "✨"
}
