package com.judi.fitappka

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import com.judi.fitappka.databinding.ActivityCreateExerciseBinding
import extensions.Extensions.toast

class CreateExerciseActivity : AppCompatActivity(){

    private lateinit var exerciseMusclePartsReference: DatabaseReference
    private lateinit var binding: ActivityCreateExerciseBinding
    val hashMapOfNames = hashMapOf<String,Any>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateExerciseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonBackCreate.setOnClickListener{
            startActivity(Intent(this,MainActivity::class.java))
            finish()
        }


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
                exerciseProperties.put("name",binding.editTextExerciseName.text.toString())

               saveData(binding.spinnerMusclePartList.selectedItem.toString(),exerciseProperties)

            }
            else{
                toast("Uzupełnij nazwę ćwiczenia")
            }

        }





    }

    private fun saveData(musclePartName: String, hashMap: HashMap<String,Any>){
        exerciseMusclePartsReference = FirebaseDatabase.getInstance()
            .getReference("Test/TestExercises/$musclePartName")


        exerciseMusclePartsReference.get().addOnSuccessListener {
            var nextExId = it.child("nextId").value.toString().toInt()
            exerciseMusclePartsReference.child(nextExId.toString()).updateChildren(hashMap)
            nextExId+=1
            exerciseMusclePartsReference.child("nextId").setValue(nextExId)
        }.addOnFailureListener{
            Log.e("firebase", "Error getting data", it)
        }
    }
}