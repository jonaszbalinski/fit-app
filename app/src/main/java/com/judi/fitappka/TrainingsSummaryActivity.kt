package com.judi.fitappka

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import com.judi.fitappka.databinding.ActivityTrainingsSummaryBinding
import extensions.Extensions.toast

class TrainingsSummaryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTrainingsSummaryBinding
    private lateinit var exerciseTemplateReference: DatabaseReference
    private lateinit var trainingsDataReference: DatabaseReference

    var exerciseTemplateSet: MutableSet<ExerciseTemplate> = mutableSetOf()
    var trainingsDataSet: MutableSet<Training> = mutableSetOf()
    var selectedMusclePartId = 0
    var selectedExerciseId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrainingsSummaryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val activityContext = this
        val userUID = Firebase.auth.currentUser?.uid.toString()

        exerciseTemplateReference = FirebaseDatabase.getInstance()
            .getReference("Test/TestExercises") //should be "exercise templates" in db
        trainingsDataReference = FirebaseDatabase.getInstance()
            .getReference("Test/UserData/$userUID")

        binding.buttonBackFromSummary.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        val musclePartListAdapter = ArrayAdapter<String>(this,
            android.R.layout.simple_spinner_dropdown_item)

        exerciseTemplateReference.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                musclePartListAdapter.clear()
                exerciseTemplateSet.clear()
                for (musclePart in dataSnapshot.children) {
                    val musclePartName = musclePart.key.toString()
                    musclePartListAdapter.add(decodeMusclePartName(musclePartName))
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
                    binding.spinnerChooseMusclePart.adapter = musclePartListAdapter
                    binding.spinnerChooseMusclePart.setSelection(selectedMusclePartId)
                }, 200)
            }

            override fun onCancelled(error: DatabaseError) {
                toast(getString(R.string.error_connecting_to_db, error.toString()))
            }
        })

        binding.spinnerChooseMusclePart.onItemSelectedListener =
            object: AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, id: Int, pos: Long) {
                    selectedMusclePartId = binding.spinnerChooseMusclePart.selectedItemId.toInt()
                    selectedExerciseId = 0
                    val exerciseListAdapter = ArrayAdapter<String>(activityContext,
                        android.R.layout.simple_spinner_dropdown_item)

                    for(exercise in exerciseTemplateSet) {
                        if(exercise.musclePart == encodeMusclePartName(binding
                                .spinnerChooseMusclePart.selectedItem.toString())){
                            exerciseListAdapter.add(exercise.name)
                        }
                    }
                    binding.spinnerChooseExcercise.adapter = exerciseListAdapter
                    binding.spinnerChooseExcercise.setSelection(selectedExerciseId)
                }
                override fun onNothingSelected(arg0: AdapterView<*>?) {}
        }

        binding.spinnerChooseExcercise.onItemSelectedListener =
            object: AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, id: Int, pos: Long) {
                    selectedExerciseId = binding.spinnerChooseExcercise.selectedItemId.toInt()
                    for (exerciseTemplate in exerciseTemplateSet) {
                        if (exerciseTemplate.musclePart == encodeMusclePartName(
                                binding.spinnerChooseMusclePart.selectedItem.toString())) {
                            if (exerciseTemplate.name ==
                                binding.spinnerChooseExcercise.selectedItem.toString()) {
                                val summary = calculateExerciseSummary(exerciseTemplate,
                                    trainingsDataSet)
                                addExerciseSummaryInfoToLayout(summary, binding.linearLayoutSummary)
                            }
                        }
                    }
                }
                override fun onNothingSelected(arg0: AdapterView<*>?) {}
        }
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
                                 trainingSet: Set<Training>): MutableMap<String, Float> {
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

        return summaryHashMap
    }

    fun addExerciseSummaryInfoToLayout(summary: MutableMap<String, Float>, layout: LinearLayout) {
        layout.removeAllViews()

        val layoutParams = LinearLayout
            .LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT)
        layoutParams.setMargins(5, 5, 5, 10)

        for (pair in summary) {
            val horizontalLL = LinearLayout(this)
            horizontalLL.orientation = LinearLayout.HORIZONTAL
            horizontalLL.gravity = Gravity.CENTER
            horizontalLL.layoutParams = layoutParams

            val textViewKey = TextView(this)
            textViewKey.text = decodeSummary(pair.key) + ":"
            textViewKey.textSize = resources.getDimension(R.dimen.trainingMediumFontSize)
            textViewKey.gravity = Gravity.CENTER
            textViewKey.setPadding(5, 10, 5, 15)
            textViewKey.setTextColor(resources
                .getColor(R.color.primaryLayoutText))

            val textViewValue = TextView(this)
            textViewValue.text = String.format("%.2f", pair.value)
            textViewValue.textSize = resources.getDimension(R.dimen.trainingMediumFontSize)
            textViewValue.gravity = Gravity.CENTER
            textViewValue.setPadding(5, 10, 5, 15)
            textViewValue.setTextColor(resources
                .getColor(R.color.primaryLayoutText))

            horizontalLL.addView(textViewKey)
            horizontalLL.addView(textViewValue)
            layout.addView(horizontalLL)
        }
    }
    private fun decodeMusclePartName(musclePart: String): String {
        return when (musclePart) {
            "Chest" -> getString(R.string.chest)
            "Back" -> getString(R.string.back)
            "Arms" -> getString(R.string.arms)
            "Legs" -> getString(R.string.legs)
            "Shoulders" -> getString(R.string.shoulders)
            "Abdominals" -> getString(R.string.abdominals)
            else -> musclePart
        }
    }

    private fun encodeMusclePartName(musclePart: String): String {
        return when (musclePart) {
            getString(R.string.chest) -> "Chest"
            getString(R.string.back) -> "Back"
            getString(R.string.arms) -> "Arms"
            getString(R.string.legs) -> "Legs"
            getString(R.string.shoulders) -> "Shoulders"
            getString(R.string.abdominals) -> "Abdominals"
            else -> musclePart
        }
    }

    private fun decodeSummary(summary: String): String {
        return when (summary) {
            "Total weight" -> getString(R.string.total_weight)
            "Total duration" -> getString(R.string.total_duration)
            "Total distance" -> getString(R.string.total_distance)
            "Average speed" -> getString(R.string.average_speed)
            else -> summary
        }
    }
}