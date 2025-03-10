package com.example.cs_internal

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth

class EmailVerificationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_email_verification)

        val verifyButton : Button = findViewById(R.id.verifyButton)
        val authenticator = FirebaseAuth.getInstance()
        val user = authenticator.currentUser

        verifyButton.setOnClickListener(){
            user!!.sendEmailVerification()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Email for verification is sent!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Email sending is failed", Toast.LENGTH_SHORT).show()
                        Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
                    }
               }

        }

        val handler = Handler(Looper.getMainLooper())
        val emailVerificationChecker = object : Runnable {
            override fun run() {
                user!!.reload()
                if (user.isEmailVerified) {
                    Toast.makeText(
                        this@EmailVerificationActivity,
                        "Email is verified successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                    val intent = Intent(this@EmailVerificationActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    handler.postDelayed(this, 1000)
                }
            }
        }
        handler.postDelayed(emailVerificationChecker, 1000)

    }
}