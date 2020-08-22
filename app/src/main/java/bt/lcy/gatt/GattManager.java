package bt.lcy.gatt;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import java.lang.reflect.Method;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentHashMap;

import bt.lcy.gatt.operation.GattCharacteristicReadOperation;
import bt.lcy.gatt.operation.GattCharacteristicWriteOperation;
import bt.lcy.gatt.operation.GattDescriptorReadOperation;
import bt.lcy.gatt.operation.GattOperation;

public class GattManager {
    private static final String TAG = GattManager.class.getName();
    private Context mContext;
    private String mServiceUUID;
    private boolean hasService;
    private ConcurrentLinkedQueue<GattOperation> mQueue;
    private ConcurrentHashMap<String, BluetoothGatt> mGatts;
    private GattOperation mCurrentOperation;
    private HashMap<UUID, ArrayList<CharacteristicChangeListener>> mCharacteristicChangeListeners;
    private AsyncTask<Void, Void, Void> mCurrentOperationTimeout;

    private ProgressDialog mDialog;
    private Handler mHandler;
    private Runnable mRunnable;

    public GattManager(Context context, String service_uuid) {

        mContext = context;
        mServiceUUID = service_uuid;
        hasService = false;
        mQueue = new ConcurrentLinkedQueue<>();
        mGatts = new ConcurrentHashMap<>();
        mCurrentOperation = null;
        mCharacteristicChangeListeners = new HashMap<>();

        mDialog = ProgressDialog.show(mContext, "", "Connecting...", true);

        mHandler = new android.os.Handler();
        mRunnable = new Runnable() {
            public void run() {
                Log.i(TAG, "10000 milliseconds BLE Connection timeout");
                if (!hasService) {
                    ((AppCompatActivity)mContext).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mDialog.dismiss();
                            mHandler.removeCallbacks(mRunnable);
                            returnToDeviceSelection("BLE Connection Timed out");
                        }
                    });
                }
            }
        };
        mHandler.postDelayed(mRunnable, 10000);
    }

    public synchronized void cancelCurrentOperationBundle() {
        Log.v("GattManager", "Cancelling current operation. Queue size before: " + mQueue.size());
        if(mCurrentOperation != null && mCurrentOperation.getBundle() != null) {
            for(GattOperation op : mCurrentOperation.getBundle().getOperations()) {
                mQueue.remove(op);
            }
        }
        Log.v("GattManager", "Queue size after: " + mQueue.size());
        mCurrentOperation = null;
        drive();
    }

    public synchronized void connectToDevice(final BluetoothDevice device, final boolean requestMtu) {
        BluetoothGatt temp_gatt = device.connectGatt(mContext, true, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(final BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);

                if (status == 133) {
                    Log.e("GattManager", "Got the status 133 bug, closing gatt");
                    gatt.close();
                    mGatts.remove(device.getAddress());
                    ((AppCompatActivity)mContext).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mDialog.dismiss();
                            mHandler.removeCallbacks(mRunnable);
                            ((AppCompatActivity) mContext).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    returnToDeviceSelection("Device disconnected.");
                                }
                            });
                        }
                    });
                    return;
                }

                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i("GattManager", "Gatt connected to device " + device.getAddress());
                    ((AppCompatActivity)mContext).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            gatt.discoverServices();
                        }
                    });
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.i("GattManager", "Disconnected from gatt server " + device.getAddress() + ", newState: " + newState);
                    mGatts.remove(device.getAddress());
                    setCurrentOperation(null);
                    gatt.close();
                    drive();
                    ((AppCompatActivity)mContext).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mDialog.dismiss();
                            mHandler.removeCallbacks(mRunnable);
                            ((AppCompatActivity) mContext).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    returnToDeviceSelection("Device disconnected.");
                                }
                            });
                        }
                    });
                }
            }

            @Override
            public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                super.onDescriptorRead(gatt, descriptor, status);
                ((GattDescriptorReadOperation) mCurrentOperation).onRead(descriptor);
                setCurrentOperation(null);
                drive();
            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                super.onDescriptorWrite(gatt, descriptor, status);
                setCurrentOperation(null);
                drive();
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicRead(gatt, characteristic, status);
                ((GattCharacteristicReadOperation) mCurrentOperation).onRead(characteristic);
                setCurrentOperation(null);
                drive();
            }

            @Override
            public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                Log.d("GattManager", "services discovered, status: " + status);

                ((AppCompatActivity)mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mDialog.dismiss();
                        mHandler.removeCallbacks(mRunnable);
                    }
                });

                List<BluetoothGattService> services = gatt.getServices();

                for (BluetoothGattService s : services) {
                    if (s.getUuid().toString().equals(mServiceUUID)) {
                        hasService = true;
                    }
                }
                if (!hasService) {
                    ((AppCompatActivity)mContext).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            gatt.close();
                            returnToDeviceSelection("Selected device does not contain the expected BLE services/characteristics. Make sure correct firmware is programmed and the appropriate device is being selected.");
                        }
                    });
                }
                else {
                    mGatts.put(device.getAddress(), gatt);
                    if (requestMtu) {
                        gatt.requestMtu(512);
                    }
                    execute(gatt, mCurrentOperation);
                }
            }


            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicWrite(gatt, characteristic, status);
                Log.d("GattManager", "Characteristic " + characteristic.getUuid() + "written to on device " + device.getAddress());
                if (mCurrentOperation != null && mCurrentOperation.type() == GattOperation.OperationType.OPERATION_CHAR_WRITE)
                    ((GattCharacteristicWriteOperation) mCurrentOperation).onWrite(characteristic, status);
                setCurrentOperation(null);
                drive();
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);
//                    Log.d("GattManager", "Characteristic " + characteristic.getUuid() + "was changed, device: " + device.getAddress());
                if (mCharacteristicChangeListeners.containsKey(characteristic.getUuid())) {
                    for (CharacteristicChangeListener listener : mCharacteristicChangeListeners.get(characteristic.getUuid())) {
                        listener.onCharacteristicChanged(device.getAddress(), characteristic);
                    }
                }
            }
        });

        boolean refreshed = false;
        while (!refreshed) {
            refreshed = refreshDeviceCache(temp_gatt);
            Log.d("GattManager", "Refreshing device cache");
        }
        Log.d("GattManager", "Device Cache Refreshed: " + refreshed);
    }

    public synchronized void queue(GattOperation gattOperation) {
        mQueue.add(gattOperation);
        Log.v("GattManager", "Queueing Gatt operation, size will now become: " + mQueue.size());
        drive();
    }
    private synchronized void drive() {
        if(mCurrentOperation != null) {
            Log.e("GattManager", "tried to drive, but currentOperation was not null, " + mCurrentOperation);
            return;
        }
        if( mQueue.size() == 0) {
            Log.v("GattManager", "Queue empty, drive loop stopped.");
            mCurrentOperation = null;
            return;
        }

        final GattOperation operation = mQueue.poll();
        Log.v("GattManager", "Driving Gatt queue, size will now become: " + mQueue.size());
        setCurrentOperation(operation);


        if(mCurrentOperationTimeout != null) {
            mCurrentOperationTimeout.cancel(true);
        }
        mCurrentOperationTimeout = new AsyncTask<Void, Void, Void>() {
            @Override
            protected synchronized Void doInBackground(Void... voids) {
                try {
                    Log.v("GattManager", "Starting to do a background timeout");
                    wait(operation.getTimoutInMillis());
                } catch (InterruptedException e) {
                    Log.v("GattManager", "was interrupted out of the timeout");
                }
                if(isCancelled()) {
                    Log.v("GattManager", "The timeout was cancelled, so we do nothing.");
                    return null;
                }
                Log.v("GattManager", "Timeout ran to completion, time to cancel the entire operation bundle. Abort, abort!");
                cancelCurrentOperationBundle();
                return null;
            }

            @Override
            protected synchronized void onCancelled() {
                super.onCancelled();
                notify();
            }
        }.execute();

        final BluetoothDevice device = operation.getDevice();
        if(mGatts.containsKey(device.getAddress())) {
            execute(mGatts.get(device.getAddress()), operation);
        } else {
//            connectToDevice(device);
            Log.e("GattManager", "Bluetooth Device not yet connected. Call GattManager.connectToDevice(btDevice) first.");
        }
    }
    private void execute(BluetoothGatt gatt, GattOperation operation) {
        if(operation != mCurrentOperation) {
            return;
        }
        operation.execute(gatt);
        if(!operation.hasAvailableCompletionCallback()) {
            setCurrentOperation(null);
            drive();
        }
    }
    public synchronized void setCurrentOperation(GattOperation currentOperation) {
        mCurrentOperation = currentOperation;
    }
    public BluetoothGatt getGatt(BluetoothDevice device) {
        return mGatts.get(device);
    }
    public void addCharacteristicChangeListener(UUID characteristicUuid, CharacteristicChangeListener characteristicChangeListener) {
        if(!mCharacteristicChangeListeners.containsKey(characteristicUuid)) {
            mCharacteristicChangeListeners.put(characteristicUuid, new ArrayList<CharacteristicChangeListener>());
        }
        mCharacteristicChangeListeners.get(characteristicUuid).add(characteristicChangeListener);
    }
    public void queue(GattOperationBundle bundle) {
        for(GattOperation operation : bundle.getOperations()) {
            queue(operation);
        }
    }
    public class ConnectionStateChangedBundle {
        public final int mNewState;
        public final String mAddress;
        public ConnectionStateChangedBundle(String address, int newState) {
            mAddress = address;
            mNewState = newState;
        }
    }

    public HashMap<UUID, ArrayList<CharacteristicChangeListener>> getCharacteristicChangeListeners() {
        return mCharacteristicChangeListeners;
    }

    public void close(BluetoothDevice device) {
        if (mGatts.containsKey(device.getAddress())) {
            mGatts.get(device.getAddress()).close();
            mGatts.remove(device.getAddress());
        }
    }


    private boolean refreshDeviceCache(BluetoothGatt gatt){
        try {
            Log.d("ProjectZero", "Refreshing device cache");
            BluetoothGatt localBluetoothGatt = gatt;
            Method localMethod = localBluetoothGatt.getClass().getMethod("refresh", new Class[0]);
            if (localMethod != null) {
                boolean bool = ((Boolean) localMethod.invoke(localBluetoothGatt, new Object[0])).booleanValue();
                return bool;
            }
        }
        catch (Exception localException) {
            Log.e("ProjectZero", "An exception occured while refreshing device");
        }
        return false;
    }

    private void returnToDeviceSelection(String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setMessage(msg)
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
//                        ((AppCompatActivity)mContext).getSupportFragmentManager().popBackStack();
                        ((AppCompatActivity)mContext).onBackPressed();
                        ((AppCompatActivity)mContext).getParent().finish();
                        ((AppCompatActivity)mContext).onBackPressed();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }
}
