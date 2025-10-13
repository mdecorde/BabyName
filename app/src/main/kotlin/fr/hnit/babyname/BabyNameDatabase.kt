/*
* Copyright (C) 2025 Baby Name Developers
* SPDX-License-Identifier: GPL-3.0-or-later
*/

package fr.hnit.babyname

import android.app.Activity
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

import fr.hnit.babyname.BabyName.Origin
import fr.hnit.babyname.BabyName.OriginRarity
import fr.hnit.babyname.BabyName.Gender

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
        val names = arrayListOf<BabyName>()
        names.addAll(allNames)
        names.addAll(newNames)
        setNames(names)
        /*
        for (item in allNames) {
            namesMap[item.name] = item
        }

        for (item in newNames) {
            val name = namesMap[item.name]
            if (name != null) {
                for (newOrigin in item.origins) {
                    val index = name.origins.indexOfFirst { it.name == newOrigin.name}
                    if (index != -1) {
                        // The new origin overwrites to old one.
                        name.origins[index] = newOrigin
                    } else {
                        // Add new origin.
                        name.origins = sortOrigins(
                            name.origins.plus(newOrigin)
                        )
                    }
                }
            } else {
                // Add new name.
                namesMap[item.name] = item
            }
        }

        setNames(arrayListOf(*namesMap.values.toTypedArray()))
        */
    }

    // The input is expected to be a sorted and distinct list
    fun setNames(newNames: ArrayList<BabyName>) {
        // sort for binary search
        newNames.sortBy { it.name }

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

    private fun parseNameCSV(name: String): String? {
        if (name.isNotEmpty() && name == name.trim()) {
            return name
        } else {
            return null
        }
    }

    private fun parseOriginName(country: String): String? {
        if (country.isNotEmpty() && country == country.trim()) {
            return country
        } else {
            return null
        }
    }

    private fun parseRarity(rarity: String): OriginRarity? {
        return when (rarity) {
            "1" -> OriginRarity.RARITY_1 // very rare
            "2" -> OriginRarity.RARITY_2
            "3" -> OriginRarity.RARITY_3
            "4" -> OriginRarity.RARITY_4
            "5" -> OriginRarity.RARITY_5
            "6" -> OriginRarity.RARITY_6
            "7" -> OriginRarity.RARITY_7
            "8" -> OriginRarity.RARITY_8
            "9" -> OriginRarity.RARITY_9
            "10" -> OriginRarity.RARITY_10
            "11" -> OriginRarity.RARITY_11
            "12" -> OriginRarity.RARITY_12
            "13" -> OriginRarity.RARITY_13 // very common
            else -> null
        }
    }

    private fun rarityString(rarity: OriginRarity): String {
        return when (rarity) {
            OriginRarity.RARITY_1 -> "1" // very rare
            OriginRarity.RARITY_2 -> "2"
            OriginRarity.RARITY_3 -> "3"
            OriginRarity.RARITY_4 -> "4"
            OriginRarity.RARITY_5 -> "5"
            OriginRarity.RARITY_6 -> "6"
            OriginRarity.RARITY_7 -> "7"
            OriginRarity.RARITY_8 -> "8"
            OriginRarity.RARITY_9 -> "9"
            OriginRarity.RARITY_10 -> "10"
            OriginRarity.RARITY_11 -> "11"
            OriginRarity.RARITY_12 -> "12"
            OriginRarity.RARITY_13 -> "13" // very common
        }
    }

    private fun parseGender(entry: String): Gender? {
        return when (entry) {
            "M" -> Gender.MALE
            "F" -> Gender.FEMALE
            "1M" -> Gender.MOSTLY_MALE
            "1F" -> Gender.MOSTLY_FEMALE
            "?M" -> Gender.SOMEWHAT_MALE
            "?F" -> Gender.SOMEWHAT_FEMALE
            "?" -> Gender.UNISEX
            else -> null
        }
    }

    private fun genderString(gender: Gender): String {
        return when (gender) {
            Gender.MALE -> "M"
            Gender.FEMALE -> "F"
            Gender.MOSTLY_MALE -> "1M"
            Gender.MOSTLY_FEMALE -> "1F"
            Gender.SOMEWHAT_MALE -> "?M"
            Gender.SOMEWHAT_FEMALE -> "?F"
            Gender.UNISEX -> "?"
        }
    }

    private fun originString(origin: Origin): String {
        if (origin.rarity != null) {
            return "${origin.name}:${genderString(origin.gender)}:${rarityString(origin.rarity)}"
        } else {
            return "${origin.name}:${genderString(origin.gender)}"
        }
    }

    private fun parseOriginsCSV(entry: String): Array<Origin>? {
        val origins = ArrayList<Origin>()

        for (item in entry.split(",")) {
            val tokens = item.split(":")
            if (tokens.size == 2) {
                val originName = parseOriginName(tokens[0])
                val originGender = parseGender(tokens[1])

                if (originName == null || originGender == null) {
                    return null
                }

                origins.add(Origin(originName, originGender, null))
            } else if (tokens.size == 3) {
                val originName = parseOriginName(tokens[0])
                val originGender = parseGender(tokens[1])
                val originRarity = parseRarity(tokens[2])
                if (originName == null || originGender  == null || originRarity == null) {
                    return null
                }

                origins.add(Origin(originName, originGender, originRarity))
            } else {
                return null
            }
        }

        return sortOrigins(origins.toTypedArray())
    }

    // Sort Origin Array in-line.
    private fun sortOrigins(origins: Array<Origin>): Array<Origin> {
        // Sort by rarity first, then by name.
        origins.sortWith { o1: Origin, o2: Origin ->
            if (o1.rarity != null && o2.rarity != null && o1.rarity != o2.rarity) {
                o2.rarity.ordinal - o1.rarity.ordinal
            } else {
                o1.name.compareTo(o2.name)
            }
        }
        return origins
    }

    fun importCSV(text: String, external: Boolean = true): ArrayList<BabyName> {
        val names = arrayListOf<BabyName>()
        val nameSet = hashSetOf<String>() // to find duplicates
        var lineNumber = 0
        // Data header, may be muliple consecutive lines starting with '#'.
        var headerLine = 0
        var header = ""

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

            lineNumber += 1

            // Skip empty lines.
            if (line.isEmpty()) {
                continue
            }

            // Collect header.
            if (line.startsWith("#")) {
                if ((headerLine + 1) == lineNumber && header.isNotEmpty()) {
                    header += "\n" + line
                } else {
                    header = line
                }
                headerLine = lineNumber
                continue
            }

            val items = line.split(";")
            if (items.size != 3) {
                throw Exception("Failed to parse line $lineNumber: ${getExcerpt(line)}")
            }

            val name = parseNameCSV(items[0])
            val origins = parseOriginsCSV(items[1])
            val description = items[2].trim()

            if (name == null) {
                throw Exception("Invalid name in line $lineNumber: ${getExcerpt(line)}")
            }

            if (origins == null) {
                throw Exception("Invalid origin in line $lineNumber: ${getExcerpt(line)}")
            }

            if (name in nameSet) {
                throw Exception("Duplicate name in line $lineNumber: ${getExcerpt(line)}")
            }

            if (origins.map { it.name }.distinct().size != origins.size) {
                throw Exception("Duplicate origin in line $lineNumber: ${getExcerpt(line)}")
            }

            names.add(BabyName(names.size, name, origins, header))
        }

        return names
    }

    fun exportCSV(): String {
        // Clone names list.
        val all = allNames.toMutableList()

        // Sort by header first, name second.
        all.sortWith { n1: BabyName, n2: BabyName ->
            if (n1.header != n2.header) {
                n1.header.compareTo(n2.header)
            } else {
                n1.name.compareTo(n2.name)
            }
        }

        return buildString {
            var header = ""
            for (name in all) {
                if (name.header != header) {
                    append(name.header)
                    append("\n")
                    header = name.header
                }

                append(name.name)
                append(";")
                append(
                    name.origins.joinToString(",") { originString(it) }
                )
                append(";")
                append("\n")
            }
        }
    }

    fun getAllOriginNames(): HashSet<String> {
        val all = HashSet<String>()
        for (name in allNames) {
            for (origin in name.origins) {
                all.add(origin.name)
            }
        }
        return all
    }
}
