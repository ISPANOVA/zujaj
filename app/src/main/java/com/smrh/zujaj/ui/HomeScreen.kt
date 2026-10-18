package com.smrh.zujaj.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
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
        DockBar(tab, Modifier.align(Alignment.BottomCenter)) { tab = it }
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
                    Modifier.animateContentSize().clip(RoundedCornerShape(50))
                        .background(if (sel) Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.78f))) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)))
                        .clickable { onTab(i) }
                        .padding(horizontal = if (sel) 18.dp else 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(icon, label, tint = if (sel) Color(0xFF14110A) else Z.sub, modifier = Modifier.size(22.dp))
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

/** A widget shown on a soft dark stage, name centred under it. */
@Composable
fun PreviewTile(st: WidgetStyle, modifier: Modifier = Modifier, showName: Boolean = true, stage: androidx.compose.ui.unit.Dp? = null, onClick: () -> Unit) {
    val small = st.shape == Shape.SMALL
    val src = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(if (pressed) 0.95f else 1f, androidx.compose.animation.core.spring(dampingRatio = 0.55f, stiffness = 500f), label = "press")
    Column(modifier.graphicsLayer { scaleX = scale; scaleY = scale }.clip(RoundedCornerShape(28.dp))
        .clickable(interactionSource = src, indication = androidx.compose.material3.ripple(), onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.fillMaxWidth().then(if (stage != null) Modifier.height(stage) else Modifier).clip(RoundedCornerShape(28.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF17171D), Color(0xFF0E0E13))))
                .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.09f), Color.White.copy(alpha = 0.02f))), RoundedCornerShape(28.dp))
                .padding(if (small) 18.dp else 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            WidgetPreview(st.defaultConfig(), if (stage != null) Modifier.fillMaxSize() else Modifier.fillMaxWidth(), density = 2f)
        }
        if (showName) {
            Row(Modifier.padding(top = 10.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(st.name(Prefs.isArabicUi), color = Z.text.copy(alpha = 0.86f), fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (Flags.isPro(st)) ProBadge()
                else if (Flags.isNew(st)) NewBadge()
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
private fun SectionHeader(title: String, count: Int?, onAll: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 26.dp, bottom = 12.dp), verticalAlignment = Alignment.Bottom) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (onAll != null) {
            Row(Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onAll).padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text((count?.let { "$it · " } ?: "") + tr("See all", "عرض الكل"), color = LocalAccent.current, fontSize = 13.sp)
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = LocalAccent.current, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// =============================================================================== discover
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DiscoverTab(nav: Nav) {
    val pro by Billing.pro.collectAsState()
    val flags by Flags.flags.collectAsState()
    val ver by Prefs.version.collectAsState()
    touch(ver)
    val accent = LocalAccent.current
    val all = Flags.visible()
    var chip by rememberSaveable { mutableStateOf("all") }
    val featured = remember(flags) {
        listOf("prayer_spotlight", "clock_arabesque", "digital_fade", "date_calli_full", "clock_wide", "digital_neon")
            .mapNotNull { id -> all.firstOrNull { it.id == id } }
    }
    val hour = LocalTime.now().hour
    val greet = when (hour) {
        in 5..11 -> tr("Good morning", "صباح الخير")
        in 12..16 -> tr("Good afternoon", "طاب يومك")
        else -> tr("Good evening", "مساء الخير")
    }
    val cats = Cat.values().filter { c -> all.any { it.cat == c } }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 130.dp)) {
        // ---- header: greeting + round actions
        item {
            Row(Modifier.fillMaxWidth().padding(start = 22.dp, end = 16.dp, top = 18.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(greet, color = Z.sub, fontSize = 13.sp)
                    Text(tr("Zujaj", "زجاج"), fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, softWrap = false,
                        letterSpacing = if (Prefs.isArabicUi) 0.sp else (-0.5).sp)
                }
                RoundAction(Icons.Rounded.Search, Color.White.copy(alpha = 0.07f), Z.text) { nav.go(Screen.Search) }
                Spacer(Modifier.width(10.dp))
                RoundAction(
                    Icons.Rounded.WorkspacePremium,
                    if (pro) Color(0xFF2A2210) else Z.pro, if (pro) Z.pro else Color(0xFF2A1C05),
                ) { nav.go(Screen.Paywall) }
            }
        }
        if (flags.notice.isNotBlank()) item {
            Glass(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), radius = 18.dp, alpha = 0.08f) {
                Text(flags.notice, modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.bodyMedium)
            }
        }
        // ---- category chips (filter the feed in place)
        item {
            val keys = listOf("all", "popular", "new") + cats.map { it.name }
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(keys) { key ->
                    val sel = key == chip
                    val cat = Cat.values().firstOrNull { it.name == key }
                    val icon = cat?.let { catIcon(it) } ?: when (key) { "popular" -> Icons.Rounded.Whatshot; "new" -> Icons.Rounded.AutoAwesome; else -> Icons.Rounded.GridView }
                    Row(
                        Modifier.clip(RoundedCornerShape(50))
                            .background(if (sel) Z.text else Color.White.copy(alpha = 0.06f))
                            .border(1.dp, if (sel) Color.Transparent else Z.line, RoundedCornerShape(50))
                            .clickable { chip = key }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(icon, null, tint = if (sel) Z.bg else (cat?.let { catColor(it) } ?: accent), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (key == "all") tr("All", "الكل") else collectionTitle(key), fontSize = 13.sp,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium, color = if (sel) Z.bg else Z.text)
                    }
                }
            }
        }
        if (chip == "all") {
            // ---- featured pager
            if (featured.isNotEmpty()) item {
                val pager = rememberPagerState { featured.size }
                LaunchedEffect(pager) {
                    while (true) {
                        delay(5000)
                        pager.animateScrollToPage((pager.currentPage + 1) % featured.size)
                    }
                }
                Column {
                    HorizontalPager(pager, contentPadding = PaddingValues(horizontal = 16.dp), pageSpacing = 12.dp) { page ->
                        val st = featured[page]
                        Box(
                            Modifier.fillMaxWidth().height(280.dp).clip(RoundedCornerShape(32.dp))
                                .background(Backdrops.brush(page + 1))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(32.dp))
                                .clickable { nav.go(Screen.Editor(st.id)) },
                        ) {
                            Box(
                                Modifier.fillMaxWidth().padding(
                                    start = if (st.shape == Shape.SMALL) 92.dp else 22.dp,
                                    end = if (st.shape == Shape.SMALL) 92.dp else 22.dp, top = 24.dp,
                                ),
                                contentAlignment = Alignment.TopCenter,
                            ) { WidgetPreview(st.defaultConfig(), Modifier.fillMaxWidth(), density = 2.4f) }
                            Row(
                                Modifier.align(Alignment.BottomStart).fillMaxWidth()
                                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xC0000000))))
                                    .padding(start = 22.dp, end = 14.dp, top = 22.dp, bottom = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(tr("FEATURED", "مختارات"), color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                                    Text(st.name(Prefs.isArabicUi), color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                }
                                Box(Modifier.size(42.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = Color.Black, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.Center) {
                        repeat(featured.size) { i ->
                            val sel = i == pager.currentPage
                            Box(Modifier.padding(horizontal = 3.dp).height(5.dp).width(if (sel) 20.dp else 5.dp).clip(RoundedCornerShape(50))
                                .background(if (sel) accent else Color.White.copy(alpha = 0.2f)))
                        }
                    }
                }
            }
            // ---- one shelf per category
            cats.forEach { cat ->
                val list = all.filter { it.cat == cat }
                item(key = "row_${cat.name}") {
                    Column(Modifier.animateItem()) {
                    SectionHeader(if (Prefs.isArabicUi) cat.ar else cat.en, list.size) { chip = cat.name }
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(list.take(10), key = { it.id }) { st ->
                            // every card in a shelf has the same height so rows line up calmly
                            PreviewTile(st, Modifier.width(if (st.shape == Shape.SMALL) 168.dp else 300.dp), stage = 168.dp) { nav.go(Screen.Editor(st.id)) }
                        }
                    }
                    }
                }
            }
        } else {
            // ---- filtered feed: a calm vertical showcase
            val list = collection(chip)
            val rows = feedRows(list)
            item(key = "title_$chip") {
                Text(
                    "${collectionTitle(chip)} · ${list.size}", color = Z.sub, fontSize = 13.sp,
                    modifier = Modifier.animateItem().padding(start = 22.dp, end = 22.dp, bottom = 10.dp),
                )
            }
            // keyed rows: switching Clock ↔ Digital fades the old cards out and the new ones in, sliding up
            items(rows.size, key = { i -> "f_${chip}_" + rows[i].joinToString { it.id } }) { i ->
                val row = rows[i]
                val shown = remember { androidx.compose.animation.core.Animatable(0f) }
                LaunchedEffect(Unit) { kotlinx.coroutines.delay((i * 45L).coerceAtMost(270L)); shown.animateTo(1f, androidx.compose.animation.core.tween(380, easing = androidx.compose.animation.core.FastOutSlowInEasing)) }
                Row(Modifier.animateItem().graphicsLayer { alpha = shown.value; translationY = (1f - shown.value) * 40.dp.toPx() }
                    .fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    row.forEach { st -> PreviewTile(st, Modifier.weight(1f)) { nav.go(Screen.Editor(st.id)) } }
                    if (row.size == 1 && row[0].shape == Shape.SMALL) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/** Two small widgets per row, wide ones on their own row. */
private fun feedRows(list: List<WidgetStyle>): List<List<WidgetStyle>> {
    val out = ArrayList<List<WidgetStyle>>()
    for (s in pack(list)) {
        val last = out.lastOrNull()
        if (s.shape == Shape.SMALL && last != null && last.size == 1 && last[0].shape == Shape.SMALL) out[out.size - 1] = last + s
        else out += listOf(s)
    }
    return out
}

@Composable
private fun RoundAction(icon: ImageVector, bg: Color, tint: Color, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(CircleShape).background(bg).border(1.dp, Z.line, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = tint, modifier = Modifier.size(21.dp)) }
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
fun StyleGrid(list: List<WidgetStyle>, header: (@Composable () -> Unit)? = null, onPick: (WidgetStyle) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (header != null) item(span = { GridItemSpan(2) }) { header() }
        items(pack(list), key = { it.id }, span = { GridItemSpan(if (it.shape == Shape.SMALL) 1 else 2) }) { st ->
            PreviewTile(st, Modifier.animateItem()) { onPick(st) }
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
    var size by remember { mutableStateOf<Shape?>(null) }
    val ver by Prefs.version.collectAsState()
    touch(ver)
    val list = collection(s.key).filter { size == null || it.shape == size }
    Column(Modifier.fillMaxSize()) {
        TopBar(collectionTitle(s.key), { nav.back() })
        SizeFilter(size) { size = it }
        if (list.isEmpty()) {
            Text(
                if (s.key == "saved") tr("Tap ♥ in the editor to save widgets here", "اضغط ♥ داخل المحرر لحفظ الويدجت هنا") else tr("Nothing here yet", "لا يوجد شيء هنا بعد"),
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
    val res = if (q.isBlank()) emptyList() else all.filter {
        it.en.contains(q, true) || it.ar.contains(q) || it.cat.en.contains(q, true) || it.cat.ar.contains(q)
    }
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
