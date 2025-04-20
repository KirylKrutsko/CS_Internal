package com.example.cs_internal

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlin.math.abs

// the class prevents swipe refreshes when swiping right/left between pages
// by customizing the swiping condition
class CustomSwipeRefreshLayout(context: Context, attrs: AttributeSet) : SwipeRefreshLayout(context, attrs) {
    private var startX: Float = 0f
    private var startY: Float = 0f
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x
                startY = event.y
                return super.onInterceptTouchEvent(event)
            }
            MotionEvent.ACTION_MOVE -> {
                val endX = event.x
                val endY = event.y
                val diffX = abs(endX - startX)
                val diffY = abs(endY - startY)
                if (diffX > touchSlop && diffY < diffX * 5) { // the slope of slide at least 5 for intensively vertical swipe
                    return false
                }
            }
        }
        return super.onInterceptTouchEvent(event)
    }
}
