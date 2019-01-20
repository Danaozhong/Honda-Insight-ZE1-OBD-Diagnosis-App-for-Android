package com.texelography.hybridinsight;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class SplashViewActivity extends Activity
{
    //private Timer timer;
    private TextView lblStatus;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native void android_on_create_form();


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        //Remove notification bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_splash_view);

        lblStatus = (TextView) findViewById(R.id.txtProgress);
        lblStatus.setText("Initializing BLE...");

        /* Start the CPP code */
        android_on_create_form();


        // Start BLE
        initializeBLE();

        //new PrefetchData().execute();
        //timer = new Timer();
        //    timer.schedule(new RemindTask(), 2*1000);

    }

    // Just for testing, again...
    class RemindTask extends TimerTask
    {
        public void run() {
            System.out.format("Time's up!%n");
            //timer.cancel(); //Terminate the timer thread

            Intent i = new Intent(SplashViewActivity.this, MainView.class);
            startActivity(i);

            // close this activity
            finish();
        }
    }


    // BLE Stuff starts here
    private final static int REQUEST_ENABLE_BT = 1;
    private BluetoothService mBluetoothService;
    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection()
    {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothService = ((BluetoothService.LocalBinder) service).getService();
            if (!mBluetoothService.initialize()) {
                Log.e("BT initialization", "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            lblStatus.setText("Searching OBDII Diagnosis device...");

            mBluetoothService.connect("Test123");

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothService = null;
        }
    };

    protected void initializeBLE()
    {
        lblStatus.setText("Requesting permission for location...");
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // No explanation needed, we can request the permission.

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    1234);
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // No explanation needed, we can request the permission.

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1234);
        }

        lblStatus.setText("Accessing bluetooth adapter...");

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null)
        {
            // Device doesn't support Bluetooth
            lblStatus.setText("Unfortunately, your device does not suuport BLE.");
            Log.e("BT initialization", "Devide does not support Bluetooth, app will not work properly!");
        }
        else
        {
            lblStatus.setText("Enable Bluetooth adapter...");
            if (!mBluetoothAdapter.isEnabled())
            {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }

            Intent gattServiceIntent = new Intent(this, BluetoothService.class);
            bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        }
    }



    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothService.ACTION_GATT_CONNECTED.equals(action))
            {
                lblStatus.setText("BLE connection established!");

                /* Actually, here it is too early to already switch to the main view. The BLE
                device might not authentificate, or send incorrect data. TODO create a callback from
                C++ code when the BLE authentification and handshake was complete. Only then switch
                to the main view
                 */
                Intent i = new Intent(SplashViewActivity.this, MainView.class);
                startActivity(i);

                // close this activity
                // Currently, not possible, would leak the BLE object. BLE object needs somehow kept
                //finish();

            } else if (BluetoothService.ACTION_GATT_DISCONNECTED.equals(action)) {
                //mConnected = false;
                //updateConnectionState(R.string.disconnected);
                //invalidateOptionsMenu();
                //clearUI();
            } else if (BluetoothService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                //displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothService.ACTION_DATA_AVAILABLE.equals(action)) {
                //displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                if (intent.hasExtra(BluetoothService.EXTRA_DATA))
                {
                    final byte[] data = intent.getByteArrayExtra(BluetoothService.EXTRA_DATA);
                    if (data != null && data.length > 0)
                    {
                        /* Forward received data to C++ */
                        //String string_data = new String(data);
                        //Log.d("Bluetooth", "Data received (length " + Integer.toString(data.length) +  " ), forwarning...");

                        /* Get the UUID */
                        //final String uuid = intent.getStringExtra(BluetoothService.EXTRA_SENDER_UUID);

                        //android_ble_data_received(uuid.getBytes(), uuid.length(), data, data.length);
                        //mBluetoothService.sendBytes("test", "test".getBytes());
                    } else
                    {
                        Log.d("Bluetooth", "Main window invalid data!");
                    }
                }
            }
            else
            {
                Log.d("Bluetooth", "Unknown broadcast received: " + action);
            }
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }
}


