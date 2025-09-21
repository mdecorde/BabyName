/*
* Copyright (C) 2025 Baby Name Developers
* SPDX-License-Identifier: GPL-3.0-or-later
*/

package fr.hnit.babyname

import android.app.Activity
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class BabyNameDatabase {
    public var isLoaded = false
    private var allNames = arrayListOf<BabyName>()

    fun initialize(activity: Activity) {
        try {
            val databaseFileName = "babynames.csv"
            val csvData = BufferedReader(InputStreamReader(activity.assets.open(databaseFileName)))
                .use(BufferedReader::readText)
            setNames(importCSV(csvData, false))

            Log.d(this, "Loaded ${allNames.size} names")
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun get(index: Int): BabyName {
        //Log.d(this, "index: $index, size: ${allNames.size}")
        return allNames[index]
    }

    fun size(): Int {
        return allNames.size
    }

    fun addNames(newNames: ArrayList<BabyName>) {
        val mergedNames = arrayListOf<BabyName>()
        mergedNames.addAll(allNames)
        mergedNames.addAll(newNames)
        mergedNames.sortBy({it.name})
        mergedNames.distinctBy { getBabyNameLine(it) }
        setNames(mergedNames)
    }

    // The input is expected to be a sorted and distinct list
    fun setNames(newNames: ArrayList<BabyName>) {
        // record id changes
        val map = HashMap<Int, Int>()

        for (oldIndex in allNames.indices) {
            val oldName = allNames[oldIndex]
            val newIndex = newNames.binarySearchBy(oldName.name) { it.name }
            if (oldIndex != newIndex) {
                map[oldIndex] = newIndex
            }
        }

        // fix all indices
        for (id in newNames.indices) {
            newNames[id].id = id
        }

        // update projects
        for (project in MainActivity.projects) {
            project.updateIDs(map)
        }

        // set new names as last step
        allNames = newNames
        isLoaded = true
    }

    private fun getExcerpt(str: String, max: Int = 32): String {
        if (str.length < max) {
            return str
        } else {
            return str.substring(0, max) + "..."
        }
    }

    fun importCSV(text: String, external: Boolean = true): ArrayList<BabyName> {
        val names = arrayListOf<BabyName>()
        val lineSet = hashSetOf<String>()
        var lineNumber = 0

        var offset = 0
        var run = true
        while (run) {
            val lineEnd = text.indexOf("\n", offset)
            val line = if (lineEnd >= 0) {
                val lineStart = offset
                offset = lineEnd + 1
                text.substring(lineStart, lineEnd)
            } else if (offset <= text.length) {
                run = false
                text.substring(offset)
            } else {
                break
            }

            if (line.isEmpty()) {
                continue
            }

            lineNumber += 1
            val items = line.split(";")
            if (items.size != 3) {
                throw Exception("Failed to parse line $lineNumber: ${getExcerpt(line)}")
            }

            val name = items[0]
            val genres = HashSet(items[1].split(","))
            val origins = HashSet(items[2].split(","))

            // remove empty entries
            genres.remove("")
            origins.remove("")

            if (external) {
                if (name.isEmpty() || name != name.trim()) {
                    throw Exception("Invalid name in line $lineNumber: ${getExcerpt(line)}")
                }

                for (genre in genres) {
                    if (genre.isEmpty() || genre != genre.trim() || (genre != "m" && genre != "f")) {
                        throw Exception("Invalid genre in line $lineNumber: ${getExcerpt(line)}")
                    }
                }

                for (origin in origins) {
                    if (origin.isEmpty() || origin != origin.trim()) {
                        throw Exception("Invalid origin in line $lineNumber: ${getExcerpt(line)}")
                    }
                }
            }

            val isMale = ("m" in genres)
            val isFemale = ("f" in genres)
            val item = BabyName(names.size, name, isMale, isFemale, origins)

            if (external) {
                // check for duplicate lines
                val lineHash = getBabyNameLine(item)
                if (lineHash in lineSet) {
                    throw Exception("Duplicate name in line $lineNumber: ${getExcerpt(line)} ($lineHash)")
                } else {
                    lineSet.add(lineHash)
                }
            }

            names.add(item)
        }

        if (external) {
            names.sortBy({getBabyNameLine(it)})
        }

        return names
    }

    // Hash BabyName object to find duplicates.
    private fun getBabyNameLine(name: BabyName) : String {
        return buildString {
            append(name.name)
            append(";")
            if (name.isMale && name.isFemale) {
                append("m,f");
            } else if (name.isMale) {
                append("m")
            } else if (name.isFemale) {
                append("f")
            }
            append(";")
            append(name.origins.joinToString(separator = ","))
        }
    }

    fun exportCSV(): String {
        return buildString {
            for (name in allNames ) {
                append(name.name)
                append(";")
                if (name.isMale && name.isFemale) {
                    append("m,f");
                } else if (name.isMale) {
                    append("m")
                } else if (name.isFemale) {
                    append("f")
                }
                append(";")
                append(name.origins.joinToString(separator = ","))
                append("\n")
            }
        }
    }

    fun getAllOrigins(): HashSet<String> {
        val all = HashSet<String>()
        for (name in allNames) {
            all.addAll(name.origins)
        }
        return all
    }
}
