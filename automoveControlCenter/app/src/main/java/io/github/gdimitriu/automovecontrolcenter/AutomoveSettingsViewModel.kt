package io.github.gdimitriu.automovecontrolcenter

import android.bluetooth.BluetoothSocket
import android.util.Log
import androidx.lifecycle.ViewModel
private const val TAG = "AutomoveSettingsViewModel"
class AutomoveSettingsViewModel : ViewModel()  {
    var isChanged : Boolean

    var bleSocket: BluetoothSocket?

    var connectionType : ConnectionType

    var currentKp : String
    var isChangedKp : Boolean

    var currentKd : String
    var isChangedKd : Boolean

    var currentKi : String
    var isChangedKi : Boolean

    var currentLampStatus : Boolean
    var isChangedLamp : Boolean

    var isChangedLeftBarcode : Boolean
    var currentLeftBarcode : String

    var isChangedRightBarcode : Boolean
    var currentRightBarcode : String

    var isChangedStopBarcode : Boolean
    var currentStopBarcode : String

    var isChangedForwardBarcode : Boolean
    var currentForwardBarcode : String

    var isChangedBackwardBarcode : Boolean
    var currentBackwardBarcode : String

    var isChangedTurn90 : Boolean
    var currentTurn90 : String

    var isChangedCurrentPower : Boolean
    var currentPower : String

    var isChangedType : Boolean
    var currentType : Boolean

    init {
        Log.d(TAG, "Initialized the model view")
        isChanged = true
        connectionType = ConnectionType.NONE
        bleSocket = null
        currentKp = "0"
        isChangedKp = false
        currentKd = "0"
        isChangedKd = false
        currentKi = "0"
        isChangedKi = false

        currentLampStatus = false
        isChangedLamp = true


        isChangedLeftBarcode = true
        isChangedRightBarcode = true
        isChangedStopBarcode = true
        isChangedForwardBarcode = true
        isChangedBackwardBarcode = true

        currentLeftBarcode = "0"
        currentRightBarcode = "5"
        currentForwardBarcode = "4"
        currentBackwardBarcode = "3"
        currentStopBarcode = "6"

        isChangedTurn90 = true
        currentTurn90 = "1000"

        isChangedCurrentPower = true
        currentPower = "255"

        isChangedType = true
        currentType = false
    }

    override fun onCleared() {
        Log.d(TAG,"Clearing and close the socket.")
        super.onCleared()
        bleSocket?.close()
    }

    fun closeSockets() {
        bleSocket?.close()
        connectionType = ConnectionType.NONE
    }
}