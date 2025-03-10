package com.example.cs_internal

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast

class SortLinearLayout(context: Context, attributeSet: AttributeSet) : LinearLayout(context, attributeSet){

    private val selectedItems : MutableSet<Int> = mutableSetOf()
    fun set(adapter: SearchRecyclerViewAdapter, isTagLayout : Boolean){
        for(i in 0 until childCount){
            selectedItems.add(i)
            val child = getChildAt(i)
            child.setOnClickListener{
                if(selectedItems.size == childCount){
                    unselectAll()
                    selectItem(i)
                }
                else if(selectedItems.contains(i)){
                    unselectItem(i)
                    if(selectedItems.size == 0){
                        selectAll()
                    }
                }
                else{
                    selectItem(i)
                }
                val selectedItemsOrEmpty : MutableSet<Int> = mutableSetOf()
                if(selectedItems.size != childCount) selectedItemsOrEmpty.addAll(selectedItems)
                if(isTagLayout) adapter.filterByTags(selectedItemsOrEmpty)
                else adapter.filterByMark(selectedItemsOrEmpty)
            }
        }
    }

    private fun selectItem(pos : Int){
        val item = getChildAt(pos)
        item.alpha = 1f
        selectedItems.add(pos)
    }
    private fun unselectItem(pos : Int){
        val item = getChildAt(pos)
        item.alpha = 0.2f
        selectedItems.remove(pos)
    }
    private fun selectAll(){
        for(i in 0 until childCount){
            selectItem(i)
        }
    }
    private fun unselectAll(){
        for(i in 0 until childCount){
            unselectItem(i)
        }
    }
}