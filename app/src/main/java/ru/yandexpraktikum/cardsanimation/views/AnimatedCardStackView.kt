package ru.yandexpraktikum.cardsanimation.views

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.widget.FrameLayout
import ru.yandexpraktikum.cardsanimation.animation.*
import ru.yandexpraktikum.cardsanimation.model.CardData
import kotlin.math.cos
import kotlin.math.sin

class AnimatedCardStackView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private var cardDataList: List<CardData> = emptyList()
    private val cards = mutableListOf<AnimatedCardView>()
    private var isRotated = false
    private var step = SwapStep.IDLE
    private var animator: AnimatorSet? = null
    private var horizontalDragOffset = 0f
    private var verticalDragOffset = 0f
    private var gestureHandled = false
    private var gestureAccepted = false
    private val density get() = resources.displayMetrics.density
    private val busy get() = step != SwapStep.IDLE || animator != null

    private val gestureDetector = GestureDetector(context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent) = true

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                if (gestureAccepted && !busy) {
                    gestureHandled = true
                    performClick()
                }
                return true
            }

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
                if (gestureAccepted && !gestureHandled && !busy) {
                    classifySwipe(velocityX, velocityY, 800f * density)?.let {
                        gestureHandled = true
                        handleSwipe(it)
                    }
                }
                return true
            }
        })

    init {
        isClickable = true
        clipChildren = false
        clipToPadding = false
    }

    fun setCards(newCardDataList: List<CardData>) {
        cancelAnimation()
        cardDataList = newCardDataList.toList()
        cards.clear()
        removeAllViews()
        isRotated = false
        cardDataList.forEachIndexed { index, data ->
            val card = AnimatedCardView(context).apply {
                setCardData(data)
                setStackPosition(index)
            }
            cards.add(card)
            addView(card, LayoutParams((100 * density).toInt(), (160 * density).toInt(), Gravity.CENTER))
        }
        updateCardPositions()
    }

    private fun updateCardPositions() {
        cards.forEachIndexed { index, card ->
            val rotation = calculateCardRotation(index, cards.size, isRotated)
            card.setStackPosition(index)
            card.elevation = index * density
            card.rotation = rotation
        }
    }

    override fun onInterceptTouchEvent(event: MotionEvent) = true

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            horizontalDragOffset = 0f
            verticalDragOffset = 0f
            gestureHandled = false
            gestureAccepted = !busy
            parent?.requestDisallowInterceptTouchEvent(true)
        }
        gestureDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_UP -> {
                handleDragEnd()
                parent?.requestDisallowInterceptTouchEvent(false)
            }
            MotionEvent.ACTION_CANCEL -> {
                gestureAccepted = false
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        if (!busy) handleSwipe(if (isRotated) Swipe.DOWN else Swipe.UP)
        return true
    }

    private fun handleDragEnd() {
        if (gestureAccepted && !gestureHandled && !busy) {
            classifySwipe(horizontalDragOffset, verticalDragOffset, 40 * density)?.let {
                gestureHandled = true
                handleSwipe(it)
            }
        }
        gestureAccepted = false
    }

    private fun handleSwipe(swipe: Swipe) {
        when (swipe) {
            Swipe.HORIZONTAL -> if (cards.size > 1) startCardSwapAnimation(cards.first())
            Swipe.UP, Swipe.DOWN -> {
                isRotated = swipe == Swipe.UP
                animateRotations()
            }
        }
    }

    private fun animateRotations(onComplete: () -> Unit = {}) {
        runAnimation(cards.mapIndexed { index, card ->
            card.createRotationAnimator(calculateCardRotation(index, cards.size, isRotated))
        }, ROTATION_DURATION.toLong(), onComplete)
    }

    private fun startCardSwapAnimation(bottomCard: AnimatedCardView) {
        val radians = Math.toRadians(bottomCard.rotation.toDouble())
        val distance = 50 * density
        step = SwapStep.MOVE_OUT
        runAnimation(listOf(
            ObjectAnimator.ofFloat(bottomCard, "translationX", distance * cos(radians).toFloat()),
            ObjectAnimator.ofFloat(bottomCard, "translationY", distance * sin(radians).toFloat())
        ), 300) {
            step = SwapStep.MOVE_TO_TOP
            bottomCard.cardView.cardElevation = (4 + cards.size) * density
            bottomCard.elevation = cards.size * density
            bottomCard.bringToFront()
            runAnimation(listOf(
                ObjectAnimator.ofFloat(bottomCard, "translationX", 0f),
                ObjectAnimator.ofFloat(bottomCard, "translationY", 0f)
            ), 300) {
                step = SwapStep.SETTLE
                cardDataList = reorderCards(cardDataList)
                cards.removeAt(0)
                cards.add(bottomCard)
                cards.forEachIndexed { index, card ->
                    card.setStackPosition(index)
                    card.elevation = index * density
                }
                animateRotations { step = SwapStep.IDLE }
            }
        }
    }

    private fun runAnimation(parts: List<Animator>, duration: Long, onComplete: () -> Unit) {
        val set = AnimatorSet()
        set.playTogether(parts)
        set.duration = duration
        set.addListener(object : AnimatorListenerAdapter() {
            private var cancelled = false
            override fun onAnimationCancel(animation: Animator) { cancelled = true }
            override fun onAnimationEnd(animation: Animator) {
                if (animator === set) animator = null
                if (!cancelled) onComplete()
            }
        })
        animator = set
        set.start()
    }

    private fun cancelAnimation() {
        animator?.cancel()
        animator = null
        step = SwapStep.IDLE
        cards.forEach {
            it.animate().cancel()
            it.translationX = 0f
            it.translationY = 0f
        }
    }

    override fun onDetachedFromWindow() {
        cancelAnimation()
        updateCardPositions()
        super.onDetachedFromWindow()
    }
}
