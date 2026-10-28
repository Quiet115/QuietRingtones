package com.quietringtones.core

/** Un sonido del catálogo online: un clip corto que se oye con una vista previa y se guarda al usarlo. */
data class OnlineSound(
    val id: String,
    val title: String,
    val artist: String,
    val kinds: Set<SoundKind>,
    val url: String,
    val seconds: Double,
    val bytes: Long,
    val sha256: String,
    val license: String,
    val source: String,
) {
    /** Una vez usado queda entre los audios del usuario con su título como nombre. */
    fun isAddedTo(yours: List<Sound>): Boolean = yours.any { it.name == title }
}

/** Los sonidos del catálogo que valen para [kind], en el orden del catálogo. */
fun List<OnlineSound>.forKind(kind: SoundKind): List<OnlineSound> = filter { kind in it.kinds }