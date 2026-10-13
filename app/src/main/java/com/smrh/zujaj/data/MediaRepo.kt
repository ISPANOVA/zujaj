package com.smrh.zujaj.data

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.SystemClock
import android.provider.Settings
import android.service.notification.NotificationListenerService
import com.smrh.zujaj.widget.WidgetUpdater
import java.io.File
import java.io.FileOutputStream

data class MediaState(
    val title: String,
    val artist: String,
    val pkg: String,
    val playing: Boolean,
    val duration: Long,
    val position: Long,
    val art: Bitmap?,
    val real: Boolean,
) {
    val progress: Float get() = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
}

object MediaRepo {
    @Volatile private var controller: MediaController? = null
    @Volatile private var artCache: Pair<String, Bitmap?>? = null
    private var appCtx: Context? = null

    fun hasAccess(ctx: Context): Boolean {
        val flat = Settings.Secure.getString(ctx.contentResolver, "enabled_notification_listeners") ?: return false
        return flat.contains(ctx.packageName + "/")
    }

    fun current(ctx: Context): MediaState {
        val c = controller ?: findController(ctx)
        if (c != null) {
            val md = c.metadata
            val st = c.playbackState
            if (md != null) {
                val title = md.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
                val artist = md.getString(MediaMetadata.METADATA_KEY_ARTIST)
                    ?: md.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST) ?: ""
                val dur = md.getLong(MediaMetadata.METADATA_KEY_DURATION)
                var pos = st?.position ?: 0L
                if (st != null && st.state == PlaybackState.STATE_PLAYING && st.lastPositionUpdateTime > 0) {
                    pos += ((SystemClock.elapsedRealtime() - st.lastPositionUpdateTime) * st.playbackSpeed).toLong()
                }
                return MediaState(
                    title.ifBlank { "—" }, artist, c.packageName,
                    st?.state == PlaybackState.STATE_PLAYING, dur, pos.coerceAtLeast(0), art(ctx, md, title), true
                )
            }
        }
        return MediaState("Midnight Drive", "Zujaj", "", false, 212_000, 84_000, null, false)
    }

    private fun art(ctx: Context, md: MediaMetadata, title: String): Bitmap? {
        val key = title + "|" + (md.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: "")
        artCache?.let { if (it.first == key) return it.second }
        val bmp = md.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: md.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: md.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        val scaled = bmp?.let {
            val m = maxOf(it.width, it.height)
            if (m > 512) Bitmap.createScaledBitmap(it, it.width * 512 / m, it.height * 512 / m, true) else it
        }
        // Persist so the widget can draw it even after process restart
        runCatching {
            val f = File(ctx.cacheDir, "media_art.png")
            if (scaled != null) FileOutputStream(f).use { scaled.compress(Bitmap.CompressFormat.PNG, 100, it) } else f.delete()
        }
        artCache = key to scaled
        return scaled
    }

    fun cachedArt(ctx: Context): Bitmap? = artCache?.second
        ?: runCatching { BitmapFactory.decodeFile(File(ctx.cacheDir, "media_art.png").path) }.getOrNull()

    private fun findController(ctx: Context): MediaController? {
        if (!hasAccess(ctx)) return null
        return runCatching {
            val msm = ctx.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val list = msm.getActiveSessions(ComponentName(ctx, MediaListenerService::class.java))
            (list.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: list.firstOrNull())
                .also { attach(ctx, it) }
        }.getOrNull()
    }

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = refresh()
        override fun onPlaybackStateChanged(state: PlaybackState?) = refresh()
        override fun onSessionDestroyed() { controller = null; refresh() }
    }

    private var lastRefresh = 0L
    private fun refresh() {
        val ctx = appCtx ?: return
        val now = SystemClock.elapsedRealtime()
        if (now - lastRefresh < 400) return
        lastRefresh = now
        WidgetUpdater.updateCategory(ctx, "media")
    }

    fun attach(ctx: Context, c: MediaController?) {
        appCtx = ctx.applicationContext
        if (c?.sessionToken == controller?.sessionToken) return
        controller?.unregisterCallback(callback)
        controller = c
        c?.registerCallback(callback)
    }

    fun onSessionsChanged(ctx: Context, list: List<MediaController>?) {
        val best = list?.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: list?.firstOrNull()
        attach(ctx, best)
        WidgetUpdater.updateCategory(ctx, "media")
    }

    // ---- Transport controls ----
    fun command(ctx: Context, cmd: String) {
        val c = controller ?: findController(ctx)
        if (c == null) {
            // No session: open the default music player
            return
        }
        val t = c.transportControls
        when (cmd) {
            "play_pause" -> if (c.playbackState?.state == PlaybackState.STATE_PLAYING) t.pause() else t.play()
            "next" -> t.skipToNext()
            "prev" -> t.skipToPrevious()
            // Compat sessions (most players) understand these custom actions
            "shuffle" -> runCatching {
                t.sendCustomAction("android.support.v4.media.session.action.SET_SHUFFLE_MODE",
                    android.os.Bundle().apply { putInt("android.support.v4.media.session.action.ARGUMENT_SHUFFLE_MODE", 1) })
            }
            "repeat" -> runCatching {
                t.sendCustomAction("android.support.v4.media.session.action.SET_REPEAT_MODE",
                    android.os.Bundle().apply { putInt("android.support.v4.media.session.action.ARGUMENT_REPEAT_MODE", 2) })
            }
        }
    }

    fun launchIntent(ctx: Context): android.content.Intent? {
        val pkg = controller?.packageName ?: return null
        return ctx.packageManager.getLaunchIntentForPackage(pkg)
    }
}

class MediaListenerService : NotificationListenerService() {
    private var listener: MediaSessionManager.OnActiveSessionsChangedListener? = null

    override fun onListenerConnected() {
        super.onListenerConnected()
        val msm = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val cn = ComponentName(this, MediaListenerService::class.java)
        val l = MediaSessionManager.OnActiveSessionsChangedListener { MediaRepo.onSessionsChanged(applicationContext, it) }
        runCatching {
            msm.addOnActiveSessionsChangedListener(l, cn)
            listener = l
            MediaRepo.onSessionsChanged(applicationContext, msm.getActiveSessions(cn))
        }
    }

    override fun onListenerDisconnected() {
        listener?.let {
            runCatching { (getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager).removeOnActiveSessionsChangedListener(it) }
        }
        super.onListenerDisconnected()
    }
}
