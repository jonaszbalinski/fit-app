package com.judi.fitappka

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.CalendarView.OnDateChangeListener
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.get
import androidx.core.view.isEmpty
import com.google.firebase.database.*
import com.judi.fitappka.databinding.ActivityExerciseDatabaseBinding
import extensions.Extensions.toast
import org.w3c.dom.Text


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

        val buttonEdit = Button(this)
        buttonEdit.text = getString(R.string.edit_exercise)

        horizontalLL.addView(buttonDelete)
        horizontalLL.addView(buttonEdit)
        ll.addView(horizontalLL)
    }

    private fun addPropertyInfoToExercise(parent: LinearLayout, propertyName: String, value: String) {
        val horizontalLL = LinearLayout(this)

        horizontalLL.orientation = LinearLayout.HORIZONTAL
        val propertyTextView = TextView(this)
        propertyTextView.text = propertyName

        val valueTextView = TextView(this)
        valueTextView.text = value

        horizontalLL.addView(propertyTextView)
        horizontalLL.addView(valueTextView)
        parent.addView(horizontalLL)
    }
}