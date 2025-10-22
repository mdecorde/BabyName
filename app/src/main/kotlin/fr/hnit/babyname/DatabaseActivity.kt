package fr.hnit.babyname

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.concurrent.thread

class DatabaseActivity : AppCompatActivity() {
    private lateinit var builder: AlertDialog.Builder
    private lateinit var addButton: Button
    private lateinit var replaceButton: Button
    private lateinit var exportButton: Button
    private lateinit var resetButton: Button

    private fun showErrorMessage(title: String, message: String) {
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton(android.R.string.ok, null)
        builder.show()
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_database)

        builder = AlertDialog.Builder(this, R.style.AlertDialogTheme)

        addButton = findViewById(R.id.AddButton)
        replaceButton = findViewById(R.id.ReplaceButton)
        exportButton = findViewById(R.id.ExportButton)
        resetButton = findViewById(R.id.ResetButton)

        addButton.setOnClickListener { v: View? ->
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "text/*"
            addFileLauncher.launch(intent)
        }

        replaceButton.setOnClickListener { v: View? ->
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "text/*"
            replaceFileLauncher.launch(intent)
        }

        exportButton.setOnClickListener {
            val timestamp = SimpleDateFormat("yyyy-MM-dd-hh-mm").format(Date());
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.putExtra(Intent.EXTRA_TITLE, "baby-names-${timestamp}.csv")
            intent.type = "text/*"
            exportFileLauncher.launch(intent)
        }

        resetButton.setOnClickListener {
            builder
                .setTitle("Reset")
                .setMessage("Really reset Database?")
                .setPositiveButton(R.string.button_reset) { dialog, id ->
                    resetDatabase()
                }.setNegativeButton(R.string.button_abort) { dialog, id ->
                    dialog.dismiss()
                }

            val dialog = builder.create()
            dialog.show()
        }
    }

    private var addFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val intent = result.data ?: return@registerForActivityResult
            val uri = intent.data ?: return@registerForActivityResult

            enableButtons(false)

            thread(start = true) {
                importDatabase(uri, true)
                runOnUiThread { enableButtons(true) }
            }
        }
    }

    private var replaceFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val intent = result.data ?: return@registerForActivityResult
            val uri = intent.data ?: return@registerForActivityResult

            enableButtons(false)

            thread(start = true) {
                importDatabase(uri, false)
                runOnUiThread { enableButtons(true) }
            }
        }
    }

    private var exportFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val intent = result.data ?: return@registerForActivityResult
            val uri: Uri = intent.data ?: return@registerForActivityResult

            enableButtons(false)

            thread(start = true) {
                try {
                    val names = MainActivity.database.getAll()
                    val csv = BabyNameDatabase.serializeNames(names)
                    val count = MainActivity.database.size()

                    val fos = contentResolver.openOutputStream(uri)
                    fos!!.write(csv.toByteArray())
                    fos.close()

                    runOnUiThread {
                        Toast.makeText(this, "Exported $count entries.", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        showErrorMessage("Error", e.toString())
                    }
                }

                runOnUiThread {
                    enableButtons(true)
                }
            }
        }
    }

    private fun resetDatabase() {
        enableButtons(false)

        thread(start = true) {
            try {
                MainActivity.database.resetDatabase(applicationContext)
                runOnUiThread {
                    Toast.makeText(this, "Database reset done.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    showErrorMessage("Error", e.toString())
                }
            }

            runOnUiThread {
                enableButtons(true)
            }
        }
    }

    private fun enableButtons(enable: Boolean) {
        for (button in listOf(addButton, replaceButton, exportButton, resetButton)) {
            button.alpha = if (enable) { 1.0f } else { .5f }
            button.isClickable = enable
        }
    }

    private fun importDatabase(uri: Uri, doAdd: Boolean) {
        try {
            val byteData = readFile(this, uri)
            val stringData = String(byteData, 0, byteData.size)
            val oldCount = MainActivity.database.size()
            val names = BabyNameDatabase.deserializeNames(stringData)
            if (doAdd) {
                MainActivity.database.addNames(names)
                val newCount = MainActivity.database.size()
                runOnUiThread {
                    Toast.makeText(this, "Added ${newCount - oldCount} new names. $newCount total.", Toast.LENGTH_LONG).show()
                }
            } else {
                MainActivity.database.setNames(names)
                val newCount = MainActivity.database.size()
                runOnUiThread {
                    Toast.makeText(this, "Replaced database with $newCount names.", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            runOnUiThread {
                showErrorMessage("Error", e.toString())
            }
        }
    }

    companion object {
        private fun getFileSize(ctx: Context, uri: Uri?): Long {
            val cursor = ctx.contentResolver.query(uri!!, null, null, null, null)
            cursor!!.moveToFirst()
            val size = cursor.getLong(cursor.getColumnIndex(OpenableColumns.SIZE).coerceAtLeast(0))
            cursor.close()
            return size
        }

        private fun readFile(ctx: Context, uri: Uri): ByteArray {
            val size = getFileSize(ctx, uri).toInt()
            val iStream = ctx.contentResolver.openInputStream(uri)
            val buffer = ByteArrayOutputStream()
            var nRead: Int
            val data = ByteArray(size)
            while (iStream!!.read(data, 0, data.size).also { nRead = it } != -1) {
                buffer.write(data, 0, nRead)
            }
            iStream.close()
            return data
        }
    }
}
