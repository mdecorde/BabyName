/*
* Copyright (C) 2025 Baby Name Developers
* SPDX-License-Identifier: GPL-3.0-or-later
*/

package fr.hnit.babyname

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ScrollSearchAdapter(private val scrollActivity: ScrollSearchActivity,
        private val project: BabyNameProject, private val nexts: ArrayList<Int>, private val scores: HashMap<Int, Float>)
    : RecyclerView.Adapter<ScrollSearchAdapter.ViewHolder>() {

    var onItemClick: ((BabyName) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_scroll, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        //Log.d(this, "onBindViewHolder $position")

        val index = nexts[position]
        val babyName = MainActivity.database.get(index)

        holder.rankView.text = "${position + 1}."
        holder.textView.text = scrollActivity.getHighlightedName(babyName.name)
        holder.rateBar.rating = scores[index] ?: 0F
        holder.extraView.text = project.getShortOriginsString(scrollActivity.applicationContext, babyName)

        holder.extraView.setOnClickListener {
            onItemClick?.invoke(babyName)
        }

        holder.rateBar.onRatingBarChangeListener = RatingBar.OnRatingBarChangeListener {
                ratingBar: RatingBar, rating: Float, fromUser: Boolean ->
            if (fromUser) {
                scrollActivity.onRatingChangeListener(babyName, rating)
            }
        }
    }

    override fun getItemCount(): Int {
        return nexts.size
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val rankView: TextView = itemView.findViewById(R.id.rankView)
        val rateBar: RatingBar = itemView.findViewById(R.id.rateBar)
        val textView: TextView = itemView.findViewById(R.id.nameView)
        val extraView: TextView = itemView.findViewById(R.id.extraView)
    }
}
