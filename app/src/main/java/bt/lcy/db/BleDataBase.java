package bt.lcy.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * 项目名称: BleReadTool
 * 开发者: yjdwbj
 * 创建时间:  3:47 PM
 */

@Database(entities = {UsbVendorIds.class, GattCharacteristic.class}, version = 1)
public abstract class BleDataBase extends RoomDatabase {
    // https://developer.android.com/training/data-storage/room/prepopulate
    // https://www.vogella.com/tutorials/AndroidSQLite/article.html
    private static final String DB_NAME = "ble_db";
    private static BleDataBase instance;

    public abstract UsbVendorIdsDao UsbVendorIdsDao();

    public abstract GattCharacteristicDao GattCharacteristicDao();

    public static synchronized BleDataBase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(), BleDataBase.class, DB_NAME)
                    .createFromAsset("databases/ble_db")
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build();
        }
        return instance;
    }

    public static void destroyInstance() {
        instance = null;
    }


    // create table and import data from csv
    //
    // echo -e ".separator |\ncreate table characteristic (uid integer not null primary key,descriptor text);
    // \ncreate table usbvendor (vid integer not null primary key,company text);\n.import /home/michael/usbvendor.csv usbvendor
    // \n.import /home/michael/characteristic.csv characteristic" | sqlite3 ble_db

    // cat ble_raw.data
    // [...]
    //    GATT Characteristic and Object Type 0x2B82 Volume Offset Control Point
    //    GATT Characteristic and Object Type 0x2B83 Audio Output Description
    //    GATT Characteristic and Object Type 0x2B8E Device Time Feature
    //    GATT Characteristic and Object Type 0x2B8F Device Time Parameters
    //    GATT Characteristic and Object Type 0x2B90 Device Time
    //    GATT Characteristic and Object Type 0x2B91 Device Time Control Point
    //    GATT Characteristic and Object Type 0x2B92 Time Change Log Data
    // [...]

    // grep '^GATT'  ble_raw.data | grep -v '^$' | sed 's/[0-9A-F]\{4\}/;&;/g'| awk  -F ';' '{print strtonum("0x"$2)"|"$3}' > ~/characteristic.csv

}
