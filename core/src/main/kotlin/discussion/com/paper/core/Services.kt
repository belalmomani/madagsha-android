package discussion.com.paper.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// ---------- الإعلانات: فشل الإعلان لا يعطل اللعبة ----------
interface AdBackend {
    fun init(): Boolean
    fun showInterstitial(): Boolean
}
class AdManager(private val backend: AdBackend?, private val enabled: Boolean, private val minRoundsBetween: Int = 3) {
    private var ready = false
    private var roundsSince = 0
    fun start() { ready = try { enabled && backend?.init() == true } catch (_: Throwable) { false } }
    /** يُستدعى فقط عند انتقال طبيعي (نهاية جولة/نتائج) ولا يحجب اللعب. */
    fun onRoundEnded(): Boolean {
        roundsSince++
        if (!ready || roundsSince < minRoundsBetween) return false
        roundsSince = 0
        return try { backend?.showInterstitial() == true } catch (_: Throwable) { false }
    }
}

// ---------- الحفظ المحلي والمزامنة ----------
@Serializable
data class MatchSnapshot(
    val playerIds: List<Int>, val names: Map<Int, String>, val avatars: Map<Int, Int>,
    val balances: Map<Int, Int>, val history: List<RoundRecord>, val settings: RuleSettings,
    val teams: List<List<Int>> = emptyList()
)
interface LocalStore { fun write(key: String, value: String); fun read(key: String): String? }
class InMemoryStore : LocalStore {
    private val m = mutableMapOf<String, String>()
    override fun write(key: String, value: String) { m[key] = value }
    override fun read(key: String) = m[key]
}
interface RemoteStore { fun push(uid: String, json: String) }

class MatchRepository(private val local: LocalStore, private val remote: RemoteStore? = null, private val uid: () -> String? = { null }) {
    private val json = Json { ignoreUnknownKeys = true }
    /** يحفظ محليًا دائمًا؛ فشل الشبكة/المزامنة لا يؤثر على الحفظ المحلي. يعيد true إذا نجحت المزامنة. */
    fun save(s: MatchSnapshot): Boolean {
        val text = json.encodeToString(s)
        local.write("match", text)
        val u = uid() ?: return false
        return try { remote?.push(u, text); remote != null } catch (_: Throwable) { false }
    }
    fun load(): MatchSnapshot? = local.read("match")?.let { try { json.decodeFromString<MatchSnapshot>(it) } catch (_: Exception) { null } }
}

// ---------- اختيار الشخصيات: 8 شخصيات، لا تكرار ----------
class AvatarPicker(val total: Int = 8) {
    private val taken = mutableMapOf<Int, Int>() // avatar -> player
    fun isTaken(avatar: Int) = avatar in taken
    fun pick(player: Int, avatar: Int): Boolean {
        require(avatar in 1..total)
        if (taken[avatar].let { it != null && it != player }) return false
        taken.entries.removeAll { it.value == player }
        taken[avatar] = player
        return true
    }
    fun avatarOf(player: Int) = taken.entries.firstOrNull { it.value == player }?.key
    fun free(): List<Int> = (1..total).filter { it !in taken }
}
