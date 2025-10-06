/*
* Copyright (C) 2025 Baby Name Developers
* SPDX-License-Identifier: GPL-3.0-or-later
*/

package fr.hnit.babyname

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

class EditActivity : AppCompatActivity() {
    lateinit var project: BabyNameProject
    lateinit var adapter: OriginAdapter
    lateinit var originsListView: ListView
    lateinit var originsLogicRadio: RadioGroup
    lateinit var genderRadio: RadioGroup
    lateinit var patternText: EditText
    lateinit var counterText: TextView
    lateinit var keepCheckbox: CheckBox

    // backup if this is not a new project
    var existingNexts = listOf<Int>()
    var existingScores = HashMap<Int, Float>()

    var loadFinished: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)

        originsListView = findViewById(R.id.origins_list)
        originsLogicRadio = findViewById(R.id.origins_logic_radio)
        genderRadio = findViewById(R.id.gender_radio)
        patternText = findViewById(R.id.pattern_text)
        counterText = findViewById(R.id.counter_text)
        keepCheckbox = findViewById(R.id.keep_checkbox)

        val defaultBackgroundColor = patternText.getDrawingCacheBackgroundColor()
        patternText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun afterTextChanged(editable: Editable) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                try {
                    // check if the pattern is valid
                    Pattern.compile(s.toString().trim { it <= ' ' })
                    patternText.setBackgroundColor(defaultBackgroundColor)
                    if (loadFinished) {
                        updateNameCounter()
                    }
                } catch (e: PatternSyntaxException) {
                    // set background to orange
                    patternText.setBackgroundColor(Color.rgb(255, 165, 0))
                }
            }
        })

        genderRadio.setOnCheckedChangeListener { _: RadioGroup?, _: Int ->
            if (loadFinished) {
                updateNameCounter()
            }
        }

        val allOrigins = MainActivity.database.getAllOrigins()
        val origins = ArrayList(allOrigins)
        origins.sort()

        adapter = OriginAdapter(origins, applicationContext) {
            if (loadFinished) {
                updateNameCounter()
            }
        }
        originsListView.adapter = adapter

        originsLogicRadio.setOnCheckedChangeListener { _: RadioGroup?, _: Int ->
            if (loadFinished) {
                updateNameCounter()
            }
        }

        keepCheckbox.setOnClickListener {
            if (loadFinished) {
                updateNameCounter()
            }
        }

        val projectIndex = intent.getIntExtra(MainActivity.PROJECT_EXTRA, -1)
        project = if (projectIndex == -1) {
            // create new project
            BabyNameProject()
        } else {
            // get existing project
            MainActivity.projects[projectIndex]
        }

        applyFromProject(project)

        updateKeepNames()

        updateNameCounter()
        loadFinished = true
    }

    private fun updateKeepNames() {
        val keepText = findViewById<TextView>(R.id.keep_text)
        val keepLayout = findViewById<LinearLayout>(R.id.keep_layout)
        val existingNames = existingNexts.isNotEmpty()

        // Show checkbox setting only when we edit
        // an existing project that still has names.
        if (existingNames) {
            keepText.text = String.format(getString(R.string.keep_existing_names), existingNexts.size)
            keepLayout.visibility = View.VISIBLE
        } else {
            keepText.text = ""
            keepLayout.visibility = View.GONE
        }
    }

    fun applyFromProject(project: BabyNameProject) {
        // backup existing nexts and scores
        existingNexts = project.nexts
        existingScores = project.scores

        val genderSelection = when (project.gender) {
            BabyNameProject.GenderSelection.ALL -> R.id.gender_all_radio
            BabyNameProject.GenderSelection.MALE -> R.id.gender_male_radio
            BabyNameProject.GenderSelection.FEMALE -> R.id.gender_female_radio
            BabyNameProject.GenderSelection.NEUTRAL -> R.id.gender_neutral_radio
        }
        genderRadio.check(genderSelection)

        val originsLogicSelection = when (project.originsLogic) {
            BabyNameProject.OriginsLogic.AND -> R.id.origins_logic_and
            BabyNameProject.OriginsLogic.OR -> R.id.origins_logic_or
        }
        originsLogicRadio.check(originsLogicSelection)

        // set pattern
        patternText.setText(project.pattern.toString())

        // clear origin selection
        for (i in adapter.checked.indices) {
            adapter.checked[i] = false
        }

        // select project origins
        for (origin in project.origins) {
            val i = adapter.getPosition(origin)
            if (i > -1) {
                adapter.checked[i] = true
            }
        }

        adapter.notifyDataSetChanged()
    }

    fun storeToProject(project: BabyNameProject): Boolean {
        // update origins
        project.origins.clear()
        var i = 0
        while (i < adapter.origins.size) {
            val origin = adapter.origins[i]
            val checked = adapter.checked[i]
            if (checked && origin != null) {
                project.origins.add(origin)
            }
            i += 1
        }

        // update gender selection
        project.gender = when (genderRadio.checkedRadioButtonId) {
            R.id.gender_male_radio -> BabyNameProject.GenderSelection.MALE
            R.id.gender_female_radio -> BabyNameProject.GenderSelection.FEMALE
            R.id.gender_all_radio -> BabyNameProject.GenderSelection.ALL
            R.id.gender_neutral_radio -> BabyNameProject.GenderSelection.NEUTRAL
            else -> {
                Log.w(this, "Unhandled radio button id: ${genderRadio.checkedRadioButtonId}")
                BabyNameProject.GenderSelection.ALL
            }
        }

        // update origins logic
        project.originsLogic = when (originsLogicRadio.checkedRadioButtonId) {
            R.id.origins_logic_and -> BabyNameProject.OriginsLogic.AND
            R.id.origins_logic_or -> BabyNameProject.OriginsLogic.OR
            else -> {
                Log.w(this, "Unhandled origins logic radio")
                BabyNameProject.OriginsLogic.OR
            }
        }

        // update name pattern
        try {
            project.pattern = Pattern.compile(patternText.text.toString().trim { it <= ' ' })
        } catch (e: PatternSyntaxException) {
            Toast.makeText(
                this,
                String.format(getString(R.string.name_pattern_malformed), e.message),
                Toast.LENGTH_LONG
            ).show()
            return false
        }

        val newNexts = mutableListOf<Int>()
        for (i in 0 until MainActivity.database.size()) {
            if (project.isNameValid(MainActivity.database.get(i))) {
                newNexts.add(i)
            }
        }

        // keep previous nexts and scores (if this is not a new project)
        if (keepCheckbox.isChecked) {
            newNexts.addAll(existingNexts)
            newNexts.addAll(existingScores.keys)

            project.nexts = newNexts.distinct().shuffled()
            project.scores = existingScores
            project.nextsIndex = -1
        } else {
            project.nexts = newNexts.shuffled()
            project.scores.clear()
            project.nextsIndex = -1
        }

        project.nextsIndex = -1
        project.needSaving = true

        return true
    }

    fun updateNameCounter() {
        //Log.d("updateNameCounter()");

        val tmp = BabyNameProject()
        counterText.text = if (storeToProject(tmp)) {
            String.format(getString(R.string.names_counter), tmp.nexts.size)
        } else {
            "???"
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.edit, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_save_project -> {
                if (storeToProject(project)) {
                    if (project.nexts.isEmpty()) {
                        Toast.makeText(this, R.string.too_much_constraint, Toast.LENGTH_SHORT).show()
                        return false
                    }

                    // add if not added yet
                    if (MainActivity.projects.indexOf(project) == -1) {
                        MainActivity.projects.add(project)
                    }

                    finish()
                }
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }
}
