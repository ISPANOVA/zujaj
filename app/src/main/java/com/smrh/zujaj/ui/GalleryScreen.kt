package com.smrh.zujaj.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Publish
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smrh.zujaj.catalog.Catalog
import com.smrh.zujaj.catalog.Shape
import com.smrh.zujaj.data.Prefs
import com.smrh.zujaj.model.WidgetConfig
import com.smrh.zujaj.remote.Gallery
import kotlinx.coroutines.launch

/** Designs other people made, approved by the admin. Tap one to open it in the editor as your own copy. */
@Composable
fun GalleryScreen(nav: Nav) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var byLikes by rememberSaveable { mutableStateOf(true) }
    var items by remember { mutableStateOf<List<Gallery.Item>?>(null) }
    var failed by remember { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }
    LaunchedEffect(byLikes, reload) {
        failed = false; items = null
        runCatching { Gallery.approved(byLikes) }.onSuccess { items = it }.onFailure { failed = true }
    }
    val pane = LocalPaneWidth.current
    val gut = Sp.gutter(pane)
    Column(Modifier.fillMaxSize()) {
        TopBar(tr("Community designs", "تصاميم الناس"), { nav.back() })
        Segmented(listOf(tr("Most loved", "الأكتر حبًا"), tr("Newest", "الأحدث")), if (byLikes) 0 else 1, Modifier.padding(horizontal = gut, vertical = 4.dp)) { byLikes = it == 0 }
        // one snapshot per frame: switching tabs clears the list while the grid may still be drawing the old one
        val shown = items
        when {
            failed -> EmptyState(Icons.Rounded.CloudOff, tr("Couldn't load the gallery", "معرفناش نحمّل التصاميم"), tr("Check your connection and try again.", "اتأكد من النت وجرّب تاني."), tr("Try again", "جرّب تاني")) { reload++ }
            shown == null -> LazyVerticalGrid(GridCells.Adaptive(if (pane >= 600) 300.dp else 160.dp),
                contentPadding = PaddingValues(start = gut, end = gut, top = 8.dp), userScrollEnabled = false,
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(6) { Box(Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(Rad.l)).shimmer()) }
            }
            shown.isEmpty() -> EmptyState(Icons.Rounded.Groups, tr("No designs yet", "لسه مفيش تصاميم"),
                tr("Be the first: open any widget, make it yours, then Share → Publish to gallery.", "كن الأول: افتح أي ويدجت، ظبطها، وبعدين مشاركة ← انشر في المعرض."))
            else -> LazyVerticalGrid(GridCells.Adaptive(if (pane >= 600) 300.dp else 160.dp),
                contentPadding = PaddingValues(start = gut, end = gut, top = 8.dp, bottom = 40.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(shown, key = { it.id }, span = { val sh = Catalog.byId(it.styleId)?.shape; GridItemSpan(if (sh == Shape.SMALL) 1 else maxLineSpan) }) { g ->
                    val cfg = remember(g.code) { ShareDesign.decode(g.code) } ?: return@items
                    var liked by remember(g.id) { mutableStateOf(Gallery.liked(g.id)) }
                    var likes by remember(g.id) { mutableLongStateOf(g.likes) }
                    Column(Modifier.clip(RoundedCornerShape(Rad.l)).background(Z.surface).border(1.dp, Z.line, RoundedCornerShape(Rad.l))
                        .clickable { nav.go(Screen.Editor(cfg.styleId, shared = g.code)) }) {
                        Box(Modifier.fillMaxWidth().padding(8.dp).clip(RoundedCornerShape(Rad.m)).background(Backdrops.brush(g.id.hashCode())).padding(12.dp), contentAlignment = Alignment.Center) {
                            WidgetPreview(cfg, if (Catalog.byId(cfg.styleId)?.shape == Shape.SMALL) Modifier.fillMaxWidth(0.85f) else Modifier.fillMaxWidth(), density = 1.8f)
                        }
                        Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(g.title.ifBlank { Catalog.byId(cfg.styleId)?.name(Prefs.isArabicUi) ?: "" }, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                color = Z.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            Row(Modifier.clip(RoundedCornerShape(50)).clickable(enabled = !liked) {
                                liked = true; likes++; Haptics.tick(ctx)
                                scope.launch { runCatching { Gallery.like(g.id) }.onFailure { liked = false; likes-- } }
                            }.padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, tr("Like", "إعجاب"), tint = if (liked) Color(0xFFFF6B9A) else Z.sub, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("$likes", fontSize = 12.sp, color = Z.sub)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Editor → Share → "Publish to gallery": a title, then it goes to the admin for approval. */
@Composable
fun PublishDialog(cfg: WidgetConfig, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    ZDialog(Icons.Rounded.Publish, tr("Publish to the gallery", "انشر في المعرض"),
        tr("Your design (no photos, contacts or apps) is reviewed, then everyone can use it.", "تصميمك (من غير صور أو جهات اتصال أو تطبيقات) بيتراجع، وبعدها الكل يقدر يستخدمه."), onDismiss = onDone) {
        var missing by remember { mutableStateOf(false) }
        ZField(title, { title = it.take(40); missing = false }, tr("Your name or the design's name", "اسمك أو اسم التصميم"), Icons.Rounded.Publish, error = missing)
        if (missing) Text(tr("Write a name first", "اكتب اسم الأول"), color = Color(0xFFE5484D), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(16.dp))
        PrimaryButton(if (busy) tr("Sending…", "بنبعت…") else tr("Publish", "انشر"), Modifier.fillMaxWidth()) {
            if (busy) return@PrimaryButton
            if (title.trim().length < 2) { missing = true; return@PrimaryButton }
            if (!Gallery.canSubmit()) { Toast.makeText(ctx, tr("That's 3 today — try again tomorrow", "كده 3 النهارده — جرّب بكرة"), Toast.LENGTH_LONG).show(); return@PrimaryButton }
            busy = true
            scope.launch {
                val ok = runCatching { Gallery.submit(cfg, title) }.isSuccess
                busy = false
                Toast.makeText(ctx, if (ok) tr("Sent ✓ — it appears after review", "اتبعت ✓ — هيظهر بعد المراجعة") else tr("Couldn't send — check your connection", "معرفناش نبعت — اتأكد من النت"), Toast.LENGTH_LONG).show()
                if (ok) { Haptics.success(ctx); onDone() }
            }
        }
        TextButton(onClick = onDone) { Text(tr("Cancel", "إلغاء"), color = Z.sub) }
    }
}
