package com.flikk.shakechat.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Firestore collections (Double Bubble tho clash avvakunda "sc_" prefix):
 *  sc_users/{uid}                 profile
 *  sc_users/{uid}/friends/{other} friends list
 *  sc_queue/{uid}                 shake chesi wait chestunna vaallu
 *  sc_rooms/{roomId}              chat room (stranger / friend)
 *  sc_rooms/{roomId}/msgs/{id}    messages
 *  sc_reports/{id}                reports (console lo chudandi)
 */
object Repo {
    val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()
    val uid: String? get() = auth.currentUser?.uid

    private val users get() = db.collection("sc_users")
    private val queue get() = db.collection("sc_queue")
    val rooms get() = db.collection("sc_rooms")

    private const val STALE_MS = 60_000L
    private const val SEARCH_MS = 45_000L

    // ---------- Auth ----------
    suspend fun signUp(email: String, pass: String) {
        auth.createUserWithEmailAndPassword(email.trim(), pass).await()
    }

    suspend fun signIn(email: String, pass: String) {
        auth.signInWithEmailAndPassword(email.trim(), pass).await()
    }

    suspend fun resetPassword(email: String) {
        auth.sendPasswordResetEmail(email.trim()).await()
    }

    fun signOut() = auth.signOut()

    // ---------- Profile ----------
    suspend fun loadProfile(): Profile? {
        val me = uid ?: return null
        val snap = users.document(me).get().await()
        return snap.data?.let { Profile.from(me, it) }
    }

    suspend fun saveProfile(p: Profile) {
        users.document(p.uid).set(p.toMap(), SetOptions.merge()).await()
    }

    suspend fun block(other: String) {
        val me = uid ?: return
        users.document(me).update("blocked", FieldValue.arrayUnion(other)).await()
        runCatching { users.document(me).collection("friends").document(other).delete().await() }
    }

    suspend fun unblockAll() {
        val me = uid ?: return
        users.document(me).update("blocked", emptyList<String>()).await()
    }

    suspend fun report(other: String, roomId: String, reason: String) {
        val me = uid ?: return
        db.collection("sc_reports").add(
            mapOf(
                "by" to me, "against" to other, "roomId" to roomId,
                "reason" to reason, "ts" to FieldValue.serverTimestamp(),
            )
        ).await()
    }

    // ---------- Shake matching ----------

    /**
     * Shake chesaka call avutundi. Match dorikithe roomId, lekapothe (45s) null.
     * 1) Already wait chestunna vaallalo okarini "claim" cheyyadaniki try.
     * 2) Evaru lekapothe queue lo kurchoni, evaraina mana doc ki roomId pettedaaka wait.
     */
    suspend fun findMatch(me: Profile): String? {
        tryClaim(me, null)?.let { return it }

        val myTs = System.currentTimeMillis()
        val myDoc = queue.document(me.uid)
        myDoc.set(
            mapOf(
                "name" to me.name, "age" to me.age, "lang" to me.lang,
                "interests" to me.interests, "blocked" to me.blocked,
                "ts" to myTs, "roomId" to null,
            )
        ).await()

        val found = CompletableDeferred<String>()
        val reg: ListenerRegistration = myDoc.addSnapshotListener { s, _ ->
            s?.getString("roomId")?.let { if (!found.isCompleted) found.complete(it) }
        }
        try {
            return withTimeoutOrNull(SEARCH_MS) {
                var result: String? = null
                while (result == null) {
                    result = withTimeoutOrNull(6_000) { found.await() }
                    if (result == null) result = tryClaim(me, myTs)
                }
                result
            }
        } finally {
            reg.remove()
            runCatching { myDoc.delete().await() }
        }
    }

    /** Queue lo unna vaallani chusi, best match ni transaction tho pair cheyyi. */
    private suspend fun tryClaim(me: Profile, myTs: Long?): String? {
        val now = System.currentTimeMillis()
        val sameLang = runCatching {
            queue.whereEqualTo("lang", me.lang).limit(15).get().await().documents
        }.getOrDefault(emptyList())
        val pool = sameLang.ifEmpty {
            runCatching { queue.limit(15).get().await().documents }.getOrDefault(emptyList())
        }

        @Suppress("UNCHECKED_CAST")
        val candidates = pool.filter { d ->
            val ts = d.getLong("ts") ?: 0L
            val theirBlocked = (d.get("blocked") as? List<String>) ?: emptyList()
            d.id != me.uid &&
                d.get("roomId") == null &&
                now - ts < STALE_MS &&
                d.id !in me.blocked &&
                me.uid !in theirBlocked &&
                // iddaru wait chestunte, kotta vaallu paata vaallani claim chestaru (double room raakunda)
                (myTs == null || ts < myTs)
        }.sortedWith(
            compareByDescending<com.google.firebase.firestore.DocumentSnapshot> { d ->
                ((d.get("interests") as? List<String>) ?: emptyList()).count { it in me.interests }
            }.thenBy { it.getLong("ts") ?: 0L }
        )

        for (c in candidates) {
            val rid = runCatching { claim(me, c.id) }.getOrNull()
            if (rid != null) return rid
        }
        return null
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun claim(me: Profile, other: String): String? {
        val otherRef = queue.document(other)
        val selfRef = queue.document(me.uid)
        val roomRef = rooms.document()
        return db.runTransaction<String?> { tx ->
            val o = tx.get(otherRef)
            val s = tx.get(selfRef)
            if (s.exists() && s.getString("roomId") != null) return@runTransaction s.getString("roomId")
            if (!o.exists() || o.getString("roomId") != null) return@runTransaction null

            val info = mapOf(
                me.uid to mapOf("name" to me.name, "age" to me.age, "interests" to me.interests),
                other to mapOf(
                    "name" to (o.getString("name") ?: "Stranger"),
                    "age" to (o.getLong("age") ?: 18L),
                    "interests" to ((o.get("interests") as? List<String>) ?: emptyList()),
                ),
            )
            tx.set(
                roomRef,
                mapOf(
                    "members" to listOf(me.uid, other),
                    "info" to info,
                    "friend" to emptyMap<String, Boolean>(),
                    "isFriend" to false,
                    "leftBy" to null,
                    "createdAt" to System.currentTimeMillis(),
                )
            )
            tx.update(otherRef, "roomId", roomRef.id)
            if (s.exists()) tx.update(selfRef, "roomId", roomRef.id)
            roomRef.id
        }.await()
    }

    suspend fun cancelSearch() {
        val me = uid ?: return
        runCatching { queue.document(me).delete().await() }
    }

    // ---------- Room ----------
    fun messagesQuery(roomId: String): Query =
        rooms.document(roomId).collection("msgs").orderBy("cts").limitToLast(300)

    suspend fun send(roomId: String, text: String) {
        val me = uid ?: return
        rooms.document(roomId).collection("msgs").add(
            mapOf("from" to me, "text" to text, "cts" to System.currentTimeMillis())
        ).await()
    }

    suspend fun leave(roomId: String) {
        val me = uid ?: return
        runCatching { rooms.document(roomId).update("leftBy", me).await() }
    }

    suspend fun wantFriend(roomId: String) {
        val me = uid ?: return
        rooms.document(roomId).update("friend.$me", true).await()
    }

    fun friendRoomId(a: String, b: String) = "f_" + listOf(a, b).sorted().joinToString("_")

    /** Iddaru "Add friend" nokkaka – prathi okkaru tama friends list lo save chestaru. */
    suspend fun saveFriend(me: Profile, peer: Peer) {
        val fid = friendRoomId(me.uid, peer.uid)
        rooms.document(fid).set(
            mapOf(
                "members" to listOf(me.uid, peer.uid).sorted(),
                "isFriend" to true,
                "info" to mapOf(
                    me.uid to mapOf("name" to me.name, "age" to me.age, "interests" to me.interests),
                    peer.uid to mapOf("name" to peer.name, "age" to peer.age, "interests" to peer.interests),
                ),
                "createdAt" to System.currentTimeMillis(),
            ),
            SetOptions.merge(),
        ).await()
        users.document(me.uid).collection("friends").document(peer.uid).set(
            mapOf(
                "name" to peer.name, "age" to peer.age,
                "roomId" to fid, "since" to System.currentTimeMillis(),
            )
        ).await()
    }

    fun friendsQuery(): Query? {
        val me = uid ?: return null
        return users.document(me).collection("friends").orderBy("since", Query.Direction.DESCENDING)
    }

    suspend fun removeFriend(other: String) {
        val me = uid ?: return
        users.document(me).collection("friends").document(other).delete().await()
    }
}
