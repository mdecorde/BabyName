/*
* Copyright (C) 2025 Baby Name Developers
* SPDX-License-Identifier: GPL-3.0-or-later
*/

package fr.hnit.babyname

import android.content.Context

internal object Origins {
    fun getLocaleOriginGender(ctx: Context, gender: BabyName.Gender): String {
        val id = when (gender) {
            BabyName.Gender.MALE -> R.string.gender_male
            BabyName.Gender.FEMALE -> R.string.gender_female
            BabyName.Gender.SOMEWHAT_MALE -> R.string.gender_somewhat_male
            BabyName.Gender.SOMEWHAT_FEMALE -> R.string.gender_somewhat_female
            BabyName.Gender.MOSTLY_MALE -> R.string.gender_mostly_male
            BabyName.Gender.MOSTLY_FEMALE -> R.string.gender_mostly_female
            BabyName.Gender.UNISEX -> R.string.gender_unisex
        }
        return ctx.getString(id)
    }

    fun getLocaleOriginName(ctx: Context, originName: String): String {
        val id = when (originName) {
            // Origins used in nam_dict.txt database file
            "Great Britain" -> R.string.origin_item_great_britain
            "Ireland" -> R.string.origin_item_ireland
            "U.S.A." -> R.string.origin_item_usa
            "Italy" -> R.string.origin_item_italy
            "Malta" -> R.string.origin_item_malta
            "Portugal" -> R.string.origin_item_portugal
            "Spain" -> R.string.origin_item_spain
            "France" -> R.string.origin_item_france
            "Belgium" -> R.string.origin_item_belgium
            "Luxembourg" -> R.string.origin_item_luxembourg
            "Netherlands" -> R.string.origin_item_netherlands
            "East Frisia" -> R.string.origin_item_east_frisia
            "Germany" -> R.string.origin_item_germany
            "Austria" -> R.string.origin_item_austria
            "Swiss" -> R.string.origin_item_swiss
            "Iceland" -> R.string.origin_item_iceland
            "Denmark" -> R.string.origin_item_denmark
            "Norway" -> R.string.origin_item_norway
            "Sweden" -> R.string.origin_item_sweden
            "Finland" -> R.string.origin_item_finland
            "Estonia" -> R.string.origin_item_estonia
            "Latvia" -> R.string.origin_item_latvia
            "Lithuania" -> R.string.origin_item_lithuania
            "Poland" -> R.string.origin_item_poland
            "Czech Republic" -> R.string.origin_item_czech_republic
            "Slovakia" -> R.string.origin_item_slovakia
            "Hungary" -> R.string.origin_item_hungary
            "Romania" -> R.string.origin_item_romania
            "Bulgaria" -> R.string.origin_item_bulgaria
            "Bosnia and Herzegovina" -> R.string.origin_item_bosnia_and_herzegovina
            "Croatia" -> R.string.origin_item_croatia
            "Kosovo" -> R.string.origin_item_kosovo
            "Macedonia" -> R.string.origin_item_macedonia
            "Montenegro" -> R.string.origin_item_montenegro
            "Serbia" -> R.string.origin_item_serbia
            "Slovenia" -> R.string.origin_item_slovenia
            "Albania" -> R.string.origin_item_albania
            "Greece" -> R.string.origin_item_greece
            "Russia" -> R.string.origin_item_russia
            "Belarus" -> R.string.origin_item_belarus
            "Moldova" -> R.string.origin_item_moldova
            "Ukraine" -> R.string.origin_item_ukraine
            "Armenia" -> R.string.origin_item_armenia
            "Azerbaijan" -> R.string.origin_item_azerbaijan
            "Georgia" -> R.string.origin_item_georgia
            "Kazakhstan" -> R.string.origin_item_kazakhstan
            "Kyrgyzstan" -> R.string.origin_item_kyrgyzstan
            "Tajikistan" -> R.string.origin_item_tajikistan
            "Turkmenistan" -> R.string.origin_item_turkmenistan
            "Uzbekistan" -> R.string.origin_item_uzbekistan
            "Turkey" -> R.string.origin_item_turkey
            "Arabia/Persia" -> R.string.origin_item_arabia_persia
            "Israel" -> R.string.origin_item_israel
            "China" -> R.string.origin_item_china
            "India" -> R.string.origin_item_india
            "Sri Lanka" -> R.string.origin_item_sri_lanka
            "Japan" -> R.string.origin_item_japan
            "Korea" -> R.string.origin_item_korea
            "Vietnam" -> R.string.origin_item_vietnam
            "Unknown" -> R.string.origin_item_unknown
            // Origins used in Prenom.txt database file
            "Ancient Celtic" -> R.string.origin_item_ancient_celtic
            "Ancient Egyptian" -> R.string.origin_item_ancient_egyptian
            "Ancient Germanic" -> R.string.origin_item_ancient_germanic
            "Ancient Greek" -> R.string.origin_item_ancient_greek
            "Ancient Rome" -> R.string.origin_item_ancient_rome
            "Ancient Scandinavia" -> R.string.origin_item_ancient_scandinavia
            "Ancient" -> R.string.origin_item_ancient
            "Anglo-Saxon Mythology" -> R.string.origin_item_anglo_saxon_mythology
            "Astronomy" -> R.string.origin_item_astronomy
            "Celtic Mythology" -> R.string.origin_item_celtic_mythology
            "Egyptian Mythology" -> R.string.origin_item_egyptian_mythology
            "Far Eastern Mythology" -> R.string.origin_item_far_eastern_mythology
            "Germanic Mythology" -> R.string.origin_item_germanic_mythology
            "Greek Mythology" -> R.string.origin_item_greek_mythology
            "Hindu Mythology" -> R.string.origin_item_hindu_mythology
            "History" -> R.string.origin_item_history
            "Irish Mythology" -> R.string.origin_item_irish_mythology
            "Literature" -> R.string.origin_item_literature
            "Mythology" -> R.string.origin_item_mythology
            "Near Eastern Mythology" -> R.string.origin_item_near_eastern_mythology
            "New World Mythology" -> R.string.origin_item_new_world_mythology
            "Norse Mythology" -> R.string.origin_item_norse_mythology
            "Roman Mythology" -> R.string.origin_item_roman_mythology
            "Slavic Mythology" -> R.string.origin_item_slavic_mythology
            "Welsh Mythology" -> R.string.origin_item_welsh_mythology
            else -> null
        }

        if (id != null) {
            return ctx.getString(id)
        }

        Log.w(this, "Warning: missing translation for origin: $originName")

        return originName
    }
}
