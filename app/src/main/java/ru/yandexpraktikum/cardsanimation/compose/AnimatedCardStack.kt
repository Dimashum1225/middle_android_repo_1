package ru.yandexpraktikum.cardsanimation.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.yandexpraktikum.cardsanimation.animation.*
import ru.yandexpraktikum.cardsanimation.model.CardData
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnimatedCardStack(cards: List<CardData>) {
    var order by remember(cards) { mutableStateOf(cards.indices.toList()) }
    var isRotated by remember(cards) { mutableStateOf(false) }
    var busy by remember(cards) { mutableStateOf(false) }
    var step by remember(cards) { mutableStateOf(SwapStep.IDLE) }
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
                            movement.animateTo(1f, tween(300))
                            step = SwapStep.MOVE_TO_TOP
                            movement.animateTo(0f, tween(300))
                            step = SwapStep.SETTLE
                            order = reorderCards(order)
                        } else {
                            isRotated = swipe == Swipe.UP
                        }
                        delay(ROTATION_DURATION.toLong())
                    } finally {
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
                        offsetY = if (moving) distance * movement.value * sin(radians).toFloat() else 0f
                    )
                }
            }
        }
    }
}
