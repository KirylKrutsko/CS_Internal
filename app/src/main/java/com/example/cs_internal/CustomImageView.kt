package com.example.cs_internal

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

class CustomImageView : AppCompatImageView {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    // manually adjusts the dimensions of ImageView when the source is reloaded
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val drawable: Drawable? = drawable
        if (drawable != null) {
            val width = MeasureSpec.getSize(widthMeasureSpec)
            val diw = drawable.intrinsicWidth
            if (diw > 0) {
                val dih = drawable.intrinsicHeight
                val ratio = diw.toFloat() / dih.toFloat()
                setMeasuredDimension(width, (width / ratio).toInt()) // adjust to ratio
            } else {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            }
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }
}
