package com.smrh.zujaj.ui

import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Redeem
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Add
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.rounded.AdminPanelSettings
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.FilterChip
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smrh.zujaj.catalog.Cat
import com.smrh.zujaj.catalog.Catalog
import com.smrh.zujaj.catalog.WidgetStyle
import com.smrh.zujaj.data.Prefs
import com.smrh.zujaj.remote.CodeUnit
import com.smrh.zujaj.remote.Codes
import com.smrh.zujaj.remote.Flags
import com.smrh.zujaj.remote.RedeemCode
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Admin dashboard: widgets (Pro / New / Hidden), redeem codes and general switches. */
@Composable
fun AdminScreen(nav: Nav) {
    val auth = Flags.auth()
    Column(Modifier.fillMaxSize()) {
        TopBar(tr("Control panel", "لوحة التحكم"), { nav.back() })
        if (auth == null) {
            Text(tr("Firebase isn't configured in this build.", "Firebase غير مُعد في هذه النسخة."), modifier = Modifier.padding(20.dp), color = Z.sub)
            return
        }
        if (!AdminSession.unlocked) { Text(tr("Locked", "مقفول"), modifier = Modifier.padding(20.dp)); return }
        var user by remember { mutableStateOf(auth.currentUser?.takeIf { AdminSession.isAdminEmail(it.email) }) }
        var error by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(Unit) {
            if (user != null) return@LaunchedEffect
            val r = runCatching {
                runCatching { auth.signInWithEmailAndPassword(AdminSession.email, AdminSession.pass).await() }
                    .recoverCatching {
                        // first time with email codes: move the account from the old password to the secret key
                        auth.signInWithEmailAndPassword(AdminSession.email, AdminSession.gatePass).await()
                        auth.currentUser!!.updatePassword(AdminSession.pass).await()
                    }
                    .recoverCatching { auth.createUserWithEmailAndPassword(AdminSession.email, AdminSession.pass).await() }
                    .getOrThrow()
            }
            r.onSuccess { user = auth.currentUser }.onFailure { error = it.message }
        }
        if (user == null) {
            Text(error ?: tr("Signing in…", "جارٍ تسجيل الدخول…"), modifier = Modifier.padding(20.dp), color = Z.sub)
            return
        }
        var tab by remember { mutableIntStateOf(0) }
        // header: who is signed in, and that changes go live
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF2A1F4D), Color(0xFF15151B))))
            .border(1.dp, Z.tint(0.1f), RoundedCornerShape(22.dp)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            IconTile(Icons.Rounded.AdminPanelSettings, Color(0xFF8B7CFF), 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(tr("Admin", "المسؤول"), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(user?.email ?: "", color = Z.sub, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(Modifier.clip(RoundedCornerShape(50)).background(Color(0xFF34C77B).copy(alpha = 0.15f)).padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF34C77B)))
                Spacer(Modifier.width(6.dp))
                Text(tr("Live", "مباشر"), color = Color(0xFF34C77B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        // sections of the panel, one per tab
        val tabs = listOf(
            Icons.Rounded.Widgets to tr("Widgets", "الويدجت"),
            Icons.Rounded.Star to tr("Editor's choice", "المختارات"),
            Icons.Rounded.Redeem to tr("Codes", "الأكواد"),
            Icons.Rounded.Campaign to tr("Messages", "الرسائل"),
            Icons.Rounded.MonetizationOn to tr("Ads", "الإعلانات"),
            Icons.Rounded.Security to tr("Security", "الحماية"),
        )
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tabs.size) { i ->
                val (ic, l) = tabs[i]
                val sel = tab == i
                val bg by androidx.compose.animation.animateColorAsState(if (sel) LocalAccent.current else Z.tint(0.06f), label = "at")
                Row(Modifier.clip(RoundedCornerShape(16.dp)).background(bg).border(1.dp, if (sel) Color.Transparent else Z.line, RoundedCornerShape(16.dp))
                    .clickable { tab = i }.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(ic, null, tint = if (sel) Color(0xFF14110A) else Z.sub, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(l, color = if (sel) Color(0xFF14110A) else Z.text, fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium, fontSize = 13.sp)
                }
            }
        }
        androidx.compose.animation.AnimatedContent(tab, transitionSpec = {
            (androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(220)) + androidx.compose.animation.slideInVertically { it / 12 })
                .togetherWith(androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(120)))
        }, label = "admin") { t ->
            when (t) {
                0 -> WidgetsAdmin()
                1 -> PicksAdmin()
                2 -> CodesAdmin()
                3 -> GeneralAdmin(user?.email ?: "", 0)
                4 -> GeneralAdmin(user?.email ?: "", 1)
                else -> GeneralAdmin(user?.email ?: "", 2)
            }
        }
    }
}

// =============================================================================== widgets
@Composable
private fun WidgetsAdmin() {
    val ctx = LocalContext.current
    val scope = AdminWork
    val flags by Flags.flags.collectAsState()
    var pro by remember(flags) { mutableStateOf((flags.pro ?: Catalog.all.filter { it.pro }.map { it.id }).toSet()) }
    var fresh by remember(flags) { mutableStateOf((flags.fresh ?: Catalog.all.filter { it.isNew }.map { it.id }).toSet()) }
    var hidden by remember(flags) { mutableStateOf(flags.hidden.toSet()) }
    var q by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf<Cat?>(null) }
    var filter by remember { mutableIntStateOf(0) } // 0 all, 1 pro, 2 free, 3 new, 4 hidden
    val origPro = (flags.pro ?: Catalog.all.filter { it.pro }.map { it.id }).toSet()
    val origFresh = (flags.fresh ?: Catalog.all.filter { it.isNew }.map { it.id }).toSet()
    val changes = (pro - origPro).size + (origPro - pro).size + (fresh - origFresh).size + (origFresh - fresh).size +
        (hidden - flags.hidden.toSet()).size + (flags.hidden.toSet() - hidden).size
    val list = Catalog.all.filter { st ->
        (cat == null || st.cat == cat) &&
            (q.isBlank() || st.en.contains(q, true) || st.ar.contains(q) || st.id.contains(q, true)) &&
            when (filter) { 1 -> st.id in pro; 2 -> st.id !in pro; 3 -> st.id in fresh; 4 -> st.id in hidden; else -> true }
    }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 110.dp)) {
            // stats
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Stat(tr("Total", "الكل"), Catalog.all.size, Z.text, Modifier.weight(1f))
                    Stat("Pro", pro.size, Z.pro, Modifier.weight(1f))
                    Stat(tr("New", "جديد"), fresh.size, Color(0xFF3DDC97), Modifier.weight(1f))
                    Stat(tr("Hidden", "مخفي"), hidden.size, Color(0xFFE5484D), Modifier.weight(1f))
                }
            }
            // search
            item {
                OutlinedTextField(
                    q, { q = it }, singleLine = true, placeholder = { Text(tr("Search widgets", "ابحث عن ويدجت")) },
                    leadingIcon = { Icon(Icons.Rounded.Search, null) }, shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LocalAccent.current, unfocusedBorderColor = Z.line),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
            // categories
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { ZChip(tr("All", "الكل") + " · ${Catalog.all.size}", cat == null) { cat = null } }
                    items(Cat.values().filter { c -> Catalog.all.any { it.cat == c } }) { c ->
                        ZChip((if (Prefs.isArabicUi) c.ar else c.en) + " · ${Catalog.all.count { it.cat == c }}", cat == c) { cat = c }
                    }
                }
            }
            // status filter
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val opts = listOf(tr("Any", "أي حالة"), "Pro", tr("Free", "مجاني"), tr("New", "جديد"), tr("Hidden", "مخفي"))
                    items(opts.size) { i -> ZChip(opts[i], filter == i) { filter = i } }
                }
            }
            // bulk actions for what's on screen
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("${list.size} shown", "${list.size} ظاهرة"), color = Z.sub, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    SmallAction(tr("All Pro", "الكل برو"), Z.pro) { pro = pro + list.map { it.id } }
                    SmallAction(tr("All free", "الكل مجاني"), Z.sub) { pro = pro - list.map { it.id }.toSet() }
                }
            }
            items(list, key = { it.id }) { st ->
                WidgetAdminRow(
                    st, st.id in pro, st.id in fresh, st.id in hidden,
                    onPro = { pro = if (it) pro + st.id else pro - st.id },
                    onNew = { fresh = if (it) fresh + st.id else fresh - st.id },
                    onHide = { hidden = if (it) hidden + st.id else hidden - st.id },
                )
            }
        }
        // publish bar
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Z.bg.copy(alpha = 0.95f), Z.bg)))
                .navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 12.dp)
        ) {
            PrimaryButton(
                if (changes > 0) tr("Publish $changes changes", "نشر $changes تعديل") else tr("Published · live", "منشور · مباشر"),
                Modifier.fillMaxWidth(),
            ) {
                scope.launch {
                    val r = runCatching { Flags.save(flags.copy(pro = pro.toList(), fresh = fresh.toList(), hidden = hidden.toList())) }
                    Toast.makeText(ctx, if (r.isSuccess) tr("Published ✓ — live for everyone", "تم النشر ✓ — ظاهر للجميع الآن") else adminError(r.exceptionOrNull()), Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, n: Int, color: Color, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(Z.tint(0.05f)).border(1.dp, Z.line, RoundedCornerShape(16.dp)).padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("$n", color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Z.sub, fontSize = 11.sp)
    }
}

@Composable
private fun SmallAction(text: String, color: Color, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(50)).border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(50)).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 6.dp)) {
        Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Toggle(text: String, on: Boolean, color: Color, onChange: (Boolean) -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(50))
            .background(if (on) color.copy(alpha = 0.2f) else Color.Transparent)
            .border(1.dp, if (on) color else Z.line, RoundedCornerShape(50))
            .clickable { onChange(!on) }.padding(horizontal = 11.dp, vertical = 6.dp),
    ) { Text(text, color = if (on) color else Z.sub, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun WidgetAdminRow(st: WidgetStyle, isPro: Boolean, isNew: Boolean, isHidden: Boolean, onPro: (Boolean) -> Unit, onNew: (Boolean) -> Unit, onHide: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp).clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = if (isHidden) 0.02f else 0.05f)).border(1.dp, Z.line, RoundedCornerShape(18.dp)).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(78.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF101015)).padding(5.dp), contentAlignment = Alignment.Center) {
            WidgetPreview(st.defaultConfig(), Modifier.fillMaxWidth(), density = 1f)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(st.name(Prefs.isArabicUi), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                color = if (isHidden) Z.sub else Z.text)
            Text("${if (Prefs.isArabicUi) st.cat.ar else st.cat.en} · ${sizeLabel(st.shape)}", fontSize = 11.sp, color = Z.sub, maxLines = 1)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Toggle("PRO", isPro, Z.pro, onPro)
                Toggle(tr("NEW", "جديد"), isNew, Color(0xFF3DDC97), onNew)
                Toggle(tr("HIDE", "مخفي"), isHidden, Color(0xFFE5484D), onHide)
            }
        }
    }
}

// =============================================================================== codes
@Composable
private fun CodesAdmin() {
    var usersOf by remember { mutableStateOf<String?>(null) }
    val ctx = LocalContext.current
    val scope = AdminWork
    val ar = Prefs.isArabicUi
    var codes by remember { mutableStateOf<List<RedeemCode>?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    LaunchedEffect(reload) {
        runCatching { Codes.list() }.onSuccess { codes = it; err = null }.onFailure { err = it.message }
    }
    // form
    var code by remember { mutableStateOf(Codes.randomCode()) }
    var amount by remember { mutableIntStateOf(1) }
    var unit by remember { mutableStateOf(CodeUnit.MONTHS) }
    var unlimited by remember { mutableStateOf(false) }
    var people by remember { mutableIntStateOf(1) }
    var deadline by remember { mutableIntStateOf(0) } // days the code stays redeemable, 0 = no deadline
    var note by remember { mutableStateOf("") }
    var grants by remember { mutableStateOf("pro") }
    var busy by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<String?>(null) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 60.dp)) {
        item {
            Section(tr("New code", "كود جديد")) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        code, { code = Codes.normalize(it) }, singleLine = true, label = { Text(tr("Code", "الكود")) },
                        textStyle = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace, letterSpacing = 1.sp),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { code = Codes.randomCode() }) { Icon(Icons.Rounded.Casino, null, tint = LocalAccent.current) }
                }
                Divider()
                Text(tr("Unlocks", "الكود بيفتح"), color = Z.sub, fontSize = 12.sp, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("pro" to tr("Pro (widgets)", "برو (الويدجت)"), "wall" to tr("Wallpapers", "الخلفيات"), "both" to tr("Pro + Wallpapers", "برو + الخلفيات"))) { (k, l) ->
                        ZChip(l, grants == k) { grants = k }
                    }
                }
                Divider()
                Text(tr("Pro duration", "مدة البرو"), color = Z.sub, fontSize = 12.sp, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CodeUnit.values().toList()) { u -> ZChip(if (ar) u.ar else u.en, unit == u) { unit = u } }
                }
                if (unit != CodeUnit.FOREVER) {
                    Counter(tr("How many", "العدد"), amount, 1, 120) { amount = it }
                }
                Divider()
                SwitchRow(tr("Unlimited people", "عدد غير محدود من الأشخاص"), unlimited) { unlimited = it }
                if (!unlimited) Counter(tr("People who can use it", "عدد الأشخاص المسموح لهم"), people, 1, 100000) { people = it }
                Text(tr("Each phone can use a code only once.", "كل موبايل يقدر يستخدم الكود مرة واحدة بس."), color = Z.sub, fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                Divider()
                Text(tr("Code can be redeemed until", "الكود صالح للاستخدام لحد"), color = Z.sub, fontSize = 12.sp, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val opts = listOf(0 to tr("No deadline", "بدون موعد"), 1 to tr("1 day", "يوم"), 7 to tr("1 week", "أسبوع"), 30 to tr("1 month", "شهر"), 90 to tr("3 months", "٣ شهور"), 365 to tr("1 year", "سنة"))
                    items(opts) { (d, label) -> ZChip(label, deadline == d) { deadline = d } }
                }
                Divider()
                OutlinedTextField(note, { note = it }, singleLine = true, label = { Text(tr("Note (who is it for?)", "ملاحظة (الكود لمين؟)")) },
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(12.dp))
                val summary = (if (unit == CodeUnit.FOREVER) tr("Pro forever", "برو للأبد") else tr("Pro for $amount ${unit.en.lowercase()}", "برو لمدة $amount ${unit.ar}")) +
                    " · " + (if (unlimited) tr("unlimited people", "عدد غير محدود") else tr("$people people", "$people شخص"))
                Text(summary, color = LocalAccent.current, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 16.dp))
                PrimaryButton(if (busy) "…" else tr("Create code", "إنشاء الكود"), Modifier.fillMaxWidth().padding(12.dp), icon = Icons.Rounded.Add) {
                    if (busy) return@PrimaryButton
                    val c = Codes.normalize(code)
                    if (c.length < 4) { Toast.makeText(ctx, tr("Code is too short", "الكود قصير"), Toast.LENGTH_SHORT).show(); return@PrimaryButton }
                    busy = true
                    scope.launch {
                        val r = runCatching {
                            if (Codes.exists(c)) error(tr("This code already exists", "الكود ده موجود قبل كده"))
                            Codes.create(RedeemCode(c, amount, unit, if (unlimited) 0 else people.toLong(), 0, true,
                                if (deadline == 0) 0 else System.currentTimeMillis() + deadline * 86_400_000L, note.trim(), 0, grants))
                        }
                        busy = false
                        if (r.isSuccess) {
                            copy(ctx, c)
                            Toast.makeText(ctx, tr("Created & copied: $c", "اتعمل واتنسخ: $c"), Toast.LENGTH_LONG).show()
                            code = Codes.randomCode(); note = ""; reload++
                        } else Toast.makeText(ctx, adminError(r.exceptionOrNull()), Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(start = 22.dp, end = 10.dp, top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Codes", "الأكواد") + (codes?.let { " · ${it.size}" } ?: ""), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = { reload++ }) { Icon(Icons.Rounded.Refresh, null, tint = Z.sub) }
            }
            if (err != null) Text(err ?: "", color = Color(0xFFE5484D), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 22.dp))
            if (codes == null && err == null) Text(tr("Loading…", "جارٍ التحميل…"), color = Z.sub, modifier = Modifier.padding(22.dp))
            if (codes?.isEmpty() == true) Text(tr("No codes yet", "مفيش أكواد لسه"), color = Z.sub, modifier = Modifier.padding(22.dp))
        }
        items(codes ?: emptyList(), key = { it.code }) { c ->
            CodeCard(c,
                onCopy = { copy(ctx, c.code); Toast.makeText(ctx, tr("Copied", "اتنسخ"), Toast.LENGTH_SHORT).show() },
                onActive = { a -> scope.launch { runCatching { Codes.setActive(c.code, a) }; reload++ } },
                onDelete = { confirmDelete = c.code },
                onUsers = { usersOf = c.code },
            )
        }
    }
    usersOf?.let { code -> CodeUsersDialog(code, { usersOf = null; reload++ }) }
    confirmDelete?.let { c ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text(tr("Delete $c?", "حذف $c؟")) },
            text = { Text(tr("People who already redeemed it keep their Pro time. To take Pro away, use Users → Revoke everyone first.", "اللي استخدموه قبل كده هيفضل معاهم البرو لحد مدته. لو عايز تشيله منهم، ادخل على «المستخدمين» ← «إلغاء للكل» الأول.")) },
            confirmButton = { TextButton(onClick = { confirmDelete = null; scope.launch { runCatching { Codes.delete(c) }; reload++ } }) { Text(tr("Delete", "حذف"), color = Color(0xFFE5484D)) } },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text(tr("Cancel", "إلغاء")) } },
            containerColor = Z.surface,
        )
    }
}

@Composable
private fun Counter(label: String, value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        RoundBtn(Icons.Rounded.Remove) { onChange((value - 1).coerceAtLeast(min)) }
        OutlinedTextField(
            value.toString(), { t -> t.filter { it.isDigit() }.take(6).toIntOrNull()?.let { onChange(it.coerceIn(min, max)) } },
            singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center),
            shape = RoundedCornerShape(12.dp), modifier = Modifier.width(92.dp).padding(horizontal = 6.dp),
        )
        RoundBtn(Icons.Rounded.Add) { onChange((value + 1).coerceAtMost(max)) }
    }
}

@Composable
private fun RoundBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(Modifier.size(36.dp).clip(CircleShape).background(Z.tint(0.08f)).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = Z.text, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun CodeCard(c: RedeemCode, onCopy: () -> Unit, onActive: (Boolean) -> Unit, onDelete: () -> Unit, onUsers: () -> Unit) {
    val ar = Prefs.isArabicUi
    val expired = c.expiresAt in 1 until System.currentTimeMillis()
    val full = c.maxUses > 0 && c.uses >= c.maxUses
    val (status, sc) = when {
        !c.active -> tr("Off", "متوقف") to Z.sub
        expired -> tr("Expired", "انتهى") to Color(0xFFE5484D)
        full -> tr("Used up", "اكتمل") to Color(0xFFFF8A4C)
        else -> tr("Active", "شغال") to Color(0xFF3DDC97)
    }
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).clip(RoundedCornerShape(20.dp))
            .background(Z.tint(0.05f)).border(1.dp, Z.line, RoundedCornerShape(20.dp)).padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(c.code, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f))
            Box(Modifier.clip(RoundedCornerShape(50)).background(sc.copy(alpha = 0.16f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                Text(status, color = sc, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (c.unit == CodeUnit.FOREVER) { Icon(Icons.Rounded.AllInclusive, null, tint = Z.pro, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)) }
            Text("${c.grantsLabel(ar).uppercase()} · ${c.durationLabel(ar)}", color = Z.pro, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(10.dp))
            Text(
                if (c.maxUses == 0L) tr("${c.uses} used · unlimited", "${c.uses} استخدام · غير محدود") else tr("${c.uses} / ${c.maxUses} used", "${c.uses} من ${c.maxUses} استخدموه"),
                color = Z.sub, fontSize = 12.sp,
            )
        }
        if (c.maxUses > 0) {
            Box(Modifier.padding(top = 8.dp).fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)).background(Z.tint(0.08f))) {
                Box(Modifier.fillMaxWidth((c.uses.toFloat() / c.maxUses).coerceIn(0f, 1f)).height(5.dp).clip(RoundedCornerShape(50)).background(LocalAccent.current))
            }
        }
        val df = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
        val meta = listOfNotNull(
            c.note.takeIf { it.isNotBlank() },
            if (c.expiresAt > 0) tr("redeem by ", "صالح لحد ") + df.format(Date(c.expiresAt)) else null,
            if (c.createdAt > 0) tr("created ", "اتعمل ") + df.format(Date(c.createdAt)) else null,
        ).joinToString(" · ")
        if (meta.isNotBlank()) Text(meta, color = Z.sub, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
        Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Switch(c.active, onActive, colors = SwitchDefaults.colors(checkedTrackColor = LocalAccent.current))
            Spacer(Modifier.width(8.dp))
            Text(tr("Enabled", "مفعّل"), color = Z.sub, fontSize = 12.sp, modifier = Modifier.weight(1f))
            TextButton(onClick = onUsers) {
                Icon(Icons.Rounded.Groups, null, tint = LocalAccent.current, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp)); Text(tr("Users", "المستخدمين"), color = LocalAccent.current, fontSize = 13.sp)
            }
            IconButton(onClick = onCopy) { Icon(Icons.Rounded.ContentCopy, null, tint = Z.text) }
            IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, null, tint = Color(0xFFE5484D)) }
        }
    }
}

/** Phones that used a code: see when, until when, and take Pro away (or give it back). */
@Composable
private fun CodeUsersDialog(code: String, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val scope = AdminWork
    var list by remember { mutableStateOf<List<Codes.Redemption>?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var confirmAll by remember { mutableStateOf(false) }
    LaunchedEffect(reload) { runCatching { Codes.redemptions(code) }.onSuccess { list = it; err = null }.onFailure { err = it.message } }
    val df = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
    fun act(block: suspend () -> Unit) { busy = true; scope.launch { val r = runCatching { block() }; busy = false
        if (r.isFailure) Toast.makeText(ctx, adminError(r.exceptionOrNull()), Toast.LENGTH_LONG).show(); reload++ } }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxWidth(0.94f).fillMaxHeight(0.8f).clip(RoundedCornerShape(26.dp)).background(Z.surface).padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(tr("Users of", "مستخدمين") + " " + code, style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Monospace)
                    list?.let { l -> Text(tr("${l.count { !it.revoked }} active · ${l.count { it.revoked }} revoked", "${l.count { !it.revoked }} شغال · ${l.count { it.revoked }} ملغي"), color = Z.sub, fontSize = 12.sp) }
                }
                IconButton(onClick = { reload++ }) { Icon(Icons.Rounded.Refresh, null, tint = Z.sub) }
            }
            Spacer(Modifier.height(8.dp))
            if (err != null) Text(err ?: "", color = Color(0xFFE5484D), fontSize = 12.sp)
            if (list == null && err == null) Text(tr("Loading…", "جارٍ التحميل…"), color = Z.sub)
            if (list?.isEmpty() == true) Text(tr("Nobody used this code yet", "محدش استخدم الكود ده لسه"), color = Z.sub, modifier = Modifier.padding(vertical = 20.dp))
            androidx.compose.foundation.lazy.LazyColumn(Modifier.weight(1f)) {
                items(list ?: emptyList(), key = { it.device }) { r ->
                    val ended = !r.revoked && r.until != Long.MAX_VALUE && r.until < System.currentTimeMillis()
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(16.dp)).background(Z.tint(0.05f)).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(r.model, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(tr("used ", "استخدمه ") + (if (r.at > 0) df.format(Date(r.at)) else "—"), color = Z.sub, fontSize = 11.sp)
                            Text(
                                when {
                                    r.revoked -> tr("Revoked", "ملغي")
                                    r.until == Long.MAX_VALUE -> tr("Pro forever", "برو للأبد")
                                    ended -> tr("Ended ", "انتهى ") + df.format(Date(r.until))
                                    else -> tr("Pro until ", "برو لحد ") + df.format(Date(r.until))
                                },
                                color = when { r.revoked -> Color(0xFFE5484D); ended -> Z.sub; else -> Color(0xFF3DDC97) }, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            )
                        }
                        if (r.revoked) TextButton(enabled = !busy, onClick = { act { Codes.restore(code, r) } }) { Text(tr("Restore", "رجّعه"), color = Color(0xFF3DDC97)) }
                        else if (!ended) TextButton(enabled = !busy, onClick = { act { Codes.revoke(code, r) } }) { Text(tr("Revoke", "إلغاء"), color = Color(0xFFE5484D)) }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if ((list?.count { !it.revoked } ?: 0) > 0)
                    TextButton(enabled = !busy, onClick = { confirmAll = true }) { Text(tr("Revoke everyone", "إلغاء للكل"), color = Color(0xFFE5484D), fontWeight = FontWeight.Bold) }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text(tr("Close", "قفل"), color = Z.sub) }
            }
        }
    }
    if (confirmAll) AlertDialog(
        onDismissRequest = { confirmAll = false },
        title = { Text(tr("Revoke $code for everyone?", "إلغاء $code للكل؟")) },
        text = { Text(tr("Everyone who used it loses Pro and the code is turned off. You can restore people one by one later.",
            "كل اللي استخدموه هيتشال منهم البرو والكود هيتقفل. تقدر ترجّع أي حد بعدين واحد واحد.")) },
        confirmButton = { TextButton(onClick = { confirmAll = false; act { Codes.revokeAll(code) } }) { Text(tr("Revoke all", "إلغاء للكل"), color = Color(0xFFE5484D)) } },
        dismissButton = { TextButton(onClick = { confirmAll = false }) { Text(tr("Cancel", "إلغاء")) } },
        containerColor = Z.surface,
    )
}

private fun copy(ctx: Context, s: String) {
    (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("code", s))
}

// =============================================================================== general
@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
private fun GeneralAdmin(email: String, page: Int) {
    val ctx = LocalContext.current
    val scope = AdminWork
    val flags by Flags.flags.collectAsState()
    var freeAll by remember(flags) { mutableStateOf(flags.freeAll) }
    var notice by remember(flags) { mutableStateOf(flags.notice) }
    var certs by remember(flags) { mutableStateOf(flags.certs.joinToString("\n")) }
    var otp by remember(flags) { mutableStateOf(flags.otpUrl) }
    var once by remember(flags) { mutableStateOf(flags.noticeOnce) }
    var latest by remember(flags) { mutableStateOf(if (flags.latestCode > 0) flags.latestCode.toString() else "") }
    var force by remember(flags) { mutableStateOf(flags.forceUpdate) }
    var adsOn by remember(flags) { mutableStateOf(flags.adsOn) }
    var adBanner by remember(flags) { mutableStateOf(flags.adBanner) }
    var adReward by remember(flags) { mutableStateOf(flags.adReward) }
    var adPer by remember(flags) { mutableStateOf(flags.adWallPerDay.toString()) }
    var adLock by remember(flags) { mutableStateOf(flags.adblockLock) }
    var unAds by remember(flags) { mutableStateOf(flags.adUnlockAds.toString()) }
    var unDays by remember(flags) { mutableStateOf(flags.adUnlockDays.toString()) }
    var upNote by remember(flags) { mutableStateOf(flags.updateNote) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        if (page == 0) Section(tr("Promotion", "العروض")) {
            SwitchRow(tr("Everything free", "كل حاجة مجاني"), freeAll, tr("Every Pro widget unlocked for all users", "كل ويدجت البرو مفتوحة لكل المستخدمين")) { freeAll = it }
        }
        if (page == 0) Section(tr("Message to all users", "رسالة لكل المستخدمين")) {
            OutlinedTextField(notice, { notice = it }, placeholder = { Text(tr("Shown on the home page (leave empty to hide)", "بتظهر في الصفحة الرئيسية (سيبها فاضية عشان تختفي)")) },
                shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(12.dp))
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(false to tr("Always on home", "دايماً في الرئيسية"), true to tr("Pop-up once", "تظهر مرة واحدة بس")).forEach { (v, l) ->
                    FilterChip(once == v, { once = v }, label = { Text(l) })
                }
            }
        }
        if (page == 0) Section(tr("App update", "تحديث التطبيق")) {
            Text(tr("Latest version code on Google Play. Phones with an older copy get asked to update. This phone: ${com.smrh.zujaj.BuildConfig.VERSION_CODE}",
                "رقم آخر إصدار (versionCode) على جوجل بلاي. اللي عنده نسخة أقدم هيطلع له طلب تحديث. التليفون ده: ${com.smrh.zujaj.BuildConfig.VERSION_CODE}"),
                color = Z.sub, fontSize = 12.sp, modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp))
            OutlinedTextField(latest, { latest = it.filter { c -> c.isDigit() }.take(7) }, singleLine = true, placeholder = { Text(tr("e.g. 21 (empty = off)", "مثلاً ٢١ (فاضي = مقفول)")) },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(12.dp))
            SwitchRow(tr("Required update", "تحديث إجباري"), force, tr("Old copies can't be used until they update", "النسخ القديمة متشتغلش غير لما يحدّثوا")) { force = it }
            OutlinedTextField(upNote, { upNote = it }, placeholder = { Text(tr("What's new (optional)", "إيه الجديد (اختياري)")) },
                shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(12.dp))
        }
        if (page == 1) Section(tr("Ads", "الإعلانات")) {
            SwitchRow(tr("Show ads to free users", "إعلانات للمستخدمين المجانيين"), adsOn, tr("Pro & wallpaper subscribers never see ads", "المشتركين مبيشوفوش إعلانات")) { adsOn = it }
            SwitchRow(tr("Lock the app for ad blockers", "اقفل التطبيق على اللي بيمنع الإعلانات"), adLock,
                tr("AdGuard, private DNS, Blokada… must be turned off (subscribers aren't affected)", "AdGuard أو DNS خاص أو Blokada… لازم يتقفلوا (المشتركين مش متأثرين)")) { adLock = it }
            OutlinedTextField(adBanner, { adBanner = it.trim() }, singleLine = true, label = { Text(tr("Banner ad unit ID", "كود البانر (Ad unit ID)")) }, placeholder = { Text("ca-app-pub-…/…") },
                shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp))
            OutlinedTextField(adReward, { adReward = it.trim() }, singleLine = true, label = { Text(tr("Rewarded ad unit ID", "كود إعلان المكافأة (Rewarded)")) }, placeholder = { Text("ca-app-pub-…/…") },
                shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp))
            OutlinedTextField(adPer, { adPer = it.filter { c -> c.isDigit() }.take(2) }, singleLine = true, label = { Text(tr("Free wallpaper uses per day (ads)", "عدد مرات الخلفيات بالإعلانات في اليوم")) },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(12.dp))
        }
        if (page == 1) Section(tr("Unlock a widget with ads", "فتح ويدجت بالإعلانات")) {
            Text(tr("Free users watch this many ads to open ONE Pro widget (the one they watched for) for the days below. 0 ads = off.",
                "المستخدم المجاني يشوف العدد ده من الإعلانات ويفتح ويدجت برو واحدة بس (اللي شاف لها الإعلانات) للمدة اللي تحت. ٠ إعلانات = مقفول."),
                color = Z.sub, fontSize = 12.sp, modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp))
            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(unAds, { unAds = it.filter { c -> c.isDigit() }.take(2) }, singleLine = true, label = { Text(tr("Ads needed", "عدد الإعلانات")) },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f))
                OutlinedTextField(unDays, { unDays = it.filter { c -> c.isDigit() }.take(2) }, singleLine = true, label = { Text(tr("Days open", "مدة الفتح (أيام)")) },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f))
            }
        }
        if (page == 2) Section(tr("Anti-piracy", "الحماية من التهكير")) {
            Text(tr("After uploading to Google Play, paste the SHA-256 of the \"App signing key certificate\" (Play Console → Test and release → App integrity). Copies signed with any other key never get Pro.",
                "بعد ما ترفع على جوجل بلاي، الصق SHA-256 بتاع \"App signing key certificate\" (Play Console ← Test and release ← App integrity). أي نسخة متوقعة بمفتاح تاني مش هتاخد برو أبداً."),
                color = Z.sub, fontSize = 12.sp, modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp))
            OutlinedTextField(certs, { certs = it }, placeholder = { Text("AB:CD:…") }, minLines = 2,
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(12.dp))
            Text(tr("This build: ", "النسخة دي: ") + com.smrh.zujaj.security.Guard.certHashes(ctx).joinToString { it.take(16) + "…" } +
                (if (com.smrh.zujaj.security.Guard.compromised()) tr(" · ⚠ untrusted", " · ⚠ غير موثوقة") else " · ✓"),
                color = Z.sub, fontSize = 11.sp, modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 12.dp))
        }
        if (page == 2) Section(tr("Email code for this panel", "كود الإيميل للوحة التحكم")) {
            Text(tr("Web-app URL of the email-code service. When set, entering the panel needs the password, the email AND a 6-digit code sent to your Gmail.",
                "رابط خدمة كود الإيميل. لما يتحط، الدخول للوحة محتاج الباسوورد والإيميل وكود من ٦ أرقام بيوصل على الجيميل."),
                color = Z.sub, fontSize = 12.sp, modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp))
            OutlinedTextField(otp, { otp = it.trim() }, singleLine = true, placeholder = { Text("https://script.google.com/macros/s/…/exec") },
                shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(12.dp))
        }
        PrimaryButton(tr("Save", "حفظ"), Modifier.fillMaxWidth().padding(16.dp)) {
            scope.launch {
                val list = certs.split('\n', ',', ' ').map { it.trim() }.filter { it.isNotEmpty() }
                val r = runCatching { Flags.save(flags.copy(freeAll = freeAll, notice = notice.trim(), certs = list, otpUrl = otp, noticeOnce = once, latestCode = latest.toLongOrNull() ?: 0, forceUpdate = force, updateNote = upNote.trim(), adsOn = adsOn, adBanner = adBanner, adReward = adReward, adWallPerDay = adPer.toIntOrNull() ?: 3, adblockLock = adLock, adUnlockAds = unAds.toIntOrNull() ?: 3, adUnlockDays = (unDays.toIntOrNull() ?: 3).coerceAtLeast(1))) }
                Toast.makeText(ctx, if (r.isSuccess) tr("Saved ✓", "اتحفظ ✓") else adminError(r.exceptionOrNull()), Toast.LENGTH_LONG).show()
            }
        }
        Text(tr("Signed in as $email", "مسجل دخول بـ $email"), color = Z.sub, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(40.dp))
    }
}

// =============================================================================== editor's choice
/** Pick and order the "Editor's choice" cards on the home page. */
@Composable
private fun PicksAdmin() {
    val ctx = LocalContext.current
    val scope = AdminWork
    val flags by Flags.flags.collectAsState()
    var picks by remember(flags) { mutableStateOf(flags.featured.ifEmpty { DEFAULT_PICKS }.filter { Catalog.byId(it) != null }) }
    var q by remember { mutableStateOf("") }
    val changed = picks != flags.featured.ifEmpty { DEFAULT_PICKS }.filter { Catalog.byId(it) != null }
    val rest = Catalog.all.filter { it.id !in picks && (q.isBlank() || it.en.contains(q, true) || it.ar.contains(q) || it.id.contains(q, true)) }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 110.dp)) {
            item {
                Text(tr("These cards slide at the top of the home page, in this order.", "الكروت دي بتتقلب أول الصفحة الرئيسية، بالترتيب ده."),
                    color = Z.sub, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
            }
            itemsIndexed(picks, key = { _, id -> "p_$id" }) { i, id ->
                val st = Catalog.byId(id) ?: return@itemsIndexed
                Row(Modifier.animateItem().fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clip(RoundedCornerShape(18.dp))
                    .background(LocalAccent.current.copy(alpha = 0.08f)).border(1.dp, LocalAccent.current.copy(alpha = 0.3f), RoundedCornerShape(18.dp)).padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("${i + 1}", color = LocalAccent.current, fontWeight = FontWeight.Bold, modifier = Modifier.width(22.dp))
                    Box(Modifier.width(74.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF101015)).padding(4.dp)) { WidgetPreview(st.defaultConfig(), Modifier.fillMaxWidth(), density = 1f) }
                    Spacer(Modifier.width(10.dp))
                    Text(st.name(Prefs.isArabicUi), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    RoundBtn(Icons.Rounded.KeyboardArrowUp) { if (i > 0) picks = picks.toMutableList().also { java.util.Collections.swap(it, i, i - 1) } }
                    Spacer(Modifier.width(4.dp))
                    RoundBtn(Icons.Rounded.KeyboardArrowDown) { if (i < picks.size - 1) picks = picks.toMutableList().also { java.util.Collections.swap(it, i, i + 1) } }
                    Spacer(Modifier.width(4.dp))
                    RoundBtn(Icons.Rounded.Close) { picks = picks - id }
                }
            }
            item {
                OutlinedTextField(q, { q = it }, singleLine = true, placeholder = { Text(tr("Add a widget…", "ضيف ويدجت…")) },
                    leadingIcon = { Icon(Icons.Rounded.Search, null) }, shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LocalAccent.current, unfocusedBorderColor = Z.line),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp))
            }
            items(rest.take(60), key = { "a_" + it.id }) { st ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clip(RoundedCornerShape(18.dp))
                    .background(Z.tint(0.04f)).clickable { picks = picks + st.id }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(74.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF101015)).padding(4.dp)) { WidgetPreview(st.defaultConfig(), Modifier.fillMaxWidth(), density = 1f) }
                    Spacer(Modifier.width(10.dp))
                    Text(st.name(Prefs.isArabicUi), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Icon(Icons.Rounded.Add, null, tint = LocalAccent.current)
                }
            }
        }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Z.bg.copy(alpha = 0.95f), Z.bg)))
            .navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 12.dp)) {
            PrimaryButton(if (changed) tr("Publish picks", "نشر المختارات") else tr("Published · live", "منشور · مباشر"), Modifier.fillMaxWidth()) {
                scope.launch {
                    val r = runCatching { Flags.save(flags.copy(featured = picks)) }
                    Toast.makeText(ctx, if (r.isSuccess) tr("Published ✓", "اتنشر ✓") else adminError(r.exceptionOrNull()), Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}



/** Admin saves keep running even if you leave the page; only the app closing stops them. */
object AdminWork : kotlinx.coroutines.CoroutineScope by kotlinx.coroutines.MainScope()

/** A clear message instead of a technical one. */
fun adminError(e: Throwable?): String {
    val m = e?.message.orEmpty().lowercase()
    return when {
        e == null -> tr("Something went wrong — try again", "حصلت مشكلة — جرب تاني")
        "unavailable" in m || "network" in m || "offline" in m || "timeout" in m || e is java.io.IOException ->
            tr("No internet — check your connection and try again", "مفيش نت — اتأكد من الاتصال وجرب تاني")
        "permission" in m || "denied" in m || "unauthenticated" in m ->
            tr("Not allowed — sign in to the panel again", "مش مسموح — ادخل لوحة التحكم من جديد")
        else -> tr("Couldn't save — try again", "مقدرتش أحفظ — جرب تاني")
    }
}
