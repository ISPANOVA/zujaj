package com.smrh.zujaj.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddToHomeScreen
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smrh.zujaj.catalog.Catalog
import com.smrh.zujaj.data.Prefs
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/** First launch: welcome → make it yours (light/dark + colour) → how it works. Shown once, to new users only. */
object Onboarding {
    /** survives the theme rebuilding the screen while the user picks a colour */
    var page by mutableIntStateOf(0)
    var visible by mutableStateOf(true)
    fun done() = Prefs.getString("onboarded") == "1" || Prefs.getString("wall_intro") == "1"
    fun finish() { Prefs.setString("onboarded", "1"); visible = false }
}

@Composable
fun OnboardingScreen() {
    if (Onboarding.done() && !Onboarding.visible) return
    if (Prefs.getString("onboarded") == "1") return
    if (Prefs.getString("wall_intro") == "1") { LaunchedEffect(Unit) { Onboarding.finish() }; return }
    val ctx = LocalContext.current
    val accent = LocalAccent.current
    val pager = rememberPagerState(initialPage = Onboarding.page) { 3 }
    val scope = rememberCoroutineScope()
    LaunchedEffect(pager.currentPage) { Onboarding.page = pager.currentPage; Haptics.tick(ctx) }
    val t = rememberInfiniteTransition(label = "onb")
    val phase by t.animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "ph")
    AnimatedVisibility(Onboarding.visible, enter = fadeIn(), exit = fadeOut(tween(420)) + scaleOut(targetScale = 1.06f)) {
        Box(Modifier.fillMaxSize().background(Z.bg).clickable(MutableInteractionSource(), null) {}) {
            // drifting glow orbs
            Canvas(Modifier.fillMaxSize()) {
                val a = phase * 2 * PI.toFloat()
                fun orb(c: Color, al: Float, x: Float, y: Float, r: Float) =
                    drawCircle(Brush.radialGradient(listOf(c.copy(alpha = al), Color.Transparent), Offset(x, y), r), r, Offset(x, y))
                val k = if (Z.light) 0.55f else 1f
                orb(accent, 0.35f * k, size.width * (0.2f + 0.1f * sin(a)), size.height * (0.18f + 0.04f * sin(a * 2)), size.width * 0.9f)
                orb(Color(0xFF6C5CE7), 0.30f * k, size.width * (0.9f + 0.08f * sin(a + 2)), size.height * 0.55f, size.width * 0.8f)
                orb(Color(0xFF13B5A8), 0.18f * k, size.width * 0.3f, size.height * (0.95f + 0.03f * sin(a + 4)), size.width * 0.8f)
            }
            Column(Modifier.fillMaxSize().systemBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), horizontalArrangement = Arrangement.End) {
                    if (pager.currentPage < 2) Text(tr("Skip", "تخطي"), color = Z.sub, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clip(RoundedCornerShape(50)).clickable { Haptics.tick(ctx); Onboarding.finish() }.padding(horizontal = 14.dp, vertical = 8.dp))
                    else Spacer(Modifier.height(36.dp))
                }
                HorizontalPager(pager, Modifier.weight(1f)) { p ->
                    when (p) {
                        0 -> WelcomePage(phase)
                        1 -> LookPage()
                        else -> StepsPage(pager.currentPage == 2)
                    }
                }
                // dots
                Row(Modifier.fillMaxWidth().padding(bottom = 18.dp), horizontalArrangement = Arrangement.Center) {
                    repeat(3) { i ->
                        val on = pager.currentPage == i
                        val w by animateDpAsState(if (on) 26.dp else 8.dp, spring(0.6f, 400f), label = "d")
                        val c by animateColorAsState(if (on) accent else Z.tint(0.25f), label = "c")
                        Box(Modifier.padding(horizontal = 4.dp).height(8.dp).width(w).clip(RoundedCornerShape(50)).background(c))
                    }
                }
                PrimaryButton(if (pager.currentPage < 2) tr("Next", "التالي") else tr("Let's start", "يلا نبدأ"),
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 22.dp)) {
                    if (pager.currentPage < 2) scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                    else { Haptics.success(ctx); Onboarding.finish() }
                }
            }
        }
    }
}

@Composable
private fun WelcomePage(phase: Float) {
    val accent = LocalAccent.current
    val picks = remember { listOf("clock_neon", "calli_lawn", "event_ring", "prayer_hub").mapNotNull { Catalog.byId(it) }.take(3) }
    val t = rememberInfiniteTransition(label = "w")
    val pulse by t.animateFloat(0.85f, 1.05f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "p")
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val inA by animateFloatAsState(if (shown) 1f else 0f, tween(700), label = "in")
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        // glowing glass logo with a spinning rim
        Box(Modifier.size(150.dp).graphicsLayer { scaleX = 0.7f + 0.3f * inA; scaleY = 0.7f + 0.3f * inA; alpha = inA }, contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = 0.55f * pulse), Color.Transparent)), size.minDimension * 0.75f)
                rotate(phase * 360f) {
                    drawCircle(Brush.sweepGradient(listOf(accent, Color(0xFF8B7CFF), Color(0xFF3DD9B4), accent)), size.minDimension * 0.36f,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(5.dp.toPx()))
                }
            }
            // the real app logo
            Box(Modifier.size(96.dp).clip(RoundedCornerShape(28.dp))
                .background(androidx.compose.ui.res.colorResource(com.smrh.zujaj.R.color.ic_bg))
                .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(28.dp)), contentAlignment = Alignment.Center) {
                androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(com.smrh.zujaj.R.drawable.ic_fg), null, Modifier.fillMaxSize())
            }
        }
        Spacer(Modifier.height(26.dp))
        Text(tr("Welcome to Zujaj", "أهلاً بيك في زجاج"), fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Z.text, textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer { alpha = inA; translationY = (1 - inA) * 30f })
        Spacer(Modifier.height(10.dp))
        Text(tr("Widgets and wallpapers with an artistic touch — made to look like you.", "ويدجت وخلفيات بلمسة فنية، معمولة على ذوقك انت."),
            fontSize = 15.sp, color = Z.sub, textAlign = TextAlign.Center, modifier = Modifier.graphicsLayer { alpha = inA })
        Spacer(Modifier.height(30.dp))
        // three widgets floating gently
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            picks.forEachIndexed { i, st ->
                val y = sin((phase * 2 * PI + i * 2.1).toFloat()) * 8f
                Box(Modifier.weight(1f).graphicsLayer { translationY = y; alpha = inA; rotationZ = (i - 1) * 4f }.clip(RoundedCornerShape(18.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF1C1C24), Color(0xFF111117)))).padding(6.dp)) {
                    WidgetPreview(st.defaultConfig(), Modifier.fillMaxWidth(), density = 1.4f)
                }
            }
        }
    }
}

@Composable
private fun LookPage() {
    val ctx = LocalContext.current
    val accent = LocalAccent.current
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(tr("Make it yours", "خليه على ذوقك"), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Z.text)
        Spacer(Modifier.height(8.dp))
        Text(tr("You can change this any time in Settings.", "تقدر تغيّر ده في أي وقت من الإعدادات."), fontSize = 14.sp, color = Z.sub)
        Spacer(Modifier.height(26.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(Triple(1, tr("Dark", "غامق"), Icons.Rounded.DarkMode), Triple(2, tr("Light", "فاتح"), Icons.Rounded.LightMode), Triple(0, tr("Auto", "تلقائي"), Icons.Rounded.PhoneAndroid))
                .forEach { (m, label, icon) ->
                    val on = Z.mode() == m
                    val bw by animateDpAsState(if (on) 2.dp else 1.dp, label = "bw")
                    Column(Modifier.weight(1f).pressScale().clip(RoundedCornerShape(22.dp))
                        .background(Z.surface).border(bw, if (on) accent else Z.line, RoundedCornerShape(22.dp))
                        .clickable { Haptics.tick(ctx); Prefs.setString("theme_mode", m.toString()); Z.refresh(ctx) }.padding(vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        // tiny phone mock in that look
                        val dark = m == 1 || (m == 0 && !Z.light)
                        Box(Modifier.size(52.dp, 84.dp).clip(RoundedCornerShape(12.dp)).background(if (dark) Color(0xFF0B0B12) else Color(0xFFF3F2F7))
                            .border(1.dp, Z.line, RoundedCornerShape(12.dp)).padding(7.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Box(Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(5.dp)).background(accent))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    repeat(2) { Box(Modifier.weight(1f).height(16.dp).clip(RoundedCornerShape(5.dp)).background(if (dark) Color(0xFF22222C) else Color.White)) }
                                }
                                Box(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(4.dp)).background(if (dark) Color(0xFF22222C) else Color.White))
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(icon, null, tint = if (on) accent else Z.sub, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(4.dp))
                            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Z.text)
                        }
                    }
                }
        }
        Spacer(Modifier.height(28.dp))
        Text(tr("Your colour", "لونك"), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Z.text)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Z.accents.forEachIndexed { i, a ->
                val on = i == (if (Z.accentIdx >= 0) Z.accentIdx else Prefs.accentIndex)
                val s by animateFloatAsState(if (on) 1.15f else 1f, spring(0.5f, 500f), label = "s")
                Box(Modifier.graphicsLayer { scaleX = s; scaleY = s }.size(38.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(a.color, a.color.copy(alpha = 0.75f))))
                    .border(if (on) 3.dp else 0.dp, Color.White, CircleShape)
                    .clickable { Haptics.tick(ctx); Z.pickAccent(i) }, contentAlignment = Alignment.Center) {
                    if (on) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun StepsPage(active: Boolean) {
    val accent = LocalAccent.current
    val steps = listOf(
        Triple(Icons.Rounded.TouchApp, tr("Pick a design", "اختار تصميم"), tr("Hundreds of clocks, prayer times, dates and photos.", "مئات الساعات ومواقيت الصلاة والتواريخ والصور.")),
        Triple(Icons.Rounded.Brush, tr("Make it yours", "ظبطه على ذوقك"), tr("Colours, fonts, glass — every detail.", "الألوان والخطوط والزجاج — كل تفصيلة.")),
        Triple(Icons.Rounded.AddToHomeScreen, tr("Add it to your screen", "حطه على شاشتك"), tr("One tap and it's on your home screen.", "ضغطة واحدة وتبقى على الشاشة الرئيسية.")),
    )
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp), verticalArrangement = Arrangement.Center) {
        Text(tr("Three steps, that's it", "تلات خطوات وبس"), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Z.text, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(28.dp))
        steps.forEachIndexed { i, (icon, title, sub) ->
            var shown by remember { mutableStateOf(false) }
            LaunchedEffect(active) { if (active) { kotlinx.coroutines.delay(150L + i * 220L); shown = true } }
            val a by animateFloatAsState(if (shown) 1f else 0f, spring(0.7f, 260f), label = "a")
            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp).graphicsLayer { alpha = a; translationX = (1 - a) * 80f }
                .clip(RoundedCornerShape(22.dp)).background(Z.surface).border(1.dp, Z.line, RoundedCornerShape(22.dp)).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(15.dp))
                    .background(Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.7f)))), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = Color(0xFF14110A), modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("${i + 1}. $title", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Z.text)
                    Text(sub, fontSize = 13.sp, color = Z.sub)
                }
            }
        }
    }
}
