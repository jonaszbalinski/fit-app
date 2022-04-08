package com.judi.fitappka

import android.app.ActionBar
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.marginBottom
import com.google.firebase.database.*
import com.judi.fitappka.databinding.ActivityUserTrainingsBinding
import extensions.Extensions.toast
import kotlinx.coroutines.delay

class UserTrainingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUserTrainingsBinding

    private lateinit var exerciseTemplateReference: DatabaseReference
    private lateinit var trainingsDataReference: DatabaseReference
    var exerciseTemplateSet: MutableSet<ExerciseTemplate> = mutableSetOf()
    var trainingsDataSet: MutableSet<Training> = mutableSetOf()
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
                Handler().postDelayed(Runnable {
                    updateTrainingList(snapshot)
                }, 300)
            }

            override fun onCancelled(error: DatabaseError) {
                toast(getString(R.string.error_connecting_to_db, error.toString()))
            }
        })
    }

    fun updateTrainingList(snapshot: DataSnapshot) {
        trainingsDataSet.clear()
        for (training in snapshot.children) {
            if(training.key.toString() == "nextId") {
                trainingsNextId = training.value.toString().toInt()
            }
            else {
                val trainingId = training.key.toString().toInt()
                val newTraining = Training(trainingId)
                for (musclePart in training.children) {
                    if(musclePart.key.toString() == "date") {
                        newTraining.changeDate(musclePart.value.toString())
                    }
                    val musclePartName = musclePart.key.toString()
                    for (exercise in musclePart.children) {
                        val exerciseId = exercise.key.toString().toInt()
                        val newExercise = Exercise(-1, "", "")
                        if(newExercise.createFromJSONData(exercise, exerciseId, musclePartName,
                            exerciseTemplateSet)) {
                            newTraining.addExercise(newExercise)
                        }
                    }
                }
                trainingsDataSet.add(newTraining)
            }
        }
        updateTrainingView()
    }

    private fun updateTrainingView(trainingId: Int = 1) {
        for(training in trainingsDataSet) {
            if(trainingId == training.id) {
                val formattedDate = training.date.substring(IntRange(0, 1)) + "." + training.date
                    .substring(IntRange(2, 3)) + "." + training.date.substring(4)
                binding.textViewDateOfTraining.text = formattedDate

                binding.linearLayoutExerciseList.removeAllViews()
                for(exercise in training.exerciseList) {
                    addExerciseToView(trainingId, exercise, binding.linearLayoutExerciseList)
                }
            }
        }
    }

    private fun addExerciseToView(trainingId: Int, exercise: Exercise, linearLayout: LinearLayout) {
        val ll = LinearLayout(this)
        ll.setBackgroundColor(Color.DKGRAY)
        ll.orientation = LinearLayout.VERTICAL

        val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT)
        layoutParams.setMargins(5, 5, 5, 15)
        ll.layoutParams = layoutParams

        val exerciseNameTV = TextView(this)
        exerciseNameTV.text = exercise.name + "\n"
        exerciseNameTV.gravity = Gravity.CENTER
        exerciseNameTV.textSize = 20f

        ll.addView(exerciseNameTV)

        addExerciseProperty("Partia mięśniowa:", exercise.musclePart, ll)
        if(exercise.containsSeries) {
            addExerciseProperty(getString(R.string.series), exercise.series.toString(), ll)
        }
        if(exercise.containsReps) {
            addExerciseProperty(getString(R.string.reps), exercise.reps.toString(), ll)
        }
        if(exercise.containsWeight) {
            addExerciseProperty(getString(R.string.weight), exercise.weight.toString(), ll)
        }
        if(exercise.containsDistance) {
            addExerciseProperty(getString(R.string.distance), exercise.distance.toString(), ll)
        }
        if(exercise.containsDuration) {
            addExerciseProperty(getString(R.string.duration), exercise.duration.toString(), ll)
        }
        val space = TextView(this)
        space.text = "\n"
        ll.addView(space)

        val hl = LinearLayout(this)
        hl.orientation = LinearLayout.HORIZONTAL
        hl.gravity = Gravity.CENTER

        val buttonEditExercise = Button(this)
        buttonEditExercise.text = "Edytuj ćwiczenie"
        buttonEditExercise.setOnClickListener {

        }
        hl.addView(buttonEditExercise)

        val buttonDeleteExercise = Button(this)
        buttonDeleteExercise.text = "Usuń ćwiczenie"
        buttonDeleteExercise.setOnClickListener {
            trainingsDataReference.child(trainingId.toString())
                .child(exercise.musclePart).child(exercise.id.toString()).removeValue()
        }
        hl.addView(buttonDeleteExercise)
        ll.addView(hl)

        linearLayout.addView(ll)
    }

    private fun addExerciseProperty(name: String, value: String, linearLayout: LinearLayout) {
        val hl = LinearLayout(this)
        hl.orientation = LinearLayout.HORIZONTAL
        hl.gravity = Gravity.CENTER

        val nameTV = TextView(this)
        nameTV.gravity = Gravity.CENTER
        nameTV.text = name + " "
        nameTV.textSize = 16f

        val valueTV = TextView(this)
        valueTV.gravity = Gravity.CENTER
        valueTV.text = value
        valueTV.textSize = 16f

        hl.addView(nameTV)
        hl.addView(valueTV)

        linearLayout.addView(hl)
    }
}