package co.tryterra.terrawearosdemo

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import co.tryterra.terrawearos.enums.StreamDataTypes
import co.tryterra.terrawearos.Terra
import co.tryterra.terrawearos.enums.DataTypes
import co.tryterra.terrawearos.enums.ExerciseTypes
import co.tryterra.terrawearosdemo.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

@ExperimentalCoroutinesApi
class MainActivity : Activity(), CoroutineScope {

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    private lateinit var binding: ActivityMainBinding

    private var terra: Terra? =null
    private lateinit var dataView: TextView
    private lateinit var startRecordRT: Button
    private lateinit var connectB: Button
    private lateinit var exercise: Button
    private var connected: Boolean = false

    private lateinit var stopButton: Button
    private lateinit var pauseButton: Button
    private lateinit var resumeButton: Button

    private lateinit var heartRateEx: TextView
    private lateinit var stepEx: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        terra = Terra(this, setOf(StreamDataTypes.HEART_RATE))

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dataView = findViewById(R.id.textView)

        startRecordRT = findViewById(R.id.startStream)
        connectB = findViewById(R.id.connectTerra)
        exercise = findViewById(R.id.startExercise)


        startRecordRT.setOnClickListener {
            if (connected) {
                terra!!.startStream()
                streamData()
            }
        }
        connectB.setOnClickListener {
            connectBT()
        }

        exercise.setOnClickListener {
            terra!!.prepareExercise(
                ExerciseTypes.RUNNING,
                setOf(DataTypes.STEPS, DataTypes.HEART_RATE),
                false
            ) {
                if (it) {
                    terra!!.startExercise(
                        ExerciseTypes.RUNNING,
                        setOf(DataTypes.STEPS, DataTypes.HEART_RATE, DataTypes.TOTAL_CALORIES)
                    )
                    runOnUiThread(Runnable {
                        setContentView(R.layout.exercise_layout)
                        stopButton = findViewById(R.id.stopExercise)
                        pauseButton = findViewById(R.id.pauseExercise)
                        resumeButton = findViewById(R.id.resumeExercise)

                        heartRateEx = findViewById(R.id.heartRateEx)
                        stepEx = findViewById(R.id.stepsEx)

                        stopButton.setOnClickListener {
                            terra!!.stopExercise()
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }

                        pauseButton.setOnClickListener {
                            terra!!.pauseExercise()
                        }

                        resumeButton.setOnClickListener {
                            terra!!.resumeExercise()
                        }

                        suspend{
                            terra!!.steps.collect { data ->
                                stepEx.text = data.toString()
                            }

                            terra!!.heartRate.collect { data ->
                                heartRateEx.text = data.toString()
                            }
                        }
                    })
                }
            }
        }

    }


    private fun connectBT(){
        if (terra!= null){
            terra!!.startBluetoothDiscovery{if (it){connected = true} }
        }
    }

    private fun streamData() = launch {
        if (terra != null) {
            terra!!.dataPoint.collect {
                dataView.text = it.`val`.toString()
            }
        }
    }
}