package bt.lcy.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * 项目名称: BleReadTool
 * 开发者: yjdwbj
 * 创建时间:  4:27 PM
 */

@Entity(tableName = "characteristic")
public class GattCharacteristic {
    //    create table characteristic (uid integer not null primary key,descriptor text);
    @PrimaryKey
    public int uid;

    @ColumnInfo(name = "descriptor")
    public String descriptor;
}
