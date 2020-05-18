package bt.lcy.btread;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;

import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import adapters.BtDevicesAdapter;
import adapters.BtServicesAdapter;


public class BtService extends Service {
    // 蓝牙低功耗概览  https://developer.android.com/guide/topics/connectivity/bluetooth-le.html
    // 要在AndroidManifest.xml 里面添加 <service android:name=".BtService" android:enabled="true"/>
    private static final String TAG = BtService.class.getSimpleName();

    private BluetoothGatt gatt;


    private final LocalBinder mBinder = new LocalBinder();
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter adapter;
    private String deviceAddress;
    private BluetoothGattCharacteristic systemid;
    private BluetoothDevice device;

    private boolean isAmoBoard = false;


    private int connectionState = BluetoothProfile.STATE_CONNECTED;

    private final static String INTENT_PREIFX = BtService.class.getPackage().getName();
    public final static String ACTION_GATT_CONNECTED = INTENT_PREIFX + ".ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = INTENT_PREIFX + ".ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = INTENT_PREIFX + ".ACTION_GATT_SERVICES_DISCONNECTED";
    public final static String ACTION_DATA_AVAILABLE = INTENT_PREIFX + ".ACTION_DATA_AVAILABEL";
    public final static String EXTRA_SERVICE_UUID = INTENT_PREIFX + ".EXTRA_SERVICE_UUID";
    public final static String EXTRA_CHARACTERISTIC_UUID = INTENT_PREIFX + ".EXTRA_CHARACTERISTIC_UUID";
    public final static String EXTRA_DATA = INTENT_PREIFX + ".EXTRA_DATA";
    public final static String EXTRA_TEXT = INTENT_PREIFX + ".EXTRA_TEXT";


    private HashMap<String, BluetoothGattCharacteristic> userCharacterMap = new HashMap<>();


    public BluetoothDevice getDevice() {
        return device;
    }

    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {

                intentAction = ACTION_GATT_CONNECTED;
                connectionState = BluetoothProfile.STATE_CONNECTED;
                //开启查找服务
                BtService.this.gatt.discoverServices();
                broadcastUpdate(intentAction);
//                Log.i(TAG, "----------- Connection to GATT Server.");

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                connectionState = BluetoothProfile.STATE_DISCONNECTED;
                Log.w(TAG, "?????? Disconnect to GATT Server.");
                broadcastUpdate(intentAction);
                userCharacterMap.clear();
                close(); // important
                if (adapter != null || gatt != null) {
                    gatt.disconnect();
                }
            } else if (newState == BluetoothProfile.STATE_CONNECTING) {
                Log.i(TAG, gatt.getDevice().getAddress() + "Connecting ...... ??? ");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
//            Log.i(TAG, "!!!!!!!!!!!! onServicesDiscoverd received: " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                checkAmoBoard();
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);

            } else {
                Log.w(TAG, "onServicesDiscoverd received: " + status);
            }
        }


        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

//            Log.i(TAG, "onCharacteristicChanged onCharacteristicChanged -------------- " + characteristic.getUuid().toString());
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            super.onCharacteristicChanged(gatt, characteristic);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

//            Log.i(TAG, "!!!onCharacteristicRead: ----- " + characteristic.getUuid().toString());
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
            super.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "onCharacteristicWrite uuid:" + characteristic.getUuid().toString());
            } else {
                Log.w(TAG, "!!!!!!Not Success  onCharacteristicWrite uuid:" + characteristic.getUuid().toString());
                gatt.writeCharacteristic(characteristic);
            }
            super.onCharacteristicWrite(gatt, characteristic, status);

        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void checkAmoBoard() {
        Log.i(TAG, "Check if its AmoBoard ......" + gatt.getServices().size());
        for (final BluetoothGattService service : gatt.getServices()) {
            String uuid = service.getUuid().toString();
            if (BtStaticVal.DEVICE_INFORMATION_SERVICE.equals(uuid.substring(0, 8))) {
                for (final BluetoothGattCharacteristic bc : service.getCharacteristics()) {
                    uuid = bc.getUuid().toString();
                    if (BtStaticVal.SRV_SYSTEM_ID.equals(uuid.substring(0, 8))) {
                        userCharacterMap.put(bc.getUuid().toString(), bc);
                        break;
                    }

                }

            }
//            Log.i(TAG,"check amoboard services.... " + uuid);
            if (BtStaticVal.UUID_KEY_DATA.equals(uuid)) {
                isAmoBoard = true;
                for (final BluetoothGattCharacteristic bc : service.getCharacteristics())
                    userCharacterMap.put(bc.getUuid().toString(), bc);
                break;
            }


        }

    }


    public boolean isAmoBoard() {
        return isAmoBoard;
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
//        Log.i(TAG, "Will send broadcast action: " + action);
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_SERVICE_UUID, characteristic.getService().getUuid().toString());
        intent.putExtra(EXTRA_CHARACTERISTIC_UUID, characteristic.getUuid().toString());
//        intent.putExtra(EXTRA_TEXT,Arrays.toString(characteristic.getValue()));

        sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        public BtService getService() {
            return BtService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        deviceAddress = intent.getStringExtra(BtDeviceServicesActivity.EXTRAS_DEVICE_ADDR);
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        deviceAddress = null;
        close();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        disconnect();
        super.onDestroy();
    }

    public boolean connect() {
        return connect(deviceAddress);
    }

    public boolean connect(final String address) {
        Log.i(TAG, "Want to connect " + address);
        if (address == null || adapter == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        if (deviceAddress != null && address.equals(deviceAddress) && gatt != null) {
            if (gatt.connect()) {
                connectionState = BluetoothProfile.STATE_CONNECTED;
                return true;
            } else {
                return false;
            }
        }
        device = adapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found,Unable to connect.");
            return false;
        }
        // 连接到设备,成功之后回调bluetoothGattCallback
        gatt = device.connectGatt(this, false, bluetoothGattCallback);
//        Log.i(TAG, "Connected gatt " + address + " gatt:" + gatt);
        deviceAddress = address;
        connectionState = BluetoothProfile.STATE_CONNECTED;
        return true;
    }

    public void close() {
        if (gatt == null) return;

        gatt.close();
        gatt = null;
    }

    public void disconnect() {
        if ( gatt == null) {
            Log.w(TAG, "disconnect BluetoothAdapter not initailized.");
            return;
        }
        gatt.disconnect();
    }


    public void readCharateristic(BluetoothGattCharacteristic characteristic) {
        if (adapter == null || gatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        Log.i(TAG, "readCharacteristic call");
        gatt.readCharacteristic(characteristic);
    }

    public byte[] getValue(final BluetoothGattCharacteristic characteristic) {
        return characteristic.getValue();
    }


    public void writeCharacteristic(final BluetoothGattCharacteristic characteristic) {
        gatt.writeCharacteristic(characteristic);
    }

    public BluetoothGattCharacteristic getAmoBoardCharacteristic(final String uuid) {
//        Log.i(TAG,"userCharacterList size: " + userCharacterMap.size());
//        Log.w(TAG,"get uuid char is : "  + uuid + userCharacterMap.containsKey(uuid));
        if (userCharacterMap.containsKey(uuid))
            return userCharacterMap.get(uuid);
        return null;
    }


    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, final boolean flag) {
        gatt.setCharacteristicNotification(characteristic, flag);
    }

    public void writeDescriptor(BluetoothGattDescriptor descriptor) {
        gatt.writeDescriptor(descriptor);
    }

    public List<BluetoothGattService> getSupportedGattServices() {
        if (gatt == null) return null;

        return gatt.getServices();
    }

    public boolean initialize() {
        if (bluetoothManager == null) {
            bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }
        adapter = bluetoothManager.getAdapter();
        if (adapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        Log.i(TAG, "BluetoothAdapter has initialized...................Ok");
        return true;
    }


}
