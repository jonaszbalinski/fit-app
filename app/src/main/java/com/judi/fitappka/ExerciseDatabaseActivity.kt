package com.judi.fitappka

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.google.firebase.database.*
import com.judi.fitappka.databinding.ActivityExerciseDatabaseBinding
import extensions.Extensions.toast

class ExerciseDatabaseActivity : AppCompatActivity() {
    private lateinit var testExerciseReference: DatabaseReference
    private lateinit var binding: ActivityExerciseDatabaseBinding
    var exerciseSet: MutableSet<Exercise> = mutableSetOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExerciseDatabaseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        testExerciseReference = FirebaseDatabase.getInstance().getReference("Test/TestExercises")
        testExerciseReference.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                exerciseSet.clear()
                for (musclePart in dataSnapshot.children) {
                    val musclePartName = musclePart.key
                    for (exercise in musclePart.children) {
                        val newExercise = Exercise(-1, "", "")
                        if(newExercise.createFromJSONData(exercise, musclePartName)) {
                            exerciseSet.add(newExercise)
                        }
                    }
                }
                updateExerciseList()
            }

            override fun onCancelled(error: DatabaseError) {
                toast(getString(R.string.error_connecting_to_db, error.toString()))
            }
        })

        binding.buttonShowExerciseList.setOnClickListener {
            binding.scrollViewExercises.isVisible = !binding.scrollViewExercises.isVisible
        }
    }

    fun updateExerciseList() {
        binding.linearLayoutExercises.removeAllViews()
        exerciseSet.forEach { exercise ->
            val ll = LinearLayout(this)
            ll.orientation = LinearLayout.HORIZONTAL
            ll.setPadding(5, 5, 5, 5)
            val tv = TextView(this)
            tv.text = exercise.name
            ll.addView(tv)

            binding.linearLayoutExercises.addView(ll)
        }
    }
}