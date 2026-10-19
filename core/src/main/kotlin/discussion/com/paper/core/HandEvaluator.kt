package discussion.com.paper.core

enum class HandType { ALL_DIFFERENT, ONE_PAIR, TWO_PAIRS, THREE_KIND, FOUR_KIND }

object HandEvaluator {
    /** مجموعات (العدد، القوة) مرتبة تنازليًا. */
    fun groups(hand: List<Card>): List<Pair<Int, Int>> =
        hand.groupBy { it.rank }.map { (r, l) -> l.size to r.power }
            .sortedWith(compareByDescending<Pair<Int, Int>> { it.first }.thenByDescending { it.second })

    fun type(hand: List<Card>): HandType {
        val g = groups(hand).map { it.first }
        return when {
            g.firstOrNull() == 4 -> HandType.FOUR_KIND
            g.firstOrNull() == 3 -> HandType.THREE_KIND
            g.count { it == 2 } >= 2 -> HandType.TWO_PAIRS
            g.firstOrNull() == 2 -> HandType.ONE_PAIR
            else -> HandType.ALL_DIFFERENT
        }
    }

    private fun lex(a: List<Pair<Int, Int>>, b: List<Pair<Int, Int>>): Int {
        for (i in 0 until minOf(a.size, b.size)) {
            a[i].first.compareTo(b[i].first).let { if (it != 0) return it }
            a[i].second.compareTo(b[i].second).let { if (it != 0) return it }
        }
        return a.size.compareTo(b.size).let { -it } // مجموعات أقل = يد أقوى
    }

    /** موجب إذا a أقوى من b، صفر عند التعادل. */
    fun compare(a: List<Card>, b: List<Card>, settings: RuleSettings): Int {
        if (settings.comparisonMode == ComparisonMode.TYPE_FIRST) {
            type(a).compareTo(type(b)).let { if (it != 0) return it }
        }
        return lex(groups(a), groups(b))
    }
}
