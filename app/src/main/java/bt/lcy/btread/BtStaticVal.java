package bt.lcy.btread;

import java.util.HashMap;

/*
 * Create by michael on 6/17/18
 */

public class BtStaticVal {


    /**


     *     Device Name	00002a00-0000-1000-8000-00805f9b34fb
     Appearance	00002a01-0000-1000-8000-00805f9b34fb
     Peripheral Privacy Flag	00002a02-0000-1000-8000-00805f9b34fb
     Reconnection Address	00002a03-0000-1000-8000-00805f9b34fb
     Peripheral Preferred Connection Parameters	00002a04-0000-1000-8000-00805f9b34fb
     Service Changed	00002a05-0000-1000-8000-00805f9b34fb
     System ID	00002a23-0000-1000-8000-00805f9b34fb
     Model Number String	00002a24-0000-1000-8000-00805f9b34fb
     Serial Number String	00002a25-0000-1000-8000-00805f9b34fb
     Firmware Revision String	00002a26-0000-1000-8000-00805f9b34fb
     Hardware Revision String	00002a27-0000-1000-8000-00805f9b34fb
     Software Revision String	00002a28-0000-1000-8000-00805f9b34fb
     Manufacturer Name String	00002a29-0000-1000-8000-00805f9b34fb
     IEEE 11073-20601 Regulatory Certification Data List	00002a2a-0000-1000-8000-00805f9b34fb
     Battery Level	00002a19-0000-1000-8000-00805f9b34fb

     */


    public static String UUID_KEY_DATA ="0000fff0-0000-1000-8000-00805f9b34fb";

    public static String UUID_CHAR1 = "0000fff1-0000-1000-8000-00805f9b34fb";
    public static String UUID_CHAR2 = "0000fff2-0000-1000-8000-00805f9b34fb";
    public static String UUID_CHAR3 = "0000fff3-0000-1000-8000-00805f9b34fb";
    public static String UUID_CHAR4 = "0000fff4-0000-1000-8000-00805f9b34fb";
    public static String UUID_CHAR5 = "0000fff5-0000-1000-8000-00805f9b34fb";
    public static String UUID_CHAR6 = "0000fff6-0000-1000-8000-00805f9b34fb";
    public static String UUID_CHAR7 = "0000fff7-0000-1000-8000-00805f9b34fb";
    public static String UUID_CHAR8 = "0000fff8-0000-1000-8000-00805f9b34fb";
    public static String UUID_CHAR9 = "0000fff9-0000-1000-8000-00805f9b34fb";
    public static String UUID_CHARA = "0000fffa-0000-1000-8000-00805f9b34fb";

    public static String SYSTEM_ID=  "00002a23-0000-1000-8000-00805f9b34fb";

    // services
    public static final String GENERIC_ACCESS_UUID = "00001800";
    public static final String GENERIC_ATTRIBUTE_UUID = "00001801";
    public static final String IMMEDIATE_ALERT = "00001802";
    public static final String LINK_LOSS = "00001803";
    public static final String CURRENT_TIME_SERVICE = "00001805";

    public static final String BLOOD_PRESSURE = "00001810";
    public static final String ALERT_NOTIFICATION_SERVICE = "00001811";
    public static final String CYCLING_SPEED_AND_CADENCE = "00001816";
    public static final String CYCLING_POWER = "00001818";
    public static final String DEVICE_INFORMATION_SERVICE = "0000180a";
    public static final String BATTERY_SERVICE="0000180f";
    public static final String HEART_RATE="0000180d";
    public static final String USER_DATA="0000181c";
    public static final String ENVIRONMENTAL_SENSING ="0000181a";





    private final static HashMap<String,Integer> charactersMap ;
    private final static HashMap<String,Integer> serviceMap;

    static {
        serviceMap = new HashMap<>();
        serviceMap.put(GENERIC_ACCESS_UUID,R.string.generic_access);
        serviceMap.put(GENERIC_ATTRIBUTE_UUID,R.string.generic_attribute);
        serviceMap.put(IMMEDIATE_ALERT,R.string.immediate_alert);
        serviceMap.put(DEVICE_INFORMATION_SERVICE,R.string.device_information_service);
        serviceMap.put(LINK_LOSS,R.string.link_loss);
        serviceMap.put(CURRENT_TIME_SERVICE,R.string.current_time_service);
        serviceMap.put(BLOOD_PRESSURE,R.string.blood_pressure);
        serviceMap.put(ALERT_NOTIFICATION_SERVICE,R.string.alert_notification_service);
        serviceMap.put(CYCLING_SPEED_AND_CADENCE,R.string.cycling_speed_and_cadence);
        serviceMap.put(CYCLING_POWER,R.string.cycling_power);
        serviceMap.put(BATTERY_SERVICE,R.string.battery_service);
        serviceMap.put(HEART_RATE,R.string.heart_rate);
        serviceMap.put(USER_DATA,R.string.user_data);
        serviceMap.put(ENVIRONMENTAL_SENSING,R.string.environmental_sensing);

    }

    // characteristics

    public static final String SRV_DEVICE_NAME = "00002a00";
    public static final String SRV_APPEARANCE = "00002a01";
    public static final String SRV_PERIPHERAL_PRIVACY_FLAG = "00002a02";
    public static final String SRV_RECONNECTION_ADDRESS = "00002a03";
    public static final String SRV_PERIPHERAL_PREFERRED_CONNECTION_PARAMETERS  = "00002a04";
    public static final String SRV_SERVICE_CHANGED = "00002a05";
    public static final String SRV_BATTERY_LEVEL = "00002a19";
    public static final String SRV_SYSTEM_ID = "00002a23";
    public static final String SRV_MODEL_NUMBER = "00002a24";
    public static final String SRV_SERIAL_NUMBER = "00002a25";
    public static final String SRV_FIRMWARE_REVISION = "00002a26";
    public static final String SRV_HARDWARE_REVISION = "00002a27";
    public static final String SRV_SOFTWARE_REVISION = "00002a28";
    public static final String SRV_MANUFACTURER_NAME = "00002a29";
    public static final String SRV_IEEE_11073 = "00002a2a";
    public static final String SRV_PNP_ID = "00002a50";





    static {
        charactersMap = new HashMap<>();
        charactersMap.put(SRV_DEVICE_NAME,R.string.device_name);
        charactersMap.put(SRV_APPEARANCE,R.string.appearance);
        charactersMap.put(SRV_PERIPHERAL_PRIVACY_FLAG,R.string.peripheral_privacy_flag);
        charactersMap.put(SRV_RECONNECTION_ADDRESS,R.string.reconnection_address);
        charactersMap.put(SRV_PERIPHERAL_PREFERRED_CONNECTION_PARAMETERS,R.string.ppcp);
        charactersMap.put(SRV_SERVICE_CHANGED,R.string.service_changed);
        charactersMap.put(SRV_SYSTEM_ID,R.string.systemid);
        charactersMap.put(SRV_MODEL_NUMBER,R.string.model_number);
        charactersMap.put(SRV_SERIAL_NUMBER,R.string.serial_number);
        charactersMap.put(SRV_FIRMWARE_REVISION,R.string.firmware_revision);
        charactersMap.put(SRV_HARDWARE_REVISION,R.string.hardware_revision);
        charactersMap.put(SRV_SOFTWARE_REVISION,R.string.software_revision);
        charactersMap.put(SRV_MANUFACTURER_NAME,R.string.manufacturer_name);
        charactersMap.put(SRV_IEEE_11073,R.string.ieee_11073);
        charactersMap.put(SRV_BATTERY_LEVEL,R.string.battery_level);
        charactersMap.put(SRV_PNP_ID,R.string.pnp_id);
    }

    static public int getCharacteristics(final String key)
    {
        if(charactersMap.containsKey(key))
            return charactersMap.get(key);
        else
            return R.string.unknow_characteristic;
    }

    static public  int getServices(final String key)
    {
        if(serviceMap.containsKey(key))
            return serviceMap.get(key);
        else
            return R.string.unknow_service;
    }
}
