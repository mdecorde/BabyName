/*
* Copyright (C) 2025 Baby Name Developers
* SPDX-License-Identifier: GPL-3.0-or-later
*/

package fr.hnit.babyname

import android.content.Context

internal object Origins {
    fun getLocaleOrigin(ctx: Context, origin: String?): String {
        if (origin == null) {
            return ""
        }

        val id = when (origin) {
            "african" -> R.string.origin_item_african
            "albanian" -> R.string.origin_item_albanian
            "ancientceltic" -> R.string.origin_item_ancient_celtic
            "ancientegyptian" -> R.string.origin_item_ancient_egyptian
            "ancientgermanic" -> R.string.origin_item_ancient_germanic
            "ancientscandinavian" -> R.string.origin_item_ancient_scandinavian
            "arabic" -> R.string.origin_item_arabic
            "armenian" -> R.string.origin_item_armenian
            "astronomy" -> R.string.origin_item_astronomy
            "basque" -> R.string.origin_item_basque
            "biblical" -> R.string.origin_item_biblical
            "breton" -> R.string.origin_item_breton
            "bulgarian" -> R.string.origin_item_bulgarian
            "catalan" -> R.string.origin_item_catalan
            "celtic" -> R.string.origin_item_celtic
            "chinese" -> R.string.origin_item_chinese
            "cornish" -> R.string.origin_item_cornish
            "croatian" -> R.string.origin_item_croatian
            "czech" -> R.string.origin_item_czech
            "danish" -> R.string.origin_item_danish
            "dutch" -> R.string.origin_item_dutch
            "egyptian" -> R.string.origin_item_egyptian
            "english" -> R.string.origin_item_english
            "esperanto" -> R.string.origin_item_esperanto
            "estonian" -> R.string.origin_item_estonian
            "fareastern" -> R.string.origin_item_far_eastern
            "finnish" -> R.string.origin_item_finnish
            "french" -> R.string.origin_item_french
            "frisian" -> R.string.origin_item_frisian
            "galician" -> R.string.origin_item_galician
            "german" -> R.string.origin_item_german
            "germanic" -> R.string.origin_item_germanic
            "greek" -> R.string.origin_item_greek
            "hawaiian" -> R.string.origin_item_hawaiian
            "hindu" -> R.string.origin_item_hindu
            "history" -> R.string.origin_item_history
            "hungarian" -> R.string.origin_item_hungarian
            "icelandic" -> R.string.origin_item_icelandic
            "indian" -> R.string.origin_item_indian
            "iranian" -> R.string.origin_item_iranian
            "irish" -> R.string.origin_item_irish
            "italian" -> R.string.origin_item_italian
            "japanese" -> R.string.origin_item_japanese
            "jewish" -> R.string.origin_item_jewish
            "judeo-christianlegend" -> R.string.origin_item_juedeo_christian_legend
            "khmer" -> R.string.origin_item_khmer
            "korean" -> R.string.origin_item_korean
            "latvian" -> R.string.origin_item_latvian
            "literature" -> R.string.origin_item_literature
            "lithuanian" -> R.string.origin_item_lithuanian
            "macedonian" -> R.string.origin_item_macedonian
            "manx" -> R.string.origin_item_manx
            "maori" -> R.string.origin_item_maori
            "mormon" -> R.string.origin_item_mormon
            "mythology" -> R.string.origin_item_mythology
            "nativeamerican" -> R.string.origin_item_native_american
            "neareastern" -> R.string.origin_item_near_eastern
            "newworld" -> R.string.origin_item_new_world
            "norse" -> R.string.origin_item_norse
            "norwegian" -> R.string.origin_item_norwegian
            "polish" -> R.string.origin_item_polish
            "portuguese" -> R.string.origin_item_portuguese
            "provençal" -> R.string.origin_item_provençal
            "roman" -> R.string.origin_item_roman
            "romanian" -> R.string.origin_item_romanian
            "russian" -> R.string.origin_item_russian
            "scandinavian" -> R.string.origin_item_scandinavian
            "scottish" -> R.string.origin_item_scottish
            "serbian" -> R.string.origin_item_serbian
            "slavic" -> R.string.origin_item_slavic
            "slovak" -> R.string.origin_item_slovak
            "slovene" -> R.string.origin_item_slovene
            "spanish" -> R.string.origin_item_spanish
            "swedish" -> R.string.origin_item_swedish
            "thai" -> R.string.origin_item_thai
            "theology" -> R.string.origin_item_theology
            "turkish" -> R.string.origin_item_turkish
            "ukrainian" -> R.string.origin_item_ukrainian
            "vietnamese" -> R.string.origin_item_vietnamese
            "welsh" -> R.string.origin_item_welsh
            else -> null
        }

        if (id != null) {
            return ctx.getString(id)
        }

        Log.w(this, "Warning: missing translation for origin: $origin")

        // Fallback: Make first character upper case.
        return origin.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase() else it.toString()
        }
    }
}
