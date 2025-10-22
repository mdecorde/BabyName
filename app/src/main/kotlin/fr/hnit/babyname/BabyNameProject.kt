/*
* Copyright (C) 2025 Baby Name Developers
* SPDX-License-Identifier: GPL-3.0-or-later
*/

package fr.hnit.babyname

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.util.UUID
import java.util.regex.Pattern
import kotlin.math.min

import fr.hnit.babyname.BabyName.Gender

class BabyNameProject() : Serializable {
    var needSaving = false
    var iD: String = UUID.randomUUID().toString()
    var genderSelection = GenderSelection.ALL
    var originsSelection = ArrayList<String>()
    var originsSelectionLogic = OriginsLogic.OR
    // A regular expression.
    var pattern = Pattern.compile(".*")
    // Scores. Each key must always be present in nexts.
    var scores = HashMap<Int, Float>()
    // List of indexes into the database
    var nexts = listOf<Int>()
    // Index into nexts, may be -1 .. (nexts.size - 1)
    var nextsIndex = -1

    enum class OriginsLogic {
        OR,
        AND
    }

    // gender selection
    enum class GenderSelection {
        ALL,
        MALE,
        FEMALE,
        NEUTRAL
    }

    fun cloneProject(): BabyNameProject {
        val project = BabyNameProject()
        project.genderSelection = genderSelection
        project.originsSelection = ArrayList(originsSelection)
        project.originsSelectionLogic = originsSelectionLogic
        project.pattern = pattern
        project.scores = HashMap(scores)
        project.nexts = nexts.toMutableList()
        project.nextsIndex = nextsIndex
        return project
    }

    fun getBest(): BabyName? {
        var bestScoreIndex = -1
        var bestScoreValue = 0f

        for (entry in scores.iterator()) {
            if (entry.value > bestScoreValue) {
                bestScoreIndex = entry.key
                bestScoreValue = entry.value
            }
        }

        if (bestScoreIndex == -1) {
            return null
        } else {
            return MainActivity.database.getName(bestScoreIndex)
        }
    }

    // fix name ids when the database changes
    fun updateIDs(changeMap: HashMap<Int, Int>) {
        val newNexts = ArrayList<Int>()
        for (next in nexts) {
            val newIndex = changeMap.get(next)
            if (newIndex != null) {
                if (newIndex >= 0) {
                    // new index
                    newNexts.add(newIndex)
                } else {
                    // name deleted
                }

                // invalidate
                nextsIndex = -1
            } else {
                // no change
                newNexts.add(next)
            }
        }
        nexts = newNexts

        val newScores = hashMapOf<Int, Float>()
        for ((id, score) in scores) {
            val newIndex = changeMap.get(id)
            if (newIndex != null) {
                if (newIndex >= 0) {
                    // new index
                    newScores[newIndex] = score
                } else {
                    // name deleted
                }
            }
        }
        scores = newScores
    }

    fun getLongOriginsString(context: Context, name: BabyName): String {
        return buildString {
            for (origin in name.origins) {
                append(Origins.getLocaleOriginName(context, origin.name))
                append(": ")
                append(Origins.getLocaleOriginGender(context, origin.gender))
                if (origin.rarity != null) {
                    append(" (")
                    append(BabyName.getRarityApproximation(origin.rarity))
                    append(")")
                }
                append("\n")
            }
        }
    }

    /*
    * Show a simplified origin information.
    * - Only origins that were used as match (4 at most).
    * - Show genders only as Male/Female/Unisex.
    * - No rarity.
    */
    fun getShortOriginsString(context: Context, name: BabyName): String {
        fun showGender(gender: Gender): Boolean {
            return when (genderSelection) {
                GenderSelection.ALL -> gender == Gender.MALE || gender == Gender.FEMALE || gender == Gender.UNISEX
                GenderSelection.MALE -> gender == Gender.MALE
                GenderSelection.FEMALE -> gender == Gender.FEMALE
                GenderSelection.NEUTRAL -> gender == Gender.UNISEX
            }
        }

        fun simplifyGender(gender: Gender): Gender {
            return when (gender) {
                Gender.MALE, Gender.MOSTLY_MALE, -> Gender.MALE
                Gender.FEMALE, Gender.MOSTLY_FEMALE -> Gender.FEMALE
                Gender.UNISEX, Gender.SOMEWHAT_MALE, Gender.SOMEWHAT_FEMALE -> Gender.UNISEX
            }
        }

        val matchedOrigins = if (originsSelection.isNotEmpty()) {
            name.origins.filter { it.name in originsSelection }
        } else {
            name.origins.toList()
        }
        matchedOrigins.sortedBy { it.rarity }

        val showNames = matchedOrigins.map { it.name }.distinct().take(4)
        val matchedOriginGenders = matchedOrigins.map { it.gender }.distinct()
        val showGenders = matchedOriginGenders.map { simplifyGender(it) }.filter { showGender(it) }.distinct()

        showGenders.sortedBy { it.ordinal }

        // Translate to native strings.
        val showGendersPrint = showGenders.map { Origins.getLocaleOriginGender(context, it) }.toMutableList()
        val showNamesPrint = showNames.map { Origins.getLocaleOriginName(context, it) }.toMutableList()

        if (name.origins.size != showNames.size) {
            showNamesPrint.add("...")
        }

        if (name.origins.map { it.gender }.distinct().size != showGenders.size) {
            showGendersPrint.add("...")
        }

        return "$showGendersPrint $showNamesPrint"
    }

    // check if a name matches
    fun isNameValid(name: BabyName?): Boolean {
        if (name == null) {
            return false
        }

        if (!name.origins.any { matchGender(genderSelection, it.gender) }) {
            return false
        }

        if (originsSelection.isNotEmpty()) {
            if (originsSelectionLogic == OriginsLogic.OR) {
                var originMatches = false
                for (originName in originsSelection) {
                    if (name.origins.any { it -> it.name == originName && matchGender(genderSelection, it.gender) }) {
                        originMatches = true
                        break
                    }
                }

                if (!originMatches) {
                    return false
                }
            }

            if (originsSelectionLogic == OriginsLogic.AND) {
                for (origin in originsSelection) {
                    if (!name.origins.any { it -> it.name == origin && matchGender(genderSelection, it.gender)}) {
                        return false
                    }
                }
            }
        }

        if (pattern.toString() != ".*") {
            return pattern.matcher(name.name).matches()
        }

        return true
    }

    fun getTop10(): List<Int> {
        val names = scores.keys.toMutableList()

        //Log.d("names before sort: "+names+" scores: "+scores);
        names.sortWith { b1: Int, b2: Int -> (2 * ((scores[b2] ?: 0f) - (scores[b1] ?: 0f))).toInt() }

        //Log.d("names after sort: "+names);
        val min = min(10, names.size)
        return names.subList(0, min)
    }

    companion object {
        const val DROP_RATE_PERCENT = 20

        private fun matchGender(genderSelection: GenderSelection, gender: Gender): Boolean {
            return when (genderSelection) {
                GenderSelection.ALL -> true
                GenderSelection.MALE -> when (gender) {
                    Gender.MALE, Gender.MOSTLY_MALE, Gender.SOMEWHAT_MALE -> true
                    else -> false
                }
                GenderSelection.FEMALE -> when (gender) {
                    Gender.FEMALE, Gender.MOSTLY_FEMALE, Gender.SOMEWHAT_FEMALE -> true
                    else -> false
                }
                GenderSelection.NEUTRAL -> when (gender) {
                    Gender.UNISEX, Gender.SOMEWHAT_MALE, Gender.SOMEWHAT_FEMALE -> true
                    else -> false
                }
            }
        }

        fun readProject(filename: String?, context: Context): BabyNameProject? {
            var project: BabyNameProject? = null
            try {
                val fis = context.openFileInput(filename)
                val ois = ObjectInputStream(fis)
                project = ois.readObject() as BabyNameProject
                fis.close()
            } catch (e: Exception) {
                e.printStackTrace()
                context.deleteFile(filename)
            }
            return project
        }

        fun storeProject(project: BabyNameProject, context: Context): Boolean {
            val fileName = project.iD + ".baby"
            try {
                val file = File(context.filesDir, fileName)
                if (file.exists() && file.isFile) {
                    if (!file.delete()) {
                        throw Exception("Failed to delete existing file: $fileName")
                    }
                }
                if (!file.createNewFile()) {
                    throw Exception("Failed to create new file: $fileName")
                }
                val fos = FileOutputStream(file)
                val serializer = ObjectOutputStream(fos)
                serializer.writeObject(project)
                fos.close()

                project.needSaving = false
            } catch (e: Exception) {
                Log.e(this, "Cannot open $fileName")
                e.printStackTrace()
                return false
            }

            return true
        }
    }
}
