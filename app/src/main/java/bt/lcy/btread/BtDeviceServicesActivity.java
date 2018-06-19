package bt.lcy.btread;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import adapters.BtDevicesAdapter;
import adapters.BtServicesAdapter;

public class BtDeviceServicesActivity extends AppCompatActivity {

    private static final String TAG=BtDeviceServicesActivity.class.getSimpleName();

    public static final  String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final  String EXTRAS_DEVICE_ADDR = "DEVICE_ADDRESS";

    private TextView deviceNameField;
    private TextView deviceAddressField;

    static private BtService btService;

    private String deviceName;
    private String deviceAddress;
    private  boolean isConnected;

    private BtServicesAdapter btServicesAdapter;
    private ExpandableListView expandableListView;
//    private Activity parentActivity;




    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.service_opt,menu);
        menu.findItem(R.id.menu_connect).setVisible(!isConnected);
        menu.findItem(R.id.menu_disconnect).setVisible(isConnected);
        return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()){
            case R.id.menu_connect:
                btService.connect(deviceAddress);
                return true;
            case R.id.menu_disconnect:
                btService.disconnect();
                return true;
            case android.R.id.home:
            {

                clearUI();
                btService.onDestroy();
                btService = null;
                onBackPressed();
                return true;
            }

        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {

        super.onStop();
    }

    private final BroadcastReceiver gattBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final  String action = intent.getAction();

//            Log.i(TAG,"!!!!!Got a BroadCast: " + action);
            if(BtService.ACTION_GATT_CONNECTED.equals(action))
            {
                isConnected = true;
                invalidateOptionsMenu();
            }
            else if(BtService.ACTION_GATT_DISCONNECTED.equals(action))
            {
                isConnected = false;

                clearUI();
                Toast.makeText(getApplicationContext(),R.string.srv_disconnect,Toast.LENGTH_SHORT).show();
                invalidateOptionsMenu();

            }
            else if(BtService.ACTION_GATT_SERVICES_DISCOVERED.equals(action))
            {
                displayGattServices(btService.getSupportedGattServices());
            }else if(BtService.ACTION_DATA_AVAILABLE.equals(action))
            {
               final String uuid = intent.getStringExtra(BtService.EXTRA_SERVICE_UUID);
               final String text = intent.getStringExtra(BtService.EXTRA_TEXT);
               final String cuuid = intent.getStringExtra(BtService.EXTRA_CHARACTERISTIC_UUID);
//               Log.i(TAG, " ACTION_DATA_AVAILABLE uuid: " + uuid + " cuuid :" + cuuid + " text:" + text);
            }
        }
    };



    private final ExpandableListView.OnChildClickListener childClickListener = new ExpandableListView.OnChildClickListener() {
        @Override
        public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {


            if (btServicesAdapter == null)
            {
                Log.w(TAG,"btServicesAdapter null..... ");
                return false;
            }

            final BluetoothGattCharacteristic characteristic = btServicesAdapter.getChild(groupPosition,childPosition);
            ConsoleActivity.setCharacteristic(characteristic);
            ConsoleActivity.setBtService(btService);


            final Intent cmdIntent = new Intent(BtDeviceServicesActivity.this,ConsoleActivity.class);
            startActivity(cmdIntent);
            return true;
        }
    };

    static public void setBtService(final BtService service)
    {
        BtDeviceServicesActivity.btService = service;
    }

    private void displayGattServices(List<BluetoothGattService> gattServiceList)
    {
//        Log.w(TAG,"-->>displayGattServices ..... size : " + gattServiceList.size());
        if(gattServiceList == null) return;
        btServicesAdapter = new BtServicesAdapter(this, gattServiceList);
        expandableListView.setAdapter(btServicesAdapter);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_services);

//        Toast.makeText(this,"BtDeviceService List",Toast.LENGTH_SHORT);

        final Intent intent = getIntent();
        deviceAddress =  intent.getStringExtra(EXTRAS_DEVICE_ADDR);
        deviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);

        deviceAddressField = (TextView)findViewById(R.id.srv_dev_uuid);
        deviceNameField = (TextView)findViewById(R.id.srv_dev_name);

        deviceNameField.setText(deviceName);
        deviceAddressField.setText(deviceAddress);

        expandableListView = (ExpandableListView) findViewById(R.id.list_services);
        expandableListView.setOnChildClickListener(childClickListener);


//        Log.i(TAG,"BtDeviceService List ....... " + deviceAddress);
        getSupportActionBar().setTitle(deviceName);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        displayGattServices(btService.getSupportedGattServices());

//        final Intent gattIntent = new Intent(this,BtService.class);
//        bindService(gattIntent,serviceConnection,BIND_AUTO_CREATE);

    }

    @Override
    protected void onResume(){
        super.onResume();
        registerReceiver(gattBroadcastReceiver,makeGattUpdateIntenFilter());
        if(btService != null){
            final boolean result = btService.connect(deviceAddress);
            Log.i(TAG,"Connection request result=" + result);
        }
    }

    private void clearUI(){
        expandableListView.setAdapter((SimpleExpandableListAdapter)null);
    }

    @Override
    protected void onPause(){
        super.onPause();
        unregisterReceiver(gattBroadcastReceiver);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
//        btService.disconnect();
//        unbindService(serviceConnection);
        btService = null;
    }


    private static IntentFilter makeGattUpdateIntenFilter(){
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BtService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BtService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BtService.ACTION_GATT_SERVICES_DISCOVERED);
//        intentFilter.addAction(BtService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

}
