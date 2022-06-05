package com.judi.fitappka

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import com.judi.fitappka.databinding.ActivityTrainingsSummaryBinding
import extensions.Extensions.toast
import extensions.OnSwipeTouchListener
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class TrainingsSummaryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTrainingsSummaryBinding
    private lateinit var exerciseTemplateReference: DatabaseReference
    private lateinit var trainingsDataReference: DatabaseReference
    private lateinit var onGraphSwipeTouchListener: OnSwipeTouchListener

    var exerciseTemplateSet: MutableSet<ExerciseTemplate> = mutableSetOf()
    var trainingsDataSet: MutableSet<Training> = mutableSetOf()
    var selectedMusclePartId = 0
    var selectedExerciseId = 0
    var selectedDateRange = 7
    var selectedExerciseTemplate: ExerciseTemplate? = null

    var listOfGraphSeries: MutableMap<String, LineGraphSeries<DataPoint>> = mutableMapOf()
    var listOfGraphSeriesIterator = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrainingsSummaryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val activityContext = this
        val userUID = Firebase.auth.currentUser?.uid.toString()

        onGraphSwipeTouchListener = object : OnSwipeTouchListener(activityContext) {
            override fun onSwipeLeft() {
                listOfGraphSeriesIterator++
                if(listOfGraphSeriesIterator >= listOfGraphSeries.size) {
                    listOfGraphSeriesIterator = 0
                }
                for (child in binding.linearLayoutSummary.children) {
                    if(child !is GraphView) binding.linearLayoutSummary.removeView(child)
                }
                binding.graph.removeAllSeries()
                binding.graph.addSeries(listOfGraphSeries.values.toList()[listOfGraphSeriesIterator])
                val textView = TextView(activityContext)
                textView.text = decodeSummary(listOfGraphSeries.keys.toList()[listOfGraphSeriesIterator])
                textView.gravity = Gravity.CENTER
                textView.setTextColor(Color.WHITE)
                binding.linearLayoutSummary.addView(textView, 0)
            }
            override fun onSwipeRight() {
                listOfGraphSeriesIterator--
                if(listOfGraphSeriesIterator < 0) {
                    listOfGraphSeriesIterator = listOfGraphSeries.size - 1
                }
                for (child in binding.linearLayoutSummary.children) {
                    if(child !is GraphView) binding.linearLayoutSummary.removeView(child)
                }
                binding.graph.removeAllSeries()
                binding.graph.addSeries(listOfGraphSeries.values.toList()[listOfGraphSeriesIterator])
                val textView = TextView(activityContext)
                textView.text = decodeSummary(listOfGraphSeries.keys.toList()[listOfGraphSeriesIterator])
                textView.gravity = Gravity.CENTER
                textView.setTextColor(Color.WHITE)
                binding.linearLayoutSummary.addView(textView, 0)
            }
        }

        binding.graph.setOnTouchListener(onGraphSwipeTouchListener)

        exerciseTemplateReference = FirebaseDatabase.getInstance()
            .getReference("Test/TestExercises") //should be "exercise templates" in db
        trainingsDataReference = FirebaseDatabase.getInstance()
            .getReference("Test/UserData/$userUID")

        binding.buttonBackFromSummary.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

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
            }
            override fun onCancelled(error: DatabaseError) {
                toast(getString(R.string.error_connecting_to_db, error.toString()))
            }
        })

        trainingsDataReference.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Handler().postDelayed({
                    updateTrainingList(snapshot)
                    binding.spinnerChooseMusclePart.adapter = musclePartListAdapter
                    binding.spinnerChooseMusclePart.setSelection(selectedMusclePartId)
                }, 150)
            }

            override fun onCancelled(error: DatabaseError) {
                toast(getString(R.string.error_connecting_to_db, error.toString()))
            }
        })

        binding.spinnerChooseMusclePart.onItemSelectedListener =
            object: AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, id: Int, pos: Long) {
                    selectedMusclePartId = binding.spinnerChooseMusclePart.selectedItemId.toInt()
                    selectedExerciseId = 0
                    val exerciseListAdapter = ArrayAdapter<String>(activityContext,
                        android.R.layout.simple_spinner_dropdown_item)

                    for(exercise in exerciseTemplateSet) {
                        if(exercise.musclePart == encodeMusclePartName(binding
                                .spinnerChooseMusclePart.selectedItem.toString())){
                            exerciseListAdapter.add(exercise.name)
                        }
                    }
                    binding.spinnerChooseExcercise.adapter = exerciseListAdapter
                    binding.spinnerChooseExcercise.setSelection(selectedExerciseId)
                }
                override fun onNothingSelected(arg0: AdapterView<*>?) {}
        }

        binding.spinnerChooseExcercise.onItemSelectedListener =
            object: AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, id: Int, pos: Long) {
                    selectedExerciseId = binding.spinnerChooseExcercise.selectedItemId.toInt()
                    for (exerciseTemplate in exerciseTemplateSet) {
                        if (exerciseTemplate.musclePart == encodeMusclePartName(
                                binding.spinnerChooseMusclePart.selectedItem.toString())) {
                            if (exerciseTemplate.name ==
                                binding.spinnerChooseExcercise.selectedItem.toString()) {
                                selectedExerciseTemplate = exerciseTemplate
                                calculateExerciseSummary(exerciseTemplate, trainingsDataSet)
                            }
                        }
                    }
                }
                override fun onNothingSelected(arg0: AdapterView<*>?) {}
        }

        val dateRangesAdapterView = ArrayAdapter<String>(this,
            android.R.layout.simple_spinner_dropdown_item)
        dateRangesAdapterView.add(getString(R.string.last_7_days))
        dateRangesAdapterView.add(getString(R.string.last_30_days))
        dateRangesAdapterView.add(getString(R.string.last_90_days))
        dateRangesAdapterView.add(getString(R.string.last_year))

        binding.spinnerChooseDates.adapter = dateRangesAdapterView
        binding.spinnerChooseDates.setSelection(0)

        binding.spinnerChooseDates.onItemSelectedListener =
            object: AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, id: Int, pos: Long) {
                    selectedDateRange = when(binding.spinnerChooseDates.selectedItem.toString()) {
                        getString(R.string.last_7_days) -> 7
                        getString(R.string.last_30_days) -> 30
                        getString(R.string.last_90_days) -> 90
                        getString(R.string.last_year) -> 365
                        else -> 0
                    }

                    if(selectedExerciseTemplate != null) {
                        calculateExerciseSummary(selectedExerciseTemplate!!, trainingsDataSet)
                    }
                }
                override fun onNothingSelected(arg0: AdapterView<*>?) {}
        }
    }

    fun updateTrainingList(snapshot: DataSnapshot) {
        trainingsDataSet.clear()
        for (training in snapshot.children) {
            if(training.key.toString() == "nextId") continue
            else {
                val trainingId = training.key.toString().toInt()
                val newTraining = Training(trainingId)
                for (musclePart in training.children) {
                    val musclePartName = musclePart.key.toString()
                    if(musclePartName == "date") {
                        newTraining.changeDate(musclePart.value.toString())
                    }
                    else {
                        for (exercise in musclePart.children) {
                            val exerciseId = exercise.key.toString().toInt()
                            val newExercise = Exercise()
                            newExercise.createFromJSONData(exerciseId, musclePartName,
                                exerciseTemplateSet)
                            for(series in exercise.children) {
                                val newSeries = Series()
                                newSeries.addToExercise(series, series.key.toString().toInt(),
                                    newExercise, exerciseTemplateSet)
                            }
                            newTraining.addExercise(newExercise)
                        }
                    }
                }
                trainingsDataSet.add(newTraining)
            }
        }
    }

    fun calculateExerciseSummary(exerciseTemplate: ExerciseTemplate,
                                 trainingSet: Set<Training>) {
        val summaryHashMap = mutableMapOf<String, Float>()
        val graphInfoHashMap = mutableMapOf<String, List<Float>>()
        if(exerciseTemplate.containsWeight) {
            summaryHashMap["Total weight"] = 0f
            graphInfoHashMap["Total weight"] = mutableListOf()
        }
        if(exerciseTemplate.containsDuration) {
            summaryHashMap["Total duration"] = 0f
            graphInfoHashMap["Total duration"] = mutableListOf()
        }
        if(exerciseTemplate.containsDistance) {
            summaryHashMap["Total distance"] = 0f
            graphInfoHashMap["Total distance"] = mutableListOf()
        }


        val sdf = SimpleDateFormat("ddMMyyyy", Locale.getDefault())
        val currentDate = sdf.format(Date())

        val weightPointList = mutableListOf<Float>()
        val durationPointList = mutableListOf<Float>()
        val distancePointList = mutableListOf<Float>()

        for(training in trainingSet) {
            if(calculateDaysBetweenDates(currentDate, training.date) <= selectedDateRange) {
                var weightPoints = 0f
                var durationPoints = 0f
                var distancePoints = 0f
                for (musclePart in training.musclePartMap) {
                    if(musclePart.key == exerciseTemplate.musclePart) {
                        for (exerciseInTraining in musclePart.value) {
                            if(exerciseInTraining.id == exerciseTemplate.id) {
                                if(exerciseTemplate.containsWeight) {
                                    for (series in exerciseInTraining.listOfSeries) {
                                        var toAdd = series.weight!!
                                        if(exerciseTemplate.containsReps) {
                                            toAdd *= series.reps!!
                                        }
                                        summaryHashMap["Total weight"] =
                                            summaryHashMap["Total weight"]!! + toAdd
                                        weightPoints += toAdd
                                    }
                                }
                                if(exerciseTemplate.containsDuration) {
                                    for (series in exerciseInTraining.listOfSeries) {
                                        var toAdd = series.duration!!
                                        if(exerciseTemplate.containsReps) {
                                            toAdd *= series.reps!!
                                        }
                                        summaryHashMap["Total duration"] =
                                            summaryHashMap["Total duration"]!! + toAdd
                                        durationPoints += toAdd
                                    }
                                }
                                if(exerciseTemplate.containsDistance) {
                                    for (series in exerciseInTraining.listOfSeries) {
                                        var toAdd = series.distance!!
                                        if(exerciseTemplate.containsReps) {
                                            toAdd *= series.reps!!
                                        }
                                        summaryHashMap["Total distance"] =
                                            summaryHashMap["Total distance"]!! + toAdd
                                        distancePoints += toAdd
                                    }
                                }
                            }
                        }
                    }
                }

                if(exerciseTemplate.containsWeight) {
                    weightPointList.add(weightPoints)
                }
                if(exerciseTemplate.containsDuration) {
                    durationPointList.add(durationPoints)
                }
                if(exerciseTemplate.containsDistance) {
                    distancePointList.add(distancePoints)
                }
            }
        }

        if(exerciseTemplate.containsWeight) {
            graphInfoHashMap["Total weight"] = weightPointList
        }
        if(exerciseTemplate.containsDuration) {
            graphInfoHashMap["Total duration"] = durationPointList
        }
        if(exerciseTemplate.containsDistance) {
            graphInfoHashMap["Total distance"] = distancePointList
        }



        if(exerciseTemplate.containsDistance && exerciseTemplate.containsDuration) {
            if(summaryHashMap["Total distance"]!! > 0f && summaryHashMap["Total duration"]!! > 0f) {
                summaryHashMap["Average speed"] =
                    (summaryHashMap["Total distance"]!! / summaryHashMap["Total duration"]!!) * 60f
            }
        }

        addExerciseSummaryInfoToLayout(summaryHashMap, graphInfoHashMap,
            binding.linearLayoutSummary)
    }

    private fun addExerciseSummaryInfoToLayout(summary: MutableMap<String, Float>,
                                               graphPoints: MutableMap<String, List<Float>>,
                                               layout: LinearLayout) {
        for (child in layout.children) {
            if(child !is GraphView) layout.removeView(child)
        }

        val layoutParams = LinearLayout
            .LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT)
        layoutParams.setMargins(5, 5, 5, 10)

        for (pair in summary) {
            val horizontalLL = LinearLayout(this)
            horizontalLL.orientation = LinearLayout.HORIZONTAL
            horizontalLL.gravity = Gravity.CENTER
            horizontalLL.layoutParams = layoutParams

            val textViewKey = TextView(this)
            textViewKey.text = decodeSummary(pair.key) + ":"
            textViewKey.textSize = resources.getDimension(R.dimen.trainingMediumFontSize)
            textViewKey.gravity = Gravity.CENTER
            textViewKey.setPadding(5, 10, 5, 15)
            textViewKey.setTextColor(resources
                .getColor(R.color.primaryLayoutText))

            val textViewValue = TextView(this)
            textViewValue.text = String.format("%.2f", pair.value)
            textViewValue.textSize = resources.getDimension(R.dimen.trainingMediumFontSize)
            textViewValue.gravity = Gravity.CENTER
            textViewValue.setPadding(5, 10, 5, 15)
            textViewValue.setTextColor(resources
                .getColor(R.color.primaryLayoutText))

            horizontalLL.addView(textViewKey)
            horizontalLL.addView(textViewValue)
            //layout.addView(horizontalLL)
        }

        drawGraphSummary(graphPoints)
    }

    private fun drawGraphSummary(graphPoints: MutableMap<String, List<Float>>) {
        binding.graph.removeAllSeries()
        listOfGraphSeries.clear()
        listOfGraphSeriesIterator = 0

        for (pair in graphPoints) {
            val series = LineGraphSeries<DataPoint>()
            for (i in 0 until pair.value.size) {
                series.appendData(DataPoint(i.toDouble(), pair.value[i].toDouble()),
                    true, pair.value.size)
            }

            series.isDrawDataPoints = true
            series.dataPointsRadius = 10f
            series.color = Color.GREEN
            series.thickness = 5
            series.title = decodeSummary(pair.key)

            listOfGraphSeries[pair.key] = series
        }

        binding.graph.addSeries(listOfGraphSeries.values.toList()[listOfGraphSeriesIterator])
        val textView = TextView(this)
        textView.text = decodeSummary(listOfGraphSeries.keys.toList()[listOfGraphSeriesIterator])
        textView.gravity = Gravity.CENTER
        textView.setTextColor(Color.WHITE)
        binding.linearLayoutSummary.addView(textView, 0)
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

    private fun decodeSummary(summary: String): String {
        return when (summary) {
            "Total weight" -> getString(R.string.total_weight)
            "Total duration" -> getString(R.string.total_duration)
            "Total distance" -> getString(R.string.total_distance)
            "Average speed" -> getString(R.string.average_speed)
            else -> summary
        }
    }

    private fun calculateDaysBetweenDates(currentDateUnformatted: String,
                                          trainingDateUnformatted: String): Long {
        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

        val currentDay = currentDateUnformatted.substring(0, 2)
        val currentMonth = currentDateUnformatted.substring(2, 4)
        val currentYear = currentDateUnformatted.substring(4, 8)
        val currentDateFormatted = "$currentDay.$currentMonth.$currentYear"
        val currentDate = sdf.parse(currentDateFormatted)

        val trainingDay = trainingDateUnformatted.substring(0, 2)
        val trainingMonth = trainingDateUnformatted.substring(2, 4)
        val trainingYear = trainingDateUnformatted.substring(4, 8)
        val trainingDateFormatted = "$trainingDay.$trainingMonth.$trainingYear"
        val trainingDate = sdf.parse(trainingDateFormatted)

        val diff: Long = (currentDate?.time ?: 0) - (trainingDate?.time ?: 0)

        return TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS)
    }
}