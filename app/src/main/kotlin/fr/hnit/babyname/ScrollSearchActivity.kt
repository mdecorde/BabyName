/*
* Copyright (C) 2025 Baby Name Developers
* SPDX-License-Identifier: GPL-3.0-or-later
*/

package fr.hnit.babyname

import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import fr.hnit.babyname.BabyNameProject.Companion.DROP_RATE_PERCENT

class ScrollSearchActivity : AppCompatActivity() {
    private lateinit var project: BabyNameProject
    private lateinit var recyclerView: RecyclerView
    private lateinit var scrollAdapter: ScrollSearchAdapter
    private lateinit var sortPatternTextView: EditText
    private lateinit var counterTextView: TextView
    private lateinit var sortButton: Button
    private lateinit var builder: AlertDialog.Builder

    private var nexts = ArrayList<Int>()
    private var scores = HashMap<Int, Float>()
    private var needSaving = false

    private var sortPattern = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scroll_search)

        sortPatternTextView = findViewById(R.id.filter_text)
        counterTextView = findViewById(R.id.counter_text)
        sortButton = findViewById(R.id.sortButton)
        recyclerView = findViewById(R.id.recyclerview)

        builder = AlertDialog.Builder(this, R.style.AlertDialogTheme)

        recyclerView.layoutManager = LinearLayoutManager(this)

        val index = intent.getIntExtra(MainActivity.PROJECT_EXTRA, 0)
        if (index >= 0 && MainActivity.projects.size > index) {
            project = MainActivity.projects[index]
        } else {
            finish()
            return
        }

        if (project.nexts.isEmpty()) {
            Toast.makeText(applicationContext, R.string.message_no_name_to_review, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // create a copy
        nexts = ArrayList(project.nexts)
        scores = HashMap(project.scores)
        needSaving = project.needSaving

        scrollAdapter = ScrollSearchAdapter(this, project, nexts, scores)

        recyclerView.adapter = scrollAdapter

        sortNexts()

        sortButton.setOnClickListener {
            sortNexts()
        }

        sortPatternTextView.doOnTextChanged { text, start, count, after ->
            // Split text into array list.
            sortPattern = ArrayList(text.toString().split("\\s".toRegex()))

            sortNexts()
        }

        scrollAdapter.onItemClick = { babyName: BabyName ->
            showNameDetails(babyName)
        }

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                if (direction == ItemTouchHelper.LEFT) {
                    val position = viewHolder.adapterPosition
                    val nameId = nexts.removeAt(position)
                    val scoreBackup = scores.remove(nameId)

                    val needSavingBackup =  needSaving
                    needSaving = true

                    updateCounter()
                    scrollAdapter.notifyItemRemoved(position)

                    val snackbar = Snackbar.make(
                        recyclerView,
                        R.string.name_was_removed,
                        Snackbar.LENGTH_LONG
                    )
                    snackbar.setAction(R.string.undo) {
                        nexts.add(position, nameId)
                        needSaving = needSavingBackup
                        if (scoreBackup != null) {
                            scores[nameId] = scoreBackup
                        }
                        updateCounter()
                        recyclerView.scrollToPosition(position)
                        scrollAdapter.notifyItemInserted(position)
                    }

                    snackbar.setActionTextColor(Color.YELLOW)
                    snackbar.show()
                }
            }
        }).attachToRecyclerView(recyclerView)
    }

    private fun dropUnratedDialog() {
        val amountToRemove = nexts.count { !scores.containsKey(it) }

        builder.setTitle(R.string.dialog_drop_unrated_title)
        builder.setMessage(String.format(getString(R.string.dialog_drop_unrated_message), amountToRemove, nexts.size))

        builder.setPositiveButton(R.string.yes) { dialog, _ ->
            if (amountToRemove > 0) {
                nexts.removeAll { !scores.containsKey(it) }
                needSaving = true

                updateCounter()

                scrollAdapter.notifyDataSetChanged()
                recyclerView.scrollToPosition(0)
            }
            dialog.dismiss()
        }

        builder.setNegativeButton(R.string.no) { dialog, which ->
            dialog.dismiss()
        }

        val alert = builder.create()
        alert.show()
    }

    private fun drop20PCDialog() {
        val amountToRemove = ((DROP_RATE_PERCENT * nexts.size) / 100)

        builder.setTitle(R.string.dialog_drop_worst_title)
        builder.setMessage(String.format(getString(R.string.dialog_drop_worst_message), amountToRemove, nexts.size))

        builder.setPositiveButton(R.string.yes) { dialog, _ ->
            if (amountToRemove > 0) {
                // sort by score, lowest scores last
                val nextCopy = ArrayList(nexts)
                nextCopy.sortWith { i1: Int, i2: Int ->
                    (2 * ((scores[i2] ?: 0F) - (scores[i1] ?: 0F))).toInt()
                }
                val dropSet = HashSet(nextCopy.takeLast(amountToRemove))

                // keep scores updated
                for (idx in dropSet) {
                    scores.remove(idx)
                }

                nexts.removeAll { dropSet.contains(it) }
                needSaving = true

                updateCounter()

                scrollAdapter.notifyDataSetChanged()
                recyclerView.scrollToPosition(0)
            }

            dialog.dismiss()
        }

        builder.setNegativeButton(R.string.no) { dialog, which ->
            dialog.dismiss()
        }

        val alert = builder.create()
        alert.show()
    }

    fun onRatingChangeListener(babyName: BabyName, rating: Float) {
        val oldRating = scores[babyName.id] ?: 0F
        if (rating != oldRating) {
            //Log.d(this, "rating changed for ${babyName.name}: $oldScore => $newScore")
            scores[babyName.id] = rating
            needSaving = true
        }
    }

    var nextsLock = ReentrantLock()

    // Sort names by match length first, rating score second, soundex score third.
    private fun sortNexts() {
        // Make a thread-exclusive copy
        val sortPatternCopy = sortPattern.toMutableList()
        val scoresCopy = HashMap(scores)

        thread (start = true) {
            nextsLock.lock()
            try {
                if (sortPatternCopy != sortPattern || scoresCopy != scores) {
                    // Sort parameters have changed => abort.
                    return@thread
                }

                nexts.sortWith { i: Int, j: Int ->
                    val a = MainActivity.database.getName(i)
                    val b = MainActivity.database.getName(j)

                    val aMatchLength = getMatchPercent(a.name, sortPatternCopy)
                    val bMatchLength = getMatchPercent(b.name, sortPatternCopy)

                    if (aMatchLength != bMatchLength) {
                        return@sortWith (bMatchLength - aMatchLength)
                    } else {
                        val aScore = ((scoresCopy[a.id] ?: 0f) * 2f).toInt()
                        val bScore = ((scoresCopy[b.id] ?: 0f) * 2f).toInt()

                        if (aScore != bScore) {
                            return@sortWith (bScore - aScore)
                        } else {
                            val aSoundexScore = getSoundexScore(scoresCopy, a)
                            val bSoundexScore = getSoundexScore(scoresCopy,b)

                            if (aSoundexScore != bSoundexScore) {
                                return@sortWith bSoundexScore - aSoundexScore
                            } else {
                                val aOriginScore = getOriginScore(scoresCopy, a)
                                val bOriginScore = getOriginScore(scoresCopy, b)
                                return@sortWith (bOriginScore - aOriginScore)
                            }
                        }
                    }
                }

                runOnUiThread {
                    updateCounter()

                    scrollAdapter.notifyDataSetChanged()
                    recyclerView.scrollToPosition(0)
                }
            } finally {
                nextsLock.unlock()
            }
        }
    }

    private fun updateCounter() {
        counterTextView.text = if (sortPattern.isEmpty()) {
            nexts.size.toString()
        } else {
            var counter = 0
            for (next in nexts) {
                val item = MainActivity.database.getName(next)
                if (isMatch(item.name, sortPattern)) {
                    counter += 1
                }
            }
            counter.toString()
        }
    }

    public override fun onStop() {
        if (needSaving) {
            // update project.nexts but keep order
            val newNextsSet = HashSet(nexts)
            val newNexts =  project.nexts.filter { newNextsSet.contains(it) }

            // fixup project.nextsIndex (in case the value got deleted from nexts)
            if (project.nextsIndex >= 0 && project.nextsIndex < project.nexts.size) {
                val oldNextId = project.nexts[project.nextsIndex]
                project.nextsIndex = newNexts.indexOf(oldNextId)
            } else {
                project.nextsIndex = -1
            }

            project.nexts = newNexts
            project.scores = scores
            project.needSaving = needSaving

            MainActivity.instance?.updateView()
        }

        super.onStop()
    }

    fun showNameDetails(name: BabyName) {
        builder
            .setTitle(name.name)
            .setMessage(project.getLongOriginsString(applicationContext, name))
            .setPositiveButton(R.string.ok) { dialog, id ->
                dialog.dismiss()
            }

        val dialog = builder.create()
        dialog.show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        Log.d(this, "onCreateOptionsMenu()")

        val titles = listOf(R.string.menu_drop_unrated, R.string.menu_drop_worst_20pc, R.string.menu_abort)
        for (title in titles) {
            menu.add(0, title, 0, title)
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d(this, "onOptionsItemSelected() ${item.itemId}")

        when (item.itemId) {
            R.string.menu_drop_unrated -> {
                dropUnratedDialog()
            }
            R.string.menu_drop_worst_20pc -> {
                drop20PCDialog()
            }
            R.string.menu_abort -> {
                needSaving = false
                finish()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    fun getHighlightedName(name: String): SpannableString {
        val ss = SpannableString(name)
        if (sortPattern.isEmpty()) {
            return ss
        } else {
            val matches = getMatches(name, sortPattern)
            for (m in matches) {
                ss.setSpan(BackgroundColorSpan(Color.YELLOW), m.begin, m.begin + m.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            return ss
        }
    }

    companion object {
        data class Match(val begin: Int, val length: Int)

        // for sorting
        private fun getMatchPercent(value: String, patterns: List<String>): Int {
            if (patterns.isEmpty()) {
                return 0
            } else {
                val matches = getMatches(value, patterns)
                return ((10000 * matches.fold(0){ acc, next -> acc + next.length }) / value.length) / 100
            }
        }

        // for sorting
        private fun getOriginScore(scores: HashMap<Int, Float>, target: BabyName): Int {
            var maxScore = 0
            for (id in scores.keys) {
                var score = 0
                val source = MainActivity.database.getName(id)
                for (targetOrigin in target.origins) {
                    if (targetOrigin in source.origins) {
                        score += ((scores.get(id) ?: 0f) * 2f).toInt()
                    }
                }

                if (score > maxScore) {
                    maxScore = score
                }
            }
            return maxScore
        }

        // for sorting
        private fun getSoundexScore(scores: HashMap<Int, Float>, target: BabyName): Int {
            var maxScore = 0
            for (id in scores.keys) {
                val source = MainActivity.database.getName(id)
                if (target.soundex == source.soundex) {
                    val score = ((scores.get(id) ?: 0f) * 2f).toInt()
                    if (score > maxScore) {
                        maxScore = score
                    }
                }
            }
            return maxScore
        }

        private fun isMatch(value: String, patterns: List<String>): Boolean {
            for (p in patterns) {
                if (value.indexOf(p) == -1) {
                    return false
                }
            }
            return true
        }

        // return array of matching ranges as array of begin,length pairs
        private fun getMatches(value: String, patterns: List<String>): List<Match> {
            val caseSensitive = patterns.any { it.isNotEmpty() && it[0].isUpperCase() }
            val value = if (caseSensitive) {
                value
            } else {
                value.lowercase()
            }

            // find matching ranges
            val matches = mutableListOf<Match>()
            for (p in patterns) {
                val i = value.indexOf(p)
                if (i == -1) return emptyList()
                matches.add(Match(i, p.length))
            }

            matches.sortBy { it.begin  }

            // merge overlapping ranges
            var prev : Match? = null
            val ranges = mutableListOf<Match>()
            for (m in matches) {
                if (prev != null && m.begin <= prev.begin + prev.length) {
                    val length = prev.length.coerceAtLeast(m.begin + m.length - prev.begin);
                    prev = Match(prev.begin, length)
                } else {
                    ranges.add(m)
                    prev = m
                }
            }
            return ranges
        }
    }
}
