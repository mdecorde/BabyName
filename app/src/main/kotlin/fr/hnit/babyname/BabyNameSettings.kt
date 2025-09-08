package fr.hnit.babyname

import android.content.Context
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.Charset

class BabyNameSettings() {
	var isLoaded = false
    var version = BuildConfig.VERSION_NAME
    var nextOnRating = false

    fun load(ctx: Context) {
        if (!fileExists(ctx, SETTINGS_FILE)) {
            return
        }

        try {
            val stringData = readInternalFile(ctx, SETTINGS_FILE)
            val obj = JSONObject(
                String(stringData, Charset.forName("UTF-8"))
            )
            nextOnRating = obj.optBoolean("next_on_rating", nextOnRating)
            version = obj.optString("version", version)
            isLoaded = true
        } catch (e: Exception) {
          e.printStackTrace()
        }
	}

    fun save(ctx: Context) {
        try {
            val obj = JSONObject()
            obj.put("version", version)
            obj.put("next_on_rating", nextOnRating)
            writeInternalFile(ctx, SETTINGS_FILE, obj.toString().toByteArray())
        } catch (e: Exception) {
            e.printStackTrace()
        }
	}

    companion object {
        private const val SETTINGS_FILE = "settings.json"

        fun fileExists(ctx: Context, fileName: String) : Boolean {
            val file = File(ctx.filesDir, fileName)
            return file.exists() && file.isFile
        }

        fun writeInternalFile(ctx: Context, fileName: String, dataArray: ByteArray) {
            val file = File(ctx.filesDir, fileName)
            if (file.exists() && file.isFile) {
                if (!file.delete()) {
                    throw IOException("Failed to delete existing file: $fileName")
                }
            }
            file.createNewFile()
            val fos = FileOutputStream(file)
            fos.write(dataArray)
            fos.close()
        }

        fun readInternalFile(ctx: Context, fileName: String): ByteArray {
            val file = File(ctx.filesDir, fileName)
            if (!file.exists() || !file.isFile) {
                throw IOException("File does not exist: $fileName")
            }
            val fis = FileInputStream(file)
            var nRead: Int
            val dataArray = ByteArray(16384)
            val buffer = ByteArrayOutputStream()
            while (fis.read(dataArray, 0, dataArray.size).also { nRead = it } != -1) {
                buffer.write(dataArray, 0, nRead)
            }
            fis.close()
            return buffer.toByteArray()
        }
	}
}
