package bt.lcy.btread;

import bt.lcy.btread.BuildConfig;

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
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.AdapterView;
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
    private BtService btService;
    private MenuItem stopScann;
    private boolean mScanning;
    private Handler handler;
    private ProgressDialog dialog;
    private Runnable mRunnable;
    private Handler mHandler;
    private BluetoothDevice mDevice;
    private int REQUEST_ENABLE_BT = 1;
    private BluetoothGatt mGatt;


    // BLE相关。
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
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
    private BtScanner btScanner;
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
                                rstring.append(String.format("%02x", b));
                            }
                            // 根据上面嵌入式工程里的定义，硬编码取值，不知在其它设备是否是通用的。
                            int company = scanRecord[6] << 8 | scanRecord[5];
                            btDevicesAdapter.addDevice(device, rssi, company);
//                            Log.i("描述广播数据 --->", rstring + " , company = " + company);
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
            int company = record[6] << 8 | record[5];
//            Log.i("广播数据 --->", result.getScanRecord().toString() + ", " + result.getScanRecord().getManufacturerSpecificData().get(0xff00));

            BluetoothDevice btDevice = result.getDevice();
            btDevicesAdapter.addDevice(btDevice, result.getRssi(), company);
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

    // 扫描BLE设备。
    private void scanLeDevice(final boolean enable) {

        mScanning = enable;
        if (enable) {
            Log.w(TAG, "start to scanLeDevice........");
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT < 21) {
                        bluetoothAdapter.stopLeScan(leScanCallback);
                    } else {
                        if (mLEScanner != null && bluetoothAdapter.isEnabled())
                            mLEScanner.stopScan(mScanCallback);
                    }
                }
            }, SCAN_PERIOD);
            mLEScanner.startScan(filters, settings, mScanCallback);
//            if (Build.VERSION.SDK_INT < 21) {
//                bluetoothAdapter.startLeScan(leScanCallback);
//            } else {
//                mLEScanner.startScan(filters, settings, mScanCallback);
//            }
        } else {
            Log.w(TAG, "stop to scanLeDevice........");
            if (Build.VERSION.SDK_INT < 21) {
                bluetoothAdapter.stopLeScan(leScanCallback);
            } else {
                if(mLEScanner != null )
                    mLEScanner.stopScan(mScanCallback);
            }
        }
    }

    private void startScan() {

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            // 扫描BLE设备。
            if (Build.VERSION.SDK_INT >= 21) {
                mLEScanner = bluetoothAdapter.getBluetoothLeScanner();
                settings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .setReportDelay(0)
                        .build();
                filters = new ArrayList<ScanFilter>();
            }
            scanLeDevice(true);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        handler = new Handler();
        super.onCreate(savedInstanceState);
        // 设置首页布局。
        setContentView(R.layout.main_activity_layout);
        getSupportActionBar().setTitle(R.string.title);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            }
        }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        mHandler = new Handler();

        // 获取 BluetoothAdapter
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        // buletooth not supported!!!
        if (bluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }


        btDevicesAdapter = new BtDevicesAdapter(getBaseContext());
        ListView listView = (ListView) findViewById(R.id.ble_list);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 连接所选的设备。
                view.setSelected(true);

                if (mScanning) {
                    scanLeDevice(false);
                    invalidateOptionsMenu();
                }
                mDevice = btDevicesAdapter.getDevice(position);
                dialog = ProgressDialog.show(MainActivity.this, "",
                        "连接 " + mDevice.getName() + ",稍等...", true);
                // 这里是为了检查目标的设备有那些具体的服务与特征，根据不同的特征服务来使用不同的界面，如果只针对某一特定的设备，可以省略这一步。
                Log.i(TAG, "!!!!!-------------> uuid is: " + mDevice.getUuids() + " status ");
                mDevice.connectGatt(getApplicationContext(), false, new BluetoothGattCallback() {

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
                        if (status == GATT_SUCCESS) {
                            // 发现服务后，遍历服务与特征。
                            for (final BluetoothGattService service : gatt.getServices()) {
                                String uuid = service.getUuid().toString();
                                Log.i(TAG, "!!!!!-> uuid is: " + service.getUuid().toString());
                                if (BtStaticVal.UUID_KEY_DATA.equals(uuid)) {
//                                    isAmoBoard = true;
//                                    for (final BluetoothGattCharacteristic bc : service.getCharacteristics())
//                                        userCharacterMap.put(bc.getUuid().toString(), bc);
//                                    break;
                                    //打开AmoMcu开发板独立页面.
                                    AmoMcuBoardActivity.setBtService(btService);
                                    final Intent amoBoard = new Intent(MainActivity.this, AmoMcuBoardActivity.class);
                                    startActivity(amoBoard);
                                } else if (TiMsp432ProjectZeroActivity.isVaildUUID(uuid)) {
                                    for (final BluetoothGattCharacteristic bc : service.getCharacteristics()) {
                                        // 列出所有的UUID。
//                                        userCharacterMap.put(bc.getUuid().toString(), bc);
                                        StringBuilder property = new StringBuilder();
//                                        int ps = bc.getProperties();
//                                        property.append(" w: " + ((ps & BluetoothGattCharacteristic.PROPERTY_READ) != 0 ? "true" : "false"));
//                                        property.append(" ,r: " + ((ps & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0 ? "true" : "false"));
//                                        property.append(" ,n: " + ((ps & PROPERTY_NOTIFY) != 0 ? "true" : "false"));
//                                        property.append(" ,wn: " + ((ps & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0 ? "true" : "false"));
//                                        property.append(" ,sw: " + ((ps & BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE) != 0 ? "true" : "false"));
//                                        Log.i(TAG, "!!!!!-> uuid is: " + bc.getUuid().toString() + ", property : " + property);
                                    }
                                    gatt.disconnect();
                                    gatt.close();
                                    // 打开Ti msp432 ProjectZero开发板。
                                    // 允许其他应用启动您的 Activity https://developer.android.com/training/basics/intents/filters

                                    Intent tiBoard = new Intent(MainActivity.this, TiMsp432ProjectZeroActivity.class);
                                    //   Parcelable 和 Bundle ,Activity 之间发数据。
                                    //   https://developer.android.com/guide/components/activities/parcelables-and-bundles
                                    Bundle bundle = new Bundle();
                                    bundle.putParcelable(BT_DEVICE, mDevice);
                                    tiBoard.putExtras(bundle);
                                    Log.i(TAG, "start Ti ProjectZero!!!!!!!!!!!!!!1, " + tiBoard.toString());
                                    finishActivity(0);
                                    startActivity(tiBoard);
                                } else {
                                    //       Intent 和 Intent 过滤器     https://developer.android.com/guide/components/intents-filters
                                    final Intent intent = new Intent(MainActivity.this, BtDeviceServicesActivity.class);
                                    intent.putExtra(BtDeviceServicesActivity.EXTRAS_DEVICE_NAME, mDevice.getName());
                                    intent.putExtra(BtDeviceServicesActivity.EXTRAS_DEVICE_ADDR, mDevice.getAddress());
                                    startActivity(intent);
                                }
                            }
                            dialog.dismiss();
                        }
                    }
                });

                Log.i(TAG, "bindService Connect to device: " + mDevice.getName() + " addr: " + mDevice.getAddress());
            }
        });

        listView.setAdapter(btDevicesAdapter);
        invalidateOptionsMenu();
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
            scanLeDevice(false);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (mGatt != null) {
            mGatt.close();
            mGatt = null;
        }

        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mLEScanner != null && bluetoothAdapter.isEnabled()) {
            mLEScanner.stopScan(mScanCallback);
        }
        if (mGatt != null) {
            mGatt.close();
            mGatt = null;
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
                btDevicesAdapter.clear();
                btDevicesAdapter.notifyDataSetChanged();
                if (btScanner == null) {
                    btScanner = new BtScanner(bluetoothAdapter, leScanCallback);
                    btScanner.startScanning();
                    invalidateOptionsMenu();
                }
                startScan();
                invalidateOptionsMenu();
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                invalidateOptionsMenu();
//                if (btScanner != null) {
//                    btScanner.stopScanning();
//                    btScanner = null;
//                    invalidateOptionsMenu();
//                }
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

    private void startActivity() {
        if (btService == null) {
            return;
        }
        if (btService.isAmoBoard()) {
            //打开AmoMcu开发板独立页面.
            AmoMcuBoardActivity.setBtService(btService);
            final Intent amoBoard = new Intent(this, AmoMcuBoardActivity.class);
            startActivity(amoBoard);
        } else if (btService.isTiProjectZero()) {
            // 打开Ti msp432 ProjectZero开发板。
            // 允许其他应用启动您的 Activity https://developer.android.com/training/basics/intents/filters

            Intent tiBoard = new Intent(this, TiMsp432ProjectZeroActivity.class);
            //   Parcelable 和 Bundle ,Activity 之间发数据。
            //   https://developer.android.com/guide/components/activities/parcelables-and-bundles
            Bundle bundle = new Bundle();
            bundle.putParcelable(BT_DEVICE, mDevice);
            tiBoard.putExtras(bundle);
            Log.i(TAG, "start Ti ProjectZero!!!!!!!!!!!!!!1, " + tiBoard.toString());
            startActivity(tiBoard);
        } else {
            //       Intent 和 Intent 过滤器     https://developer.android.com/guide/components/intents-filters
            final Intent intent = new Intent(this, BtDeviceServicesActivity.class);
            intent.putExtra(BtDeviceServicesActivity.EXTRAS_DEVICE_NAME, btService.getDevice().getName());
            intent.putExtra(BtDeviceServicesActivity.EXTRAS_DEVICE_ADDR, btService.getDevice().getAddress());
            startActivity(intent);
        }
        dialog.dismiss();
    }


    /**
     * 搜索蓝牙的线程
     */
    private static class BtScanner extends Thread {
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
                Log.w(TAG, "start to scanning........");
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
                bluetoothAdapter.stopLeScan(mLeScanCallback);
            }
        }
    }
}
