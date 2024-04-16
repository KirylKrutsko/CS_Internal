package com.example.cs_internal

import android.content.Context
import android.content.Intent
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import com.example.cs_internal.DataSingleton.filmLists
import com.google.firebase.database.DatabaseReference
import java.lang.Exception

class RecyclerViewAdapter(
    private val databaseUserRef : DatabaseReference,
    val context: Context, private val pageNum : Int
) : RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.film_item, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val filmItem = filmLists[pageNum][position]
        holder.titleView.text = filmItem.getTitle()
        holder.descriptionView.text = filmItem.getDescription()
        holder.ratingView.text = filmItem.getRating()
        holder.optionsButton.setOnClickListener{ view->
            val popupMenu = PopupMenu(context, view)
            popupMenu.menuInflater.inflate(R.menu.list_item_menu, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { menuItem ->
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
            val intent = Intent(context, FilmActivity::class.java)
                .apply {
                putExtra("pageNum", pageNum)
                putExtra("itemPosition", position)
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

    override fun getItemCount(): Int = filmLists[pageNum].size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleView : TextView = itemView.findViewById(R.id.filmTitle)
        val ratingView : TextView = itemView.findViewById(R.id.film_rating)
        val descriptionView : TextView = itemView.findViewById(R.id.film_description)
        val optionsButton : ImageButton = itemView.findViewById(R.id.optionsButton)
    }

}