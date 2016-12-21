package com.innovate365.lorenzo.firefly;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.StringDef;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.innovate365.lorenzo.bluetoothlegatt.BluetoothLeService;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private BluetoothAdapter mBluetoothAdapter;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 10000;
    private Handler mHandler;
    private Handler mPairHandler;

    private boolean mScanning;

    private boolean mConnected;
    private boolean mGetBattery = false;

    private byte MfgData[];
    byte mSettingBytes[] = new byte[2];

    private LedConfig mLedConfig;
    private TextView txtViewState;
    private TextView txtViewBattery;

    ImageView imgRed, imgGreen, imgBlue;
    SeekBar mSeekBar;
    RadioGroup mRadioGroupLed, mRadioGroupDisco;
    LinearLayout mLayoutLamp;
    Button mButtonTest,mButtonPair,mButtonApply,mButtonScan;

    private BluetoothLeService mBluetoothLeService;
    private String mDeviceAddress;
    private BluetoothDevice mDevice = null;
    private boolean mDevicePaired = false;

    private int mConnectMode = BluetoothLeService.CONNECT_EVENT;

    public static final String TAG = "MainActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MfgData = new byte[32];

        mLedConfig = new LedConfig(this);
        mConnected = false;

        IntentFilter intent = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mPairReceiver, intent);

        txtViewState = (TextView)findViewById(R.id.lblState);
        imgRed = (ImageView)findViewById(R.id.img_lampr);
        imgGreen = (ImageView)findViewById(R.id.img_lampg);
        imgBlue = (ImageView)findViewById(R.id.img_lampb);
        mSeekBar = (SeekBar)findViewById(R.id.seekBar);
        mRadioGroupLed = (RadioGroup)findViewById(R.id.radioGroupLedTime);
        mRadioGroupDisco = (RadioGroup)findViewById(R.id.radioGroupDisco);
        mLayoutLamp = (LinearLayout)findViewById(R.id.layoutLamp);
        mButtonTest = (Button)findViewById(R.id.buttonTest);
        mButtonPair = (Button) findViewById(R.id.buttonPair);
        mButtonApply = (Button) findViewById(R.id.buttonApply);
        mButtonScan = (Button) findViewById(R.id.buttonScan);

        txtViewBattery = (TextView)findViewById(R.id.lblBattery);

        enableSettingView(false);
        mLayoutLamp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLedConfig.mLedColor = (mLedConfig.mLedColor + 1) % 3;
                updateRampState();
            }
        });

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mLedConfig.mBlinking = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        mRadioGroupLed.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                int radio_id;
                for(int i = 0; i < 3; i++)
                {
                    radio_id = mRadioGroupLed.getChildAt(i).getId();
                    if(radio_id == checkedId)
                    {
                        mLedConfig.mLedOnTime = i;
                        break;
                    }
                }
            }
        });

        mRadioGroupDisco.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                int radio_id;
                for(int i = 0; i < 2; i++)
                {
                    radio_id = mRadioGroupDisco.getChildAt(i).getId();
                    if(radio_id == checkedId)
                    {
                        mLedConfig.mDiscoMode = i;
                        break;
                    }
                }
            }
        });

        mButtonTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testEvent();
            }
        });

        mButtonPair.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pairFireFly();
            }
        });

        mButtonScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!mScanning)
                    scanLeDevice(true);
            }
        });

        mButtonApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyConfig();
            }
        });

        mHandler = new Handler();
        mPairHandler = new Handler();

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        loadConfig();
    }

    @Override
    protected void onResume() {
        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
        scanLeDevice(true);
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
        scanLeDevice(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        unregisterReceiver(mPairReceiver);
        mBluetoothLeService = null;
        mConnected = false;
    }


    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mDevice = null;
            mScanning = true;
            mButtonScan.setText("Scanning");
            enableButtons(false);
            mDeviceAddress = "";
            txtViewState.setText("None");
            txtViewBattery.setText("N/A");
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mButtonScan.setText("Scan");
            mScanning = false;
            enableButtons(true);
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    void updateRampState()
    {
        if(mLedConfig.mLedColor == 0)
        {
            imgRed.setVisibility(View.VISIBLE);
            imgGreen.setVisibility(View.GONE);
            imgBlue.setVisibility(View.GONE);
        }
        else if(mLedConfig.mLedColor == 1)
        {
            imgRed.setVisibility(View.GONE);
            imgGreen.setVisibility(View.VISIBLE);
            imgBlue.setVisibility(View.GONE);
        }
        else
        {
            imgRed.setVisibility(View.GONE);
            imgGreen.setVisibility(View.GONE);
            imgBlue.setVisibility(View.VISIBLE);
        }
    }

    void applyConfig()
    {
        if(mBluetoothLeService == null)
            return;
        int val;
        mConnectMode = BluetoothLeService.CONNECT_SETTING;

        val = mLedConfig.mLedColor | (mLedConfig.mBlinking << 2) | (mLedConfig.mDiscoMode << 6);
        mSettingBytes[0] = (byte)(val & 0xff);
        val = mLedConfig.mLedOnTime;
        mSettingBytes[1] = (byte)(val & 0xff);
        scanLeDevice(false);
        mBluetoothLeService.connectWithSetting(mDeviceAddress,mSettingBytes);
        enableButtons(false);
    }

    void loadConfig()
    {
        mLedConfig.loadConfig();
        updateRampState();

        mSeekBar.setProgress(mLedConfig.mBlinking);

        if(mLedConfig.mLedOnTime < 0 || mLedConfig.mLedOnTime > 5)
            mLedConfig.mLedOnTime = 0;
        int radio_button_Id = mRadioGroupLed.getChildAt(mLedConfig.mLedOnTime).getId();
        mRadioGroupLed.check( radio_button_Id );

        if(mLedConfig.mDiscoMode < 0 || mLedConfig.mDiscoMode > 1)
            mLedConfig.mDiscoMode = 0;
        radio_button_Id = mRadioGroupDisco.getChildAt(mLedConfig.mDiscoMode).getId();
        mRadioGroupDisco.check(radio_button_Id);;
    }

    private boolean checkAdvertisingPacket(final byte[] scanRecord)
    {
        byte[] advertisedData = Arrays.copyOf(scanRecord, scanRecord.length);
        int i = 0;
        int offset = 0;
        while (offset < (advertisedData.length - 2)) {
            int len = advertisedData[offset];
            offset++;
            if (len == 0)
                break;

            int type = (int)advertisedData[offset] & 0xff;
            offset++;
            switch (type) {
                case 0xFF:
                    Log.d(TAG, "Manufacturer Specific Data size:" + len + " bytes");
                    i = 0;
                    while (len > 1) {
                        if (i < 32) {
                            MfgData[i++] = advertisedData[offset++];
                        }
                        len -= 1;
                    }
                    Log.d(TAG, "Manufacturer Specific Data saved." + MfgData.toString());
                    break;
                default:
                    offset += (len - 1);
                    break;
            }
        }

        int m1 = (int)MfgData[0] & 0xff;
        int m2 = (int)MfgData[1] & 0xff;
        if(m1 == 0x4C && m2 == 0x5A)
            return true;
        return false;
    }

    private boolean checkBonded(String address)
    {
        Set<BluetoothDevice> myBondedDevices = mBluetoothAdapter.getBondedDevices();
        for(BluetoothDevice mydevice:myBondedDevices ){
            Log.i("BondedInfo", address + "   " + mydevice.getAddress());
            if(mydevice.getAddress().equals(address)){
                return true;
            }
        }
        return false;
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, final byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "Name=" + device.getName() + ", ADDR=" + device.getAddress());
                            if(device.getName().equals("FireFly") && checkAdvertisingPacket(scanRecord))
                            {
                                scanLeDevice(false);
                                mDevice = device;
                                mDeviceAddress = device.getAddress();
                                enableSettingView(true);
                                if(checkBonded(device.getAddress()))
                                    mDevicePaired = true;
                                else
                                    mDevicePaired = false;
                                updateViewState();
                            }
                        }
                    });
                }
            };

    public void enableButtons(boolean enable)
    {
        mButtonScan.setEnabled(enable);
        mButtonTest.setEnabled(enable);
        mButtonPair.setEnabled(enable);
        mButtonApply.setEnabled(enable);
    }

    public void enableSettingView(boolean enable)
    {
        mLayoutLamp.setEnabled(enable);
        findViewById(R.id.layoutSetting1).setEnabled(enable);
        findViewById(R.id.layoutSetting2).setEnabled(enable);
        findViewById(R.id.layoutSetting3).setEnabled(enable);
    }



    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };
    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if(BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;

                if(mConnectMode == BluetoothLeService.CONNECT_EVENT)
                {
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            disconnectFireFly();
                        }
                    }, mLedConfig.getLedOnTimeForDelay());
                }

            }

            else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                Toast.makeText(MainActivity.this,"Disconnected",Toast.LENGTH_SHORT).show();
                enableButtons(true);
            }

            else if(BluetoothLeService.ACTION_BATTERY_READ.equals(action)) {
                TextView view = (TextView)findViewById(R.id.lblBattery);
                int value = intent.getIntExtra(BluetoothLeService.EXTRA_DATA,0);
                view.setText(String.format("%d",value) + "%");
                Toast.makeText(MainActivity.this,"Battery=" + String.format("%d",value) + "%",Toast.LENGTH_SHORT).show();
            }

            else if(BluetoothLeService.ACTION_GATT_WRITE_DONE.equals(action)) {
                Toast.makeText(MainActivity.this, "Apply Success", Toast.LENGTH_SHORT).show();
                enableButtons(true);
            }

        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_BATTERY_READ);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_WRITE_DONE);

        return intentFilter;
    }

    public void testEvent()
    {
        if(mBluetoothLeService == null)
            return;
        mConnectMode = BluetoothLeService.CONNECT_EVENT;
        mBluetoothLeService.connectWithEvent(mDeviceAddress);
        scanLeDevice(false);
        enableButtons(false);
    }

    public void pairFireFly()
    {
        if(mDevice == null)
            return;
        if(mDevicePaired)
        {
            try {
                Method method = mDevice.getClass().getMethod("removeBond", (Class[]) null);
                method.invoke(mDevice, (Object[]) null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else
        {
            try {
                Method method = mDevice.getClass().getMethod("createBond", (Class[]) null);
                method.invoke(mDevice, (Object[]) null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        mPairHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                enableButtons(true);
            }
        }, 10000); // 10s
        enableButtons(false);
    }

    public void disconnectFireFly()
    {
        if(mConnected && mBluetoothLeService != null) {
            mBluetoothLeService.disconnect();
        }
        enableButtons(true);
    }

    private final BroadcastReceiver mPairReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                final int state        = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                final int prevState    = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);
                if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {
                    Toast.makeText(MainActivity.this,"Paired",Toast.LENGTH_SHORT).show();
                    mDevicePaired = true;
                } else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED) {
                    Toast.makeText(MainActivity.this, "Unpaired", Toast.LENGTH_SHORT).show();
                    mDevicePaired = false;
                }
                mPairHandler.removeCallbacksAndMessages(null);
                enableButtons(true);
                updateViewState();
            }
        }
    };

    private void updateViewState()
    {
        if(mDeviceAddress.length() <= 0)
            return;
        if(mDevicePaired) {
            mButtonPair.setText("Unpair");
            txtViewState.setText(mDeviceAddress + " (Paired)");
        } else {
            txtViewState.setText(mDeviceAddress + " (Not Paired)");
            mButtonPair.setText("Pair");
        }
    }
}
