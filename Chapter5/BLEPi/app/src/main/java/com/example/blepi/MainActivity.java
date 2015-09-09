package com.example.blepi;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;

import android.content.Intent;
import android.os.Build;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.UUID;


@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends ActionBarActivity {
    public static boolean TEST_ANDROID_18 = true; // set to true of you donot want to test. Set to false if you want to test Android 4.3 parts on Android 5.0 device
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bleScanner;
    private BluetoothGatt bleGatt;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final UUID UUID_Service = UUID.fromString("19fc95c0-c111-11e3-9904-0002a5d5c51b");
    private static final UUID UUID_TEMPERATURE = UUID.fromString("21fac9e0-c111-11e3-9246-0002a5d5c51b");
    private static final UUID UUID_HUMIDITY = UUID.fromString("31fac9e0-c111-11e3-9246-0002a5d5c51b");

    private static final UUID UUID_REBOOT = UUID.fromString("41fac9e0-c111-11e3-9246-0002a5d5c51b");
    private volatile boolean isSendReboot = false;

    private static final UUID UUID_TURNON = UUID.fromString("51fac9e0-c111-11e3-9246-0002a5d5c51b");
    private static final UUID UUID_TURNOFF = UUID.fromString("61fac9e0-c111-11e3-9246-0002a5d5c51b");
    private volatile boolean isSendTurnOn = false;
    private volatile boolean isSendTurnOff = false;

    private static final UUID UUID_WHISTLE = UUID.fromString("71fac9e0-c111-11e3-9246-0002a5d5c51b");
    private volatile boolean isSendWhistle = false;

    private static final UUID UUID_WHISTLE_AND_TURNON = UUID.fromString("81fac9e0-c111-11e3-9246-0002a5d5c51b");
    private volatile boolean isSendWhistleAndTurnOn = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        TelephonyManager TelephonyMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        TelephonyMgr.listen(new PhoneListener(), PhoneStateListener.LISTEN_CALL_STATE);
    }

    class PhoneListener extends PhoneStateListener {
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    Toast.makeText(getApplicationContext(), incomingNumber, Toast.LENGTH_LONG).show();
                    Toast.makeText(getApplicationContext(), "CALL_STATE_RINGING", Toast.LENGTH_LONG).show();
                    isSendWhistleAndTurnOn = true;
                    break;
                default:
                    break;
            }
        }
    }

    private void startScan() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            if(TEST_ANDROID_18 && Build.VERSION.SDK_INT >= 21) {
                bleScanner = bluetoothAdapter.getBluetoothLeScanner();
                if (bleScanner != null) {
                    final ScanFilter scanFilter = new ScanFilter.Builder().build();
                    ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
                    bleScanner.startScan(Arrays.asList(scanFilter), settings, scanCallback);
                }
            } else {
                bluetoothAdapter.startLeScan(mLeScanCallback);
            }
        }
    }

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            connected(result.getDevice());
            super.onScanResult(callbackType, result);
        }
    };
   private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
       @Override
       public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            connected(device);
       }
   };

    private void connected(BluetoothDevice device) {
        if("Gopher".equals(device.getName())) {
            Toast.makeText(MainActivity.this, "Gopher found", Toast.LENGTH_SHORT).show();
            if(TEST_ANDROID_18 && Build.VERSION.SDK_INT >= 21) {
                if (bleScanner != null) {
                    bleScanner.stopScan(scanCallback);
                }
            } else {
                bluetoothAdapter.stopLeScan(mLeScanCallback);
            }
            bleGatt = device.connectGatt(getApplicationContext(), false, bleGattCallback);
            if(bleGatt != null) {
                MainActivity.this.findViewById(R.id.reboot_button).setEnabled(true);
                MainActivity.this.findViewById(R.id.turnon_button).setEnabled(true);
                MainActivity.this.findViewById(R.id.turnoff_button).setEnabled(true);
                MainActivity.this.findViewById(R.id.whistle_button).setEnabled(true);
            }
        }
    }

    private BluetoothGattCallback bleGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            gatt.discoverServices();
            super.onConnectionStateChange(gatt, status, newState);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            BluetoothGattService service = gatt.getService(UUID_Service);

            BluetoothGattCharacteristic temperatureCharacteristic = service.getCharacteristic(UUID_TEMPERATURE);
            gatt.readCharacteristic(temperatureCharacteristic);

            super.onServicesDiscovered(gatt, status);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, int status) {
            final String value = characteristic.getStringValue(0);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView tv = null;
                    if (UUID_HUMIDITY.equals(characteristic.getUuid())) {
                        tv = (TextView) MainActivity.this.findViewById(R.id.humidity_textview);
                    } else if (UUID_TEMPERATURE.equals(characteristic.getUuid()))  {
                        tv = (TextView) MainActivity.this.findViewById(R.id.temperature_textview);
                    }
                    if(tv != null)
                        tv.setText(value);
                }
            });

            BluetoothGattService service = gatt.getService(UUID_Service);

            if(isSendReboot) {
                BluetoothGattCharacteristic rebootCharacteristic = service.getCharacteristic(UUID_REBOOT);
                rebootCharacteristic.setValue("reboot");
                gatt.writeCharacteristic(rebootCharacteristic);
            } else if(isSendTurnOn) {
                BluetoothGattCharacteristic turnOnCharacteristic = service.getCharacteristic(UUID_TURNON);
                turnOnCharacteristic.setValue("turnon");
                gatt.writeCharacteristic(turnOnCharacteristic);
            } else if(isSendTurnOff) {
                BluetoothGattCharacteristic turnOffCharacteristic = service.getCharacteristic(UUID_TURNOFF);
                turnOffCharacteristic.setValue("turnoff");
                gatt.writeCharacteristic(turnOffCharacteristic);
            } else if(isSendWhistle) {
                BluetoothGattCharacteristic whistleCharacteristic = service.getCharacteristic(UUID_WHISTLE);
                whistleCharacteristic.setValue("whistle");
                gatt.writeCharacteristic(whistleCharacteristic);
            }  else if(isSendWhistleAndTurnOn) {
                BluetoothGattCharacteristic whistleAndTurnOnCharacteristic = service.getCharacteristic(UUID_WHISTLE_AND_TURNON);
                whistleAndTurnOnCharacteristic.setValue("whistleturnon");
                gatt.writeCharacteristic(whistleAndTurnOnCharacteristic);
            } else {
               readNextCharacteristic(gatt, characteristic);
            }
            super.onCharacteristicRead(gatt, characteristic, status);
        }

        private void readNextCharacteristic(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            BluetoothGattService service = gatt.getService(UUID_Service);

            if (UUID_HUMIDITY.equals(characteristic.getUuid())) {
                BluetoothGattCharacteristic temperatureCharacteristic = service.getCharacteristic(UUID_TEMPERATURE);
                gatt.readCharacteristic(temperatureCharacteristic);
            } else {
                BluetoothGattCharacteristic humidityCharacteristic = service.getCharacteristic(UUID_HUMIDITY);
                gatt.readCharacteristic(humidityCharacteristic);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            isSendReboot = false;

            isSendTurnOn = false;
            isSendTurnOff = false;
            isSendWhistle = false;
            isSendWhistleAndTurnOn = false;

            readNextCharacteristic(gatt, characteristic);

            super.onCharacteristicWrite(gatt, characteristic, status);
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_ENABLE_BT){
            startScan();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        startScan();
        super.onResume();
    }


    @Override
    protected void onPause() {
        if(TEST_ANDROID_18 && Build.VERSION.SDK_INT >= 21) {
            if (bleScanner != null) {
                bleScanner.stopScan(scanCallback);
            }
        } else {
            bluetoothAdapter.stopLeScan(mLeScanCallback);
        }

        if (bleGatt != null) {
            bleGatt.close();
            bleGatt.disconnect();
            bleGatt = null;
        }

        super.onPause();
    }


    public void sendRebootCommand(View v) throws InterruptedException {
        isSendReboot = true;
    }

    public void sendTurnOnCommand(View v) throws InterruptedException {
        isSendTurnOn = true;
    }

    public void sendTurnOffCommand(View v) throws InterruptedException {
        isSendTurnOff = true;
    }

    public void sendWhistleCommand(View v) throws InterruptedException {
        isSendWhistle = true;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
