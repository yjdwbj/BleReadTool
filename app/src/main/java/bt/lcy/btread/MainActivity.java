package bt.lcy.btread;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
//import android.support.v7.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.webkit.WebView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Scanner;

import adapters.BtDevicesAdapter;

public class MainActivity extends ListActivity {

    private final static String TAG = MainActivity.class.getSimpleName();
    private BtService btService;
    private MenuItem stopScann;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private BluetoothAdapter bluetoothAdapter;
    private BtScanner btScanner;
    private BtDevicesAdapter btDevicesAdapter;
    private boolean isBinded = false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);



        getActionBar().setTitle(R.string.title);

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


        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        // buletooth not supported!!!
        if (bluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }


        // Example of a call to a native method
//        TextView tv = (TextView) findViewById(R.id.sample_text);
//        tv.setText(stringFromJNI());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_scan, menu);
        menu.findItem(R.id.menu_about).setVisible(true);
        stopScann = menu.findItem(R.id.menu_scan);
        if (btScanner == null || !btScanner.isScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
//            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            // menu.findItem(R.id.menu_refresh).setActionView(null);
        }

        return true;
    }


    private final BroadcastReceiver gattBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final  String action = intent.getAction();

//            Log.i(TAG,"!!!!!Got a BroadCast: " + action);
            if(BtService.ACTION_GATT_SERVICES_DISCOVERED.equals(action))
            {
                startActivity();
            }
        }
    };


    @Override
    protected void onResume() {
        //要注册才能开启使用
        registerReceiver(gattBroadcastReceiver,makeGattUpdateIntenFilter());
        super.onResume();

        if (!bluetoothAdapter.isEnabled()) {
            final Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

            startActivityForResult(enableBtIntent, 1);
            return;
        }
        init();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_CANCELED) {
                finish();
            } else {
                init();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        Log.i(TAG,TAG + " onPause.....");
        if (btScanner != null) {
            btScanner.stopScanning();
            btScanner = null;
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(gattBroadcastReceiver);
        unbindService(serviceConnection);
    }

    private void showAboutUs() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        WebView webView = new WebView(this);
        webView.loadUrl("http://yjdwbj.github.com");
        webView.getSettings().setDefaultTextEncodingName("UTF-8");
        webView.loadDataWithBaseURL ("","<p>蓝牙BLE测试工具.</p><p>技术支持请联系:yjdwbj@126.com</p>","text/html","UTF-8","");
        builder.setView(webView)
                .setTitle(R.string.about)
                .setNeutralButton("Ok", null)
                .show();
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            isBinded = true;
            btService = ((BtService.LocalBinder)service).getService();

            final String deviceAddress = getIntent().getStringExtra(BtDeviceServicesActivity.EXTRAS_DEVICE_ADDR);
            if(!btService.initialize())
            {
                Toast.makeText(MainActivity.this,R.string.error_bluetooth_initialize,Toast.LENGTH_SHORT).show();
                finish();
            }
            Log.i(TAG,"Connection Service");
//            btService.connect(deviceAddress);
            btService.connect();

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.w(TAG,"Disconnection Service");
            Toast.makeText(MainActivity.this,R.string.srv_disconnect,Toast.LENGTH_SHORT).show();
          //  clearUI();

            btService = null;
        }
    };

    @Override
    protected void onListItemClick(ListView listView, View view, int position, long id) {

        view.setSelected(true);
        btDevicesAdapter.setProcessFlag(true);
        if(isBinded)
            unbindService(serviceConnection);

        if (btScanner != null) {
            btScanner.stopScanning();
            btScanner = null;
            invalidateOptionsMenu();
        }
        final BluetoothDevice device = btDevicesAdapter.getDevice(position);
        if (device == null) return;



        final Intent gattIntent = new Intent(this,BtService.class);
        gattIntent.putExtra(BtDeviceServicesActivity.EXTRAS_DEVICE_NAME, device.getName());
        gattIntent.putExtra(BtDeviceServicesActivity.EXTRAS_DEVICE_ADDR, device.getAddress());
        bindService(gattIntent,serviceConnection,BIND_AUTO_CREATE);
        Log.w(TAG, "Connect to device: " + device.getName() + " addr: " + device.getAddress());

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                btDevicesAdapter.clear();
                btDevicesAdapter.notifyDataSetChanged();
                if (btScanner == null) {
                    btScanner = new BtScanner(bluetoothAdapter, mLeScanCallback);
                    btScanner.startScanning();
                    invalidateOptionsMenu();
                }
                break;
            case R.id.menu_stop:
                if (btScanner != null) {
                    btScanner.stopScanning();
                    btScanner = null;
                    invalidateOptionsMenu();
                }
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


    //搜索蓝牙的回调函数
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            btDevicesAdapter.addDevice(device, rssi);
                            btDevicesAdapter.notifyDataSetChanged();
                        }
                    });
                }
            };

    private void init() {
        if (btDevicesAdapter == null) {
            btDevicesAdapter = new BtDevicesAdapter(getBaseContext());
            setListAdapter(btDevicesAdapter);
        }

        if (btScanner == null) {
            Toast.makeText(this, "Start Scanning....", Toast.LENGTH_SHORT).show();
            btScanner = new BtScanner(bluetoothAdapter, mLeScanCallback);
            btScanner.startScanning();
        }
        invalidateOptionsMenu();
    }


    private void showGithub()
    {
        final Intent webview = new Intent(this,WebClient.class);
        startActivity(webview);
    }


    private static IntentFilter makeGattUpdateIntenFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BtService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BtService.ACTION_GATT_SERVICES_DISCOVERED);
        return intentFilter;
    }

    private void startActivity()
    {

        btDevicesAdapter.setProcessFlag(false);
        if(btService.isAmoBoard())
        {
            //打开AmoMcu开发板独立页面.
            AmoMcuBoardActivity.setBtService(btService);
            final Intent amoBoard = new Intent(this,AmoMcuBoardActivity.class);
            startActivity(amoBoard);
        }else{
            BtDeviceServicesActivity.setBtService(btService);
            final Intent intent = new Intent(this, BtDeviceServicesActivity.class);
            intent.putExtra(BtDeviceServicesActivity.EXTRAS_DEVICE_NAME, btService.getDevice().getName());
            intent.putExtra(BtDeviceServicesActivity.EXTRAS_DEVICE_ADDR, btService.getDevice().getAddress());
            startActivity(intent);
        }
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

                    sleep(1000);

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
