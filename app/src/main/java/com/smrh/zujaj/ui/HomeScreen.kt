package com.smrh.zujaj.ui

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
@Composable
fun HomeScreen(nav: Nav) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
            // tabs glide sideways in the direction of the tab you picked
            androidx.compose.animation.AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    (androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(260, 60)) +
                        androidx.compose.animation.slideInHorizontally(androidx.compose.animation.core.tween(320, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { it / 6 * dir })
                        .togetherWith(androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(160)) +
                            androidx.compose.animation.slideOutHorizontally(androidx.compose.animation.core.tween(320)) { -it / 10 * dir })
                },
                label = "tabs",
            ) { t ->
                when (t) {
                    0 -> DiscoverTab(nav)
                    1 -> FavoritesTab(nav)
                    2 -> MyWidgetsTab(nav)
                    else -> SettingsScreen(nav, asTab = true)
                }
            }
        }
        // labels follow the app language even when it changes while this screen is open
        val ver by Prefs.version.collectAsState()
        key(ver, Prefs.isArabicUi) { DockBar(tab, Modifier.align(Alignment.BottomCenter)) { tab = it } }
    }
}

@Composable
private fun DockBar(tab: Int, modifier: Modifier, onTab: (Int) -> Unit) {
    val accent = LocalAccent.current
    val items = listOf(
        Icons.Rounded.Home to tr("Discover", "اكتشف"),
        Icons.Rounded.Favorite to tr("Saved", "المفضلة"),
        Icons.Rounded.Dashboard to tr("Mine", "ويدجتاتي"),
        Icons.Rounded.Settings to tr("Settings", "الإعدادات"),
    )
    // floating glass capsule; the selected tab grows into a lit pill with its label
    Box(
        modifier.fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC07070C), Color(0xF207070C))))
            .navigationBarsPadding().padding(top = 26.dp, bottom = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier.shadow(24.dp, RoundedCornerShape(50), ambientColor = Color.Black, spotColor = Color.Black)
                .clip(RoundedCornerShape(50))
                .background(Brush.verticalGradient(listOf(Color(0xF2202028), Color(0xF2121218))))
                .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.04f))), RoundedCornerShape(50))
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { i, (icon, label) ->
                val sel = i == tab
                Row(
                    Modifier.animateContentSize(androidx.compose.animation.core.spring(dampingRatio = 0.62f, stiffness = 420f)).clip(RoundedCornerShape(50))
                        .background(if (sel) Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.78f))) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)))
                        .clickable { onTab(i) }
                        .padding(horizontal = if (sel) 18.dp else 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val bounce by androidx.compose.animation.core.animateFloatAsState(if (sel) 1.12f else 1f, androidx.compose.animation.core.spring(dampingRatio = 0.4f, stiffness = 500f), label = "b")
                    Icon(icon, label, tint = if (sel) Color(0xFF14110A) else Z.sub, modifier = Modifier.size(22.dp).graphicsLayer { scaleX = bounce; scaleY = bounce })
                    if (sel) {
                        Spacer(Modifier.width(8.dp))
                        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF14110A), maxLines = 1)
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

/** Size groups in the order they are shown: big first, small last. */
fun shapeOrder(s: Shape) = when (s) { Shape.LARGE -> 0; Shape.WIDE -> 1; Shape.BANNER -> 2; Shape.SMALL -> 3 }

fun shapeGroupTitle(s: Shape) = when (s) {
    Shape.LARGE -> tr("Large", "كبيرة")
    Shape.WIDE -> tr("Wide", "عريضة")
    Shape.BANNER -> tr("Banners", "شرائط")
    Shape.SMALL -> tr("Small", "صغيرة")
}

/** Tidy order: by size (big → small), then free before Pro, then the catalogue order. */
fun tidy(list: List<WidgetStyle>): List<WidgetStyle> =
    list.withIndex().sortedWith(compareBy({ shapeOrder(it.value.shape) }, { if (Flags.isPro(it.value)) 1 else 0 }, { it.index })).map { it.value }

/** A widget on a soft stage that looks like a bit of a home screen; badge in the corner, name under it. */
@Composable
fun PreviewTile(st: WidgetStyle, modifier: Modifier = Modifier, showName: Boolean = true, stage: androidx.compose.ui.unit.Dp? = null, onClick: () -> Unit) {
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
                .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(24.dp))
                .padding(if (small) 16.dp else 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            WidgetPreview(st.defaultConfig(), if (stage != null) Modifier.fillMaxSize() else Modifier.fillMaxWidth(), density = 2f)
        }
        if (showName) {
            Row(Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, top = 9.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(st.name(Prefs.isArabicUi), color = Z.text.copy(alpha = 0.9f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
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
            Row(Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.06f)).clickable(onClick = onAll).padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
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
    val featured = remember(flags) {
        listOf("prayer_hub", "calli_lawn", "photo_verse", "event_card", "prayer_journey", "clock_neon", "w_today_tomorrow", "event_ring")
            .mapNotNull { id -> all.firstOrNull { it.id == id } }
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
    if (showNew) WallIntroDialog(onTry = { Prefs.setString("wall_intro", "1"); showNew = false; nav.go(Screen.Wallpapers) }) { Prefs.setString("wall_intro", "1"); showNew = false }

    val list = remember(chip, flags, ver) { tidy(collection(chip)) }
    val drift = androidx.compose.animation.core.rememberInfiniteTransition(label = "home")
    val t by drift.animateFloat(0f, 1f, androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(9000, easing = androidx.compose.animation.core.LinearEasing), androidx.compose.animation.core.RepeatMode.Reverse), label = "t")

    LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = 130.dp)) {
        // ---- header over a slowly drifting aurora
        item(key = "head") {
            Box(Modifier.fillMaxWidth().drawBehind {
                val w = size.width; val h = size.height
                val c1 = androidx.compose.ui.geometry.Offset(w * (0.12f + 0.55f * t), h * 0.25f)
                val c2 = androidx.compose.ui.geometry.Offset(w * (0.95f - 0.45f * t), h * 0.75f)
                drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = 0.26f), Color.Transparent), center = c1, radius = w * 0.6f), radius = w * 0.6f, center = c1)
                drawCircle(Brush.radialGradient(listOf(Color(0xFF7C6CF2).copy(alpha = 0.22f), Color.Transparent), center = c2, radius = w * 0.55f), radius = w * 0.55f, center = c2)
            }) {
                Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 18.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(greet, color = Z.sub, fontSize = 13.sp)
                        Text(tr("Zujaj", "زجاج"), fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1,
                            style = androidx.compose.ui.text.TextStyle(brush = Brush.linearGradient(listOf(Color.White, accent.copy(alpha = 0.9f)))))
                    }
                    RoundAction(Icons.Rounded.Search, Color.White.copy(alpha = 0.08f), Z.text) { nav.go(Screen.Search) }
                    Spacer(Modifier.width(8.dp))
                    val pulse by drift.animateFloat(1f, 1.08f, androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(1400), androidx.compose.animation.core.RepeatMode.Reverse), label = "pulse")
                    Box(Modifier.graphicsLayer { if (!pro) { scaleX = pulse; scaleY = pulse } }) {
                        RoundAction(Icons.Rounded.WorkspacePremium, if (pro) Color.White.copy(alpha = 0.08f) else Z.pro, if (pro) Z.pro else Color(0xFF2A1C05)) { nav.go(Screen.Paywall) }
                    }
                }
            }
        }
        if (flags.notice.isNotBlank() && !flags.noticeOnce) item(key = "notice") {
            Text(flags.notice, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.06f)).padding(14.dp))
        }
        // ---- showcase: auto-sliding, the side cards shrink and fade (parallax)
        if (featured.isNotEmpty()) item(key = "show") {
            val pager = rememberPagerState { featured.size }
            LaunchedEffect(pager) { while (true) { delay(4500); pager.animateScrollToPage((pager.currentPage + 1) % featured.size) } }
            Column {
                HorizontalPager(pager, contentPadding = PaddingValues(horizontal = 22.dp), pageSpacing = 12.dp) { page ->
                    val st = featured[page]
                    val off = ((pager.currentPage - page) + pager.currentPageOffsetFraction).coerceIn(-1f, 1f)
                    Box(Modifier.fillMaxWidth().height(230.dp)
                        .graphicsLayer { val k = 1f - 0.08f * kotlin.math.abs(off); scaleX = k; scaleY = k; alpha = 1f - 0.4f * kotlin.math.abs(off) }
                        .clip(RoundedCornerShape(30.dp)).background(Backdrops.brush(page + 1))
                        .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(30.dp))
                        .clickable { nav.go(Screen.Editor(st.id)) }) {
                        Box(Modifier.fillMaxWidth().padding(start = if (st.shape == Shape.SMALL) 80.dp else 22.dp, end = if (st.shape == Shape.SMALL) 80.dp else 22.dp, top = 20.dp)
                            .graphicsLayer { translationX = off * 60.dp.toPx() }, contentAlignment = Alignment.TopCenter) {
                            WidgetPreview(st.defaultConfig(), Modifier.fillMaxWidth(), density = 2.3f)
                        }
                        Row(Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xC0000000))))
                            .padding(start = 20.dp, end = 14.dp, top = 20.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(tr("FEATURED", "مختارات"), color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                                Text(st.name(Prefs.isArabicUi), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                            Box(Modifier.size(40.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = Color.Black, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.Center) {
                    repeat(featured.size) { i ->
                        val sel = i == pager.currentPage
                        val w by androidx.compose.animation.core.animateDpAsState(if (sel) 20.dp else 6.dp, label = "dot")
                        Box(Modifier.padding(horizontal = 3.dp).height(6.dp).width(w).clip(RoundedCornerShape(50)).background(if (sel) accent else Color.White.copy(alpha = 0.2f)))
                    }
                }
            }
        }
        // ---- wallpapers card with a moving sheen
        item(key = "wall") {
            val src = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            val pressed by src.collectIsPressedAsState()
            val sc by androidx.compose.animation.core.animateFloatAsState(if (pressed) 0.97f else 1f, androidx.compose.animation.core.spring(dampingRatio = 0.55f, stiffness = 500f), label = "wp")
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp).graphicsLayer { scaleX = sc; scaleY = sc }.clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(listOf(Color(0xFFEE9B62), Color(0xFF9B59B6), Color(0xFF3B3A8F))))
                .drawBehind {
                    val x = size.width * (t * 1.6f - 0.3f)
                    drawRect(Brush.linearGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.22f), Color.Transparent),
                        start = androidx.compose.ui.geometry.Offset(x - 120f, 0f), end = androidx.compose.ui.geometry.Offset(x + 120f, size.height)))
                }
                .clickable(interactionSource = src, indication = androidx.compose.material3.ripple()) { nav.go(Screen.Wallpapers) }.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val bob by drift.animateFloat(-3f, 3f, androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(1600), androidx.compose.animation.core.RepeatMode.Reverse), label = "bob")
                    Box(Modifier.size(48.dp).graphicsLayer { translationY = bob * density }.clip(RoundedCornerShape(15.dp)).background(Color.White.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Wallpaper, null, tint = Color.White, modifier = Modifier.size(25.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(tr("Wallpapers", "الخلفيات"), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        Text(tr("Put the clock behind your photo", "حط الساعة ورا صورتك"), color = Color.White.copy(alpha = 0.88f), fontSize = 13.sp, maxLines = 1)
                    }
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Color.White)
                }
            }
        }
        // ---- collection tabs (stay on top while scrolling)
        stickyHeader(key = "tabs") {
            LazyRow(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Z.bg, Z.bg, Z.bg.copy(alpha = 0.92f)))),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val keys = listOf("popular", "new", "free") + cats.map { it.name }
                items(keys, key = { it }) { k -> TabPill(collectionTitle(k), chip == k, accent) { chip = k } }
            }
        }
        if (list.isEmpty()) item(key = "empty_$chip") { EmptyNote() }
        // ---- the chosen collection, grouped by size; rows slide in one after another
        var rowsSinceAd = 0
        var slot = 0
        var idx = 0
        listOf(Shape.LARGE, Shape.WIDE, Shape.BANNER, Shape.SMALL).forEach { shape ->
            val group = list.filter { it.shape == shape }
            if (group.isEmpty()) return@forEach
            item(key = "g_${chip}_$shape") { GroupLabel("${shapeGroupTitle(shape)} · ${group.size}") }
            val rows = if (shape == Shape.SMALL) group.chunked(2) else group.map { listOf(it) }
            rows.forEach { row ->
                val order = idx++
                item(key = "r_${chip}_${row.joinToString { it.id }}") {
                    val shown = remember { androidx.compose.animation.core.Animatable(0f) }
                    LaunchedEffect(Unit) { delay((order.coerceAtMost(6) * 55L)); shown.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.8f, stiffness = 220f)) }
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp)
                        .graphicsLayer { alpha = shown.value; translationY = (1f - shown.value) * 46.dp.toPx(); val k = 0.94f + 0.06f * shown.value; scaleX = k; scaleY = k },
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { st -> PreviewTile(st, Modifier.weight(1f)) { nav.go(Screen.Editor(st.id)) } }
                        if (row.size == 1 && shape == Shape.SMALL) Spacer(Modifier.weight(1f))
                    }
                }
                rowsSinceAd++
                if (rowsSinceAd >= 4) { rowsSinceAd = 0; val sl = slot++; item(key = "ad_${chip}_${row.first().id}") { com.smrh.zujaj.ads.AdBanner(sl) } }
            }
        }
    }
    // a new tab starts from its top (tabs stay pinned)
    LaunchedEffect(chip) {
        val tabs = (if (featured.isNotEmpty()) 3 else 2) + (if (flags.notice.isNotBlank() && !flags.noticeOnce) 1 else 0)
        if (listState.firstVisibleItemIndex > tabs) listState.animateScrollToItem(tabs)
    }
}

/** Collection tab: the selected one fills with colour and grows a little (springy). */
@Composable
private fun TabPill(text: String, sel: Boolean, accent: Color, onClick: () -> Unit) {
    val bg by androidx.compose.animation.animateColorAsState(if (sel) accent else Color.White.copy(alpha = 0.06f), androidx.compose.animation.core.tween(260), label = "tbg")
    val fg by androidx.compose.animation.animateColorAsState(if (sel) Color(0xFF14110A) else Z.text, androidx.compose.animation.core.tween(260), label = "tfg")
    val k by androidx.compose.animation.core.animateFloatAsState(if (sel) 1.06f else 1f, androidx.compose.animation.core.spring(dampingRatio = 0.45f, stiffness = 500f), label = "tk")
    Box(Modifier.graphicsLayer { scaleX = k; scaleY = k }.clip(RoundedCornerShape(50)).background(bg)
        .border(1.dp, if (sel) Color.Transparent else Z.line, RoundedCornerShape(50))
        .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 9.dp)) {
        Text(text, color = fg, fontSize = 13.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
    }
}

@Composable
private fun EmptyNote() {
    Column(Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Rounded.GridView, null, tint = Z.sub, modifier = Modifier.size(34.dp))
        Spacer(Modifier.height(10.dp))
        Text(tr("Nothing matches these filters", "مفيش ويدجت بالفلاتر دي"), color = Z.sub, textAlign = TextAlign.Center)
    }
}

@Composable
private fun FilterPill(text: String, sel: Boolean, icon: ImageVector?, color: Color, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(50))
            .background(if (sel) Z.text else Color.White.copy(alpha = 0.05f))
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
fun StyleGrid(list: List<WidgetStyle>, header: (@Composable () -> Unit)? = null, ads: Boolean = true, onPick: (WidgetStyle) -> Unit) {
    // big ones first, then small ones in pairs; free before Pro; a small ad every few rows
    val ordered = pack(tidy(list))
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (header != null) item(span = { GridItemSpan(2) }) { header() }
        var cells = 0
        var slot = 0
        ordered.forEach { st ->
            item(key = st.id, span = { GridItemSpan(if (st.shape == Shape.SMALL) 1 else 2) }) { PreviewTile(st, Modifier.animateItem()) { onPick(st) } }
            cells += if (st.shape == Shape.SMALL) 1 else 2
            if (ads && cells >= 10 && cells % 2 == 0) { cells = 0; val sl = slot++; item(key = "ad_g_${st.id}", span = { GridItemSpan(2) }) { com.smrh.zujaj.ads.AdBanner(sl) } }
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
        val shapes = listOf(Shape.LARGE, Shape.WIDE, Shape.BANNER, Shape.SMALL).filter { sh -> base.any { it.shape == sh } }
        if (shapes.size > 1) LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val opts = listOf("" to tr("All sizes", "كل المقاسات")) + shapes.map { it.name to shapeGroupTitle(it) }
            items(opts) { (k, l) -> FilterPill(l, size == k, null, LocalAccent.current) { size = k } }
        }
        if (list.isEmpty()) {
            Text(
                if (s.key == "saved") tr("Tap ♥ in the editor to save widgets here", "اضغط ♥ داخل المحرر لحفظ الويدجت هنا") else tr("Nothing matches these filters", "مفيش ويدجت بالفلاتر دي"),
                color = Z.sub, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(40.dp),
            )
        }
        StyleGrid(list) { nav.go(Screen.Editor(it.id)) }
    }
}

@Composable
fun FavoritesTab(nav: Nav) {
    val ver by Prefs.version.collectAsState()
    touch(ver)
    val list = collection("saved")
    Column(Modifier.fillMaxSize()) {
        Text(tr("Saved", "المفضلة"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 10.dp))
        if (list.isEmpty()) {
            Column(Modifier.fillMaxWidth().padding(36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(72.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.06f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Favorite, null, tint = LocalAccent.current, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.height(14.dp))
                Text(tr("Tap ♥ on any widget to keep it here", "اضغط ♥ على أي ويدجت لتحفظه هنا"), color = Z.sub, textAlign = TextAlign.Center)
            }
        } else StyleGrid(list) { nav.go(Screen.Editor(it.id)) }
    }
}

@Composable
fun SearchScreen(nav: Nav) {
    var q by remember { mutableStateOf("") }
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
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LocalAccent.current, unfocusedBorderColor = Z.line),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).focusRequester(focus),
        )
        if (q.isNotBlank() && res.isEmpty()) Text(tr("No widgets match", "لا توجد نتائج"), color = Z.sub, modifier = Modifier.padding(24.dp))
        StyleGrid(res) { nav.go(Screen.Editor(it.id)) }
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
        StyleGrid(list) { st ->
            nav.go(Screen.Editor(st.id, s.widgetId, configure = true))
        }
    }
}

@Composable
private fun RoundAction(icon: ImageVector, bg: Color, tint: Color, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(CircleShape).background(bg).border(1.dp, Z.line, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = tint, modifier = Modifier.size(21.dp)) }
}
