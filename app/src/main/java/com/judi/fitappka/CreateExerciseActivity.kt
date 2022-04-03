package com.judi.fitappka

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.get
import com.google.firebase.database.*
import com.judi.fitappka.databinding.ActivityCreateExerciseBinding
import extensions.Extensions.toast

class CreateExerciseActivity : AppCompatActivity(){

    private lateinit var exerciseMusclePartsReference: DatabaseReference
    private lateinit var binding: ActivityCreateExerciseBinding
    private lateinit var nextEx: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateExerciseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        exerciseMusclePartsReference = FirebaseDatabase.getInstance()
            .getReference("Test/TestExercises")

        val musclePartListAdapter = ArrayAdapter<String>(this,
            android.R.layout.simple_spinner_dropdown_item)

        exerciseMusclePartsReference.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (musclePart in dataSnapshot.children) {
                    val musclePartName = musclePart.key
                    musclePartListAdapter.add(musclePartName)
                }
                binding.spinnerMusclePartList.adapter = musclePartListAdapter
            }
            override fun onCancelled(error: DatabaseError) {
                toast(getString(R.string.error_connecting_to_db, error.toString()))
            }
        })

        binding.buttonCreateExercise.setOnClickListener{

            if(binding.editTextExerciseName.text.toString()!=""){
                val exerciseProperties = hashMapOf<String,Any>()
                if(binding.switchDistance.isChecked){
                    exerciseProperties.put("distance",true)
                }
                if(binding.switchWeight.isChecked){
                    exerciseProperties.put("weight",true)

                }
                if(binding.switchReps.isChecked){
                    exerciseProperties.put("reps",true)

                }
                if(binding.switchDutarion.isChecked){
                    exerciseProperties.put("duration",true)

                }
                if(binding.switchSeries.isChecked){
                    exerciseProperties.put("series",true)

                }
                exerciseProperties.put("name",binding.editTextExerciseName.text.toString())
                val chosenMusclePart = exerciseMusclePartsReference.child(binding.spinnerMusclePartList.selectedItem.toString())
                val ex: MutableList<String> = mutableListOf()
                chosenMusclePart.addValueEventListener(object: ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        for (Exercise in dataSnapshot.children) {
                            ex.add(Exercise.key.toString())
                        }
                        val substr = ex.last().substring(2,ex.last().length)
                        val next = substr.toInt()+1
                        nextEx = "Ex"+next.toString()


                    }
                    override fun onCancelled(error: DatabaseError) {
                        toast(getString(R.string.error_connecting_to_db, error.toString()))
                    }

                })
                Tu Nie działaaaaaaa
                val newExercise = chosenMusclePart.child(nextEx)
                newExercise.updateChildren(exerciseProperties)
            }
            else{
                toast("Uzupełniej nazwę ćwiczenia")
            }

        }





    }

}