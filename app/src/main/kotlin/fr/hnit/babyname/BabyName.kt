/*
* Copyright (C) 2025 Baby Name Developers
* SPDX-License-Identifier: GPL-3.0-or-later
*/

package fr.hnit.babyname

import java.io.Serializable

class BabyName(var id: Int, var name: String, var origins: Array<Origin>, val header: String) : Serializable {
    var soundex = generateSoundex(name)

    // from very rare (1) to very common (13)
    enum class Frequency {
        FREQUENCY_1, // 0.00390625..0.0078125% of the population
        FREQUENCY_2, // 0.0078125..0.015625%
        FREQUENCY_3, // 0.015625..0.03125%
        FREQUENCY_4, // 0.03125..0.0625%
        FREQUENCY_5, // 0.0625..0.125%
        FREQUENCY_6, // 0.125..0.25%
        FREQUENCY_7, // 0.25..0.5%
        FREQUENCY_8, // 0.5..1.0%
        FREQUENCY_9, // 1.0..2.0%
        FREQUENCY_10, // 2.0..4.0%
        FREQUENCY_11, // 4.0..8.0%
        FREQUENCY_12, // 8.0..16%
        FREQUENCY_13 // 16..32%
    }

    data class Origin(val name: String, val gender: Gender, val frequency: Frequency?)

    enum class Gender {
        MALE, // M: male first name
        FEMALE, // F: female first name
        SOMEWHAT_MALE, // "?M": unisex name, which is mostly male
        SOMEWHAT_FEMALE, // "?F": unisex name, which is mostly female
        MOSTLY_MALE, // "1M": male name, if first part of name; else: mostly male name
        MOSTLY_FEMALE, // "1F": female name, if first part of name; else: mostly female name
        UNISEX // "?": can be male or female
    }

    companion object {
        fun getFrequencyApproximation(frequency: Frequency?): String {
            // Approximate ratio of the middle of the frequency bracket
            return when (frequency) {
                Frequency.FREQUENCY_1 -> "1:19200" // 0.00390625% - 0.0078125%
                Frequency.FREQUENCY_2 -> "1:9600" // 0.0078125% - 0.015625%
                Frequency.FREQUENCY_3 -> "1:4800" // 0.015625% - 0.03125%
                Frequency.FREQUENCY_4 -> "1:2400" // 0.03125% - 0.0625%
                Frequency.FREQUENCY_5 -> "1:1200" // 0.0625% - 0.125%s
                Frequency.FREQUENCY_6 -> "1:600" // 0.125% - 0.25%
                Frequency.FREQUENCY_7 -> "1:300" // 0.25% - 0.5%
                Frequency.FREQUENCY_8 -> "1:150" // 0.5% - 1.0%
                Frequency.FREQUENCY_9 -> "1:75" // 1.0% - 2.0%
                Frequency.FREQUENCY_10 -> "1:37" // 2.0% - 4.0%
                Frequency.FREQUENCY_11 -> "1:18" // 4.0% - 8.0%
                Frequency.FREQUENCY_12 -> "1:9" // 8.0% - 16%
                Frequency.FREQUENCY_13 -> "1:4" // 16% - 32%
                null -> "1:?"
            }
        }

        // Return Soundex identifier encoded as Integer.
        private fun generateSoundex(name: String): Int {
            var prev = 0
            var ex = 0
            for (i in name.indices) {
                val c = name[i].lowercaseChar()
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
