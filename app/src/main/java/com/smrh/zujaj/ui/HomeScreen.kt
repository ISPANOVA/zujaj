package com.smrh.zujaj.ui

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FilterAltOff
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.StickyNote2
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.WatchLater
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smrh.zujaj.billing.Billing
import com.smrh.zujaj.catalog.Cat
import com.smrh.zujaj.catalog.Shape
import com.smrh.zujaj.catalog.WidgetStyle
import com.smrh.zujaj.data.Prefs
import com.smrh.zujaj.remote.Flags
import kotlinx.coroutines.delay
import java.time.LocalTime

// =============================================================================== shell
private data class Dest(val icon: ImageVector, val en: String, val ar: String)
private val dests = listOf(
    Dest(Icons.Rounded.Home, "Discover", "اكتشف"),
    Dest(Icons.Rounded.Favorite, "Saved", "المفضلة"),
    Dest(Icons.Rounded.Dashboard, "Mine", "ويدجتاتي"),
    Dest(Icons.Rounded.Settings, "Settings", "الإعدادات"),
)

/**
 * The app shell. Phones get a floating dock at the bottom; from 840dp (tablets, unfolded foldables,
 * landscape large phones) the destinations move to a side rail so the content gets the full height and
 * the grid gets the full remaining width. The selected tab is saved, so folding/unfolding keeps it.
 */
@Composable
fun HomeScreen(nav: Nav) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val pane = LocalPaneWidth.current
    val rail = WidthClass.of(pane) == WidthClass.EXPANDED
    val ver by Prefs.version.collectAsState()
    val density = androidx.compose.ui.platform.LocalDensity.current.density
    val content: @Composable () -> Unit = {
        androidx.compose.animation.AnimatedContent(
            targetState = tab,
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                // fade-through: old tab fades out fast, new one fades in with a 24dp drift
                (androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(210, 90)) +
                    androidx.compose.animation.slideInHorizontally(androidx.compose.animation.core.tween(280, 60, Motion.ease)) { (24 * density).toInt() * dir })
                    .togetherWith(androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(90)))
            },
            label = "tabs",
        ) { t ->
            when (t) {
                0 -> DiscoverTab(nav)
                1 -> FavoritesTab(nav) { tab = 0 }
                2 -> MyWidgetsTab(nav)
                else -> SettingsScreen(nav, asTab = true)
            }
        }
    }
    if (rail) {
        Row(Modifier.fillMaxSize()) {
            key(ver, Prefs.isArabicUi) { NavRail(tab) { tab = it } }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                // the content pane is narrower than the window by the rail: grids size themselves to this
                androidx.compose.runtime.CompositionLocalProvider(LocalPaneWidth provides (pane - 88), LocalDockSpace provides 24.dp) { content() }
            }
        }
    } else {
        Box(Modifier.fillMaxSize()) {
            content()
            // labels follow the app language even when it changes while this screen is open
            key(ver, Prefs.isArabicUi) { DockBar(tab, Modifier.align(Alignment.BottomCenter)) { tab = it } }
        }
    }
}

/** Bottom space a tab leaves free for the floating dock (none beside a rail). */
val LocalDockSpace = androidx.compose.runtime.compositionLocalOf { 120.dp }

@Composable
private fun NavRail(tab: Int, onTab: (Int) -> Unit) {
    val accent = LocalAccent.current
    val ctx = androidx.compose.ui.platform.LocalContext.current
    Column(
        Modifier.width(88.dp).fillMaxHeight().navigationBarsPadding().padding(vertical = Sp.l),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Sp.s),
    ) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(androidx.compose.ui.res.colorResource(com.smrh.zujaj.R.color.ic_bg))) {
            androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(com.smrh.zujaj.R.drawable.ic_fg), null, Modifier.fillMaxSize())
        }
        Spacer(Modifier.height(Sp.xl))
        dests.forEachIndexed { i, d ->
            val sel = i == tab
            val pill by androidx.compose.animation.animateColorAsState(if (sel) accent.copy(alpha = 0.18f) else Color.Transparent, Motion.color(), label = "rp")
            val fg by androidx.compose.animation.animateColorAsState(if (sel) accent else Z.sub, Motion.color(), label = "rf")
            Column(
                Modifier.width(72.dp).clip(RoundedCornerShape(Rad.m)).clickable { Haptics.tick(ctx); onTab(i) }.padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.width(56.dp).height(32.dp).clip(RoundedCornerShape(50)).background(pill), contentAlignment = Alignment.Center) {
                    Icon(d.icon, null, tint = fg, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.height(4.dp))
                Text(tr(d.en, d.ar), color = fg, fontSize = 12.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
            }
        }
    }
}

@Composable
private fun DockBar(tab: Int, modifier: Modifier, onTab: (Int) -> Unit) {
    val accent = LocalAccent.current
    val ctx = androidx.compose.ui.platform.LocalContext.current
    // floating capsule; the selected destination opens into a pill with its label
    Box(
        modifier.fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Z.bg.copy(alpha = 0.85f), Z.bg)))
            .navigationBarsPadding().padding(top = 22.dp, bottom = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier.shadow(if (Z.light) 10.dp else 18.dp, RoundedCornerShape(50), ambientColor = Color.Black, spotColor = Color.Black)
                .clip(RoundedCornerShape(50))
                .background(Z.surface.copy(alpha = 0.97f))
                .border(1.dp, Z.tint(0.09f), RoundedCornerShape(50))
                .padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            dests.forEachIndexed { i, d ->
                val sel = i == tab
                val bg by androidx.compose.animation.animateColorAsState(if (sel) accent else Color.Transparent, Motion.color(), label = "db")
                Row(
                    Modifier.animateContentSize(Motion.std(Motion.MED)).clip(RoundedCornerShape(50)).background(bg)
                        .clickable { Haptics.tick(ctx); onTab(i) }
                        .padding(horizontal = if (sel) 18.dp else 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(d.icon, tr(d.en, d.ar), tint = if (sel) Color(0xFF14110A) else Z.sub, modifier = Modifier.size(22.dp))
                    if (sel) {
                        Spacer(Modifier.width(8.dp))
                        Text(tr(d.en, d.ar), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF14110A), maxLines = 1)
                    }
                }
            }
        }
    }
}

// =============================================================================== helpers
fun catIcon(c: Cat): ImageVector = when (c) {
    Cat.CLOCK -> Icons.Rounded.WatchLater
    Cat.DIGITAL -> Icons.Rounded.Schedule
    Cat.DATE -> Icons.Rounded.CalendarMonth
    Cat.WEATHER -> Icons.Rounded.WbSunny
    Cat.PRAYER -> Icons.Rounded.Explore
    Cat.ISLAMIC -> Icons.Rounded.AutoAwesome
    Cat.MEDIA -> Icons.Rounded.MusicNote
    Cat.PHOTO -> Icons.Rounded.Photo
    Cat.COUNTERS -> Icons.Rounded.Timer
    Cat.APPS -> Icons.Rounded.Apps
    Cat.PEOPLE -> Icons.Rounded.People
    Cat.SEARCH -> Icons.Rounded.Search
    Cat.SYSTEM -> Icons.Rounded.BatteryChargingFull
    Cat.QUOTES -> Icons.Rounded.FormatQuote
    Cat.TOOLS -> Icons.Rounded.Tune
    Cat.CALLI -> Icons.Rounded.Brush
    Cat.HABITS -> Icons.Rounded.TaskAlt
    Cat.NOTES -> Icons.Rounded.StickyNote2
    Cat.FOCUS -> Icons.Rounded.HourglassBottom
}

private val catColors = listOf(
    Color(0xFF7C6CF2), Color(0xFF13B5A8), Color(0xFFE7B75F), Color(0xFFFF8A4C), Color(0xFF4DB5FF),
    Color(0xFFE8478B), Color(0xFF3DDC97), Color(0xFFB45CFF),
)

fun catColor(c: Cat): Color = catColors[c.ordinal % catColors.size]

/** Lists for the special collections. */
fun collection(key: String): List<WidgetStyle> {
    val all = Flags.visible()
    return when (key) {
        "popular" -> all.filter { it.popular }
        "new" -> all.filter { Flags.isNew(it) }
        "free" -> all.filter { !Flags.isPro(it) }
        "saved" -> all.filter { Prefs.favorites.contains(it.id) }
        "all" -> all
        else -> all.filter { it.cat.name == key }
    }
}

fun collectionTitle(key: String): String = when (key) {
    "popular" -> tr("Popular", "الأشهر")
    "new" -> tr("New", "الجديد")
    "free" -> tr("Free", "مجاني")
    "saved" -> tr("Saved", "المفضلة")
    "all" -> tr("All widgets", "كل الويدجت")
    else -> Cat.valueOf(key).let { if (Prefs.isArabicUi) it.ar else it.en }
}

/** Size groups in the order they are shown: small first, big after. */
fun shapeOrder(s: Shape) = when (s) { Shape.SMALL -> 0; Shape.WIDE -> 1; Shape.BANNER -> 2; Shape.TALL -> 3; Shape.LARGE -> 4; Shape.XL -> 5 }

fun shapeGroupTitle(s: Shape) = when (s) {
    Shape.LARGE -> tr("Large", "كبيرة")
    Shape.WIDE -> tr("Wide", "عريضة")
    Shape.BANNER -> tr("Banners", "شرائط")
    Shape.SMALL -> tr("Small", "صغيرة")
    Shape.TALL -> tr("Tall", "طويلة")
    Shape.XL -> tr("Boards", "لوحات كاملة")
}

/** Tidy order: by size (big → small), then free before Pro, then the catalogue order. */
fun tidy(list: List<WidgetStyle>): List<WidgetStyle> =
    list.withIndex().sortedWith(compareBy({ shapeOrder(it.value.shape) }, { if (Flags.isPro(it.value)) 1 else 0 }, { it.index })).map { it.value }

/** How many cards share a row: small widgets 2 (3–4 on big screens), wide ones 2 (3 on big screens). */
fun perRow(shape: Shape, widthDp: Int): Int = when (shape) {
    // window-size classes: compact < 600 · medium < 840 · expanded < 1200 · large
    Shape.SMALL, Shape.TALL -> when { widthDp >= 1200 -> 6; widthDp >= 840 -> 5; widthDp >= 600 -> 4; widthDp >= 480 -> 3; else -> 2 }
    else -> when { widthDp >= 1200 -> 4; widthDp >= 840 -> 3; widthDp >= 600 -> 2; else -> 2 }
}

/** A widget on a soft stage that looks like a bit of a home screen; badge in the corner, name under it. */
@Composable
fun PreviewTile(st: WidgetStyle, modifier: Modifier = Modifier, showName: Boolean = true, stage: androidx.compose.ui.unit.Dp? = null, compact: Boolean = false, onClick: () -> Unit) {
    val small = st.shape == Shape.SMALL
    val src = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(if (pressed) 0.96f else 1f, androidx.compose.animation.core.spring(dampingRatio = 0.55f, stiffness = 500f), label = "press")
    val tint = catColor(st.cat)
    Column(modifier.graphicsLayer { scaleX = scale; scaleY = scale }.clip(RoundedCornerShape(24.dp))
        .clickable(interactionSource = src, indication = androidx.compose.material3.ripple(), onClick = onClick)) {
        Box(
            Modifier.fillMaxWidth().then(if (stage != null) Modifier.height(stage) else Modifier).clip(RoundedCornerShape(24.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF17171D), Color(0xFF101015))))
                .border(1.dp, Z.tint(0.07f), RoundedCornerShape(24.dp))
                .padding(if (compact) 8.dp else if (small) 16.dp else 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            WidgetPreview(st.defaultConfig(), if (stage != null) Modifier.fillMaxSize() else Modifier.fillMaxWidth(), density = 2f)
        }
        if (showName) {
            Row(Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, top = 9.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(st.name(Prefs.isArabicUi), color = Z.text.copy(alpha = 0.9f), fontSize = if (compact) 12.sp else 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                Spacer(Modifier.width(6.dp))
                if (Flags.isPro(st)) ProBadge() else if (Flags.isNew(st)) NewBadge()
            }
        }
    }
}

fun sizeLabel(s: Shape) = when (s) {
    Shape.SMALL -> tr("Small · 2×2", "صغير · ٢×٢")
    Shape.WIDE -> tr("Wide · 4×2", "عريض · ٤×٢")
    Shape.BANNER -> tr("Banner · 4×1", "شريط · ٤×١")
    Shape.LARGE -> tr("Large · 4×3", "كبير · ٤×٣")
    Shape.TALL -> tr("Tall · 2×4", "طويل · ٢×٤")
    Shape.XL -> tr("Board · 4×5", "لوحة · ٤×٥")
}

@Composable
private fun SectionHeader(title: String, count: Int?, onAll: (() -> Unit)?, icon: ImageVector? = null, color: Color = LocalAccent.current) {
    Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 10.dp, top = 22.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Box(Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(color.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 19.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            if (count != null) Text(tr("$count widgets", "$count ويدجت"), color = Z.sub, fontSize = 12.sp)
        }
        if (onAll != null) {
            Row(Modifier.clip(RoundedCornerShape(50)).background(Z.tint(0.06f)).clickable(onClick = onAll).padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(tr("See all", "الكل"), color = Z.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Z.sub, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** Small subheading inside a list, e.g. "Wide · 12". */
@Composable
private fun GroupLabel(text: String) {
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, color = Z.sub, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f).height(1.dp).background(Z.line))
    }
}

// =============================================================================== discover
/** The app's own "Editor's choice" when the control panel hasn't set one. */
val DEFAULT_PICKS = listOf("prayer_hub", "calli_lawn", "photo_verse", "event_card", "prayer_journey", "clock_neon", "w_today_tomorrow", "event_ring")

/**
 * Home: a living aurora header, an auto-sliding showcase, an animated Wallpapers card, collection
 * tabs that stay on top, and the chosen collection below — big widgets first, small ones in pairs,
 * free before Pro. Everything moves softly: tabs, cards and rows animate in.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DiscoverTab(nav: Nav) {
    val pro by Billing.pro.collectAsState()
    val flags by Flags.flags.collectAsState()
    val ver by Prefs.version.collectAsState()
    touch(ver)
    val accent = LocalAccent.current
    val all = Flags.visible()
    val cats = Cat.values().filter { c -> all.any { it.cat == c } }
    var chip by rememberSaveable { mutableStateOf("popular") }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    // rows animate in once per visit to Discover; returning from a widget shows them in place at once
    var entered by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(900); entered = true }
    val featured = remember(flags) {
        flags.featured.ifEmpty { DEFAULT_PICKS }.mapNotNull { id -> all.firstOrNull { it.id == id } }
    }
    val hour = LocalTime.now().hour
    val greet = when (hour) { in 5..11 -> tr("Good morning", "صباح الخير"); in 12..16 -> tr("Good afternoon", "طاب يومك"); else -> tr("Good evening", "مساء الخير") }
    // Wallpapers intro: right after the battery guide is done (or straight away if it isn't needed)
    var showNew by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (Prefs.getString("wall_intro") == "1") return@LaunchedEffect
        delay(900)
        while (Prefs.getString("bat_intro") != "1") delay(400)
        delay(500)
        showNew = Prefs.getString("wall_intro") != "1"
    }
    // one-time: "watch ads to open a Pro widget" — after the wallpapers intro
    var showUnlock by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (Prefs.getString("unlock_intro") == "1") return@LaunchedEffect
        delay(1500)
        while (Prefs.getString("wall_intro") != "1" || Prefs.getString("bat_intro") != "1") delay(500)
        delay(700)
        if (com.smrh.zujaj.ads.AdUnlock.enabled() && !Prefs.isPro) showUnlock = true
    }
    if (showUnlock) AdUnlockIntro { Prefs.setString("unlock_intro", "1"); showUnlock = false }
    if (showNew) WallIntroDialog(onTry = { Prefs.setString("wall_intro", "1"); showNew = false; nav.go(Screen.Wallpapers) }) { Prefs.setString("wall_intro", "1"); showNew = false }

    val ctxHome = androidx.compose.ui.platform.LocalContext.current
    val list = remember(chip, flags, ver) { tidy(collection(chip)) }
    val screenDp = LocalPaneWidth.current
    val still = remember { Z.reducedMotion(ctxHome) }
    val wide = WidthClass.of(screenDp) != WidthClass.COMPACT
    val gut = Sp.gutter(screenDp)

    LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = LocalDockSpace.current + 10.dp)) {
        // ---- header: greeting + wordmark on a still light wash (no looping motion on the home page —
        // it cost battery and pulled the eye away from the widgets), then a real search entry
        item(key = "head") {
            Column(Modifier.fillMaxWidth().drawBehind {
                val r = minOf(size.width, 1100f) * 0.75f
                drawCircle(Brush.radialGradient(listOf(Color(0xFF7C6CF2).copy(alpha = if (Z.light) 0.10f else 0.20f), Color.Transparent), center = androidx.compose.ui.geometry.Offset(r * 0.25f, 0f), radius = r), radius = r, center = androidx.compose.ui.geometry.Offset(r * 0.25f, 0f))
            }.padding(start = gut, end = gut, top = Sp.l, bottom = Sp.s)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(greet, color = Z.sub, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text(tr("Zujaj", "زجاج"), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, color = Z.text)
                    }
                    if (pro) RoundAction(Icons.Rounded.WorkspacePremium, Z.tint(0.06f), Z.pro, tr("Zujaj Pro", "زجاج برو")) { nav.go(Screen.Paywall) }
                    else Row(Modifier.heightIn(min = 40.dp).clip(RoundedCornerShape(50)).background(Z.pro.copy(alpha = 0.14f)).border(1.dp, Z.pro.copy(alpha = 0.45f), RoundedCornerShape(50))
                        .clickable { nav.go(Screen.Paywall) }.padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.WorkspacePremium, null, tint = Z.pro, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Pro", color = Z.pro, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
                Spacer(Modifier.height(Sp.l))
                // search as a field, not a lone icon: the first thing most people want is "find me a prayer clock"
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(Rad.m)).background(Z.surface).border(1.dp, Z.line, RoundedCornerShape(Rad.m))
                    .clickable { nav.go(Screen.Search) }.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Search, null, tint = Z.sub, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(tr("Search widgets — clock, prayer, calendar…", "ابحث — ساعة، صلاة، تقويم…"), color = Z.sub, fontSize = 14.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
            }
        }
        if (flags.notice.isNotBlank() && !flags.noticeOnce) item(key = "notice") {
            Text(flags.notice, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth().padding(horizontal = gut, vertical = 6.dp)
                .clip(RoundedCornerShape(16.dp)).background(Z.tint(0.06f)).padding(14.dp))
        }
        // ---- showcase and the Wallpapers entry. Phones stack them; wide windows put them side by side so the
        // first screen shows both without a tall empty band.
        item(key = "show") {
            val showcase: @Composable (Modifier) -> Unit = { m ->
                if (featured.isNotEmpty()) {
                    val pager = rememberPagerState { featured.size }
                    if (!still) LaunchedEffect(pager) { while (true) { delay(5000); pager.animateScrollToPage((pager.currentPage + 1) % featured.size, animationSpec = Motion.std(Motion.SLOW * 2)) } }
                    Column(m) {
                        HorizontalPager(pager, contentPadding = PaddingValues(horizontal = if (wide) 0.dp else gut), pageSpacing = 12.dp) { page ->
                            val st = featured[page]
                            val off = ((pager.currentPage - page) + pager.currentPageOffsetFraction).coerceIn(-1f, 1f)
                            val cardH = if (wide) 260.dp else 232.dp
                            val capH = 58.dp
                            Column(Modifier.fillMaxWidth().height(cardH)
                                .graphicsLayer { alpha = 1f - 0.35f * kotlin.math.abs(off) }
                                .clip(RoundedCornerShape(Rad.xl)).background(Z.surface)
                                .border(1.dp, Z.tint(0.08f), RoundedCornerShape(Rad.xl))
                                .clickable { nav.go(Screen.Editor(st.id)) }) {
                                // the widget sits on its own stage and is sized to fit it — never cut by the caption
                                androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).padding(6.dp).clip(RoundedCornerShape(Rad.l)).background(Backdrops.brush(page + 1)),
                                    contentAlignment = Alignment.Center) {
                                    val fitW = minOf(maxWidth - 32.dp, (maxHeight - 28.dp) * (st.shape.dw.toFloat() / st.shape.dh))
                                    Box(Modifier.width(fitW).graphicsLayer { translationX = off * 40.dp.toPx() }) {
                                        WidgetPreview(st.defaultConfig(), Modifier.fillMaxWidth(), density = 2.3f)
                                    }
                                }
                                Row(Modifier.fillMaxWidth().height(capH).padding(start = 18.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(tr("Editor's pick", "اختيارنا"), color = accent, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                        Text(st.name(Prefs.isArabicUi), color = Z.text, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    }
                                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = Z.sub, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.Center) {
                            repeat(featured.size) { i ->
                                val sel = i == pager.currentPage
                                val w by androidx.compose.animation.core.animateDpAsState(if (sel) 18.dp else 6.dp, Motion.std(), label = "dot")
                                Box(Modifier.padding(horizontal = 3.dp).height(6.dp).width(w).clip(RoundedCornerShape(50)).background(if (sel) accent else Z.tint(0.18f)))
                            }
                        }
                    }
                }
            }
            if (wide) Row(Modifier.fillMaxWidth().padding(horizontal = gut, vertical = Sp.s), horizontalArrangement = Arrangement.spacedBy(Sp.l)) {
                showcase(Modifier.weight(1.6f))
                WallpapersEntry(Modifier.weight(1f).height(260.dp), tall = true) { nav.go(Screen.Wallpapers) }
            } else {
                Column {
                    showcase(Modifier.padding(top = Sp.s))
                    WallpapersEntry(Modifier.fillMaxWidth().padding(horizontal = gut, vertical = Sp.l), tall = false) { nav.go(Screen.Wallpapers) }
                }
            }
        }
        // ---- collection tabs (stay on top while scrolling)
        stickyHeader(key = "tabs") {
            LazyRow(Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = gut, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val keys = listOf("popular", "new", "free") + cats.map { it.name }
                items(keys, key = { it }) { k -> TabPill(collectionTitle(k), chip == k, accent) { chip = k } }
            }
        }
        if (list.isEmpty()) item(key = "empty_$chip") { EmptyNote() }
        // ---- the chosen collection, grouped by size; rows slide in one after another
        var rowsSinceAd = 0
        var slot = 0
        var idx = 0
        listOf(Shape.SMALL, Shape.WIDE, Shape.BANNER, Shape.TALL, Shape.LARGE, Shape.XL).forEach { shape ->
            val group = list.filter { it.shape == shape }
            if (group.isEmpty()) return@forEach
            item(key = "g_${chip}_$shape") { GroupLabel("${shapeGroupTitle(shape)} · ${group.size}") }
            val per = perRow(shape, screenDp)
            val rows = group.chunked(per)
            rows.forEach { row ->
                val order = idx++
                item(key = "r_${chip}_${row.joinToString { it.id }}") {
                    // rows rise in once, lightly staggered; scrolling back up doesn't replay it
                    val shown = remember { androidx.compose.animation.core.Animatable(if (still || entered) 1f else 0f) }
                    LaunchedEffect(Unit) { if (shown.value < 1f) { delay((order.coerceAtMost(5) * 40L)); shown.animateTo(1f, Motion.std(Motion.SLOW)) } }
                    Row(Modifier.fillMaxWidth().padding(horizontal = gut, vertical = 7.dp)
                        .graphicsLayer { alpha = shown.value; translationY = (1f - shown.value) * 18.dp.toPx() },
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { st -> PreviewTile(st, Modifier.weight(1f), compact = shape != Shape.SMALL) { nav.go(Screen.Editor(st.id)) } }
                        repeat(per - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                rowsSinceAd++
                val homeEvery = com.smrh.zujaj.ads.AdPolicy.every(com.smrh.zujaj.ads.AdPolicy.Place.HOME)
                if (homeEvery > 0 && rowsSinceAd >= homeEvery) { rowsSinceAd = 0; val sl = slot++; item(key = "ad_${chip}_${row.first().id}") { com.smrh.zujaj.ads.AdBanner(sl) } }
            }
        }
    }
    // a NEW collection starts from its top (tabs stay pinned). Only on an actual change of collection —
    // coming back from a widget re-runs this screen and must leave the scroll exactly where it was.
    var shownChip by rememberSaveable { mutableStateOf(chip) }
    LaunchedEffect(chip) {
        if (chip == shownChip) return@LaunchedEffect
        shownChip = chip
        val tabs = 2 + (if (flags.notice.isNotBlank() && !flags.noticeOnce) 1 else 0)
        if (listState.firstVisibleItemIndex > tabs) listState.scrollToItem(tabs)
    }
}

/** Collection tab: the selected one fills with colour and grows a little (springy). */
@Composable
private fun TabPill(text: String, sel: Boolean, accent: Color, onClick: () -> Unit) {
    // tabs float over the page (no bar behind them), so each pill carries its own frosted fill
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val bg by androidx.compose.animation.animateColorAsState(if (sel) accent else Z.surface2.copy(alpha = 0.96f), Motion.color(), label = "tbg")
    val fg by androidx.compose.animation.animateColorAsState(if (sel) Color(0xFF14110A) else Z.text.copy(alpha = 0.88f), Motion.color(), label = "tfg")
    Box(Modifier.heightIn(min = 36.dp).clip(RoundedCornerShape(50)).background(bg)
        .border(1.dp, if (sel) Color.Transparent else Z.tint(0.09f), RoundedCornerShape(50))
        .clickable { Haptics.tick(ctx); onClick() }.padding(horizontal = 16.dp, vertical = 9.dp), contentAlignment = Alignment.Center) {
        Text(text, color = fg, fontSize = 13.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
    }
}

@Composable
private fun EmptyNote() = EmptyState(Icons.Rounded.GridView, tr("Nothing here yet", "مفيش حاجة هنا لسه"), tr("Try another collection above.", "جرّب مجموعة تانية من فوق."))

/** One empty-state pattern for the whole app: a quiet icon, a line that says what's missing, and a way forward. */
@Composable
fun EmptyState(icon: ImageVector, title: String, body: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(horizontal = Sp.xxl, vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(64.dp).clip(RoundedCornerShape(Rad.l)).background(Z.tint(0.05f)).border(1.dp, Z.line, RoundedCornerShape(Rad.l)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Z.sub, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(Sp.l))
        Text(title, color = Z.text, fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(body, color = Z.sub, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 360.dp))
        if (action != null && onAction != null) {
            Spacer(Modifier.height(Sp.xl))
            PrimaryButton(action, Modifier.widthIn(min = 200.dp)) { onAction() }
        }
    }
}

/** Page title used by the top-level tabs — compact, so content starts high on the screen. */
@Composable
fun TabTitle(title: String, sub: String? = null, trailing: @Composable () -> Unit = {}) {
    val gut = Sp.gutter(LocalPaneWidth.current)
    Row(Modifier.fillMaxWidth().padding(start = gut, end = gut, top = Sp.l + 4.dp, bottom = Sp.m), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Z.text, maxLines = 1)
            if (sub != null) Text(sub, fontSize = 13.sp, color = Z.sub, maxLines = 2)
        }
        trailing()
    }
}

/** Entry to the wallpaper studio: a calm dark card with a small art swatch instead of a full rainbow slab. */
@Composable
private fun WallpapersEntry(modifier: Modifier, tall: Boolean, onClick: () -> Unit) {
    val swatch = Brush.linearGradient(listOf(Color(0xFFEE9B62), Color(0xFF9B59B6), Color(0xFF3B3A8F)))
    Box(modifier.pressScale(0.98f).clip(RoundedCornerShape(Rad.xl)).background(Z.surface).border(1.dp, Z.line, RoundedCornerShape(Rad.xl)).clickable(onClick = onClick)) {
        if (tall) {
            Column(Modifier.fillMaxSize().padding(Sp.l + 2.dp)) {
                Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(Rad.l)).background(swatch), contentAlignment = Alignment.Center) {
                    Text("09:41", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(Sp.m))
                Text(tr("Wallpapers", "الخلفيات"), color = Z.text, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(tr("Put the clock behind your photo", "حط الساعة ورا صورتك"), color = Z.sub, fontSize = 13.sp, maxLines = 2)
            }
        } else {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(52.dp).clip(RoundedCornerShape(Rad.s + 2.dp)).background(swatch), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Wallpaper, null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(tr("Wallpapers", "الخلفيات"), color = Z.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(tr("Put the clock behind your photo", "حط الساعة ورا صورتك"), color = Z.sub, fontSize = 13.sp, maxLines = 1)
                }
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Z.sub)
            }
        }
    }
}

@Composable
private fun FilterPill(text: String, sel: Boolean, icon: ImageVector?, color: Color, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(50))
            .background(if (sel) Z.text else Z.tint(0.05f))
            .border(1.dp, if (sel) Color.Transparent else Z.line, RoundedCornerShape(50))
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { Icon(icon, null, tint = if (sel) Z.bg else color, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)) }
        Text(text, fontSize = 13.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium, color = if (sel) Z.bg else Z.text, maxLines = 1)
    }
}


// =============================================================================== grids
/** Pair small (1-column) widgets so the 2-column grid never leaves an empty half row. */
fun pack(list: List<WidgetStyle>): List<WidgetStyle> {
    val out = ArrayList<WidgetStyle>(list.size)
    var pending: WidgetStyle? = null
    for (s in list) {
        if (s.shape == Shape.SMALL) {
            if (pending != null) { out += pending; out += s; pending = null } else pending = s
        } else out += s
    }
    pending?.let { out += it }
    return out
}

@Composable
fun StyleGrid(list: List<WidgetStyle>, header: (@Composable () -> Unit)? = null, ads: Boolean = true, adEvery: Int = com.smrh.zujaj.ads.AdPolicy.every(com.smrh.zujaj.ads.AdPolicy.Place.CATALOG), onPick: (WidgetStyle) -> Unit) {
    // small ones first, then the bigger sizes; each size under its own label, several per row;
    // free before Pro; a small ad every few rows
    val ordered = tidy(list)
    val w = LocalPaneWidth.current
    val cols = 60 // every row count used (2, 3, 4, 5, 6) divides 60
    LazyVerticalGrid(
        columns = GridCells.Fixed(cols),
        contentPadding = PaddingValues(start = Sp.gutter(w), end = Sp.gutter(w), top = 4.dp, bottom = LocalDockSpace.current),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (header != null) item(span = { GridItemSpan(cols) }) { header() }
        var slot = 0
        var rows = 0
        listOf(Shape.SMALL, Shape.WIDE, Shape.BANNER, Shape.TALL, Shape.LARGE, Shape.XL).forEach { shape ->
            val group = ordered.filter { it.shape == shape }
            if (group.isEmpty()) return@forEach
            val per = perRow(shape, w)
            item(key = "grp_$shape", span = { GridItemSpan(cols) }) { GroupLabel("${shapeGroupTitle(shape)} · ${group.size}") }
            group.chunked(per).forEach { row ->
                row.forEach { st ->
                    item(key = st.id, span = { GridItemSpan(cols / per) }) { PreviewTile(st, Modifier.animateItem(), compact = shape != Shape.SMALL) { onPick(st) } }
                }
                // keep the grid aligned when a row isn't full
                if (row.size < per) item(key = "pad_${row.first().id}", span = { GridItemSpan(cols / per * (per - row.size)) }) { Spacer(Modifier) }
                rows++
                if (ads && adEvery > 0 && rows % adEvery == 0) { val sl = slot++; item(key = "ad_g_${row.first().id}", span = { GridItemSpan(cols) }) { com.smrh.zujaj.ads.AdBanner(sl) } }
            }
        }
    }
}

@Composable
private fun SizeFilter(sel: Shape?, onSel: (Shape?) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val opts = listOf<Shape?>(null) + Shape.values().toList()
        items(opts) { o ->
            ZChip(o?.let { sizeLabel(it) } ?: tr("All sizes", "كل المقاسات"), sel == o) { onSel(o) }
        }
    }
}

@Composable
fun CategoryScreen(nav: Nav, s: Screen.Category) {
    var size by rememberSaveable { mutableStateOf("") }
    var tier by rememberSaveable { mutableIntStateOf(0) }
    val ver by Prefs.version.collectAsState()
    touch(ver)
    val base = collection(s.key)
    val list = base.filter { (size.isEmpty() || it.shape.name == size) && (tier == 0 || (tier == 1) == !Flags.isPro(it)) }
    Column(Modifier.fillMaxSize()) {
        TopBar(collectionTitle(s.key), { nav.back() })
        if (s.key != "free") Segmented(listOf(tr("All", "الكل"), tr("Free", "مجاني"), "Pro"), tier, Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) { tier = it }
        val shapes = listOf(Shape.XL, Shape.LARGE, Shape.TALL, Shape.WIDE, Shape.BANNER, Shape.SMALL).filter { sh -> base.any { it.shape == sh } }
        if (shapes.size > 1) LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val opts = listOf("" to tr("All sizes", "كل المقاسات")) + shapes.map { it.name to shapeGroupTitle(it) }
            items(opts) { (k, l) -> FilterPill(l, size == k, null, LocalAccent.current) { size = k } }
        }
        if (list.isEmpty()) EmptyState(Icons.Rounded.FilterAltOff, tr("Nothing matches these filters", "مفيش ويدجت بالفلاتر دي"),
            tr("Show every size and tier again.", "رجّع كل المقاسات والأنواع."), tr("Clear filters", "امسح الفلاتر")) { size = ""; tier = 0 }
        StyleGrid(list) { nav.go(Screen.Editor(it.id)) }
    }
}

@Composable
fun FavoritesTab(nav: Nav, onDiscover: () -> Unit = {}) {
    val ver by Prefs.version.collectAsState()
    touch(ver)
    val list = collection("saved")
    Column(Modifier.fillMaxSize()) {
        TabTitle(tr("Saved", "المفضلة"), if (list.isEmpty()) null else tr("${list.size} widgets you kept", "${list.size} ويدجت محفوظة"))
        if (list.isEmpty()) EmptyState(Icons.Rounded.FavoriteBorder, tr("No saved widgets yet", "لسه مفيش ويدجت محفوظة"),
            tr("Tap ♥ in any widget's editor and it will wait for you here.", "اضغط ♥ جوه أي ويدجت وهتلاقيها مستنياك هنا."), tr("Discover widgets", "اكتشف الويدجت"), onDiscover)
        else StyleGrid(list, adEvery = com.smrh.zujaj.ads.AdPolicy.every(com.smrh.zujaj.ads.AdPolicy.Place.FAVORITES, list.size)) { nav.go(Screen.Editor(it.id)) }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(nav: Nav) {
    var q by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    val all = Flags.visible()
    val res = if (q.isBlank()) emptyList() else all.filter { com.smrh.zujaj.catalog.WidgetSearch.matches(it, q) }
    Column(Modifier.fillMaxSize()) {
        TopBar(tr("Search", "بحث"), { nav.back() })
        OutlinedTextField(
            q, { q = it }, singleLine = true,
            placeholder = { Text(tr("Clock, prayer, calendar…", "ساعة، صلاة، تقويم…")) },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            shape = RoundedCornerShape(Rad.m),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LocalAccent.current, unfocusedBorderColor = Z.line),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).focusRequester(focus),
        )
        if (q.isNotBlank() && res.isEmpty()) EmptyState(Icons.Rounded.SearchOff, tr("No widgets match “$q”", "مفيش نتايج لـ «$q»"), tr("Try a simpler word like clock, prayer or date.", "جرّب كلمة أبسط زي ساعة أو صلاة أو تاريخ."))
        else if (q.isBlank()) {
            // suggestions instead of a blank page
            androidx.compose.foundation.layout.FlowRow(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(tr("Clock", "ساعة"), tr("Prayer", "صلاة"), tr("Calendar", "تقويم"), tr("Weather", "طقس"), tr("Quran", "قرآن"), tr("Battery", "بطارية"), tr("Photo", "صورة")).forEach { w -> ZChip(w, false) { q = w } }
            }
        }
        StyleGrid(res, adEvery = com.smrh.zujaj.ads.AdPolicy.every(com.smrh.zujaj.ads.AdPolicy.Place.SEARCH, res.size)) { nav.go(Screen.Editor(it.id)) }
    }
}

/**
 * Launcher / lock-screen "configure" flow: every design is offered for this widget, whatever
 * size it was placed at (designs scale to fit). The one it shows now is listed first.
 */
@Composable
fun PickScreen(nav: Nav, s: Screen.Pick) {
    var chip by rememberSaveable { mutableStateOf("all") }
    val current = Prefs.widgetConfig(s.widgetId)?.styleId
    val all = Flags.visible()
    val cats = Cat.values().filter { c -> all.any { it.cat == c } }
    val list = (if (chip == "all") all else all.filter { it.cat.name == chip })
        .sortedWith(compareBy<WidgetStyle>({ it.id != current }, { it.shape != s.shape }))
    Column(Modifier.fillMaxSize()) {
        TopBar(tr("Choose any widget", "اختار أي ويدجت"), null)
        Text(
            tr("Every design fits this spot — pick one, customise it, then Save.", "كل التصميمات تنفع للمكان ده — اختار واحدة، عدّلها، واضغط حفظ."),
            color = Z.sub, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp),
        )
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { ZChip(tr("All", "الكل"), chip == "all") { chip = "all" } }
            items(cats) { c -> ZChip(if (Prefs.isArabicUi) c.ar else c.en, chip == c.name) { chip = c.name } }
        }
        StyleGrid(list, ads = false) { st ->
            nav.go(Screen.Editor(st.id, s.widgetId, configure = true))
        }
    }
}

@Composable
private fun RoundAction(icon: ImageVector, bg: Color, tint: Color, label: String? = null, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(CircleShape).background(bg).border(1.dp, Z.line, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, tint = tint, modifier = Modifier.size(21.dp)) }
}


/** Shown once: Pro widgets can now be opened for a few days by watching a few ads. */
@Composable
private fun AdUnlockIntro(onDismiss: () -> Unit) {
    val need = com.smrh.zujaj.ads.AdUnlock.needed(); val days = com.smrh.zujaj.ads.AdUnlock.days()
    val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "ui")
    val step by t.animateFloat(0f, need + 1.5f, androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(2600 + need * 500, easing = androidx.compose.animation.core.LinearEasing)), label = "s")
    val bob by t.animateFloat(-5f, 5f, androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(1200), androidx.compose.animation.core.RepeatMode.Reverse), label = "b")
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val sc by androidx.compose.animation.core.animateFloatAsState(if (shown) 1f else 0.85f, androidx.compose.animation.core.spring(0.55f, 300f), label = "sc")
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.graphicsLayer { scaleX = sc; scaleY = sc }.clip(RoundedCornerShape(30.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF2B2140), Z.surface))).padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(tr("NEW", "جديد"), color = Z.pro, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(14.dp))
            // lock that opens as the ads fill up
            val opened = step >= need
            Box(Modifier.size(84.dp).graphicsLayer { translationY = bob }.clip(CircleShape)
                .background(Brush.linearGradient(if (opened) listOf(Color(0xFF3DDC97), Color(0xFF1E9E6B)) else listOf(Color(0xFFF6D98B), Color(0xFFB07A2A)))),
                contentAlignment = Alignment.Center) {
                Icon(if (opened) Icons.Rounded.LockOpen else Icons.Rounded.Lock, null, tint = Color.White, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(need) { i ->
                    val on = step > i + 0.5f
                    Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(if (on) Color.White else Z.tint(0.12f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.PlayArrow, null, tint = if (on) Color(0xFF2B2140) else Color.White.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(tr("Pro widgets — free with ads", "ويدجت البرو ببلاش بالإعلانات"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(tr("Open any Pro widget, watch $need short ads and it's yours for $days days. Each widget opens on its own.",
                "افتح أي ويدجت برو، شوف $need إعلانات قصيرة، وهتبقى بتاعتك لمدة $days أيام بس. كل ويدجت بتتفتح لوحدها."),
                color = Z.sub, textAlign = TextAlign.Center, fontSize = 14.sp)
            Spacer(Modifier.height(18.dp))
            PrimaryButton(tr("Got it", "تمام"), Modifier.fillMaxWidth(), onClick = onDismiss)
        }
    }
}
