package com.judi.fitappka

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.get
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

        binding.linearLayoutAddExercise.visibility = View.GONE
        val activityContext = this


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

        binding.calendarView.setOnDateChangeListener { _, year, month, day ->
            selectedDate = ""
            selectedDate += if (day < 10) "0$day" else "$day"
            val fixedMonth = month + 1
            selectedDate += if (fixedMonth < 10) "0$fixedMonth" else "$fixedMonth"
            selectedDate += "$year"

            exerciseDataReference.get().addOnSuccessListener {
                updateExerciseList(it)
            }.addOnFailureListener {
                toast("Error during receiving data")
            }
        }

        binding.buttonAddExercise.setOnClickListener {
            if(selectedDate != "") {
                binding.linearLayoutAddExercise.visibility = View.VISIBLE
                val nameListAdapter = ArrayAdapter<String>(this,
                    android.R.layout.simple_spinner_dropdown_item)
                for (exerciseTemplate in exerciseTemplateSet) {
                    nameListAdapter.add(exerciseTemplate.name)
                }
                binding.spinnerListOfExercises.adapter = nameListAdapter
            }
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
                                ///
                                /// here add values from edittext, id from exerciseTemplate and date
                                /// from selectedDate to create a new exercise in database
                                ///

                                // TODO: value validation
                                if(seriesEditText != null) {
                                    exerciseValuesInfo["series"] = seriesEditText.text.toString().toInt()
                                }
                                if(repsEditText != null) {
                                    exerciseValuesInfo["reps"] = repsEditText.text.toString().toInt()
                                }
                                if(weightEditText != null) {
                                    exerciseValuesInfo["weight"] = weightEditText.text.toString().toFloat()
                                }
                                if(distanceEditText != null) {
                                    exerciseValuesInfo["distance"] = distanceEditText.text.toString().toFloat()
                                }
                                if(durationEditText != null) {
                                    exerciseValuesInfo["duration"] = durationEditText.text.toString().toFloat()
                                }

                                val exerciseIdInfo = hashMapOf<String, Any>(
                                    exerciseTemplate.id.toString() to exerciseValuesInfo
                                )

                                exerciseDataReference.child(selectedDate).updateChildren(exerciseIdInfo)

                                binding.linearLayoutAddExercise.visibility = View.GONE
                            }

                            binding.linearLayoutAddExerciseInfo.addView(buttonAdd)
                        }
                    }
                }

                override fun onNothingSelected(arg0: AdapterView<*>?) {

                }
            }
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
        updateExerciseView()
    }

    private fun updateExerciseView() {
        binding.scrollViewButtons.removeAllViews()
        binding.scrollViewExercise.removeAllViews()
        val ll = LinearLayout(this)
        ll.orientation = LinearLayout.HORIZONTAL
        binding.scrollViewButtons.addView(ll)

        ll.gravity = Gravity.CENTER

        for (exercise in exerciseDataSet) {
            val button = Button(this)
            button.text = exercise.id.toString()
            button.setOnClickListener {
                openExerciseInfo(exercise)
            }
            ll.addView(button)
            openExerciseInfo(exercise)
        }
    }

    private fun openExerciseInfo(exercise: Exercise) {
        binding.scrollViewExercise.removeAllViews()
        val ll = LinearLayout(this)
        ll.orientation = LinearLayout.VERTICAL
        binding.scrollViewExercise.addView(ll)
        ll.gravity = Gravity.CENTER

        // title
        val title = TextView(this)
        title.text = exercise.name
        ll.addView(title)

        // data
        if(exercise.series != null) {
            addPropertyInfoToExercise(ll, getString(R.string.series),
                exercise.series.toString())
        }
        if(exercise.reps != null) {
            addPropertyInfoToExercise(ll, getString(R.string.reps),
                exercise.reps.toString())
        }
        if(exercise.weight != null) {
            addPropertyInfoToExercise(ll, getString(R.string.weight),
                exercise.weight.toString())
        }
        if(exercise.distance != null) {
            addPropertyInfoToExercise(ll, getString(R.string.distance),
                exercise.distance.toString())
        }
        if(exercise.duration != null) {
            addPropertyInfoToExercise(ll, getString(R.string.duration),
                exercise.duration.toString())
        }

        // buttons
        val horizontalLL = LinearLayout(this)
        horizontalLL.orientation = LinearLayout.HORIZONTAL

        val buttonDelete = Button(this)
        buttonDelete.text = getString(R.string.delete_exercise)
        buttonDelete.setOnClickListener {
            exerciseDataReference.child(selectedDate).child(exercise.id.toString()).removeValue()
        }

        val buttonEdit = Button(this)
        buttonEdit.text = getString(R.string.edit_exercise)

        horizontalLL.addView(buttonDelete)
        horizontalLL.addView(buttonEdit)
        ll.addView(horizontalLL)
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