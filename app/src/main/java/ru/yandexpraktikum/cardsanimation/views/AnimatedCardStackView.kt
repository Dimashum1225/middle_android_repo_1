package ru.yandexpraktikum.cardsanimation.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import ru.yandexpraktikum.cardsanimation.animation.*
import ru.yandexpraktikum.cardsanimation.model.CardData
import android.view.GestureDetector
import android.view.MotionEvent

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

    private var horizontalDragOffset = 0f
    private var verticalDragOffset = 0f
    private var gestureHandled = false
    private val gestureDetector = GestureDetector(context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent) = true
            override fun onSingleTapUp(e: MotionEvent): Boolean = performClick()
            override fun onScroll(
                e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float
            ): Boolean {
                horizontalDragOffset -= distanceX
                verticalDragOffset -= distanceY
                return true
            }
            override fun onFling(
                e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float
            ): Boolean {
                classifySwipe(velocityX, velocityY, 800 * density)?.let {
                    gestureHandled = true
                    handleSwipe(it)
                }
                return true
            }
        })

    override fun onInterceptTouchEvent(event: MotionEvent) = true

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            horizontalDragOffset = 0f
            verticalDragOffset = 0f
            gestureHandled = false
        }
        gestureDetector.onTouchEvent(event)
        if (event.actionMasked == MotionEvent.ACTION_UP) handleDragEnd()
        return true
    }

    private fun handleDragEnd() {
        if (!gestureHandled) {
            classifySwipe(horizontalDragOffset, verticalDragOffset, 40 * density)?.let(::handleSwipe)
        }
    }

    private fun handleSwipe(swipe: Swipe) {
        android.util.Log.d("CardStack", "Swipe: $swipe")
    }
}
