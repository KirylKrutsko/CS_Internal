package com.example.cs_internal

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import com.google.firebase.database.*
import com.google.firebase.storage.StorageReference
import com.google.gson.annotations.Expose
import com.squareup.picasso.Picasso
import java.io.File
import java.io.Serializable
import java.util.*
import kotlin.collections.HashMap

// open allows inheritance
open class FilmItem(
    @Expose private var title : String,
    @Expose private var type: Int,
    private var databaseRef : String
) : Serializable {
    // expose notation defines which fields to put in export files
    @Expose private var description : String? = null
    @Expose private var rating: String? = null
    private var imageLink : String? = null
    @Expose private var mark : Int? = null
    @Expose private var commentary : String? = null
    @Expose private var link : String? = null
    @Expose private var year : Int? = null
    @Expose private var currentWatchTime : Int? = null
    @Expose private var tags : MutableList<String> = mutableListOf()
    @Expose private var comments : MutableList<Comment> = mutableListOf()
    @Expose var isASeries : Boolean = false
    @Expose private var lastEditTime : Long = System.currentTimeMillis()

    // constructor to create a new empty film item
    // also creates a field in database
    constructor(title: String, type: Int, databaseUserRef: DatabaseReference, listener: DatabaseSyncListener)
            : this(title, type, "") {
        addFilmToDatabase(databaseUserRef, listener)
    }

    // full constructor to save existing item from database
    constructor(
        title: String, type: Int, databaseRef : String, description : String?, rating : String?, imageLink : String?,
        mark : Int?, commentary: String?, link : String?, year : Int?, currentWatchTime : Int?, lastEditTime : Long,
        tags: MutableList<String>, comments: MutableList<Comment>, isASeries : Boolean
    ) : this(title, type, databaseRef){
        this.description = description
        this.commentary = commentary
        this.rating = rating
        this.imageLink = imageLink
        this.link = link
        this.mark = mark
        this.year = year
        this.currentWatchTime = currentWatchTime
        this.lastEditTime = lastEditTime
        this.tags = tags
        this.comments = comments
        this.isASeries = isASeries
    }

    // simple copying, used to create a searched copy
    constructor(filmItem: FilmItem) : this(filmItem.title, filmItem.type, filmItem.databaseRef) {
        this.description = filmItem.description
        this.commentary = filmItem.commentary
        this.rating = filmItem.rating
        this.imageLink = filmItem.imageLink
        this.link = filmItem.link
        this.mark = filmItem.mark
        this.year = filmItem.year
        this.currentWatchTime = filmItem.currentWatchTime
        this.lastEditTime = filmItem.lastEditTime
        this.tags = filmItem.tags
        this.comments = filmItem.comments
        this.isASeries = filmItem.isASeries
    }

    private fun toHashMap() : HashMap<String, *>{
        return hashMapOf(
            "title" to title,
            "type" to type,
            "desc" to description,
            "commentary" to commentary,
            "rating" to rating,
            "imageLink" to imageLink,
            "year" to year,
            "mark" to mark,
            "currentWatchTime" to currentWatchTime,
            "isASeries" to isASeries
        )
    }
    private fun tagsToHashMap() : HashMap<String, *>{
        val hashMap : HashMap<String, String> = hashMapOf()
        for(i in tags.indices){
            hashMap[i.toString()] = tags[i]
        }
        return hashMap
    }
    private fun commentsToHashMap() : HashMap<String, *>{
        val hashMap : HashMap<String, String> = hashMapOf()
        for(comment in comments){
            hashMap[comment.time.toString()] = comment.text
        }
        return hashMap
    }

    private fun addFilmToDatabase(databaseUserRef : DatabaseReference, listener: DatabaseSyncListener){
        val toAddRef = databaseUserRef.push()
        if(toAddRef.key == null){
            listener.onFailure(Exception("Null database response"))
        } else {
            databaseRef = toAddRef.key!!
            toAddRef.setValue(this.toHashMap()).addOnSuccessListener {
                listener.onSuccess()
            }.addOnFailureListener {
                listener.onFailure(Exception("Database setValue request rejected"))
            }
        }
    }
    private fun deleteFilmFromDatabase(databaseUserRef: DatabaseReference, listener: DatabaseSyncListener) {
        databaseUserRef.child(databaseRef).removeValue().addOnSuccessListener {
            listener.onSuccess()
        }.addOnFailureListener {
            listener.onFailure(Exception("Database removeValue request rejected"))
        }
    }
    private fun deleteImageFromStorage(storageUserRef: StorageReference, listener: DatabaseSyncListener){
        if(hasImage()){
            storageUserRef.child("film_images/$imageLink").delete().addOnSuccessListener {
                listener.onSuccess()
            }.addOnFailureListener {
                listener.onFailure(Exception("Storage delete request rejected"))
            }
        } else {
            listener.onSuccess()
        }
    }
    private fun downloadAndLoadImageFromStorage(imageView: ImageView, storageUserRef: StorageReference, context: Context, listener: DatabaseSyncListener){
        if(hasImage()){
            val imageRef = storageUserRef.child("film_images/$imageLink")
            val file = File(context.cacheDir, databaseRef)
            imageRef.getFile(file)
                .addOnSuccessListener {
                    val editor = context.getSharedPreferences("FilmImages", Context.MODE_PRIVATE).edit()
                    editor.putString(databaseRef, file.toURI().toString())
                    editor.apply()
                    if(loadImageLocally(imageView, context)){
                        listener.onSuccess()
                    }
                    else{
                        listener.onFailure(Exception("Device local memory error"))
                    }
                }
                .addOnFailureListener {
                    listener.onFailure(Exception("Storage getFile request rejected"))
                }
        }
        else{
            listener.onSuccess()
        }
    }
    private fun loadImageLocally(imageView: ImageView, context : Context) : Boolean {
        val sharedPreferences = context.getSharedPreferences("FilmImages", Context.MODE_PRIVATE)
        val savedImageUriString = sharedPreferences.getString(databaseRef, null)
        if (savedImageUriString != null) {
            val savedImageUri = Uri.parse(savedImageUriString)
            Picasso.get().invalidate(savedImageUri)
            Picasso.get().load(savedImageUri).into(imageView)
            val layoutParams = imageView.layoutParams
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            imageView.layoutParams = layoutParams
            imageView.visibility = VISIBLE
            return true
        }
        return false
    }
    fun saveDataChange(databaseUserRef: DatabaseReference, listener: DatabaseSyncListener){
        databaseUserRef.child(databaseRef).runTransaction(object : Transaction.Handler{
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                currentData.value = toHashMap()
                currentData.child("tags").value = tagsToHashMap()
                currentData.child("comments").value = commentsToHashMap()
                currentData.child("lastEditTime").value = System.currentTimeMillis()
                return Transaction.success(currentData)
            }
            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if (error != null) {
                    listener.onFailure(Exception(error.message))
                } else if (!committed) {
                    listener.onFailure(Exception("Transaction request rejected"))
                } else {
                    listener.onSuccess()
                }
            }
        })
    }
    fun setImageByURI(localUri: Uri, imageView: ImageView, context: Context, storageUserRef : StorageReference, databaseUserRef: DatabaseReference, listener: DatabaseSyncListener){
        Picasso.get().load(localUri).into(object : com.squareup.picasso.Target {
            override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                bitmap?.let {
                    val compressedImage = ImageLoader().compressImage(bitmap, 1024)
                    val imageRef = storageUserRef.child("film_images/${UUID.randomUUID()}.jpg")
                    imageRef.putBytes(compressedImage).addOnSuccessListener {
                        deleteImageFromStorage(storageUserRef, object : DatabaseSyncListener{
                            override fun onSuccess() {
                                databaseUserRef.child("$databaseRef/imageLink")
                                    .setValue(imageRef.name)
                                    .addOnSuccessListener {
                                        imageLink = imageRef.name
                                        downloadAndLoadImageFromStorage(imageView, storageUserRef, context, listener)
                                    }.addOnFailureListener(){ ex->
                                        listener.onFailure(ex)
                                    }
                            }
                            override fun onFailure(exception: Exception) {
                                listener.onFailure(exception)
                            }
                        })
                    }.addOnFailureListener { ex->
                        listener.onFailure(ex)
                    }
                }
            }
            override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                listener.onFailure(Exception("Failed to prepare image bitmap for uploading"))
            }
            override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}
        })
    }
    fun deleteFilm(databaseUserRef: DatabaseReference, storageUserRef: StorageReference, context: Context, listener: DatabaseSyncListener){
        deleteFilmFromDatabase(databaseUserRef, object : DatabaseSyncListener{
            override fun onSuccess() {
                deleteImageFromStorage(storageUserRef, object  : DatabaseSyncListener{
                    override fun onSuccess() {
                        val editor = context.getSharedPreferences("FilmImages", Context.MODE_PRIVATE).edit()
                        editor.remove(databaseRef)
                        editor.apply()
                        listener.onSuccess()
                    }
                    override fun onFailure(exception: java.lang.Exception) {
                        listener.onFailure(exception)
                    }
                })
            }
            override fun onFailure(exception: Exception) {
                listener.onFailure(exception)
            }
        })
    }
    fun deleteImage(imageView: ImageView, listPosition : Int, databaseUserRef: DatabaseReference, storageUserRef: StorageReference, context: Context, listener: DatabaseSyncListener){
        deleteImageFromStorage(storageUserRef, object : DatabaseSyncListener{
            override fun onSuccess() {
                databaseUserRef.child("$databaseRef/imageLink").removeValue().addOnSuccessListener {
                    this@FilmItem.imageLink = null
                    val editor = context.getSharedPreferences("FilmImages", Context.MODE_PRIVATE).edit()
                    editor.remove(databaseRef)
                    editor.apply()
                    imageView.setImageResource(R.drawable.outline_image_24)
                    imageView.visibility = GONE
                    DataSingleton.adapters[type].notifyItemChanged(listPosition)
                    listener.onSuccess()
                }.addOnFailureListener{
                    listener.onFailure(Exception("Database setValue request rejected"))
                }
            }
            override fun onFailure(exception: Exception) {
                listener.onFailure(exception)
            }
        })
    }
    fun changeType(toMove : Int, databaseUserRef: DatabaseReference, listener: DatabaseSyncListener){
        databaseUserRef.child("$databaseRef/type").setValue(toMove).addOnSuccessListener {
            databaseUserRef.child("$databaseRef/lastEditTime").setValue(System.currentTimeMillis())
            type = toMove
            listener.onSuccess()
        }.addOnFailureListener{
            listener.onFailure(Exception("Database setValue request rejected"))
        }
    }
    fun loadImageIfExists(imageView : ImageView, context: Context, storageUserRef: StorageReference, listener: DatabaseSyncListener){
        if(hasImage()){
            if(loadImageLocally(imageView, context)){
                listener.onSuccess()
            }
            else {
                downloadAndLoadImageFromStorage(imageView, storageUserRef, context, listener)
            }
        }
        else{
            listener.onSuccess()
        }
    }

    fun hasImage() : Boolean{
        return imageLink!=null
    }
    fun getTitle() : String {
        return title
    }
    fun getDescription() : String? {
        return description
    }
    fun getRating() : String? {
        return rating
    }
    fun getCommentary() : String?{
        return commentary
    }
    fun getMark() : Int?{
        return mark
    }
    fun getDatabaseRef() : String{
        return databaseRef
    }
    fun getTags() : Array<String>{
        return tags.toTypedArray()
    }
    fun getLink() : String?{
        return link
    }
    fun getComments() : Array<Comment> {
        return comments.toTypedArray()
    }
    fun getWatchTime() : Int?{
        return currentWatchTime
    }
    fun getYear() : Int?{
        return year
    }
    fun getLastEditTime() : Long {
        return lastEditTime
    }
    fun getType(): Int {
        return type
    }

    fun setTitle(title : String){
        if(title.isNotBlank()) this.title = title.trim()
    }
    fun setYear(newYear : Int?){
        if(newYear != null && newYear > 1900 && newYear < 2100) year = newYear
    }
    fun setDescription(description: String?){
        if(description.orEmpty().isBlank()) this.description = null
        else this.description = description
    }
    fun setCommentary(commentary : String?){
        if(commentary.orEmpty().isBlank()) this.commentary = null
        else this.commentary = commentary
    }
    fun setRating(rating: String?){
        if(rating.orEmpty().isBlank()) this.rating = null
        else this.rating = rating
    }
    fun setMark(newMark : Int?){
        if(newMark in (0 until 5)) {
            mark = newMark
        }
        else mark = null
    }
    fun addTag(name : String){
        if(name != "") tags.add(name.trim())
    }
    fun deleteAllTags(){
        tags.clear()
    }
    fun setLink(new : String?){
        val short = new.orEmpty().trim()
        if(short == "") this.link = null
        else this.link = short
    }
    fun setWatchTime(time : Int){
        if(time == 0) currentWatchTime = null
        else currentWatchTime = time
    }
    fun addComment(new : Comment){
        if(new.text != "") comments.add(new)
    }
    fun setReference(ref : String){
        databaseRef = ref
    }
    fun deleteAllComments() {
        comments.clear()
    }
    fun sortComments(){
        comments.sort()
    }

    fun smartSearch(target: String) : FilmItemSearched? {
        val searchedItem = FilmItemSearched(target, this)
        if(searchedItem.getMatchValue() > 0.0) return searchedItem
        else return null
    }

}
