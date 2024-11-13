package com.example.cs_internal

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TmdbSearchRecyclerViewAdapter(
    private var resultList: MutableList<TmdbSearchResponse.TmdbItem>,
    private val context : Context,
    private val toAddPage : Int
) : RecyclerView.Adapter<TmdbSearchRecyclerViewAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TmdbSearchRecyclerViewAdapter.ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.film_item, parent, false))
    }

    override fun getItemCount(): Int {
        return resultList.size
    }

    override fun onBindViewHolder(holder: TmdbSearchRecyclerViewAdapter.ViewHolder, pos: Int) {
        val current = resultList[pos]
        val imageLink = "https://image.tmdb.org/t/p/w500${current.poster_path}"
        holder.titleView.text = current.title
        holder.descriptionView.maxLines = Integer.MAX_VALUE
        holder.descriptionView.text = current.overview
        var date = current.release_date
        if(date.length>4) date = date.subSequence(0, 4).toString()
        holder.yearView.text = date
        holder.markImage.visibility = GONE
        if(current.poster_path != null){
            Picasso.get().load(imageLink).into(holder.imageView)
        }
        else holder.imageView.visibility = GONE
        holder.tagLayout.visibility = GONE
        holder.optionsButton.setOnClickListener {
            val popupMenu = PopupMenu(context, it)
            popupMenu.menuInflater.inflate(R.menu.tmdb_item_menu, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { menuItem ->
                when(menuItem.itemId){
                    R.id.action_add -> {
                        addFilm(current, imageLink, date)
                        true
                    }
                    R.id.action_translate_to_original -> {
                        translate(pos, current.original_language)
                        true
                    }
                    R.id.action_translate_to_english -> {
                        translate(pos, "en-US")
                        true
                    }
                    R.id.action_translate_to_russian -> {
                        translate(pos, "ru-RU")
                        true
                    }
                    else -> false
                }
            }
            popupMenu.show()
        }
        holder.itemView.setOnClickListener{
            addFilm(current, imageLink, date)
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleView : TextView = itemView.findViewById(R.id.filmTitle)
        val yearView : TextView = itemView.findViewById(R.id.filmYear)
        val descriptionView : TextView = itemView.findViewById(R.id.film_description)
        val optionsButton : ImageButton = itemView.findViewById(R.id.optionsButton)
        val imageView : ImageView = itemView.findViewById(R.id.imageView2)
        val tagLayout : LinearLayout = itemView.findViewById(R.id.tagLayout)
        val markImage : ImageView = itemView.findViewById(R.id.markImage)
    }

    fun changeList(new: MutableList<TmdbSearchResponse.TmdbItem>){
        resultList = new
        notifyDataSetChanged()
    }

    private fun addFilm(current : TmdbSearchResponse.TmdbItem, imageLink : String, year : String) {
        val intent = Intent(context, FilmActivity::class.java)
        intent.putExtra("new", true)
        intent.putExtra("pageNum", toAddPage)
        intent.putExtra("title", current.title)
        intent.putExtra("desc", current.overview)
        intent.putExtra("image", imageLink)
        intent.putExtra("year", year)
        context.startActivity(intent)
    }

    private fun translate(pos : Int, language: String){
        TmdbRetrofitClient.instance.getMovieDetails(
            resultList[pos].id.toString(),
            DataSingleton.TMDB_API_KEY,
            language
        ).enqueue(object :
            Callback<TmdbSearchResponse.TmdbItem> {
            override fun onResponse(call: Call<TmdbSearchResponse.TmdbItem>, response: Response<TmdbSearchResponse.TmdbItem>){
                val result = response.body()
                if(result != null){
                    resultList[pos] = result
                    notifyItemChanged(pos)
                }
                else Toast.makeText(context, "failed\nresult is null", Toast.LENGTH_LONG).show()
            }
            override fun onFailure(call: Call<TmdbSearchResponse.TmdbItem>, t: Throwable) {
                Toast.makeText(context, "Query failed", Toast.LENGTH_LONG).show()
                Toast.makeText(context, t.message, Toast.LENGTH_LONG).show()
            }
        })
    }
}