package com.judi.fitappka

class Training(val id: Int, var date: String = "") {
    private val exerciseList = mutableListOf<Exercise>()

    fun addExercise(newExercise: Exercise) {
        for(exercise in exerciseList) {
            if(exercise.id == newExercise.id) {
                exerciseList.remove(exercise)
                break
            }
        }
        exerciseList.add(newExercise)
    }

    fun changeDate(newDate: String) {
        if(newDate.length == 8) {
            date = newDate
        }
    }
}