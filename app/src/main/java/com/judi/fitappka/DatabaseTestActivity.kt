package com.judi.fitappka

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.judi.fitappka.databinding.ActivityCreateAccountBinding
import com.judi.fitappka.databinding.ActivityDatabaseTestBinding
import extensions.Extensions.toast


class DatabaseTestActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDatabaseTestBinding
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDatabaseTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var Reference =  FirebaseDatabase.getInstance().getReference("Exercises")

        Reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                val nazwaCwiczenia  = dataSnapshot.child("MusclePart1").child("Exercise1").child("Name")
                binding.textExerciseName.setText(nazwaCwiczenia.getValue().toString())
            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException())
            }
        })


        binding.buttonWriteToDatabase.setOnClickListener{

            val database = FirebaseDatabase.getInstance().getReference("Exercises").child("MusclePart1").child("Exercise1").child("Name")
            val name = binding.textWriteToDatabase.text.toString()
            database.setValue(name)
        }
    }
}