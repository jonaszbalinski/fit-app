package com.judi.fitappka

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.database.*
import com.judi.fitappka.databinding.ActivityExerciseDatabaseBinding
import com.judi.fitappka.databinding.ActivityUserTrainingsBinding
import extensions.Extensions.toast

class UserTrainingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUserTrainingsBinding

    private lateinit var exerciseTemplateReference: DatabaseReference
    private lateinit var trainingsDataReference: DatabaseReference
    var exerciseTemplateSet: MutableSet<ExerciseTemplate> = mutableSetOf()
    var trainingsDataSet: MutableSet<Exercise> = mutableSetOf()
    var trainingsNextId = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserTrainingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        exerciseTemplateReference = FirebaseDatabase.getInstance()
            .getReference("Test/TestExercises") //should be "exercise templates" in db
        trainingsDataReference = FirebaseDatabase.getInstance()
            .getReference("Test/UserData/3") //"3" should be replaced with user id


        exerciseTemplateReference.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                exerciseTemplateSet.clear()
                for (musclePart in dataSnapshot.children) {
                    val musclePartName = musclePart.key.toString()
                    for (exercise in musclePart.children) {
                        if(exercise.key.toString() == "nextId") continue
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

        trainingsDataReference.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                updateTrainingList(snapshot)
            }

            override fun onCancelled(error: DatabaseError) {
                toast(getString(R.string.error_connecting_to_db, error.toString()))
            }
        })
    }

    fun updateTrainingList(snapshot: DataSnapshot) {
        trainingsDataSet.clear()
        for (training in snapshot.children) {
            if(training.toString() == "nextId") {
                trainingsNextId = training.value.toString().toInt()
            }
            else {
                val trainingId = training.toString().toInt()
                val newTraining = Training(trainingId)
                for (musclePart in training.children) {
                    val musclePartName = musclePart.toString()
                    for (exercise in musclePart.children) {
                        val exerciseId = exercise.key.toString().toInt()
                        val newExercise = Exercise(-1, "", "")
                        newExercise.createFromJSONData(exercise, exerciseId, musclePartName,
                            exerciseTemplateSet)
                        newTraining.addExercise(newExercise)
                    }
                }
            }
        }
        //updateExerciseView()
    }
}