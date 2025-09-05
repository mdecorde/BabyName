/*
* Copyright (C) 2025 Baby Name Developers
* SPDX-License-Identifier: GPL-3.0-or-later
*/

package fr.hnit.babyname

import android.content.Context
import android.util.SparseArray
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class BabyNameDatabase : SparseArray<BabyName>() {
    fun initialize(ctx: Context) {
        val thread: Thread = object : Thread() {
            override fun run() {
                try {
                    val databaseFileName = "babynames.csv"
                    val reader =
                        BufferedReader(InputStreamReader(ctx.assets.open(databaseFileName)))
                    var lineNumber = 0
                    while (reader.ready()) {
                        lineNumber += 1
                        val line = reader.readLine()
                        val items = line.split(";".toRegex()).toTypedArray()
                        if (items.size != 3) {
                            Log.e(this, "Failed to parse line in $databaseFileName:$lineNumber: $line")
                            break
                        }

                        val name = items[0]
                        val genres =
                            HashSet(listOf(*items[1].split(",".toRegex()).toTypedArray()))
                        val origins =
                            HashSet(listOf(*items[2].split(",".toRegex()).toTypedArray()))

                        // remove empty entries
                        genres.remove("")
                        origins.remove("")

                        if (name.isNotEmpty()) {
                            val isMale = ("m" in genres)
                            val isFemale = ("f" in genres)
                            val b = BabyName(name, isMale, isFemale, origins)
                            this@BabyNameDatabase.put(b.id, b)
                        } else {
                            Log.e(this, "Empty baby name in $databaseFileName:$lineNumber: $line")
                        }
                    }
                    reader.close()

                    Log.d(this, "Loaded " + this@BabyNameDatabase.size() + " names")
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        thread.start()
    }

    fun getAllOrigins(): HashSet<String> {
        val all = HashSet<String>()
        val n = this.size()
        for (i in 0 until n) {
            val entry = this[i]
            if (entry != null) {
                all.addAll(entry.origins)
            }
        }
        return all
    }
}
