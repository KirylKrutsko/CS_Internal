package com.example.cs_internal

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.TextView
import com.example.cs_internal.DataSingleton.filmLists
import com.google.android.play.integrity.internal.w
import com.google.firebase.database.DatabaseReference
import com.google.firebase.storage.StorageReference

class PageFragment(databaseUserRef : DatabaseReference, storageUserRef: StorageReference, context : Context, pageNum : Int) : Fragment() {

    private var columnCount = 1
    val myAdapter = RecyclerViewAdapter(databaseUserRef, storageUserRef, context, filmLists[pageNum], pageNum)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            columnCount = it.getInt(ARG_COLUMN_COUNT)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.page_fragment, container, false)
        val recyclerView : RecyclerView = view.findViewById(R.id.list)
        with(recyclerView) {
            layoutManager = when {
                columnCount <= 1 -> LinearLayoutManager(context)
                else -> GridLayoutManager(context, columnCount)
            }
            adapter = myAdapter
        }
        return view
    }

    companion object {
        const val ARG_COLUMN_COUNT = "column-count"
        @JvmStatic
        fun newInstance(columnCount: Int, databaseUserRef : DatabaseReference, storageUserRef: StorageReference, context: Context, pageNum: Int, allAdapters: List<RecyclerViewAdapter>) =
            PageFragment(databaseUserRef, storageUserRef, context, pageNum).apply {
                arguments = Bundle().apply {
                    putInt(ARG_COLUMN_COUNT, columnCount)
                }
            }
    }
}