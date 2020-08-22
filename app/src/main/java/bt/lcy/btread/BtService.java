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
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Button;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;


import timber.log.Timber;

import adapters.BtDevicesAdapter;
import adapters.BtServicesAdapter;

import static android.bluetooth.BluetoothDevice.BOND_BONDED;
import static android.bluetooth.BluetoothDevice.BOND_BONDING;
import static android.bluetooth.BluetoothDevice.BOND_NONE;
import static android.bluetooth.BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION;
import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_INDICATE;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_NOTIFY;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE;
import static android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT;
import static android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE;
import static android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_SIGNED;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;
import static bt.lcy.btread.BtStaticVal.CCC_DESCRIPTOR_UUID;


public class BtService extends Service {
    // 蓝牙低功耗概览  https://developer.android.com/guide/topics/connectivity/bluetooth-le.html
    // 要在AndroidManifest.xml 里面添加 <service android:name=".BtService" android:enabled="true"/>
    private static final String TAG = BtService.class.getSimpleName();
    public static final int GATT_AUTH_FAIL = 137;


    private BluetoothGatt bluetoothGatt;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable discoverServicesRunnable;
    private final Queue<Runnable> commandQueue = new ConcurrentLinkedQueue<>();
    private boolean commandQueueBusy;

    private Runnable timeoutRunnable;
    private final LocalBinder mBinder = new LocalBinder();
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter adapter;
    private String deviceAddress;
    private BluetoothGattCharacteristic systemid;
    private BluetoothDevice device;

    private final Set<UUID> notifyingCharacteristics = new HashSet<>();
    private boolean isAmoBoard = false;
    private boolean isTiProjectZero = false;
    private String cachedName;

    private int state;

    private final static String INTENT_PREIFX = BtService.class.getPackage().getName();
    public final static String ACTION_GATT_CONNECTED = INTENT_PREIFX + ".ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = INTENT_PREIFX + ".ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = INTENT_PREIFX + ".ACTION_GATT_SERVICES_DISCONNECTED";
    public final static String ACTION_DATA_AVAILABLE = INTENT_PREIFX + ".ACTION_DATA_AVAILABEL";
    public final static String EXTRA_SERVICE_UUID = INTENT_PREIFX + ".EXTRA_SERVICE_UUID";
    public final static String EXTRA_CHARACTERISTIC_UUID = INTENT_PREIFX + ".EXTRA_CHARACTERISTIC_UUID";
    public final static String EXTRA_DATA = INTENT_PREIFX + ".EXTRA_DATA";
    public final static String EXTRA_TEXT = INTENT_PREIFX + ".EXTRA_TEXT";
    public final static String WRITE_TEXT = INTENT_PREIFX + ".WRITE_TEXT";


    /**
     * The connection was terminated because of a L2C failure
     */
    @SuppressWarnings("WeakerAccess")
    public static final int GATT_CONN_L2C_FAILURE = 1;

    /**
     * The connection has timed out
     */
    @SuppressWarnings("WeakerAccess")
    public static final int GATT_CONN_TIMEOUT = 8;

    /**
     * GATT read operation is not permitted
     */
    @SuppressWarnings("WeakerAccess")
    public static final int GATT_READ_NOT_PERMITTED = 2;

    /**
     * GATT write operation is not permitted
     */
    @SuppressWarnings("WeakerAccess")
    public static final int GATT_WRITE_NOT_PERMITTED = 3;

    /**
     * Insufficient authentication for a given operation
     */
    @SuppressWarnings("WeakerAccess")
    public static final int GATT_INSUFFICIENT_AUTHENTICATION = 5;

    /**
     * The given request is not supported
     */
    @SuppressWarnings("WeakerAccess")
    public static final int GATT_REQUEST_NOT_SUPPORTED = 6;

    /**
     * Insufficient encryption for a given operation
     */
    @SuppressWarnings("WeakerAccess")
    public static final int GATT_INSUFFICIENT_ENCRYPTION = 15;

    /**
     * The connection was terminated by the peripheral
     */
    @SuppressWarnings("WeakerAccess")
    public static final int GATT_CONN_TERMINATE_PEER_USER = 19;

    /**
     * The connection was terminated by the local host
     */
    @SuppressWarnings("WeakerAccess")
    public static final int GATT_CONN_TERMINATE_LOCAL_HOST = 22;

    /**
     * The connection lost because of LMP timeout
     */
    @SuppressWarnings("WeakerAccess")
    public static final int GATT_CONN_LMP_TIMEOUT = 34;

    /**
     * The connection was terminated due to MIC failure
     */
    @SuppressWarnings("WeakerAccess")
    public static final int BLE_HCI_CONN_TERMINATED_DUE_TO_MIC_FAILURE = 61;

    /**
     * The connection cannot be established
     */
    @SuppressWarnings("WeakerAccess")
    public static final int GATT_CONN_FAIL_ESTABLISH = 62;

    /**
     * The peripheral has no resources to complete the request
     */
    @SuppressWarnings("WeakerAccess")
    public static final int GATT_NO_RESOURCES = 128;

    /**
     * Something went wrong in the bluetooth stack
     */
    @SuppressWarnings("WeakerAccess")
    public static final int GATT_INTERNAL_ERROR = 129;

    /**
     * The GATT operation could not be executed because the stack is busy
     */
    @SuppressWarnings("WeakerAccess")
    public static final int GATT_BUSY = 132;

    /**
     * Generic error, could be anything
     */
    @SuppressWarnings("WeakerAccess")
    public static final int GATT_ERROR = 133;

    /**
     * The connection was cancelled
     */
    @SuppressWarnings("WeakerAccess")
    public static final int GATT_CONN_CANCEL = 256;

    // The maximum number of enabled notifications Android supports (BTA_GATTC_NOTIF_REG_MAX)
    private static final int MAX_NOTIFYING_CHARACTERISTICS = 15;


    private static HashMap<String, BluetoothGattCharacteristic> userCharacterMap = new HashMap<>();


    public BluetoothDevice getDevice() {
        return device;
    }

    public static void clearCharacterMap() { userCharacterMap.clear();}

    private static String statusToString(final int error) {
        switch (error) {
            case GATT_SUCCESS:
                return "SUCCESS";
            case GATT_CONN_L2C_FAILURE:
                return "GATT CONN L2C FAILURE";
            case GATT_CONN_TIMEOUT:
                return "GATT CONN TIMEOUT";  // Connection timed out
            case GATT_CONN_TERMINATE_PEER_USER:
                return "GATT CONN TERMINATE PEER USER";
            case GATT_CONN_TERMINATE_LOCAL_HOST:
                return "GATT CONN TERMINATE LOCAL HOST";
            case BLE_HCI_CONN_TERMINATED_DUE_TO_MIC_FAILURE:
                return "BLE HCI CONN TERMINATED DUE TO MIC FAILURE";
            case GATT_CONN_FAIL_ESTABLISH:
                return "GATT CONN FAIL ESTABLISH";
            case GATT_CONN_LMP_TIMEOUT:
                return "GATT CONN LMP TIMEOUT";
            case GATT_CONN_CANCEL:
                return "GATT CONN CANCEL ";
            case GATT_BUSY:
                return "GATT BUSY";
            case GATT_ERROR:
                return "GATT ERROR"; // Device not reachable
            case GATT_AUTH_FAIL:
                return "GATT AUTH FAIL";  // Device needs to be bonded
            case GATT_NO_RESOURCES:
                return "GATT NO RESOURCES";
            case GATT_INTERNAL_ERROR:
                return "GATT INTERNAL ERROR";
            default:
                return "UNKNOWN (" + error + ")";
        }
    }

    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            String intentAction;

            final int previousState = state;
            state = newState;
            Log.i(TAG,"onConnectionStateChange: " + status + ", newstate : " + newState);
            if(status == GATT_SUCCESS){
                switch (newState) {
                    case BluetoothProfile.STATE_CONNECTED:
                        successfullyConnected(device.getBondState());
                        break;
                    case BluetoothProfile.STATE_DISCONNECTED:
                        successfullyDisconnected(previousState);
                        break;
                    case BluetoothProfile.STATE_DISCONNECTING:
                        Timber.i("peripheral is disconnecting");
                        break;
                    case BluetoothProfile.STATE_CONNECTING:
                        Timber.i("peripheral is connecting");
                    default:
                        Timber.e("unknown state received");
                        break;
                }
            }else{
                connectionStateChangeUnsuccessful(status, previousState, newState);
            }
//            if (newState == BluetoothProfile.STATE_CONNECTED) {
//
//                connectionState = BluetoothProfile.STATE_CONNECTED;
//                //开启查找服务
//                bluetoothGatt.discoverServices();
//                intentAction = ACTION_GATT_CONNECTED;
//                broadcastUpdate(intentAction);
//            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
//                connectionState = BluetoothProfile.STATE_DISCONNECTED;
//                Log.w(TAG, "?????? Disconnect to GATT Server.");
//                intentAction = ACTION_GATT_DISCONNECTED;
//                broadcastUpdate(intentAction);
//                userCharacterMap.clear();
//                gatt.close(); // important
//            } else if (newState == BluetoothProfile.STATE_CONNECTING) {
//                Log.i(TAG, gatt.getDevice().getAddress() + "STATE_CONNECTING  Connecting ...... ??? ");
//            }else{
//                gatt.close();
//            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
//            super.onServicesDiscovered(gatt, status);
//            Log.i(TAG, "!!!!!!!!!!!! onServicesDiscoverd received: " + status);
            if (status == GATT_SUCCESS) {
                // 发现服务后，遍历服务与特征。
                checkServiceList();
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscoverd received: " + status);
            }
        }


        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

//            Log.i(TAG, "onCharacteristicChanged onCharacteristicChanged -------------- " + characteristic.getUuid().toString());
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
//            super.onCharacteristicChanged(gatt, characteristic);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

//            Log.i(TAG, "!!!onCharacteristicRead: ----- " + characteristic.getUuid().toString());
            if (status == GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
            super.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

            if (status == GATT_SUCCESS) {
                Timber.d(TAG, "onCharacteristicWrite uuid:" + characteristic.getUuid().toString());
            } else {
                if(status == GATT_INSUFFICIENT_AUTHENTICATION || status == GATT_AUTH_FAIL){
                    Log.w(TAG, "!!!!!!Not Success  onCharacteristicWrite uuid:" + characteristic.getUuid().toString() + " status : " + statusToString(status));
                }else{
                    gatt.writeCharacteristic(characteristic);
                }
            }
//            super.onCharacteristicWrite(gatt, characteristic, status);

        }



        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.i(TAG,"onDescriptorWrite uuid: " +  descriptor.getUuid().toString()  + " status : " + status);
            final BluetoothGattCharacteristic parentCharacteristic = descriptor.getCharacteristic();
            if (status != GATT_SUCCESS) {
//                Timber.d(TAG, "onCharacteristicWrite uuid:" + characteristic.getUuid().toString());
                Log.i(TAG,"failed onDescriptorWrite uuid: " +  parentCharacteristic.getUuid().toString());
            }

            if(descriptor.getUuid().equals(UUID.fromString(CCC_DESCRIPTOR_UUID ))){
                if(status == GATT_SUCCESS){
                    byte[] value = descriptor.getValue();
                    if(value != null){
                        if(Arrays.equals(value,BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) ||
                           Arrays.equals(value,BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)){
                            notifyingCharacteristics.add(parentCharacteristic.getUuid());
                            if (notifyingCharacteristics.size() > MAX_NOTIFYING_CHARACTERISTICS) {
                                Timber.e("too many (%d) notifying characteristics. The maximum Android can handle is %d", notifyingCharacteristics.size(), MAX_NOTIFYING_CHARACTERISTICS);
                            }else if (Arrays.equals(value, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)){
                                // Notify was turned off, so remove it from the set of notifying characteristics
                                notifyingCharacteristics.remove(parentCharacteristic.getUuid());
                            } else {
                                Timber.e("unexpected CCC descriptor value");
                            }
                        }
                    }

                }
            }

//            super.onDescriptorWrite(gatt, descriptor, status);
        }
    };




    private void successfullyConnected(int bondstate) {
        Log.i(TAG,"connected to '%s' (%s) "+getName()+bondStateToString(bondstate));

        if (bondstate == BOND_NONE || bondstate == BOND_BONDED) {
            delayedDiscoverServices(getServiceDiscoveryDelay(bondstate));
        } else if (bondstate == BOND_BONDING) {
            // Apparently the bonding process has already started, so let it complete. We'll do discoverServices once bonding finished
            Log.i(TAG,"waiting for bonding to complete");
        }
    }

    private void delayedDiscoverServices(final long delay) {
        discoverServicesRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG,"discovering services of '%s' with delay of %d ms"+ getName() + delay);
                if (!bluetoothGatt.discoverServices()) {
                    Log.e(TAG,"discoverServices failed to start");
                }
                discoverServicesRunnable = null;
            }
        };
        mainHandler.postDelayed(discoverServicesRunnable, delay);
    }

    private long getServiceDiscoveryDelay(int bondstate) {
        long delayWhenBonded = 0;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
            // It seems delays when bonded are only needed in versions Nougat or lower
            // This issue was observed on a Nexus 5 (M) and Sony Xperia L1 (N) when connecting to a A&D UA-651BLE
            // The delay is needed when devices have the Service Changed Characteristic.
            // If they don't have it the delay isn't needed but we do it anyway to keep code simple
            delayWhenBonded = 1000L;
        }
        return bondstate == BOND_BONDED ? delayWhenBonded : 0;
    }

    private void successfullyDisconnected(int previousState) {
        if (previousState == BluetoothProfile.STATE_CONNECTED || previousState == BluetoothProfile.STATE_DISCONNECTING) {
            Timber.i("disconnected '%s' on request", getName());
        } else if (previousState == BluetoothProfile.STATE_CONNECTING) {
            Timber.i("cancelling connect attempt");
        }
        completeDisconnect(false, GATT_SUCCESS);
    }

    private void connectionStateChangeUnsuccessful(int status, int previousState, int newState) {
        // Check if service discovery completed
        if (discoverServicesRunnable != null) {
            // Service discovery is still pending so cancel it
            mainHandler.removeCallbacks(discoverServicesRunnable);
            discoverServicesRunnable = null;
        }
//        boolean servicesDiscovered = bluetoothGatt.getServices().isEmpty();
        completeDisconnect(true, status);

    }


    private void checkServiceList() {
        // 遍历 GATT 服务器上的服务与特征。
        for (final BluetoothGattService service : bluetoothGatt.getServices()) {
            String uuid = service.getUuid().toString();
//            Log.i(TAG,"@@@@@@@@@@@check Device services.... " + uuid);
            if (BtStaticVal.DEVICE_INFORMATION_SERVICE.equals(uuid.substring(0, 8)) ||
                    BtStaticVal.GENERIC_ACCESS_UUID.equals(uuid.substring(0, 8))) {
                for (final BluetoothGattCharacteristic bc : service.getCharacteristics()) {
                    uuid = bc.getUuid().toString();
//                    Log.i(TAG,"@@@@@@@@@@@check Device service.getCharacteristics.... " + bc.getUuid().toString());
                    if (BtStaticVal.SRV_SYSTEM_ID.equals(uuid.substring(0, 8))) {
                        userCharacterMap.put(bc.getUuid().toString(), bc);
                    }else if(BtStaticVal.SRV_DEVICE_NAME.equals(uuid.substring(0, 8))){
                        userCharacterMap.put(bc.getUuid().toString(), bc);
                    }
                }
            }else if (BtStaticVal.UUID_KEY_DATA.equals(uuid)) {
                isAmoBoard = true;
//                Log.i(TAG,"!!!!!-> isAmoBoard  uuid is: " + BtStaticVal.UUID_KEY_DATA);
                for (final BluetoothGattCharacteristic bc : service.getCharacteristics())
                    userCharacterMap.put(bc.getUuid().toString(), bc);
                break;
            }else  if (TiMsp432ProjectZeroActivity.isVaildUUID(uuid)) {
                isTiProjectZero = true;
                for (final BluetoothGattCharacteristic bc : service.getCharacteristics()){
                    // 列出所有的UUID。
                    userCharacterMap.put(bc.getUuid().toString(), bc);
                    StringBuilder property = new StringBuilder();
                    int ps = bc.getProperties();
                    property.append(" w: " + (( ps & BluetoothGattCharacteristic.PROPERTY_READ) != 0 ? "true" : "false"));
                    property.append(" ,r: " + (( ps & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0 ? "true" : "false"));
                    property.append(" ,n: " + (( ps & PROPERTY_NOTIFY) != 0 ? "true" : "false"));
                    property.append(" ,wn: " + (( ps & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0 ? "true" : "false"));
                    property.append(" ,sw: " + (( ps & BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE) != 0 ? "true" : "false"));
                    Log.i(TAG,"!!!!!-> uuid is: " + bc.getUuid().toString() + ", property : " + property);
                }
            }
        }

    }

    /**
     * Complete the disconnect after getting connectionstate == disconnected
     */
    private void completeDisconnect(boolean notify, final int status) {
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
        Timber.d(TAG,"completeDisconnect----------->!!!!!!!!");
        broadcastUpdate(ACTION_GATT_DISCONNECTED);
    }

    private String bondStateToString(final int state) {
        switch (state) {
            case BOND_NONE:
                return "BOND_NONE";
            case BOND_BONDING:
                return "BOND_BONDING";
            case BOND_BONDED:
                return "BOND_BONDED";
            default:
                return "UNKNOWN";
        }
    }



    public String getName() {
        String name = device.getName();
        if (name != null) {
            // Cache the name so that we even know it when bluetooth is switched off
            cachedName = name;
        }
        return cachedName;
    }

    public boolean isAmoBoard() {
        return isAmoBoard;
    }

    public boolean isTiProjectZero() {
        return isTiProjectZero;
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

    private void broadcastUpdate(final String action) {
        sendBroadcast(new Intent(action));
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
        Log.i(TAG, "Want to connect " + address + " ,now state : "+ state);
        if (address == null || adapter == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

//        Log.d(TAG,"They equal ? " + address.equals(deviceAddress));
//        if (deviceAddress != null && address.equals(deviceAddress) && bluetoothGatt != null) {
//            if (bluetoothGatt.connect()) {
//                return true;
//            } else {
//                return false;
//            }
//        }
        device = adapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found,Unable to connect.");
            return false;
        }

        if(state == BluetoothProfile.STATE_DISCONNECTED) {
            mainHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    state = BluetoothProfile.STATE_CONNECTING;
                    deviceAddress = address;
                    bluetoothGatt = device.connectGatt(BtService.this, false, bluetoothGattCallback);

                    // 连接到设备,成功之后回调bluetoothGattCallback
                    Log.i(TAG,"device.connectGatt --> " + address);
                    // 发送广播，让接收者做出相应的操作。
                    broadcastUpdate(ACTION_GATT_CONNECTED);
                }
            }, 200);
        }else if(state == STATE_CONNECTED){

            if (deviceAddress != null && address.equals(deviceAddress) && bluetoothGatt != null) {

                if (bluetoothGatt.connect()) {
                    Log.i(TAG, "STATE_CONNECTED ..................");
                    state = BluetoothProfile.STATE_CONNECTED;
                    broadcastUpdate(ACTION_GATT_CONNECTED);
                    return true;
                } else {
                    return false;
                }
            }
        }
        else {
            Log.i(TAG,"peripheral '%s' not yet disconnected, will not connect "+ getName());
        }
        return true;
    }

    public void close() {
        if (bluetoothGatt == null) return;

        bluetoothGatt.close();
        bluetoothGatt = null;
    }

    public void disconnect() {
        if ( bluetoothGatt == null) {
            Log.w(TAG, "disconnect BluetoothAdapter not initailized.");
            return;
        }
        bluetoothGatt.disconnect();
    }


    public void readCharateristic(BluetoothGattCharacteristic characteristic) {
        if (adapter == null || bluetoothGatt == null) {
            Log.d(TAG, "BluetoothAdapter not initialized");
            return;
        }
        bluetoothGatt.readCharacteristic(characteristic);
    }

    public byte[] getValue(final BluetoothGattCharacteristic characteristic) {
        return characteristic.getValue();
    }

    private byte[] copyOf(byte[] source) {
        if (source == null) return new byte[0];
        final int sourceLength = source.length;
        final byte[] copy = new byte[sourceLength];
        System.arraycopy(source, 0, copy, 0, sourceLength);
        return copy;
    }


    public boolean writeCharacteristic(final BluetoothGattCharacteristic characteristic,@NotNull final byte[] value,final int writeType) {
        if(bluetoothGatt == null){
            Log.e(TAG,"gatt is null");
            return false;
        }
        final byte[] bytesToWrite = copyOf(value);

        int writeProperty;
        switch (writeType){
            case WRITE_TYPE_DEFAULT:
                writeProperty = PROPERTY_WRITE;
                break;
            case WRITE_TYPE_NO_RESPONSE:
                writeProperty = PROPERTY_WRITE_NO_RESPONSE;
                break;
            case WRITE_TYPE_SIGNED:
                writeProperty = PROPERTY_SIGNED_WRITE;
                break;
            default:
                writeProperty = 0;
                break;
        }

        if((characteristic.getProperties() & writeProperty) ==0){
            Timber.e("characteristic <%s> does not support writeType '%s'", characteristic.getUuid(), writeTypeToString(writeType));
            Log.i(TAG,"characteristic " + characteristic.getUuid() +"does not support writeType "+ writeTypeToString(writeType) );
            return false;
        }
        characteristic.setValue(bytesToWrite);
        characteristic.setWriteType(writeType);

        if(!bluetoothGatt.writeCharacteristic(characteristic)){
            Timber.e("writeCharacteristic failed for characteristic: %s", characteristic.getUuid());
            Log.i(TAG,"writeCharacteristic  failed " + characteristic.getUuid() +"does not support writeType "+ writeTypeToString(writeType) );
            return false;
        }
        return true;
    }

    public static BluetoothGattCharacteristic getCharacteristicByUUID(final String uuid) {
        if (userCharacterMap.containsKey(uuid))
            return userCharacterMap.get(uuid);
        return null;
    }


    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, final boolean flag) {
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(CCC_DESCRIPTOR_UUID));
        if(descriptor == null){
            Log.i(TAG,"BluetoothGattDescriptor null ");
            return;
        }
        byte[] value;
        int properties = characteristic.getProperties();
        if ((properties & PROPERTY_NOTIFY) > 0) {
            value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
        } else if ((properties & PROPERTY_INDICATE) > 0) {
            value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE;
        } else {
            Timber.e("characteristic %s does not have notify or indicate property", characteristic.getUuid());
            Log.i(TAG,"characteristic  does not have notify or indicate property :" +characteristic.getUuid().toString());
            return ;
        }
        final byte[] finalValue = flag ? value : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;

        bluetoothGatt.setCharacteristicNotification(characteristic, flag);
        descriptor.setValue( flag ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        boolean result;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            bluetoothGatt.writeDescriptor(descriptor);
        }else{
            // Up to Android 6 there is a bug where Android takes the writeType of the parent characteristic instead of always WRITE_TYPE_DEFAULT
            // See: https://android.googlesource.com/platform/frameworks/base/+/942aebc95924ab1e7ea1e92aaf4e7fc45f695a6c%5E%21/#F0
            Log.i(TAG," Up to Android 6 there is a bug where Android takes the writeType of the parent characteristic instead of ");
            final BluetoothGattCharacteristic parentCharacteristic = descriptor.getCharacteristic();
            final int originalWriteType = parentCharacteristic.getWriteType();
            parentCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            result = bluetoothGatt.writeDescriptor(descriptor);
            parentCharacteristic.setWriteType(originalWriteType);
        }
    }

    public void writeDescriptor(BluetoothGattDescriptor descriptor) {
        bluetoothGatt.writeDescriptor(descriptor);
    }

    public List<BluetoothGattService> getSupportedGattServices() {
        if (bluetoothGatt == null) return null;

        return bluetoothGatt.getServices();
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

    private String writeTypeToString(final int writeType) {
        switch (writeType) {
            case WRITE_TYPE_DEFAULT:
                return "WRITE_TYPE_DEFAULT";
            case WRITE_TYPE_NO_RESPONSE:
                return "WRITE_TYPE_NO_RESPONSE";
            case WRITE_TYPE_SIGNED:
                return "WRITE_TYPE_SIGNED";
            default:
                return "unknown writeType";
        }
    }


}
