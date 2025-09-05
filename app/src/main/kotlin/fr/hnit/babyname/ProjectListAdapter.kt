/*
* Copyright (C) 2025 Baby Name Developers
* SPDX-License-Identifier: GPL-3.0-or-later
*/

package fr.hnit.babyname

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.TextView

class ProjectListAdapter(private val main: MainActivity, private val itemsArrayList: ArrayList<BabyNameProject>) :
    ArrayAdapter<BabyNameProject>(main, R.layout.item_project, itemsArrayList) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val project = itemsArrayList[position]

        val inflater = context
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        val rowView = inflater.inflate(R.layout.item_project, parent, false)

        val text = rowView.findViewById<TextView>(R.id.list_text)
        text.text = main.projectToString(project)

        rowView.findViewById<ImageButton>(R.id.list_scroll_find)
            .setOnClickListener { main.doScrollSearch(project) }

        rowView.findViewById<ImageButton>(R.id.list_flip_find)
            .setOnClickListener { main.doFlipSearch(project) }

        rowView.findViewById<ImageButton>(R.id.list_delete)
            .setOnClickListener { main.doDeleteProject(project) }

        rowView.findViewById<ImageButton>(R.id.list_top)
            .setOnClickListener { main.doShowTop10(project) }

        rowView.isLongClickable = true

        return rowView
    }
}
