package com.judi.fitappka

import com.google.firebase.database.DataSnapshot

open class ExerciseTemplate(var id: Int,
                            var musclePart: String,
                            var name: String,
                            var containsReps: Boolean = false,
                            var containsSeries: Boolean = false,
                            var containsWeight: Boolean = false,
                            var containsDistance: Boolean = false,
                            var containsDuration: Boolean = false) {
    private var unknownData: MutableMap<String?, String> = mutableMapOf()
    private var unknownDataIterator = 0

    fun createFromJSONData(dataSnapshot: DataSnapshot, musclePartBranch: String): Boolean {
        musclePart = musclePartBranch

        id = dataSnapshot.key.toString().toInt()

        for (property in dataSnapshot.children) {
            when (property.key) {
                "name" -> name = property.value.toString()
                "reps" -> containsReps = true
                "series" -> containsSeries = true
                "weight" -> containsWeight = true
                "distance" -> containsDistance = true
                "duration" -> containsDuration = true
                else -> {
                    if (property.key == null) {
                        unknownData[unknownDataIterator.toString()] = property.value.toString()
                        unknownDataIterator += 1
                    } else {
                        unknownData[property.key] = property.value.toString()
                    }
                }
            }
        }
        if(name == "" && id == -1) {
            return false
        }
        return true
    }
}