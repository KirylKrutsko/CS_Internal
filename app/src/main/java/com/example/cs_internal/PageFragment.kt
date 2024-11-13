package com.example.cs_internal

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import com.example.cs_internal.DataSingleton.filmLists
import com.google.firebase.database.DatabaseReference
import com.google.firebase.storage.StorageReference

class PageFragment(databaseUserRef : DatabaseReference, storageUserRef: StorageReference, context : Context, pageNum : Int) : Fragment() {

    val recyclerViewAdapter = MainRecyclerViewAdapter(databaseUserRef, storageUserRef, context, filmLists[pageNum], pageNum)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.page_fragment, container, false)
        val recyclerView : CustomRecyclerView = view.findViewById(R.id.list)
        recyclerView.adapter = recyclerViewAdapter
        recyclerView.setButton(view.findViewById(R.id.scrollUpButton))
        return view
    }
}