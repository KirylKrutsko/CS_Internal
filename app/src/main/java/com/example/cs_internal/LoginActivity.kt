package com.example.cs_internal

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.method.PasswordTransformationMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.*
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val authenticator = FirebaseAuth.getInstance()

        val emailText : EditText = findViewById(R.id.editTextTextEmailAddress2)
        val passwordText : EditText = findViewById(R.id.editPasswordText)
        val loginButton : Button = findViewById(R.id.loginButton)
        val signupLink : TextView = findViewById(R.id.textViewSignUp)
        val seePasswordButton : ImageButton = findViewById(R.id.seePasswordButton)
        val resetPassword : TextView = findViewById(R.id.textViewPasswordReset)

        val spannableForSignUp = SpannableString.valueOf(signupLink.text)
        val clickableSpan1 = object : ClickableSpan(){
            override fun onClick(widget: View) {
                val intentToSignupActivity = Intent(this@LoginActivity, SignupActivity::class.java)
                startActivity(intentToSignupActivity)
            }
        }
        spannableForSignUp.setSpan(clickableSpan1,spannableForSignUp.length-12,spannableForSignUp.length, 0)
        signupLink.text = spannableForSignUp
        signupLink.movementMethod = LinkMovementMethod()

        val spannableForPasswordReset = SpannableString.valueOf(resetPassword.text)
        val clickableSpan2 = object : ClickableSpan(){
            override fun onClick(widget: View) {
                val intentToResetActivity = Intent(this@LoginActivity, ResetPasswordActivity::class.java)
                startActivity(intentToResetActivity)
            }
        }
        spannableForPasswordReset.setSpan(clickableSpan2,22,spannableForPasswordReset.length, 0)
        resetPassword.text = spannableForPasswordReset
        resetPassword.movementMethod = LinkMovementMethod()

        seePasswordButton.setOnClickListener {
            if (passwordText.transformationMethod is PasswordTransformationMethod) {
                passwordText.transformationMethod = null
            } else {
                passwordText.transformationMethod = PasswordTransformationMethod.getInstance()
            }
            passwordText.setSelection(passwordText.text.length)
        }

        loginButton.setOnClickListener {
            val enteredEmail = emailText.text.toString()
            val enteredPassword = passwordText.text.toString()

            if(enteredEmail.isBlank() || enteredPassword.isBlank()){
                Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show()
            }
            else{  // logic check
                authenticator.signInWithEmailAndPassword(enteredEmail, enteredPassword)
                    .addOnCompleteListener(this){ task ->
                        if(task.isSuccessful){
                            Toast.makeText(this, "Logged in successfully", Toast.LENGTH_SHORT).show()
                            val intentToMainActivity = Intent(this@LoginActivity, MainActivity::class.java)
                            startActivity(intentToMainActivity)
                            finish()
                        }
                        else{
                            Toast.makeText(this,
                                "Login failed\nCheck entered data and try again",
                                Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }
    }
}