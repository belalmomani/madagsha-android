package discussion.com.paper

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import discussion.com.paper.core.*

val Green = Color(0xFF0B3D24); val Gold = Color(0xFFE8B923); val Cream = Color(0xFFF7F1E1)

@Composable fun avatarRes(i: Int): Int = when (i) {
    1 -> R.drawable.avatar_1; 2 -> R.drawable.avatar_2; 3 -> R.drawable.avatar_3; 4 -> R.drawable.avatar_4
    5 -> R.drawable.avatar_5; 6 -> R.drawable.avatar_6; 7 -> R.drawable.avatar_7; else -> R.drawable.avatar_8 }

@Composable fun GoldButton(text: String, enabled: Boolean = true, modifier: Modifier = Modifier, onClick: () -> Unit) =
    Button(onClick, enabled = enabled, modifier = modifier.heightIn(min = 48.dp), colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black)) { Text(text, fontWeight = FontWeight.Bold) }

@Composable fun Title(t: String) = Text(t, color = Gold, fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(12.dp))

@Composable fun Page(content: @Composable ColumnScope.() -> Unit) =
    Column(Modifier.fillMaxSize().background(Green).statusBarsPadding().navigationBarsPadding().padding(12.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, content = content)

@Composable fun cardLabel(c: Card): String = c.rank.arabic + when (c.suit) { Suit.SPADES -> "♠"; Suit.HEARTS -> "♥"; Suit.CLUBS -> "♣"; Suit.DIAMONDS -> "♦" }

@Composable fun CardView(c: Card?, modifier: Modifier = Modifier) {
    val red = c?.suit == Suit.HEARTS || c?.suit == Suit.DIAMONDS
    Box(modifier.size(64.dp, 92.dp).clip(RoundedCornerShape(8.dp)).background(if (c == null) Color(0xFF14573A) else Cream).border(2.dp, Gold, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
        if (c != null) Text(cardLabel(c), color = if (red) Color(0xFFC62828) else Color.Black, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable fun AppRoot(vm: GameViewModel) {
    when (vm.screen) {
        Screen.HOME -> Home(vm); Screen.MODE -> Mode(vm); Screen.COUNT -> Count(vm); Screen.TEAMS -> Teams(vm)
        Screen.AVATAR -> Avatars(vm); Screen.DEAL -> Deal(vm); Screen.TABLE -> Table(vm)
        Screen.RESULT -> Result(vm); Screen.PRIZE -> Prize(vm); Screen.FINAL -> Final(vm)
        Screen.HISTORY -> History(vm); Screen.SETTINGS -> Settings(vm); Screen.HELP -> Help(vm); Screen.PRIVACY -> Privacy(vm)
    }
}

@Composable fun Home(vm: GameViewModel) = Page {
    Image(painterResource(R.mipmap.ic_launcher), null, Modifier.size(180.dp).clip(RoundedCornerShape(32.dp)))
    Title("مداقشة")
    GoldButton("لعبة جديدة", modifier = Modifier.fillMaxWidth()) { vm.go(Screen.MODE) }
    Spacer(Modifier.height(8.dp)); GoldButton("سجل الجولات", modifier = Modifier.fillMaxWidth()) { vm.go(Screen.HISTORY) }
    Spacer(Modifier.height(8.dp)); GoldButton("الإعدادات", modifier = Modifier.fillMaxWidth()) { vm.go(Screen.SETTINGS) }
    Spacer(Modifier.height(8.dp)); GoldButton("المساعدة", modifier = Modifier.fillMaxWidth()) { vm.go(Screen.HELP) }
    Spacer(Modifier.height(8.dp)); GoldButton("الخصوصية والإعلانات", modifier = Modifier.fillMaxWidth()) { vm.go(Screen.PRIVACY) }
}

@Composable fun Mode(vm: GameViewModel) = Page {
    Title("اختر النمط")
    GoldButton("فردي", modifier = Modifier.fillMaxWidth()) { vm.teamMode = false; vm.go(Screen.COUNT) }
    Spacer(Modifier.height(8.dp))
    GoldButton("شراكة (فرق متساوية)", modifier = Modifier.fillMaxWidth()) { vm.teamMode = true; if (vm.playerCount % 2 != 0) vm.playerCount = 6; vm.go(Screen.COUNT) }
}

@Composable fun Count(vm: GameViewModel) = Page {
    Title("عدد اللاعبين")
    val opts = if (vm.teamMode) listOf(4, 6, 8) else (4..8).toList()
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(opts) { n ->
        Button({ vm.playerCount = n }, colors = ButtonDefaults.buttonColors(containerColor = if (vm.playerCount == n) Gold else Color(0xFF14573A), contentColor = if (vm.playerCount == n) Color.Black else Color.White)) { Text("$n") }
    } }
    Spacer(Modifier.height(16.dp))
    GoldButton("التالي") { vm.go(if (vm.teamMode) Screen.TEAMS else Screen.AVATAR) }
}

@Composable fun Teams(vm: GameViewModel) = Page {
    Title("الفِرق")
    val a = vm.ids.filter { it % 2 == 0 }; val b = vm.ids.filter { it % 2 == 1 }
    Text("الفريق الأول: " + a.joinToString("، ") { vm.name(it) }, color = Color.White)
    Text("الفريق الثاني: " + b.joinToString("، ") { vm.name(it) }, color = Color.White)
    Text("اللاعبون يجلسون بالتناوب؛ الفرق متساوية.", color = Gold)
    Spacer(Modifier.height(12.dp)); GoldButton("التالي") { vm.go(Screen.AVATAR) }
}

@Composable fun Avatars(vm: GameViewModel) = Page {
    Title("اختر شخصيتك")
    Text("لكل لاعب شخصية واحدة، ولا يجوز تكرارها", color = Color.White)
    var v by remember { mutableStateOf(vm.avatarVersion) }; v.let { }
    var err by remember { mutableStateOf("") }
    val mine = vm.picker.avatarOf(0)
    LazyVerticalGrid(GridCells.Fixed(4), Modifier.height(260.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items((1..8).toList()) { a ->
            val takenByOther = vm.picker.isTaken(a) && mine != a
            Image(painterResource(avatarRes(a)), null, Modifier.size(72.dp).clip(CircleShape).border(if (mine == a) 4.dp else 1.dp, if (mine == a) Gold else Color.Gray, CircleShape)
                .clickable { if (!vm.pickAvatar(a)) err = "هذه الشخصية مختارة" else { err = ""; v = vm.avatarVersion } }, alpha = if (takenByOther) 0.3f else 1f)
        }
    }
    if (err.isNotEmpty()) Text(err, color = Color(0xFFFF8A80))
    Spacer(Modifier.height(12.dp))
    GoldButton("ابدأ", enabled = mine != null) { vm.startMatch() }
}

@Composable fun Deal(vm: GameViewModel) = Page {
    Title("توزيع الأوراق")
    Text("تم توزيع ${vm.settings.cardsPerPlayer} أوراق لكل لاعب", color = Color.White)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { vm.hands[0].orEmpty().forEach { CardView(it) } }
    Spacer(Modifier.height(16.dp)); GoldButton("إلى الطاولة") { vm.beginTable() }
}

@Composable fun Table(vm: GameViewModel) = Box(Modifier.fillMaxSize().background(Green)) {
    Image(painterResource(R.drawable.table_bg), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = 0.55f)
    val r = vm.round ?: return@Box
    vm.tick.let { }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(8.dp)) {
        Text("الكيس: ${r.bag}   الطلب الحالي: ${if (r.currentBid == 0) "—" else r.currentBid}", color = Gold, fontSize = 18.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 6.dp)) {
            items(vm.ids.drop(1)) { id ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painterResource(avatarRes(vm.picker.avatarOf(id) ?: 8)), null, Modifier.size(52.dp).clip(CircleShape).border(if (r.currentPlayer == id) 3.dp else 1.dp, Gold, CircleShape), alpha = if (id in r.folded) 0.3f else 1f)
                    Text(vm.name(id) + if (r.topBidder == id) " ★" else "", color = Color.White, fontSize = 12.sp)
                    Text("${vm.ledger?.balances?.get(id) ?: 0}", color = Gold, fontSize = 12.sp)
                }
            }
        }
        Column(Modifier.weight(1f).fillMaxWidth()) { vm.log.takeLast(5).forEach { Text(it, color = Color.White, fontSize = 14.sp) } }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { vm.hands[0].orEmpty().forEach { CardView(it, Modifier.padding(2.dp)) } }
        Text(if (r.currentPlayer == 0 && !r.isFinished) "دورك" else "انتظر...", color = Gold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        val myTurn = r.currentPlayer == 0 && !r.isFinished
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (myTurn) items(r.legalBids().take(12)) { b ->
                Button({ vm.humanBid(b) }, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black)) { Text(if (b == vm.settings.sinko) "سينكو $b" else if (r.isAllIn(b)) "أول أن $b" else "$b") }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GoldButton("انسحاب", myTurn, Modifier.weight(1f)) { vm.humanFold() }
            GoldButton("مداقشة", myTurn && r.topBidder != null && r.topBidder != 0, Modifier.weight(1f)) { vm.humanChallenge() }
        }
        BannerSlot()
    }
}

/** بانر Start.io في شريط سفلي منفصل لا يغطي الأوراق أو الأزرار. */
@Composable fun BannerSlot() {
    if (BuildConfig.STARTIO_APP_ID.isBlank()) return
    Box(Modifier.fillMaxWidth().height(56.dp)) {
        AndroidView(factory = { ctx -> try { com.startapp.sdk.ads.banner.Banner(ctx) } catch (_: Throwable) { android.view.View(ctx) } }, modifier = Modifier.fillMaxSize())
    }
}

@Composable fun Result(vm: GameViewModel) = Page {
    val o = vm.outcome; val l = vm.ledger
    Title("نتيجة الجولة")
    if (o != null) {
        Text(when (o) {
            is RoundOutcome.BidWon -> "${vm.name(o.winnerId)} فاز بالطلب ${o.amount}"
            is RoundOutcome.ChallengeWon -> "نجحت المداقشة! ${vm.name(o.winnerId)} يفوز على ${vm.name(o.loserId)} بـ ${o.amount}"
            is RoundOutcome.ChallengeLost -> "فشلت المداقشة. ${vm.name(o.winnerId)} يفوز بـ ${o.amount}"
        }, color = Color.White, textAlign = TextAlign.Center)
    }
    vm.ids.forEach { id -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
        Text("${vm.name(id)}: ", color = Color.White, modifier = Modifier.width(80.dp))
        vm.hands[id].orEmpty().forEach { Text(cardLabel(it) + " ", color = Gold) }
        Spacer(Modifier.weight(1f)); Text("${l?.balances?.get(id) ?: 0}", color = Gold)
    } }
    Spacer(Modifier.height(12.dp)); GoldButton("جولة جديدة") { vm.newRound() }
}

@Composable fun Prize(vm: GameViewModel) = Page {
    val o = vm.outcome ?: return@Page
    Title("توزيع الجائزة")
    Text("جائزتك ${o.amount}. يمكنك إعطاء أي مبلغ للاعبين تختارهم (المجموع ≤ ${o.amount}).", color = Color.White, textAlign = TextAlign.Center)
    val shares = remember { mutableStateMapOf<Int, String>() }
    val l = vm.ledger
    vm.ids.drop(1).forEach { id ->
        val allowed = l == null || l.validateDistribution(0, o.amount, mapOf(id to 0)) == null
        if (allowed) OutlinedTextField(shares[id] ?: "", { shares[id] = it.filter(Char::isDigit) }, label = { Text(vm.name(id)) }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(2.dp))
    }
    var err by remember { mutableStateOf("") }
    if (err.isNotEmpty()) Text(err, color = Color(0xFFFF8A80))
    Spacer(Modifier.height(8.dp))
    GoldButton("تأكيد") {
        val map = shares.mapNotNull { (k, v) -> v.toIntOrNull()?.takeIf { it > 0 }?.let { k to it } }.toMap()
        vm.applyPrize(map)?.let { err = it }
    }
}

@Composable fun Final(vm: GameViewModel) = Page {
    Title("انتهت المباراة")
    val w = vm.ledger?.winners().orEmpty()
    Text("الفائز: " + w.joinToString("، ") { vm.name(it) } + " — وصل إلى ${vm.settings.targetScore} بالضبط", color = Gold, textAlign = TextAlign.Center)
    Spacer(Modifier.height(12.dp)); GoldButton("الرئيسية") { vm.go(Screen.HOME) }
}

@Composable fun History(vm: GameViewModel) = Page {
    Title("سجل الجولات")
    val h = vm.ledger?.history ?: vm.repo.load()?.history.orEmpty()
    if (h.isEmpty()) Text("لا توجد جولات بعد", color = Color.White)
    h.asReversed().forEach { Text("جولة ${it.number}: ${vm.name(it.winnerId)} ← ${it.amount}", color = Color.White, modifier = Modifier.padding(2.dp)) }
    Spacer(Modifier.height(12.dp)); GoldButton("رجوع") { vm.go(Screen.HOME) }
}

@Composable fun Settings(vm: GameViewModel) = Page {
    Title("الإعدادات (قواعد قابلة للتعديل)")
    val s = vm.settings
    @Composable fun row(t: String, v: String, onClick: () -> Unit) = Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(10.dp)) { Text(t, color = Color.White, modifier = Modifier.weight(1f)); Text(v, color = Gold) }
    row("طريقة المقارنة", if (s.comparisonMode == ComparisonMode.GROUP_LEXICOGRAPHIC) "حسب المجموعات" else "نوع التركيبة أولًا") {
        vm.settings = s.copy(comparisonMode = if (s.comparisonMode == ComparisonMode.GROUP_LEXICOGRAPHIC) ComparisonMode.TYPE_FIRST else ComparisonMode.GROUP_LEXICOGRAPHIC) }
    row("عند التعادل", if (s.tieBreak == TieBreak.BIDDER_WINS) "يفوز صاحب الطلب" else "يفوز المتحدي") {
        vm.settings = s.copy(tieBreak = if (s.tieBreak == TieBreak.BIDDER_WINS) TieBreak.CHALLENGER_WINS else TieBreak.BIDDER_WINS) }
    row("غرامة المداقشة الفاشلة", if (s.failedChallengePenalty) "نعم" else "لا") { vm.settings = s.copy(failedChallengePenalty = !s.failedChallengePenalty) }
    row("تجاوز الهدف", if (s.overshootPolicy == OvershootPolicy.IGNORE_GAIN) "يُتجاهل الربح" else "يرتد للخلف") {
        vm.settings = s.copy(overshootPolicy = if (s.overshootPolicy == OvershootPolicy.IGNORE_GAIN) OvershootPolicy.BOUNCE_BACK else OvershootPolicy.IGNORE_GAIN) }
    row("الأرصدة السالبة", if (s.allowNegativeBalance) "مسموحة" else "ممنوعة") { vm.settings = s.copy(allowNegativeBalance = !s.allowNegativeBalance) }
    row("الكيس", "${s.bagStart}") { vm.settings = s.copy(bagStart = if (s.bagStart >= 500) 100 else s.bagStart + 100) }
    Spacer(Modifier.height(12.dp)); GoldButton("رجوع") { vm.go(Screen.HOME) }
}

@Composable fun Help(vm: GameViewModel) = Page {
    Title("المساعدة")
    Text("• من 4 إلى 8 لاعبين، 32 ورقة، 4 أوراق لكل لاعب.\n• الرتب: قص > شايب > بنت > ولد > 10 > 9 > 8 > 7.\n• الكيس يبدأ من 100. سينكو = 5 وحدات (أقل طلب). أول أن = الكيس كاملًا.\n• الطلب تصاعدي فقط، ويمكنك الانسحاب.\n• المداقشة: إن كانت أوراقك أقوى من صاحب الطلب الأعلى تفوز عليه.\n• الهدف: الوصول إلى 5000 بالضبط.\n• الفائز يوزع جائزته على من يختار.", color = Color.White)
    Spacer(Modifier.height(12.dp)); GoldButton("رجوع") { vm.go(Screen.HOME) }
}

@Composable fun Privacy(vm: GameViewModel) = Page {
    Title("الخصوصية والإعلانات")
    Text("تعمل اللعبة دون إنترنت وتحفظ بياناتك على جهازك. عند تفعيل Firebase تُزامَن بيانات المباراة مع حسابك فقط. تعرض اللعبة إعلانات من Start.io عند تفعيلها، وفشل الإعلان لا يؤثر على اللعب. أضف رابط سياسة الخصوصية الكاملة قبل النشر.", color = Color.White)
    Spacer(Modifier.height(12.dp)); GoldButton("رجوع") { vm.go(Screen.HOME) }
}
