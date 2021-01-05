package bt.lcy.btread;

import bt.lcy.btread.BuildConfig;

import android.provider.Settings;
import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;

import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import adapters.BtDevicesAdapter;
import timber.log.Timber;

import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_NOTIFY;
import static bt.lcy.btread.BtStaticVal.BT_DEVICE;

/**
 * BLE 广播数据包。
 * https://www.silabs.com/community/wireless/bluetooth/knowledge-base.entry.html/2017/02/10/bluetooth_advertisin-hGsf
 * https://silabs-prod.adobecqms.net/content/usergenerated/asi/cloud/content/siliconlabs/en/community/wireless/bluetooth/knowledge-base/jcr:content/content/primary/blog/bluetooth_advertisin-zCHh.social.0.10.html
 * 蓝牙协议三个标准:
 * Apple       iBeacon,
 * Google      Eddystone
 * Radius Network  AltBeacon. https://altbeacon.org/. 开源的标准，从而不倾向于任何特定的供应商。该规格可以免费使用，而不用支付版税或授权费
 */
public class MainActivity extends AppCompatActivity {

    private final static String TAG = MainActivity.class.getSimpleName();
    private MenuItem stopScann;
    private boolean mScanning;
    private ProgressDialog dialog;
    private Runnable mRunnable;
    private Handler mHandler;
    private BluetoothDevice mDevice;
    private ExpandableListView mExpandListView;
    GestureDetectorCompat gDetector;

    private int REQUEST_ENABLE_BT = 1;


    // BLE相关。
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 60000;
    private static final Map<Integer, String> leStatus = new HashMap<Integer, String>() {{
        put(ScanCallback.SCAN_FAILED_ALREADY_STARTED, "扫描进行中");
        put(ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED, "程序没有注册");
        put(ScanCallback.SCAN_FAILED_INTERNAL_ERROR, "内部错误");
        put(ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED, "不支持特性");
    }};


    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private BluetoothAdapter bluetoothAdapter;
    public BtScanner btScanner;
    private BtDevicesAdapter btDevicesAdapter;
    private boolean isBinded = false;

    //搜索蓝牙的回调函数
    private BluetoothAdapter.LeScanCallback leScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            StringBuffer rstring = new StringBuffer();
                            for (byte b : scanRecord) {
                                rstring.append(String.format("%02x,", b));
                            }
                            // 根据上面嵌入式工程里的定义，硬编码取值，不知在其它设备是否是通用的。
//                            btDevicesAdapter.addDevice();
//                            Log.i("描述广播数据 --->", rstring.toString());
                            btDevicesAdapter.addDevice(device, rssi, scanRecord);
                            btDevicesAdapter.notifyDataSetChanged();
                        }
                    });
                }
            };

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            // 如果设备端在广播数据包中，加入厂商等一些信息，可以在这里解出。它就是上面onLeScan里的 scanRecord
            // 参考：https://stackoverflow.com/questions/24003777/read-advertisement-packet-in-android
            // TI BLE ProjectZero 是如下定义的:
//                    static uint8_t advertData[] = {
//                            0x02, /* Length */
//                            SAP_GAP_ADTYPE_FLAGS,
//                            DEFAULT_DISCOVERABLE_MODE | SAP_GAP_ADTYPE_FLAGS_BREDR_NOT_SUPPORTED,
//
//                            /* Manufacturer specific advertising data */
//                            0x06,
//                            0xFF, /* SAP_GAP_ADTYPE_MANUFACTURER_SPECIFIC */
//                            LO_UINT16(TI_COMPANY_ID), HI_UINT16(TI_COMPANY_ID),
//                            TI_ST_DEVICE_ID,
//                            TI_ST_KEY_DATA_ID, 0x00 /* Key state */
//                    };
//  example: 02010606ff0d000300000d0950726f6a656374205a65726f051250002003020a000000000000000000000000000000000000000000000000000000000000
            byte[] record = result.getScanRecord().getBytes();
//            StringBuffer rstring = new StringBuffer();
//            for(byte b: record){
//                rstring.append(String.format("%02x",b));
//            }
            // 根据上面嵌入式工程里的定义，硬编码取值，不知在其它设备是否是通用的。
            btDevicesAdapter.addDevice(result);
            btDevicesAdapter.notifyDataSetChanged();

        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                Log.w("ScanResult - Results", sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("Scan Failed", "Error Code: " + errorCode);
        }
    };

    public boolean isScanning() {
        return mScanning;
    }

    // 扫描BLE设备。
    public void scanLeDevice(final boolean enable) {
        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    invalidateOptionsMenu();
                    Log.w(TAG, "stop scanning after SCAN_PERIOD seconds.");
                    if (mLEScanner != null && bluetoothAdapter.isEnabled())
                        mLEScanner.stopScan(mScanCallback);
                }
            }, SCAN_PERIOD);  // stop scanning after SCAN_PERIOD seconds.

            mLEScanner.startScan(filters, settings, mScanCallback);
            Log.w(TAG, "start to scanLeDevice........");
        } else {
            mHandler.removeCallbacksAndMessages(null);
            if (mLEScanner != null)
                mLEScanner.stopScan(mScanCallback);
            Log.w(TAG, "stop to scanLeDevice........");
        }
        mScanning = enable;
    }

    public void stopScan() {
        if (Build.VERSION.SDK_INT < 21) {
            if (btScanner != null) {
                btScanner.stopScanning();
            }
        } else {
            scanLeDevice(false);
        }
        invalidateOptionsMenu();
    }

    public void startScan() {
        checkRequiredService(MainActivity.this);
        BtDevicesAdapter.clearAdvertisingData();
        BtDevicesAdapter.clear();
        btDevicesAdapter.notifyDataSetChanged();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            if (Build.VERSION.SDK_INT >= 21) {
                mLEScanner = bluetoothAdapter.getBluetoothLeScanner();
                settings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .setReportDelay(0)
                        .build();
                filters = new ArrayList<ScanFilter>();
                scanLeDevice(true);
            } else {
                if (btScanner == null) {
                    btScanner = new BtScanner(bluetoothAdapter, leScanCallback);
                }
                btScanner.startScanning();
            }
        }
        invalidateOptionsMenu();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();

        // 设置首页布局。
        setContentView(R.layout.main_activity_layout);
        getSupportActionBar().setTitle(R.string.title);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_DENIED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            }
        }


        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 获取 BluetoothAdapter
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        // buletooth not supported!!!
        if (bluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mExpandListView = (ExpandableListView) findViewById(R.id.ble_list);
        gDetector = new GestureDetectorCompat(MainActivity.this,new GestureDetector.SimpleOnGestureListener(){
            protected static final float FLIP_DISTANCE = 50;
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                return super.onSingleTapConfirmed(e);
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

//                if (e2.getX() - e1.getX() > FLIP_DISTANCE) {
//                    // fling to right
//                    return true;
//                }
//                if (e1.getY() - e2.getY() > FLIP_DISTANCE) {
//                   // fling to up
//                    return true;
//                }

                if (e2.getY() - e1.getY() > FLIP_DISTANCE) {
                    // only on fling down.
                    if(mScanning){
                        stopScan();
                    }
                    startScan();
                    return true;
                }
                return super.onFling(e1, e2, velocityX, velocityY);
            }
        });


        btDevicesAdapter = new BtDevicesAdapter(MainActivity.this, mExpandListView);

        mExpandListView.setAdapter(btDevicesAdapter);

//        mExpandListView.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                // forward onTouchEvent to GestureDetector
//                return gDetector.onTouchEvent(event);
//            }
//        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // forward onTouchEvent to GestureDetector
        return gDetector.onTouchEvent(event);
    }

    private void checkRequiredService(Context context) {
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;
        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
        }

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {
        }

        if (!gps_enabled && !network_enabled) {
            // notify user
            new AlertDialog.Builder(context)
                    .setMessage(R.string.gps_network_not_enabled)
                    .setPositiveButton(R.string.open_location_settings, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                            context.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_scan, menu);
        menu.findItem(R.id.menu_about).setVisible(true);
        stopScann = menu.findItem(R.id.menu_scan);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
        }

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_CANCELED) {
                finish();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        Log.i(TAG, TAG + " onPause.....");
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {

            stopScan();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mLEScanner != null && bluetoothAdapter.isEnabled()) {
            startScan();
        }
    }


    private void showAboutUs() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        WebView webView = new WebView(this);
        webView.loadUrl("http://yjdwbj.github.com");
        webView.getSettings().setDefaultTextEncodingName("UTF-8");
        String versionName = BuildConfig.VERSION_NAME;
        String ver = "<p>蓝牙BLE测试工具  v%s </p><p>技术支持请联系:yjdwbj@gmail.com</p>";
        webView.loadDataWithBaseURL("", String.format(ver, versionName), "text/html", "UTF-8", "");
        builder.setView(webView)
                .setTitle(R.string.about)
                .setNeutralButton("Ok", null)
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                startScan();
                break;
            case R.id.menu_stop:
                stopScan();
                break;
            case R.id.menu_about:
                showAboutUs();
                break;
            case R.id.github:
                showGithub();
                break;

        }
        return true;
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();


    private void showGithub() {
        final Intent webview = new Intent(this, WebClient.class);
        startActivity(webview);
    }

    /**
     * 搜索蓝牙的线程
     */
    public static class BtScanner extends Thread {
        private final static String TAG = BtScanner.class.getSimpleName();

        private final BluetoothAdapter bluetoothAdapter;
        private final BluetoothAdapter.LeScanCallback mLeScanCallback;

        private volatile boolean isScanning = false;

        BtScanner(BluetoothAdapter adapter, BluetoothAdapter.LeScanCallback callback) {
            bluetoothAdapter = adapter;
            mLeScanCallback = callback;
        }

        public boolean isScanning() {
            return isScanning;
        }

        public void startScanning() {
            synchronized (this) {
                isScanning = true;
                start();
            }
        }

        public void stopScanning() {
            synchronized (this) {
                isScanning = false;
                bluetoothAdapter.stopLeScan(mLeScanCallback);
            }
        }

        @Override
        public void run() {
            try {
                Log.w(TAG, "start to scanning thread........");
                while (true) {
                    synchronized (this) {
                        if (!isScanning)
                            break;
                        bluetoothAdapter.startLeScan(mLeScanCallback);
                    }

                    sleep(10000);

                    synchronized (this) {
                        bluetoothAdapter.stopLeScan(mLeScanCallback);
                    }
                }
            } catch (InterruptedException ignore) {

            } finally {
                Log.w(TAG, "stop the scanning thread!!!!!");
                bluetoothAdapter.stopLeScan(mLeScanCallback);
            }
        }
    }
}
