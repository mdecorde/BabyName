/*
* Copyright (C) 2025 Baby Name Developers
* SPDX-License-Identifier: GPL-3.0-or-later
*/

package fr.hnit.babyname

import android.os.Bundle
import android.widget.CompoundButton
import com.google.android.material.switchmaterial.SwitchMaterial
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setTitle(R.string.menu_settings)

        findViewById<SwitchMaterial>(R.id.nextOnRatingSwitch).apply {
            isChecked = MainActivity.settings.nextOnRating
            setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
                MainActivity.settings.nextOnRating = isChecked
                MainActivity.settings.save(applicationContext)
            }
        }
    }
}
