/*
* Copyright (C) 2025 Baby Name Developers
* SPDX-License-Identifier: GPL-3.0-or-later
*/

package fr.hnit.babyname

import android.content.Context
import java.io.Serializable

class BabyName(var name: String, var isMale: Boolean, var isFemale: Boolean, var origins: HashSet<String>) : Serializable {
    var id = nextId++
    var soundex = generateSoundex(name)

    fun getMetaString(context: Context): String {
        var meta = ""
        if (isMale || isFemale) {
            meta += genresToLocale(context, isMale, isFemale).toString()
            meta += " "
        }
        if (origins.isNotEmpty()) {
            val origins = ArrayList(origins)
            origins.sort()
            meta += originsToLocale(context, origins).toString()
        }
        return meta
    }

    companion object {
        private var nextId = 0

        private fun genresToLocale(context: Context, isMale: Boolean, isFemale: Boolean): ArrayList<String> {
            val ret = ArrayList<String>()

            if (isFemale) {
                ret.add(context.getString(R.string.girl))
            }

            if (isMale) {
                ret.add(context.getString(R.string.boy))
            }

            return ret
        }

        // Make every origin begin with an upper case letter.
        // It would be nice to have proper localisation in the future.
        private fun originsToLocale(context: Context, origins: ArrayList<String>): ArrayList<String> {
            val ret = ArrayList<String>()
            for (origin in origins) {
                ret.add(Origins.getLocaleOrigin(context, origin))
            }
            return ret
        }

        // Return Soundex identifier encoded as Integer.
        private fun generateSoundex(name: String): Int {
            var prev = 0
            var ex = 0
            for (i in name.indices) {
                val c = name[i]
                val x = when (c) {
                    'a', 'e', 'i', 'o', 'u', 'y', 'h', 'w' -> 1
                    'b', 'f', 'p', 'v' -> 2
                    'c', 'g', 'j', 'k', 'q', 's', 'x', 'z' -> 3
                    'd', 't' -> 4
                    'l' -> 5
                    'm', 'n' -> 6
                    'r' -> 7
                    else -> 8
                }

                if (x != 8 && x != prev) {
                    ex = ex * 10 + x
                }
                prev = x
            }
            return ex
        }
    }
}
