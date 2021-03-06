package com.judi.fitappka

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.judi.fitappka.databinding.ActivityMainBinding
import extensions.Extensions.toast
import utils.FirebaseUtils.firebaseAuth

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSignOut.setOnClickListener {
            firebaseAuth.signOut()
            startActivity(Intent(this, SignInActivity::class.java))
            toast(getString(R.string.logged_out))
            finish()
        }



        binding.buttonCreateExerciseMain.setOnClickListener{
            startActivity(Intent(this, CreateExerciseActivity::class.java))
            finish()
        }

        binding.buttonEditExerciseMain.setOnClickListener{
            startActivity(Intent(this, EditExerciseActivity::class.java))
            finish()
        }

        binding.buttonShowTrainings.setOnClickListener{
            startActivity(Intent(this, UserTrainingsActivity::class.java))
            finish()
        }

        binding.buttonShowSummary.setOnClickListener{
            startActivity(Intent(this, TrainingsSummaryActivity::class.java))
            finish()
        }

        /*
            val user = Firebase.auth.currentUser?.uid
        Dostań UID
         */
    }
}