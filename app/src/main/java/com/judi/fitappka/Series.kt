package com.judi.fitappka

import com.google.firebase.database.DataSnapshot

class Series {
    var id = -1
    var reps: Int? = null
    var weight: Float? = null
    var distance: Float? = null
    var duration: Float? = null

    fun addToExercise(dataSnapshot: DataSnapshot, newID: Int, exercise: Exercise,
                      exerciseTemplateSet: MutableSet<ExerciseTemplate>): Boolean {
        id = newID
        for(exerciseTemplate in exerciseTemplateSet) {
            if(exercise.id == exerciseTemplate.id &&
                exercise.musclePart == exerciseTemplate.musclePart) {
                addDataFromJSON(dataSnapshot, exerciseTemplate)
                exercise.addSeries(this)
                return true
            }
        }
        return false
    }

    private fun addDataFromJSON(dataSnapshot: DataSnapshot, exerciseTemplate: ExerciseTemplate) {
        initProperties(exerciseTemplate)
        for(property in dataSnapshot.children) {
            when (property.key) {
                "reps" -> reps = property.value.toString().toInt()
                "weight" -> weight = property.value.toString().toFloat()
                "distance" -> distance = property.value.toString().toFloat()
                "duration" -> duration = property.value.toString().toFloat()
            }
        }
    }

    private fun initProperties(exerciseTemplate: ExerciseTemplate) {
        if(exerciseTemplate.containsReps) {
            reps = -1
        }
        if(exerciseTemplate.containsWeight) {
            weight = -1.0f
        }
        if(exerciseTemplate.containsDistance) {
            distance = -1.0f
        }
        if(exerciseTemplate.containsDuration) {
            duration = -1.0f
        }
    }
}