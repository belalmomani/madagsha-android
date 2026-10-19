package discussion.com.paper.core

import kotlinx.serialization.Serializable

/** كيفية مقارنة اليدين. الافتراضي يحقق المثالين الأصليين. */
enum class ComparisonMode {
    /** تُقارن المجموعات بالترتيب (العدد ثم الرتبة): قصان+مختلفتان تفوز على 7،7،8،8. */
    GROUP_LEXICOGRAPHIC,
    /** نوع التركيبة أولًا ثم الرتب (لا يحقق مثال القصّين). */
    TYPE_FIRST
}

/** عند التعادل التام في القوة. */
enum class TieBreak { BIDDER_WINS, CHALLENGER_WINS }

/** عند تجاوز الرصيد 5000. */
enum class OvershootPolicy { IGNORE_GAIN, BOUNCE_BACK }

enum class LeftoverPolicy { SET_ASIDE }

@Serializable
data class RuleSettings(
    val cardsPerPlayer: Int = 4,
    val leftoverPolicy: LeftoverPolicy = LeftoverPolicy.SET_ASIDE,
    val comparisonMode: ComparisonMode = ComparisonMode.GROUP_LEXICOGRAPHIC,
    val tieBreak: TieBreak = TieBreak.BIDDER_WINS,
    val bagStart: Int = 100,
    val unitValue: Int = 1,
    val sinkoUnits: Int = 5,
    /** خطوة الزيادة بالنقاط؛ 0 تعني = قيمة سينكو. */
    val raiseStep: Int = 0,
    val targetScore: Int = 5000,
    val overshootPolicy: OvershootPolicy = OvershootPolicy.IGNORE_GAIN,
    val allowNegativeBalance: Boolean = true,
    /** يدفع المتحدي الفاشل قيمة الطلب لصاحب الطلب. */
    val failedChallengePenalty: Boolean = true,
    /** يخسر صاحب الطلب قيمة الطلب للمتحدي الناجح (وإلا تُدفع من الكيس). */
    val successfulChallengeBidderPays: Boolean = true,
    /** في الشراكة: الجائزة تُوزَّع على زملاء الفريق فقط. */
    val teamPrizeOnlyTeammates: Boolean = true,
    /** الطلب مرة واحدة فقط لكل لاعب في الجولة؛ بعدها يستطيع الانسحاب أو المداقشة فقط. */
    val oneBidPerPlayer: Boolean = true
) {
    val sinko: Int get() = sinkoUnits * unitValue
    val step: Int get() = if (raiseStep > 0) raiseStep else sinko
}
