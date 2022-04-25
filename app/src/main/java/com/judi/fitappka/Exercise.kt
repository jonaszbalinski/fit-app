package com.judi.fitappka

import android.util.Log
import com.google.firebase.database.DataSnapshot

class Exercise(var id: Int, var musclePart: String, var name: String,
               var containsReps: Boolean = false, var containsWeight: Boolean = false,
               var containsDistance: Boolean = false, var containsDuration: Boolean = false) {
    var reps: Int? = null
    var weight: Float? = null
    var distance: Float? = null
    var duration: Float? = null

    init {
        if(containsReps) reps = -1
        if(containsWeight) weight = -1.0f
        if(containsDistance) distance = -1.0f
        if(containsDuration) duration = -1.0f
    }

    fun getValues() : String {
        var toReturn = "$name ($musclePart): "
        if(containsReps) toReturn += " |Reps: $reps| "
        if(containsWeight) toReturn += " |Weight: $weight| "
        if(containsDistance) toReturn += " |Distance: $distance| "
        if(containsDuration) toReturn += " |Duration: $duration| "
        return toReturn
    }

    fun createFromJSONData(dataSnapshot: DataSnapshot, newID: Int, inputMusclePart: String,
                           exerciseTemplateSet: MutableSet<ExerciseTemplate>) : Boolean {
        id = newID
        musclePart = inputMusclePart
        var isInSet = false

        for(exerciseTemplate in exerciseTemplateSet) {
            if(id == exerciseTemplate.id && musclePart == exerciseTemplate.musclePart) {
                isInSet = true
                id = exerciseTemplate.id
                name = exerciseTemplate.name
                copyExerciseInfo(exerciseTemplate)

                for(property in dataSnapshot.children) {
                    when (property.key) {
                        "reps" -> reps = property.value.toString().toInt()
                        "weight" -> weight = property.value.toString().toFloat()
                        "distance" -> distance = property.value.toString().toFloat()
                        "duration" -> duration = property.value.toString().toFloat()
                    }
                }
                return isInSet
            }
        }
        return isInSet
    }

    private fun copyExerciseInfo(exerciseTemplate: ExerciseTemplate) {
        if(exerciseTemplate.containsReps) {
            containsReps = true;
            reps = -1
        }
        if(exerciseTemplate.containsWeight) {
            containsWeight = true;
            weight = -1.0f
        }
        if(exerciseTemplate.containsDistance) {
            containsDistance = true;
            distance = -1.0f
        }
        if(exerciseTemplate.containsDuration) {
            containsDuration= true;
            duration = -1.0f
        }
    }
}