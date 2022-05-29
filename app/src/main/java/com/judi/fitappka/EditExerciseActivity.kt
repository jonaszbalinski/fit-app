package com.judi.fitappka

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import com.judi.fitappka.databinding.ActivityEditExerciseBinding
import extensions.Extensions.toast

class EditExerciseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditExerciseBinding
    private lateinit var exerciseTemplateReference: DatabaseReference
    private lateinit var userReference: DatabaseReference
    private lateinit var currentUserReference: DatabaseReference
    val hashMapOfNames = hashMapOf<String,Any>()
    var exerciseTemplateSet: MutableSet<ExerciseTemplate> = mutableSetOf()
    var exerciseDataSet: MutableSet<Exercise> = mutableSetOf()
    var userStoredExercises: MutableSet<Int> = mutableSetOf()

    override fun onCreate(savedInstanceState: Bundle?) {

        val userUID = Firebase.auth.currentUser?.uid.toString()

        var selectedId = 1;
        var selectedMusclePartId = 0;

        super.onCreate(savedInstanceState)
        binding = ActivityEditExerciseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonBackFromEdit.setOnClickListener{
            startActivity(Intent(this,MainActivity::class.java))
            finish()
        }

        exerciseTemplateReference = FirebaseDatabase.getInstance()
            .getReference("Test/TestExercises")


        userReference = FirebaseDatabase.getInstance()
            .getReference("Test/UserData");

        //create userData for logged user if it does not exist
        userReference.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if(!dataSnapshot.child(userUID).exists()){
                    addUser(userUID)
                }
                else{
                    currentUserReference = userReference.child(userUID)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                toast(getString(R.string.error_connecting_to_db, error.toString()))
            }
        })

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
                binding.spinnerMusclePartList2.adapter = musclePartListAdapter
                binding.spinnerMusclePartList2.setSelection(selectedMusclePartId)
            }
            override fun onCancelled(error: DatabaseError) {
                toast(getString(R.string.error_connecting_to_db, error.toString()))
            }
        })
        val mainContext = this


        binding.spinnerMusclePartList2.onItemSelectedListener=
        object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, id: Int, pos: Long) {

                selectedMusclePartId = binding.spinnerMusclePartList2.selectedItemId.toInt()

                val musclePartListAdapter2 = ArrayAdapter<String>(mainContext,
                    android.R.layout.simple_spinner_dropdown_item)

                for(exercise in exerciseTemplateSet) {
                    if(exercise.musclePart == encodeMusclePartName(binding
                            .spinnerMusclePartList2.selectedItem.toString())){
                        musclePartListAdapter2.add(exercise.name)
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
                    currentUserReference.addValueEventListener(object: ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            var noted = false
                            for(training in dataSnapshot.children){
                                if(training.child(encodeMusclePartName(binding
                                        .spinnerMusclePartList2.selectedItem.toString()))
                                        .child((binding.spinnerExcerciseNameListEdit.selectedItemId+1)
                                                .toString()).exists()){
                                    noted = true
                                    break
                                }
                            }
                            binding.editTextEditExerciseName.setText(binding
                                .spinnerExcerciseNameListEdit.selectedItem.toString())

                            if(!noted){
                                for(exercise in exerciseTemplateSet) {
                                    if(exercise.name == binding.spinnerExcerciseNameListEdit
                                            .selectedItem.toString()){
                                        binding.switchDutarion2.isChecked=exercise.containsDuration
                                        binding.switchDistance2.isChecked=exercise.containsDistance
                                        binding.switchReps2.isChecked = exercise.containsReps
                                        binding.switchWeight2.isChecked = exercise.containsWeight

                                        binding.switchDutarion2.isVisible = true
                                        binding.switchDistance2.isVisible = true
                                        binding.switchReps2.isVisible = true
                                        binding.switchWeight2.isVisible = true
                                        binding.textViewChooseProperties.text =
                                            getString(R.string.choose_properties)
                                    }
                                }
                            }
                            else{
                                binding.textViewChooseProperties.text =
                                    getString(R.string.exercise_already_reported)
                                binding.switchDutarion2.isVisible = false
                                binding.switchDistance2.isVisible = false
                                binding.switchReps2.isVisible = false
                                binding.switchWeight2.isVisible = false
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {
                            toast(getString(R.string.error_connecting_to_db, error.toString()))
                        }
                    })

                }
                override fun onNothingSelected(arg0: AdapterView<*>?) {}
            }

        binding.buttonEditExercise.setOnClickListener{
            if(binding.editTextEditExerciseName.text.toString()!=""){
                val exerciseProperties = hashMapOf<String,Any>()
                var id = 0
                for(exercise in exerciseTemplateSet) {
                    if (exercise.name ==
                        binding.spinnerExcerciseNameListEdit.selectedItem.toString()) {
                        id = exercise.id
                        selectedId = id
                        continue
                    }
                }

                exerciseProperties["name"] = binding.editTextEditExerciseName.text.toString()
                if(binding.textViewChooseProperties.text == getString(
                        R.string.exercise_already_reported)) {
                    changeName(encodeMusclePartName(binding
                        .spinnerMusclePartList2.selectedItem.toString()),
                        exerciseProperties["name"].toString(), id)
                }
                else{
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
                    saveData(encodeMusclePartName(binding.spinnerMusclePartList2.selectedItem
                        .toString()), exerciseProperties, id)
                }
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

    private fun changeName(musclePartName: String, name: String,nextExId:Int){
        exerciseTemplateReference = FirebaseDatabase.getInstance()
            .getReference("Test/TestExercises/$musclePartName")

        exerciseTemplateReference.get().addOnSuccessListener {
            exerciseTemplateReference.child(nextExId.toString()).child("name").setValue(name)
            toast("zaktualizowano ćwiczenie!")
        }.addOnFailureListener{
            Log.e("firebase", "Error getting data", it)
        }
    }

    private fun addUser(uid: String){
        userReference.child(uid).child("nextId").setValue(1)
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

    private fun checkIfNoted(exerciseId: Int,musclePartName: String): Boolean {
        var noted = false
        currentUserReference.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for(training in dataSnapshot.children){
                    if(training.child(musclePartName).child(exerciseId.toString()).exists()){
                        var noted = true
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                toast(getString(R.string.error_connecting_to_db, error.toString()))
            }
        })
        return noted
    }
}