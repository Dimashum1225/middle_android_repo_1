package ru.yandexpraktikum.cardsanimation.animation

internal const val ROTATION_DURATION = 300

fun calculateCardRotation(cardIndex: Int, cardCount: Int, isRotated: Boolean): Float {
    if (cardCount <= 1) return 0f
    val spread = if (isRotated) 180f else 45f
    return spread / 2 - cardIndex * spread / (cardCount - 1)
}
