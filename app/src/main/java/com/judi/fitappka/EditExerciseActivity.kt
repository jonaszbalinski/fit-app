package com.judi.fitappka

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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

        var selectedId = 1;
        var selectedmusclepartId = 0;

        super.onCreate(savedInstanceState)
        binding = ActivityEditExerciseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonBackEdit.setOnClickListener{
            startActivity(Intent(this,MainActivity::class.java))
            finish()
        }

        exerciseTemplateReference = FirebaseDatabase.getInstance()
            .getReference("Test/TestExercises")

        val musclePartListAdapter = ArrayAdapter<String>(this,
            android.R.layout.simple_spinner_dropdown_item)

        exerciseTemplateReference.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                musclePartListAdapter.clear()
                exerciseTemplateSet.clear()
                for (musclePart in dataSnapshot.children) {
                    val musclePartName = musclePart.key.toString()
                    musclePartListAdapter.add(musclePartName)

                    for (exercise in musclePart.children) {
                        if(exercise.key.toString() == "nextId") continue
                        val newExercise = ExerciseTemplate(-1, "", "")
                        if(newExercise.createFromJSONData(exercise, musclePartName.toString())) {
                            exerciseTemplateSet.add(newExercise)
                        }
                    }
                }
                binding.spinnerMusclePartList2.adapter = musclePartListAdapter
                binding.spinnerMusclePartList2.setSelection(selectedmusclepartId)
            }
            override fun onCancelled(error: DatabaseError) {
                toast(getString(R.string.error_connecting_to_db, error.toString()))
            }
        })
        val mainContext = this

        binding.spinnerMusclePartList2.onItemSelectedListener=
        object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, id: Int, pos: Long) {

                selectedmusclepartId = binding.spinnerMusclePartList2.selectedItemId.toInt()

                val musclePartListAdapter2 = ArrayAdapter<String>(mainContext,
                    android.R.layout.simple_spinner_dropdown_item)

                for(exercise in exerciseTemplateSet) {
                    if(exercise.musclePart==binding.spinnerMusclePartList2.selectedItem.toString()){
                    musclePartListAdapter2.add(exercise.name.toString())
                    }
                }
                binding.spinnerExcerciseNameListEdit.adapter = musclePartListAdapter2
                binding.spinnerExcerciseNameListEdit.setSelection(selectedId-1)
            }
            override fun onNothingSelected(arg0: AdapterView<*>?) {
            }
        }

        binding.spinnerExcerciseNameListEdit.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener{

                override fun onItemSelected(parent: AdapterView<*>?, view: View?, id: Int, pos: Long) {

                    binding.editTextEditExerciseName.setText(binding.spinnerExcerciseNameListEdit.selectedItem.toString())

                    for(exercise in exerciseTemplateSet) {

                        if(exercise.name == binding.spinnerExcerciseNameListEdit.selectedItem.toString()){
                            binding.switchDutarion2.isChecked=exercise.containsDuration

                            binding.switchDistance2.isChecked=exercise.containsDistance

                            binding.switchReps2.isChecked = exercise.containsReps

                            binding.switchWeight2.isChecked = exercise.containsWeight
                        }
                    }


                }
                override fun onNothingSelected(arg0: AdapterView<*>?) {
                }
            }

        binding.buttonEditExercise.setOnClickListener{
            if(binding.editTextEditExerciseName.text.toString()!=""){
                val exerciseProperties = hashMapOf<String,Any>()
                var id: Int =0
                for(exercise in exerciseTemplateSet) {

                    if (exercise.name == binding.spinnerExcerciseNameListEdit.selectedItem.toString()) {
                        id = exercise.id
                        selectedId = id
                        continue
                    }
                }

                if(binding.switchDistance2.isChecked){
                    exerciseProperties["distance"] = true
                }
                if(binding.switchWeight2.isChecked){
                    exerciseProperties["weight"] = true

                }
                if(binding.switchReps2.isChecked){
                    exerciseProperties["reps"] = true

                }
                if(binding.switchDutarion2.isChecked){
                    exerciseProperties["duration"] = true

                }

                exerciseProperties["name"] = binding.editTextEditExerciseName.text.toString()

                saveData(binding.spinnerMusclePartList2.selectedItem.toString(),exerciseProperties,id)

            }
            else{
                toast("Uzupełniej nazwę ćwiczenia")
            }

        }


    }

    private fun saveData(musclePartName: String, hashMap: HashMap<String,Any>,nextExId:Int){

        exerciseTemplateReference = FirebaseDatabase.getInstance()
            .getReference("Test/TestExercises/$musclePartName")


        exerciseTemplateReference.get().addOnSuccessListener {

            exerciseTemplateReference.child(nextExId.toString()).removeValue()
            exerciseTemplateReference.child(nextExId.toString()).updateChildren(hashMap)

            toast("zaktualizowano ćwiczenie!")

        }.addOnFailureListener{
            Log.e("firebase", "Error getting data", it)
        }
    }
}