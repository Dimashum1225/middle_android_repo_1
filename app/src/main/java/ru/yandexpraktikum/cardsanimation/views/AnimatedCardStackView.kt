package ru.yandexpraktikum.cardsanimation.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import ru.yandexpraktikum.cardsanimation.animation.*
import ru.yandexpraktikum.cardsanimation.model.CardData

class AnimatedCardStackView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private val cards = mutableListOf<AnimatedCardView>()
    private var cardDataList: List<CardData> = emptyList()
    private var isRotated = false
    private val density get() = resources.displayMetrics.density

    init {
        clipChildren = false
        clipToPadding = false
        setOnClickListener {
            isRotated = !isRotated
            updateCardPositions(true)
        }
    }

    fun setCards(newCardDataList: List<CardData>) {
        cards.forEach { it.animate().cancel() }
        cards.clear()
        removeAllViews()
        cardDataList = newCardDataList.toList()
        isRotated = false
        cardDataList.forEach { data ->
            val card = AnimatedCardView(context).apply { setCardData(data) }
            cards.add(card)
            addView(card, LayoutParams((100 * density).toInt(), (160 * density).toInt(), Gravity.CENTER))
        }
        updateCardPositions(false)
    }

    private fun updateCardPositions(animated: Boolean) {
        cards.forEachIndexed { index, card ->
            card.setStackPosition(index)
            card.elevation = index * density
            val rotation = calculateCardRotation(index, cards.size, isRotated)
            if (animated) {
                card.createRotationAnimator(rotation).apply {
                    duration = ROTATION_DURATION.toLong()
                    start()
                }
            } else card.rotation = rotation
        }
    }
}
