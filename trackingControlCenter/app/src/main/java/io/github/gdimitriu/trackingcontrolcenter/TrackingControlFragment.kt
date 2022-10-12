package io.github.gdimitriu.trackingcontrolcenter

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothSocket
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.switchmaterial.SwitchMaterial
import java.lang.Exception

private const val TAG = "TrackingControl"

@DelicateCoroutinesApi
class TrackingControlFragment : Fragment() {
    private val trackingSettingsViewModel: TrackingSettingsViewModel by activityViewModels()

    private var isStart : Boolean = false
    //disconnect
    private lateinit var disconnectButton : Button
    private lateinit var startButton : Button
    private lateinit var stopButton : Button
    private lateinit var deployButton : Button
    private lateinit var loadButton : Button
    private lateinit var trackingOnOffSwitch : SwitchMaterial
    private lateinit var lampOnOffSwitch : SwitchMaterial
    private lateinit var enginePanOnOffSwitch : SwitchMaterial
    private lateinit var kiEditText : EditText
    private lateinit var kpEditText : EditText
    private lateinit var kdEditText : EditText

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.tracking_control, container, false)
        //disconnect button
        disconnectButton = view.findViewById(R.id.droid_disconnect)
        disconnectButton.setOnTouchListener { view, motionEvent ->
            val event = motionEvent as MotionEvent
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                trackingSettingsViewModel.closeSockets()
            }
            return@setOnTouchListener false
        }
        //start button
        startButton = view.findViewById(R.id.droid_start)
        startButton.setOnTouchListener { view, motionEvent ->
            val event = motionEvent as MotionEvent
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                if (sendOneWayCommandToDroid("S#\n")) {
                    Log.d(TAG, "Start")
                }
            }
            return@setOnTouchListener false
        }
        //stop button
        stopButton = view.findViewById(R.id.droid_stop)
        stopButton.setOnTouchListener { view, motionEvent ->
            val event = motionEvent as MotionEvent
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                if (sendOneWayCommandToDroid("s#\n")) {
                    Log.d(TAG, "Stop")
                }
            }
            return@setOnTouchListener false
        }
        //deploy button
        deployButton = view.findViewById(R.id.droid_deploy)
        deployButton.setOnTouchListener { view, motionEvent ->
            val event = motionEvent as MotionEvent
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                if (trackingSettingsViewModel.isChangedKp) {
                    if (sendOneWayCommandToDroid(String.format("P%s#\n",trackingSettingsViewModel.currentKp))) {
                        Log.d(TAG, "Send Kp $trackingSettingsViewModel.currentKp")
                        trackingSettingsViewModel.isChangedKp = false
                    }
                }
                if (trackingSettingsViewModel.isChangedKd) {
                    if (sendOneWayCommandToDroid(String.format("D%s#\n",trackingSettingsViewModel.currentKd))) {
                        Log.d(TAG, "Send Kd $trackingSettingsViewModel.currentKd")
                        trackingSettingsViewModel.isChangedKd = false
                    }
                }
                if (trackingSettingsViewModel.isChangedKi) {
                    if (sendOneWayCommandToDroid(String.format("I%s#\n",trackingSettingsViewModel.currentKi))) {
                        Log.d(TAG, "Send Ki $trackingSettingsViewModel.currentKi")
                        trackingSettingsViewModel.isChangedKi = false
                    }
                }
                if (trackingSettingsViewModel.isChangedTracking) {
                    if (trackingSettingsViewModel.currentTracking) {
                        if (sendOneWayCommandToDroid("T#\n")) {
                            Log.d(TAG, "Send Tracking on")
                            trackingSettingsViewModel.currentTracking = false
                        }
                    } else {
                        if (sendOneWayCommandToDroid("t#\n")) {
                            Log.d(TAG, "Send Tracking off")
                            trackingSettingsViewModel.currentTracking = false
                        }
                    }
                }
                if (trackingSettingsViewModel.isChangedEnginePan) {
                    if (sendOneWayCommandToDroid("E#\n")) {
                        Log.d(TAG, "Send Engine Pan ${trackingSettingsViewModel.currentEnginePanStatus}")
                        trackingSettingsViewModel.isChangedEnginePan = false
                    }
                }
                if (trackingSettingsViewModel.isChangedLamp) {
                    if (sendOneWayCommandToDroid("L#\n")) {
                        Log.d(TAG, "Send Lamp ${trackingSettingsViewModel.currentEnginePanStatus}")
                        trackingSettingsViewModel.isChangedLamp = false
                    }
                }
            }
            return@setOnTouchListener false
        }
        //load button
        loadButton = view.findViewById(R.id.droid_load)
        loadButton.setOnTouchListener { view, motionEvent ->
            val event = motionEvent as MotionEvent
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                getDataFromDroid();
                kiEditText.setText(trackingSettingsViewModel.currentKi)
                kdEditText.setText(trackingSettingsViewModel.currentKd)
                kpEditText.setText(trackingSettingsViewModel.currentKp)
            }
            return@setOnTouchListener false
        }
        trackingOnOffSwitch = view.findViewById(R.id.TrackingOnOff)
        trackingOnOffSwitch.setOnCheckedChangeListener {
                _, isChecked -> trackingSettingsViewModel.currentTracking = isChecked
            trackingSettingsViewModel.isChangedTracking = true
        }
        lampOnOffSwitch = view.findViewById(R.id.LampOnOff)
        lampOnOffSwitch.setOnCheckedChangeListener {
                _, isChecked -> trackingSettingsViewModel.currentLampStatus = isChecked
            trackingSettingsViewModel.isChangedLamp = true
        }
        enginePanOnOffSwitch = view.findViewById(R.id.EnginePanOnOff)
        enginePanOnOffSwitch.setOnCheckedChangeListener {
                _, isChecked -> trackingSettingsViewModel.currentEnginePanStatus = isChecked
            trackingSettingsViewModel.isChangedEnginePan = true
        }
        //Ki
        val kiWatcher = object : TextWatcher {
            override fun beforeTextChanged(sequence: CharSequence?, start: Int, count: Int, after: Int) {
                //
            }

            override fun onTextChanged(sequence: CharSequence?, start: Int, before: Int, count: Int) {
                trackingSettingsViewModel.currentKi = sequence.toString()
                trackingSettingsViewModel.isChangedKi = true
            }

            override fun afterTextChanged(sequence: Editable?) {
                //
            }
        }
        kiEditText = view.findViewById(R.id.KiData)
        kiEditText.setText(trackingSettingsViewModel.currentKi)
        kiEditText.addTextChangedListener(kiWatcher)
        //Kd
        val kdWatcher = object : TextWatcher {
            override fun beforeTextChanged(sequence: CharSequence?, start: Int, count: Int, after: Int) {
                //
            }

            override fun onTextChanged(sequence: CharSequence?, start: Int, before: Int, count: Int) {
                trackingSettingsViewModel.currentKd = sequence.toString()
                trackingSettingsViewModel.isChangedKd = true
            }

            override fun afterTextChanged(sequence: Editable?) {
                //
            }
        }
        kdEditText = view.findViewById(R.id.KdData)
        kdEditText.setText(trackingSettingsViewModel.currentKd)
        kdEditText.addTextChangedListener(kdWatcher)
        //Kp
        val kpWatcher = object : TextWatcher {
            override fun beforeTextChanged(sequence: CharSequence?, start: Int, count: Int, after: Int) {
                //
            }

            override fun onTextChanged(sequence: CharSequence?, start: Int, before: Int, count: Int) {
                trackingSettingsViewModel.currentKp = sequence.toString()
                trackingSettingsViewModel.isChangedKp = true
            }

            override fun afterTextChanged(sequence: Editable?) {
                //
            }
        }
        kpEditText = view.findViewById(R.id.KpData)
        kpEditText.setText(trackingSettingsViewModel.currentKp)
        kpEditText.addTextChangedListener(kpWatcher)
        return view;
    }

    private fun sendOneWayCommandToDroid(message : String, hasAck : Boolean = true) : Boolean = runBlocking {
        if (trackingSettingsViewModel.connectionType == ConnectionType.BLE && validateBleSocketConnection(trackingSettingsViewModel.bleSocket)) {
            var job = GlobalScope.launch {
                try {
                    val outputStreamWriter =
                        OutputStreamWriter(trackingSettingsViewModel.bleSocket?.getOutputStream())
                    val inputStreamReader =
                        BufferedReader(InputStreamReader(trackingSettingsViewModel.bleSocket?.getInputStream()))
                    //send data
                    outputStreamWriter.write(message)
                    outputStreamWriter.flush()
                    if (hasAck) {
                        val status = inputStreamReader.readLine()
                        Log.d(TAG, "s=$status")
                    }
                } catch (e : Exception) {
                    trackingSettingsViewModel.bleSocket?.close()
                    trackingSettingsViewModel.bleSocket = null
                    trackingSettingsViewModel.connectionType = ConnectionType.NONE
                    Log.e(TAG, e.toString())
                }
            }
            job.join()
            return@runBlocking true
        } else if (trackingSettingsViewModel.connectionType == ConnectionType.NONE) {
            val builder: AlertDialog.Builder? = activity?.let {
                AlertDialog.Builder(it)
            }
            builder?.setMessage("Connect first the Droid !")?.setTitle("Connection failed !")
            val dialog: AlertDialog? = builder?.create()
            dialog?.show()
        }
        return@runBlocking false
    }

    private fun getDataFromDroid() : Boolean = runBlocking {
        if (trackingSettingsViewModel.connectionType == ConnectionType.BLE && validateBleSocketConnection(trackingSettingsViewModel.bleSocket)) {
            var job = GlobalScope.launch {
                try {
                    val outputStreamWriter =
                        OutputStreamWriter(trackingSettingsViewModel.bleSocket?.getOutputStream())
                    val inputStreamReader =
                        BufferedReader(InputStreamReader(trackingSettingsViewModel.bleSocket?.getInputStream()))
                    //send data
                    //Kd
                    outputStreamWriter.write("D#")
                    outputStreamWriter.flush()
                    var status = inputStreamReader.readLine()
                    trackingSettingsViewModel.currentKd = status
                    trackingSettingsViewModel.isChangedKd = false
                    Log.d(TAG, "s=$status")
                    //Kp
                    outputStreamWriter.write("P#")
                    outputStreamWriter.flush()
                    status = inputStreamReader.readLine()
                    trackingSettingsViewModel.currentKp = status
                    trackingSettingsViewModel.isChangedKp = false
                    Log.d(TAG, "s=$status")
                    //Ki
                    outputStreamWriter.write("I#")
                    outputStreamWriter.flush()
                    status = inputStreamReader.readLine()
                    trackingSettingsViewModel.currentKi = status
                    trackingSettingsViewModel.isChangedKi = false
                    Log.d(TAG, "s=$status")
                } catch (e : Exception) {
                    trackingSettingsViewModel.bleSocket?.close()
                    trackingSettingsViewModel.bleSocket = null
                    trackingSettingsViewModel.connectionType = ConnectionType.NONE
                    Log.e(TAG, e.toString())
                }
            }
            job.join()
            return@runBlocking true
        } else if (trackingSettingsViewModel.connectionType == ConnectionType.NONE) {
            val builder: AlertDialog.Builder? = activity?.let {
                AlertDialog.Builder(it)
            }
            builder?.setMessage("Connect first the Droid !")?.setTitle("Connection failed !")
            val dialog: AlertDialog? = builder?.create()
            dialog?.show()
        }
        return@runBlocking false
    }
    companion object {
        fun newInstance(): TrackingControlFragment {
            return TrackingControlFragment()
        }
    }

    override fun onStop() {
        super.onStop()
    }

    private fun validateBleSocketConnection(socket: BluetoothSocket?): Boolean {
        if (socket == null || !socket.isConnected) {
            val builder: AlertDialog.Builder? = activity?.let {
                AlertDialog.Builder(it)
            }
            builder?.setMessage("Connect first the Droid !")?.setTitle("Connection failed !")
            val dialog: AlertDialog? = builder?.create()
            dialog?.show()
            return false
        }
        return true
    }
}