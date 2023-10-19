package io.github.gdimitriu.automovecontrolcenter

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothSocket
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

private const val TAG = "AutomoveControl"

class AutomoveControlFragment : Fragment() {

    private val automoveSettingsViewModel :AutomoveSettingsViewModel by activityViewModels()
    
    private lateinit var disconnectButton : Button
    private lateinit var startButton : Button
    private lateinit var stopButton : Button
    private lateinit var deployButton : Button
    private lateinit var loadButton : Button
    private lateinit var kiEditText : EditText
    private lateinit var kpEditText : EditText
    private lateinit var kdEditText : EditText
    private lateinit var forwardBarcodeText : EditText
    private lateinit var backwardBarcodeText : EditText
    private lateinit var leftBarcodeText : EditText
    private lateinit var rightBarcodeText : EditText
    private lateinit var stopBarcodeText : EditText
    private lateinit var currentPowerText : EditText
    private lateinit var tur90Text : EditText
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var lampOnOffSwitch : Switch
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var typeSwitch: Switch
    private lateinit var autocalibrationButton : Button

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.automove_control,container, false)
        //disconnect button
        disconnectButton = view.findViewById(R.id.droid_disconnect)
        disconnectButton.setOnTouchListener { view, motionEvent ->
            val event = motionEvent as MotionEvent
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                automoveSettingsViewModel.closeSockets()
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
        //autocalibration
        autocalibrationButton = view.findViewById(R.id.droid_autocalibration)
        autocalibrationButton.setOnTouchListener { view, motionEvent ->
            val event = motionEvent as MotionEvent
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                if (sendOneWayCommandToDroid("a#\n")) {
                    Log.d(TAG, "Autocalibration")
                }
            }
            return@setOnTouchListener false
        }
        //deploy button
        deployButton = view.findViewById(R.id.droid_deploy)
        deployButton.setOnTouchListener { view, motionEvent ->
            val event = motionEvent as MotionEvent
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                if (automoveSettingsViewModel.isChangedKp) {
                    if (sendOneWayCommandToDroid(String.format("P%s#\n",automoveSettingsViewModel.currentKp))) {
                        Log.d(TAG, "Send Kp $automoveSettingsViewModel.currentKp")
                        automoveSettingsViewModel.isChangedKp = false
                    }
                }
                if (automoveSettingsViewModel.isChangedKd) {
                    if (sendOneWayCommandToDroid(String.format("D%s#\n",automoveSettingsViewModel.currentKd))) {
                        Log.d(TAG, "Send Kd $automoveSettingsViewModel.currentKd")
                        automoveSettingsViewModel.isChangedKd = false
                    }
                }
                if (automoveSettingsViewModel.isChangedKi) {
                    if (sendOneWayCommandToDroid(String.format("I%s#\n",automoveSettingsViewModel.currentKi))) {
                        Log.d(TAG, "Send Ki $automoveSettingsViewModel.currentKi")
                        automoveSettingsViewModel.isChangedKi = false
                    }
                }
                if (automoveSettingsViewModel.isChangedLamp) {
                    if ( automoveSettingsViewModel.currentLampStatus ) {
                        if (sendOneWayCommandToDroid("l1#\n")) {
                            Log.d(
                                TAG,
                                "Send Lamp ${automoveSettingsViewModel.currentLampStatus}"
                            )
                            automoveSettingsViewModel.isChangedLamp = false
                        }
                    } else {
                        if (sendOneWayCommandToDroid("l0#\n")) {
                            Log.d(
                                TAG,
                                "Send Lamp ${automoveSettingsViewModel.currentLampStatus}"
                            )
                            automoveSettingsViewModel.isChangedLamp = false
                        }
                    }
                }
                if (automoveSettingsViewModel.isChangedForwardBarcode) {
                    if (sendOneWayCommandToDroid(String.format("Sf%s#\n",automoveSettingsViewModel.currentForwardBarcode))) {
                        Log.d(TAG, "Send forward barcode ${automoveSettingsViewModel.currentForwardBarcode}")
                        automoveSettingsViewModel.isChangedForwardBarcode = false
                    }
                }
                if (automoveSettingsViewModel.isChangedBackwardBarcode) {
                    if (sendOneWayCommandToDroid(String.format("Sb%s#\n",automoveSettingsViewModel.currentBackwardBarcode))) {
                        Log.d(TAG, "Send backward barcode ${automoveSettingsViewModel.currentBackwardBarcode}")
                        automoveSettingsViewModel.isChangedBackwardBarcode = false
                    }
                }
                if (automoveSettingsViewModel.isChangedLeftBarcode) {
                    if (sendOneWayCommandToDroid(String.format("Sl%s#\n",automoveSettingsViewModel.currentLeftBarcode))) {
                        Log.d(TAG, "Send left barcode ${automoveSettingsViewModel.currentLeftBarcode}")
                        automoveSettingsViewModel.isChangedLeftBarcode = false
                    }
                }
                if (automoveSettingsViewModel.isChangedRightBarcode) {
                    if (sendOneWayCommandToDroid(String.format("Sr%s#\n",automoveSettingsViewModel.currentRightBarcode))) {
                        Log.d(TAG, "Send right barcode ${automoveSettingsViewModel.currentRightBarcode}")
                        automoveSettingsViewModel.isChangedRightBarcode = false
                    }
                }
                if (automoveSettingsViewModel.isChangedStopBarcode) {
                    if (sendOneWayCommandToDroid(String.format("Ss%s#\n",automoveSettingsViewModel.currentStopBarcode))) {
                        Log.d(TAG, "Send stop barcode ${automoveSettingsViewModel.currentStopBarcode}")
                        automoveSettingsViewModel.isChangedStopBarcode = false
                    }
                }
                if (automoveSettingsViewModel.isChangedCurrentPower) {
                    if (sendOneWayCommandToDroid(String.format("V%s#\n",automoveSettingsViewModel.currentPower))) {
                        Log.d(TAG, "Send current power ${automoveSettingsViewModel.currentPower}")
                        automoveSettingsViewModel.isChangedCurrentPower = false
                    }
                }
                if (automoveSettingsViewModel.isChangedTurn90) {
                    if (sendOneWayCommandToDroid(String.format("t%s#\n",automoveSettingsViewModel.currentTurn90))) {
                        Log.d(TAG, "Send turn90 ${automoveSettingsViewModel.currentTurn90} ms")
                        automoveSettingsViewModel.isChangedTurn90 = false
                    }
                }
                if (automoveSettingsViewModel.isChangedLamp) {
                    if ( automoveSettingsViewModel.currentLampStatus ) {
                        if (sendOneWayCommandToDroid("l1#\n")) {
                            Log.d(
                                TAG,
                                "Send Lamp ${automoveSettingsViewModel.currentLampStatus}"
                            )
                            automoveSettingsViewModel.isChangedLamp = false
                        }
                    } else {
                        if (sendOneWayCommandToDroid("l0#\n")) {
                            Log.d(
                                TAG,
                                "Send Lamp ${automoveSettingsViewModel.currentLampStatus}"
                            )
                            automoveSettingsViewModel.isChangedLamp = false
                        }
                    }
                }
                if (automoveSettingsViewModel.isChangedType) {
                    if ( automoveSettingsViewModel.currentType ) {
                        if (sendOneWayCommandToDroid("mb#\n")) {
                            Log.d(TAG, "Send Barcode")
                            automoveSettingsViewModel.isChangedType = false
                        }
                    } else {
                        if (sendOneWayCommandToDroid("ml#\n")) {
                            Log.d(TAG, "Send Line")
                            automoveSettingsViewModel.isChangedType = false
                        }
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
                kiEditText.setText(automoveSettingsViewModel.currentKi)
                kdEditText.setText(automoveSettingsViewModel.currentKd)
                kpEditText.setText(automoveSettingsViewModel.currentKp)
                forwardBarcodeText.setText(automoveSettingsViewModel.currentForwardBarcode)
                backwardBarcodeText.setText(automoveSettingsViewModel.currentBackwardBarcode)
                leftBarcodeText.setText(automoveSettingsViewModel.currentLeftBarcode)
                rightBarcodeText.setText(automoveSettingsViewModel.currentRightBarcode)
                stopBarcodeText.setText(automoveSettingsViewModel.currentStopBarcode)
                currentPowerText.setText(automoveSettingsViewModel.currentPower)
                tur90Text.setText(automoveSettingsViewModel.currentTurn90)
            }
            return@setOnTouchListener false
        }
        //Ki
        val kiWatcher = object : TextWatcher {
            override fun beforeTextChanged(sequence: CharSequence?, start: Int, count: Int, after: Int) {
                //
            }

            override fun onTextChanged(sequence: CharSequence?, start: Int, before: Int, count: Int) {
                automoveSettingsViewModel.currentKi = sequence.toString()
                automoveSettingsViewModel.isChangedKi = true
            }

            override fun afterTextChanged(sequence: Editable?) {
                //
            }
        }
        kiEditText = view.findViewById(R.id.KiData)
        kiEditText.setText(automoveSettingsViewModel.currentKi)
        kiEditText.addTextChangedListener(kiWatcher)
        //Kd
        val kdWatcher = object : TextWatcher {
            override fun beforeTextChanged(sequence: CharSequence?, start: Int, count: Int, after: Int) {
                //
            }

            override fun onTextChanged(sequence: CharSequence?, start: Int, before: Int, count: Int) {
                automoveSettingsViewModel.currentKd = sequence.toString()
                automoveSettingsViewModel.isChangedKd = true
            }

            override fun afterTextChanged(sequence: Editable?) {
                //
            }
        }
        kdEditText = view.findViewById(R.id.KdData)
        kdEditText.setText(automoveSettingsViewModel.currentKd)
        kdEditText.addTextChangedListener(kdWatcher)
        //Kp
        val kpWatcher = object : TextWatcher {
            override fun beforeTextChanged(sequence: CharSequence?, start: Int, count: Int, after: Int) {
                //
            }

            override fun onTextChanged(sequence: CharSequence?, start: Int, before: Int, count: Int) {
                automoveSettingsViewModel.currentKp = sequence.toString()
                automoveSettingsViewModel.isChangedKp = true
            }

            override fun afterTextChanged(sequence: Editable?) {
                //
            }
        }
        kpEditText = view.findViewById(R.id.KpData)
        kpEditText.setText(automoveSettingsViewModel.currentKp)
        kpEditText.addTextChangedListener(kpWatcher)
        // forward barcode
        val forwardBarcodeWatcher = object : TextWatcher {
            override fun beforeTextChanged(sequence: CharSequence?, start: Int, count: Int, after: Int) {
                //
            }

            override fun onTextChanged(sequence: CharSequence?, start: Int, before: Int, count: Int) {
                automoveSettingsViewModel.currentForwardBarcode = sequence.toString()
                automoveSettingsViewModel.isChangedForwardBarcode = true
            }

            override fun afterTextChanged(sequence: Editable?) {
                //
            }
        }
        forwardBarcodeText = view.findViewById(R.id.forwardbarcodeData)
        forwardBarcodeText.setText(automoveSettingsViewModel.currentForwardBarcode)
        forwardBarcodeText.addTextChangedListener(forwardBarcodeWatcher)

        // backward barcode
        val backwardBarcodeWatcher = object : TextWatcher {
            override fun beforeTextChanged(sequence: CharSequence?, start: Int, count: Int, after: Int) {
                //
            }

            override fun onTextChanged(sequence: CharSequence?, start: Int, before: Int, count: Int) {
                automoveSettingsViewModel.currentBackwardBarcode = sequence.toString()
                automoveSettingsViewModel.isChangedBackwardBarcode = true
            }

            override fun afterTextChanged(sequence: Editable?) {
                //
            }
        }
        backwardBarcodeText = view.findViewById(R.id.backwardbarcodeData)
        backwardBarcodeText.setText(automoveSettingsViewModel.currentBackwardBarcode)
        backwardBarcodeText.addTextChangedListener(backwardBarcodeWatcher)

        // left barcode
        val leftBarcodeWatcher = object : TextWatcher {
            override fun beforeTextChanged(sequence: CharSequence?, start: Int, count: Int, after: Int) {
                //
            }

            override fun onTextChanged(sequence: CharSequence?, start: Int, before: Int, count: Int) {
                automoveSettingsViewModel.currentLeftBarcode = sequence.toString()
                automoveSettingsViewModel.isChangedLeftBarcode = true
            }

            override fun afterTextChanged(sequence: Editable?) {
                //
            }
        }
        leftBarcodeText = view.findViewById(R.id.leftbarcodeData)
        leftBarcodeText.setText(automoveSettingsViewModel.currentLeftBarcode)
        leftBarcodeText.addTextChangedListener(leftBarcodeWatcher)

        // right barcode
        val rightBarcodeWatcher = object : TextWatcher {
            override fun beforeTextChanged(sequence: CharSequence?, start: Int, count: Int, after: Int) {
                //
            }

            override fun onTextChanged(sequence: CharSequence?, start: Int, before: Int, count: Int) {
                automoveSettingsViewModel.currentRightBarcode = sequence.toString()
                automoveSettingsViewModel.isChangedRightBarcode = true
            }

            override fun afterTextChanged(sequence: Editable?) {
                //
            }
        }
        rightBarcodeText = view.findViewById(R.id.rightbarcodeData)
        rightBarcodeText.setText(automoveSettingsViewModel.currentRightBarcode)
        rightBarcodeText.addTextChangedListener(rightBarcodeWatcher)

        // stop barcode
        val stopBarcodeWatcher = object : TextWatcher {
            override fun beforeTextChanged(sequence: CharSequence?, start: Int, count: Int, after: Int) {
                //
            }

            override fun onTextChanged(sequence: CharSequence?, start: Int, before: Int, count: Int) {
                automoveSettingsViewModel.currentStopBarcode = sequence.toString()
                automoveSettingsViewModel.isChangedStopBarcode = true
            }

            override fun afterTextChanged(sequence: Editable?) {
                //
            }
        }
        stopBarcodeText = view.findViewById(R.id.stopbarcodeData)
        stopBarcodeText.setText(automoveSettingsViewModel.currentStopBarcode)
        stopBarcodeText.addTextChangedListener(stopBarcodeWatcher)

        // current power
        val currentPowerWatcher = object : TextWatcher {
            override fun beforeTextChanged(sequence: CharSequence?, start: Int, count: Int, after: Int) {
                //
            }

            override fun onTextChanged(sequence: CharSequence?, start: Int, before: Int, count: Int) {
                automoveSettingsViewModel.currentPower = sequence.toString()
                automoveSettingsViewModel.isChangedCurrentPower = true
            }

            override fun afterTextChanged(sequence: Editable?) {
                //
            }
        }
        currentPowerText = view.findViewById(R.id.currentPowerData)
        currentPowerText.setText(automoveSettingsViewModel.currentPower)
        currentPowerText.addTextChangedListener(currentPowerWatcher)
        // turn90
        val turn90Watcher = object : TextWatcher {
            override fun beforeTextChanged(sequence: CharSequence?, start: Int, count: Int, after: Int) {
                //
            }

            override fun onTextChanged(sequence: CharSequence?, start: Int, before: Int, count: Int) {
                automoveSettingsViewModel.currentTurn90 = sequence.toString()
                automoveSettingsViewModel.isChangedTurn90 = true
            }

            override fun afterTextChanged(sequence: Editable?) {
                //
            }
        }
        tur90Text = view.findViewById(R.id.turn90Data)
        tur90Text.setText(automoveSettingsViewModel.currentTurn90)
        tur90Text.addTextChangedListener(turn90Watcher)

        lampOnOffSwitch = view.findViewById(R.id.lampOnOff)
        lampOnOffSwitch.setOnCheckedChangeListener {
                _, isChecked -> automoveSettingsViewModel.currentLampStatus = isChecked
            automoveSettingsViewModel.isChangedLamp = true
        }
        typeSwitch = view.findViewById(R.id.barcode_line)
        typeSwitch.setOnCheckedChangeListener {
                _, isChecked -> automoveSettingsViewModel.currentType = isChecked
            automoveSettingsViewModel.isChangedType= true
        }
        return view
    }
    private fun sendOneWayCommandToDroid(message : String, hasAck : Boolean = true) : Boolean = runBlocking {
        if (automoveSettingsViewModel.connectionType == ConnectionType.BLE && validateBleSocketConnection(automoveSettingsViewModel.bleSocket)) {
            var job = GlobalScope.launch {
                try {
                    val outputStreamWriter =
                        OutputStreamWriter(automoveSettingsViewModel.bleSocket?.getOutputStream())
                    val inputStreamReader =
                        BufferedReader(InputStreamReader(automoveSettingsViewModel.bleSocket?.getInputStream()))
                    //send data
                    outputStreamWriter.write(message)
                    outputStreamWriter.flush()
                    if (hasAck) {
                        val status = inputStreamReader.readLine()
                        Log.d(TAG, "s=$status")
                    }
                } catch (e : Exception) {
                    automoveSettingsViewModel.bleSocket?.close()
                    automoveSettingsViewModel.bleSocket = null
                    automoveSettingsViewModel.connectionType = ConnectionType.NONE
                    Log.e(TAG, e.toString())
                }
            }
            job.join()
            return@runBlocking true
        } else if (automoveSettingsViewModel.connectionType == ConnectionType.NONE) {
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
        if (automoveSettingsViewModel.connectionType == ConnectionType.BLE && validateBleSocketConnection(automoveSettingsViewModel.bleSocket)) {
            var job = GlobalScope.launch {
                try {
                    val outputStreamWriter =
                        OutputStreamWriter(automoveSettingsViewModel.bleSocket?.getOutputStream())
                    val inputStreamReader =
                        BufferedReader(InputStreamReader(automoveSettingsViewModel.bleSocket?.getInputStream()))
                    //send data
                    //Kd
                    outputStreamWriter.write("D#")
                    outputStreamWriter.flush()
                    var status = inputStreamReader.readLine()
                    automoveSettingsViewModel.currentKd = status
                    automoveSettingsViewModel.isChangedKd = false
                    Log.d(TAG, "s=$status")
                    //Kp
                    outputStreamWriter.write("P#")
                    outputStreamWriter.flush()
                    status = inputStreamReader.readLine()
                    automoveSettingsViewModel.currentKp = status
                    automoveSettingsViewModel.isChangedKp = false
                    Log.d(TAG, "s=$status")
                    //Ki
                    outputStreamWriter.write("I#")
                    outputStreamWriter.flush()
                    status = inputStreamReader.readLine()
                    automoveSettingsViewModel.currentKi = status
                    automoveSettingsViewModel.isChangedKi = false
                    Log.d(TAG, "s=$status")
                    //forward barcode
                    outputStreamWriter.write("Gf#")
                    outputStreamWriter.flush()
                    status = inputStreamReader.readLine()
                    automoveSettingsViewModel.currentForwardBarcode = status
                    automoveSettingsViewModel.isChangedForwardBarcode = false
                    Log.d(TAG, "s=$status")
                    //backward barcode
                    outputStreamWriter.write("Gb#")
                    outputStreamWriter.flush()
                    status = inputStreamReader.readLine()
                    automoveSettingsViewModel.currentBackwardBarcode = status
                    automoveSettingsViewModel.isChangedBackwardBarcode = false
                    Log.d(TAG, "s=$status")
                    //left barcode
                    outputStreamWriter.write("Gl#")
                    outputStreamWriter.flush()
                    status = inputStreamReader.readLine()
                    automoveSettingsViewModel.currentLeftBarcode = status
                    automoveSettingsViewModel.isChangedLeftBarcode = false
                    Log.d(TAG, "s=$status")
                    //right barcode
                    outputStreamWriter.write("Gr#")
                    outputStreamWriter.flush()
                    status = inputStreamReader.readLine()
                    automoveSettingsViewModel.currentRightBarcode = status
                    automoveSettingsViewModel.isChangedRightBarcode = false
                    Log.d(TAG, "s=$status")
                    //stop barcode
                    outputStreamWriter.write("Gs#")
                    outputStreamWriter.flush()
                    status = inputStreamReader.readLine()
                    automoveSettingsViewModel.currentStopBarcode = status
                    automoveSettingsViewModel.isChangedStopBarcode = false
                    Log.d(TAG, "s=$status")
                    //current power
                    outputStreamWriter.write("v#")
                    outputStreamWriter.flush()
                    status = inputStreamReader.readLine()
                    automoveSettingsViewModel.currentPower = status
                    automoveSettingsViewModel.isChangedCurrentPower = false
                    Log.d(TAG, "s=$status")
                    //turn90
                    outputStreamWriter.write("t#")
                    outputStreamWriter.flush()
                    status = inputStreamReader.readLine()
                    automoveSettingsViewModel.currentTurn90 = status
                    automoveSettingsViewModel.isChangedTurn90 = false
                    Log.d(TAG, "s=$status")
                } catch (e : Exception) {
                    automoveSettingsViewModel.bleSocket?.close()
                    automoveSettingsViewModel.bleSocket = null
                    automoveSettingsViewModel.connectionType = ConnectionType.NONE
                    Log.e(TAG, e.toString())
                }
            }
            job.join()
            return@runBlocking true
        } else if (automoveSettingsViewModel.connectionType == ConnectionType.NONE) {
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
        fun newInstance(): AutomoveControlFragment {
            return AutomoveControlFragment()
        }
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