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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

@Composable
fun HomeScreen(nav: Nav) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val pro by Billing.pro.collectAsState()
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { nav.go(Screen.Settings) }) { Icon(Icons.Rounded.Settings, null, tint = Z.text) }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { Wordmark() }
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(if (pro) Z.pro.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.07f))
                        .clickable { nav.go(Screen.Paywall) }, contentAlignment = Alignment.Center
                ) { Icon(Icons.Rounded.WorkspacePremium, null, tint = Z.pro) }
            }
            when (tab) {
                0 -> WidgetsTab(nav)
                1 -> IconsTab()
                2 -> WallsTab(nav)
                else -> MyWidgetsTab(nav)
            }
        }
        BottomBar(tab, Modifier.align(Alignment.BottomCenter)) { tab = it }
    }
}

@Composable
private fun BottomBar(tab: Int, modifier: Modifier, onTab: (Int) -> Unit) {
    val accent = LocalAccent.current
    val items = listOf(
        Icons.Rounded.Widgets to tr("Widgets", "الويدجت"),
        Icons.Rounded.Apps to tr("Icons", "الأيقونات"),
        Icons.Rounded.Image to tr("Walls", "الخلفيات"),
        Icons.Rounded.Dashboard to tr("Mine", "ويدجتاتي"),
    )
    Row(
        modifier.navigationBarsPadding().padding(bottom = 12.dp).clip(RoundedCornerShape(30.dp))
            .background(Color(0xE6131318)).border(1.dp, Z.line, RoundedCornerShape(30.dp)).padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items.forEachIndexed { i, (icon, label) ->
            val sel = i == tab
            Column(
                Modifier.clip(RoundedCornerShape(24.dp)).background(if (sel) accent.copy(alpha = 0.16f) else Color.Transparent)
                    .clickable { onTab(i) }.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(icon, null, tint = if (sel) accent else Z.sub, modifier = Modifier.size(22.dp))
                Text(label, fontSize = 11.sp, color = if (sel) accent else Z.sub)
            }
        }
    }
}

private sealed class Filter(val en: String, val ar: String) {
    data object All : Filter("All", "الكل")
    data object Popular : Filter("Popular", "الأشهر")
    data object New : Filter("New", "الجديد")
    data object Favorites : Filter("Favorites", "المفضلة")
    data object Free : Filter("Free", "مجاني")
    data class C(val cat: Cat) : Filter(cat.en, cat.ar)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WidgetsTab(nav: Nav, shape: Shape? = null, onPick: ((WidgetStyle) -> Unit)? = null) {
    val filters = (if (shape != null) listOf<Filter>(Filter.All) else emptyList()) +
        listOf(Filter.Popular, Filter.New, Filter.Favorites, Filter.Free) + Cat.values().map { Filter.C(it) }
    var sel by rememberSaveable { mutableIntStateOf(0) }
    val flags by Flags.flags.collectAsState()
    val ver by Prefs.version.collectAsState()
    val all = Flags.visible().filter { shape == null || it.shape == shape }
    val f = filters[sel]
    val list = when (f) {
        Filter.All -> all
        Filter.Popular -> all.filter { it.popular } + all.filter { !it.popular && Flags.isNew(it) }
        Filter.New -> all.filter { Flags.isNew(it) }
        Filter.Favorites -> all.filter { Prefs.favorites.contains(it.id) }
        Filter.Free -> all.filter { !Flags.isPro(it) }
        is Filter.C -> all.filter { it.cat == f.cat }
    }
    touch(ver)

    Column(Modifier.fillMaxSize()) {
        if (flags.notice.isNotBlank()) {
            Glass(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp), radius = 16.dp, alpha = 0.08f) {
                Text(flags.notice, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
            }
        }
        LazyRow(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filters.size) { i -> ZChip(if (Prefs.isArabicUi) filters[i].ar else filters[i].en, i == sel) { sel = i } }
        }
        if (list.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                Text(
                    if (f == Filter.Favorites) tr("Tap ♥ in the editor to add favorites", "اضغط ♥ داخل المحرر لإضافة مفضلات") else tr("Nothing here yet", "لا يوجد شيء هنا بعد"),
                    color = Z.sub, textAlign = TextAlign.Center
                )
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = pagePadding,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(list, key = { it.id }, span = { GridItemSpan(if (it.shape == Shape.SMALL) 1 else 2) }) { st ->
                StyleCard(st) { if (onPick != null) onPick(st) else nav.go(Screen.Editor(st.id)) }
            }
        }
    }
}

@Composable
fun StyleCard(st: WidgetStyle, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth()) {
            WidgetPreview(st.defaultConfig(), Modifier.fillMaxWidth())
            Row(Modifier.align(Alignment.TopEnd).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (Flags.isNew(st)) { NewBadge(); Spacer(Modifier.width(4.dp)) }
                if (Flags.isPro(st)) ProBadge()
            }
            if (Prefs.favorites.contains(st.id)) Icon(Icons.Rounded.Favorite, null, tint = LocalAccent.current, modifier = Modifier.align(Alignment.TopStart).padding(10.dp).size(14.dp))
        }
        Text(st.name(Prefs.isArabicUi), color = Z.sub, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp, bottom = 6.dp))
    }
}

/** Launcher "add widget" flow: choose a style that fits the placed widget size. */
@Composable
fun PickScreen(nav: Nav, s: Screen.Pick) {
    Column(Modifier.fillMaxSize()) {
        TopBar(tr("Choose a widget", "اختر ويدجت"), null)
        WidgetsTab(nav, s.shape) { st -> nav.go(Screen.Editor(st.id, s.widgetId, configure = true)) }
    }
}

@Suppress("unused")
private val spacer = Modifier.height(1.dp)
