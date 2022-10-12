package io.github.gdimitriu.trackingcontrolcenter

import android.bluetooth.BluetoothSocket
import android.util.Log
import androidx.lifecycle.ViewModel

private const val TAG = "TrackingSettingsViewModel"
class TrackingSettingsViewModel : ViewModel()  {
    var isChanged : Boolean

    var bleSocket: BluetoothSocket?

    var connectionType : ConnectionType

    var currentKp : String
    var isChangedKp : Boolean

    var currentKd : String
    var isChangedKd : Boolean

    var currentKi : String
    var isChangedKi : Boolean

    var currentTracking : Boolean
    var isChangedTracking : Boolean

    var currentLampStatus : Boolean
    var isChangedLamp : Boolean

    var currentEnginePanStatus : Boolean
    var isChangedEnginePan : Boolean

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
        currentTracking = false
        isChangedTracking = true
        currentLampStatus = false
        isChangedLamp = true
        currentEnginePanStatus = false
        isChangedEnginePan = true
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