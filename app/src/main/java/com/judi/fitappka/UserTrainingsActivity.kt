package com.judi.fitappka

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import com.judi.fitappka.databinding.ActivityUserTrainingsBinding
import extensions.Extensions.toast
import extensions.OnSwipeTouchListener
import java.util.*


class UserTrainingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUserTrainingsBinding
    private lateinit var exerciseTemplateReference: DatabaseReference
    private lateinit var trainingsDataReference: DatabaseReference
    private lateinit var onSwipeTouchListener: OnSwipeTouchListener

    var exerciseTemplateSet: MutableSet<ExerciseTemplate> = mutableSetOf()
    var trainingsDataSet: MutableSet<Training> = mutableSetOf()
    var trainingsIdsSet: SortedSet<Int> = sortedSetOf()
    var trainingsIdSetIterator = 0
    var trainingsNextId = 1

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserTrainingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        showAddExerciseMenu(false)
        val activityContext = this
        val userUID = Firebase.auth.currentUser?.uid.toString()

        onSwipeTouchListener = object : OnSwipeTouchListener(activityContext) {
            override fun onSwipeLeft() {
                if(trainingsIdSetIterator >= trainingsIdsSet.size) {
                    trainingsIdSetIterator = 0
                }
                else {
                    trainingsIdSetIterator += 1
                }
                updateTrainingView(trainingsIdsSet.elementAt(trainingsIdSetIterator))
            }
            override fun onSwipeRight() {
                if(trainingsIdSetIterator <= 0) {
                    trainingsIdSetIterator = trainingsIdsSet.size - 1
                }
                else {
                    trainingsIdSetIterator -= 1
                }
                /*for(training in trainingsDataSet) {
                    for (musclePart in training.musclePartMap.values) {
                        for (exercise in musclePart) {
                            exercise.isAddNewSeriesActive = false
                        }
                    }
                }*/
                updateTrainingView(trainingsIdsSet.elementAt(trainingsIdSetIterator))
            }
        }

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
                Handler().postDelayed({
                    updateTrainingList(snapshot)
                }, 100)
            }

            override fun onCancelled(error: DatabaseError) {
                toast(getString(R.string.error_connecting_to_db, error.toString()))
            }
        })

        binding.buttonCancelAddExercise.setOnClickListener {
            showAddExerciseMenu(false)
        }
        binding.buttonCancelAddSeries.setOnClickListener {
            showAddSeriesMenu(false)
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
                                    val s = repsEditText.text.toString().toIntOrNull()

                                    if(s != null)
                                        exerciseValuesInfo["reps"] =
                                            repsEditText.text.toString().toInt()
                                    else
                                        exerciseValuesInfo["reps"] = -1
                                }
                                if(weightEditText != null) {
                                    val s = weightEditText.text.toString().toFloatOrNull()

                                    if(s != null)
                                        exerciseValuesInfo["weight"] =
                                            weightEditText.text.toString().toInt()
                                    else
                                        exerciseValuesInfo["weight"] = -1
                                }
                                if(distanceEditText != null) {
                                    val s = distanceEditText.text.toString().toFloatOrNull()

                                    if(s != null)
                                        exerciseValuesInfo["distance"] =
                                            distanceEditText.text.toString().toInt()
                                    else
                                        exerciseValuesInfo["distance"] = -1
                                }
                                if(durationEditText != null) {
                                    val s = durationEditText.text.toString().toFloatOrNull()

                                    if(s != null)
                                        exerciseValuesInfo["duration"] =
                                            durationEditText.text.toString().toInt()
                                    else
                                        exerciseValuesInfo["duration"] = -1
                                }

                                var nextSeriesId = 1
                                for (training in trainingsDataSet) {
                                    if (trainingsIdsSet.elementAt(trainingsIdSetIterator)
                                        == training.id) {
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

                                trainingsDataReference.child(trainingsIdsSet
                                    .elementAt(trainingsIdSetIterator).toString())
                                    .child(exerciseTemplate.musclePart)
                                    .child(exerciseTemplate.id.toString())
                                    .updateChildren(seriesIdInfo)

                                showAddExerciseMenu(false)
                            }

                            binding.linearLayoutAddExerciseInfo.addView(buttonAdd)
                        }
                    }
                }
                override fun onNothingSelected(arg0: AdapterView<*>?) {}
            }
    }

    private fun showAddExerciseMenu(shouldBeVisible: Boolean) {
        if(shouldBeVisible) {
            showAddSeriesMenu(false)
            binding.constraintLayoutAddExercise.visibility = View.VISIBLE
            binding.textViewDateOfTraining.visibility = View.GONE
            binding.scrollViewExerciseList.visibility = View.GONE
            binding.buttonAddTraining.visibility = View.GONE
            binding.buttonBackToMenu.visibility = View.GONE
        }
        else {
            binding.constraintLayoutAddExercise.visibility = View.GONE
            binding.textViewDateOfTraining.visibility = View.VISIBLE
            binding.scrollViewExerciseList.visibility = View.VISIBLE
            binding.buttonAddTraining.visibility = View.VISIBLE
            binding.buttonBackToMenu.visibility = View.VISIBLE
        }
    }

    private fun showAddSeriesMenu(shouldBeVisible: Boolean) {
        if(shouldBeVisible) {
            showAddExerciseMenu(false)
            binding.constraintLayoutAddSeries.visibility = View.VISIBLE
            binding.textViewDateOfTraining.visibility = View.GONE
            binding.scrollViewExerciseList.visibility = View.GONE
            binding.buttonAddTraining.visibility = View.GONE
            binding.buttonBackToMenu.visibility = View.GONE
        }
        else {
            binding.constraintLayoutAddSeries.visibility = View.GONE
            binding.textViewDateOfTraining.visibility = View.VISIBLE
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
                trainingsIdsSet.add(trainingId)
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
        updateTrainingView(trainingsIdsSet.elementAt(trainingsIdSetIterator))
    }

    private fun updateTrainingView(trainingId: Int = 1) {
        for(training in trainingsDataSet) {
            if(trainingId == training.id) {
                val formattedDate = training.date.substring(IntRange(0, 1)) + "." + training.date
                    .substring(IntRange(2, 3)) + "." + training.date.substring(4)
                binding.textViewDateOfTraining.text = formattedDate
                binding.textViewDateOfTraining.textSize = resources
                    .getDimension(R.dimen.trainingBigFontSize)
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

                val seriesTVLayoutParams = LinearLayout
                    .LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                seriesTVLayoutParams.setMargins(5, 5, 5, 10)

                for(musclePart in training.musclePartMap.keys) {
                    val musclePartLL = LinearLayout(this)
                    musclePartLL.orientation = LinearLayout.VERTICAL
                    musclePartLL.setBackgroundColor(resources
                        .getColor(R.color.primaryLayoutBackground))
                    musclePartLL.layoutParams = musclePartLayoutParams
                    binding.linearLayoutExerciseList.addView(musclePartLL)

                    val musclePartTV = TextView(this)
                    musclePartTV.text = decodeMusclePartName(musclePart)
                    musclePartTV.textSize = resources.getDimension(R.dimen.trainingBigFontSize)
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
                        exerciseTV.textSize = resources.getDimension(R.dimen.trainingMediumFontSize)
                        exerciseTV.gravity = Gravity.CENTER
                        exerciseTV.setPadding(5, 10, 5, 15)
                        exerciseLL.addView(exerciseTV)

                        val listOfColumns = mutableListOf(getString(R.string.series))
                        if (exercise.containsReps) listOfColumns.add(getString(R.string.reps))
                        if (exercise.containsWeight) listOfColumns.add(getString(R.string.weight))
                        if (exercise.containsDistance) listOfColumns.add(getString(R.string.distance))
                        if (exercise.containsDuration) listOfColumns.add(getString(R.string.duration))
                        //listOfColumns.add("") //copy

                        val seriesLL = LinearLayout(this)
                        seriesLL.orientation = LinearLayout.VERTICAL
                        //seriesLL.gravity = Gravity.CENTER
                        seriesLL.setBackgroundColor(resources
                            .getColor(R.color.minorLayoutBackground))
                        seriesLL.layoutParams = seriesLayoutParams
                        exerciseLL.addView(seriesLL)

                        val firstRowInfo = addSeriesInfo(seriesLL, listOfColumns, seriesLayoutParams)

                        val deleteExerciseTV1 = TextView(this)
                        deleteExerciseTV1.text = "(" + getString(R.string.delete)
                        deleteExerciseTV1.setTextColor(resources.getColor(R.color.addPrimaryLayoutText))
                        deleteExerciseTV1.textSize = resources.getDimension(R.dimen.trainingSmallFontSize)
                        deleteExerciseTV1.gravity = Gravity.END
                        deleteExerciseTV1.layoutParams = seriesLayoutParams
                        deleteExerciseTV1.isClickable = true
                        deleteExerciseTV1.setOnClickListener {
                            trainingsDataReference.child(training.id.toString())
                                .child(musclePart).child(exercise.id.toString()).removeValue()
                        }
                        val deleteExerciseTV2 = TextView(this)
                        deleteExerciseTV2.text = getString(R.string.exercise_no_capitalize) + ")"
                        deleteExerciseTV2.setTextColor(resources.getColor(R.color.addPrimaryLayoutText))
                        deleteExerciseTV2.textSize = resources.getDimension(R.dimen.trainingSmallFontSize)
                        deleteExerciseTV2.gravity = Gravity.START
                        deleteExerciseTV2.layoutParams = seriesLayoutParams
                        deleteExerciseTV2.isClickable = true
                        deleteExerciseTV2.setOnClickListener {
                            trainingsDataReference.child(training.id.toString())
                                .child(musclePart).child(exercise.id.toString()).removeValue()
                        }
                        firstRowInfo.addView(deleteExerciseTV1) //delete
                        firstRowInfo.addView(deleteExerciseTV2) //delete

                        var seriesIt = 1
                        for(series in exercise.listOfSeries) {
                            val listOfValues = mutableListOf(seriesIt.toString())
                            val seriesValueInfo = hashMapOf<String, Any>()

                            if (series.reps != null)  {
                                listOfValues.add(series.reps.toString())
                                seriesValueInfo["reps"] = series.reps!!
                            }
                            if (series.weight != null) {
                                listOfValues.add(series.weight.toString())
                                seriesValueInfo["weight"] = series.weight!!
                            }
                            if (series.distance != null) {
                                listOfValues.add(series.distance.toString())
                                seriesValueInfo["distance"] = series.distance!!
                            }
                            if (series.duration != null) {
                                listOfValues.add(series.duration.toString())
                                seriesValueInfo["duration"] = series.duration!!
                            }

                            val listOfValuesLL = addSeriesInfo(seriesLL,
                                listOfValues, seriesLayoutParams)
                            val copyTV = TextView(this)
                            copyTV.text = "(" + getString(R.string.copy) + ")"
                            copyTV.setTextColor(resources.getColor(R.color.addPrimaryLayoutText))
                            copyTV.textSize = resources.getDimension(R.dimen.trainingSmallFontSize)
                            copyTV.gravity = Gravity.CENTER
                            copyTV.layoutParams = seriesTVLayoutParams
                            copyTV.isClickable = true
                            copyTV.setOnClickListener {
                                val nextSeriesId = training.getNextSeriesId(exercise)
                                val seriesIdInfo = hashMapOf<String, Any>(
                                    nextSeriesId.toString() to seriesValueInfo
                                )
                                trainingsDataReference.child(training.id.toString())
                                    .child(musclePart).child(exercise.id.toString())
                                    .updateChildren(seriesIdInfo)
                            }
                            listOfValuesLL.addView(copyTV)
                            val deleteTV = TextView(this)
                            deleteTV.text = "(" + getString(R.string.delete) + ")"
                            deleteTV.setTextColor(resources.getColor(R.color.addPrimaryLayoutText))
                            deleteTV.textSize = resources.getDimension(R.dimen.trainingSmallFontSize)
                            deleteTV.gravity = Gravity.CENTER
                            deleteTV.layoutParams = seriesTVLayoutParams
                            deleteTV.isClickable = true
                            deleteTV.setOnClickListener {
                                trainingsDataReference.child(training.id.toString())
                                    .child(musclePart).child(exercise.id.toString())
                                    .child(series.id.toString()).removeValue()
                            }
                            listOfValuesLL.addView(deleteTV)

                            seriesIt += 1
                        }
                        val addSeriesTV = TextView(this)
                        addSeriesTV.text = getString(R.string.add_series)
                        addSeriesTV.setTextColor(resources.getColor(R.color.addPrimaryLayoutText))
                        addSeriesTV.textSize = resources.getDimension(R.dimen.trainingMediumFontSize)
                        addSeriesTV.gravity = Gravity.CENTER
                        addSeriesTV.layoutParams = seriesTVLayoutParams
                        addSeriesTV.isClickable = true
                        addSeriesTV.setOnClickListener {
                            addSeriesTV.visibility = View.GONE

                            val newSeriesLL = LinearLayout(this)
                            newSeriesLL.orientation = LinearLayout.HORIZONTAL
                            newSeriesLL.layoutParams = seriesLayoutParams
                            newSeriesLL.setBackgroundColor(resources.getColor(R.color.primaryLayoutBackground))

                            val newSeriesIdTV = TextView(this)
                            newSeriesIdTV.text = seriesIt.toString()
                            newSeriesIdTV.setTextColor(resources.getColor(R.color.primaryLayoutText))
                            newSeriesIdTV.textSize = resources.getDimension(R.dimen.trainingSmallFontSize)
                            newSeriesIdTV.gravity = Gravity.CENTER
                            newSeriesIdTV.layoutParams = seriesLayoutParams
                            newSeriesLL.addView(newSeriesIdTV)

                            val editTexts = mutableSetOf<EditText>()
                            if (exercise.containsReps) {
                                val editTextReps = EditText(this)
                                editTexts.add(editTextReps)
                            }
                            if (exercise.containsWeight) {
                                val editTextWeight = EditText(this)
                                editTexts.add(editTextWeight)
                            }
                            if (exercise.containsDistance) {
                                val editTextDistance = EditText(this)
                                editTexts.add(editTextDistance)
                            }
                            if (exercise.containsDuration) {
                                val editTextDuration = EditText(this)
                                editTexts.add(editTextDuration)
                            }

                            for (editText in editTexts) {
                                editText.layoutParams = seriesLayoutParams
                                editText.setTextColor(resources.getColor(R.color.primaryLayoutText))
                                editText.textSize = resources.getDimension(R.dimen.trainingSmallFontSize)
                                editText.gravity = Gravity.CENTER
                                editText.layoutParams = seriesLayoutParams
                                editText.inputType = InputType.TYPE_NUMBER_FLAG_DECIMAL
                                newSeriesLL.addView(editText)
                            }

                            val nextSeriesId = training.getNextSeriesId(exercise)

                            val confirmAddSeriesTV1 = TextView(this)
                            confirmAddSeriesTV1.text = getString(R.string.add_series_1)
                            confirmAddSeriesTV1.setTextColor(resources.getColor(R.color.addPrimaryLayoutText))
                            confirmAddSeriesTV1.textSize = resources.getDimension(R.dimen.trainingMediumFontSize)
                            confirmAddSeriesTV1.gravity = Gravity.END
                            confirmAddSeriesTV1.layoutParams = seriesLayoutParams
                            confirmAddSeriesTV1.isClickable = true
                            confirmAddSeriesTV1.setOnClickListener {
                                val propertiesInput = hashMapOf<String, Any>()
                                var propertiesIterator = 0
                                if (exercise.containsReps) {
                                    propertiesInput["reps"] = editTexts
                                        .elementAt(propertiesIterator).text.toString().toInt()
                                    propertiesIterator += 1
                                }
                                if (exercise.containsWeight) {
                                    propertiesInput["weight"] = editTexts
                                        .elementAt(propertiesIterator).text.toString().toFloat()
                                    propertiesIterator += 1
                                }
                                if (exercise.containsDistance) {
                                    propertiesInput["distance"] = editTexts
                                        .elementAt(propertiesIterator).text.toString().toFloat()
                                    propertiesIterator += 1
                                }
                                if (exercise.containsDuration) {
                                    propertiesInput["duration"] = editTexts
                                        .elementAt(propertiesIterator).text.toString().toFloat()
                                    propertiesIterator += 1
                                }

                                val seriesIdInfo = hashMapOf<String, Any>(
                                    nextSeriesId.toString() to propertiesInput
                                )
                                trainingsDataReference.child(training.id.toString())
                                    .child(musclePart).child(exercise.id.toString())
                                    .updateChildren(seriesIdInfo)
                            }

                            val confirmAddSeriesTV2 = TextView(this)
                            confirmAddSeriesTV2.text = getString(R.string.add_series_2)
                            confirmAddSeriesTV2.setTextColor(resources.getColor(R.color.addPrimaryLayoutText))
                            confirmAddSeriesTV2.textSize = resources.getDimension(R.dimen.trainingMediumFontSize)
                            confirmAddSeriesTV2.gravity = Gravity.START
                            confirmAddSeriesTV2.layoutParams = seriesLayoutParams
                            confirmAddSeriesTV2.isClickable = true
                            confirmAddSeriesTV2.setOnClickListener {
                                val propertiesInput = hashMapOf<String, Any>()
                                var propertiesIterator = 0
                                if (exercise.containsReps) {
                                    propertiesInput["reps"] = editTexts
                                        .elementAt(propertiesIterator).text.toString().toInt()
                                    propertiesIterator += 1
                                }
                                if (exercise.containsWeight) {
                                    propertiesInput["weight"] = editTexts
                                        .elementAt(propertiesIterator).text.toString().toFloat()
                                    propertiesIterator += 1
                                }
                                if (exercise.containsDistance) {
                                    propertiesInput["distance"] = editTexts
                                        .elementAt(propertiesIterator).text.toString().toFloat()
                                    propertiesIterator += 1
                                }
                                if (exercise.containsDuration) {
                                    propertiesInput["duration"] = editTexts
                                        .elementAt(propertiesIterator).text.toString().toFloat()
                                    propertiesIterator += 1
                                }

                                val seriesIdInfo = hashMapOf<String, Any>(
                                    nextSeriesId.toString() to propertiesInput
                                )
                                trainingsDataReference.child(training.id.toString())
                                    .child(musclePart).child(exercise.id.toString())
                                    .updateChildren(seriesIdInfo)
                            }

                            newSeriesLL.addView(confirmAddSeriesTV1)
                            newSeriesLL.addView(confirmAddSeriesTV2)
                            seriesLL.addView(newSeriesLL)
                        }

                        seriesLL.addView(addSeriesTV)
                    }
                }
                val buttonsLayoutParams = LinearLayout
                    .LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                exerciseLayoutParams.setMargins(5, 5, 5, 10)

                val horizontalLL = LinearLayout(this)
                horizontalLL.orientation = LinearLayout.VERTICAL
                horizontalLL.layoutParams = buttonsLayoutParams

                val addExerciseButton = LayoutInflater.from(this)
                    .inflate(R.layout.button, null) as Button
                addExerciseButton.layoutParams = seriesLayoutParams
                addExerciseButton.text = getString(R.string.add_exercise)
                addExerciseButton.textSize = resources.getDimension(R.dimen.trainingSmallFontSize)
                addExerciseButton.setOnClickListener {
                    showAddExerciseMenu(true)
                    val nameListAdapter = ArrayAdapter<String>(this,
                        android.R.layout.simple_spinner_dropdown_item)
                    for (exerciseTemplate in exerciseTemplateSet) {
                        nameListAdapter.add(exerciseTemplate.name)
                    }
                    binding.spinnerListOfExercises.adapter = nameListAdapter
                }
                horizontalLL.addView(addExerciseButton)

                val deleteTrainingButton = LayoutInflater.from(this)
                    .inflate(R.layout.button, null) as Button
                deleteTrainingButton.layoutParams = seriesLayoutParams
                deleteTrainingButton.text = getString(R.string.delete_training)
                deleteTrainingButton.textSize = resources.getDimension(R.dimen.trainingSmallFontSize)
                deleteTrainingButton.setOnClickListener {
                    trainingsDataReference.child(trainingsIdsSet.elementAt(trainingsIdSetIterator)
                        .toString()).removeValue()
                    trainingsIdSetIterator -= 1
                    if(trainingsIdSetIterator < 0) {
                        trainingsIdSetIterator = trainingsIdsSet.size - 1
                    }
                    updateTrainingView(trainingsIdsSet.elementAt(trainingsIdSetIterator))
                }
                horizontalLL.addView(deleteTrainingButton)

                binding.linearLayoutExerciseList.addView(horizontalLL)

                applyTouchListenerToAllChildren(binding.scrollViewExerciseList, onSwipeTouchListener)
                return
            }
        }
    }

    private fun applyTouchListenerToAllChildren(parent: ViewGroup, listener: OnSwipeTouchListener) {
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            if (child is ViewGroup) {
                applyTouchListenerToAllChildren(child, listener)
                child.setOnTouchListener(listener)
            } else {
                if(child is TextView) {
                    if(!child.isClickable) {
                        child.isClickable = true
                        child.setOnTouchListener(listener)
                    }
                }
                else if(child !is Button) {
                    child?.setOnTouchListener(listener)
                }
            }
        }
    }

    private fun addSeriesInfo(parent: LinearLayout, valueList: List<String>,
                              layoutParams: LinearLayout.LayoutParams): LinearLayout {
        val horizontalLL = LinearLayout(this)
        horizontalLL.orientation = LinearLayout.HORIZONTAL
        horizontalLL.layoutParams = layoutParams

        for(value in valueList) {
            val propertyTextView = TextView(this)
            propertyTextView.text = value
            propertyTextView.setTextColor(resources.getColor(R.color.minorLayoutText))
            propertyTextView.textSize = resources.getDimension(R.dimen.trainingSmallFontSize)
            propertyTextView.gravity = Gravity.CENTER
            propertyTextView.layoutParams = layoutParams
            horizontalLL.addView(propertyTextView)
        }

        parent.addView(horizontalLL)
        return horizontalLL
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