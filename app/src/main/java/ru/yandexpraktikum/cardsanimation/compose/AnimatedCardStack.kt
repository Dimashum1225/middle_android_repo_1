package ru.yandexpraktikum.cardsanimation.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.yandexpraktikum.cardsanimation.animation.*
import ru.yandexpraktikum.cardsanimation.model.CardData
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity

@Composable
fun AnimatedCardStack(cards: List<CardData>) {
    var isRotated by remember(cards) { mutableStateOf(false) }
    var order by remember(cards) { mutableStateOf(cards.indices.toList()) }
    val threshold = with(LocalDensity.current) { 40.dp.toPx() }
    val onSwipe by rememberUpdatedState<(Swipe) -> Unit>({ swipe ->
        when (swipe) {
            Swipe.UP -> isRotated = true
            Swipe.DOWN -> isRotated = false
            Swipe.HORIZONTAL -> order = reorderCards(order)
        }
    })
    Box(
        modifier = Modifier.fillMaxWidth().height(300.dp).clickable { isRotated = !isRotated }
            .pointerInput(cards, threshold) {
                var drag = Offset.Zero
                detectDragGestures(
                    onDragStart = { drag = Offset.Zero },
                    onDragCancel = { drag = Offset.Zero },
                    onDragEnd = {
                        classifySwipe(drag.x, drag.y, threshold)?.let(onSwipe)
                        drag = Offset.Zero
                    }
                ) { change, amount ->
                    change.consume()
                    drag += amount
                }
            },
        contentAlignment = Alignment.Center
    ) {
        order.forEachIndexed { index, id ->
            key(id) {
                AnimatedCard(index, cards[id], calculateCardRotation(index, cards.size, isRotated))
            }
        }
    }
}
