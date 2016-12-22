package com.innovate365.lorenzo.firefly;

import android.app.AlertDialog;
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.innovate365.lorenzo.bluetoothlegatt.BluetoothLeService;

import java.util.UUID;

public class MasterActivity extends AppCompatActivity {
    private BluetoothGatt[] mGatts = new BluetoothGatt[2];
    BluetoothDevice[] mDevices = new BluetoothDevice[2];
    private ToggleButton[] mToggleButtons = new ToggleButton[2];
    private TextView mTextView;

    private EditText mIntervalEdit;
    private Button mButtonApply;

    public static String LED1_ON = "LED1 ON";
    public static String LED1_OFF = "LED1 OFF";

    public static String LED2_ON = "LED2 ON";
    public static String LED2_OFF = "LED2 OFF";

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    public final String TAG = this.getClass().getName();

    private BluetoothAdapter mBluetoothAdapter;

    public final static String ACTION_GATT_CONNECTED            = "ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED         = "ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED  = "ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE            = "ACTION_DATA_AVAILABLE";
    public final static String ACTION_BATTERY_READ              = "ACTION_BATTERY_READ";

    private static final UUID Custom_Service_UUID = UUID.fromString("edfec62e-9910-0bac-5241-d8bda6932a2f");
    private static final UUID Led_State_UUID = UUID.fromString("5a87b4ef-3bfa-76a8-e642-92933c31434f");
    private static final UUID Control_Point_UUID = UUID.fromString("2d86686a-53dc-25b3-0c4a-f0e10c8dee20");

    private boolean mScanning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if(this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect BlueSwtich");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                });
                builder.show();
            }
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "BikeBle Demo", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        mTextView = (TextView)findViewById(R.id.textView);
        mToggleButtons[0] = (ToggleButton)findViewById(R.id.toggleButton);
        mToggleButtons[1] = (ToggleButton)findViewById(R.id.toggleButton2);

        mToggleButtons[0].setChecked(true);
        mToggleButtons[1].setChecked(true);
        mToggleButtons[0].setEnabled(false);
        mToggleButtons[1].setEnabled(false);


        mIntervalEdit = (EditText)findViewById(R.id.editBlinkInterval);
        mButtonApply = (Button)findViewById(R.id.buttonApply);

        mIntervalEdit.setEnabled(false);
        mButtonApply.setEnabled(false);

        mButtonApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                writeControlValue();
            }
        });

        mToggleButtons[0].setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                writeLedState(0,!isChecked);
            }
        });

        mToggleButtons[1].setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                writeLedState(1,!isChecked);
            }
        });


        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if(!mBluetoothAdapter.isEnabled())
        {
            Intent intentBtEnabled = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            int REQUEST_ENABLE_BT = 2;
            startActivityForResult(intentBtEnabled, REQUEST_ENABLE_BT);
        }
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mDevices[0] = mDevices[1] = null;

        scanLeDevice(true);

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == PERMISSION_REQUEST_COARSE_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "coarse location permission granted");
            } else {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Functionality limited");
                builder.setMessage("Since location access has not been granted, this app will not be able to discover blueswitches when in the background");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {

                    }
                });
                builder.show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            if(!mScanning) {
                mBluetoothAdapter.startLeScan(mLeScanCallback);
                mScanning = true;
                mTextView.setText("Scanning...");
            }
        } else {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
        }
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, final byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(device.getName().equals("BikeBle"))
                            {
                                int i;
                                for(i = 0; i < 2; i++)
                                    if(mDevices[i] == device)
                                        return;

                                Log.d(TAG, "Name=" + device.getName() + ", ADDR=" + device.getAddress());

                                for(i = 0; i < 2; i++)
                                {
                                    if(mDevices[i] == null) {
                                        mDevices[i] = device;
                                        mGatts[i] = device.connectGatt(MasterActivity.this,false,mGattCallback);
                                        break;
                                    }
                                }
                                if(mDevices[0] != null && mDevices[1] != null)
                                    scanLeDevice(false);
                            }
                        }
                    });
                }
            };

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected to GATT server.");
                Log.i(TAG, "Attempting to start service discovery:" +
                        gatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from GATT server.");
                int i;
                for(i = 0; i < 2; i++) {
                    if (mGatts[i] == gatt) {

                        Intent intent = new Intent(ACTION_GATT_DISCONNECTED);
                        intent.putExtra("index",i);
                        sendBroadcast(intent);
                        break;
                    }
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                int i;
                Log.i(TAG, "onServicesDiscovered success");

                for(i = 0; i < 2; i++)
                {
                    if(mGatts[i] == gatt)
                    {
                        Intent intent = new Intent(ACTION_GATT_SERVICES_DISCOVERED);
                        intent.putExtra("index",i);
                        sendBroadcast(intent);
                        break;
                    }
                }
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {

            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {

        }
    };

    @Override
    protected void onDestroy() {
        unregisterReceiver(mGattUpdateReceiver);
        if(mScanning)
            scanLeDevice(false);
        if(mGatts[0] != null)
            mGatts[0].disconnect();
        if(mGatts[1] != null)
            mGatts[1].disconnect();

        super.onDestroy();
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if(ACTION_GATT_CONNECTED.equals(action)) {

            }
            else if (ACTION_GATT_DISCONNECTED.equals(action)) {
                int index = intent.getIntExtra("index",0);
                mDevices[index] = null;
                mGatts[index] = null;
                mToggleButtons[index].setEnabled(false);
                scanLeDevice(true);

                if(!mToggleButtons[0].isEnabled() && !mToggleButtons[1].isEnabled())
                {
                    mIntervalEdit.setEnabled(false);
                    mButtonApply.setEnabled(false);
                }
            }
            else if(ACTION_BATTERY_READ.equals(action)) {

            }
            else if(ACTION_GATT_SERVICES_DISCOVERED.equals(action))
            {
                int index = intent.getIntExtra("index",0);
                mToggleButtons[index].setEnabled(true);
                mIntervalEdit.setEnabled(true);
                mButtonApply.setEnabled(true);
                if(mToggleButtons[0].isEnabled() && mToggleButtons[1].isEnabled())
                    mTextView.setText("Scan finished");
            }

        }
    };
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_GATT_CONNECTED);
        intentFilter.addAction(ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(ACTION_DATA_AVAILABLE);
        intentFilter.addAction(ACTION_BATTERY_READ);
        intentFilter.addAction(ACTION_GATT_SERVICES_DISCOVERED);

        return intentFilter;
    }

    public void writeLedState(int index, boolean state)
    {
        BluetoothGattService mCustomSrvice;
        BluetoothGattCharacteristic mLedState;

        mCustomSrvice = mGatts[index].getService(Custom_Service_UUID);
        if(mCustomSrvice == null) {
            Log.d(TAG, "Custom service not found!");
            return;
        }

        mLedState = mCustomSrvice.getCharacteristic(Led_State_UUID);
        if(mLedState == null) {
            Log.d(TAG, "Battery level not found!");
            return;
        }
        Log.d(TAG, "write initiated");

        byte[] val = new byte[1];
        if(state)
            val[0] = 1;
        else
            val[0] = 0;
        mLedState.setValue(val);
        mGatts[index].writeCharacteristic(mLedState);
    }

    public void writeControlValue()
    {
        int index;
        BluetoothGattService mCustomSrvice;
        BluetoothGattCharacteristic mControlPoint;

        for(index = 0; index < 2; index++)
        {
            if(mGatts[index] == null)
                continue;

            mCustomSrvice = mGatts[index].getService(Custom_Service_UUID);
            if(mCustomSrvice == null) {
                Log.d(TAG, "Custom service not found!");
                return;
            }

            mControlPoint = mCustomSrvice.getCharacteristic(Control_Point_UUID);
            if(mControlPoint == null) {
                Log.d(TAG, "Battery level not found!");
                return;
            }
            Log.d(TAG, "write initiated");

            String s = mIntervalEdit.getText().toString();
            int n = Integer.parseInt(s);
            if(n >= 10000) {
                n = 10000;
                s = String.valueOf(n);
                mIntervalEdit.setText(s);
            }
            n = n / 10;
            byte[] val = new byte[2];
            val[0] = (byte)(n & 0xff);
            val[1] = (byte)( (n >> 8) & 0xff);
            mControlPoint.setValue(val);
            mGatts[index].writeCharacteristic(mControlPoint);
        }

    }


}
