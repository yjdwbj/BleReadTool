package bt.lcy.gatt.operation;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import java.util.UUID;

import bt.lcy.gatt.GattCharacteristicWriteCallback;

public class GattCharacteristicWriteOperation  extends  GattOperation{
    private final UUID mService;
    private final UUID mCharacteristic;
    private final byte[] mValue;
    private final GattCharacteristicWriteCallback mCallback;
    public GattCharacteristicWriteOperation(BluetoothDevice device, UUID service, UUID characteristic, byte[] value) {
        super(device, OperationType.OPERATION_CHAR_WRITE);
        mService = service;
        mCharacteristic = characteristic;
        mValue = value;
        mCallback = null;
    }
    public GattCharacteristicWriteOperation(BluetoothDevice device, UUID service, UUID characteristic, byte[] value, GattCharacteristicWriteCallback callback) {
        super(device, OperationType.OPERATION_CHAR_WRITE);
        mService = service;
        mCharacteristic = characteristic;
        mValue = value;
        mCallback = callback;
    }
    @Override
    public void execute(BluetoothGatt gatt) {
        Log.d("GattWriteOperation", "writing to " + mCharacteristic);
        BluetoothGattCharacteristic characteristic = gatt.getService(mService).getCharacteristic(mCharacteristic);
        characteristic.setValue(mValue);
        gatt.writeCharacteristic(characteristic);
    }
    @Override
    public boolean hasAvailableCompletionCallback() {
        return true;
    }
    public void onWrite(BluetoothGattCharacteristic characteristic, int status) {
        if (mCallback != null)
            mCallback.call(characteristic.getValue(), status);
    }
}
