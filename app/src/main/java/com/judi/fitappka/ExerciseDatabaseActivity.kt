package com.judi.fitappka

import android.os.Bundle
import android.view.Gravity
import android.widget.CalendarView.OnDateChangeListener
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import com.judi.fitappka.databinding.ActivityExerciseDatabaseBinding
import extensions.Extensions.toast


class ExerciseDatabaseActivity : AppCompatActivity() {
    private lateinit var exerciseTemplateReference: DatabaseReference
    private lateinit var exerciseDataReference: DatabaseReference
    private lateinit var binding: ActivityExerciseDatabaseBinding
    var exerciseTemplateSet: MutableSet<ExerciseTemplate> = mutableSetOf()
    var exerciseDataSet: MutableSet<Exercise> = mutableSetOf()

    var selectedDate = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExerciseDatabaseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        exerciseTemplateReference = FirebaseDatabase.getInstance()
            .getReference("Test/TestExercises") //should be "exercise templates" in db
        exerciseDataReference = FirebaseDatabase.getInstance()
            .getReference("Test/UserData/1") //"1" should be replaced with user id

        exerciseTemplateReference.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                exerciseTemplateSet.clear()
                for (musclePart in dataSnapshot.children) {
                    val musclePartName = musclePart.key
                    for (exercise in musclePart.children) {
                        val newExercise = ExerciseTemplate(-1, "", "")
                        if(newExercise.createFromJSONData(exercise, musclePartName)) {
                            exerciseTemplateSet.add(newExercise)
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                toast(getString(R.string.error_connecting_to_db, error.toString()))
            }
        })

        exerciseDataReference.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                updateExerciseList(snapshot)
            }

            override fun onCancelled(error: DatabaseError) {
                toast(getString(R.string.error_connecting_to_db, error.toString()))
            }
        })

        binding.calendarView.setOnDateChangeListener(OnDateChangeListener { _, year, month, day ->
            selectedDate = ""
            selectedDate += if(day < 10) "0$day" else "$day"
            val fixedMonth = month + 1
            selectedDate += if(fixedMonth < 10) "0$fixedMonth" else "$fixedMonth"
            selectedDate += "$year"

            binding.linearLayout.removeAllViews()
            val tv = TextView(this)
            tv.text = selectedDate
            tv.textSize += 1
            tv.gravity = Gravity.CENTER
            binding.linearLayout.addView(tv)
            binding.linearLayout.addView(TextView(this))

            exerciseDataReference.get().addOnSuccessListener {
                updateExerciseList(it)
                for(exercise in exerciseDataSet) {
                    val exerciseTV = TextView(this)
                    exerciseTV.text = exercise.getValues()
                    exerciseTV.gravity = Gravity.CENTER
                    binding.linearLayout.addView(exerciseTV)
                }
            }.addOnFailureListener{
                toast("Error during receiving data")
            }

        })
    }

    private fun updateExerciseList(snapshot: DataSnapshot) {
        exerciseDataSet.clear()
        for (date in snapshot.children) {
            val dateInString = date.key
            if(dateInString == selectedDate){
                for (exercise in date.children) {
                    val newExercise = Exercise(-1, "", "")
                    if(newExercise.createFromJSONData(exercise, exerciseTemplateSet)){
                        exerciseDataSet.add(newExercise)
                    }
                }
            }
        }
    }
}