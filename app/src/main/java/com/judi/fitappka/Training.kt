package com.judi.fitappka

class Training(val id: Int, var date: String = "") {
    val musclePartMap = hashMapOf<String, MutableList<Exercise>>()

    fun addExercise(newExercise: Exercise) {
        if(musclePartMap[newExercise.musclePart] == null)
            musclePartMap[newExercise.musclePart] = mutableListOf(newExercise)
        else
            musclePartMap[newExercise.musclePart]!!.add(newExercise)
    }

    fun changeDate(newDate: String) {
        if(newDate.length == 8) {
            date = newDate
        }
    }

    fun getNextSeriesId(exercise: Exercise): Int {
        if(musclePartMap[exercise.musclePart] != null) {
            for(exercisesInList in musclePartMap[exercise.musclePart]!!) {
                if(exercise.id == exercisesInList.id) {
                    var nextId = -1
                    for(series in exercisesInList.listOfSeries) {
                        if (series.id >= nextId) {
                            nextId = series.id + 1
                        }
                    }

                    return nextId
                }
            }
        }

        return 1
    }
}