package com.example.cs_internal

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.util.Patterns
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class SignupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val authenticator = FirebaseAuth.getInstance()

        val nameText : EditText = findViewById(R.id.editNameText)
        val passwordText : EditText = findViewById(R.id.editPasswordText)
        val signupButton : Button = findViewById(R.id.signupButton)
        val backButton : ImageView = findViewById(R.id.backButton)
        val seePasswordButton : ImageButton = findViewById(R.id.seePasswordButton)
        val emailText : EditText = findViewById(R.id.editTextTextEmailAddress)

        seePasswordButton.setOnClickListener {
            if (passwordText.transformationMethod is PasswordTransformationMethod) {
                passwordText.transformationMethod = null
                seePasswordButton.setImageResource(R.drawable.eye_off_svgrepo_com)
            } else {
                passwordText.transformationMethod = PasswordTransformationMethod.getInstance()
                seePasswordButton.setImageResource(R.drawable.eye_svgrepo_com)
            }
            passwordText.setSelection(passwordText.text.length)
        }

        backButton.setOnClickListener {
            val intentToLoginActivity = Intent(this@SignupActivity, LoginActivity::class.java)
            startActivity(intentToLoginActivity)
        }

        signupButton.setOnClickListener {
            val enteredName = nameText.text.toString().trimEnd()
            val enteredPassword = passwordText.text.toString()
            val enteredEmail = emailText.text.toString()

            if(enteredName.isBlank() || enteredPassword.isBlank() || enteredEmail.isBlank()){
                Toast.makeText(this, "Please enter all name, email and password", Toast.LENGTH_SHORT).show()
            }
            // email validation
            else if(!Patterns.EMAIL_ADDRESS.matcher(enteredEmail).matches()){
                Toast.makeText(this, "The entered email has a wrong format!", Toast.LENGTH_SHORT).show()
            }
            // password validation
            else if(enteredPassword.length < 6){
                Toast.makeText(this, "Password must be at least 8 characters long!", Toast.LENGTH_SHORT).show()
            }
            else if(!enteredPassword.any { it.isDigit() }){
                Toast.makeText(this, "Password must contain a digit!", Toast.LENGTH_SHORT).show()
            }
            else if(!enteredPassword.any { it.isUpperCase() }){
                Toast.makeText(this, "Password must contain an uppercase!", Toast.LENGTH_SHORT).show()
            }
            else if(!enteredPassword.any { it.isLowerCase() }){
                Toast.makeText(this, "Password must contain an lowercase!", Toast.LENGTH_SHORT).show()
            }
            else {
                // user creation
                authenticator.createUserWithEmailAndPassword(enteredEmail, enteredPassword)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            val user = authenticator.currentUser
                            val profileUpdates = userProfileChangeRequest {
                                displayName = enteredName
                            }
                            user!!.updateProfile(profileUpdates)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        Toast.makeText(this, "User is created successfully!", Toast.LENGTH_SHORT).show()
                                        val intentToMainActivity = Intent(this@SignupActivity, MainActivity::class.java)
                                        startActivity(intentToMainActivity)
                                        finish()
                                    }
                                    else{
                                        Toast.makeText(this, "User profile update failed", Toast.LENGTH_SHORT).show()
                                        Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
                                    }
                                }
                        } else {
                            Toast.makeText(this, "User creation failed", Toast.LENGTH_SHORT).show()
                            Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
                        }
                    }
            }
        }
    }
}