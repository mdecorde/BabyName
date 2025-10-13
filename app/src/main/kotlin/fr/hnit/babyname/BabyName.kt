/*
* Copyright (C) 2025 Baby Name Developers
* SPDX-License-Identifier: GPL-3.0-or-later
*/

package fr.hnit.babyname

import java.io.Serializable

class BabyName(var id: Int, var name: String, var origins: Array<Origin>, val header: String) : Serializable {
    var soundex = generateSoundex(name)

    // from very rare (1) to very common (13)
    enum class OriginRarity {
        RARITY_1, // 0.00390625..0.0078125% of the population
        RARITY_2, // 0.0078125..0.015625%
        RARITY_3, // 0.015625..0.03125%
        RARITY_4, // 0.03125..0.0625%
        RARITY_5, // 0.0625..0.125%
        RARITY_6, // 0.125..0.25%
        RARITY_7, // 0.25..0.5%
        RARITY_8, // 0.5..1.0%
        RARITY_9, // 1.0..2.0%
        RARITY_10, // 2.0..4.0%
        RARITY_11, // 4.0..8.0%
        RARITY_12, // 8.0..16%
        RARITY_13 // 16..32%
    }

    data class Origin(val name: String, val gender: Gender, val rarity: OriginRarity?)

    enum class Gender {
        MALE, // male first name
        FEMALE, // female first name
        SOMEWHAT_MALE, // male name, if first part of name; else: mostly male name
        SOMEWHAT_FEMALE, // female name, if first part of name; else: mostly female name
        MOSTLY_MALE, // unisex name, which is mostly male
        MOSTLY_FEMALE, // unisex name, which is mostly female
        UNISEX // can be male or female
    }

    companion object {
        fun getRarityApproximation(rarity: OriginRarity?): String {
            // Approximate ratio of the middle of the rarity bracket
            return when (rarity) {
                OriginRarity.RARITY_1 -> "1:19200" // 0.00390625% - 0.0078125%
                OriginRarity.RARITY_2 -> "1:9600" // 0.0078125% - 0.015625%
                OriginRarity.RARITY_3 -> "1:4800" // 0.015625% - 0.03125%
                OriginRarity.RARITY_4 -> "1:2400" // 0.03125% - 0.0625%
                OriginRarity.RARITY_5 -> "1:1200" // 0.0625% - 0.125%s
                OriginRarity.RARITY_6 -> "1:600" // 0.125% - 0.25%
                OriginRarity.RARITY_7 -> "1:300" // 0.25% - 0.5%
                OriginRarity.RARITY_8 -> "1:150" // 0.5% - 1.0%
                OriginRarity.RARITY_9 -> "1:75" // 1.0% - 2.0%
                OriginRarity.RARITY_10 -> "1:37" // 2.0% - 4.0%
                OriginRarity.RARITY_11 -> "1:18" // 4.0% - 8.0%
                OriginRarity.RARITY_12 -> "1:9" // 8.0% - 16%
                OriginRarity.RARITY_13 -> "1:4" // 16% - 32%
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
