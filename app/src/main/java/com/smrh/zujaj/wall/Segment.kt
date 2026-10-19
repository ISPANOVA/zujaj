package com.smrh.zujaj.wall

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.tasks.await

/** Finds the main subject of a photo (mountain, building, person…) and returns an alpha mask. */
object Segment {
    sealed class Result {
        data class Ok(val mask: Bitmap) : Result()
        data class Fail(val why: String) : Result()
    }

    suspend fun run(ctx: Context, photo: Bitmap): Result {
        // work on a reasonably sized copy (fast, enough detail for a wallpaper)
        val k = (maxOf(photo.width, photo.height) / 1600f).coerceAtLeast(1f)
        val src = if (k > 1f) Bitmap.createScaledBitmap(photo, (photo.width / k).toInt(), (photo.height / k).toInt(), true) else photo
        val img = InputImage.fromBitmap(src, 0)
        // 1) any subject (objects, buildings, people…) via Google Play services
        val subject = runCatching {
            val opts = com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions.Builder()
                .enableForegroundConfidenceMask().build()
            val client = com.google.mlkit.vision.segmentation.subject.SubjectSegmentation.getClient(opts)
            val r = client.process(img).await()
            client.close()
            val buf = r.foregroundConfidenceMask ?: error("no mask")
            buf.rewind()
            val px = IntArray(src.width * src.height) { Color.argb((buf.get().coerceIn(0f, 1f) * 255).toInt(), 0, 0, 0) }
            Bitmap.createBitmap(px, src.width, src.height, Bitmap.Config.ARGB_8888)
        }
        subject.getOrNull()?.let { return Result.Ok(it) }
        // ask Play services to download the model for next time
        runCatching {
            val client = com.google.mlkit.vision.segmentation.subject.SubjectSegmentation.getClient(
                com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions.Builder().enableForegroundConfidenceMask().build())
            com.google.android.gms.common.moduleinstall.ModuleInstall.getClient(ctx).installModules(
                com.google.android.gms.common.moduleinstall.ModuleInstallRequest.newBuilder().addApi(client).build())
        }
        // 2) people (bundled in the app, always available)
        val selfie = runCatching {
            val opts = com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions.Builder()
                .setDetectorMode(com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions.SINGLE_IMAGE_MODE).build()
            val client = com.google.mlkit.vision.segmentation.Segmentation.getClient(opts)
            val r = client.process(img).await()
            client.close()
            val buf = r.buffer; buf.rewind()
            val w = r.width; val h = r.height
            var any = 0
            val px = IntArray(w * h) { val v = buf.float.coerceIn(0f, 1f); if (v > 0.5f) any++; Color.argb((v * 255).toInt(), 0, 0, 0) }
            if (any < w * h / 200) error("nothing found")
            Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888)
        }
        selfie.getOrNull()?.let { return Result.Ok(it) }
        return Result.Fail(subject.exceptionOrNull()?.message ?: "not found")
    }
}
