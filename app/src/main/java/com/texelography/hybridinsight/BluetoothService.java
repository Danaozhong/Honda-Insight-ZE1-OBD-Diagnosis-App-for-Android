package com.texelography.hybridinsight;


import android.app.Service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import android.os.Binder;
import android.os.IBinder;

/**
 * Created by Clemens on 20.01.2018.
 */

public class BluetoothService extends Service  {
    private final static String TAG = BluetoothService.class.getSimpleName();
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothGatt mBluetoothGatt;
    //private BluetoothGattCharacteristic mBluetoothRxCommandCharacteristic;
    //private BluetoothGattCharacteristic mBluetoothTxCommandCharacteristic;

    /* List of Characteristics that have the read property */
    private  ArrayList<BluetoothGattCharacteristic> mBLEReadCharacteristics;
    private  ArrayList<String> mBLEReadCharacteristicsUUID;

    /* List of Characteristics that have the write property */
    private ArrayList<BluetoothGattCharacteristic> mBLEWriteCharacteristics;
    private ArrayList<String> mBLEWriteCharacteristicsUUID;


    private boolean mScanning;
    private Handler mHandler;

    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";
    public final static String EXTRA_SENDER_UUID =
            "com.example.bluetooth.le.EXTRA_SENDER_UUID";

    private String mBluetoothDeviceAddress;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback()
            {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,  byte[] scanRecord)
                {
                    final String deviceName = device.getName();
                    Log.d("Bluetooth", "device found: " + deviceName);
                    if (deviceName.equals("OBDIIC&C"))
                    {
                        Log.d("Bluetooth", "OBDIIC&C found, abort scanning!");
                        mBluetoothDevice = device;

                        scanLeDevice(false);
                        connect("Test");
                        //connectToDevice();
                    }
                    // mLeDeviceListAdapter.addDevice(device);
                    //mLeDeviceListAdapter.notifyDataSetChanged();
                }
            };



    public boolean readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w("BLE", "BluetoothAdapter not initialized");
            return false;
        }
        return mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }


    public int sendBLEBytes(String uuid, byte[] data)
    {
        if (null == mBLEWriteCharacteristicsUUID)
        {
            return -1;
        }
        Log.w("BLE Transmit", "Compare src " + uuid.toLowerCase());
        for(int i = 0; i < mBLEWriteCharacteristicsUUID.size(); ++i)
        {
            Log.w("BLE Transmit", "Compare to " + mBLEWriteCharacteristicsUUID.get(i).toLowerCase());
            if (mBLEWriteCharacteristicsUUID.get(i).toLowerCase().equals(uuid.toLowerCase()))
            {
                mBLEWriteCharacteristics.get(i).setValue(data);
                mBluetoothGatt.writeCharacteristic(mBLEWriteCharacteristics.get(i));
                break;
            }
        }
        return 0;

    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public boolean setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }
        return mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
    /*
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E"));
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);
        */

        /*
        // This is specific to Heart Rate Measurement.
        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
        */
    }
/*

BLECharacteristicList ble_interface_characteristics =
{
		/*
	{0, "6E400002-B5A3-F393-E0A9-E50E24DCCA9E", BLECharacteristic::PROPERTY_WRITE, new MyCallbacks()},

{BLE_CHARACTERISTIC_ID_RECEIVE_COMMANDS, 	    "6E400002-B5A3-F393-E0A9-E50E24DCCA2E", BLECharacteristic::PROPERTY_WRITE, new MyCallbacks()},


    {BLE_CHARACTERISTIC_ID_SEND_COMMANDS, 			"6E400002-B5A3-F393-E0A9-E50E24DC2A2F", BLECharacteristic::PROPERTY_NOTIFY, NULL},


    {BLE_CHARACTERISTIC_ID_SEND_OBD_DATA, 			"6E400002-B5A3-F393-E0A9-E50E24DCCA2F", BLECharacteristic::PROPERTY_NOTIFY, NULL},


    {BLE_CHARACTERISTIC_ID_SEND_OBD_ERROR_CODES, 	"6E400002-B5A3-F393-E0A9-E50E24DCCA2A", BLECharacteristic::PROPERTY_NOTIFY, NULL},
};



 */

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = "Unknown service";
        String unknownCharaString = "Unknown characteristic"; //getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        //mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            //currentServiceData.put(
            //        LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            //currentServiceData.put(LIST_UUID, uuid);
            //gattServiceData.add(currentServiceData);
            Log.d("Bluetooth", "Serice UUID: " + uuid);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                Log.d("Bluetooth", "Characteristics UUID: " + uuid);

                /* Discover characteristic properties */
                final int charaProp = gattCharacteristic.getProperties();
                Log.d("Bluetooth", "Characteristic Properties: " + Integer.toString(charaProp));


                if ((charaProp & BluetoothGattCharacteristic.PROPERTY_READ) > 0)
                {
                    Log.d("Bluetooth", "This is a read characteristic!");
                    if (!readCharacteristic(gattCharacteristic))
                    {
                        Log.d("Bluetooth", "Reading of the characteristic failed!");
                    } else
                    {
                        /* Ignore Read characteristics */
                    }
                    Log.d("Bluetooth", "Found OBDII Read Characteristic: " + uuid);
                }
                if ((charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0)
                {
                    Log.d("Bluetooth", "This characteristic can be written to!");
                    mBluetoothGatt.writeCharacteristic(gattCharacteristic);

                    if (null == mBLEWriteCharacteristics)
                    {
                        mBLEWriteCharacteristics = new ArrayList<BluetoothGattCharacteristic>();
                        mBLEWriteCharacteristicsUUID = new ArrayList<String>();
                    }

                    mBLEWriteCharacteristics.add(gattCharacteristic);
                    mBLEWriteCharacteristicsUUID.add(uuid);
                    Log.d("Bluetooth", "Found OBDII Write Characteristic: " + uuid);

                }
                if ((charaProp & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0)
                {
                    /*
                    Log.d("Bluetooth", "This is a notify characteristic, turning ON!");
                    if (!setCharacteristicNotification(gattCharacteristic, true))
                    {
                        Log.d("Bluetooth", "Turning on notification failed!");
                    }
                    UUID CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dc2a2f"); //"00002902-0000-1000-8000-00805f9b34fb");

                    //"00002902-0000-1000-8000-00805f9b34fb";
                    BluetoothGattDescriptor descriptor = gattCharacteristic.getDescriptor(
                            CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID);
                    if (null == descriptor)
                    {

                        Log.d("Bluetooth", "Getting the descriptor failed.");
                    } else
                    {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        if (!mBluetoothGatt.writeDescriptor(descriptor))
                        {
                            Log.d("Bluetooth", "Writing the notification descriptor failed!");
                        }
                    }
    */
                    Log.d("Bluetooth", "Found OBDII Notfiy Characteristic: " + uuid);

                    if (null == mBLEReadCharacteristics)
                    {
                        mBLEReadCharacteristics = new ArrayList<BluetoothGattCharacteristic>();
                        mBLEReadCharacteristicsUUID = new ArrayList<String>();
                    }
                    mBLEReadCharacteristics.add(gattCharacteristic);
                    mBLEReadCharacteristicsUUID.add(uuid);
                }
                /*
                currentCharaData.put(
                        LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
                */
            }
            //mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

        readRequest();
    }

    public void readRequest()
    {
        if (mBLEReadCharacteristics == null)
        {
            Log.d("Bluetooth", "No read characteristics found yet.");
            return;
        }
        for (int i = 0; i != mBLEReadCharacteristics.size(); ++i)
        {
            /*
            if ((charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0)
            {
                Log.d("Bluetooth", "This characteristic can be written to!");
                mBluetoothGatt.writeCharacteristic(mBLEReadCharacteristics.get(i));
                //gattCharacteristic.writeD
                //mBluetoothGatt.readCharacteristic(characteristic);
            }
            if ((charaProp & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0)
            {
            */
                Log.d("Bluetooth", "This is a notify characteristic, turning ON!");
                if (!setCharacteristicNotification(mBLEReadCharacteristics.get(i), true))
                {
                    Log.d("Bluetooth", "Turning on notification failed!");
                }

                UUID CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dc2a2f"); //"00002902-0000-1000-8000-00805f9b34fb");

                //"00002902-0000-1000-8000-00805f9b34fb";
                BluetoothGattDescriptor descriptor = mBLEReadCharacteristics.get(i).getDescriptor(
                        CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID);
                if (null == descriptor)
                {

                    Log.d("Bluetooth", "Getting the descriptor failed.");
                } else
                {
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    if (!mBluetoothGatt.writeDescriptor(descriptor))
                    {
                        Log.d("Bluetooth", "Writing the notification descriptor failed!");
                    }
                }

                //BluetoothGattDescriptor descriptor = mBluetoothRxCommandCharacteristic.getDescriptor(CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID);
            //}
        }
    }




    private void broadcastUpdate(final String action) {
        Log.d("Bluetooth", "Received Update!");
        final Intent intent = new Intent(action);
        sendBroadcast(intent);

        int broadcastType = 99;

        /* To be moved */
        if (BluetoothService.ACTION_GATT_CONNECTED.equals(action)) {
            broadcastType = 0;
            //updateConnectionState(R.string.connected);
            //invalidateOptionsMenu();
        } else if (BluetoothService.ACTION_GATT_DISCONNECTED.equals(action)) {
            broadcastType = 1;
            //mConnected = false;
            //updateConnectionState(R.string.disconnected);
            //invalidateOptionsMenu();
            //clearUI();
        } else if (BluetoothService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
            broadcastType = 2;
            // Show all the supported services and characteristics on the user interface.
            List<BluetoothGattService> services = getSupportedGattServices();
            Log.d("Bluetooth", "Services received!");
            displayGattServices(services);

        } else if (BluetoothService.ACTION_DATA_AVAILABLE.equals(action)) {
            final String data = intent.getStringExtra(BluetoothService.EXTRA_DATA);
            Log.d("Bluetooth", "Data received: " + data);

        }
        String uuid = "Test";
        byte[] data = "Test".getBytes();
        android_on_broadcast_receive(broadcastType, uuid.getBytes(), uuid.length(), data, data.length);


    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic)
    {
        final Intent intent = new Intent(action);

        /* ACTION_DATA_AVAILABLE */
        UUID mUUIDOBDIICnC = characteristic.getUuid();
        Log.d("Bluetooth", "UUID is " + mUUIDOBDIICnC.toString());
        // This is special handling for the Heart Rate Measurement profile. Data
        // parsing is carried out as per profile specifications.
        int flag = characteristic.getProperties();


        int broadcastType = 99;


        Log.d("Bluetooth", String.format("Found properties: %s", flag));

        if (BluetoothService.ACTION_DATA_AVAILABLE.equals(action))
        {
            broadcastType = 3; // ACTION_DATA_AVAILABLE

            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0)
            {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for (byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));
                String value = new String(data) + "\n" + stringBuilder.toString();
                intent.putExtra(EXTRA_DATA, data);
                intent.putExtra(EXTRA_SENDER_UUID, characteristic.getUuid().toString());

                Log.d("Bluetooth", "Data that has been received of length " + data.length);
                //sendBytes("dummy", "Test1235325".getBytes());
                //sendCommand();

                String uuid = mUUIDOBDIICnC.toString();
                android_on_broadcast_receive(broadcastType, uuid.getBytes(), uuid.length(), data, data.length);
            }
            sendBroadcast(intent);
        }


    }

    // Various callback methods defined by the BLE API.
    private final BluetoothGattCallback mGattCallback =
            new BluetoothGattCallback()
            {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                                    int newState)
                {
                    String intentAction;
                    if (newState == BluetoothProfile.STATE_CONNECTED)
                    {
                        intentAction = ACTION_GATT_CONNECTED;
                        mConnectionState = STATE_CONNECTED;
                        broadcastUpdate(intentAction);
                        Log.i("Bluetooth LE", "Connected to GATT server.");
                        Log.i("Bluetooth LE", "Attempting to start service discovery:" +
                                mBluetoothGatt.discoverServices());

                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED)
                    {
                        intentAction = ACTION_GATT_DISCONNECTED;
                        mConnectionState = STATE_DISCONNECTED;
                        Log.i("Bluetooth LE", "Disconnected from GATT server.");
                        broadcastUpdate(intentAction);
                    }
                }

                @Override
                // New services discovered
                public void onServicesDiscovered(BluetoothGatt gatt, int status)
                {
                    if (status == BluetoothGatt.GATT_SUCCESS)
                    {
                        broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                    } else
                    {
                        Log.w("OBDIIC&C", "onServicesDiscovered received: " + status);
                    }
                }

                @Override
                // Result of a characteristic read operation
                public void onCharacteristicRead(BluetoothGatt gatt,
                                                 BluetoothGattCharacteristic characteristic,
                                                 int status)
                {
                    Log.d("Bluetooth", "Received characteristics!");
                    if (status == BluetoothGatt.GATT_SUCCESS)
                    {
                        broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                    }
                }

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt,
                                                    BluetoothGattCharacteristic characteristic) {
                    broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                }

            };
                private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            /*mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);
            */

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    private Handler handler;

    @Override
    public void onCreate() {
        super.onCreate();

        /* Call the native C++ code to inform it about the presence of this class */
        android_on_create_bluetooth_service();
        // let's create a thread pool with five threads
        handler = new Handler();
    }
/*
    @Override
    protected void onHandleIntent(Intent workIntent)
    {
        // Gets data from the incoming Intent
        String dataString = workIntent.getDataString();
        Log.d("Bluetooth", "bluetooth communication thread started!");
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


        scanLeDevice(true);


        while(true)
        {

            final int charaProp = gattCharacteristic.getProperties();

            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0)
            {
                Log.d("Bluetooth", "This is a read characteristic!");
                readCharacteristic(gattCharacteristic);
            }
            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                Log.d("Bluetooth", "This is a notify characteristic, turning ON!");
                setCharacteristicNotification(
                        gattCharacteristic, true);
            }
        }

    }
*/

    public class LocalBinder extends Binder {
        BluetoothService getService() {
            return BluetoothService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }


    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }


        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice("30:AE:A4:38:81:76");

        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        Log.d("Bluetooth", "Disconnect!!!");
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        Log.d("Bluetooth", "Shutting down!!!");
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }


    public native void android_on_create_bluetooth_service();
    public native void android_on_broadcast_receive(int broadcast_type, byte[] uuid, int uuid_length, byte[] value, int length);

}