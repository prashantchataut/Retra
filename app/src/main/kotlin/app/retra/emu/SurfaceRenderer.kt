package app.retra.emu

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView

/**
 * Draws frames from the emulation thread into a TextureView's surface with a hardware canvas
 * (Surface.lockHardwareCanvas). A TextureView composes like any other view, so the rounded bezel
 * clip, glass sheets, blur and shared-element transitions all work on top of the game image.
 * The GBA image is scaled to fit (optionally integer-scaled) with nearest or bilinear filtering.
 */
class SurfaceRenderer : TextureView.SurfaceTextureListener, FrameSink {
    private val lock = Any()
    private val bitmap = Bitmap.createBitmap(EmuCore.WIDTH, EmuCore.HEIGHT, Bitmap.Config.ARGB_8888)
    private val paint = Paint()
    private val dst = Rect()
    private var surface: Surface? = null
    private var width = 0
    private var height = 0

    @Volatile var smooth = false
    @Volatile var integerScale = false
    @Volatile var background = Color.BLACK

    override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
        synchronized(lock) {
            surface = Surface(texture)
            this.width = width
            this.height = height
        }
        redraw()
    }

    override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
        synchronized(lock) {
            this.width = width
            this.height = height
        }
        redraw()
    }

    override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
        synchronized(lock) {
            surface?.release()
            surface = null
        }
        return true
    }

    override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit

    override fun onFrame(pixels: IntArray) {
        synchronized(lock) {
            bitmap.setPixels(pixels, 0, EmuCore.WIDTH, 0, 0, EmuCore.WIDTH, EmuCore.HEIGHT)
            draw()
        }
    }

    /** Redraws the last frame (after resize, a filter change, or while paused). */
    fun redraw() = synchronized(lock) { draw() }

    private fun draw() {
        val s = surface ?: return
        if (width == 0 || height == 0 || !s.isValid) return
        val canvas: Canvas = try {
            s.lockHardwareCanvas()
        } catch (e: Exception) {
            return
        }
        try {
            computeDst(width, height, integerScale, dst)
            paint.isFilterBitmap = smooth
            canvas.drawColor(background)
            canvas.drawBitmap(bitmap, null, dst, paint)
        } finally {
            runCatching { s.unlockCanvasAndPost(canvas) }
        }
    }

    companion object {
        /** Largest 3:2 rect centred in w x h; with [integer], the largest whole multiple of 240x160 (if >= 1x). */
        fun computeDst(w: Int, h: Int, integer: Boolean, out: Rect) {
            var scale = minOf(w / EmuCore.WIDTH.toFloat(), h / EmuCore.HEIGHT.toFloat())
            if (integer && scale >= 1f) scale = kotlin.math.floor(scale)
            val dw = (EmuCore.WIDTH * scale).toInt()
            val dh = (EmuCore.HEIGHT * scale).toInt()
            val left = (w - dw) / 2
            val top = (h - dh) / 2
            out.set(left, top, left + dw, top + dh)
        }
    }
}
