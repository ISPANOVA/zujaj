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
                .background(Brush.linearGradient(listOf(tint.copy(alpha = 0.16f), Color(0xFF111117), Color(0xFF0C0C11))))
                .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.02f))), RoundedCornerShape(24.dp))
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
@Composable
fun DiscoverTab(nav: Nav) {
    val pro by Billing.pro.collectAsState()
    val flags by Flags.flags.collectAsState()
    val ver by Prefs.version.collectAsState()
    touch(ver)
    val accent = LocalAccent.current
    val all = Flags.visible()
    var chip by rememberSaveable { mutableStateOf("all") }       // category / collection
    var tier by rememberSaveable { mutableIntStateOf(0) }        // 0 all · 1 free · 2 pro
    var size by rememberSaveable { mutableStateOf("") }          // "" or Shape name
    val hour = LocalTime.now().hour
    val greet = when (hour) {
        in 5..11 -> tr("Good morning", "صباح الخير")
        in 12..16 -> tr("Good afternoon", "طاب يومك")
        else -> tr("Good evening", "مساء الخير")
    }
    val cats = Cat.values().filter { c -> all.any { it.cat == c } }
    fun pass(st: WidgetStyle) = (tier == 0 || (tier == 1) == !Flags.isPro(st)) && (size.isEmpty() || st.shape.name == size)
    // "What's new": introduce Wallpapers once (after the first-run battery guide)
    var showNew by remember { mutableStateOf(false) }
    var showThemes by remember { mutableStateOf(false) }
    if (showThemes) ThemesDialog { showThemes = false }
    LaunchedEffect(Unit) { delay(2200); showNew = Prefs.getString("wall_intro") != "1" && Prefs.getString("bat_intro") == "1" }
    if (showNew) WallIntroDialog(onTry = { Prefs.setString("wall_intro", "1"); showNew = false; nav.go(Screen.Wallpapers) }) { Prefs.setString("wall_intro", "1"); showNew = false }
    var adSlot = 0
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 130.dp)) {
        // ---- header
        item(key = "head") {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 14.dp, top = 18.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(greet, color = Z.sub, fontSize = 13.sp)
                    Text(tr("Zujaj", "زجاج"), fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, softWrap = false)
                }
                RoundAction(Icons.Rounded.WorkspacePremium, if (pro) Color(0xFF2A2210) else Z.pro, if (pro) Z.pro else Color(0xFF2A1C05)) { nav.go(Screen.Paywall) }
            }
        }
        // ---- search pill
        item(key = "search") {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = 0.06f)).border(1.dp, Z.line, RoundedCornerShape(18.dp))
                .clickable { nav.go(Screen.Search) }.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Search, null, tint = Z.sub, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(tr("Search ${all.size} widgets", "ابحث في ${all.size} ويدجت"), color = Z.sub, fontSize = 14.sp)
            }
        }
        if (flags.notice.isNotBlank() && !flags.noticeOnce) item(key = "notice") {
            Glass(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), radius = 18.dp, alpha = 0.08f) {
                Text(flags.notice, modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.bodyMedium)
            }
        }
        // ---- bento: wallpapers + themes + pro
        item(key = "bento") {
            Row(Modifier.fillMaxWidth().height(164.dp).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BentoCard(Modifier.weight(1.15f).fillMaxHeight(), listOf(Color(0xFFF2A26B), Color(0xFF8E5BD6), Color(0xFF2B2F6B)),
                    Icons.Rounded.Wallpaper, tr("Wallpapers", "الخلفيات"), tr("Clock behind the photo", "الساعة ورا الصورة"), tr("NEW", "جديد")) { nav.go(Screen.Wallpapers) }
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    BentoCard(Modifier.fillMaxWidth().weight(1f), listOf(Color(0xFF13B5A8), Color(0xFF0E3B44)),
                        Icons.Rounded.Palette, tr("Themes", "ثيمات"), null, null, compact = true) { showThemes = true }
                    BentoCard(Modifier.fillMaxWidth().weight(1f), if (pro) listOf(Color(0xFF3A2C12), Color(0xFF17130B)) else listOf(Color(0xFFF6D98B), Color(0xFFC9963A)),
                        Icons.Rounded.WorkspacePremium, if (pro) tr("You're Pro", "أنت برو") else tr("Go Pro", "اشترك برو"), null, null, compact = true, dark = !pro) { nav.go(Screen.Paywall) }
                }
            }
        }
        val topSlot = adSlot++
        item(key = "ad_top") { com.smrh.zujaj.ads.AdBanner(topSlot) }
        // ---- filters: free / pro, size, category
        item(key = "filters") {
            Column {
                Segmented(listOf(tr("All", "الكل"), tr("Free", "مجاني"), "Pro"), tier, Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) { tier = it }
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val opts = listOf("" to tr("All sizes", "كل المقاسات")) + listOf(Shape.LARGE, Shape.WIDE, Shape.BANNER, Shape.SMALL).map { it.name to shapeGroupTitle(it) }
                    items(opts) { (k, l) -> FilterPill(l, size == k, null, accent) { size = k } }
                }
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val keys = listOf("all", "popular", "new") + cats.map { it.name }
                    items(keys) { key ->
                        val cat = Cat.values().firstOrNull { it.name == key }
                        val icon = cat?.let { catIcon(it) } ?: when (key) { "popular" -> Icons.Rounded.Whatshot; "new" -> Icons.Rounded.AutoAwesome; else -> Icons.Rounded.GridView }
                        FilterPill(if (key == "all") tr("All", "الكل") else collectionTitle(key), chip == key, icon, cat?.let { catColor(it) } ?: accent) { chip = key }
                    }
                }
            }
        }
        if (chip == "all") {
            // ---- one section per category: big ones together, small ones together, free before Pro
            var shown = 0
            cats.forEach { cat ->
                val list = tidy(all.filter { it.cat == cat && pass(it) })
                if (list.isEmpty()) return@forEach
                val big = list.filter { it.shape != Shape.SMALL }
                val small = list.filter { it.shape == Shape.SMALL }
                item(key = "sec_${cat.name}") {
                    Column(Modifier.animateItem()) {
                        SectionHeader(if (Prefs.isArabicUi) cat.ar else cat.en, list.size, { chip = cat.name }, catIcon(cat), catColor(cat))
                        if (big.isNotEmpty()) LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(big.take(10), key = { it.id }) { st -> PreviewTile(st, Modifier.width(292.dp), stage = 150.dp) { nav.go(Screen.Editor(st.id)) } }
                        }
                        if (big.isNotEmpty() && small.isNotEmpty()) Spacer(Modifier.height(12.dp))
                        if (small.isNotEmpty()) LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(small.take(12), key = { it.id }) { st -> PreviewTile(st, Modifier.width(150.dp), stage = 150.dp) { nav.go(Screen.Editor(st.id)) } }
                        }
                    }
                }
                shown++
                if (shown % 2 == 0) { val slot = adSlot++; item(key = "ad_c$shown") { com.smrh.zujaj.ads.AdBanner(slot) } }
            }
            if (shown == 0) item(key = "empty") { EmptyNote() }
        } else {
            // ---- one category / collection: grouped by size, free before Pro
            val list = tidy(collection(chip).filter { pass(it) })
            item(key = "title_$chip") {
                Text("${collectionTitle(chip)} · ${list.size}", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.animateItem().padding(start = 20.dp, end = 20.dp, top = 12.dp))
            }
            if (list.isEmpty()) item(key = "empty") { EmptyNote() }
            var rowsSinceAd = 0
            listOf(Shape.LARGE, Shape.WIDE, Shape.BANNER, Shape.SMALL).forEach { shape ->
                val group = list.filter { it.shape == shape }
                if (group.isEmpty()) return@forEach
                item(key = "g_${chip}_$shape") { GroupLabel("${shapeGroupTitle(shape)} · ${group.size}") }
                val rows = if (shape == Shape.SMALL) group.chunked(2) else group.map { listOf(it) }
                rows.forEachIndexed { i, row ->
                    item(key = "r_${chip}_${row.joinToString { it.id }}") {
                        Row(Modifier.animateItem().fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { st -> PreviewTile(st, Modifier.weight(1f)) { nav.go(Screen.Editor(st.id)) } }
                            if (row.size == 1 && shape == Shape.SMALL) Spacer(Modifier.weight(1f))
                        }
                    }
                    rowsSinceAd++
                    if (rowsSinceAd >= 4) { rowsSinceAd = 0; val slot = adSlot++; item(key = "ad_${chip}_${shape}_$i") { com.smrh.zujaj.ads.AdBanner(slot) } }
                }
            }
        }
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

@Composable
private fun BentoCard(modifier: Modifier, colors: List<Color>, icon: ImageVector, title: String, sub: String?, badge: String?, compact: Boolean = false, dark: Boolean = false, onClick: () -> Unit) {
    val src = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(if (pressed) 0.96f else 1f, androidx.compose.animation.core.spring(dampingRatio = 0.55f, stiffness = 500f), label = "bento")
    val fg = if (dark) Color(0xFF2A1C05) else Color.White
    Box(modifier.graphicsLayer { scaleX = scale; scaleY = scale }.clip(RoundedCornerShape(24.dp))
        .background(Brush.linearGradient(colors))
        .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(24.dp))
        .clickable(interactionSource = src, indication = androidx.compose.material3.ripple(), onClick = onClick)
        .padding(14.dp)) {
        if (compact) {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(fg.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = fg, modifier = Modifier.size(19.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(title, color = fg, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 2, lineHeight = 17.sp)
            }
        } else {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(fg.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = fg, modifier = Modifier.size(22.dp))
            }
            if (badge != null) Text(badge, color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopEnd).clip(RoundedCornerShape(50)).background(Color.White).padding(horizontal = 8.dp, vertical = 2.dp))
            Column(Modifier.align(Alignment.BottomStart)) {
                Text(title, color = fg, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, maxLines = 1)
                if (sub != null) Text(sub, color = fg.copy(alpha = 0.85f), fontSize = 12.sp, maxLines = 2, lineHeight = 15.sp)
            }
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
