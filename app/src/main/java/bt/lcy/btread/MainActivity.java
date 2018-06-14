package bt.lcy.btread;

import android.Manifest;
import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Scanner;

import adapters.BtDevicesAdapter;

public class MainActivity extends ListActivity {

    private final static String TAG = MainActivity.class.getSimpleName();

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private BluetoothAdapter bluetoothAdapter;
    private BtScanner btScanner;
    private BtDevicesAdapter btDevicesAdapter;


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


    @Override
    protected void onResume() {
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
        super.onPause();

        if (btScanner != null) {
            btScanner.stopScanning();
            btScanner = null;
        }
    }

    @Override
    protected void onListItemClick(ListView listView, View view, int position, long id) {
        final BluetoothDevice device = btDevicesAdapter.getDevice(position);
        if (device == null) return;

        Log.w(TAG, "Connect to device: " + device.getName() + " addr: " + device.getAddress());
        final Intent intent = new Intent(this, BtDeviceServicesActivity.class);
        intent.putExtra(BtDeviceServicesActivity.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(BtDeviceServicesActivity.EXTRAS_DEVICE_ADDR, device.getAddress());
        startActivity(intent);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                btDevicesAdapter.clear();
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
