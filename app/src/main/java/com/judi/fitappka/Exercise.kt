package com.judi.fitappka

class Exercise {
    val listOfSeries = mutableListOf<Series>()
    var id = -1
    var musclePart = "-1"
    var name = ""
    var containsReps = false
    var containsWeight = false
    var containsDistance = false
    var containsDuration = false

    fun addSeries(series: Series) {
        listOfSeries.add(series)
    }

    fun createFromJSONData(newID: Int, inputMusclePart: String,
                           exerciseTemplateSet: MutableSet<ExerciseTemplate>) : Boolean {
        var isInSet = false

        for(exerciseTemplate in exerciseTemplateSet) {
            if(newID == exerciseTemplate.id && inputMusclePart == exerciseTemplate.musclePart) {
                isInSet = true
                id = exerciseTemplate.id
                musclePart = exerciseTemplate.musclePart
                name = exerciseTemplate.name
                if (exerciseTemplate.containsReps) containsReps = true
                if (exerciseTemplate.containsWeight) containsWeight = true
                if (exerciseTemplate.containsDistance) containsDistance = true
                if (exerciseTemplate.containsDuration) containsDuration = true
                return isInSet
            }
        }
        id = -1
        musclePart = "-1"
        return isInSet
    }
}