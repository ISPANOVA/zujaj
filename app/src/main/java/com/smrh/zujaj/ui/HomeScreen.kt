package com.smrh.zujaj.ui

import androidx.compose.foundation.ExperimentalFoundationApi
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
            when (tab) {
                0 -> DiscoverTab(nav)
                1 -> FavoritesTab(nav)
                2 -> MyWidgetsTab(nav)
                else -> SettingsScreen(nav, asTab = true)
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
    Column(modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xF207070C), Color(0xFF07070C))))) {
        Spacer(Modifier.height(18.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Z.line))
        Row(
            Modifier.fillMaxWidth().background(Color(0xFF07070C)).navigationBarsPadding().padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            items.forEachIndexed { i, (icon, label) ->
                val sel = i == tab
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).clickable { onTab(i) }.padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.clip(RoundedCornerShape(50)).background(if (sel) accent.copy(alpha = 0.18f) else Color.Transparent)
                            .padding(horizontal = 18.dp, vertical = 5.dp)
                    ) { Icon(icon, null, tint = if (sel) accent else Z.sub, modifier = Modifier.size(22.dp)) }
                    Spacer(Modifier.height(3.dp))
                    Text(label, fontSize = 11.sp, color = if (sel) Z.text else Z.sub, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal)
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

/** A widget shown on a mini wallpaper. */
@Composable
fun PreviewTile(st: WidgetStyle, modifier: Modifier = Modifier, showName: Boolean = true, onClick: () -> Unit) {
    val small = st.shape == Shape.SMALL
    Column(modifier.clickable(onClick = onClick)) {
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Backdrops.brush(st.cat.ordinal % 5))
                .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(24.dp))
                .padding(if (small) 16.dp else 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            WidgetPreview(st.defaultConfig(), Modifier.fillMaxWidth(), density = 2f)
            Row(Modifier.align(Alignment.TopEnd), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (Flags.isNew(st)) NewBadge()
                if (Flags.isPro(st)) ProBadge()
            }
        }
        if (showName) {
            Row(Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(st.name(Prefs.isArabicUi), color = Z.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (Prefs.favorites.contains(st.id)) Icon(Icons.Rounded.Favorite, null, tint = LocalAccent.current, modifier = Modifier.size(13.dp))
            }
            Text(sizeLabel(st.shape), color = Z.sub, fontSize = 11.sp, modifier = Modifier.padding(start = 6.dp))
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
    val all = Flags.visible()
    val featured = remember(flags) {
        listOf("clock_arabesque", "prayer_spotlight", "date_calli_full", "media_player", "clock_wide", "digital_neon")
            .mapNotNull { id -> all.firstOrNull { it.id == id } }
    }
    val hour = LocalTime.now().hour
    val greet = when (hour) {
        in 5..11 -> tr("Good morning", "صباح الخير")
        in 12..16 -> tr("Good afternoon", "طاب يومك")
        else -> tr("Good evening", "مساء الخير")
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 120.dp)) {
        // ---- header
        item {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(greet, color = Z.sub, fontSize = 14.sp)
                    Text(tr("Make your screen yours", "اصنع شاشتك بنفسك"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                Row(
                    Modifier.clip(RoundedCornerShape(50))
                        .background(if (pro) Brush.linearGradient(listOf(Color(0xFF2A2210), Color(0xFF1A1508))) else Brush.linearGradient(listOf(Color(0xFFF6D98B), Color(0xFFC9963A))))
                        .clickable { nav.go(Screen.Paywall) }.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.WorkspacePremium, null, tint = if (pro) Z.pro else Color(0xFF2A1C05), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (pro) "PRO ✓" else tr("Go Pro", "اشترك"), color = if (pro) Z.pro else Color(0xFF2A1C05), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
        // ---- search field
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp).clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.06f)).border(1.dp, Z.line, RoundedCornerShape(18.dp))
                    .clickable { nav.go(Screen.Search) }.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Search, null, tint = Z.sub, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(tr("Search ${all.size} widgets", "ابحث في ${all.size} ويدجت"), color = Z.sub)
            }
        }
        if (flags.notice.isNotBlank()) item {
            Glass(Modifier.fillMaxWidth().padding(horizontal = 16.dp), radius = 18.dp, alpha = 0.08f) {
                Text(flags.notice, modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.bodyMedium)
            }
        }
        // ---- featured pager
        if (featured.isNotEmpty()) item {
            val pager = rememberPagerState { featured.size }
            LaunchedEffect(pager) {
                while (true) {
                    delay(4500)
                    pager.animateScrollToPage((pager.currentPage + 1) % featured.size)
                }
            }
            Column {
                HorizontalPager(pager, contentPadding = PaddingValues(horizontal = 16.dp), pageSpacing = 12.dp) { page ->
                    val st = featured[page]
                    Box(
                        Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(30.dp))
                            .background(Backdrops.brush(page + 1)).clickable { nav.go(Screen.Editor(st.id)) },
                    ) {
                        Box(
                            Modifier.fillMaxWidth().padding(
                                start = if (st.shape == Shape.SMALL) 90.dp else 24.dp,
                                end = if (st.shape == Shape.SMALL) 90.dp else 24.dp, top = 22.dp,
                            ),
                            contentAlignment = Alignment.TopCenter,
                        ) { WidgetPreview(st.defaultConfig(), Modifier.fillMaxWidth(), density = 2.4f) }
                        Row(
                            Modifier.align(Alignment.BottomStart).fillMaxWidth()
                                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xAA000000))))
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(tr("FEATURED", "مميز"), color = LocalAccent.current, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                                Text(st.name(Prefs.isArabicUi), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                            Box(Modifier.clip(RoundedCornerShape(50)).background(Color.White).padding(horizontal = 14.dp, vertical = 7.dp)) {
                                Text(tr("Customize", "خصّص"), color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.Center) {
                    repeat(featured.size) { i ->
                        val sel = i == pager.currentPage
                        Box(Modifier.padding(horizontal = 3.dp).height(6.dp).width(if (sel) 18.dp else 6.dp).clip(RoundedCornerShape(50))
                            .background(if (sel) LocalAccent.current else Color.White.copy(alpha = 0.2f)))
                    }
                }
            }
        }
        // ---- collections + categories
        item {
            SectionHeader(tr("Browse", "تصفح"), null, null)
            val chips = listOf("popular", "new", "free") + Cat.values().filter { c -> all.any { it.cat == c } }.map { it.name }
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(chips) { key ->
                    val list = collection(key)
                    val cat = Cat.values().firstOrNull { it.name == key }
                    val color = cat?.let { catColor(it) } ?: when (key) { "popular" -> Color(0xFFE7B75F); "new" -> Color(0xFF3DDC97); else -> Color(0xFF4DB5FF) }
                    val icon = cat?.let { catIcon(it) } ?: when (key) { "popular" -> Icons.Rounded.WorkspacePremium; "new" -> Icons.Rounded.AutoAwesome; else -> Icons.Rounded.Favorite }
                    Column(
                        Modifier.width(96.dp).clip(RoundedCornerShape(22.dp)).background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, Z.line, RoundedCornerShape(22.dp)).clickable { nav.go(Screen.Category(key)) }.padding(vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            Modifier.size(46.dp).clip(RoundedCornerShape(15.dp))
                                .background(Brush.linearGradient(listOf(color.copy(alpha = 0.9f), color.copy(alpha = 0.45f)))),
                            contentAlignment = Alignment.Center,
                        ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp)) }
                        Spacer(Modifier.height(8.dp))
                        Text(collectionTitle(key), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${list.size}", fontSize = 11.sp, color = Z.sub)
                    }
                }
            }
        }
        // ---- one carousel per category
        Cat.values().forEach { cat ->
            val list = all.filter { it.cat == cat }
            if (list.isEmpty()) return@forEach
            item(key = "row_${cat.name}") {
                SectionHeader(if (Prefs.isArabicUi) cat.ar else cat.en, list.size) { nav.go(Screen.Category(cat.name)) }
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(list.take(12), key = { it.id }) { st ->
                        PreviewTile(st, Modifier.width(if (st.shape == Shape.SMALL) 168.dp else 320.dp)) { nav.go(Screen.Editor(st.id)) }
                    }
                }
            }
        }
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
            PreviewTile(st) { onPick(st) }
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

/** Launcher "add widget" flow: choose a style that fits the placed widget size. */
@Composable
fun PickScreen(nav: Nav, s: Screen.Pick) {
    val list = Flags.visible().filter { it.shape == s.shape }
    Column(Modifier.fillMaxSize()) {
        TopBar(tr("Choose a widget", "اختر ويدجت"), null)
        StyleGrid(list) { st -> nav.go(Screen.Editor(st.id, s.widgetId, configure = true)) }
    }
}
