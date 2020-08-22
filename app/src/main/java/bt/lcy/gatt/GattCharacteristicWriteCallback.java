package bt.lcy.gatt;

public interface   GattCharacteristicWriteCallback {
    void call(byte[] characteristic, int status);
}
