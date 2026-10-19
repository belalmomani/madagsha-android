package discussion.com.paper.core

sealed interface RoundOutcome {
    val winnerId: Int
    val loserId: Int?
    val amount: Int
    data class BidWon(override val winnerId: Int, override val amount: Int) : RoundOutcome { override val loserId: Int? = null }
    data class ChallengeWon(override val winnerId: Int, override val loserId: Int, override val amount: Int) : RoundOutcome
    data class ChallengeLost(override val winnerId: Int, override val loserId: Int, override val amount: Int) : RoundOutcome
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
    private var turn = startIndex % playerIds.size
    val folded: Set<Int> get() = playerIds.toSet() - active.toSet()
    val activePlayers: List<Int> get() = active.toList()
    val isFinished get() = outcome != null
    val currentPlayer: Int get() = playerIds[turn]

    /** الطلبات المسموحة: تصاعدية فقط، من سينكو إلى الكيس كاملًا (أول أن). */
    fun legalBids(): List<Int> {
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
        currentBid = amount; topBidder = player; advance()
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
        val o = if (challengerWins) RoundOutcome.ChallengeWon(challenger, bidder, currentBid)
        else RoundOutcome.ChallengeLost(bidder, challenger, currentBid)
        outcome = o
        return o
    }
}
