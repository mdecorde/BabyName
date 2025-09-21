/*
* Copyright (C) 2025 Baby Name Developers
* SPDX-License-Identifier: GPL-3.0-or-later
*/

package fr.hnit.babyname

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView.AdapterContextMenuInfo
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private lateinit var projectListView: ListView
    private lateinit var noBabyTextView: TextView
    private lateinit var adapter: ProjectListAdapter
    private lateinit var builder: AlertDialog.Builder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        projectListView = findViewById(R.id.projectListView)
        noBabyTextView = findViewById(R.id.noBabyTextView)

        registerForContextMenu(projectListView)

        adapter = ProjectListAdapter(this, projects)
        projectListView.adapter = adapter

        builder = AlertDialog.Builder(this, R.style.AlertDialogTheme)

        if (!database.isLoaded || !settings.isLoaded || !projects_isLoaded) {
            thread(start = true) {
                if (!settings.isLoaded) {
                    settings.load(applicationContext)
                }

                if (!database.isLoaded) {
                    database.initialize(this)
                }

                if (!projects_isLoaded) {
                    initializeProjects()
                }

                runOnUiThread {
                    adapter.notifyDataSetChanged()
                    updateNoBabyMessage()
                }
            }
        }

        updateNoBabyMessage()
    }

    private fun storeProjects() {
        for (project in projects) {
            if (!project.needSaving) {
                continue
            }

            if (!BabyNameProject.storeProject(project, this)) {
                Toast.makeText(
                    this,
                    "Error: could not save changes to babyname project: $project",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    public override fun onResume() {
        super.onResume()
        if (this::adapter.isInitialized) {
            adapter.notifyDataSetChanged()
        }

        updateNoBabyMessage()
    }

    override fun onPause() {
        storeProjects()
        super.onPause()
    }

    override fun onDestroy() {
        storeProjects()
        super.onDestroy()
    }

    private fun updateNoBabyMessage() {

        if (projects_isLoaded) {
            if (projects.isEmpty()) {
                noBabyTextView.visibility = View.VISIBLE
                projectListView.visibility = View.GONE
            } else {
                noBabyTextView.visibility = View.GONE
                projectListView.visibility = View.VISIBLE
            }
        } else {
            noBabyTextView.visibility = View.GONE
            projectListView.visibility = View.GONE
        }
    }

    private fun initializeProjects() {
        for (filename in this.fileList()) {
            if (filename.endsWith(".baby")) {
                //Log.d(this, "Loading $filename")
                try {
                    val project = BabyNameProject.readProject(filename, this)
                    if (project != null) {
                        projects.add(project)
                    } else {
                        runOnUiThread {
                            Toast.makeText(
                                this@MainActivity,
                                "Error: could not read baby name project from $filename",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        projects_isLoaded = true
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo)
        menuInflater.inflate(R.menu.menu_list, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val info = item.menuInfo as AdapterContextMenuInfo

        if (adapter.count <= info.position) return false
        val project = adapter.getItem(info.position) ?: return false

        return when (item.itemId) {
            R.id.action_edit_project -> {
                doEditProject(project)
                true
            }

            R.id.action_reset_scores -> {
                doResetScores(project)
                true
            }

            R.id.action_delete_project -> {
                doDeleteProject(project)
                true
            }

            R.id.action_clone_project -> {
                doCloneProject(project)
                true
            }

            R.id.action_top_names -> {
                doShowTop10(project)
                true
            }

            else -> super.onContextItemSelected(item)
        }
    }

    private fun doResetScores(project: BabyNameProject) {
        builder.setTitle(R.string.reset_question_title)
        builder.setMessage(R.string.reset_question_content)

        builder.setPositiveButton(R.string.yes) { dialog, _ ->
            project.scores.clear()
            project.setNeedToBeSaved(true)
            adapter.notifyDataSetChanged()
            if (!BabyNameProject.storeProject(project, this@MainActivity)) {
                Toast.makeText(
                    this@MainActivity,
                    "Error: could not save reset changes to babyname project: $project",
                    Toast.LENGTH_LONG
                ).show()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton(R.string.no) { dialog, _ ->
            dialog.dismiss()
        }

        val alert = builder.create()
        alert.show()
    }

    private fun doCloneProject(project: BabyNameProject) {
        val cloned = project.cloneProject()
        cloned.setNeedToBeSaved(true)
        projects.add(cloned)
        adapter.notifyDataSetChanged()
    }

    fun doDeleteProject(project: BabyNameProject) {
        builder.setTitle(R.string.delete_question_title)
        builder.setMessage(R.string.delete_question_content)

        builder.setPositiveButton(R.string.yes) { dialog, _ ->
            projects.remove(project)
            this@MainActivity.deleteFile(project.iD + ".baby")
            adapter.notifyDataSetChanged()
            dialog.dismiss()
        }

        builder.setNegativeButton(R.string.no) { dialog, _ ->
            dialog.dismiss()
        }

        val alert = builder.create()
        alert.show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        Log.d(this, "onCreateOptionsMenu()")

        val titles = listOf(R.string.menu_new_baby, R.string.menu_settings, R.string.menu_database, R.string.menu_about)
        for (title in titles) {
            menu.add(0, title, 0, title)
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d(this, "onOptionsItemSelected() ${item.itemId}")

        when (item.itemId) {
            R.string.menu_new_baby -> {
                doNewBaby()
            }
            R.string.menu_settings -> {
                startActivity(Intent(applicationContext, SettingsActivity::class.java))
            }
            R.string.menu_database -> {
                startActivity(Intent(applicationContext, DatabaseActivity::class.java))
            }
            R.string.menu_about -> {
                startActivity(Intent(applicationContext, AboutActivity::class.java))
            }
        }

        return super.onOptionsItemSelected(item)
    }

    fun projectToDescription(p: BabyNameProject): String {
        var text = when (p.gender) {
            BabyNameProject.GenderSelection.ALL -> getString(R.string.boy_or_girl_name)
            BabyNameProject.GenderSelection.MALE -> getString(R.string.boy_name)
            BabyNameProject.GenderSelection.FEMALE -> getString(R.string.girl_name)
            BabyNameProject.GenderSelection.NEUTRAL -> getString(R.string.neutral_name)
        }

        // sort origins for display
        val originsTranslated = ArrayList(
            p.origins.map { it -> Origins.getLocaleOrigin(applicationContext, it) }
        )

        originsTranslated.sort()

        val separator = " " + when (p.originsLogic) {
            BabyNameProject.OriginsLogic.AND -> getString(R.string.separator_and)
            BabyNameProject.OriginsLogic.OR ->  getString(R.string.separator_or)
        } + " "

        text += " " + if (originsTranslated.size == 1) {
            String.format(getString(R.string.origin_is), originsTranslated[0])
        } else if (p.origins.size > 1) {
            String.format(getString(R.string.origin_are), originsTranslated.joinToString(separator))
        } else {
            getString(R.string.no_origin)
        }

        if (p.pattern != null && ".*" != p.pattern.toString()) {
            text += " " + String.format(getString(R.string.matches_with), p.pattern)
        }

        //Log.d(this, "p.nexts.size: ${p.nexts.size}, p.scores.size: ${p.scores.size} p.nextsIndex: ${p.nextsIndex}, remainingNames: ${p.nexts.size - p.nextsIndex}")

        if (p.nexts.size == 1) {
            text += " " + getString(R.string.one_remaining_name)
        } else {
            val remainingNames = p.nexts.size - p.nextsIndex
            text += " " + String.format(getString(R.string.remaining_names), remainingNames)
        }

        val bestName = p.getBest()
        if (p.scores.isNotEmpty() && bestName != null) {
            text += " " + String.format(getString(R.string.best_match_is), bestName.name)
        }

        return text
    }

    fun doShowTop10(project: BabyNameProject) {
        val ids = project.getTop10()

        val buffer = StringBuffer()
        for (id in ids) {
            buffer.append("${database.get(id).name}: ${project.scores[id]}\n")
        }

        if (ids.isEmpty()) {
            buffer.append(getString(R.string.no_name_rated))
        }

        builder.setTitle(R.string.top_title)
        builder.setMessage(buffer.toString())

        builder.setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }

        if (ids.isNotEmpty()) {
            builder.setNegativeButton(R.string.copy) { _, _ ->
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("baby top10", buffer.toString())
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, R.string.text_copied, Toast.LENGTH_LONG).show()
            }
        }

        val alert = builder.create()
        alert.show()

        //Toast.makeText(this, buffer.toString(), Toast.LENGTH_LONG).show();
    }

    fun doFlipSearch(project: BabyNameProject) {
        val intent = Intent(this, FlipSearchActivity::class.java)
        intent.putExtra(PROJECT_EXTRA, projects.indexOf(project))
        startActivity(intent)
    }

    fun doScrollSearch(project: BabyNameProject) {
        val intent = Intent(this, ScrollSearchActivity::class.java)
        intent.putExtra(PROJECT_EXTRA, projects.indexOf(project))
        startActivity(intent)
    }

    fun doEditProject(project: BabyNameProject?) {
        val intent = Intent(this, EditActivity::class.java)
        if (project != null) {
            intent.putExtra(PROJECT_EXTRA, projects.indexOf(project))
        }
        startActivity(intent)
    }

    private fun doNewBaby() {
        Toast.makeText(this, R.string.new_baby, Toast.LENGTH_LONG).show()
        doEditProject(null)
    }

    companion object {
        const val PROJECT_EXTRA = "project_position"
        val settings = BabyNameSettings()
        val database = BabyNameDatabase()
        val projects = ArrayList<BabyNameProject>()
        var projects_isLoaded = false
    }
}
