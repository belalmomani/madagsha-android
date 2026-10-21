package discussion.com.paper.core

import kotlin.random.Random
import kotlin.test.*

private fun c(r: Rank, s: Suit = Suit.SPADES) = Card(r, s)
private val S = RuleSettings()
private fun h(vararg r: Rank) = r.mapIndexed { i, x -> Card(x, Suit.entries[i % 4]) }

class DeckTests {
    @Test fun deckHas32UniqueCards() { assertEquals(32, Deck.full().toSet().size) }
    @Test fun fourOfEachRank() { Rank.entries.forEach { r -> assertEquals(4, Deck.full().count { it.rank == r }) } }
    @Test fun dealNoDuplicatesForAllCounts() {
        for (n in 4..8) {
            val d = Dealer.deal(n, S, Random(n))
            val all = d.hands.flatten() + d.leftover
            assertEquals(32, all.toSet().size); assertTrue(d.hands.all { it.size == 4 })
        }
    }
    @Test fun leftoverFor5And7() {
        assertEquals(12, Dealer.deal(5, S).leftover.size); assertEquals(4, Dealer.deal(7, S).leftover.size)
    }
    @Test fun rejectsBadCounts() { assertFailsWith<IllegalArgumentException> { Dealer.deal(3, S) }; assertFailsWith<IllegalArgumentException> { Dealer.deal(9, S) } }
}

class HandTests {
    @Test fun rankOrder() { assertTrue(Rank.QASS.power > Rank.SHAIB.power && Rank.EIGHT.power > Rank.SEVEN.power) }
    @Test fun twoQassBeatTwoPairs78() {
        val a = h(Rank.QASS, Rank.QASS, Rank.NINE, Rank.TEN)
        val b = h(Rank.SEVEN, Rank.SEVEN, Rank.EIGHT, Rank.EIGHT)
        assertTrue(HandEvaluator.compare(a, b, S) > 0)
    }
    @Test fun threeNinesBeatTwoPairs() {
        val a = h(Rank.NINE, Rank.NINE, Rank.NINE, Rank.SEVEN)
        val b = h(Rank.QASS, Rank.QASS, Rank.SHAIB, Rank.SHAIB)
        assertTrue(HandEvaluator.compare(a, b, S) > 0)
    }
    @Test fun twoPairsBeatOnePairWhenTopEqual() {
        val a = h(Rank.QASS, Rank.QASS, Rank.SEVEN, Rank.SEVEN)
        val b = h(Rank.QASS, Rank.QASS, Rank.EIGHT, Rank.NINE)
        assertTrue(HandEvaluator.compare(a, b, S) > 0)
    }
    @Test fun fourQassBeatFourShaib() {
        assertTrue(HandEvaluator.compare(h(Rank.QASS, Rank.QASS, Rank.QASS, Rank.QASS), h(Rank.SHAIB, Rank.SHAIB, Rank.SHAIB, Rank.SHAIB), S) > 0)
    }
    @Test fun fourBeatsThree() { assertTrue(HandEvaluator.compare(h(Rank.SEVEN, Rank.SEVEN, Rank.SEVEN, Rank.SEVEN), h(Rank.QASS, Rank.QASS, Rank.QASS, Rank.SHAIB), S) > 0) }
    @Test fun typeDetection() {
        assertEquals(HandType.ALL_DIFFERENT, HandEvaluator.type(h(Rank.QASS, Rank.SHAIB, Rank.BINT, Rank.WALAD)))
        assertEquals(HandType.TWO_PAIRS, HandEvaluator.type(h(Rank.QASS, Rank.QASS, Rank.BINT, Rank.BINT)))
    }
    @Test fun typeFirstModeIsDifferent() {
        val t = S.copy(comparisonMode = ComparisonMode.TYPE_FIRST)
        assertTrue(HandEvaluator.compare(h(Rank.QASS, Rank.QASS, Rank.NINE, Rank.TEN), h(Rank.SEVEN, Rank.SEVEN, Rank.EIGHT, Rank.EIGHT), t) < 0)
    }
}

class BiddingTests {
    private fun round(h0: List<Card>, h1: List<Card>) =
        BiddingRound(S, listOf(0, 1, 2, 3), mapOf(0 to h0, 1 to h1, 2 to h(Rank.SEVEN, Rank.EIGHT, Rank.NINE, Rank.TEN), 3 to h(Rank.SEVEN, Rank.EIGHT, Rank.NINE, Rank.WALAD)))
    private val weak = h(Rank.SEVEN, Rank.EIGHT, Rank.NINE, Rank.TEN)
    private val strong = h(Rank.QASS, Rank.QASS, Rank.SHAIB, Rank.BINT)

    @Test fun sinkoIsMinimumFive() { assertEquals(5, round(weak, strong).legalBids().first()) }
    @Test fun allInIsFullBag() { assertEquals(100, round(weak, strong).legalBids().last()) }
    @Test fun ascendingOnly() {
        val r = round(weak, strong); r.bid(0, 20)
        assertFailsWith<IllegalArgumentException> { r.bid(1, 20) }
        assertFailsWith<IllegalArgumentException> { r.bid(1, 10) }
        r.bid(1, 25)
    }
    @Test fun foldsLeaveLastBidderWinner() {
        val r = round(weak, strong); r.bid(0, 10); r.fold(1); r.fold(2); r.fold(3)
        assertEquals(RoundOutcome.BidWon(0, 10), r.outcome)
    }
    @Test fun successfulChallenge() {
        val r = round(weak, strong); r.bid(0, 50)
        val o = r.challenge(1)
        assertTrue(o is RoundOutcome.ChallengeWon && o.winnerId == 1 && o.loserId == 0)
    }
    @Test fun failedChallenge() {
        val r = round(strong, weak); r.bid(0, 50)
        assertTrue(r.challenge(1) is RoundOutcome.ChallengeLost)
    }
    @Test fun bidOnlyOncePerPlayer() {
        val r = round(weak, strong); r.bid(0, 10); r.bid(1, 15); r.bid(2, 20); r.bid(3, 25)
        assertEquals(0, r.currentPlayer); assertTrue(r.legalBids().isEmpty())
        assertFailsWith<IllegalArgumentException> { r.bid(0, 30) }
        r.fold(0); assertEquals(1, r.currentPlayer)
    }
    @Test fun bidTwiceAllowedWhenSettingOff() {
        val r = BiddingRound(S.copy(oneBidPerPlayer = false), listOf(0, 1), mapOf(0 to weak, 1 to strong))
        r.bid(0, 10); r.bid(1, 15); r.bid(0, 20)
    }
    @Test fun winnerGainsOwnBidLoserLosesOwnBid() {
        val r = round(strong, weak); r.bid(0, 10); r.bid(1, 15); r.bid(2, 20); r.bid(3, 25)
        assertEquals(RoundOutcome.ChallengeWon(0, 3, 10, 25), r.challenge(0))
    }
    @Test fun timeoutBidsSinkoAutomatically() {
        val r = round(weak, strong); assertTrue(r.onTimeout(0)); assertEquals(5, r.currentBid)
    }
    @Test fun timeoutFoldsWhenSinkoNoLongerLegal() {
        val r = round(weak, strong); r.bid(0, 20); assertFalse(r.onTimeout(1)); assertTrue(1 in r.folded)
    }
    @Test fun cannotChallengeWithoutBid() { assertFailsWith<IllegalStateException> { round(weak, strong).challenge(0) } }
}

class LedgerTests {
    private fun ledger(s: RuleSettings = S) = Ledger(s, listOf(0, 1, 2, 3))
    @Test fun prizeDistribution() {
        val l = ledger(); l.applyOutcome(RoundOutcome.BidWon(0, 100), mapOf(1 to 30, 2 to 20))
        assertEquals(50, l.balances[0]); assertEquals(30, l.balances[1]); assertEquals(20, l.balances[2])
    }
    @Test fun distributionCannotExceedPrize() { assertFailsWith<IllegalArgumentException> { ledger().applyOutcome(RoundOutcome.BidWon(0, 10), mapOf(1 to 11)) } }
    @Test fun challengeMovesPoints() {
        val l = ledger(); l.applyOutcome(RoundOutcome.ChallengeWon(1, 0, 40))
        assertEquals(40, l.balances[1]); assertEquals(-40, l.balances[0])
    }
    @Test fun loserPaysOwnBid() {
        val l = ledger(); l.applyOutcome(RoundOutcome.ChallengeWon(0, 3, 10, 25))
        assertEquals(10, l.balances[0]); assertEquals(-25, l.balances[3])
    }
    @Test fun recipientGetsGiftOnly() {
        val l = ledger(); l.applyOutcome(RoundOutcome.ChallengeWon(0, 3, 50, 80), mapOf(3 to 10))
        assertEquals(40, l.balances[0]); assertEquals(10, l.balances[3])
    }
    @Test fun nonRecipientLoserStillPaysOwnBid() {
        val l = ledger(); l.applyOutcome(RoundOutcome.ChallengeWon(0, 3, 50, 80), mapOf(1 to 10))
        assertEquals(40, l.balances[0]); assertEquals(10, l.balances[1]); assertEquals(-80, l.balances[3])
    }
    @Test fun plainWinGiftOnly() {
        val l = ledger(); l.applyOutcome(RoundOutcome.BidWon(0, 50), mapOf(2 to 10))
        assertEquals(40, l.balances[0]); assertEquals(10, l.balances[2]); assertEquals(0, l.balances[1])
    }
    @Test fun mustReachExactly5000() {
        val l = ledger(S.copy(targetScore = 100)); l.balances[0] = 95
        l.applyOutcome(RoundOutcome.BidWon(0, 10)); assertEquals(95, l.balances[0]); assertTrue(l.winners().isEmpty())
        l.applyOutcome(RoundOutcome.BidWon(0, 5)); assertEquals(listOf(0), l.winners())
    }
    @Test fun teamPrizeOnlyTeammates() {
        val l = Ledger(S, listOf(0, 1, 2, 3), listOf(listOf(0, 2), listOf(1, 3)))
        assertNotNull(l.validateDistribution(0, 50, mapOf(1 to 10))); assertNull(l.validateDistribution(0, 50, mapOf(2 to 10)))
    }
}

class ServiceTests {
    private class FailingAds : AdBackend { override fun init(): Boolean = throw RuntimeException("x"); override fun showInterstitial(): Boolean = throw RuntimeException("x") }
    private class FailingRemote : RemoteStore { override fun push(uid: String, json: String) { throw java.io.IOException("offline") } }
    private val snap = MatchSnapshot(listOf(0, 1), mapOf(0 to "أنت"), mapOf(0 to 1), mapOf(0 to 10, 1 to -10), emptyList(), S)

    @Test fun adFailureNeverThrows() { val m = AdManager(FailingAds(), true, 1); m.start(); assertFalse(m.onRoundEnded()) }
    @Test fun adsDisabledWithoutId() { val m = AdManager(null, false, 1); m.start(); assertFalse(m.onRoundEnded()) }
    @Test fun localSaveWorksOffline() {
        val repo = MatchRepository(InMemoryStore(), FailingRemote()) { "u1" }
        assertFalse(repo.save(snap)); assertEquals(snap, repo.load())
    }
    @Test fun roundTripSerialization() { val r = MatchRepository(InMemoryStore()); r.save(snap); assertEquals(snap, r.load()) }
    @Test fun avatarsAreUnique() {
        val p = AvatarPicker(); assertTrue(p.pick(0, 3)); assertFalse(p.pick(1, 3)); assertTrue(p.pick(1, 4)); assertEquals(6, p.free().size)
        assertTrue(p.pick(0, 5)); assertTrue(p.pick(1, 3))
    }
}
