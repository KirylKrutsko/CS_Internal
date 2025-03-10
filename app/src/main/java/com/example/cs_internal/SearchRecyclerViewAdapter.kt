package com.example.cs_internal

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cs_internal.DataSingleton.tags
import com.google.firebase.storage.StorageReference
import java.lang.Exception

class SearchRecyclerViewAdapter(
    private val storageUserRef : StorageReference,
    private val context: Context,
    private val allFilmList: MutableList<FilmItem>
) : RecyclerView.Adapter<SearchRecyclerViewAdapter.ViewHolder>() {

    private var currentFilmList: MutableList<FilmItem> = allFilmList

    private var appliedStringFilter : String = ""
    private val appliedTagFilter : MutableSet<Int> = mutableSetOf()
    private val appliedMarkFilter : MutableSet<Int> = mutableSetOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.film_item, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, pos: Int) {
        holder.markImage.visibility = GONE
        val filmItem = currentFilmList[pos]
        holder.titleView.text = filmItem.getTitle()
        holder.descriptionView.text = filmItem.getDescription()
        holder.yearView.text = filmItem.getYear()?.toString() ?: ""
        holder.optionsButton.visibility = View.INVISIBLE
        val mark = filmItem.getMark()
        when(mark){
            0 -> {
                holder.markImage.setImageResource(R.drawable.emoji_1_svgrepo_com)
                holder.markImage.visibility = VISIBLE
            }
            1 -> {
                holder.markImage.setImageResource(R.drawable.emoji_2_svgrepo_com)
                holder.markImage.visibility = VISIBLE
            }
            2 -> {
                holder.markImage.setImageResource(R.drawable.emoji_3_svgrepo_com)
                holder.markImage.visibility = VISIBLE
            }
            3 -> {
                holder.markImage.setImageResource(R.drawable.emoji_4_svgrepo_com)
                holder.markImage.visibility = VISIBLE
            }
            4 -> {
                holder.markImage.setImageResource(R.drawable.emoji_5_svgrepo_com)
                holder.markImage.visibility = VISIBLE
            }
        }
        holder.tagLayout.removeAllViews()
        for(tag in filmItem.getTags()){
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val itemView = inflater.inflate(R.layout.tag_list_item, null)
            val itemTextView : TextView = itemView.findViewById(R.id.textView)
            itemTextView.text = tag
            holder.tagLayout.addView(itemView)
        }
        if(filmItem.hasImage()){
            filmItem.loadImageIfExists(holder.imageView, context, storageUserRef, object : DatabaseSyncListener{
                override fun onSuccess() {}
                override fun onFailure(exception: Exception) {}
            })
        }
        else {
            holder.imageView.visibility = View.GONE
            holder.descriptionView.height = 50
        }
        holder.itemView.setOnClickListener(){
            val intent = Intent(context, FilmActivity::class.java).apply {
                putExtra("pageNum", 3)
                putExtra("clickedPosition", pos)
                putExtra("id", currentFilmList[pos].getDatabaseRef())
            }
            context.startActivity(intent)
        }
    }

    fun filterByTarget(target: String){ // called when search query is modified
        currentFilmList = filterList(target, appliedTagFilter, appliedMarkFilter)
        notifyDataSetChanged()
        appliedStringFilter = target
    }
    fun filterByTags(tagIndices: MutableSet<Int>){ // called when tags selection is modified
        currentFilmList = filterList(appliedStringFilter, tagIndices, appliedMarkFilter)
        notifyDataSetChanged()
        appliedTagFilter.clear()
        appliedTagFilter.addAll(tagIndices)
    }
    fun filterByMark(marks: MutableSet<Int>){ // called when mark selection is modified
        currentFilmList = filterList(appliedStringFilter, appliedTagFilter, marks)
        notifyDataSetChanged()
        appliedMarkFilter.clear()
        appliedMarkFilter.addAll(marks)
    }

    private fun filterList(target : String, tagIndices : MutableSet<Int>, marks : MutableSet<Int>)
    : MutableList<FilmItem> {
        val filteredList = allFilmList.mapNotNull { filmItem ->
            val markFound = marks.contains(filmItem.getMark())
            var tagFound = false
            for(i in tagIndices){
                if(filmItem.getTags().contains(tags[i])){
                    tagFound = true
                    break
                }
            }
            if((markFound || marks.isEmpty()) && (tagFound || tagIndices.isEmpty())){
                filmItem.smartSearch(target)
                // returns null if match value is too low
                // the FilmItemSearched object otherwise
            }
            else null
        }.toMutableList()
        filteredList.sortDescending() // comparison based on match value defined in FilmItemSearched class
        return filteredList as MutableList<FilmItem>
    }

    fun resetList(){
        currentFilmList = allFilmList
        notifyDataSetChanged()
        appliedStringFilter = ""
        appliedTagFilter.clear()
    }

    fun order(orderOption: String) {
        when(orderOption){
            "edit time : newest first" ->{
                allFilmList.sortByDescending { it.getLastEditTime() }
            }
            "edit time : oldest first" ->{
                allFilmList.sortBy { it.getLastEditTime() }
            }
            "created time : newest first" ->{
                allFilmList.sortByDescending { it.getDatabaseRef() }
            }
            "created time : oldest first" ->{
                allFilmList.sortBy { it.getDatabaseRef() }
            }
            "alphabetical : A to Z" ->{
                allFilmList.sortBy { it.getTitle().toString() }
            }
            "alphabetical : Z to A" ->{
                allFilmList.sortByDescending { it.getTitle().toString() }
            }
            else -> allFilmList.sortByDescending { it.getLastEditTime() }
        }
        currentFilmList = filterList(appliedStringFilter, appliedTagFilter, appliedMarkFilter)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = currentFilmList.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val titleView : TextView = itemView.findViewById(R.id.filmTitle)
        val yearView : TextView = itemView.findViewById(R.id.filmYear)
        val descriptionView : TextView = itemView.findViewById(R.id.film_description)
        val optionsButton : ImageButton = itemView.findViewById(R.id.optionsButton)
        val imageView : ImageView = itemView.findViewById(R.id.imageView2)
        val tagLayout : LinearLayout = itemView.findViewById(R.id.tagLayout)
        val markImage : ImageView = itemView.findViewById(R.id.markImage)
    }
}