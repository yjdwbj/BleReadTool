package bt.lcy.btread;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import adapters.ReadDataAdapter;

public class ConsoleActivity extends AppCompatActivity {

    private final static String TAG = ConsoleActivity.class.getSimpleName();
    private ListView listData;
    private EditText cmdLine;
    private Button toggle;
    private Button sendRead;

    private MenuItem hexMenuItem;
    private MenuItem stringMenuItem;
    private static BluetoothGattCharacteristic characteristic;
    private static BtService btService;
    private boolean write = false;
    private boolean read = false;
    private boolean notify = false;

    private ReadDataAdapter readDataAdapter;

    private boolean onlyRead = false;


    public static void setBtService(final BtService btService) {
        ConsoleActivity.btService = btService;
    }

    public static void setCharacteristic(final BluetoothGattCharacteristic characteristic) {
        ConsoleActivity.characteristic = characteristic;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.read_mode, menu);
        stringMenuItem = menu.findItem(R.id.string_mode);
        stringMenuItem.setVisible(false);
        hexMenuItem = menu.findItem(R.id.hex_string_mode);
        hexMenuItem.setVisible(true);
        return true;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        setContentView(R.layout.rwn_console);


        final Intent intent = getIntent();

        int properties = characteristic.getProperties();


        readDataAdapter = new ReadDataAdapter(this);

        listData = (ListView) findViewById(R.id.list_read);

        listData.setAdapter(readDataAdapter);

        cmdLine = (EditText) findViewById(R.id.edit_cmd);
        toggle = (Button) findViewById(R.id.button_toggle);
        sendRead = (Button) findViewById(R.id.button_send);


        read = (properties & BluetoothGattCharacteristic.PROPERTY_READ) != 0;
        write = (properties & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0;
        notify = (properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0;

        if (notify) {
            Toast.makeText(this, R.string.startlistener, Toast.LENGTH_SHORT).show();
            btService.setCharacteristicNotification(characteristic, true);
        }

        final BluetoothGattDescriptor descriptor = characteristic.getDescriptor(characteristic.getUuid());

        if (descriptor != null) {
            Log.i(TAG,"getDescriptor ONE:  " + descriptor.getUuid());
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
            btService.writeDescriptor(descriptor);
        } else {
            Log.w(TAG,"characteristic.getUuid() " + characteristic.getUuid() + " get descriptor null ");


        }

        if (read && !write) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    toggle.setVisibility(View.GONE);
                    cmdLine.setVisibility(View.GONE);
                    sendRead.setText(R.string.read);
                }
            });

        } else if (!read && write) {
            toggle.setVisibility(View.GONE);
            sendRead.setText(R.string.send);
        } else if (!read && !write && notify) {
            toggle.setVisibility(View.GONE);
        }

        toggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "click toggle button " + sendRead.getText());

                final boolean isVisible = cmdLine.getVisibility() == View.VISIBLE;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        cmdLine.setVisibility(isVisible ? View.GONE : View.VISIBLE);
                        sendRead.setText(isVisible ? R.string.read : R.string.send);
                    }
                });
            }
        });

        sendRead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final boolean isVisible = cmdLine.getVisibility() == View.VISIBLE;
                if (cmdLine.getVisibility() == View.VISIBLE) {
                    if (cmdLine.getText().toString().length() == 0)
                        return;
                    Log.i(TAG, "send  some things...............");
                    readDataAdapter.add(cmdLine.getText().toString());
                    characteristic.setValue(cmdLine.getText().toString());
                    btService.writeCharacteristic(characteristic);
                    cmdLine.setText("");
                } else {
                    Log.i(TAG, " read some things..............." + sendRead.getText().toString());
                    btService.readCharateristic(characteristic);
                }
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.stringmode);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {


        switch (item.getItemId()) {
            case android.R.id.home: {
                onBackPressed();
//                finish();
                onStop();
                return true;
            }
            case R.id.string_mode: {
                getSupportActionBar().setTitle(R.string.stringmode);
                readDataAdapter.setHexMode(false);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hexMenuItem.setVisible(true);
                        stringMenuItem.setVisible(false);
                    }
                });
//                invalidateOptionsMenu();
                return true;
            }
            case R.id.hex_string_mode: {
                readDataAdapter.setHexMode(true);
                getSupportActionBar().setTitle(R.string.hexstringmode);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hexMenuItem.setVisible(false);
                        stringMenuItem.setVisible(true);
                    }
                });

//                invalidateOptionsMenu();
                return true;
            }

        }
        return super.onOptionsItemSelected(item);
    }


    private final BroadcastReceiver gattBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();


            if (BtService.ACTION_DATA_AVAILABLE.equals(action)) {
                final String uuid = intent.getStringExtra(BtService.EXTRA_SERVICE_UUID);
//                final String text = intent.getStringExtra(BtService.EXTRA_TEXT);
                final String cuuid = intent.getStringExtra(BtService.EXTRA_CHARACTERISTIC_UUID);
                Log.i(TAG, " ACTION_DATA_AVAILABLE uuid: " + uuid + " cuuid :" + cuuid);
                String text = "empty";
                try {
                    text = new String(characteristic.getValue());

                } catch (NullPointerException e) {

                } finally {

                }

                readDataAdapter.add(text);
                final byte[] arr = text.getBytes();

                Log.i(TAG, "+++Get Notify data is " + text);
            } else if (BtService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Toast.makeText(ConsoleActivity.this, R.string.srv_disconnect, Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onResume() {

        registerReceiver(gattBroadcastReceiver, makeGattUpdateIntenFilter());
        super.onResume();
    }

    private static IntentFilter makeGattUpdateIntenFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BtService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BtService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    @Override
    protected void onPause() {

        unregisterReceiver(gattBroadcastReceiver);
        Log.i(TAG, "!!! ConsoleActivity onPuse");
        readDataAdapter.clear();
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (notify&& characteristic != null) {
            btService.setCharacteristicNotification(characteristic, false);
            final BluetoothGattDescriptor descriptor = characteristic.getDescriptor(characteristic.getUuid());
            Log.i(TAG, "get descriptor on stop " + descriptor);
            if (descriptor != null) {
                descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                btService.writeDescriptor(descriptor);
            }
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "!!! ConsoleActivity onDestroy ---");

    }

}
