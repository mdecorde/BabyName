/*
* Copyright (C) 2025 Baby Name Developers
* SPDX-License-Identifier: GPL-3.0-or-later
*/

package fr.hnit.babyname

import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
    private var nextsIndex = 0
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

        currentBabyName = getCurrentName()

        if (currentBabyName == null) {
            Toast.makeText(this@FlipSearchActivity, R.string.message_no_name_to_review, Toast.LENGTH_LONG).show()
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
                    this,
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

    private fun updateName() {
        val babyName = currentBabyName
        if (babyName == null) {
            // last or first name reached
            builder.setTitle(R.string.finish_round_title)
            builder.setMessage(String.format(getString(R.string.finish_round_message), DROP_RATE_PERCENT))
            builder.setPositiveButton(R.string.yes) { dialog: DialogInterface, id: Int ->
                nextRound()
                dialog.dismiss()
                finish()
            }
            builder.setNegativeButton(R.string.no) { dialog: DialogInterface, id: Int ->
                dialog.dismiss()
            }
            builder.show()
        } else {
            nameText.text = babyName.name

            extraText.text = babyName.getMetaString(applicationContext)

            progressCounterText.text = String.format("(%d/%d)", nextsIndex + 1, nexts.size)
            progressPercentText.text = String.format("%d%%", (100 * nextsIndex) / nexts.size)

            // set existing score or default to 0
            rateBar.rating = (scores[babyName.id] ?: 0).toFloat() / 2.0F
        }
    }

    private fun nextName() {
        currentBabyName = getNextName()
        updateName()
    }

    private fun removeName() {
        val position = nextsIndex
        val nameId = nexts.removeAt(position)
        val scoreBackup = scores.remove(nameId)
        val needSavingBackup = needSaving
        needSaving = true

        currentBabyName = getCurrentName()
        updateName()

        val snackbar = Snackbar.make(
            buttonLayout,
            R.string.name_was_removed,
            Snackbar.LENGTH_LONG
        )
        snackbar.setAction(R.string.undo) {
            nexts.add(position, nameId)
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

    private fun nextRound() {
        // sort by score, lowest scores last
        nexts.sortWith { i1: Int, i2: Int -> (2 * ((scores[i2] ?: 0F) - (scores[i1] ?: 0F))).toInt() }

        dropLast()

        nexts.shuffle()

        nextsIndex = 0

        needSaving = true
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

    private fun previousName() {
        currentBabyName = getPreviousName()
        updateName()
    }

    public override fun onStop() {
        project.nexts = nexts
        project.scores = scores
        project.nextsIndex = nextsIndex
        project.needSaving = needSaving

        MainActivity.instance?.updateView()

        super.onStop()
    }
}
