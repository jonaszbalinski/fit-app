package com.judi.fitappka

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.judi.fitappka.databinding.ActivityCreateAccountBinding
import extensions.Extensions.toast
import utils.FirebaseUtils.firebaseAuth
import utils.FirebaseUtils.firebaseUser

class CreateAccountActivity : AppCompatActivity() {
    private lateinit var userEmail: String
    private lateinit var userPassword: String
    private lateinit var createAccountInputsArray: Array<EditText>

    private lateinit var binding: ActivityCreateAccountBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        createAccountInputsArray = arrayOf(binding.etEmail, binding.etPassword, binding.etConfirmPassword)
        binding.btnCreateAccount.setOnClickListener {
            createAccount()
        }

        binding.btnSignIn2.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }

    }

    override fun onStart() {
        super.onStart()
        val user: FirebaseUser? = firebaseAuth.currentUser
        user?.let {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    private fun notEmpty(): Boolean = binding.etEmail.text.toString().trim().isNotEmpty() &&
            binding.etPassword.text.toString().trim().isNotEmpty() &&
            binding.etConfirmPassword.text.toString().trim().isNotEmpty()

    private fun identicalPassword(): Boolean {
        var identical = false
        if (notEmpty() &&
            binding.etPassword.text.toString().trim() ==
            binding.etConfirmPassword.text.toString().trim()) {
            identical = true
        }
        else if (!notEmpty()) {
            createAccountInputsArray.forEach { input ->
                if (input.text.toString().trim().isEmpty()) {
                    input.error = getString(R.string.field_is_required,
                        input.hint.toString().lowercase())
                }
            }
        }
        else {
            toast(getString(R.string.passwords_are_different))
        }
        return identical
    }

    private fun createAccount() {
        if (identicalPassword()) {
            userEmail = binding.etEmail.text.toString().trim()
            userPassword = binding.etPassword.text.toString().trim()

            firebaseAuth.createUserWithEmailAndPassword(userEmail, userPassword)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        //toast(getString(R.string.created_account))
                        sendEmailVerification()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        toast(getString(R.string.error_occurred))
                    }
                }
        }
    }

    private fun sendEmailVerification() {
        firebaseUser?.let {
            it.sendEmailVerification().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    toast(getString(R.string.mail_sent_to, userEmail))
                }

            }
        }
    }
}