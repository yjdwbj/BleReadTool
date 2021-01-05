package bt.lcy.gatt.operations;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;

import bt.lcy.gatt.GattOperationBundle;

public abstract  class GattOperation {
    private static final int DEFAULT_TIMEOUT_IN_MILLIS = 10000;
    private BluetoothDevice mDevice;
    private GattOperationBundle mBundle;
    private OperationType mType;
    public GattOperation(BluetoothDevice device, OperationType type) {
        mDevice = device;
        mType = type;
    }
    public abstract void execute(BluetoothGatt bluetoothGatt);
    public BluetoothDevice getDevice() {
        return mDevice;
    }
    public int getTimoutInMillis() {
        return DEFAULT_TIMEOUT_IN_MILLIS;
    }
    public abstract boolean hasAvailableCompletionCallback();
    public GattOperationBundle getBundle() {
        return mBundle;
    }
    public void setBundle(GattOperationBundle bundle) {
        mBundle = bundle;
    }
    public OperationType type() {return mType;}
    public enum OperationType {
        OPERATION_CHAR_READ,
        OPERATION_CHAR_WRITE,
        OPERATION_DESC_READ,
        OPERATION_DESC_WRITE,
        OPERATION_SET_NOTIFICATION
    }
}
