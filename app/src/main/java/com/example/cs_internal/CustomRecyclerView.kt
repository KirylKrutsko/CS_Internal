package com.example.cs_internal

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView

class CustomRecyclerView(context: Context, attributeSet: AttributeSet) : RecyclerView(context, attributeSet) {

    private lateinit var scrollUpButton : ImageButton // button for quick scroll to the top
    private var buttonIsVisible = false

    fun setButton(button: ImageButton){
        scrollUpButton = button
        scrollUpButton.setOnClickListener {
            smoothScrollToPosition(0)
        }
    }
    override fun onScrolled(dx: Int, dy: Int) {
        // button appears when starts scrolling up
        super.onScrolled(dx, dy)
        if(dy < -10 && !buttonIsVisible){
            scrollUpButton.animate().translationY(-280f).setDuration(300).start()
            buttonIsVisible = true
        }
        else if(dy > 0 && buttonIsVisible){
            scrollUpButton.animate().translationY(0f).setDuration(300).start()
            buttonIsVisible = false
        }
        else if(!canScrollVertically(-1) && buttonIsVisible){
            scrollUpButton.animate().translationY(0f).setDuration(300).start()
            buttonIsVisible = false
        }
    }
}