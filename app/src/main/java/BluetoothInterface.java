/**
 * Created by Clemens on 12.12.2017.
 */

//package BluetoothInterface;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.app.Activity;

/**
* Activity for scanning and displaying available BLE devices.
*/
public class BluetoothInterface extends Activity
{

    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
    private final static int REQUEST_ENABLE_BT = 1;

    public void init()
    {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device doesn't support Bluetooth
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    public void write() {


    }
}