/*
* Copyright (C) 2025 Baby Name Developers
* SPDX-License-Identifier: GPL-3.0-or-later
*/

package fr.hnit.babyname

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import fr.hnit.babyname.BabyNameProject.Companion.DROP_RATE_PERCENT

open class FlipSearchActivity : AppCompatActivity() {
    private lateinit var project: BabyNameProject
    private var currentBabyName: BabyName? = null

    private var nexts = ArrayList<Int>()
    private var scores = HashMap<Int, Float>()
    private var nextsIndex = -1
    private var needSaving = false

    private lateinit var backgroundImage: ImageView
    private lateinit var nextButton: Button
    private lateinit var removeButton: Button
    private lateinit var previousButton: Button
    private lateinit var rateBar: RatingBar
    private lateinit var nameText: TextView
    private lateinit var extraText: TextView
    private lateinit var progressCounterText: TextView
    private lateinit var progressPercentText: TextView
    private lateinit var buttonLayout: LinearLayout
    private lateinit var builder: AlertDialog.Builder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flip_search)

        backgroundImage = findViewById(R.id.imageView)
        if (Math.random() > 0.5) {
            backgroundImage.setImageResource(R.drawable.tuxbaby)
        } else {
            backgroundImage.setImageResource(R.drawable.tuxbaby2)
        }

        nextButton = findViewById(R.id.next_button)
        removeButton = findViewById(R.id.remove_button)
        previousButton = findViewById(R.id.previous_button)
        rateBar = findViewById(R.id.rate_bar)
        nameText = findViewById(R.id.name_text)
        extraText = findViewById(R.id.extra_text)
        progressCounterText = findViewById(R.id.progress_counter)
        progressPercentText = findViewById(R.id.progress_percent)
        buttonLayout = findViewById(R.id.buttons)

        nextButton.setOnClickListener { nextName() }
        removeButton.setOnClickListener { removeName() }
        previousButton.setOnClickListener { previousName() }

        builder = AlertDialog.Builder(this, R.style.AlertDialogTheme)

        val index = intent.getIntExtra(MainActivity.PROJECT_EXTRA, 0)
        if (index >= 0 && MainActivity.projects.size > index) {
            project = MainActivity.projects[index]
        }

        // create a copy
        nexts = ArrayList(project.nexts)
        scores = HashMap(project.scores)
        nextsIndex = project.nextsIndex
        needSaving = project.needSaving

        // make nextsIndex valid
        if ((nextsIndex < 0 || nextsIndex >= nexts.size) && nexts.isNotEmpty()) {
            nextsIndex = 0
        }

        currentBabyName = getCurrentName()

        if (currentBabyName == null) {
            Toast.makeText(applicationContext, R.string.message_no_name_to_review, Toast.LENGTH_LONG).show()
            finish()
        } else {
            updateName()
        }

        rateBar.onRatingBarChangeListener = RatingBar.OnRatingBarChangeListener {
                ratingBar: RatingBar, rating: Float, fromUser: Boolean ->
            val babyName = currentBabyName
            if (fromUser && babyName != null) {
                scores[babyName.id] = rating

                Toast.makeText(
                    applicationContext,
                    String.format(
                        getString(R.string.name_rated_score),
                        babyName.name,
                        rating
                    ),
                    Toast.LENGTH_SHORT
                ).show()

                needSaving = true

                if (MainActivity.settings.nextOnRating) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (babyName == currentBabyName) {
                            nextName()
                        }
                    }, 500)
                }
            }
        }
    }

    private fun updateButtons() {
        fun enableButton(button: Button, isEnabled: Boolean) {
            button.isEnabled = isEnabled
            button.alpha = if (isEnabled) 1f else 0.5f
        }

        if (nextsIndex == 0) {
            // first name
            enableButton(previousButton, false)
            enableButton(nextButton, true)
        } else if ((nextsIndex + 1) == nexts.size) {
            // last name
            enableButton(previousButton, true)
            enableButton(nextButton, false)
        } else {
            enableButton(previousButton, true)
            enableButton(nextButton, true)
        }
    }

    private fun updateName() {
        updateButtons()

        val babyName = currentBabyName
        if (babyName != null) {
            nameText.text = babyName.name

            extraText.text = babyName.getMetaString(applicationContext)

            val currentNumber = nextsIndex + 1
            progressCounterText.text = String.format("(%d/%d)", currentNumber, nexts.size)
            progressPercentText.text = String.format("%d%%", (100 * currentNumber) / nexts.size)

            // set existing score or default to 0
            rateBar.rating = scores[babyName.id] ?: 0F
        } else {
            // should only happen when all names have been removed
            if (nexts.isEmpty()) {
                Toast.makeText(applicationContext, R.string.message_no_name_to_review, Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun nextName() {
        currentBabyName = getNextName()
        updateName()
    }

    private fun removeName() {
        val nameId = nexts.removeAt(nextsIndex)
        val scoreBackup = scores.remove(nameId)
        val nextsIndexBackup = nextsIndex
        val needSavingBackup = needSaving
        nextsIndex = nextsIndex.coerceAtMost(nexts.size - 1)

        needSaving = true

        currentBabyName = getCurrentName()
        updateName()

        val snackbar = Snackbar.make(
            buttonLayout,
            R.string.name_was_removed,
            Snackbar.LENGTH_LONG
        )
        snackbar.setAction(R.string.undo) {
            nexts.add(nextsIndexBackup, nameId)
            nextsIndex = nextsIndexBackup
            needSaving = needSavingBackup
            if (scoreBackup != null) {
                scores[nameId] = scoreBackup
            }

            currentBabyName = getCurrentName()
            updateName()
        }

        snackbar.setActionTextColor(Color.YELLOW)
        snackbar.show()
    }

    private fun getCurrentName(): BabyName? {
        if (nextsIndex >= 0 && nextsIndex < nexts.size) {
            return MainActivity.database.get(nexts[nextsIndex])
        } else {
            return null
        }
    }

    private fun getPreviousName(): BabyName? {
        if (nextsIndex > 0 && nextsIndex <= nexts.size) {
            nextsIndex -= 1
            return MainActivity.database.get(nexts[nextsIndex])
        } else {
            return null
        }
    }

    private fun getNextName(): BabyName? {
        if (nextsIndex >= -1 && (nextsIndex + 1) < nexts.size) {
            needSaving = true
            nextsIndex += 1
            return MainActivity.database.get(nexts[nextsIndex])
        } else {
            return null
        }
    }

    private fun previousName() {
        currentBabyName = getPreviousName()
        updateName()
    }

    private fun dropUnratedDialog() {
        val newNexts = nexts.filter { scores.containsKey(it) } as ArrayList<Int>
        val amountToRemove = nexts.size - newNexts.size

        builder.setTitle(R.string.dialog_drop_unrated_title)
        builder.setMessage(String.format(getString(R.string.dialog_drop_unrated_message), amountToRemove, nexts.size))

        builder.setPositiveButton(R.string.yes) { dialog, _ ->
            if (amountToRemove > 0) {
                nexts = newNexts
                nextsIndex = -1
                needSaving = true
                nextName()
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

                // jump to first
                nexts = nexts.filter { !dropSet.contains(it) } as ArrayList<Int>
                nextsIndex = -1
                needSaving = true
                nextName()
            }

            dialog.dismiss()
        }

        builder.setNegativeButton(R.string.no) { dialog, which ->
            dialog.dismiss()
        }

        val alert = builder.create()
        alert.show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        Log.d(this, "onCreateOptionsMenu()")

        val titles = listOf(R.string.menu_shuffle, R.string.menu_drop_unrated, R.string.menu_drop_worst_20pc,
            R.string.menu_jump_start, R.string.menu_jump_end, R.string.menu_abort)
        for (title in titles) {
            menu.add(0, title, 0, title)
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d(this, "onOptionsItemSelected() ${item.itemId}")

        when (item.itemId) {
            R.string.menu_shuffle -> {
                nexts.shuffle()
                nextsIndex = -1
                needSaving = true
                nextName()
            }
            R.string.menu_drop_unrated -> {
                dropUnratedDialog()
            }
            R.string.menu_drop_worst_20pc -> {
                drop20PCDialog()
            }
            R.string.menu_jump_start -> {
                nextsIndex = -1
                needSaving = true
                nextName()
            }
            R.string.menu_jump_end -> {
                if (nexts.isNotEmpty()) {
                    nextsIndex = nexts.size - 2
                    needSaving = true
                }
                nextName()
            }
            R.string.menu_abort -> {
                needSaving = false
                finish()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    public override fun onStop() {
        if (needSaving) {
            project.nexts = nexts
            project.scores = scores
            project.nextsIndex = nextsIndex
            project.needSaving = true

            MainActivity.instance?.updateView()
        }
        super.onStop()
    }
}
