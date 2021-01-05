package bt.lcy.db;

import androidx.room.Dao;
import androidx.room.Query;

/**
 * 项目名称: BleReadTool
 * 开发者: yjdwbj
 * 创建时间:  4:28 PM
 */

@Dao
public interface GattCharacteristicDao {
    @Query("SELECT * FROM characteristic WHERE uid = :uid LIMIT 1")
    public GattCharacteristic getCharacteristicById(int uid);
}
