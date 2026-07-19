package app.retra.emulator.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import app.retra.emulation.api.VideoFrame
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ScreenshotResult(
    val displayName: String,
    val location: String
)

@Singleton
class ScreenshotRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun save(gameTitle: String, frame: VideoFrame): Result<ScreenshotResult> = withContext(Dispatchers.IO) {
        runCatching {
            val safeTitle = gameTitle
                .replace(Regex("[^A-Za-z0-9._ -]"), "_")
                .trim()
                .take(48)
                .ifBlank { "GBA" }
            val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
            val name = "$safeTitle-$timestamp.png"
            val bitmap = Bitmap.createBitmap(frame.width, frame.height, Bitmap.Config.ARGB_8888)
            try {
                bitmap.setPixels(frame.argb, 0, frame.width, 0, 0, frame.width, frame.height)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, name)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Retra")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                    val uri = requireNotNull(context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)) {
                        "Android could not create the screenshot entry."
                    }
                    try {
                        context.contentResolver.openOutputStream(uri, "w").use { output ->
                            requireNotNull(output) { "Android could not open screenshot storage." }
                            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) { "PNG encoding failed." }
                        }
                        val complete = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
                        check(context.contentResolver.update(uri, complete, null, null) > 0) { "Android could not publish the screenshot." }
                        ScreenshotResult(name, uri.toString())
                    } catch (error: Exception) {
                        context.contentResolver.delete(uri, null, null)
                        throw error
                    }
                } else {
                    val root = requireNotNull(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)) {
                        "External app storage is unavailable."
                    }
                    val directory = File(root, "Retra").apply { mkdirs() }
                    val file = File(directory, name)
                    FileOutputStream(file).use { output ->
                        check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) { "PNG encoding failed." }
                        output.fd.sync()
                    }
                    ScreenshotResult(name, file.absolutePath)
                }
            } finally {
                bitmap.recycle()
            }
        }
    }
}
