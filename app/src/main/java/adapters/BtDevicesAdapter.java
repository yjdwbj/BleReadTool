package adapters;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.le.ScanResult;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.os.Build;
import android.provider.Settings;
import android.text.Html;
import android.view.MotionEvent;
import android.view.ViewGroup.LayoutParams;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothGattCallback;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.util.Log;
import android.widget.BaseExpandableListAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.w3c.dom.Text;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import bt.lcy.btread.AmoMcuBoardActivity;
import bt.lcy.btread.BtDeviceServicesActivity;
import bt.lcy.btread.BtService;
import bt.lcy.btread.BtStaticVal;
import bt.lcy.btread.BuildConfig;
import bt.lcy.btread.MainActivity;
import bt.lcy.btread.R;
import bt.lcy.btread.TiMsp432ProjectZeroActivity;

import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static bt.lcy.btread.BtStaticVal.BT_DEVICE;
import static bt.lcy.btread.BtStaticVal.BT_GATTMAN;
import static bt.lcy.btread.BtStaticVal.BT_SRVUUID;
import static bt.lcy.btread.BtStaticVal.getFrom16bitUUID;

public class BtDevicesAdapter extends BaseExpandableListAdapter {
    private static String TAG = BtDevicesAdapter.class.getName();
    //https://www.journaldev.com/9942/android-expandablelistview-example-tutorial
    private final LayoutInflater inflater;
    private final static ArrayList<BluetoothDevice> btDevices = new ArrayList<BluetoothDevice>();
    private final static HashMap<BluetoothDevice, Object[]> deviceMap = new HashMap<BluetoothDevice, Object[]>();

    private final static ArrayList<String> mDeviceType = new ArrayList<>(Arrays.asList(
            // DEVICE_TYPE_UNKNOWN,DEVICE_TYPE_CLASSIC,DEVICE_TYPE_LE,DEVICE_TYPE_DUAL
            "Unknown", "BR/EDR devices", "LE-only", "BR/EDR/LE"
    ));

    private final static ArrayList<String> mFlagType = new ArrayList<>(Arrays.asList(
            "LimitedDiscoverableMode",
            "GeneralDiscoverableMode",
            "BrEdrNotSupported",
            "SimultaneousLEandBR/EDRtoSameDeviceCapa-ble(Controller)",
            "SimultaneousLEandBR/EDRtoSameDeviceCapa-ble(Host)"
    ));

         final  HashMap<Integer,String> mBeaconDeviceType = new HashMap<Integer,String>(){{
             put(1,"Xbox One");
             put(6,"Apple iPhone");
             put(7,"Apple iPad");
             put(8,"Android device");
             put(9,"Windows 10 Desktop");
             put(11,"Windows 10 Phone");
             put(12,"Linus device");
             put(13,"Windows IoT");
             put(14,"Surface Hub");
        }};


    private String getBeaconDeviceType(int index){
        if(mBeaconDeviceType.containsKey(index))
        {
            return mBeaconDeviceType.get(index);
        }

        return String.format("unknown (0x%04X)",index);
    }


    private boolean showProgressBar = false;

    private int showPBarPosition = -1;
    private Context mContext;
    private ExpandableListView mExpandableListView;

    public BtDevicesAdapter(Context context, ExpandableListView expandableListView) {
        inflater = LayoutInflater.from(context);
        mContext = context;
        mExpandableListView = expandableListView;
    }


    public static BluetoothDevice getBleDevice(int index) {
        return btDevices.get(index);
    }


    public static void addBleDevice(BluetoothDevice device) {
        btDevices.add(device);
    }

    public static Object[] getAdvertisingData(BluetoothDevice device) {
        return deviceMap.get(device);
    }

    public static void clearAdvertisingData() {
        deviceMap.clear();
    }

    public static void clearDevicesData() {
        btDevices.clear();
    }

    // https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-cdp/77b446d0-8cea-4821-ad21-fabdf4d9a569?redirectedfrom=MSDN
    public class AdvertisingBeancon {
        short id;        // Microsoft ID (2 bytes):
        byte scenario;   // Scenario Type (1 byte)
        byte devtype_ver;  //  Version and Device Type (1 byte);
        byte verflags; // Version and Flags (1 byte): The high 3 bits are set to 001; the lower 3 bits to 00000.
        byte reservd; // Reserved (1 byte): Currently set to zero.
        byte salt[];  // Salt (4 bytes): Four random bytes.
        byte hash[];  // Device Hash (24 bytes): SHA256 Hash of Salt plus Device Thumbprint. Truncated to 16 bytes.
    }

    public class ChunkData {
        int size;
        byte type;
        byte[] data;
    }


    public static class BriefData {
        int mRssi;
        int mCompany;
        long mStime;
        int mPeriodicAdvertisingInterval;
        boolean isConnectable;
        String mAdvContext;
        ArrayList<ChunkData> mChunks;

        BriefData() {
            mChunks = new ArrayList<>();
        }

        public void addChunk(ChunkData chunk) {
            mChunks.add(chunk);
        }

        public ChunkData getChunk(int index) {
            return mChunks.get(index);
        }

        public short get16bitUUID() {
            for (final ChunkData chunkData : mChunks) {
                if (chunkData.type == 0x2 || chunkData.type == 0x3) {
                    return (short) ((chunkData.data[1] << 8 | (0xff & chunkData.data[0])) & 0xffff);
                }
            }
            return 0;
        }
    }

    public static String getHexData(byte[] data) {
        StringBuilder hex = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            hex.append(String.format("%02X", data[i]));
        }
        return hex.toString();
    }

    public static int getUUID16(byte[] data) {
        return ((data[1] << 8 | (0xff & data[0])) & 0xffff);
    }

    public static int getUUID16(final UUID uuid) {
        return  (int)((uuid.getMostSignificantBits() >> 32) & 0xffff);
    }

    private String getFlags(byte flag) {
        String flagString = "";
        for (int n = 0; n < mFlagType.size(); n++) {
            if ((flag & (1 << n)) > 0) {
                flagString = flagString + mFlagType.get(n) + ",";
            }
        }

        return  flagString.isEmpty() ? flagString : flagString.substring(0, flagString.length() - 1);
    }

    private void showRawData(BluetoothDevice device) {
        Object[] val = deviceMap.get(device);
        BriefData briefData = (BriefData) val[0];
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext, AlertDialog.THEME_HOLO_DARK);
        String rawString = getHexData((byte[]) val[1]);

        WebView webView = new WebView(mContext);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDefaultTextEncodingName("utf-8");
        webView.setBackgroundColor(R.color.colorPrimary);


        if (Build.VERSION.SDK_INT >= 19) {
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        } else {
            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
        String rawdata = String.format("RawData:<br><textarea readonly style=\"background-color:#303F9F;color:gray;\">0x%s</textarea>Details:<br>", rawString);
        String statement = "<br>LEN. -length of EIR packet(Type+Data)in bytes,<br>TYPE-the data type as in <a href=\"https://www.blueoth.com/\">https://www.blueoth.com/</a>";
        String css = "<style>table,textarea{border-collapse:collapse;table-layout:fixed;width:100%;border:1px solid gray;}table td,textarea{word-wrap:break-word;}</style>";
        String html = String.format("<html><head>%s</head><body style=\"color:gray;\">%s<table border='1'><tr><th width='40px'>LEN.</th><th width='45px'>TYPE</th><th >VALUE</th></tr>",
                rawdata, css);

        for (int i = 0; i < briefData.mChunks.size(); i++) {
            ChunkData chunkData = briefData.getChunk(i);
            html += String.format("<tr><td>0x%d</td><td>0x%02X</td><td>0x%s</td></tr>",
                    chunkData.size, chunkData.type, getHexData(chunkData.data));
        }
        html += String.format("</table>%s</body></html>", statement);
//        Log.d(TAG, "Html: " + html);

        webView.loadData(html, "text/html; charset=utf-8", null);
        builder.setView(webView)
                .setPositiveButton("OK", null)
                .setNegativeButton("COPY", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ClipboardManager clipboard = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("rawdata", rawString);
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(mContext,
                                "已经复制rawdata到剪贴板",
                                Toast.LENGTH_SHORT).show();
                    }
                });


        AlertDialog dialog = builder.create();
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.show();
    }

    private String parseMicrsoftBeacon(byte[] data) {
        int pos = 2;
        String beaconString = String.format("<font color=\"black\">Scenario Type: Advertising Beacon <%02X><br> </font> ", data[pos]);
        pos++;
        //   The high two bits are set to 00 for the version number
        beaconString += String.format("<font color=\"black\">Version: %d<br></font>", ((data[pos] & 0xc0) >> 6) & 0xff);


        // the lower6 bits are set to Device Type
        byte index = (byte)(data[pos] & 0x3f);
        Log.i(TAG,"pos at :" + pos + " Device Type " + (data[pos] & 0x3f) + " index " + index  + " type size " + mBeaconDeviceType.size() );
        beaconString += String.format("<font color=\"black\">Device Type: %04x<br></font>", getBeaconDeviceType((data[pos] & 0x3f) & 0xff));
        pos++;
        // The high 3 bits are is version, the low 3 bits is flags.
        beaconString += String.format("<font color=\"black\">Flags: 0x%02X (version: %d)<br></font>", 0x7 & data[pos],0xe & data[pos] );
        pos++;
        beaconString += String.format("<font color=\"black\">Reserved: %02X<br></font>", data[pos]);
        pos++;
        // Four random bytes
        beaconString += String.format("<font color=\"black\">Salt: 0x%s<br></font>", getHexData(Arrays.copyOfRange(data,pos,pos+4)));
        pos +=4;

        //   SHA256 Hash of Salt plus Device Thumbprint. Truncated to 16 bytes.
        beaconString += String.format("<font color=\"black\">Device Hash: 0x%s<br></font>", getHexData(Arrays.copyOfRange(data,pos,pos+16)));

        return beaconString;

    }

    private void parseChunkData(BriefData briefData, ChunkData chunkData) {

        switch (chunkData.type) {
            // Manufacturer SpecificData = 0xff
            case -1:
                briefData.mAdvContext += String.format("<font color=\"black\">Manufacturer SpecificData (Bluethooth Core 4.1):</font><br>");
                briefData.mCompany = getUUID16(chunkData.data);

                String company = BtStaticVal.getCompany(briefData.mCompany);
                Log.i(TAG,company +" Manufacturer SpecificData  : " + getHexData(chunkData.data) + " size : " + chunkData.data.length);
                if (company.startsWith("未知厂商名称")) {
                    briefData.mAdvContext += String.format("<font color=\"black\">0x%s<br></font>", getHexData(chunkData.data));
                } else {
                    briefData.mAdvContext += String.format("<font color=\"#303F9F\">Company:</font> %s<br>", company);
                    // try decoder beacon data
                    if (chunkData.size == 30) {
                        if(briefData.mCompany == 0x9){
                            briefData.mAdvContext += parseMicrsoftBeacon(chunkData.data);
                        }
                    } else {
                        briefData.mAdvContext += String.format("<font color=\"black\">0x%s<br></font>",
                                getHexData(Arrays.copyOfRange(chunkData.data, 2, chunkData.data.length)));
                    }

                }
                break;

            // flags
            case 0x1:
                briefData.mAdvContext += String.format("<font color=\"#303F9F\">Flags:</font> %s<br>", getFlags(chunkData.data[0]));
                break;

            // Incomplete List of 16-bit Service Class UUIDs
            case 0x2:
                briefData.mAdvContext += String.format("<font color=\"#303F9F\">Incomplete List of 16-bit Service Class UUIDs:</font> 0x%04X<br>",
                        getUUID16(chunkData.data));
                briefData.mAdvContext += String.format("0x%04X<br>", getUUID16(Arrays.copyOfRange(chunkData.data, 2, 4)));
                break;

            //  Complete List of 16-bit Service Class UUIDs
            case 0x3:
                briefData.mAdvContext += String.format("<font color=\"#303F9F\">Complete List of 16-bit Service Class UUIDs:</font> 0x%04X<br>",
                        getUUID16(chunkData.data));
                briefData.mAdvContext += String.format("0x%04X<br>", getUUID16(Arrays.copyOfRange(chunkData.data, 2, 4)));
                break;

            // Complete Local Name
            case 0x9:
                briefData.mAdvContext += String.format("<font color=\"#303F9F\">Complete Local Name:</font> %s<br>", new String(chunkData.data));
                break;

            // Tx Power Level
            case 0xa:
                Log.d(TAG, " Tx Power Level type " + chunkData.type + " size : " + chunkData.size + String.format(" data 0x%S", getHexData(chunkData.data)));
                briefData.mAdvContext += String.format("<font color=\"#303F9F\">Tx Power Level:</font> %ddBm<br>", chunkData.data[0]);
                break;

            // Slave Connection Interval RangeThe first 2 octets defines the minimum value forthe connection interval in the following manner:
            // connInterval min = Conn_Interval_Min * 1.25 msConn_Interval_Min range: 0x0006 to 0x0C80Value of 0xFFFF indicates no specific
            // minimum.Values outside the range are reserved. (excluding0xFFFF) .
            // The second 2 octets defines the maximum valuefor the connection interval in the following manner:connInterval max = Conn_Interval_Max * 1.25
            // msConn_Interval_Max range: 0x0006 to 0x0C80Conn_Interval_Max shall be equal to or greaterthan the Conn_Interval_Min.Value of 0xFFFF indicates
            // no specific maximum.Values outside the range are reserved (excluding0xFFFF)
            case 0x12:
                Log.d(TAG, " Slave Connection Interval Range type " + chunkData.type + " size : " + chunkData.size + String.format(" data 0x%S", getHexData(chunkData.data)));
                float first = getUUID16(chunkData.data) * (float) 1.25;
                float second = getUUID16(Arrays.copyOfRange(chunkData.data, 2, 4)) * (float) 1.25;
                briefData.mAdvContext += String.format("<font color=\"#303F9F\">Slave Connection Interval Range:</font> %2.2fms - %2.2fms<br>", first, second);
                break;

            // Service Data
            case 0x16:
                int uuid = getUUID16(chunkData.data);
                if (uuid == 0x1809) {
                    briefData.mAdvContext += String.format("<font color=\"#303F9F\">Temperature:</font> %d°C<br>", chunkData.data[2]);
                } else if (uuid == 0x181a) {
                    briefData.mAdvContext += String.format("<font color=\"#303F9F\">Humidity:</font> %d°F<br>", chunkData.data[2]);
                } else {
                    briefData.mAdvContext += String.format("<font color=\"#303F9F\">Service Data:</font>  UUID: 0x%04X Data: 0x%S <br>",
                            uuid, getHexData(Arrays.copyOfRange(chunkData.data, 2, chunkData.data.length)));
                }
                break;

            // Appearance
            case 0x19:
                // refer https://specificationrefs.bluetooth.com/assigned-values/Appearance%20Values.pdf
                int value = getUUID16(chunkData.data);
                briefData.mAdvContext += String.format("<font color=\"#303F9F\">Appearance:</font> %s<br>", BtStaticVal.getAppearance(value));
                break;

            default:
                Log.d(TAG, "unsupport!!!! " + chunkData.type + " size : " + chunkData.size + String.format(" data 0x%S", getHexData(chunkData.data)));
        }
    }

    // for Build.VERSION.SDK_INT < 21
    public void addDevice(BluetoothDevice device, final int rssi, final byte[] record) {
        if (!btDevices.contains(device)) {
            int i = 0;
            BriefData briefData = new BriefData();
            briefData.mStime = System.currentTimeMillis();
            briefData.mRssi = rssi;
//            briefData.mPeriodicAdvertisingInterval = result.getPeriodicAdvertisingInterval();
//            briefData.isConnectable = result.isConnectable();

            btDevices.add(device);
            briefData.mAdvContext = String.format(
                    "<font color=\"#303F9F\">Device type:</font> %s<br>",
                    (device.getType() < mDeviceType.size() ? mDeviceType.get(device.getType()) : "InVaild"));

            for (; i < record.length - 1; ) {
                byte size = record[i];
                int vsize = size - 1;
                if (size == 0) {
                    break;
                }
                ChunkData chunkData = new ChunkData();
                briefData.addChunk(chunkData);
                chunkData.size = size;
                i++;
                chunkData.type = record[i];
                i++;
                chunkData.data = Arrays.copyOfRange(record, i, i + vsize);
                parseChunkData(briefData, chunkData);

                i += vsize;
            }

//            if (result.getTxPower() != ScanResult.TX_POWER_NOT_PRESENT) {
//                briefData.mAdvContext += String.format("<font color=\"#303F9F\">Tx Power:</font> %d dBm<br>", result.getTxPower());
//            }
//
//            if (ScanResult.PERIODIC_INTERVAL_NOT_PRESENT != result.getPeriodicAdvertisingInterval()) {
//                briefData.mAdvContext += String.format("<font color=\"#303F9F\">Slave Connection Interval Range:</font> %d ms<br>", result.getPeriodicAdvertisingInterval());
//            }

            // result,rawData
            Object[] val = new Object[]{briefData, Arrays.copyOfRange(record, 0, i)};
            deviceMap.put(device, val);
        } else {
            Object[] val = deviceMap.get(device);
            BriefData briefData = (BriefData) val[0];
            briefData.mStime = System.currentTimeMillis();
            briefData.mRssi = rssi;
//            briefData.mPeriodicAdvertisingInterval = result.getPeriodicAdvertisingInterval();
        }
    }

    //
    public void addDevice(ScanResult result) {
        byte[] record = result.getScanRecord().getBytes();
        BluetoothDevice device = result.getDevice();
        if (!btDevices.contains(device)) {
            int i = 0;
            BriefData briefData = new BriefData();
            briefData.mStime = System.currentTimeMillis();
            briefData.mRssi = result.getRssi();
            briefData.mPeriodicAdvertisingInterval = result.getPeriodicAdvertisingInterval();
            briefData.isConnectable = result.isConnectable();

            btDevices.add(device);
            briefData.mAdvContext = String.format(
                    "<font color=\"#303F9F\">Device type:</font> %s<br><font color=\"#303F9F\">Advertising type:</font> %s<br>",
                    (device.getType() < mDeviceType.size() ? mDeviceType.get(device.getType()) : "InVaild"),
                    result.isLegacy() ? "Legacy" : "unkown");

            for (; i < record.length - 1; ) {
                byte size = record[i];
                if (size == 0) {
                    break;
                }
                ChunkData chunkData = new ChunkData();
                briefData.addChunk(chunkData);
                chunkData.size = size;
                i++;
                chunkData.type = record[i];
                i++;
                chunkData.data = Arrays.copyOfRange(record, i, i + size - 1);
                parseChunkData(briefData, chunkData);
                i += size - 1;
            }

            if (result.getTxPower() != ScanResult.TX_POWER_NOT_PRESENT) {
                briefData.mAdvContext += String.format("<font color=\"#303F9F\">Tx Power:</font> %d dBm<br>", result.getTxPower());
            }

            if (ScanResult.PERIODIC_INTERVAL_NOT_PRESENT != result.getPeriodicAdvertisingInterval()) {
                briefData.mAdvContext += String.format("<font color=\"#303F9F\">Slave Connection Interval Range:</font> %d ms<br>", result.getPeriodicAdvertisingInterval());
            }

            // result,rawData
            Object[] val = new Object[]{briefData, Arrays.copyOfRange(record, 0, i)};
            deviceMap.put(device, val);
        } else {
            Object[] val = deviceMap.get(device);
            BriefData briefData = (BriefData) val[0];

            if (briefData.mRssi != result.getRssi()) {
                briefData.mStime = System.currentTimeMillis();
                briefData.mRssi = result.getRssi();
            }
            briefData.mRssi = result.getRssi();
            briefData.mPeriodicAdvertisingInterval = result.getPeriodicAdvertisingInterval();
        }


    }

    public static BluetoothDevice getDevice(int pos) {
        return btDevices.get(pos);
    }

    public static void clear() {
        btDevices.clear();
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;
    }

    @Override
    public int getChildType(int groupPosition, int childPosition) {
        return super.getChildType(groupPosition, childPosition);
    }

    @Override
    public View getChildView(int listPosition, final int expandedListPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {
        final String expandedListText = (String) getChild(listPosition, expandedListPosition);
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_adv_info, null);
        }
        TextView expandedListTextView = (TextView) convertView
                .findViewById(R.id.expandedListItem);
        expandedListTextView.setText(Html.fromHtml(expandedListText));

        TextView rawData = (TextView) convertView
                .findViewById(R.id.raw_data);

        rawData.setOnTouchListener(new View.OnTouchListener() {
            @Override
            synchronized public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    showRawData(btDevices.get(listPosition));
                }
                return true;
            }
        });

        return convertView;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return 1;
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        Object[] val = deviceMap.get(btDevices.get(groupPosition));
        BriefData briefData = (BriefData) val[0];
        return briefData.mAdvContext;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public void onGroupCollapsed(int groupPosition) {
        super.onGroupCollapsed(groupPosition);
    }

    @Override
    public int getGroupType(int groupPosition) {
        return super.getGroupType(groupPosition);
    }

    @Override
    public int getGroupTypeCount() {
        return super.getGroupTypeCount();
    }

    @Override
    public int getGroupCount() {
        return btDevices.size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return mExpandableListView.getItemAtPosition(groupPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {

        ViewHolder viewHolder;

        final short Thermometer = 0x1809;
        final short EnvironmentalSensor = 0x181a;
        BluetoothDevice device = btDevices.get(groupPosition);
        Object[] val = deviceMap.get(device);
        BriefData briefData = (BriefData) val[0];

        int rssi = 0;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_devices, null);
            // 扫描后每一行的设备摘要信息。
            viewHolder = new ViewHolder();
            viewHolder.deviceAddress = (TextView) convertView.findViewById(R.id.device_address);
            viewHolder.deviceImage = (ImageView) convertView.findViewById(R.id.device_image);
            viewHolder.deviceName = (TextView) convertView.findViewById(R.id.device_name);
            viewHolder.deviceRssi = (TextView) convertView.findViewById(R.id.device_rssi_val);
            viewHolder.deviceRssiImg = (ImageView) convertView.findViewById(R.id.device_rssi_img);
            viewHolder.deviceCompany = (TextView) convertView.findViewById(R.id.device_company);
            viewHolder.deviceConnect = (TextView) convertView.findViewById(R.id.device_connect);
            viewHolder.deviceConnect.setTag(groupPosition);
            convertView.setTag(viewHolder);

        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        if (briefData.isConnectable) {
            viewHolder.deviceConnect.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        connectToSelectedDevice(groupPosition);
                    }
                    return true;
                }
            });
        } else {
            //viewHolder.deviceConnect.setVisibility(View.GONE);
            String srvdata = "";
            for (int i = 0; i < briefData.mChunks.size(); i++) {
                ChunkData chunkData = briefData.getChunk(i);
                if (chunkData.type == 0x16) {
                    int uuid = chunkData.data[1] << 8 | chunkData.data[0];
                    if (uuid == 0x1809) {
                        srvdata += String.format(" %d°C ", chunkData.data[2]);
                    }
                    if (uuid == 0x181a) {
                        srvdata += String.format(" %d°F ", chunkData.data[2]);
                    }
                }
            }
            viewHolder.deviceConnect.setText(srvdata);
            viewHolder.deviceConnect.setBackground(null);
        }


        final boolean f = showProgressBar && convertView.isSelected();


        final String deviceName = device.getName();
        if (deviceName != null && deviceName.length() > 0) {
            viewHolder.deviceName.setText(deviceName);
        } else {
            viewHolder.deviceName.setText(R.string.unknown_devices);
        }

        viewHolder.deviceAddress.setText(device.getAddress());

        long gap = System.currentTimeMillis() - briefData.mStime;
        viewHolder.deviceCompany.setText(BtStaticVal.getCompany(briefData.mCompany));
        float sigStrenght = (briefData.mRssi + 100) * 2;
        String text = gap > 1000 ? (gap / 1000) + "s" : gap + "ms";
        viewHolder.deviceRssi.setText(" " + briefData.mRssi + " dBm <-> " + text);

        if (sigStrenght < 20) {
            viewHolder.deviceRssiImg.setImageResource(R.drawable.ic_signal_cellular_0_bar_24px);
        } else if (sigStrenght < 40) {
            viewHolder.deviceRssiImg.setImageResource(R.drawable.ic_signal_cellular_1_bar_24px);
        } else if (sigStrenght < 60) {
            viewHolder.deviceRssiImg.setImageResource(R.drawable.ic_signal_cellular_2_bar_24px);
        } else if (sigStrenght < 80) {
            viewHolder.deviceRssiImg.setImageResource(R.drawable.ic_signal_cellular_3_bar_24px);
        } else {
            viewHolder.deviceRssiImg.setImageResource(R.drawable.ic_signal_cellular_4_bar_24px);
        }

        return convertView;
    }

    private void connectToSelectedDevice(int groupPosition) {
        ProgressDialog dialog;
        BluetoothDevice mDevice = btDevices.get(groupPosition);
        Object[] val = deviceMap.get(mDevice);
        BriefData briefData = (BriefData) val[0];
        MainActivity mainActivity = (MainActivity) mContext;
        Bundle bundle = new Bundle();
        bundle.putParcelable(BT_DEVICE, mDevice);
        Map<Integer, Runnable> supportBoardView = new HashMap<>();

        supportBoardView.put(0, () -> {
            final Intent intent = new Intent(mContext, BtDeviceServicesActivity.class);
            intent.putExtras(bundle);
            intent.putExtra(BT_SRVUUID, getFrom16bitUUID(briefData.get16bitUUID()).toString());
            mainActivity.startActivity(intent);
        });

        supportBoardView.put(1, () -> {
            //打开AmoMcu开发板独立页面.
            //AmoMcuBoardActivity.setBtService(btService);
            final Intent intent = new Intent(mContext, AmoMcuBoardActivity.class);
            intent.putExtras(bundle);
            mainActivity.startActivity(intent);
        });

        supportBoardView.put(2, () -> {
            // 打开Ti msp432 ProjectZero开发板。
            // 允许其他应用启动您的 Activity https://developer.android.com/training/basics/intents/filters
            Intent intent = new Intent(mContext, TiMsp432ProjectZeroActivity.class);
            //   Parcelable 和 Bundle ,Activity 之间发数据。
            //   https://developer.android.com/guide/components/activities/parcelables-and-bundles
            intent.putExtras(bundle);
            intent.putExtra(BT_SRVUUID, getFrom16bitUUID(briefData.get16bitUUID()).toString());
            Log.i(TAG, "start Ti ProjectZero!!!!!!!!!!!!!!1, " + intent.toString());
//                                    finishActivity(0);
            mainActivity.startActivity(intent);
        });

        // 连接所选的设备。
        if (mainActivity.isScanning()) {
            mainActivity.stopScan();
        }

        dialog = ProgressDialog.show(mContext, "",
                "连接 " + mDevice.getName() + ",稍等...", true);
        // 这里是为了检查目标的设备有那些具体的服务与特征，根据不同的特征服务来使用不同的界面，如果只针对某一特定的设备，可以省略这一步。
        Log.i(TAG, "!!!!!-------------> uuid is: " + mDevice.getUuids() + " status ");
        mDevice.connectGatt(mContext.getApplicationContext(), false, new BluetoothGattCallback() {

            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                if (status == GATT_SUCCESS) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        //开启查找服务
                        gatt.discoverServices();
                    }
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                BtService.mDeviceServices = gatt.getServices();
                int devType = 0;
                if (status == GATT_SUCCESS) {
                    if (briefData.mCompany == 0x000D & TiMsp432ProjectZeroActivity.isVaildUUID()) {
                        devType = 2;
                    } else if (mDevice.getName().startsWith("AmoSmartRF")) {
                        devType = 0;
                    }
                    supportBoardView.get(devType).run();
                    dialog.dismiss();
//                    gatt.disconnect();
//                    gatt.close();
                }
            }
        });
    }


    public static class ViewHolder {
        ImageView deviceImage;
        TextView deviceName;
        TextView deviceAddress;
        TextView deviceRssi;
        TextView deviceCompany;
        ImageView deviceRssiImg;
        TextView deviceConnect;
    }
}
