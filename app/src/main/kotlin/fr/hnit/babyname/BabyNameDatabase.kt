/*
* Copyright (C) 2025 Baby Name Developers
* SPDX-License-Identifier: GPL-3.0-or-later
*/

package fr.hnit.babyname

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

import fr.hnit.babyname.BabyName.Origin
import fr.hnit.babyname.BabyName.Frequency
import fr.hnit.babyname.BabyName.Gender
import fr.hnit.babyname.BabyNameSettings.Companion.fileExists
import fr.hnit.babyname.BabyNameSettings.Companion.readInternalFile
import fr.hnit.babyname.BabyNameSettings.Companion.writeInternalFile

class BabyNameDatabase {
    public var isLoaded = false
    private var allNames = arrayListOf<BabyName>()

    fun initialize(ctx: Context) {
        if (fileExists(ctx, DATABASE_PATH)) {
            Log.d(this, "initialize() loadDatabase")
            loadDatabase(ctx)
        } else {
            // First startup.
            Log.d(this, "initialize() resetDatabase")
            resetDatabase(ctx)
        }
    }

    fun resetDatabase(ctx: Context) {
        // Overwrite database with database from assets.
        val csv = BufferedReader(InputStreamReader(ctx.assets.open("babynames_original.csv")))
            .use(BufferedReader::readText)
        writeInternalFile(ctx, DATABASE_PATH, csv.toByteArray())
        loadDatabase(ctx)
    }

    fun loadDatabase(ctx: Context) {
        val databaseBytes = readInternalFile(ctx, DATABASE_PATH)
        val databaseString = String(databaseBytes, Charsets.UTF_8)
        val names = deserializeNames(databaseString)
        //Log.d(this, "Loaded ${allNames.size} names")
        setNames(names)
    }

    fun getName(index: Int): BabyName {
        //Log.d(this, "index: $index, size: ${allNames.size}")
        return allNames[index]
    }

    fun size(): Int {
        return allNames.size
    }

    fun addDatabase(ctx: Context, addDatabaseBytes: ByteArray) {
        val addDatabaseString = String(addDatabaseBytes, 0, addDatabaseBytes.size)
        val databaseBytes = readInternalFile(ctx, DATABASE_PATH)
        val newNames = deserializeNames(addDatabaseString)
        if (newNames.isNotEmpty()) {
            writeInternalFile(ctx, DATABASE_PATH,
                databaseBytes, "\n".toByteArray(), addDatabaseBytes)
            loadDatabase(ctx)
        } else {
            throw Exception("Empty Database")
        }
    }

    fun setDatabase(ctx: Context, databaseBytes: ByteArray) {
        val databaseString = String(databaseBytes, 0, databaseBytes.size)
        val newNames = deserializeNames(databaseString)
        if (newNames.isNotEmpty()) {
            writeInternalFile(ctx, DATABASE_PATH, databaseBytes)
            loadDatabase(ctx)
        } else {
            throw Exception("Empty Database")
        }
    }

    private fun mergeBabyNames(first: BabyName, second: BabyName): BabyName {
        assert(first.name == second.name)
        val firstOrigins = first.origins
        val secondOrigins = second.origins
        val origins = ArrayList<Origin>()
        for (firstOrigin in firstOrigins) {
            val secondOrigin = secondOrigins.firstOrNull { it.name == firstOrigin.name }
            if (secondOrigin == null) {
                origins.add(firstOrigin)
            } else {
                if (firstOrigin.gender == secondOrigin.gender) {
                    if (firstOrigin.frequency == secondOrigin.frequency) {
                        origins.add(firstOrigin)
                    } else if (firstOrigin.frequency == null) {
                        origins.add(secondOrigin)
                    } else if (secondOrigin.frequency == null) {
                        origins.add(firstOrigin)
                    } else {
                        // conflicting data, take first
                        origins.add(firstOrigin)
                    }
                } else {
                    // conflicting data, take first
                    origins.add(firstOrigin)
                }
            }
        }
        origins.sortBy { it.name }
        return BabyName(first.id, first.name, origins.toTypedArray())
    }

    // The input is expected to be a sorted and distinct list
    fun setNames(newNamesInput: ArrayList<BabyName>) {
        // sort to find duplicates and for binary search
        newNamesInput.sortBy { it.name }

        // merge duplicate names
        val newNames = ArrayList<BabyName>()
        for (name in newNamesInput) {
            val last = newNames.lastOrNull()
            if (last != null && name.name == last.name) {
                val mergedName = mergeBabyNames(last, name)
                newNames[newNames.lastIndex] = mergedName
            } else {
                newNames.add(name)
            }
        }

        // fix all indices
        for (id in newNames.indices) {
            newNames[id].id = id
        }

        if (allNames.isNotEmpty()) {
            // record id changes
            val map = HashMap<Int, Int>()

            for (oldIndex in allNames.indices) {
                val oldName = allNames[oldIndex]
                val newIndex = newNames.binarySearchBy(oldName.name) { it.name }
                if (newIndex < 0) {
                    // name does not exist anymore
                    map[oldIndex] = -1
                } else {
                    // index changed
                    map[oldIndex] = newIndex
                }
            }

            // update projects
            for (project in MainActivity.projects) {
                project.updateIDs(map)
            }
        }

        // set new names as last step
        allNames = newNames
        isLoaded = true
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

    companion object {
        const val DATABASE_PATH = "babynames.csv"

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

        private fun parseFrequency(frequency: String): Frequency? {
            return when (frequency) {
                "1" -> Frequency.FREQUENCY_1 // very rare
                "2" -> Frequency.FREQUENCY_2
                "3" -> Frequency.FREQUENCY_3
                "4" -> Frequency.FREQUENCY_4
                "5" -> Frequency.FREQUENCY_5
                "6" -> Frequency.FREQUENCY_6
                "7" -> Frequency.FREQUENCY_7
                "8" -> Frequency.FREQUENCY_8
                "9" -> Frequency.FREQUENCY_9
                "10" -> Frequency.FREQUENCY_10
                "11" -> Frequency.FREQUENCY_11
                "12" -> Frequency.FREQUENCY_12
                "13" -> Frequency.FREQUENCY_13 // very common
                else -> null
            }
        }

        private fun frequencyString(frequency: Frequency): String {
            return when (frequency) {
                Frequency.FREQUENCY_1 -> "1" // very rare
                Frequency.FREQUENCY_2 -> "2"
                Frequency.FREQUENCY_3 -> "3"
                Frequency.FREQUENCY_4 -> "4"
                Frequency.FREQUENCY_5 -> "5"
                Frequency.FREQUENCY_6 -> "6"
                Frequency.FREQUENCY_7 -> "7"
                Frequency.FREQUENCY_8 -> "8"
                Frequency.FREQUENCY_9 -> "9"
                Frequency.FREQUENCY_10 -> "10"
                Frequency.FREQUENCY_11 -> "11"
                Frequency.FREQUENCY_12 -> "12"
                Frequency.FREQUENCY_13 -> "13" // very common
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
            if (origin.frequency != null) {
                return "${origin.name}:${genderString(origin.gender)}:${frequencyString(origin.frequency)}"
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
                    val originFrequency = parseFrequency(tokens[2])
                    if (originName == null || originGender  == null || originFrequency == null) {
                        return null
                    }

                    origins.add(Origin(originName, originGender, originFrequency))
                } else {
                    return null
                }
            }

            return sortOrigins(origins.toTypedArray())
        }

        // Sort Origin Array in-line.
        private fun sortOrigins(origins: Array<Origin>): Array<Origin> {
            // Sort by frequency first, then by name.
            origins.sortWith { o1: Origin, o2: Origin ->
                if (o1.frequency != null && o2.frequency != null && o1.frequency != o2.frequency) {
                    o2.frequency.ordinal - o1.frequency.ordinal
                } else {
                    o1.name.compareTo(o2.name)
                }
            }
            return origins
        }

        fun deserializeNames(text: String): ArrayList<BabyName> {
            val names = arrayListOf<BabyName>()
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

                lineNumber += 1

                // Skip empty lines.
                if (line.isEmpty() || line.startsWith("#")) {
                    continue
                }

                val items = line.split(";")
                if (items.size != 3) {
                    throw Exception("Failed to parse line $lineNumber: ${getExcerpt(line)}")
                }

                val name = parseNameCSV(items[0])
                val origins = parseOriginsCSV(items[1])
                val description = items[2].trim() // not used yet

                if (name == null) {
                    throw Exception("Invalid name in line $lineNumber: ${getExcerpt(line)}")
                }

                if (origins == null) {
                    throw Exception("Invalid origin in line $lineNumber: ${getExcerpt(line)}")
                }

                if (origins.map { originString(it) }.distinct().size != origins.size) {
                    throw Exception("Duplicate origin in line $lineNumber: ${getExcerpt(line)}")
                }

                names.add(BabyName(names.size, name, origins))
            }

            return names
        }

/*
        fun serializeNames(names: ArrayList<BabyName>): String {
            return buildString {
                for (name in names) {
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
 */
    }
}
