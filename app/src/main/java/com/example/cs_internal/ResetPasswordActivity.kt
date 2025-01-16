package com.example.cs_internal

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth

class ResetPasswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        val authenticator = FirebaseAuth.getInstance()
        val email : EditText = findViewById(R.id.passwordResetEmail)
        val sendButton : Button = findViewById(R.id.sendEmailButton)
        val backButton : ImageButton = findViewById(R.id.imageBackButton)

        backButton.setOnClickListener(){
            val intentToLogin = Intent(this@ResetPasswordActivity, LoginActivity::class.java)
            startActivity(intentToLogin)
            finish()
        }
        sendButton.setOnClickListener(){
            val enteredEmail : String = email.text.toString()
            if(enteredEmail.isBlank()){
                Toast.makeText(this, "Please enter email", Toast.LENGTH_SHORT).show()
            } else {
                authenticator.sendPasswordResetEmail(enteredEmail).addOnFailureListener {
                    Toast.makeText(this, "Email sending failed.\nCheck your email and try again!", Toast.LENGTH_SHORT).show()
                }.addOnSuccessListener{
                    Toast.makeText(this, "Email sent!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}