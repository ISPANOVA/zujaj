package com.smrh.zujaj.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smrh.zujaj.BuildConfig
import com.smrh.zujaj.R
import com.smrh.zujaj.data.Prefs
import com.smrh.zujaj.remote.Flags
import kotlinx.coroutines.delay

/** Logo + "Loading" shown for a moment when the app opens. */
@Composable
fun Splash(show: Boolean) {
    var visible by remember { mutableStateOf(show) }
    LaunchedEffect(Unit) { delay(1200); visible = false }
    AnimatedVisibility(visible, exit = fadeOut(androidx.compose.animation.core.tween(350)) + scaleOut(targetScale = 1.04f)) {
        Box(
            Modifier.fillMaxSize().background(Color(0xFF0B0B12))
                .clickable(MutableInteractionSource(), null) {},
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(112.dp).clip(RoundedCornerShape(30.dp)).background(colorResource(R.color.ic_bg)), contentAlignment = Alignment.Center) {
                    Image(painterResource(R.drawable.ic_fg), null, Modifier.fillMaxSize())
                }
                Spacer(Modifier.height(22.dp))
                Text(tr("Zujaj", "زجاج"), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Spacer(Modifier.height(26.dp))
                CircularProgressIndicator(Modifier.size(22.dp), color = Color(0xFFE7B75F), strokeWidth = 2.dp)
                Spacer(Modifier.height(12.dp))
                Text(tr("Loading…", "جاري التحميل…"), color = Color(0xFF9C9CA6), fontSize = 13.sp)
            }
        }
    }
}

/** Update prompt (optional or required) and the one-time message from the control panel. */
@Composable
fun AppGates() {
    val ctx = LocalContext.current
    val f by Flags.flags.collectAsState()
    val outdated = f.latestCode > BuildConfig.VERSION_CODE
    var laterFor by remember { mutableLongStateOf(Prefs.getString("upd_later").toLongOrNull() ?: 0L) }
    var seen by remember { mutableStateOf(Prefs.getString("notice_seen")) }
    fun openStore() {
        val id = ctx.packageName
        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$id")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$id")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
    }
    when {
        outdated && f.forceUpdate -> Box(
            Modifier.fillMaxSize().background(Color(0xF20B0B12)).clickable(MutableInteractionSource(), null) {}.padding(28.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(84.dp).clip(RoundedCornerShape(24.dp)).background(colorResource(R.color.ic_bg))) {
                    Image(painterResource(R.drawable.ic_fg), null, Modifier.fillMaxSize())
                }
                Spacer(Modifier.height(20.dp))
                Text(tr("Update required", "لازم تحدّث التطبيق"), color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Text(f.updateNote.ifBlank { tr("A new version is out. Update to keep using Zujaj.", "فيه إصدار جديد. حدّث التطبيق عشان تكمل استخدامه.") },
                    color = Color(0xFFBDBDC6), textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
                PrimaryButton(tr("Update now", "حدّث دلوقتي"), Modifier.fillMaxWidth()) { openStore() }
            }
        }
        outdated && laterFor < f.latestCode -> GateDialog(
            tr("New update", "فيه تحديث جديد"),
            f.updateNote.ifBlank { tr("A new version of Zujaj is available.", "فيه إصدار جديد من زجاج.") },
            tr("Update", "حدّث"), { openStore() },
            tr("Later", "بعدين"), { laterFor = f.latestCode; Prefs.setString("upd_later", f.latestCode.toString()) },
        )
        f.noticeOnce && f.notice.isNotBlank() && seen != f.notice -> GateDialog(
            tr("Message", "رسالة"), f.notice, tr("OK", "تمام"), { seen = f.notice; Prefs.setString("notice_seen", f.notice) }, null, null,
        )
    }
}

@Composable
private fun GateDialog(title: String, body: String, ok: String, onOk: () -> Unit, other: String?, onOther: (() -> Unit)?) {
    androidx.compose.ui.window.Dialog(onDismissRequest = { (onOther ?: onOk)() }) {
        Column(Modifier.clip(RoundedCornerShape(26.dp)).background(Z.surface).padding(22.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text(body, color = Z.sub)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (other != null && onOther != null) TextButton(onClick = onOther) { Text(other, color = Z.sub) }
                TextButton(onClick = onOk) { Text(ok, color = LocalAccent.current, fontWeight = FontWeight.Bold) }
            }
        }
    }
}
