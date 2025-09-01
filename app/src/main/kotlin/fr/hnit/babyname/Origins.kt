package fr.hnit.babyname

import android.content.Context

internal object Origins {
    private val originMap = hashMapOf(
        "african" to R.string.origin_item_african,
        "albanian" to R.string.origin_item_albanian,
        "ancientceltic" to R.string.origin_item_ancient_celtic,
        "ancientegyptian" to R.string.origin_item_ancient_egyptian,
        "ancientgermanic" to R.string.origin_item_ancient_germanic,
        "ancientscandinavian" to R.string.origin_item_ancient_scandinavian,
        "arabic" to R.string.origin_item_arabic,
        "armenian" to R.string.origin_item_armenian,
        "astronomy" to R.string.origin_item_astronomy,
        "basque" to R.string.origin_item_basque,
        "biblical" to R.string.origin_item_biblical,
        "breton" to R.string.origin_item_breton,
        "bulgarian" to R.string.origin_item_bulgarian,
        "catalan" to R.string.origin_item_catalan,
        "celtic" to R.string.origin_item_celtic,
        "chinese" to R.string.origin_item_chinese,
        "cornish" to R.string.origin_item_cornish,
        "croatian" to R.string.origin_item_croatian,
        "czech" to R.string.origin_item_czech,
        "danish" to R.string.origin_item_danish,
        "dutch" to R.string.origin_item_dutch,
        "egyptian" to R.string.origin_item_egyptian,
        "english" to R.string.origin_item_english,
        "esperanto" to R.string.origin_item_esperanto,
        "estonian" to R.string.origin_item_estonian,
        "fareastern" to R.string.origin_item_far_eastern,
        "finnish" to R.string.origin_item_finnish,
        "french" to R.string.origin_item_french,
        "frisian" to R.string.origin_item_frisian,
        "galician" to R.string.origin_item_galician,
        "german" to R.string.origin_item_german,
        "germanic" to R.string.origin_item_germanic,
        "greek" to R.string.origin_item_greek,
        "hawaiian" to R.string.origin_item_hawaiian,
        "hindu" to R.string.origin_item_hindu,
        "history" to R.string.origin_item_history,
        "hungarian" to R.string.origin_item_hungarian,
        "icelandic" to R.string.origin_item_icelandic,
        "indian" to R.string.origin_item_indian,
        "iranian" to R.string.origin_item_iranian,
        "irish" to R.string.origin_item_irish,
        "italian" to R.string.origin_item_italian,
        "japanese" to R.string.origin_item_japanese,
        "jewish" to R.string.origin_item_jewish,
        "judeo-christianlegend" to R.string.origin_item_juedeo_christian_legend,
        "khmer" to R.string.origin_item_khmer,
        "korean" to R.string.origin_item_korean,
        "latvian" to R.string.origin_item_latvian,
        "literature" to R.string.origin_item_literature,
        "lithuanian" to R.string.origin_item_lithuanian,
        "macedonian" to R.string.origin_item_macedonian,
        "manx" to R.string.origin_item_manx,
        "maori" to R.string.origin_item_maori,
        "mormon" to R.string.origin_item_mormon,
        "mythology" to R.string.origin_item_mythology,
        "nativeamerican" to R.string.origin_item_native_american,
        "neareastern" to R.string.origin_item_near_eastern,
        "newworld" to R.string.origin_item_new_world,
        "norse" to R.string.origin_item_norse,
        "norwegian" to R.string.origin_item_norwegian,
        "polish" to R.string.origin_item_polish,
        "portuguese" to R.string.origin_item_portuguese,
        "provençal" to R.string.origin_item_provençal,
        "roman" to R.string.origin_item_roman,
        "romanian" to R.string.origin_item_romanian,
        "russian" to R.string.origin_item_russian,
        "scandinavian" to R.string.origin_item_scandinavian,
        "scottish" to R.string.origin_item_scottish,
        "serbian" to R.string.origin_item_serbian,
        "slavic" to R.string.origin_item_slavic,
        "slovak" to R.string.origin_item_slovak,
        "slovene" to R.string.origin_item_slovene,
        "spanish" to R.string.origin_item_spanish,
        "swedish" to R.string.origin_item_swedish,
        "thai" to R.string.origin_item_thai,
        "theology" to R.string.origin_item_theology,
        "turkish" to R.string.origin_item_turkish,
        "ukrainian" to R.string.origin_item_ukrainian,
        "vietnamese" to R.string.origin_item_vietnamese,
        "welsh" to R.string.origin_item_welsh
    )

    fun getLocaleOrigin(ctx: Context, origin: String?): String {
        if (origin == null) {
            return ""
        }

        val id = originMap[origin]
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
