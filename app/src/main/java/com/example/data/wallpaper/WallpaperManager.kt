package com.example.data.wallpaper

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max

data class LocalMediaImage(
    val uri: Uri,
    val name: String
)

class WallpaperManager(private val context: Context) {
    companion object {
        const val WALLPAPER_FILE_NAME = "background.jpg"
        const val MAX_WIDTH = 2560
        const val MAX_HEIGHT = 1440
    }

    private val wallpaperDiskFile: File
        get() = File(context.filesDir, WALLPAPER_FILE_NAME)

    fun hasCustomWallpaper(): Boolean {
        return wallpaperDiskFile.exists() && wallpaperDiskFile.length() > 0
    }

    fun getCustomWallpaperFile(): File? {
        val file = wallpaperDiskFile
        return if (file.exists() && file.canRead() && file.length() > 0) file else null
    }

    suspend fun saveCustomWallpaper(sourceUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            // First decode bounds
            var inputStream: InputStream? = context.contentResolver.openInputStream(sourceUri)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            val srcWidth = options.outWidth
            val srcHeight = options.outHeight
            if (srcWidth <= 0 || srcHeight <= 0) return@withContext false

            // Compute sample size
            var sampleSize = 1
            val maxDim = max(srcWidth.toFloat() / MAX_WIDTH, srcHeight.toFloat() / MAX_HEIGHT)
            if (maxDim > 1.0f) {
                sampleSize = maxDim.toInt()
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            inputStream = context.contentResolver.openInputStream(sourceUri)
            val decodedBitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            inputStream?.close()

            if (decodedBitmap == null) return@withContext false

            // Final resize if still larger than max dimensions
            val finalBitmap = if (decodedBitmap.width > MAX_WIDTH || decodedBitmap.height > MAX_HEIGHT) {
                val scale = minOf(
                    MAX_WIDTH.toFloat() / decodedBitmap.width,
                    MAX_HEIGHT.toFloat() / decodedBitmap.height
                )
                val destW = (decodedBitmap.width * scale).toInt()
                val destH = (decodedBitmap.height * scale).toInt()
                val scaled = Bitmap.createScaledBitmap(decodedBitmap, destW, destH, true)
                if (scaled != decodedBitmap) {
                    decodedBitmap.recycle()
                }
                scaled
            } else {
                decodedBitmap
            }

            val tempFile = File(context.filesDir, "temp_bg.jpg")
            FileOutputStream(tempFile).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            finalBitmap.recycle()

            if (tempFile.renameTo(wallpaperDiskFile) || (wallpaperDiskFile.delete() && tempFile.renameTo(wallpaperDiskFile))) {
                true
            } else {
                tempFile.copyTo(wallpaperDiskFile, overwrite = true)
                tempFile.delete()
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun resetCustomWallpaper(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (wallpaperDiskFile.exists()) {
                wallpaperDiskFile.delete()
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun loadDeviceImages(): List<LocalMediaImage> = withContext(Dispatchers.IO) {
        val result = mutableListOf<LocalMediaImage>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME
        )
        val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"

        try {
            context.contentResolver.query(collection, projection, null, null, sortOrder)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)

                var count = 0
                while (cursor.moveToNext() && count < 60) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Image $id"
                    val contentUri = ContentUris.withAppendedId(collection, id)
                    result.add(LocalMediaImage(contentUri, name))
                    count++
                }
            }
        } catch (_: Exception) {}

        result
    }
}
