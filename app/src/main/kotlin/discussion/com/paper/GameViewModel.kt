package discussion.com.paper

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import discussion.com.paper.core.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class Screen { HOME, MODE, COUNT, TEAMS, AVATAR, DEAL, TABLE, RESULT, PRIZE, FINAL, HISTORY, SETTINGS, HELP, PRIVACY }

class GameViewModel(app: Application) : AndroidViewModel(app) {
    val store = PrefsStore(app)
    private val remote = FirebaseRemote(app)
    val repo = MatchRepository(store, remote) { remote.uid() }
    var ads: AdManager? = null

    var screen by mutableStateOf(Screen.HOME)
    var teamMode by mutableStateOf(false)
    var playerCount by mutableStateOf(6)
    var settings by mutableStateOf(RuleSettings())
    val picker = AvatarPicker(8)
    var avatarVersion by mutableStateOf(0)
    var humanName by mutableStateOf("أنت")

    var ledger: Ledger? = null
    var round: BiddingRound? = null
    var hands: Map<Int, List<Card>> = emptyMap()
    var outcome by mutableStateOf<RoundOutcome?>(null)
    var tick by mutableStateOf(0)
    val log = mutableStateListOf<String>()
    var roundNo = 0
    var message by mutableStateOf("")

    val botNames = listOf("أحمد", "نور", "خالد", "فهد", "ريم", "سعد", "ليث")
    fun name(id: Int) = if (id == 0) humanName else botNames[(id - 1) % botNames.size]
    val ids get() = (0 until playerCount).toList()

    fun go(s: Screen) { screen = s }

    fun pickAvatar(a: Int): Boolean { val ok = picker.pick(0, a); avatarVersion++; return ok }

    fun startMatch() {
        // الشخصيات الباقية تُوزَّع على الذكاء الاصطناعي دون تكرار
        val free = picker.free().toMutableList()
        for (id in 1 until playerCount) if (picker.avatarOf(id) == null) picker.pick(id, free.removeAt(0))
        val teams = if (teamMode) listOf(ids.filter { it % 2 == 0 }, ids.filter { it % 2 == 1 }) else emptyList()
        ledger = Ledger(settings, ids, teams)
        roundNo = 0
        newRound()
    }

    fun newRound() {
        val l = ledger ?: return
        val deal = Dealer.deal(playerCount, settings)
        hands = ids.associateWith { deal.hands[it] }
        round = BiddingRound(settings, ids, hands, settings.bagStart, roundNo % playerCount)
        roundNo++
        outcome = null; log.clear(); screen = Screen.DEAL
    }

    fun beginTable() { screen = Screen.TABLE; tick++; driveBots() }

    private fun driveBots() = viewModelScope.launch {
        val r = round ?: return@launch
        while (!r.isFinished && r.currentPlayer != 0) {
            delay(900)
            botAct(r)
            tick++
        }
        if (r.isFinished) finishRound(r.outcome!!)
    }

    private fun strength(h: List<Card>) = HandEvaluator.groups(h).first().let { it.first * 10 + it.second }

    private fun botAct(r: BiddingRound) {
        val p = r.currentPlayer
        val s = strength(hands.getValue(p))
        val bids = r.legalBids()
        val rnd = kotlin.random.Random.nextInt(100)
        when {
            r.topBidder != null && r.topBidder != p && s >= 25 && rnd < 35 -> {
                val o = r.challenge(p); log += "${name(p)} يداقش ${name(o.loserId ?: 0)}"
            }
            bids.isNotEmpty() && s >= 18 -> { val b = bids[minOf(bids.size - 1, (s - 18) / 4)]; r.bid(p, b); log += "${name(p)} يطلب $b" }
            bids.isNotEmpty() && r.topBidder == null && s >= 12 -> { r.bid(p, bids.first()); log += "${name(p)} سينكو" }
            else -> { r.fold(p); log += "${name(p)} ينسحب" }
        }
    }

    // ---- أفعال اللاعب البشري ----
    fun humanBid(a: Int) { val r = round ?: return; if (r.currentPlayer != 0 || r.isFinished) return; r.bid(0, a); log += "${name(0)} يطلب $a"; afterHuman(r) }
    fun humanFold() { val r = round ?: return; if (r.currentPlayer != 0 || r.isFinished) return; r.fold(0); log += "${name(0)} ينسحب"; afterHuman(r) }
    fun humanChallenge() {
        val r = round ?: return
        if (r.currentPlayer != 0 || r.isFinished || r.topBidder == null || r.topBidder == 0) { message = "لا يمكن المداقشة الآن"; return }
        r.challenge(0); afterHuman(r)
    }
    private fun afterHuman(r: BiddingRound) { tick++; if (r.isFinished) finishRound(r.outcome!!) else driveBots() }

    private fun finishRound(o: RoundOutcome) {
        outcome = o
        screen = if (o.winnerId == 0 && o.amount > 1) Screen.PRIZE else Screen.RESULT
        if (screen == Screen.RESULT) applyPrize(emptyMap())
    }

    fun applyPrize(shares: Map<Int, Int>): String? {
        val o = outcome ?: return null
        val l = ledger ?: return null
        l.validateDistribution(o.winnerId, o.amount, shares)?.let { return it }
        l.applyOutcome(o, shares)
        save()
        ads?.let { try { it.onRoundEnded() } catch (_: Throwable) {} }
        screen = if (l.winners().isNotEmpty()) Screen.FINAL else Screen.RESULT
        return null
    }

    fun save() {
        val l = ledger ?: return
        repo.save(MatchSnapshot(ids, ids.associateWith { name(it) }, ids.associateWith { picker.avatarOf(it) ?: 0 }, l.balances.toMap(), l.history.toList(), settings, l.teams))
    }
}
