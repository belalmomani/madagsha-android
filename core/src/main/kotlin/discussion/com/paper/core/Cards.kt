package discussion.com.paper.core

import kotlin.random.Random

/** الرتب من الأقوى إلى الأضعف: قص، شايب، بنت، ولد، 10، 9، 8، 7 */
enum class Rank(val arabic: String, val power: Int) {
    QASS("قص", 8), SHAIB("شايب", 7), BINT("بنت", 6), WALAD("ولد", 5),
    TEN("10", 4), NINE("9", 3), EIGHT("8", 2), SEVEN("7", 1)
}

enum class Suit { SPADES, HEARTS, CLUBS, DIAMONDS }

data class Card(val rank: Rank, val suit: Suit)

object Deck {
    fun full(): List<Card> = Rank.entries.flatMap { r -> Suit.entries.map { s -> Card(r, s) } }
    fun shuffled(rng: Random): List<Card> = full().shuffled(rng)
}

data class Deal(val hands: List<List<Card>>, val leftover: List<Card>)

object Dealer {
    fun deal(playerCount: Int, settings: RuleSettings, rng: Random = Random.Default): Deal {
        require(playerCount in 4..8) { "عدد اللاعبين من 4 إلى 8" }
        val need = playerCount * settings.cardsPerPlayer
        require(need <= 32) { "المجموعة لا تكفي: $need ورقة مطلوبة" }
        val deck = Deck.shuffled(rng)
        val hands = (0 until playerCount).map { i ->
            deck.subList(i * settings.cardsPerPlayer, (i + 1) * settings.cardsPerPlayer).toList()
        }
        // الأوراق المتبقية (عند 5 أو 7 لاعبين مثلًا) تُترك جانبًا؛ انظر LeftoverPolicy
        return Deal(hands, deck.drop(need))
    }
}
