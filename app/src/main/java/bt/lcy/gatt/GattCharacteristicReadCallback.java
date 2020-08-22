package bt.lcy.gatt;

public interface GattCharacteristicReadCallback {
    void call(byte[] characteristic);
}
