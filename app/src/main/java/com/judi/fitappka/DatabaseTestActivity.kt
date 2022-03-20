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


        binding.buttonWriteToDatabase.setOnClickListener{

            val textToDatabase = ""
            val database = FirebaseDatabase.getInstance().getReference("testParent").child("Name")
            val name = binding.textWriteToDatabase.text.toString()
            database.setValue(name)
        }


        binding.buttonGetFromDatabase.setOnClickListener{

            database = FirebaseDatabase.getInstance().getReference("testParent")

            database.child("Name").get().addOnSuccessListener {
                val name = it.value
                binding.textDatabaseReadText.setText(name.toString())
            }
        }
    }
}