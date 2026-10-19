package discussion.com.paper

import discussion.com.paper.core.Card
import discussion.com.paper.core.Rank
import discussion.com.paper.core.Suit

/** ربط كل ورقة بملف الصورة الخاص بها: res/drawable-nodpi/card_<rank>_<suit>.png (الظهر: card_back.png). */
object CardAssets {
    val BACK: Int get() = R.drawable.card_back
    fun face(c: Card): Int = when (c.rank to c.suit) {
        Rank.QASS to Suit.SPADES -> R.drawable.card_qass_spades
        Rank.QASS to Suit.HEARTS -> R.drawable.card_qass_hearts
        Rank.QASS to Suit.CLUBS -> R.drawable.card_qass_clubs
        Rank.QASS to Suit.DIAMONDS -> R.drawable.card_qass_diamonds
        Rank.SHAIB to Suit.SPADES -> R.drawable.card_shaib_spades
        Rank.SHAIB to Suit.HEARTS -> R.drawable.card_shaib_hearts
        Rank.SHAIB to Suit.CLUBS -> R.drawable.card_shaib_clubs
        Rank.SHAIB to Suit.DIAMONDS -> R.drawable.card_shaib_diamonds
        Rank.BINT to Suit.SPADES -> R.drawable.card_bint_spades
        Rank.BINT to Suit.HEARTS -> R.drawable.card_bint_hearts
        Rank.BINT to Suit.CLUBS -> R.drawable.card_bint_clubs
        Rank.BINT to Suit.DIAMONDS -> R.drawable.card_bint_diamonds
        Rank.WALAD to Suit.SPADES -> R.drawable.card_walad_spades
        Rank.WALAD to Suit.HEARTS -> R.drawable.card_walad_hearts
        Rank.WALAD to Suit.CLUBS -> R.drawable.card_walad_clubs
        Rank.WALAD to Suit.DIAMONDS -> R.drawable.card_walad_diamonds
        Rank.TEN to Suit.SPADES -> R.drawable.card_ten_spades
        Rank.TEN to Suit.HEARTS -> R.drawable.card_ten_hearts
        Rank.TEN to Suit.CLUBS -> R.drawable.card_ten_clubs
        Rank.TEN to Suit.DIAMONDS -> R.drawable.card_ten_diamonds
        Rank.NINE to Suit.SPADES -> R.drawable.card_nine_spades
        Rank.NINE to Suit.HEARTS -> R.drawable.card_nine_hearts
        Rank.NINE to Suit.CLUBS -> R.drawable.card_nine_clubs
        Rank.NINE to Suit.DIAMONDS -> R.drawable.card_nine_diamonds
        Rank.EIGHT to Suit.SPADES -> R.drawable.card_eight_spades
        Rank.EIGHT to Suit.HEARTS -> R.drawable.card_eight_hearts
        Rank.EIGHT to Suit.CLUBS -> R.drawable.card_eight_clubs
        Rank.EIGHT to Suit.DIAMONDS -> R.drawable.card_eight_diamonds
        Rank.SEVEN to Suit.SPADES -> R.drawable.card_seven_spades
        Rank.SEVEN to Suit.HEARTS -> R.drawable.card_seven_hearts
        Rank.SEVEN to Suit.CLUBS -> R.drawable.card_seven_clubs
        Rank.SEVEN to Suit.DIAMONDS -> R.drawable.card_seven_diamonds
        else -> BACK
    }
}
