package discussion.com.paper.core

import kotlinx.serialization.Serializable

@Serializable
data class RoundRecord(val number: Int, val winnerId: Int, val loserId: Int?, val amount: Int, val kind: String, val distribution: Map<Int, Int> = emptyMap())

/** الأرصدة. الفوز النهائي عند الوصول إلى الهدف (5000) بالضبط. */
class Ledger(val settings: RuleSettings, val playerIds: List<Int>, val teams: List<List<Int>> = emptyList()) {
    val balances: MutableMap<Int, Int> = playerIds.associateWith { 0 }.toMutableMap()
    val history = mutableListOf<RoundRecord>()

    private fun teamOf(p: Int) = teams.firstOrNull { p in it }

    private fun add(p: Int, delta: Int) {
        var nb = balances.getValue(p) + delta
        if (!settings.allowNegativeBalance && nb < 0) nb = 0
        if (nb > settings.targetScore) {
            when (settings.overshootPolicy) {
                OvershootPolicy.IGNORE_GAIN -> nb = balances.getValue(p)
                OvershootPolicy.BOUNCE_BACK -> nb = settings.targetScore - (nb - settings.targetScore)
            }
        }
        balances[p] = nb
    }

    /** يتحقق من توزيع الجائزة: المجموع ≤ الجائزة، لا قيم سالبة، ولا مستلمين خارج الفريق إن لزم. */
    fun validateDistribution(winner: Int, prize: Int, shares: Map<Int, Int>): String? {
        if (shares.values.any { it < 0 }) return "مبلغ سالب"
        if (shares.values.sum() > prize) return "المجموع يتجاوز الجائزة"
        if (winner in shares.keys) return "الفائز لا يوزع لنفسه"
        if (shares.keys.any { it !in playerIds }) return "لاعب غير موجود"
        val team = teamOf(winner)
        if (team != null && settings.teamPrizeOnlyTeammates && shares.keys.any { it !in team }) return "التوزيع لزملاء الفريق فقط"
        return null
    }

    fun applyOutcome(o: RoundOutcome, shares: Map<Int, Int> = emptyMap()): RoundRecord {
        val prize = o.amount
        validateDistribution(o.winnerId, prize, shares)?.let { throw IllegalArgumentException(it) }
        when (o) {
            is RoundOutcome.BidWon -> { }
            is RoundOutcome.ChallengeWon -> if (settings.successfulChallengeBidderPays) add(o.loserId, -prize)
            is RoundOutcome.ChallengeLost -> if (settings.failedChallengePenalty) add(o.loserId, -prize)
        }
        add(o.winnerId, prize - shares.values.sum())
        shares.forEach { (p, v) -> add(p, v) }
        val rec = RoundRecord(history.size + 1, o.winnerId, o.loserId, prize, o::class.simpleName ?: "", shares)
        history += rec
        return rec
    }

    fun teamScore(team: List<Int>) = team.sumOf { balances.getValue(it) }

    /** الفائز بالمباراة: لاعب (أو فريق) رصيده == الهدف بالضبط. */
    fun winners(): List<Int> =
        if (teams.isEmpty()) balances.filterValues { it == settings.targetScore }.keys.toList()
        else teams.filter { teamScore(it) == settings.targetScore }.flatten()
}
