package com.quietringtones

/**
 * Android devuelve el mismo sonido con formas distintas de URI según de dónde se lea
 * (`content://0@media/...?title=...` frente a `content://media/...`). Se guarda y compara la forma simple.
 */
object SoundId {
    private val userInfo = Regex("^(content://)[^/@]+@")

    fun normalize(id: String): String = id.substringBefore('?').replace(userInfo, "$1")
}
