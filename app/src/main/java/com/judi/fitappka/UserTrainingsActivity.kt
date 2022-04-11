package com.judi.fitappka

import android.app.ActionBar
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.view.View
import android.widget.*
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
    var currentVisibleTrainingId = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserTrainingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        showAddExerciseMenu(false)
        val activityContext = this

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
                            var seriesEditText: EditText? = null
                            var repsEditText: EditText? = null
                            var weightEditText: EditText? = null
                            var distanceEditText: EditText? = null
                            var durationEditText: EditText? = null

                            if(exerciseTemplate.containsSeries) {
                                seriesEditText =
                                    addPropertyInfoToExercise(binding.linearLayoutAddExerciseInfo,
                                        getString(R.string.series), "", true)
                            }
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
                                if(seriesEditText != null) {

                                    val s = seriesEditText.text.toString().toIntOrNull();

                                    if(s != null)
                                        exerciseValuesInfo["series"] =
                                            seriesEditText.text.toString().toInt()
                                    else
                                        exerciseValuesInfo["series"] = -1
                                }
                                if(repsEditText != null) {
                                    val s = repsEditText.text.toString().toIntOrNull();

                                    if(s != null)
                                        exerciseValuesInfo["series"] =
                                            repsEditText.text.toString().toInt()
                                    else
                                        exerciseValuesInfo["series"] = -1
                                }
                                if(weightEditText != null) {
                                    val s = weightEditText.text.toString().toFloatOrNull();

                                    if(s != null)
                                        exerciseValuesInfo["series"] =
                                            weightEditText.text.toString().toInt()
                                    else
                                        exerciseValuesInfo["series"] = -1
                                }
                                if(distanceEditText != null) {
                                    val s = distanceEditText.text.toString().toFloatOrNull();

                                    if(s != null)
                                        exerciseValuesInfo["series"] =
                                            distanceEditText.text.toString().toInt()
                                    else
                                        exerciseValuesInfo["series"] = -1
                                }
                                if(durationEditText != null) {
                                    val s = durationEditText.text.toString().toFloatOrNull();

                                    if(s != null)
                                        exerciseValuesInfo["series"] =
                                            durationEditText.text.toString().toInt()
                                    else
                                        exerciseValuesInfo["series"] = -1
                                }

                                val exerciseIdInfo = hashMapOf<String, Any>(
                                    exerciseTemplate.id.toString() to exerciseValuesInfo
                                )

                                trainingsDataReference.child(currentVisibleTrainingId.toString())
                                    .child(exerciseTemplate.musclePart).updateChildren(exerciseIdInfo)

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
        }
        else {
            binding.linearLayoutAddExercise.visibility = View.GONE
            binding.buttonNextTraining.visibility = View.VISIBLE
            binding.buttonPrevTraining.visibility = View.VISIBLE
            binding.scrollViewExerciseList.visibility = View.VISIBLE
            binding.buttonAddTraining.visibility = View.VISIBLE
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
        updateTrainingView(currentVisibleTrainingId)
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
                val addExerciseButton = Button(this)
                addExerciseButton.text = "Dodaj ćwiczenie"
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
                deleteTrainingButton.text = "Usuń trening"
                deleteTrainingButton.setOnClickListener {
                    trainingsDataReference.child(currentVisibleTrainingId.toString()).removeValue()
                    currentVisibleTrainingId -= 1
                    updateTrainingView(currentVisibleTrainingId)
                }
                binding.linearLayoutExerciseList.addView(deleteTrainingButton)
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
            currentVisibleTrainingId -= 1
            updateTrainingView(currentVisibleTrainingId)
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