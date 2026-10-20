package com.smrh.zujaj.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.rotate
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
                .background(Brush.linearGradient(listOf(tint.copy(alpha = 0.10f), Color(0xFF131318), Color(0xFF0C0C11))))
                .border(1.dp, Brush.linearGradient(listOf(Color(0xFFE2B85C).copy(alpha = 0.30f), Color.White.copy(alpha = 0.04f), Color(0xFFE2B85C).copy(alpha = 0.12f))), RoundedCornerShape(24.dp))
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
private val Gold = Color(0xFFE2B85C)
private val GoldSoft = Color(0xFFF3DDA6)
private val GoldDeep = Color(0xFF9C7230)

/** Faint eight-pointed-star lattice (khatam), drawn once and cached — the app's Arabic signature. */
@Composable
fun Ornament(modifier: Modifier, alpha: Float = 0.10f) {
    androidx.compose.foundation.Canvas(modifier) {
        val cell = 64.dp.toPx()
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
        val cols = (size.width / cell).toInt() + 2
        val rows = (size.height / cell).toInt() + 2
        val r = cell * 0.36f
        for (j in 0 until rows) for (i in 0 until cols) {
            val cx = i * cell + (if (j % 2 == 0) 0f else cell / 2); val cy = j * cell
            // fade towards the bottom
            val k = (1f - cy / size.height).coerceIn(0f, 1f)
            if (k <= 0f) continue
            val c = Gold.copy(alpha = alpha * k)
            val sq = androidx.compose.ui.geometry.Size(r * 1.4f, r * 1.4f)
            val tl = androidx.compose.ui.geometry.Offset(cx - r * 0.7f, cy - r * 0.7f)
            drawRect(c, tl, sq, style = stroke)
            rotate(45f, androidx.compose.ui.geometry.Offset(cx, cy)) { drawRect(c, tl, sq, style = stroke) }
            drawCircle(c, r * 0.18f, androidx.compose.ui.geometry.Offset(cx, cy), style = stroke)
        }
    }
}

/** Title of a home section: thin gold rule, name, and "See all". */
@Composable
private fun LuxHeader(title: String, sub: String?, onAll: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 30.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(3.dp).height(30.dp).clip(RoundedCornerShape(2.dp)).background(Brush.verticalGradient(listOf(GoldSoft, GoldDeep))))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            if (sub != null) Text(sub, color = Z.sub, fontSize = 12.sp, maxLines = 1)
        }
        if (onAll != null) {
            Row(Modifier.clip(RoundedCornerShape(50)).border(1.dp, Gold.copy(alpha = 0.35f), RoundedCornerShape(50)).clickable(onClick = onAll)
                .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(tr("See all", "عرض الكل"), color = GoldSoft, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Gold, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun DiscoverTab(nav: Nav) {
    val pro by Billing.pro.collectAsState()
    val flags by Flags.flags.collectAsState()
    val ver by Prefs.version.collectAsState()
    touch(ver)
    val ar = Prefs.isArabicUi
    val all = Flags.visible()
    val cats = Cat.values().filter { c -> all.any { it.cat == c } }
    val featured = remember(flags) {
        listOf("prayer_hub", "calli_lawn", "photo_verse", "event_card", "prayer_journey", "w_today_tomorrow")
            .mapNotNull { id -> all.firstOrNull { it.id == id } }
    }
    val today = remember { java.time.LocalDate.now() }
    val hijri = remember(today) { com.smrh.zujaj.data.Calendars.hijri(today) }
    var showNew by remember { mutableStateOf(false) }
    var showThemes by remember { mutableStateOf(false) }
    if (showThemes) ThemesDialog { showThemes = false }
    LaunchedEffect(Unit) { delay(2200); showNew = Prefs.getString("wall_intro") != "1" && Prefs.getString("bat_intro") == "1" }
    if (showNew) WallIntroDialog(onTry = { Prefs.setString("wall_intro", "1"); showNew = false; nav.go(Screen.Wallpapers) }) { Prefs.setString("wall_intro", "1"); showNew = false }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 130.dp)) {
        // ---------------------------------------------------------------- crest
        item(key = "crest") {
            Box(Modifier.fillMaxWidth()) {
                Ornament(Modifier.matchParentSize())
                Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent, Z.bg.copy(alpha = 0.0f), Z.bg))))
                Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 20.dp, bottom = 18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (ar) "${com.smrh.zujaj.data.Calendars.dayName(today, true)} · ${hijri.day} ${com.smrh.zujaj.data.Calendars.hijriMonth(hijri.month, true)}"
                                else "${com.smrh.zujaj.data.Calendars.dayName(today, false)} · ${hijri.day} ${com.smrh.zujaj.data.Calendars.hijriMonth(hijri.month, false)}",
                                color = Gold.copy(alpha = 0.9f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
                            )
                            Text(
                                if (ar) "زجاج" else "ZUJAJ",
                                style = androidx.compose.ui.text.TextStyle(
                                    brush = Brush.linearGradient(listOf(GoldSoft, Gold, GoldDeep)),
                                    fontFamily = if (ar) Z.kufi else Z.bodoni, fontSize = if (ar) 46.sp else 38.sp,
                                    letterSpacing = if (ar) 0.sp else 6.sp,
                                ),
                                maxLines = 1,
                            )
                            Text(tr("Widgets & wallpapers, made beautifully", "ويدجت وخلفيات بلمسة فنّية"), color = Z.sub, fontSize = 13.sp, maxLines = 1)
                        }
                        // Pro crest
                        Box(Modifier.size(50.dp).clip(CircleShape)
                            .background(if (pro) Brush.linearGradient(listOf(Color(0xFF2A2210), Color(0xFF15120A))) else Brush.linearGradient(listOf(GoldSoft, Gold, GoldDeep)))
                            .border(1.dp, Gold.copy(alpha = 0.6f), CircleShape).clickable { nav.go(Screen.Paywall) }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.WorkspacePremium, null, tint = if (pro) Gold else Color(0xFF2A1C05), modifier = Modifier.size(26.dp))
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    // search
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0xFF14141A))
                        .border(1.dp, Brush.horizontalGradient(listOf(Gold.copy(alpha = 0.35f), Color.White.copy(alpha = 0.06f))), RoundedCornerShape(18.dp))
                        .clickable { nav.go(Screen.Search) }.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Search, null, tint = Gold, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(tr("Search ${all.size} widgets", "ابحث في ${all.size} ويدجت"), color = Z.sub, fontSize = 14.sp)
                    }
                }
            }
        }
        if (flags.notice.isNotBlank() && !flags.noticeOnce) item(key = "notice") {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).clip(RoundedCornerShape(18.dp)).background(Gold.copy(alpha = 0.08f))
                .border(1.dp, Gold.copy(alpha = 0.25f), RoundedCornerShape(18.dp)).padding(14.dp)) {
                Text(flags.notice, style = MaterialTheme.typography.bodyMedium)
            }
        }
        // ---------------------------------------------------------------- two doors
        item(key = "doors") {
            Row(Modifier.fillMaxWidth().height(132.dp).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Door(Modifier.weight(1f).fillMaxHeight(), listOf(Color(0xFFE9A15B), Color(0xFF8E4FA8), Color(0xFF231B44)),
                    Icons.Rounded.Wallpaper, tr("Wallpapers", "الخلفيات"), tr("Clock behind your photo", "الساعة ورا صورتك"), tr("NEW", "جديد")) { nav.go(Screen.Wallpapers) }
                Door(Modifier.weight(1f).fillMaxHeight(), listOf(Color(0xFF1C6B66), Color(0xFF123B45), Color(0xFF0D1B24)),
                    Icons.Rounded.Palette, tr("Themes", "الثيمات"), tr("One look for all", "شكل واحد للكل"), null) { showThemes = true }
            }
        }
        // ---------------------------------------------------------------- categories as medallions
        item(key = "cats") {
            Column {
                LuxHeader(tr("Collections", "الأقسام"), null, null)
                LazyRow(contentPadding = PaddingValues(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val keys = listOf("popular", "new", "free") + cats.map { it.name }
                    items(keys, key = { it }) { k ->
                        val cat = Cat.values().firstOrNull { it.name == k }
                        val icon = cat?.let { catIcon(it) } ?: when (k) { "popular" -> Icons.Rounded.Whatshot; "new" -> Icons.Rounded.AutoAwesome; else -> Icons.Rounded.GridView }
                        Medallion(icon, collectionTitle(k)) { nav.go(Screen.Category(k)) }
                    }
                }
            }
        }
        // ---------------------------------------------------------------- featured
        if (featured.isNotEmpty()) item(key = "featured") {
            Column {
                LuxHeader(tr("Editor's picks", "مختارات زجاج"), tr("Our most loved designs", "أجمل التصميمات عندنا"), null)
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(featured, key = { it.id }) { st -> FeatureCard(st, Modifier.width(300.dp)) { nav.go(Screen.Editor(st.id)) } }
                }
            }
        }
        item(key = "ad0") { com.smrh.zujaj.ads.AdBanner(0, Modifier.padding(top = 18.dp)) }
        // ---------------------------------------------------------------- one calm row per category
        var n = 0
        cats.forEach { cat ->
            val list = tidy(all.filter { it.cat == cat })
            val big = list.filter { it.shape != Shape.SMALL }
            val small = list.filter { it.shape == Shape.SMALL }
            // a row is always one size: the big ones if there are any, else the small ones
            val row = if (big.size >= 2 || small.isEmpty()) big else small
            if (row.isEmpty()) return@forEach
            val isBig = row.first().shape != Shape.SMALL
            item(key = "cat_${cat.name}") {
                Column {
                    LuxHeader(if (ar) cat.ar else cat.en, tr("${list.size} widgets", "${list.size} ويدجت")) { nav.go(Screen.Category(cat.name)) }
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(row.take(8), key = { it.id }) { st ->
                            PreviewTile(st, Modifier.width(if (isBig) 280.dp else 156.dp), stage = if (isBig) 140.dp else 156.dp) { nav.go(Screen.Editor(st.id)) }
                        }
                    }
                }
            }
            n++
            if (n % 3 == 0) { val slot = n / 3; item(key = "ad_$n") { com.smrh.zujaj.ads.AdBanner(slot, Modifier.padding(top = 18.dp)) } }
        }
    }
}

/** Round gold-ringed icon with a label under it. */
@Composable
private fun Medallion(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(Modifier.width(78.dp).clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(58.dp).clip(CircleShape)
            .background(Brush.radialGradient(listOf(Color(0xFF2A2418), Color(0xFF121216))))
            .border(1.2.dp, Brush.linearGradient(listOf(GoldSoft, GoldDeep, Gold.copy(alpha = 0.3f))), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Gold, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(7.dp))
        Text(label, fontSize = 11.5.sp, color = Z.text.copy(alpha = 0.88f), maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

/** Big gradient entry card ("door") with a soft star pattern. */
@Composable
private fun Door(modifier: Modifier, colors: List<Color>, icon: ImageVector, title: String, sub: String, badge: String?, onClick: () -> Unit) {
    val src = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(if (pressed) 0.96f else 1f, androidx.compose.animation.core.spring(dampingRatio = 0.6f, stiffness = 500f), label = "door")
    Box(modifier.graphicsLayer { scaleX = scale; scaleY = scale }.clip(RoundedCornerShape(26.dp))
        .background(Brush.linearGradient(colors))
        .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.05f))), RoundedCornerShape(26.dp))
        .clickable(interactionSource = src, indication = androidx.compose.material3.ripple(), onClick = onClick)) {
        Ornament(Modifier.matchParentSize(), alpha = 0.16f)
        Box(Modifier.fillMaxSize().padding(14.dp)) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(Color.White.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            if (badge != null) Text(badge, color = Color(0xFF2A1C05), fontSize = 10.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopEnd).clip(RoundedCornerShape(50)).background(GoldSoft).padding(horizontal = 8.dp, vertical = 2.dp))
            Column(Modifier.align(Alignment.BottomStart)) {
                Text(title, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, maxLines = 1)
                Text(sub, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}

/** Featured widget on a rich backdrop with a gold frame. */
@Composable
private fun FeatureCard(st: WidgetStyle, modifier: Modifier, onClick: () -> Unit) {
    val i = st.id.hashCode()
    Column(modifier.clip(RoundedCornerShape(28.dp)).clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(28.dp)).background(Backdrops.brush(i))
            .border(1.dp, Brush.linearGradient(listOf(GoldSoft.copy(alpha = 0.8f), Gold.copy(alpha = 0.1f), GoldDeep.copy(alpha = 0.6f))), RoundedCornerShape(28.dp))
            .padding(horizontal = if (st.shape == Shape.SMALL) 70.dp else 18.dp, vertical = 18.dp), contentAlignment = Alignment.Center) {
            WidgetPreview(st.defaultConfig(), Modifier.fillMaxSize(), density = 2.2f)
        }
        Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(st.name(Prefs.isArabicUi), fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            Spacer(Modifier.width(6.dp))
            if (Flags.isPro(st)) ProBadge() else if (Flags.isNew(st)) NewBadge()
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
