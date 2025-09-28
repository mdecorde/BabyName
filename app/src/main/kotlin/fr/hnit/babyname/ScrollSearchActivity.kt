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
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import fr.hnit.babyname.BabyNameProject.Companion.DROP_RATE_PERCENT

class ScrollSearchActivity : AppCompatActivity() {
    private lateinit var project: BabyNameProject
    private lateinit var recyclerView: RecyclerView
    private lateinit var scrollAdapter: ScrollSearchAdapter
    private lateinit var sortPatternTextView: EditText
    private lateinit var counterTextView: TextView
    private lateinit var dropButton: Button
    private lateinit var sortButton: Button
    private lateinit var builder: AlertDialog.Builder

    private var nexts = ArrayList<Int>()
    private var scores = HashMap<Int, Float>()
    private var needSaving = false

    private var sortPattern = emptyList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scroll_search)

        sortPatternTextView = findViewById(R.id.filter_text)
        counterTextView = findViewById(R.id.counter_text)
        dropButton = findViewById(R.id.dropButton)
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

        // create a copy
        nexts = ArrayList(project.nexts)
        scores = HashMap(project.scores)
        needSaving = project.needSaving

        scrollAdapter = ScrollSearchAdapter(this, nexts, scores)

        recyclerView.adapter = scrollAdapter

        sortNexts()

        dropButton.text = String.format(getString(R.string.button_drop_percent), BabyNameProject.DROP_RATE_PERCENT)
        dropButton.setOnClickListener {
            dropDialog()
        }

        sortButton.setOnClickListener {
            sortNexts()
        }

        sortPatternTextView.doOnTextChanged { text, start, count, after ->
            //Log.d(this, text.toString())
            sortPattern = text.toString().split("\\s".toRegex())

            sortNexts()
        }

        if (nexts.size > 10) {
            dropButton.visibility = View.VISIBLE
        } else {
            dropButton.visibility = View.GONE
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
                        recyclerView.scrollToPosition(position)
                        scrollAdapter.notifyItemInserted(position)
                    }

                    snackbar.setActionTextColor(Color.YELLOW)
                    snackbar.show()
                }
            }
        }).attachToRecyclerView(recyclerView)
    }

    private fun dropLast() {
        if (nexts.size > 10) {
            val amountToRemove = ((DROP_RATE_PERCENT *  nexts.size) / 100)

            // keep scores updated
            for (idx in nexts.takeLast(amountToRemove)) {
                scores.remove(idx)
            }

            nexts = ArrayList(nexts.dropLast(amountToRemove))
            needSaving = true
        }
    }

    private fun dropDialog() {
        val amountToRemove = ((BabyNameProject.DROP_RATE_PERCENT * nexts.size) / 100)

        builder.setTitle(R.string.dialog_drop_title)
        builder.setMessage(String.format(getString(R.string.dialog_drop_message), amountToRemove, nexts.size))

        builder.setPositiveButton(R.string.yes) { dialog, _ ->
            dropLast()
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
    private fun getOriginScore(target: BabyName): Int {
        var maxScore = 0
        for (id in scores.keys) {
            var score = 0
            val source = MainActivity.database.get(id)
            for (targetOrigin in target.origins) {
                if (targetOrigin in source.origins) {
                    score += project.getIntScore(id)
                }
            }

            if (score > maxScore) {
                maxScore = score
            }
        }
        return maxScore
    }

    // for sorting
    private fun getSoundexScore(target: BabyName): Int {
        var maxScore = 0
        for (id in scores.keys) {
            val source = MainActivity.database.get(id)
            if (target.soundex == source.soundex) {
                val score = project.getIntScore(id)
                if (score > maxScore) {
                    maxScore = score
                }
            }
        }
        return maxScore
    }

    // Sort names by match length first, rating score second, soundex score third
    private fun sortNexts() {
        nexts.sortWith { i: Int, j: Int ->
            val a = MainActivity.database.get(i)
            val b = MainActivity.database.get(j)

            val aMatchLength = getMatchPercent(a.name, sortPattern)
            val bMatchLength = getMatchPercent(b.name, sortPattern)

            if (aMatchLength != bMatchLength) {
                return@sortWith (bMatchLength - aMatchLength)
            } else {
                val aScore = ((scores.get(a.id) ?: 0f) * 2f).toInt()
                val bScore = ((scores.get(b.id) ?: 0f) * 2f).toInt()

                if (aScore != bScore) {
                    return@sortWith (bScore - aScore)
                } else {
                    val aSoundexScore = getSoundexScore(a)
                    val bSoundexScore = getSoundexScore(b)

                    if (aSoundexScore != bSoundexScore) {
                        return@sortWith bSoundexScore - aSoundexScore
                    } else {
                        val aOriginScore = getOriginScore(a)
                        val bOriginScore = getOriginScore(b)
                        return@sortWith (bOriginScore - aOriginScore)
                    }
                }
            }
        }

        counterTextView.text = if (sortPattern.isEmpty()) {
            nexts.size.toString()
        } else {
            var counter = 0
            for (next in nexts) {
                val item = MainActivity.database.get(next)
                if (isMatch(item.name, sortPattern)) {
                    counter += 1
                }
            }
            counter.toString()
        }

        scrollAdapter.notifyDataSetChanged()
        recyclerView.scrollToPosition(0)
    }

    public override fun onStop() {
        project.nexts = nexts
        project.scores = scores
        project.needSaving = needSaving

        MainActivity.instance?.updateView()

        super.onStop()
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
