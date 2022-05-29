package com.judi.fitappka

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import com.judi.fitappka.databinding.ActivityUserTrainingsBinding
import extensions.Extensions.toast

class TrainingsSummaryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUserTrainingsBinding
    private lateinit var exerciseTemplateReference: DatabaseReference
    private lateinit var trainingsDataReference: DatabaseReference

    var exerciseTemplateSet: MutableSet<ExerciseTemplate> = mutableSetOf()
    var trainingsDataSet: MutableSet<Training> = mutableSetOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserTrainingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val activityContext = this
        val userUID = Firebase.auth.currentUser?.uid.toString()

        exerciseTemplateReference = FirebaseDatabase.getInstance()
            .getReference("Test/TestExercises") //should be "exercise templates" in db
        trainingsDataReference = FirebaseDatabase.getInstance()
            .getReference("Test/UserData/$userUID")

        binding.buttonBackToMenu.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

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
                Handler().postDelayed({
                    updateTrainingList(snapshot)
                    for(exerciseTemplate in exerciseTemplateSet) {
                        calculateExerciseSummary(exerciseTemplate, trainingsDataSet)
                    }
                }, 200)
            }

            override fun onCancelled(error: DatabaseError) {
                toast(getString(R.string.error_connecting_to_db, error.toString()))
            }
        })
    }

    fun updateTrainingList(snapshot: DataSnapshot) {
        trainingsDataSet.clear()
        for (training in snapshot.children) {
            if(training.key.toString() == "nextId") continue
            else {
                val trainingId = training.key.toString().toInt()
                val newTraining = Training(trainingId)
                for (musclePart in training.children) {
                    val musclePartName = musclePart.key.toString()
                    if(musclePartName == "date") {
                        newTraining.changeDate(musclePart.value.toString())
                    }
                    else {
                        for (exercise in musclePart.children) {
                            val exerciseId = exercise.key.toString().toInt()
                            val newExercise = Exercise()
                            newExercise.createFromJSONData(exerciseId, musclePartName,
                                exerciseTemplateSet)
                            for(series in exercise.children) {
                                val newSeries = Series()
                                newSeries.addToExercise(series, series.key.toString().toInt(),
                                    newExercise, exerciseTemplateSet)
                            }
                            newTraining.addExercise(newExercise)
                        }
                    }
                }
                trainingsDataSet.add(newTraining)
            }
        }
    }

    fun calculateExerciseSummary(exerciseTemplate: ExerciseTemplate,
                                 trainingSet: Set<Training>): HashMap<String, Float> {
        val summaryHashMap = mutableMapOf<String, Float>()
        if(exerciseTemplate.containsWeight) summaryHashMap["Total weight"] = 0f
        if(exerciseTemplate.containsDuration) summaryHashMap["Total duration"] = 0f
        if(exerciseTemplate.containsDistance) summaryHashMap["Total distance"] = 0f

        for(training in trainingSet) {
            for (musclePart in training.musclePartMap) {
                if(musclePart.key == exerciseTemplate.musclePart) {
                    for (exerciseInTraining in musclePart.value) {
                        if(exerciseInTraining.id == exerciseTemplate.id) {
                            if(exerciseTemplate.containsWeight) {
                                for (series in exerciseInTraining.listOfSeries) {
                                    summaryHashMap["Total weight"] =
                                        summaryHashMap["Total weight"]!! + series.weight!!
                                }
                            }
                            if(exerciseTemplate.containsDuration) {
                                for (series in exerciseInTraining.listOfSeries) {
                                    summaryHashMap["Total duration"] =
                                        summaryHashMap["Total duration"]!! + series.duration!!
                                }
                            }
                            if(exerciseTemplate.containsDistance) {
                                for (series in exerciseInTraining.listOfSeries) {
                                    summaryHashMap["Total distance"] =
                                        summaryHashMap["Total distance"]!! + series.distance!!
                                }
                            }
                        }
                    }
                }
            }
        }

        if(exerciseTemplate.containsDistance && exerciseTemplate.containsDuration) {
            if(summaryHashMap["Total distance"]!! > 0f && summaryHashMap["Total duration"]!! > 0f) {
                summaryHashMap["Average speed"] =
                    (summaryHashMap["Total distance"]!! / summaryHashMap["Total duration"]!!) * 60f
            }
        }

        var msg = exerciseTemplate.name + "\n"
        for (pair in summaryHashMap) {
            msg += pair.key + ": " + pair.value + "\n"
        }
        toast(msg)

        return summaryHashMap as HashMap<String, Float>
    }
}