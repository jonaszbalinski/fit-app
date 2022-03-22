package com.judi.fitappka

import com.google.firebase.database.DataSnapshot

class Exercise(private var id: Int, private var musclePart: String, var name: String,
               private var reps: Boolean = false, private var series: Boolean = false,
               private var weight: Boolean = false, private var distance: Boolean = false,
               private var duration: Boolean = false) {
    private var unknownData: MutableMap<String?, String> = mutableMapOf()
    private var unknownDataIterator = 0

    fun createFromJSONData(dataSnapshot: DataSnapshot, musclePartBranch: String? = null): Boolean {
        for (property in dataSnapshot.children) {
            musclePart = musclePartBranch ?: "???"

            when (property.key) {
                "id" -> id = property.value.toString().toInt()
                "name" -> name = property.value.toString()
                "reps" -> reps = true
                "series" -> series = true
                "weight" -> weight = true
                "distance" -> distance = true
                "duration" -> duration = true
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