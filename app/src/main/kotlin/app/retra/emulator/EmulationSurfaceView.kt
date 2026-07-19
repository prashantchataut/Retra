package app.retra.emulator

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import app.retra.emulation.api.VideoFrame

/**
 * Allocation-conscious software presenter for the current frame pipeline.
 * A later OpenGL/Vulkan renderer can replace this class without changing EmulationCore.
 */
class EmulationSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SurfaceView(context, attrs), SurfaceHolder.Callback {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFilterBitmap = false
        isDither = false
    }
    private var bitmap: Bitmap? = null
    private var pendingFrame: VideoFrame? = null
    private var surfaceReady = false
    private var integerScaling = true
    private var smoothing = false

    init {
        holder.addCallback(this)
        keepScreenOn = true
        contentDescription = "Emulation video output"
        setBackgroundColor(Color.BLACK)
    }

    fun configure(integerScaling: Boolean, smoothing: Boolean) {
        this.integerScaling = integerScaling
        this.smoothing = smoothing
        paint.isFilterBitmap = smoothing
        drawLatestFrame()
    }

    fun submitFrame(frame: VideoFrame) {
        pendingFrame = frame
        if (!surfaceReady) return
        if (bitmap?.width != frame.width || bitmap?.height != frame.height) {
            bitmap?.recycle()
            bitmap = Bitmap.createBitmap(frame.width, frame.height, Bitmap.Config.ARGB_8888)
        }
        bitmap?.setPixels(frame.argb, 0, frame.width, 0, 0, frame.width, frame.height)
        drawLatestFrame()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        surfaceReady = true
        pendingFrame?.let(::submitFrame)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        drawLatestFrame()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        surfaceReady = false
    }

    override fun onDetachedFromWindow() {
        bitmap?.recycle()
        bitmap = null
        super.onDetachedFromWindow()
    }

    private fun drawLatestFrame() {
        if (!surfaceReady) return
        val source = bitmap ?: return
        val canvas = runCatching { holder.lockCanvas() }.getOrNull() ?: return
        try {
            canvas.drawColor(Color.BLACK)
            val destination = aspectFit(source.width, source.height, canvas)
            canvas.drawBitmap(source, null, destination, paint)
        } finally {
            holder.unlockCanvasAndPost(canvas)
        }
    }

    private fun aspectFit(sourceWidth: Int, sourceHeight: Int, canvas: Canvas): RectF {
        val canvasWidth = canvas.width.coerceAtLeast(1)
        val canvasHeight = canvas.height.coerceAtLeast(1)
        if (integerScaling) {
            val fit = minOf(canvasWidth.toFloat() / sourceWidth, canvasHeight.toFloat() / sourceHeight)
            if (fit >= 1f) {
                val scale = kotlin.math.floor(fit).coerceAtLeast(1f)
                val width = sourceWidth * scale
                val height = sourceHeight * scale
                val left = (canvasWidth - width) / 2f
                val top = (canvasHeight - height) / 2f
                return RectF(left, top, left + width, top + height)
            }
        }
        val sourceRatio = sourceWidth.toFloat() / sourceHeight
        val targetRatio = canvasWidth.toFloat() / canvasHeight
        return if (targetRatio > sourceRatio) {
            val width = canvasHeight * sourceRatio
            val left = (canvasWidth - width) / 2f
            RectF(left, 0f, left + width, canvasHeight.toFloat())
        } else {
            val height = canvasWidth / sourceRatio
            val top = (canvasHeight - height) / 2f
            RectF(0f, top, canvasWidth.toFloat(), top + height)
        }
    }
}
