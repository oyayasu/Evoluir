package com.oyayasu.evoluir

import androidx.annotation.DrawableRes

data class IconChoice(val id: String, val label: String, @DrawableRes val resource: Int)

/** Stable icon IDs stored in SQLite, not glyphs or array indices. */
object IconCatalog {
    val all = listOf(
        IconChoice("sprout", "Broto", R.drawable.ic_sprout),
        IconChoice("heart", "Empatia", R.drawable.ic_heart),
        IconChoice("clock", "Tempo", R.drawable.ic_clock),
        IconChoice("balance", "Equilíbrio", R.drawable.ic_balance),
        IconChoice("brain", "Consciência", R.drawable.ic_brain),
        IconChoice("target", "Foco", R.drawable.ic_target),
        IconChoice("message", "Diálogo", R.drawable.ic_message),
        IconChoice("shield", "Autocontrole", R.drawable.ic_shield),
        IconChoice("flame", "Motivação", R.drawable.ic_flame),
        IconChoice("leaf", "Paciência", R.drawable.ic_leaf),
        IconChoice("sun", "Clareza", R.drawable.ic_sun),
        IconChoice("users", "Convivência", R.drawable.ic_users),
        IconChoice("hourglass", "Disciplina", R.drawable.ic_hourglass),
        IconChoice("hand", "Gentileza", R.drawable.ic_hand),
        IconChoice("book", "Reflexão", R.drawable.ic_book),
        IconChoice("star", "Prioridades", R.drawable.ic_star)
    )

    @DrawableRes
    fun res(id: String): Int = all.firstOrNull { it.id == id }?.resource ?: R.drawable.ic_sprout
}
