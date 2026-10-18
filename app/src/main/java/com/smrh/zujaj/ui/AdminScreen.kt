package com.smrh.zujaj.ui

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
        Segmented(listOf(tr("Widgets", "الويدجت"), tr("Codes", "الأكواد"), tr("General", "عام")), tab, Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) { tab = it }
        when (tab) {
            0 -> WidgetsAdmin()
            1 -> CodesAdmin()
            else -> GeneralAdmin(user?.email ?: "")
        }
    }
}

// =============================================================================== widgets
@Composable
private fun WidgetsAdmin() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
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
                    Toast.makeText(ctx, if (r.isSuccess) tr("Published ✓ — live for everyone", "تم النشر ✓ — ظاهر للجميع الآن") else (r.exceptionOrNull()?.message ?: "Error"), Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, n: Int, color: Color, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.05f)).border(1.dp, Z.line, RoundedCornerShape(16.dp)).padding(vertical = 10.dp),
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
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
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
                                if (deadline == 0) 0 else System.currentTimeMillis() + deadline * 86_400_000L, note.trim(), 0))
                        }
                        busy = false
                        if (r.isSuccess) {
                            copy(ctx, c)
                            Toast.makeText(ctx, tr("Created & copied: $c", "اتعمل واتنسخ: $c"), Toast.LENGTH_LONG).show()
                            code = Codes.randomCode(); note = ""; reload++
                        } else Toast.makeText(ctx, r.exceptionOrNull()?.message ?: "Error", Toast.LENGTH_LONG).show()
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
            )
        }
    }
    confirmDelete?.let { c ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text(tr("Delete $c?", "حذف $c؟")) },
            text = { Text(tr("People who already redeemed it keep their Pro time.", "اللي استخدموه قبل كده هيفضل معاهم البرو لحد مدته.")) },
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
    Box(Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f)).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = Z.text, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun CodeCard(c: RedeemCode, onCopy: () -> Unit, onActive: (Boolean) -> Unit, onDelete: () -> Unit) {
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
            .background(Color.White.copy(alpha = 0.05f)).border(1.dp, Z.line, RoundedCornerShape(20.dp)).padding(14.dp)
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
            Text("PRO · ${c.durationLabel(ar)}", color = Z.pro, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(10.dp))
            Text(
                if (c.maxUses == 0L) tr("${c.uses} used · unlimited", "${c.uses} استخدام · غير محدود") else tr("${c.uses} / ${c.maxUses} used", "${c.uses} من ${c.maxUses} استخدموه"),
                color = Z.sub, fontSize = 12.sp,
            )
        }
        if (c.maxUses > 0) {
            Box(Modifier.padding(top = 8.dp).fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.08f))) {
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
            IconButton(onClick = onCopy) { Icon(Icons.Rounded.ContentCopy, null, tint = Z.text) }
            IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, null, tint = Color(0xFFE5484D)) }
        }
    }
}

private fun copy(ctx: Context, s: String) {
    (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("code", s))
}

// =============================================================================== general
@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
private fun GeneralAdmin(email: String) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val flags by Flags.flags.collectAsState()
    var freeAll by remember(flags) { mutableStateOf(flags.freeAll) }
    var notice by remember(flags) { mutableStateOf(flags.notice) }
    var certs by remember(flags) { mutableStateOf(flags.certs.joinToString("\n")) }
    var otp by remember(flags) { mutableStateOf(flags.otpUrl) }
    var once by remember(flags) { mutableStateOf(flags.noticeOnce) }
    var latest by remember(flags) { mutableStateOf(if (flags.latestCode > 0) flags.latestCode.toString() else "") }
    var force by remember(flags) { mutableStateOf(flags.forceUpdate) }
    var upNote by remember(flags) { mutableStateOf(flags.updateNote) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Section(tr("Promotion", "العروض")) {
            SwitchRow(tr("Everything free", "كل حاجة مجاني"), freeAll, tr("Every Pro widget unlocked for all users", "كل ويدجت البرو مفتوحة لكل المستخدمين")) { freeAll = it }
        }
        Section(tr("Message to all users", "رسالة لكل المستخدمين")) {
            OutlinedTextField(notice, { notice = it }, placeholder = { Text(tr("Shown on the home page (leave empty to hide)", "بتظهر في الصفحة الرئيسية (سيبها فاضية عشان تختفي)")) },
                shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(12.dp))
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(false to tr("Always on home", "دايماً في الرئيسية"), true to tr("Pop-up once", "تظهر مرة واحدة بس")).forEach { (v, l) ->
                    FilterChip(once == v, { once = v }, label = { Text(l) })
                }
            }
        }
        Section(tr("App update", "تحديث التطبيق")) {
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
        Section(tr("Anti-piracy", "الحماية من التهكير")) {
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
        Section(tr("Email code for this panel", "كود الإيميل للوحة التحكم")) {
            Text(tr("Web-app URL of the email-code service. When set, entering the panel needs the password, the email AND a 6-digit code sent to your Gmail.",
                "رابط خدمة كود الإيميل. لما يتحط، الدخول للوحة محتاج الباسوورد والإيميل وكود من ٦ أرقام بيوصل على الجيميل."),
                color = Z.sub, fontSize = 12.sp, modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp))
            OutlinedTextField(otp, { otp = it.trim() }, singleLine = true, placeholder = { Text("https://script.google.com/macros/s/…/exec") },
                shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(12.dp))
        }
        PrimaryButton(tr("Save", "حفظ"), Modifier.fillMaxWidth().padding(16.dp)) {
            scope.launch {
                val list = certs.split('\n', ',', ' ').map { it.trim() }.filter { it.isNotEmpty() }
                val r = runCatching { Flags.save(flags.copy(freeAll = freeAll, notice = notice.trim(), certs = list, otpUrl = otp, noticeOnce = once, latestCode = latest.toLongOrNull() ?: 0, forceUpdate = force, updateNote = upNote.trim())) }
                Toast.makeText(ctx, if (r.isSuccess) tr("Saved ✓", "اتحفظ ✓") else (r.exceptionOrNull()?.message ?: "Error"), Toast.LENGTH_LONG).show()
            }
        }
        Text(tr("Signed in as $email", "مسجل دخول بـ $email"), color = Z.sub, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp))
    }
}

