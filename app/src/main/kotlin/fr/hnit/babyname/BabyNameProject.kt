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

class BabyNameProject() : Serializable {
    var needSaving = false
    var iD: String = UUID.randomUUID().toString()
    var gender = GenderSelection.ALL
    var origins = HashSet<String>()
    var originsLogic = OriginsLogic.OR
    var pattern: Pattern? = null
    var scores = HashMap<Int, Float>()
    var nexts = listOf<Int>()
    var nextsIndex = 0

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

    init {
        pattern = Pattern.compile(".*")
        reset()
    }

    fun cloneProject(): BabyNameProject {
        val project = BabyNameProject()
        project.gender = gender
        project.origins = origins.toHashSet()
        project.originsLogic = originsLogic
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
            return MainActivity.database.get(bestScoreIndex)
        }
    }

    // fix name ids when the database changes
    fun updateIDs(map: HashMap<Int, Int>) {
        val newNexts = ArrayList<Int>()
        for (next in nexts) {
            val newIndex = map.get(next)
            if (newIndex != null) {
                // invalidate
                nextsIndex = 0

                if (newIndex >= 0) {
                    // new index
                    newNexts.add(newIndex)
                } else {
                    // name deleted
                }
            } else {
                // index not changes
                newNexts.add(next)
            }
        }
        nexts = newNexts

        val newScores = hashMapOf<Int, Float>()
        for ((id, score) in scores) {
            val newIndex = map.get(id)
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

    // check if a name matches
    fun isNameValid(name: BabyName?): Boolean {
        if (name == null) {
            return false
        }

        val genderMatches = when (gender) {
            GenderSelection.ALL -> true
            GenderSelection.MALE -> name.isMale
            GenderSelection.FEMALE -> name.isFemale
            GenderSelection.NEUTRAL -> (name.isMale == name.isFemale)
        }

        if (!genderMatches) {
            return false
        }

        if (origins.isNotEmpty()) {
            if (originsLogic == OriginsLogic.OR) {
                var originMatches = false
                for (origin in name.origins) {
                    if (origin in origins) {
                        originMatches = true
                        continue
                    }
                }

                if (!originMatches) {
                    return false
                }
            }

            if (originsLogic == OriginsLogic.AND) {
                for (origin in origins) {
                    if (origin !in name.origins) {
                        return false
                    }
                }
            }
        }

        if (pattern != null) {
            return pattern!!.matcher(name.name).matches()
        }
        return true
    }

    fun rebuildNexts() {
        val newNexts = mutableListOf<Int>()
        for (i in 0 until MainActivity.database.size()) {
            if (isNameValid(MainActivity.database.get(i))) {
                newNexts.add(i)
            }
        }
        newNexts.shuffle()

        nexts = newNexts
        nextsIndex = 0
    }

    fun reset() {
        scores.clear()

        rebuildNexts()

        needSaving = true
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
