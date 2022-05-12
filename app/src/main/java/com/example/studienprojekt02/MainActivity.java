package com.example.studienprojekt02;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    LocationManager locationmanager;
    BluetoothAdapter bluetoothAdapter;
    BluetoothLeScanner bleScanner;
    private BluetoothGatt bluetoothServer;

    String MAC = "A4:C1:38:31:A9:E4";
    String TAG = "LOL444";


    UUID WEIGHT_MEASUREMENT_SERVICE = convertFromInteger(0x78b2);
    UUID WEIGHT_MEASUREMENT_CHARACTERISTIC = convertFromInteger(0x8a21); // indication, read-only
    UUID FEATURE_MEASUREMENT_CHARACTERISTIC = convertFromInteger(0x8a22); // indication, read-only
    UUID CMD_MEASUREMENT_CHARACTERISTIC = convertFromInteger(0x8a81); // write-only
    UUID CUSTOM5_MEASUREMENT_CHARACTERISTIC = convertFromInteger(0x8a82); // indication, read-only
    UUID CLIENT_CHARACTERISTIC_CONFIG_UUID = convertFromInteger(0x2902);



    public static final int BLUETOOTH_REQ_CODE = 1;

    private ArrayList<BluetoothDevice> scannedDevices = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        Button scanButton = findViewById(R.id.scanButton);
        Button connectButton = findViewById(R.id.connectButton);
        Button resetButton = findViewById(R.id.resetButton);
        TextView deviceTextview = findViewById(R.id.scandevicestextview);
        TextView hrTextview = findViewById(R.id.hrtextview);

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        bleScanner = bluetoothAdapter.getBluetoothLeScanner();

        locationmanager = (LocationManager) getSystemService(LOCATION_SERVICE);

        scanButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.INTERNET}
                                ,10);
                    }
                    return;
                }

                if(!bluetoothAdapter.isEnabled()){
                    Intent bluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(bluetoothIntent, BLUETOOTH_REQ_CODE);
                }

                final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                bluetoothAdapter = bluetoothManager.getAdapter();
                bleScanner = bluetoothAdapter.getBluetoothLeScanner();

                scanble();
            }
        });

        connectButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                connect();
            }
        });

        resetButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                display(0,"");
            }
        });
    }

    public void scanble(){
        TextView deviceTextview = findViewById(R.id.scandevicestextview);

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                bleScanner.stopScan(scancallback);
            }
        }, 10000);
        bleScanner.startScan(scancallback);
        if(scannedDevices.isEmpty())
            deviceTextview.setText("NO DEVICES FOUND");
    }

    private ScanCallback scancallback =
            new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);
                    TextView deviceTextview = findViewById(R.id.scandevicestextview);
                    if(!scannedDevices.contains(result.getDevice())){
                        if(scannedDevices.isEmpty())
                            deviceTextview.setText("");
                        scannedDevices.add(result.getDevice());
                        deviceTextview.setText(deviceTextview.getText() + " " + result.getDevice().getName() + " " + result.getDevice() + "\n");
                    }
                }
            };

    public void connect() {
        BluetoothDevice bluetoothDevice=bluetoothAdapter.getRemoteDevice(MAC);
        if(bluetoothDevice!=null) {
            bluetoothServer = bluetoothDevice.connectGatt(this, true, serverCallback);
        }
    }

    private BluetoothGattCallback serverCallback =
            new BluetoothGattCallback() {
                List<BluetoothGattCharacteristic> characteristics = new ArrayList<>();

                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    super.onConnectionStateChange(gatt, status, newState);
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        Log.d(TAG, "Connected to GATT.");
                        gatt.discoverServices();
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        Log.d(TAG, "Disconnected from GATT.");
                    }


                }

                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        characteristics.add(gatt.getService(WEIGHT_MEASUREMENT_SERVICE)
                                .getCharacteristic(FEATURE_MEASUREMENT_CHARACTERISTIC));
                        characteristics.add(gatt.getService(WEIGHT_MEASUREMENT_SERVICE)
                                .getCharacteristic(WEIGHT_MEASUREMENT_CHARACTERISTIC));
                        characteristics.add(gatt.getService(WEIGHT_MEASUREMENT_SERVICE)
                                .getCharacteristic(CUSTOM5_MEASUREMENT_CHARACTERISTIC));

                        subscribeToCharacteristics(gatt);

                    }

                }




                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                    if(characteristic.equals(gatt.getService(WEIGHT_MEASUREMENT_SERVICE)
                            .getCharacteristic(WEIGHT_MEASUREMENT_CHARACTERISTIC))){
                        processData(characteristic.getValue(), gatt);
                    }else{
                        display(0,"WAIT");
                    }

                }

                @Override
                public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                    if(characteristics.size() > 1){
                        characteristics.remove(0);
                        subscribeToCharacteristics(gatt);
                    }
                    else{
                        writeCMD(gatt);
                    }
                }


                private void subscribeToCharacteristics(BluetoothGatt gatt) {
                    if(characteristics.size() == 0) return;

                    BluetoothGattCharacteristic characteristic = characteristics.get(0);
                    gatt.setCharacteristicNotification(characteristic, true);

                    BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID);
                    if(descriptor != null) {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                        gatt.writeDescriptor(descriptor);
                    }
                }


                public void writeCMD(BluetoothGatt gatt){
                    BluetoothGattCharacteristic characteristic = gatt.getService(WEIGHT_MEASUREMENT_SERVICE)
                            .getCharacteristic(CMD_MEASUREMENT_CHARACTERISTIC);

                    byte[] magicnumber = {0x02,0x7b,0x7b, (byte) 0xf6,0x0d};
                    characteristic.setValue(magicnumber);
                    gatt.writeCharacteristic(characteristic);

                }


            };

    public void processData(byte[] data, BluetoothGatt gatt) {
        byte b1 = (byte) data[2];
        String s1 = String.format("%8s", Integer.toBinaryString(b1 & 0xFF)).replace(' ', '0');
        System.out.println(s1); // 10000001

        byte b2 = (byte) data[1];
        String s2 = String.format("%8s", Integer.toBinaryString(b2 & 0xFF)).replace(' ', '0');
        //System.out.println(s2); // 00000010

        String erg = s1+s2;
        //System.out.println(erg);

        int i = Integer.parseInt(erg,2);
        double d = i;

        display(d/100,"");
        gatt.close();


    }

    private void display(double HR,String s) {
        if(HR != 0){
            ((TextView) findViewById(R.id.hrtextview)).invalidate();
            ((TextView) findViewById(R.id.hrtextview)).setText(HR+"");
        }else{
            ((TextView) findViewById(R.id.hrtextview)).invalidate();
            ((TextView) findViewById(R.id.hrtextview)).setText(s);
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            Toast.makeText(MainActivity.this, "Bluetooth is ON", Toast.LENGTH_SHORT).show();
        } else if (resultCode == RESULT_CANCELED) {
            Toast.makeText(MainActivity.this, "Bluetooth operation is cancelled",
                    Toast.LENGTH_SHORT).show();
        }
    }

    public UUID convertFromInteger(int i) {
        final long MSB = 0x0000000000001000L;
        final long LSB = 0x800000805f9b34fbL;
        long value = i & 0xFFFFFFFF;
        return new UUID(MSB | (value << 32), LSB);
    }
}