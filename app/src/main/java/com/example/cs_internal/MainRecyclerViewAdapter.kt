package com.example.cs_internal

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import androidx.core.view.setPadding
import com.example.cs_internal.DataSingleton.adapters
import com.example.cs_internal.DataSingleton.tags
import com.google.firebase.database.DatabaseReference
import com.google.firebase.storage.StorageReference
import java.lang.Exception

class MainRecyclerViewAdapter(
    private val databaseUserRef : DatabaseReference,
    private val storageUserRef: StorageReference,
    val context: Context,
    private val filmList: MutableList<FilmItem>,
    private val pageNum : Int
) : RecyclerView.Adapter<MainRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.film_item, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, startPosition: Int) {
        val filmItem = filmList[startPosition]
        holder.titleView.text = filmItem.getTitle()
        holder.descriptionView.text = filmItem.getDescription()
        val year = filmItem.getYear()?.toString() ?: ""
        holder.yearView.text = year
        when(filmItem.getMark()){
            0 -> {
                holder.markImage.setImageResource(R.drawable.emoji_1_svgrepo_com)
                holder.markImage.visibility = View.VISIBLE
            }
            1 -> {
                holder.markImage.setImageResource(R.drawable.emoji_2_svgrepo_com)
                holder.markImage.visibility = View.VISIBLE
            }
            2 -> {
                holder.markImage.setImageResource(R.drawable.emoji_3_svgrepo_com)
                holder.markImage.visibility = View.VISIBLE
            }
            3 -> {
                holder.markImage.setImageResource(R.drawable.emoji_4_svgrepo_com)
                holder.markImage.visibility = View.VISIBLE
            }
            4 -> {
                holder.markImage.setImageResource(R.drawable.emoji_5_svgrepo_com)
                holder.markImage.visibility = View.VISIBLE
            }
            else -> holder.markImage.visibility = GONE
        }
        holder.tagLayout.removeAllViews()
        for(tag in filmItem.getTags()){
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val itemView = inflater.inflate(R.layout.tag_list_item, null)
            val textView : TextView = itemView.findViewById(R.id.textView)
            val button : ImageButton = itemView.findViewById(R.id.imageButton)
            textView.text = tag
            holder.tagLayout.addView(itemView)
            button.setOnClickListener {
                val intent = Intent(context, SearchActivity::class.java)
                    .putExtra("tagName", tag)
                context.startActivity(intent)
            }
        }
        if(filmItem.hasImage()){
            filmItem.loadImageIfExists(holder.imageView, context, storageUserRef, object : DatabaseSyncListener{
                override fun onSuccess() {}
                override fun onFailure(exception: Exception) {}
            })
        }
        else {
            holder.imageView.visibility = GONE
            holder.descriptionView.height = 50
        }
        holder.optionsButton.setOnClickListener { view->
            val popupMenu = PopupMenu(context, view)
            popupMenu.menuInflater.inflate(R.menu.list_item_menu, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { menuItem ->
                val pos = holder.position
                when (menuItem.itemId) {
                    R.id.action_watch_later -> {
                        moveFilmItem(filmItem, pos, 0)
                        true
                    }
                    R.id.action_watching_now -> {
                        moveFilmItem(filmItem, pos, 1)
                        true
                    }
                    R.id.action_watched -> {
                        moveFilmItem(filmItem, pos, 2)
                        true
                    }
                    R.id.action_delete -> {
                        setDeleteDialog(filmItem, pos)
                        true
                    }
                    else -> false
                }
            }
            popupMenu.show()
        }
        holder.itemView.setOnClickListener(){
            val pos = holder.position
            val intent = Intent(context, FilmActivity::class.java).apply {
                putExtra("pageNum", pageNum)
                putExtra("clickedPosition", pos)
            }
            context.startActivity(intent)
        }
    }

    private fun moveFilmItem(filmItem : FilmItem, position : Int, pageToMove : Int){
        filmItem.changeType(pageToMove, databaseUserRef, object : DatabaseSyncListener{
            override fun onSuccess() {
                filmList.removeAt(position)
                notifyItemRemoved(position)
                adapters[pageToMove].filmList.add(0, filmItem)
                adapters[pageToMove].notifyItemInserted(0)
            }
            override fun onFailure(exception: Exception) {
                Toast.makeText(context, "Action failed.", Toast.LENGTH_SHORT).show()
                Toast.makeText(context, exception.message, Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun setDeleteDialog(filmItem: FilmItem, pos: Int) {
        val view = TextView(context)
        view.setPadding(50)
        view.setText("Once you press delete button, the removed item cannot be restored!\nAre you sure to continue?")
        view.setTextColor(Color.RED)
        val dialog = AlertDialog.Builder(context)
            .setTitle("Delete film?")
            .setView(view)
            .setPositiveButton("Delete"){ _,_ ->
                deleteItem(filmItem, pos)
            }
            .setNeutralButton("Cancel"){_,_ ->}
            .create()
        dialog.show()
    }
    private fun deleteItem(filmItem: FilmItem, position: Int){
        filmList.removeAt(position)
        notifyItemRemoved(position)
        filmItem.deleteFilm(databaseUserRef, storageUserRef, context, object : DatabaseSyncListener{
            override fun onSuccess() {}
            override fun onFailure(exception: Exception) {
                Toast.makeText(context, "Failed to delete from database", Toast.LENGTH_SHORT).show()
                Toast.makeText(context, exception.message, Toast.LENGTH_LONG).show()
            }
        })
    }

    override fun getItemCount(): Int = filmList.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleView : TextView = itemView.findViewById(R.id.filmTitle)
        val yearView : TextView = itemView.findViewById(R.id.filmYear)
        val descriptionView : TextView = itemView.findViewById(R.id.film_description)
        val optionsButton : ImageButton = itemView.findViewById(R.id.optionsButton)
        val imageView : ImageView = itemView.findViewById(R.id.imageView2)
        val tagLayout : LinearLayout = itemView.findViewById(R.id.tagLayout)
        val markImage : ImageView = itemView.findViewById(R.id.markImage)
    }
}