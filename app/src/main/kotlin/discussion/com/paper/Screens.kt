package discussion.com.paper

import androidx.compose.animation.core.animateFloat
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
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

/** ورقة برسم احترافي من الأصول. glow = توهج ذهبي، faceUp=false يعرض الظهر. */
@Composable fun CardView(c: Card?, modifier: Modifier = Modifier, width: androidx.compose.ui.unit.Dp = 64.dp, glow: Boolean = false, faceUp: Boolean = true) {
    val shape = RoundedCornerShape(width * 0.08f)
    val res = if (c == null || !faceUp) CardAssets.BACK else CardAssets.face(c)
    val glowAnim by androidx.compose.animation.core.animateFloatAsState(if (glow) 1f else 0f, androidx.compose.animation.core.tween(220), label = "glow")
    Image(painterResource(res), null,
        modifier.width(width).aspectRatio(420f / 607f)
            .shadow((4 + 14 * glowAnim).dp, shape, ambientColor = Gold, spotColor = if (glow) Gold else Color.Black)
            .then(if (glow) Modifier.border(2.dp, Gold, shape) else Modifier),
        contentScale = ContentScale.Fit)
}

/** توزيع متحرك: تنطلق الورقة من منتصف الطاولة وتنقلب لتظهر. delayMs لتتابع التوزيع. */
@Composable fun DealtCard(c: Card, index: Int, total: Int, width: androidx.compose.ui.unit.Dp = 72.dp) {
    val t = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 140L)
        t.animateTo(1f, androidx.compose.animation.core.tween(520, easing = androidx.compose.animation.core.FastOutSlowInEasing))
    }
    val fan = (index - (total - 1) / 2f)
    Box(Modifier.graphicsLayer {
        val p = t.value
        translationY = (1f - p) * -520f
        rotationZ = (1f - p) * -30f + p * fan * 4f
        rotationY = if (p < 0.5f) p * 360f else (1f - p) * 360f
        cameraDistance = 14f * density
        alpha = (p * 4f).coerceIn(0f, 1f)
    }) { CardView(c, width = width, faceUp = t.value >= 0.5f) }
}

/** الأوراق الداعمة للتركيبة الرابحة (المجموعة الأولى). */
fun winningCards(hand: List<Card>): Set<Card> {
    val top = HandEvaluator.groups(hand).first()
    return if (top.first >= 2) hand.filter { it.rank.power == top.second }.toSet()
    else hand.filter { it.rank.power == top.second }.take(1).toSet()
}

/** صورة لاعب: بورتريه دائري بإطار ذهبي، توهج نابض عند دوره، وشارة اسم/رصيد مقروءة. */
@Composable fun PlayerAvatar(name: String, balance: Int, avatar: Int, size: Dp, active: Boolean, folded: Boolean, isTop: Boolean, modifier: Modifier = Modifier) {
    val pulse by androidx.compose.animation.core.rememberInfiniteTransition(label = "pulse").animateFloat(0.45f, 1f,
        androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(700), androidx.compose.animation.core.RepeatMode.Reverse), label = "a")
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.TopEnd) {
            Image(painterResource(avatarRes(avatar)), null,
                Modifier.size(size).shadow(if (active) 14.dp else 6.dp, CircleShape, ambientColor = Gold, spotColor = if (active) Gold else Color.Black)
                    .clip(CircleShape).border(if (active) 4.dp else 2.5.dp, if (active) Gold.copy(alpha = pulse) else Gold.copy(alpha = 0.85f), CircleShape)
                    .graphicsLayer { alpha = if (folded) 0.35f else 1f })
            if (isTop) Box(Modifier.size(size * 0.3f).clip(CircleShape).background(Gold), contentAlignment = Alignment.Center) { Text("★", color = Color.Black, fontSize = (size.value * 0.18f).sp) }
        }
        Column(Modifier.offset(y = (-10).dp).clip(RoundedCornerShape(12.dp)).background(Color(0xDD06261A)).border(1.dp, Gold.copy(alpha = 0.6f), RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(name, color = Color.White, fontSize = (size.value * 0.19f).coerceIn(11f, 15f).sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text("$balance", color = Gold, fontSize = (size.value * 0.19f).coerceIn(11f, 15f).sp, fontWeight = FontWeight.Bold)
        }
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
    LazyVerticalGrid(GridCells.Fixed(4), Modifier.height(320.dp).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items((1..8).toList()) { a ->
            val takenByOther = vm.picker.isTaken(a) && mine != a
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(painterResource(avatarRes(a)), null, Modifier.fillMaxWidth().aspectRatio(1f).padding(2.dp).shadow(if (mine == a) 12.dp else 4.dp, CircleShape, ambientColor = Gold, spotColor = Gold).clip(CircleShape)
                    .border(if (mine == a) 4.dp else 2.dp, if (mine == a) Gold else Gold.copy(alpha = 0.5f), CircleShape)
                    .clickable { if (!vm.pickAvatar(a)) err = "هذه الشخصية مختارة" else { err = ""; v = vm.avatarVersion } }
                    .graphicsLayer { alpha = if (takenByOther) 0.3f else 1f })
                Text(vm.avatarNames[a - 1], color = if (mine == a) Gold else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
    if (err.isNotEmpty()) Text(err, color = Color(0xFFFF8A80))
    Spacer(Modifier.height(12.dp))
    GoldButton("ابدأ", enabled = mine != null) { vm.startMatch() }
}

@Composable fun Deal(vm: GameViewModel) = Page {
    Title("توزيع الأوراق")
    Text("تم توزيع ${vm.settings.cardsPerPlayer} أوراق لكل لاعب", color = Color.White)
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) { vm.hands[0].orEmpty().let { h -> h.forEachIndexed { i, c -> DealtCard(c, i, h.size, 78.dp) } } }
    Spacer(Modifier.height(16.dp)); GoldButton("إلى الطاولة") { vm.beginTable() }
}

@Composable fun Table(vm: GameViewModel) = Box(Modifier.fillMaxSize().background(Green)) {
    Image(painterResource(R.drawable.table_bg), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = 0.55f)
    val r = vm.round ?: return@Box
    vm.tick.let { }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(8.dp)) {
        Text("الكيس: ${r.bag}   الطلب الحالي: ${if (r.currentBid == 0) "—" else r.currentBid}", color = Gold, fontSize = 18.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val opp = vm.ids.drop(1); val n = opp.size
            val sz = (maxWidth / 5.2f).coerceIn(48.dp, 78.dp)
            val itemW = sz + 18.dp; val itemH = sz * 1.45f
            val humanSz = sz * 1.4f
            val rx = ((maxWidth - itemW) / 2f).value
            val cyArc = (maxHeight.value * 0.58f)
            val ry = (cyArc - itemH.value / 2f - 4f).coerceAtLeast(40f)
            opp.forEachIndexed { k, id ->
                val th = if (n == 1) PI / 2 else PI * (0.07 + 0.86 * k / (n - 1))
                val a = PI - th
                val x = maxWidth.value / 2f + rx * cos(a).toFloat() - itemW.value / 2f
                val y = cyArc - ry * sin(a).toFloat() - itemH.value / 2f
                PlayerAvatar(vm.name(id), vm.ledger?.balances?.get(id) ?: 0, vm.picker.avatarOf(id) ?: 8, sz, r.currentPlayer == id && !r.isFinished, id in r.folded, r.topBidder == id,
                    Modifier.offset(x.dp, y.dp).width(itemW))
            }
            Column(Modifier.align(Alignment.Center).fillMaxWidth(0.55f).offset(y = (-6).dp), horizontalAlignment = Alignment.CenterHorizontally) {
                vm.log.takeLast(4).forEach { Text(it, color = Color.White, fontSize = 13.sp, textAlign = TextAlign.Center) }
            }
            PlayerAvatar(vm.name(0), vm.ledger?.balances?.get(0) ?: 0, vm.picker.avatarOf(0) ?: 1, humanSz, r.currentPlayer == 0 && !r.isFinished, 0 in r.folded, r.topBidder == 0,
                Modifier.align(Alignment.BottomCenter))
        }
        var selected by remember(vm.round) { mutableStateOf<Card?>(null) }
        Row(Modifier.fillMaxWidth().height(130.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
            val h = vm.hands[0].orEmpty()
            h.forEachIndexed { i, c ->
                val mid = (h.size - 1) / 2f
                val up by androidx.compose.animation.core.animateDpAsState(if (selected == c) (-14).dp else 0.dp, label = "up")
                CardView(c, Modifier.padding(horizontal = 1.dp).offset(y = up + (kotlin.math.abs(i - mid) * 3).dp)
                    .graphicsLayer { rotationZ = (i - mid) * 4f }.clickable { selected = if (selected == c) null else c },
                    width = 76.dp, glow = selected == c)
            }
        }
        Text(if (r.currentPlayer == 0 && !r.isFinished) "دورك — ${vm.secondsLeft} ث (وإلا سينكو تلقائي)" else "انتظر...", color = Gold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
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
            is RoundOutcome.ChallengeWon -> "نجحت المداقشة! ${vm.name(o.winnerId)} يربح ${o.amount} و${vm.name(o.loserId)} يخسر ${o.loserAmount}"
            is RoundOutcome.ChallengeLost -> "فشلت المداقشة. ${vm.name(o.winnerId)} يربح ${o.amount} و${vm.name(o.loserId)} يخسر ${o.loserAmount}"
        }, color = Color.White, textAlign = TextAlign.Center)
    }
    vm.ids.forEach { id -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
        Text("${vm.name(id)}: ", color = Color.White, modifier = Modifier.width(64.dp))
        val hh = vm.hands[id].orEmpty(); val win = if (id == o?.winnerId) winningCards(hh) else emptySet()
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) { hh.forEach { CardView(it, width = 40.dp, glow = it in win) } }
        Spacer(Modifier.weight(1f)); Text("${l?.balances?.get(id) ?: 0}", color = Gold)
    } }
    Spacer(Modifier.height(12.dp)); GoldButton("جولة جديدة") { vm.newRound() }
}

@Composable fun Prize(vm: GameViewModel) = Page {
    val o = vm.outcome ?: return@Page
    val l = vm.ledger
    Title("توزيع الجائزة")
    Text("جائزتك ${o.amount}. علّم ✓ على الشريك ثم اكتب المبلغ، أو لا تعطِ أحداً.", color = Color.White, textAlign = TextAlign.Center)
    val checked = remember { mutableStateMapOf<Int, Boolean>() }
    val amounts = remember { mutableStateMapOf<Int, String>() }
    val candidates = vm.ids.drop(1).filter { l == null || l.validateDistribution(0, o.amount, mapOf(it to 0)) == null }
    val given = candidates.filter { checked[it] == true }.sumOf { amounts[it]?.toIntOrNull() ?: 0 }
    val remaining = o.amount - given
    candidates.forEach { id ->
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked[id] == true, { checked[id] = it }, colors = CheckboxDefaults.colors(checkedColor = Gold, checkmarkColor = Color.Black, uncheckedColor = Color.White))
            Text(vm.name(id), color = Color.White, modifier = Modifier.width(72.dp))
            OutlinedTextField(amounts[id] ?: "", { amounts[id] = it.filter(Char::isDigit) }, enabled = checked[id] == true, singleLine = true,
                label = { Text("المبلغ") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f).padding(2.dp))
        }
    }
    Spacer(Modifier.height(8.dp))
    Text("المتبقي لك: $remaining", color = if (remaining < 0) Color(0xFFFF8A80) else Gold, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GoldButton("تأكيد التوزيع", remaining >= 0, Modifier.weight(1f)) {
            val map = candidates.filter { checked[it] == true }.mapNotNull { id -> amounts[id]?.toIntOrNull()?.takeIf { it > 0 }?.let { id to it } }.toMap()
            vm.applyPrize(map)
        }
        GoldButton("لا أعطي أحداً", true, Modifier.weight(1f)) { vm.applyPrize(emptyMap()) }
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
    row("الطلب مرة واحدة لكل لاعب", if (s.oneBidPerPlayer) "نعم" else "لا") { vm.settings = s.copy(oneBidPerPlayer = !s.oneBidPerPlayer) }
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
