package bt.lcy.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * 项目名称: BleReadTool
 * 开发者: yjdwbj
 * 创建时间:  3:44 PM
 */
@Entity(tableName = "usbvendor")
public class UsbVendorIds {
    // create table usbvendor (vid integer not null primary key,company text);
    @PrimaryKey
    public int vid;

    @ColumnInfo(name = "company")
    public String company;

}
