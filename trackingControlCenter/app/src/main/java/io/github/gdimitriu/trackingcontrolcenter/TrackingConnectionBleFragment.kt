package io.github.gdimitriu.trackingcontrolcenter

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*

private const val TAG = "TrackingConnectionBle"

@SuppressLint("MissingPermission")
class TrackingConnectionBleFragment : Fragment() {

    private val droidSettingsViewModel: TrackingSettingsViewModel by activityViewModels()
    private var bluetoothAdapter: BluetoothAdapter? = null
    private lateinit var bleRecyclerView: RecyclerView
    private var adapter: BleAdapter? = BleAdapter(emptyList())
    private val MY_UUID = UUID(1, 4)

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        if (pairedDevices != null) {
            updateUI(pairedDevices.toList())
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_tracking_connection_ble, container, false)
        bleRecyclerView = view.findViewById(R.id.connection_ble_recycler_view) as RecyclerView
        bleRecyclerView.layoutManager = LinearLayoutManager(context)
        bleRecyclerView.adapter = adapter
        val bluetoothManager =
            context?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter != null) {
            if (!bluetoothAdapter!!.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, 1)
            } else {
                val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
                if (pairedDevices != null) {
                    updateUI(pairedDevices.toList())
                }
            }
        }
        return view
    }

    private fun updateUI(devices: List<BluetoothDevice>) {
        adapter = BleAdapter(devices)
        bleRecyclerView.adapter = adapter
    }

    companion object {
        fun newInstance(): TrackingConnectionBleFragment {
            return TrackingConnectionBleFragment()
        }
    }

    private inner class BleHolder(view: View) : RecyclerView.ViewHolder(view),
        View.OnClickListener {
        private lateinit var device: BluetoothDevice

        private val nameTextView: TextView = itemView.findViewById(R.id.ble_name)
        private val macTextView: TextView = itemView.findViewById(R.id.ble_mac)

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(device: BluetoothDevice) {
            this.device = device
            nameTextView.text = device.name
            macTextView.text = device.address
        }

        private fun connectToDroid() = runBlocking {
            var gotException: Exception? = null
            var job = GlobalScope.launch {
                if (bluetoothAdapter?.isDiscovering == true) {
                    bluetoothAdapter?.cancelDiscovery()
                }
                for (item in device.uuids) {
                    Log.d(TAG, item.uuid.toString())
                }
                try {
                    val socket: BluetoothSocket =
                        //device.createRfcommSocketToServiceRecord(item.uuid)
                        device.createInsecureRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
                    socket.connect()
                    Log.d(TAG, "connected ")
                    droidSettingsViewModel.bleSocket = socket
                    droidSettingsViewModel.connectionType = ConnectionType.BLE
                } catch (e: Exception) {
                    Log.d(TAG, "failed connection ")
                    gotException = e
                    droidSettingsViewModel.bleSocket = null
                }
            }
            job.join()
            if (gotException != null) {
                val builder: AlertDialog.Builder? = activity?.let {
                    AlertDialog.Builder(it)
                }
                builder?.setMessage(gotException!!.localizedMessage)
                    ?.setTitle("Connection failed !")
                val dialog: AlertDialog? = builder?.create()
                dialog?.show()
            } else {
                val builder: AlertDialog.Builder? = activity?.let {
                    AlertDialog.Builder(it)
                }
                builder?.setMessage("Droid is connected on ble !")
                    ?.setTitle("Connection succeeded !")
                val dialog: AlertDialog? = builder?.create()
                dialog?.show()
            }
        }

        override fun onClick(v: View) {
            connectToDroid()
        }
    }

    private inner class BleAdapter(var devices: List<BluetoothDevice>) :
        RecyclerView.Adapter<BleHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BleHolder {
            val view = layoutInflater.inflate(R.layout.connection_ble_item, parent, false)
            return BleHolder(view)
        }

        override fun onBindViewHolder(holder: BleHolder, position: Int) {
            val device = devices[position]
            holder.bind(device)
        }

        override fun getItemCount(): Int {
            return devices.size
        }

    }
}