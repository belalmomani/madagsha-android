package discussion.com.paper.core

sealed interface RoundOutcome {
    val winnerId: Int
    val loserId: Int?
    /** ما يربحه الفائز. */
    val amount: Int
    /** ما يخسره الخاسر. */
    val loserAmount: Int
    data class BidWon(override val winnerId: Int, override val amount: Int) : RoundOutcome { override val loserId: Int? = null; override val loserAmount: Int = 0 }
    data class ChallengeWon(override val winnerId: Int, override val loserId: Int, override val amount: Int, override val loserAmount: Int = amount) : RoundOutcome
    data class ChallengeLost(override val winnerId: Int, override val loserId: Int, override val amount: Int, override val loserAmount: Int = amount) : RoundOutcome
}

/** جولة طلب واحدة. الترتيب: كل لاعب نشط يطلب أو ينسحب أو يداقش. */
class BiddingRound(
    val settings: RuleSettings,
    val playerIds: List<Int>,
    val hands: Map<Int, List<Card>>,
    val bag: Int = settings.bagStart,
    startIndex: Int = 0
) {
    var currentBid = 0; private set
    var topBidder: Int? = null; private set
    var outcome: RoundOutcome? = null; private set
    private val active = playerIds.toMutableList()
    private val bidders = mutableSetOf<Int>()
    private val bids = mutableMapOf<Int, Int>()
    private var turn = startIndex % playerIds.size
    val folded: Set<Int> get() = playerIds.toSet() - active.toSet()
    val activePlayers: List<Int> get() = active.toList()
    val isFinished get() = outcome != null
    val currentPlayer: Int get() = playerIds[turn]

    /** الطلبات المسموحة: تصاعدية فقط، من سينكو إلى الكيس كاملًا (أول أن). */
    fun legalBids(): List<Int> {
        if (settings.oneBidPerPlayer && currentPlayer in bidders) return emptyList()
        val list = generateSequence(settings.sinko) { it + settings.step }.takeWhile { it < bag }.filter { it > currentBid }.toMutableList()
        if (bag > currentBid) list.add(bag)
        return list
    }
    fun isAllIn(amount: Int) = amount >= bag

    private fun advance() {
        do { turn = (turn + 1) % playerIds.size } while (playerIds[turn] !in active)
    }
    private fun checkEnd() {
        if (active.size == 1) {
            val w = active.first()
            outcome = RoundOutcome.BidWon(w, if (currentBid == 0) settings.sinko else currentBid)
            topBidder = topBidder ?: w
        }
    }

    fun bid(player: Int, amount: Int) {
        check(!isFinished && player == currentPlayer) { "ليس دور اللاعب" }
        require(amount in legalBids()) { "طلب غير مسموح: $amount" }
        currentBid = amount; topBidder = player; bidders += player; bids[player] = amount; advance()
    }

    fun fold(player: Int) {
        check(!isFinished && player == currentPlayer) { "ليس دور اللاعب" }
        active.remove(player)
        if (active.isNotEmpty() && active.size > 1) { turn = playerIds.indexOf(nextAfter(player)) }
        else if (active.size == 1) checkEnd()
    }
    private fun nextAfter(p: Int): Int {
        var i = playerIds.indexOf(p)
        do { i = (i + 1) % playerIds.size } while (playerIds[i] !in active)
        return playerIds[i]
    }

    /** مداقشة: يكشف المتحدي أوراقه أمام صاحب الطلب الأعلى. */
    fun challenge(challenger: Int): RoundOutcome {
        check(!isFinished && challenger == currentPlayer) { "ليس دور اللاعب" }
        val bidder = checkNotNull(topBidder) { "لا يوجد طلب لمداقشته" }
        require(challenger != bidder) { "لا يداقش اللاعب نفسه" }
        val cmp = HandEvaluator.compare(hands.getValue(challenger), hands.getValue(bidder), settings)
        val challengerWins = cmp > 0 || (cmp == 0 && settings.tieBreak == TieBreak.CHALLENGER_WINS)
        fun own(p: Int) = if (settings.payoutMode == PayoutMode.OWN_BID) (bids[p] ?: currentBid) else currentBid
        val o = if (challengerWins) RoundOutcome.ChallengeWon(challenger, bidder, own(challenger), own(bidder))
        else RoundOutcome.ChallengeLost(bidder, challenger, own(bidder), own(challenger))
        outcome = o
        return o
    }

    /** انتهى وقت اللاعب: سينكو تلقائي إن كان مسموحًا، وإلا انسحاب. يعيد true إذا طُلب سينكو. */
    fun onTimeout(player: Int): Boolean {
        check(!isFinished && player == currentPlayer) { "ليس دور اللاعب" }
        return if (settings.sinko in legalBids()) { bid(player, settings.sinko); true } else { fold(player); false }
    }
}
