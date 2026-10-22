package ru.yandexpraktikum.cardsanimation.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.yandexpraktikum.cardsanimation.animation.SwapStep
import ru.yandexpraktikum.cardsanimation.animation.Swipe
import ru.yandexpraktikum.cardsanimation.animation.calculateCardRotation
import ru.yandexpraktikum.cardsanimation.animation.classifySwipe
import ru.yandexpraktikum.cardsanimation.animation.reorderCards
import ru.yandexpraktikum.cardsanimation.model.CardData
import kotlin.math.cos
import kotlin.math.sin

private const val DURATION_ANIMATION_MILLS = 300

@Composable
fun AnimatedCardStack(cards: List<CardData>) {
    var order by remember(cards) { mutableStateOf(cards.indices.toList()) }
    var isRotated by remember(cards) { mutableStateOf(false) }
    var busy by remember(cards) { mutableStateOf(false) }
    var step by remember(cards) { mutableStateOf(SwapStep.IDLE) }
    var pendingRotations by remember(cards) { mutableStateOf(emptySet<Int>()) }
    val movement = remember(cards) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val threshold = with(density) { 40.dp.toPx() }
    val distance = with(density) { 50.dp.toPx() }
    val currentBusy by rememberUpdatedState(busy)
    val onSwipe by rememberUpdatedState<(Swipe) -> Unit>({ swipe ->
        if (!busy) {
            if (swipe != Swipe.HORIZONTAL || order.size > 1) {
                busy = true
                scope.launch {
                    try {
                        if (swipe == Swipe.HORIZONTAL) {
                            step = SwapStep.MOVE_OUT
                            movement.animateTo(1f, tween(DURATION_ANIMATION_MILLS))
                            step = SwapStep.MOVE_TO_TOP
                            movement.animateTo(0f, tween(DURATION_ANIMATION_MILLS))
                            step = SwapStep.SETTLE
                            pendingRotations = order.toSet()
                            order = reorderCards(order)
                        } else {
                            val expanded = swipe == Swipe.UP
                            pendingRotations = order.filterIndexed { index, _ ->
                                calculateCardRotation(index, order.size, isRotated) !=
                                    calculateCardRotation(index, order.size, expanded)
                            }.toSet()
                            isRotated = expanded
                        }
                        snapshotFlow { pendingRotations.isEmpty() }.first { it }
                    } finally {
                        pendingRotations = emptySet()
                        step = SwapStep.IDLE
                        busy = false
                    }
                }
            }
        }
    })

    Box(
        modifier = Modifier.fillMaxWidth().height(300.dp)
            .pointerInput(cards, threshold) {
                var drag = Offset.Zero
                var accepted = false
                detectDragGestures(
                    onDragStart = {
                        drag = Offset.Zero
                        accepted = !currentBusy
                    },
                    onDragCancel = { drag = Offset.Zero; accepted = false },
                    onDragEnd = {
                        if (accepted && !currentBusy) {
                            classifySwipe(drag.x, drag.y, threshold)?.let(onSwipe)
                        }
                        drag = Offset.Zero
                        accepted = false
                    }
                ) { change, amount ->
                    change.consume()
                    if (accepted) drag += amount
                }
            },
        contentAlignment = Alignment.Center
    ) {
        order.forEachIndexed { index, id ->
            key(id) {
                val moving = index == 0 &&
                    (step == SwapStep.MOVE_OUT || step == SwapStep.MOVE_TO_TOP)
                val rotation = calculateCardRotation(index, order.size, isRotated)
                val radians = Math.toRadians(rotation.toDouble())
                Box(Modifier.zIndex(if (moving && step == SwapStep.MOVE_TO_TOP) {
                    order.size.toFloat()
                } else index.toFloat())) {
                    AnimatedCard(
                        cardIndex = index,
                        cardData = cards[id],
                        targetRotation = rotation,
                        offsetX = if (moving) distance * movement.value * cos(radians).toFloat() else 0f,
                        offsetY = if (moving) distance * movement.value * sin(radians).toFloat() else 0f,
                        onRotationFinished = { pendingRotations = pendingRotations - id }
                    )
                }
            }
        }
    }
}
