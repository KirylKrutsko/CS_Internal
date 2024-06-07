package com.example.cs_internal

import android.content.Context
import android.content.Intent
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import com.example.cs_internal.DataSingleton.filmLists
import com.google.firebase.database.DatabaseReference
import com.google.firebase.storage.StorageReference
import java.lang.Exception

class RecyclerViewAdapter(
    private val databaseUserRef : DatabaseReference,
    private val storageUserRef: StorageReference,
    val context: Context, private var filmList: MutableList<FilmItem>, private val pageNum : Int
) : RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.film_item, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, pos: Int) {
        val filmItem = filmList[pos]
        holder.titleView.text = filmItem.getTitle()
        holder.descriptionView.text = filmItem.getDescription()
        holder.ratingView.text = filmItem.getRating()
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
        else holder.imageView.visibility = GONE
        holder.optionsButton.setOnClickListener { view->
            val popupMenu = PopupMenu(context, view)
            popupMenu.menuInflater.inflate(R.menu.list_item_menu, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { menuItem ->
                val position = holder.adapterPosition
                when (menuItem.itemId) {
                    R.id.action_watch_later -> {
                        moveFilmItem(filmItem, position, 0)
                        true
                    }
                    R.id.action_watching_now -> {
                        moveFilmItem(filmItem, position, 1)
                        true
                    }
                    R.id.action_watched -> {
                        moveFilmItem(filmItem, position, 2)
                        true
                    }
                    else -> false
                }
            }
            popupMenu.show()
        }
        holder.itemView.setOnClickListener(){
            val position = holder.adapterPosition
            val intent = Intent(context, FilmActivity::class.java)
                .apply {
                putExtra("pageNum", pageNum)
                putExtra("id", filmList[position].getDatabaseRef())
            }
            context.startActivity(intent)
        }
    }

    private fun moveFilmItem(filmItem : FilmItem, position : Int, toMove : Int){
        filmItem.changeType(toMove, databaseUserRef, object : DatabaseSyncListener{
            override fun onSuccess() {
                filmLists[toMove].add(0,filmItem)
                DataSingleton.adapters[toMove].notifyItemInserted(0)
                filmLists[pageNum].removeAt(position)
                DataSingleton.adapters[pageNum].notifyItemRemoved(position)
            }
            override fun onFailure(exception: Exception) {
                Toast.makeText(context, exception.toString(), Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun getItemCount(): Int = filmList.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleView : TextView = itemView.findViewById(R.id.filmTitle)
        val ratingView : TextView = itemView.findViewById(R.id.film_rating)
        val descriptionView : TextView = itemView.findViewById(R.id.film_description)
        val optionsButton : ImageButton = itemView.findViewById(R.id.optionsButton)
        val imageView : ImageView = itemView.findViewById(R.id.imageView2)
        val tagLayout : LinearLayout = itemView.findViewById(R.id.tagLayout)
    }

    fun setFilteredList(newList: MutableList<FilmItem>){
        filmList = newList
        notifyDataSetChanged()
    }
    fun resetList(){
        filmList = filmLists[pageNum]
        notifyDataSetChanged()
    }
}