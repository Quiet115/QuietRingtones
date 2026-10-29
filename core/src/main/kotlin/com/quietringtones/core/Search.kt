package com.quietringtones.core

import java.text.Normalizer

/** Búsqueda por título: sin distinguir mayúsculas ni acentos, y todas las palabras escritas deben aparecer. */
object Search {
    fun matches(title: String, query: String): Boolean {
        val words = normalize(query).split(' ').filter { it.isNotEmpty() }
        if (words.isEmpty()) return true
        val haystack = normalize(title)
        return words.all { it in haystack }
    }

    private fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase()
            .trim()
}

fun List<Sound>.matching(query: String): List<Sound> = filter { Search.matches(it.name, query) }

fun List<OnlineSound>.matchingOnline(query: String): List<OnlineSound> =
    filter { Search.matches(it.title, query) || Search.matches(it.artist, query) }