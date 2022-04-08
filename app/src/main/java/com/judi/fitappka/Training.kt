package com.judi.fitappka

class Training(val id: Int, var date: String = "") {
    val exerciseList = mutableListOf<Exercise>()

    fun addExercise(newExercise: Exercise) {
        exerciseList.add(newExercise)
    }

    fun changeDate(newDate: String) {
        if(newDate.length == 8) {
            date = newDate
        }
    }
}