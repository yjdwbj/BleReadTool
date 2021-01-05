package bt.lcy.db;

import androidx.room.Dao;
import androidx.room.Query;

/**
 * 项目名称: BleReadTool
 * 开发者: yjdwbj
 * 创建时间:  3:46 PM
 */
@Dao
public interface UsbVendorIdsDao {
    @Query("SELECT * FROM usbvendor WHERE vid=:vid LIMIT 1")
    public UsbVendorIds getCompanyById(int vid);
}
