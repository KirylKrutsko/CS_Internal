package com.example.cs_internal

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import com.squareup.picasso.Picasso
import com.yalantis.ucrop.UCrop
import java.io.File
import java.lang.Exception

class ProfileActivity : AppCompatActivity() {

    private val GALLERY_REQUEST_CODE = 1
    private val UCROP_REQUEST_CODE = 2
    private lateinit var user : FirebaseUser
    private lateinit var storageUserRef: StorageReference
    private lateinit var databaseUserRef: DatabaseReference
    private lateinit var profileImageButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val authenticator = FirebaseAuth.getInstance()
        val curUser = authenticator.currentUser

        if(curUser == null){
            val intentToLoginActivity = Intent(this@ProfileActivity, LoginActivity::class.java)
            startActivity(intentToLoginActivity)
            finish()
        }
        if(!curUser!!.isEmailVerified) {
            val intent = Intent(this, EmailVerificationActivity::class.java)
            startActivity(intent)
            finish()
        }

        user = curUser
        databaseUserRef = Firebase.database.reference.child("users/${user.uid}")
        storageUserRef = Firebase.storage.reference.child("users/${user.uid}")

        profileImageButton  = findViewById(R.id.profileImageButton)
        val usernameText : TextView = findViewById(R.id.usernameText)
        val backButton : ImageButton = findViewById(R.id.backButton)
        val logOutButton : Button = findViewById(R.id.buttonLogOut)
        val changeUsernameButton : Button = findViewById(R.id.changeName)
        val changeImageButton : Button = findViewById(R.id.setProfileImage)
        val removeImageButton : Button = findViewById(R.id.removeProfileImage)

        usernameText.setText("Welcome,\n${user.displayName}")
        ImageLoader().loadProfileImage(user.uid, storageUserRef, profileImageButton, this)

        logOutButton.setOnClickListener {
            authenticator.signOut()
            val intentToLoginActivity = Intent(this@ProfileActivity, LoginActivity::class.java)
            startActivity(intentToLoginActivity)
            finish()
        }

        changeUsernameButton.setOnClickListener {
            setUsernameDialog()
        }

        changeImageButton.setOnClickListener {
            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE)
        }

        removeImageButton.setOnClickListener {
            profileImageButton.setImageResource(R.drawable.user_icon_svgrepo_com)
            val sharedPreferences = getSharedPreferences("profile_image", Context.MODE_PRIVATE)
            sharedPreferences.edit().clear().apply()
            storageUserRef.child("profile_image").delete()
        }

        backButton.setOnClickListener {
            finish()
        }

    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK && data != null){
            when(requestCode){
                GALLERY_REQUEST_CODE -> {
                    val selectedUri : Uri? = data.data
                    if(selectedUri !=null){
                        setUCropDialog(selectedUri)
                    }
                }
                UCROP_REQUEST_CODE -> {
                    val croppedImageUri = UCrop.getOutput(data)
                    Picasso.get().invalidate(croppedImageUri)
                    Picasso.get()
                        .load(croppedImageUri)
                        .transform(CircleTransformation())
                        .into(object : com.squareup.picasso.Target {
                            override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                                if(bitmap != null){
                                    Picasso.get().load(croppedImageUri).transform(CircleTransformation()).into(profileImageButton)
                                    val sharedPreferences = getSharedPreferences("profile_image", Context.MODE_PRIVATE)
                                    sharedPreferences.edit().putString(user.uid, croppedImageUri.toString()).apply()

                                    val compressedImage = ImageLoader().compressImage(this@ProfileActivity, bitmap, 100)
                                    val imageRef = storageUserRef.child("profile_image")
                                    imageRef.putBytes(compressedImage).addOnFailureListener { ex->
                                        Toast.makeText(this@ProfileActivity, "Failed to save image to database", Toast.LENGTH_SHORT).show()
                                        Toast.makeText(this@ProfileActivity, ex.message, Toast.LENGTH_LONG).show()
                                    }
                                }
                            }

                            override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                                Toast.makeText(this@ProfileActivity, "Failed to prepare image", Toast.LENGTH_SHORT).show()
                                Toast.makeText(this@ProfileActivity, e?.message, Toast.LENGTH_LONG).show()
                            }
                            override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}
                        })
                }
            }
        }
    }
     private fun setUsernameDialog(){
         val view = EditText(this)
         view.setHint("Enter new username")
         val dialog = AlertDialog.Builder(this)
             .setTitle("Enter new username")
             .setView(view)
             .setPositiveButton("Save"){ dialog, _ ->
                 val enteredUsername = view.text.toString()
                 if(enteredUsername.isEmpty()){
                     Toast.makeText(this@ProfileActivity, "Please enter username", Toast.LENGTH_SHORT).show()
                     setUsernameDialog()
                 }
                 else{
                     user.updateProfile(
                         userProfileChangeRequest { displayName = enteredUsername }
                     ).addOnSuccessListener{
                         Toast.makeText(this@ProfileActivity, "Saved!", Toast.LENGTH_SHORT).show()
                     }.addOnFailureListener { ex->
                         Toast.makeText(this@ProfileActivity, "Failed to update username", Toast.LENGTH_SHORT).show()
                         Toast.makeText(this@ProfileActivity, ex.message, Toast.LENGTH_LONG).show()
                     }
                 }
             }
             .setNegativeButton("Cancel"){_,_->}
             .create()
         dialog.show()
     }

    private fun setUCropDialog(uri: Uri){
        val destinationUri = Uri.fromFile(File(cacheDir, "cropped_image.jpg"))
        val options = UCrop.Options()
        options.setCircleDimmedLayer(true)
        options.withAspectRatio(1f, 1f)
        UCrop.of(uri, destinationUri)
            .withOptions(options)
            .start(this@ProfileActivity, UCROP_REQUEST_CODE)
    }

}