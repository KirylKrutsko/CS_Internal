package com.example.cs_internal

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.google.firebase.database.DatabaseReference

class PagerAdapter(fm: FragmentManager, ref : DatabaseReference, context : Context) : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    private val fragments : List<PageFragment> = listOf(
        PageFragment(ref, context, 0),
        PageFragment(ref, context, 1),
        PageFragment(ref, context, 2)
    )

    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> fragments[0]
            1 -> fragments[1]
            2 -> fragments[2]
            else -> fragments[0]
        }
    }

    override fun getPageTitle(position: Int): CharSequence {
        return ""
    }

    override fun getCount(): Int {
        return 3
    }

    fun setAdapters(){
        DataSingleton.adapters.add(fragments[0].myAdapter)
        DataSingleton.adapters.add(fragments[1].myAdapter)
        DataSingleton.adapters.add(fragments[2].myAdapter)
    }
}