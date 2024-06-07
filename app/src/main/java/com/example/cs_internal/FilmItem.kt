package com.example.cs_internal

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.health.connect.datatypes.units.Length
import android.net.Uri
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import com.google.firebase.database.*
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.Serializable
import java.util.*
import kotlin.collections.HashMap

class FilmItem(
    titleString : String,
    private var type: Int,
    private var databaseRef : String
) : Serializable {
    private var title = SpannableString(titleString)
    private var description : SpannableString? = null
    private var rating: String? = null
    private var imageLink : String? = null
    private var mark : Int? = null
    private var commentary : SpannableString? = null
    private var tags : MutableList<String> = mutableListOf()
    private var link : String? = null
//    private var comments : MutableList<String> = mutableListOf()

    constructor(title: String, type: Int, databaseUserRef: DatabaseReference, listener: DatabaseSyncListener) : this(title, type, "") {
        addFilmToDatabase(databaseUserRef, listener)
    }
    constructor(title: String, type: Int, databaseRef : String, description : String?, rating : String?, commentary: String?, imageLink : String?, link : String?) : this(title, type, databaseRef){
        if(description != null){
            this.description = SpannableString(description)
        }
        if(commentary != null){
            this.commentary = SpannableString(commentary)
        }
        this.rating = rating
        this.imageLink = imageLink
        this.link = link
    }

    private fun toHashMap() : HashMap<String, *>{
        return hashMapOf(
            "title" to title.toString(),
            "type" to type,
            "desc" to description?.toString(),
            "commentary" to commentary?.toString(),
            "rating" to rating,
            "imageLink" to imageLink
        )
    }
    private fun tagsToHashMap() : HashMap<String, *>{
        val hashMap : HashMap<String, String> = hashMapOf()
        for(i in tags.indices){
            hashMap[i.toString()] = tags[i]
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

    private fun deleteFilmFromDatabase(databaseUserRef: DatabaseReference, listener: DatabaseSyncListener) {
        databaseUserRef.child(databaseRef).removeValue().addOnSuccessListener {
            listener.onSuccess()
        }.addOnFailureListener {
            listener.onFailure(Exception("Database removeValue request rejected"))
        }
    }

    fun setImageByURI(localUri: Uri, imageView: ImageView, context: Context, storageUserRef : StorageReference, databaseUserRef: DatabaseReference, listener: DatabaseSyncListener){
        Picasso.get().load(localUri).into(object : com.squareup.picasso.Target {
            override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                bitmap?.let {
                    val compressedImage = compressImage(context, bitmap)
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

    private fun compressImage(context: Context, bitmap: Bitmap) : ByteArray {
        val toReturn = ByteArrayOutputStream()
        val k = maxOf(bitmap.width.toFloat(), bitmap.height.toFloat()) / 1024
        if(k>1){
            Toast.makeText(context, "Your image will be compressed for saving into database", Toast.LENGTH_SHORT).show()
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap,(bitmap.width / k).toInt(), (bitmap.height / k).toInt(), true)
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 100, toReturn)
        }
        else{
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, toReturn)
        }
        return toReturn.toByteArray()
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

    private fun loadImageLocally(imageView: ImageView, context : Context) : Boolean {
        val sharedPreferences = context.getSharedPreferences("FilmImages", Context.MODE_PRIVATE)
        val savedImageUriString = sharedPreferences.getString(databaseRef, null)
        if (savedImageUriString != null) {
            val savedImageUri = Uri.parse(savedImageUriString)
//            Toast.makeText(context, savedImageUri.toString(), Toast.LENGTH_SHORT).show()
//            imageView.setImageURI(savedImageUri)
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
                currentData.child("title").value = title.toString()
                currentData.child("desc").value = description?.toString()
                currentData.child("rating").value = rating
                currentData.child("commentary").value = commentary?.toString()
                currentData.child("tags").value = tagsToHashMap()
                currentData.child("link").value = link
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

    private fun addTagsToDatabase(databaseUserRef: DatabaseReference, listener: DatabaseSyncListener){
        databaseUserRef.child("$databaseRef/tags").setValue(tagsToHashMap()).addOnSuccessListener {
            listener.onSuccess()
        }.addOnFailureListener(){ ex->
            listener.onFailure(ex)
        }
    }

    private fun search(target : String?, toSearch : SpannableString?) : Int {
        if(toSearch != null){
            val index = toSearch.toString().trim().lowercase().indexOf(target.orEmpty().trim().lowercase())
            return index
        }
        return -1
    }

    fun searchAndSpan(query : String?) : FilmItem? {
        var found  = false
        val titleIndex = search(query, title)
        val descIndex = search(query, description)
        val commIndex = search(query, commentary)
        if(titleIndex != -1 || descIndex != -1 || commIndex != -1){
            return copyAndSpan(titleIndex, descIndex, commIndex, query.orEmpty().length)
        }
        return null
    }

    fun hasImage() : Boolean{
        return imageLink!=null
    }
    fun getTitle() : SpannableString {
        return title
    }
    fun getDescription() : SpannableString? {
        return description
    }
    fun getRating() : String? {
        return rating
    }
    fun getCommentary() : SpannableString?{
        return commentary
    }
    fun getMark() : Int?{
        return mark
    }
    fun getDatabaseRef() : String{
        return databaseRef
    }
    fun getTags() : MutableList<String>{
        return tags
    }
    fun getLink() : String?{
        return link
    }
    fun setTitle(title : String){
        this.title = SpannableString(title.trim())
    }
    fun setDescription(description: String?){
        val short = description.orEmpty().trim()
        if(short == "") this.description = null
        else this.description = SpannableString(short)
    }
    fun setRating(rating: String?){
        val short = rating.orEmpty().trim()
        if(short == "") this.rating = null
        else this.rating = short
    }
    fun setCommentary(commentary : String?){
        val short = commentary.orEmpty().trim()
        if(short == "") this.commentary = null
        else this.commentary = SpannableString(short)
    }
    fun setMark(mark : Int?){
        this.mark = mark
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

    private constructor(
        title: String, type: Int,
        databaseRef : String,
        description : String?,
        rating: String?,
        imageLink : String?,
        mark : Int?,
        commentary : String?,
        tags : MutableList<String>,
        link : String?
    ) : this(title, type, databaseRef, description, rating, commentary, imageLink, link)
    {
        this.mark = mark
        this.tags = tags
    }
    private fun copyAndSpan(titleIndex : Int, descIndex : Int, commIndex : Int, length: Int) : FilmItem {
        val newItem = FilmItem(title.toString(), type, databaseRef, description?.toString(), rating, imageLink, mark, commentary?.toString(), tags, link)
        if(titleIndex!=-1) newItem.title.setSpan(BackgroundColorSpan(Color.YELLOW), titleIndex, titleIndex + length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        if(descIndex!=-1) newItem.description?.setSpan(BackgroundColorSpan(Color.YELLOW), descIndex, descIndex + length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        if(commIndex!=-1) newItem.commentary?.setSpan(BackgroundColorSpan(Color.YELLOW), commIndex, commIndex + length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        return newItem
    }
}