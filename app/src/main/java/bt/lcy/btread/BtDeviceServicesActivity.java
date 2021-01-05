package bt.lcy.btread;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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

//import android.support.v7.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatActivity;

import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.UUID;

import adapters.BtDevicesAdapter;
import adapters.BtServicesAdapter;
import bt.lcy.gatt.GattManager;

import static androidx.appcompat.app.ActionBar.DISPLAY_SHOW_CUSTOM;
import static bt.lcy.btread.BtStaticVal.BT_CHAR;
import static bt.lcy.btread.BtStaticVal.BT_DEVICE;
import static bt.lcy.btread.BtStaticVal.BT_SRVUUID;

public class BtDeviceServicesActivity extends AppCompatActivity {

    private static final String TAG=BtDeviceServicesActivity.class.getSimpleName();

    GattManager mGattManager;
    BluetoothDevice mDevice;

    private String deviceName;
    private String deviceAddress;
    private  boolean isConnected;

    private BtServicesAdapter btServicesAdapter;
    private ExpandableListView mExpandableListView;
//    private Activity parentActivity;
    public BluetoothDevice getDevice() {
    return mDevice;
}

    public GattManager getGattManager() {
        return mGattManager;
    }

    @Override
    protected void onStop() {

        super.onStop();
    }

    // Automatically handles Bluetooth turning off while connected to a demo
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d("PZ_Activity", "BT turned off");

                        if (mExpandableListView != null) {
                            onBackPressed();
                        }
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        break;
                    case BluetoothAdapter.STATE_ON:
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        break;
                }
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_services);
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        mDevice = (BluetoothDevice) bundle.getParcelable(BT_DEVICE);
        String srvuuid = intent.getStringExtra(BT_SRVUUID);
        Log.i(TAG,"service uuid is : " + srvuuid);
        if(0 == (UUID.fromString(srvuuid).getMostSignificantBits() >> 32))
        {
            // The device advertis header not service.
            srvuuid = BtService.mDeviceServices.get(BtService.mDeviceServices.size()-1).getUuid().toString();
        }

        Log.i(TAG," connected to  : " + srvuuid);
        mGattManager = new GattManager(this,srvuuid);
        mGattManager.connectToDevice(mDevice, false);
        isConnected = true;

        // Detect Bluetooth on/off state
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);

        mExpandableListView = (ExpandableListView) findViewById(R.id.list_services);
        displayGattServices(BtService.mDeviceServices);

        // https://stackoverflow.com/questions/12387345/how-to-center-align-the-actionbar-title-in-android
        getSupportActionBar().setDisplayOptions(DISPLAY_SHOW_CUSTOM);
        View viewActionBar = getLayoutInflater().inflate(R.layout.dev_connected,null);
        //getSupportActionBar().setCustomView(R.layout.dev_connected);
        getSupportActionBar().setCustomView(viewActionBar);
        TextView title = (TextView) viewActionBar.findViewById(R.id.dev_title);
        title.setText(Html.fromHtml( mDevice.getName() + "<br>" + mDevice.getAddress()));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void displayGattServices(List<BluetoothGattService> gattServiceList)
    {
        if(gattServiceList == null) return;
        btServicesAdapter = new BtServicesAdapter(this, gattServiceList);
        mExpandableListView.setAdapter(btServicesAdapter);
    }

    @Override
    protected void onResume(){
        super.onResume();

    }

    @Override
    protected void onPause(){
        super.onPause();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        try{
            if(mReceiver!=null)
                unregisterReceiver(mReceiver);
        }catch(Exception e)
        {

        }
        if(mGattManager !=null){
            mGattManager.close(mDevice);
            mGattManager = null;
        }
    }

    @Override
    public void onBackPressed() {
        try{
            if(mReceiver!=null)
                unregisterReceiver(mReceiver);
        }catch(Exception e)
        {

        }
        finish();
    }
}
