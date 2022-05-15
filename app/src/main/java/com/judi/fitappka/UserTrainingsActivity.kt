package com.judi.fitappka

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import com.judi.fitappka.databinding.ActivityUserTrainingsBinding
import extensions.Extensions.toast


class UserTrainingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUserTrainingsBinding

    private lateinit var exerciseTemplateReference: DatabaseReference
    private lateinit var trainingsDataReference: DatabaseReference
    var exerciseTemplateSet: MutableSet<ExerciseTemplate> = mutableSetOf()
    var trainingsDataSet: MutableSet<Training> = mutableSetOf()
    var trainingsNextId = 1
    var currentVisibleTrainingId = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserTrainingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        showAddExerciseMenu(false)
        val activityContext = this

        val userUID = Firebase.auth.currentUser?.uid.toString()

        exerciseTemplateReference = FirebaseDatabase.getInstance()
            .getReference("Test/TestExercises") //should be "exercise templates" in db
        trainingsDataReference = FirebaseDatabase.getInstance()
            .getReference("Test/UserData/$userUID")

        binding.buttonBackToMenu.setOnClickListener{
            startActivity(Intent(this,MainActivity::class.java))
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
                Handler().postDelayed(Runnable {
                    updateTrainingList(snapshot)
                }, 300)
            }

            override fun onCancelled(error: DatabaseError) {
                toast(getString(R.string.error_connecting_to_db, error.toString()))
            }
        })

        binding.buttonNextTraining.setOnClickListener {
            if(currentVisibleTrainingId >= trainingsDataSet.size) {
                currentVisibleTrainingId = 1
            }
            else {
                currentVisibleTrainingId += 1
            }
            updateTrainingView(currentVisibleTrainingId)
        }

        binding.buttonPrevTraining.setOnClickListener {
            if (currentVisibleTrainingId <= 1) {
                currentVisibleTrainingId = trainingsDataSet.size
            }
            else {
                currentVisibleTrainingId -= 1
            }
            updateTrainingView(currentVisibleTrainingId)
        }

        binding.buttonAddTraining.setOnClickListener {
            val TOCHANGEselectedDate = "25042022"
            val hashMapToUpdate = hashMapOf(
                "date" to TOCHANGEselectedDate
            )
            val secondHashMap = hashMapOf<String, Any>(
                trainingsNextId.toString() to hashMapToUpdate
            )
            trainingsDataReference.updateChildren(secondHashMap)
            currentVisibleTrainingId = trainingsNextId
            trainingsNextId += 1
            trainingsDataReference.child("nextId").setValue(trainingsNextId)

            showAddExerciseMenu(true)
            val nameListAdapter = ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item)
            for (exerciseTemplate in exerciseTemplateSet) {
                nameListAdapter.add(exerciseTemplate.name)
            }
            binding.spinnerListOfExercises.adapter = nameListAdapter

        }

        binding.spinnerListOfExercises.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View, id: Int, pos: Long) {
                    val name = parent?.getItemAtPosition(pos.toInt()) ?: return
                    for (exerciseTemplate in exerciseTemplateSet) {
                        if (name == exerciseTemplate.name) {
                            binding.linearLayoutAddExerciseInfo.removeAllViews()
                            val exerciseValuesInfo = hashMapOf<String, Any>()
                            var repsEditText: EditText? = null
                            var weightEditText: EditText? = null
                            var distanceEditText: EditText? = null
                            var durationEditText: EditText? = null


                            if(exerciseTemplate.containsReps) {
                                repsEditText =
                                    addPropertyInfoToExercise(binding.linearLayoutAddExerciseInfo,
                                        getString(R.string.reps), "", true)
                            }
                            if(exerciseTemplate.containsWeight) {
                                weightEditText =
                                    addPropertyInfoToExercise(binding.linearLayoutAddExerciseInfo,
                                        getString(R.string.weight), "", true)
                            }
                            if(exerciseTemplate.containsDistance) {
                                distanceEditText =
                                    addPropertyInfoToExercise(binding.linearLayoutAddExerciseInfo,
                                        getString(R.string.distance), "", true)
                            }
                            if(exerciseTemplate.containsDuration) {
                                durationEditText =
                                    addPropertyInfoToExercise(binding.linearLayoutAddExerciseInfo,
                                        getString(R.string.duration), "", true)
                            }

                            val buttonAdd = Button(activityContext)
                            buttonAdd.text = getString(R.string.add_exercise)
                            buttonAdd.setOnClickListener {
                                if(repsEditText != null) {
                                    val s = repsEditText.text.toString().toIntOrNull();

                                    if(s != null)
                                        exerciseValuesInfo["reps"] =
                                            repsEditText.text.toString().toInt()
                                    else
                                        exerciseValuesInfo["reps"] = -1
                                }
                                if(weightEditText != null) {
                                    val s = weightEditText.text.toString().toFloatOrNull();

                                    if(s != null)
                                        exerciseValuesInfo["weight"] =
                                            weightEditText.text.toString().toInt()
                                    else
                                        exerciseValuesInfo["weight"] = -1
                                }
                                if(distanceEditText != null) {
                                    val s = distanceEditText.text.toString().toFloatOrNull();

                                    if(s != null)
                                        exerciseValuesInfo["distance"] =
                                            distanceEditText.text.toString().toInt()
                                    else
                                        exerciseValuesInfo["distance"] = -1
                                }
                                if(durationEditText != null) {
                                    val s = durationEditText.text.toString().toFloatOrNull();

                                    if(s != null)
                                        exerciseValuesInfo["duration"] =
                                            durationEditText.text.toString().toInt()
                                    else
                                        exerciseValuesInfo["duration"] = -1
                                }

                                var nextSeriesId = 1
                                for (training in trainingsDataSet) {
                                    if (currentVisibleTrainingId == training.id) {
                                        for (musclePart in training.musclePartMap.values) {
                                            for (exercise in musclePart) {
                                                if (exercise.name == name) { // ?!!?!
                                                    nextSeriesId =
                                                        training.getNextSeriesId(exercise)
                                                }
                                            }
                                        }
                                    }
                                }

                                val seriesIdInfo = hashMapOf<String, Any>(
                                    nextSeriesId.toString() to exerciseValuesInfo
                                )

                                trainingsDataReference.child(currentVisibleTrainingId.toString())
                                    .child(exerciseTemplate.musclePart)
                                    .child(exerciseTemplate.id.toString())
                                    .updateChildren(seriesIdInfo)

                                showAddExerciseMenu(false)
                            }

                            binding.linearLayoutAddExerciseInfo.addView(buttonAdd)
                        }
                    }
                }
                override fun onNothingSelected(arg0: AdapterView<*>?) {

                }
            }
    }

    private fun showAddExerciseMenu(shouldBeVisible: Boolean) {
        if(shouldBeVisible) {
            binding.linearLayoutAddExercise.visibility = View.VISIBLE
            binding.buttonNextTraining.visibility = View.GONE
            binding.buttonPrevTraining.visibility = View.GONE
            binding.scrollViewExerciseList.visibility = View.GONE
            binding.buttonAddTraining.visibility = View.GONE
            binding.buttonBackToMenu.visibility = View.GONE
        }
        else {
            binding.linearLayoutAddExercise.visibility = View.GONE
            binding.buttonNextTraining.visibility = View.VISIBLE
            binding.buttonPrevTraining.visibility = View.VISIBLE
            binding.scrollViewExerciseList.visibility = View.VISIBLE
            binding.buttonAddTraining.visibility = View.VISIBLE
            binding.buttonBackToMenu.visibility = View.VISIBLE
        }
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
        updateTrainingView(currentVisibleTrainingId)
    }

    private fun updateTrainingView(trainingId: Int = 1) {
        for(training in trainingsDataSet) {
            if(trainingId == training.id) {
                val formattedDate = training.date.substring(IntRange(0, 1)) + "." + training.date
                    .substring(IntRange(2, 3)) + "." + training.date.substring(4)
                binding.textViewDateOfTraining.text = formattedDate
                binding.textViewDateOfTraining.textSize = resources
                    .getDimension(R.dimen.bigFontSize)
                binding.linearLayoutExerciseList.removeAllViews()

                val musclePartLayoutParams = LinearLayout
                    .LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                                  LinearLayout.LayoutParams.MATCH_PARENT)
                musclePartLayoutParams.setMargins(5, 5, 5, 15)

                val exerciseLayoutParams = LinearLayout
                    .LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                                  LinearLayout.LayoutParams.MATCH_PARENT)
                exerciseLayoutParams.setMargins(5, 5, 5, 10)

                val seriesLayoutParams = LinearLayout
                    .LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                                  LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                seriesLayoutParams.setMargins(5, 5, 5, 10)

                for(musclePart in training.musclePartMap.keys) {
                    val musclePartLL = LinearLayout(this)
                    musclePartLL.orientation = LinearLayout.VERTICAL
                    //musclePartLL.gravity = Gravity.CENTER
                    musclePartLL.setBackgroundColor(resources
                        .getColor(R.color.primaryLayoutBackground))
                    musclePartLL.layoutParams = musclePartLayoutParams
                    binding.linearLayoutExerciseList.addView(musclePartLL)

                    val musclePartTV = TextView(this)
                    musclePartTV.text = translateMusclePartName(musclePart)
                    musclePartTV.textSize = resources.getDimension(R.dimen.bigFontSize)
                    musclePartTV.gravity = Gravity.CENTER
                    musclePartTV.setPadding(5, 10, 5, 15)
                    musclePartTV.setTextColor(resources
                        .getColor(R.color.primaryLayoutText))
                    musclePartLL.addView(musclePartTV)

                    for(exercise in training.musclePartMap[musclePart]!!) {
                        val exerciseLL = LinearLayout(this)
                        exerciseLL.orientation = LinearLayout.VERTICAL
                        //exerciseLL.gravity = Gravity.CENTER
                        exerciseLL.setBackgroundColor(resources
                            .getColor(R.color.secondaryLayoutBackground))
                        exerciseLL.layoutParams = exerciseLayoutParams
                        musclePartLL.addView(exerciseLL)

                        val exerciseTV = TextView(this)
                        exerciseTV.text = exercise.name
                        exerciseTV.setTextColor(resources.getColor(R.color.secondaryLayoutText))
                        exerciseTV.textSize = resources.getDimension(R.dimen.mediumFontSize)
                        exerciseTV.gravity = Gravity.CENTER
                        exerciseTV.setPadding(5, 10, 5, 15)
                        exerciseLL.addView(exerciseTV)

                        val listOfColumns = mutableListOf(getString(R.string.series))
                        if (exercise.containsReps) listOfColumns.add(getString(R.string.reps))
                        if (exercise.containsWeight) listOfColumns.add(getString(R.string.weight))
                        if (exercise.containsDistance) listOfColumns.add(getString(R.string.distance))
                        if (exercise.containsDuration) listOfColumns.add(getString(R.string.duration))

                        val seriesLL = LinearLayout(this)
                        seriesLL.orientation = LinearLayout.VERTICAL
                        //seriesLL.gravity = Gravity.CENTER
                        seriesLL.setBackgroundColor(resources
                            .getColor(R.color.minorLayoutBackground))
                        seriesLL.layoutParams = seriesLayoutParams
                        exerciseLL.addView(seriesLL)

                        addSeriesInfo(seriesLL, listOfColumns, seriesLayoutParams)
                        var seriesIt = 1
                        for(series in exercise.listOfSeries) {
                            val listOfValues = mutableListOf(seriesIt.toString())

                            if (series.reps != null) listOfValues.add(series.reps.toString())
                            if (series.weight != null) listOfValues.add(series.weight.toString())
                            if (series.distance != null) listOfValues.add(series.distance.toString())
                            if (series.duration != null) listOfValues.add(series.duration.toString())

                            addSeriesInfo(seriesLL, listOfValues, seriesLayoutParams)
                            seriesIt += 1
                        }

                        val addSeriesButton = Button(this)
                        addSeriesButton.text = getString(R.string.add_series)
                        addSeriesButton.setOnClickListener {

                        }
                        exerciseLL.addView(addSeriesButton)
                    }
                }

                val addExerciseButton = Button(this)
                addExerciseButton.text = getString(R.string.add_exercise)
                addExerciseButton.setOnClickListener {
                    showAddExerciseMenu(true)
                    val nameListAdapter = ArrayAdapter<String>(this,
                        android.R.layout.simple_spinner_dropdown_item)
                    for (exerciseTemplate in exerciseTemplateSet) {
                        nameListAdapter.add(exerciseTemplate.name)
                    }
                    binding.spinnerListOfExercises.adapter = nameListAdapter
                }
                binding.linearLayoutExerciseList.addView(addExerciseButton)

                val deleteTrainingButton = Button(this)
                deleteTrainingButton.text = getString(R.string.delete_training)
                deleteTrainingButton.setOnClickListener {
                    trainingsDataReference.child(currentVisibleTrainingId.toString()).removeValue()
                    currentVisibleTrainingId -= 1
                    updateTrainingView(currentVisibleTrainingId)
                }
                binding.linearLayoutExerciseList.addView(deleteTrainingButton)

                return
            }
        }
    }

    private fun addSeriesInfo(parent: LinearLayout, valueList: List<String>,
                              layoutParams: LinearLayout.LayoutParams) {
        val horizontalLL = LinearLayout(this)
        horizontalLL.orientation = LinearLayout.HORIZONTAL
        horizontalLL.layoutParams = layoutParams

        for(value in valueList) {
            val propertyTextView = TextView(this)
            propertyTextView.text = value
            propertyTextView.setTextColor(resources.getColor(R.color.minorLayoutText))
            propertyTextView.textSize = resources.getDimension(R.dimen.smallFontSize)
            propertyTextView.gravity = Gravity.CENTER
            propertyTextView.layoutParams = layoutParams
            horizontalLL.addView(propertyTextView)
        }

        parent.addView(horizontalLL)
    }

    private fun translateMusclePartName(musclePart: String): String {
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

    private fun addPropertyInfoToExercise(parent: LinearLayout, propertyName: String,
                                          value: String, editable: Boolean = false): EditText? {
        val horizontalLL = LinearLayout(this)

        horizontalLL.orientation = LinearLayout.HORIZONTAL
        val propertyTextView = TextView(this)
        propertyTextView.text = ("$propertyName    ")
        horizontalLL.addView(propertyTextView)

        if (editable) {
            val valueTextView = EditText(this)
            horizontalLL.addView(valueTextView)
            parent.addView(horizontalLL)

            return valueTextView
        }
        else {
            val valueTextView = TextView(this)
            valueTextView.text = ("$value    ")
            horizontalLL.addView(valueTextView)
            parent.addView(horizontalLL)
        }
        return null
    }

}