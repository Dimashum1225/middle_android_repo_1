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

@Composable
fun AnimatedCardStack(cards: List<CardData>) {
    var isRotated by remember(cards) { mutableStateOf(false) }
    var order by remember(cards) { mutableStateOf(cards.indices.toList()) }
    Box(
        modifier = Modifier.fillMaxWidth().height(300.dp).clickable { isRotated = !isRotated },
        contentAlignment = Alignment.Center
    ) {
        order.forEachIndexed { index, id ->
            key(id) {
                AnimatedCard(index, cards[id], calculateCardRotation(index, cards.size, isRotated))
            }
        }
    }
}
