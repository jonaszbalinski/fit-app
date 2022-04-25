package com.judi.fitappka

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.google.firebase.database.*
import com.judi.fitappka.databinding.ActivityEditExerciseBinding
import extensions.Extensions.toast

class EditExerciseActivity : AppCompatActivity() {

    //private lateinit var exerciseMusclePartsReference: DatabaseReference
    private lateinit var binding: ActivityEditExerciseBinding
    private lateinit var exerciseTemplateReference: DatabaseReference
    val hashMapOfNames = hashMapOf<String,Any>()
    var exerciseTemplateSet: MutableSet<ExerciseTemplate> = mutableSetOf()
    var exerciseDataSet: MutableSet<Exercise> = mutableSetOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditExerciseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        exerciseTemplateReference = FirebaseDatabase.getInstance()
            .getReference("Test/TestExercises")


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

        val mainContext = this

        val musclePartListAdapter = ArrayAdapter<String>(this,
            android.R.layout.simple_spinner_dropdown_item)

        exerciseTemplateReference.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (musclePart in dataSnapshot.children) {
                    val musclePartName = musclePart.key
                    musclePartListAdapter.add(musclePartName)
                }
                binding.spinnerMusclePartList2.adapter = musclePartListAdapter
            }
            override fun onCancelled(error: DatabaseError) {
                toast(getString(R.string.error_connecting_to_db, error.toString()))
            }
        })
        binding.spinnerMusclePartList2.onItemSelectedListener=
        object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View, id: Int, pos: Long) {

                val musclePartListAdapter2 = ArrayAdapter<String>(mainContext,
                    android.R.layout.simple_spinner_dropdown_item)

                for(exercise in exerciseTemplateSet) {
                    if(exercise.musclePart==binding.spinnerMusclePartList2.selectedItem.toString()){
                    musclePartListAdapter2.add(exercise.name.toString())
                    }
                }
                binding.spinnerExcerciseNameListEdit.adapter = musclePartListAdapter2
            }
            override fun onNothingSelected(arg0: AdapterView<*>?) {
            }
        }

        binding.spinnerExcerciseNameListEdit.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener{
                override fun onItemSelected(parent: AdapterView<*>?, view: View, id: Int, pos: Long) {

                    binding.editTextEditExerciseName.setText(binding.spinnerExcerciseNameListEdit.selectedItem.toString())

                    for(exercise in exerciseTemplateSet) {

                        if(exercise.name == binding.spinnerExcerciseNameListEdit.selectedItem.toString()){
                            if(exercise.containsDuration)
                                binding.switchDutarion2.isChecked=true

                            if(exercise.containsDistance)
                                binding.switchDistance2.isChecked=true


                            if(exercise.containsReps)
                                binding.switchReps2.isChecked=true

                            if(exercise.containsWeight)
                                binding.switchWeight2.isChecked=true
                        }

                    }

                    binding.buttonEditExercise.setOnClickListener{



                        if(binding.editTextEditExerciseName.text.toString()!=""){
                            val exerciseProperties = hashMapOf<String,Any>()
                            var id=0
                            for(exercise in exerciseTemplateSet) {

                                if (exercise.name == binding.spinnerExcerciseNameListEdit.selectedItem.toString()) {
                                    id = exercise.id
                                }
                            }

                            if(binding.switchDistance2.isChecked){
                                exerciseProperties.put("distance",true)
                            }
                            if(binding.switchWeight2.isChecked){
                                exerciseProperties.put("weight",true)

                            }
                            if(binding.switchReps2.isChecked){
                                exerciseProperties.put("reps",true)

                            }
                            if(binding.switchDutarion2.isChecked){
                                exerciseProperties.put("duration",true)

                            }
                            if(binding.switchSeries2.isChecked){
                                exerciseProperties.put("series",true)

                            }
                            exerciseProperties.put("name",binding.editTextEditExerciseName.text.toString())

                            saveData(binding.spinnerMusclePartList2.selectedItem.toString(),exerciseProperties,id)

                        }
                        else{
                            toast("Uzupełniej nazwę ćwiczenia")
                        }

                    }





                }
                override fun onNothingSelected(arg0: AdapterView<*>?) {
                }
            }


    }

    private fun saveData(musclePartName: String, hashMap: HashMap<String,Any>,nextExId:Int){

        exerciseTemplateReference = FirebaseDatabase.getInstance()
            .getReference("Test/TestExercises/$musclePartName")



        exerciseTemplateReference.get().addOnSuccessListener {
            exerciseTemplateReference.child(nextExId.toString()).removeValue()
            exerciseTemplateReference.child(nextExId.toString()).updateChildren(hashMap)

            exerciseTemplateReference.child("nextId").setValue(nextExId)
        }.addOnFailureListener{
            Log.e("firebase", "Error getting data", it)
        }
    }
}