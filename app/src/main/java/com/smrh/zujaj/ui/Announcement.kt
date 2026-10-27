package com.smrh.zujaj.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smrh.zujaj.remote.Announce
import kotlinx.coroutines.delay

/** Shows the control-panel pop-up once the app has settled (after onboarding and the battery guide). */
@Composable
fun AnnouncementHost(nav: Nav) {
    var pop by remember { mutableStateOf<Announce.Popup?>(null) }
    LaunchedEffect(Unit) {
        if (com.smrh.zujaj.data.Prefs.getString("onboarded") != "1") return@LaunchedEffect
        delay(1200)
        val p = Announce.load() ?: return@LaunchedEffect
        if (Announce.due(p)) { Announce.markShown(p); pop = p }
    }
    pop?.let { p -> AnnouncementDialog(p, onAction = { pop = null; runAction(nav, p) }) { pop = null } }
}

@Composable
fun AnnouncementDialog(p: Announce.Popup, onAction: () -> Unit, onDismiss: () -> Unit) {
    val img = remember(p.image) {
        runCatching { Base64.decode(p.image, Base64.DEFAULT).let { BitmapFactory.decodeByteArray(it, 0, it.size) }?.asImageBitmap() }.getOrNull()
    }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.widthIn(max = 440.dp).fillMaxWidth().padding(horizontal = 22.dp).clip(RoundedCornerShape(Rad.xl)).background(Z.surface)
            .border(1.dp, Z.line, RoundedCornerShape(Rad.xl)).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            if (img != null) Image(img, null, Modifier.fillMaxWidth().aspectRatio(img.width.toFloat() / img.height.coerceAtLeast(1)).heightIn(max = 320.dp), contentScale = ContentScale.Crop)
            Column(Modifier.padding(horizontal = 22.dp, vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (p.title.isNotBlank()) Text(p.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Z.text, textAlign = TextAlign.Center)
                if (p.body.isNotBlank()) { Spacer(Modifier.height(8.dp)); Text(p.body, fontSize = 15.sp, color = Z.sub, textAlign = TextAlign.Center) }
                Spacer(Modifier.height(18.dp))
                if (p.action != "none" && p.button.isNotBlank()) PrimaryButton(p.button, Modifier.fillMaxWidth()) { onAction() }
                TextButton(onClick = onDismiss) { Text(if (p.action != "none" && p.button.isNotBlank()) tr("Not now", "مش دلوقتي") else tr("OK", "تمام"), color = Z.sub) }
            }
        }
    }
}

/** Where the pop-up's button goes. */
val popupActions = listOf(
    "none" to ("No button" to "من غير زرار"), "wallpapers" to ("Wallpapers" to "الخلفيات"), "paywall" to ("Zujaj Pro" to "زجاج برو"),
    "gallery" to ("Community designs" to "تصاميم الناس"), "search" to ("Search" to "البحث"),
    "settings" to ("Settings" to "الإعدادات"), "url" to ("A link" to "لينك"),
)

private fun runAction(nav: Nav, p: Announce.Popup) {
    when (p.action) {
        "wallpapers" -> nav.go(Screen.Wallpapers)
        "paywall" -> nav.go(Screen.Paywall)
        "gallery" -> nav.go(Screen.Gallery)
        "search" -> nav.go(Screen.Search)
        "settings" -> nav.go(Screen.Settings)
        "url" -> runCatching { appCtx?.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(p.url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}
/** Set by MainActivity so a link can be opened from a plain function. */
var appCtx: android.content.Context? = null
