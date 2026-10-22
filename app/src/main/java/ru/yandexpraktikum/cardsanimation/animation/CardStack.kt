package ru.yandexpraktikum.cardsanimation.animation

import kotlin.math.abs

internal const val ROTATION_DURATION = 300

fun calculateCardRotation(cardIndex: Int, cardCount: Int, isRotated: Boolean): Float {
    if (cardCount <= 1) return 0f
    val spread = if (isRotated) 180f else 45f
    return spread / 2 - cardIndex * spread / (cardCount - 1)
}


internal enum class Swipe { UP, DOWN, HORIZONTAL }

internal fun classifySwipe(x: Float, y: Float, threshold: Float): Swipe? = when {
    abs(x) > abs(y) && abs(x) >= threshold -> Swipe.HORIZONTAL
    abs(y) > abs(x) && abs(y) >= threshold -> if (y < 0) Swipe.UP else Swipe.DOWN
    else -> null
}
