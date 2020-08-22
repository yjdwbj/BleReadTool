package bt.lcy.btread;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import adapters.ViewPagerFragmentAdapter;
import bt.lcy.gatt.GattManager;

import static bt.lcy.btread.BtStaticVal.BT_DEVICE;

public class TiMsp432ProjectZeroActivity extends AppCompatActivity {
    static public BtService btService;
    // 参考 https://developer.android.com/guide/navigation/navigation-swipe-view-2
    ViewPager2 viewPage2;
    ViewPagerFragmentAdapter adapter;

    GattManager mGattManager;
    BluetoothDevice mDevice;

    public static String[] Profiles = {
            BtStaticVal.UUID_TI_PROJECT_ZERO,
            BtStaticVal.UUID_TI_PROJECT_ZERO_LED_SERIVCE,
            BtStaticVal.UUID_TI_PROJECT_ZERO_SW,
            BtStaticVal.UUID_TI_PROJECT_ZERO_DATA,
    };

    public static boolean isVaildUUID(String string) {
        for (String s : Profiles) {
            if (s.equals(string))
                return true;
        }
        return false;
    }

    private boolean isConnected = false;
    private String TAG = TiMsp432ProjectZeroActivity.class.getName();
    // 关于广播  https://developer.android.com/guide/components/broadcasts
    private final BroadcastReceiver gattBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.i(TAG, "!!!!!gattBroadcastReceiver Got a BroadCast: " + action + ", " + intent.toUri(Intent.URI_INTENT_SCHEME).toString());
            if (BtService.ACTION_GATT_DISCONNECTED.equals(action)) {
                isConnected = false;
                Toast.makeText(getApplicationContext(), R.string.srv_disconnect, Toast.LENGTH_SHORT).show();
                Log.i(TAG, "!!!!!Got a BroadCast: " + action + ", " + intent.toUri(Intent.URI_INTENT_SCHEME).toString());
                invalidateOptionsMenu();
                onStop();
            } else if(BtService.ACTION_GATT_CONNECTED.equals(action)) {
                Log.i(TAG, "connected name : " + btService.getName());
                Log.i(TAG, "!!!!!gattBroadcastReceiver Got a BroadCast: " + action + ", " + intent.toUri(Intent.URI_INTENT_SCHEME).toString());
                btService.setCharacteristicNotification(btService.getCharacteristicByUUID(BtStaticVal.UUID_TI_PROJECT_ZERO_SW1_STATUS), true);
                btService.setCharacteristicNotification(btService.getCharacteristicByUUID(BtStaticVal.UUID_TI_PROJECT_ZERO_SW2_STATUS), true);
                btService.setCharacteristicNotification(btService.getCharacteristicByUUID(BtStaticVal.UUID_TI_PROJECT_ZERO_DATA_N), true);

            } else if (BtService.ACTION_DATA_AVAILABLE.equals(action)) {
                // 有收到新的广播数据。
                final String uuid = intent.getStringExtra(BtService.EXTRA_SERVICE_UUID);
                final String text = intent.getStringExtra(BtService.EXTRA_TEXT);
                final String cuuid = intent.getStringExtra(BtService.EXTRA_CHARACTERISTIC_UUID);
            }
        }
    };

    public BluetoothDevice getDevice() {
        return mDevice;
    }

    public GattManager getGattManager() {
        return mGattManager;
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();

        mDevice = (BluetoothDevice) bundle.getParcelable(BT_DEVICE);

        // 参考 Activity生命周期  https://developer.android.com/guide/components/activities/activity-lifecycle
        // https://pspdfkit.com/blog/2019/using-the-bottom-navigation-view-in-android/
        setContentView(R.layout.ti_msp432_project_zero_viewpager2);
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        mGattManager = new GattManager(this,BtStaticVal.UUID_TI_PROJECT_ZERO);
        mGattManager.connectToDevice(mDevice,false);

        viewPage2 = findViewById(R.id.pager2);
        adapter = new ViewPagerFragmentAdapter(this);
        viewPage2.setAdapter(adapter);
        viewPage2.setUserInputEnabled(false);



//        new TabLayoutMediator(tabLayout, viewPage2, new TabLayoutMediator.TabConfigurationStrategy() {
//            @Override
//            public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
//                tab.setText("Tab "+ (position+1));
//                viewPage2.setCurrentItem(position);
//            }
//        });
        // 开启Lambda支持。 https://stackoverflow.com/questions/22703412/java-lambda-expressions-not-supported-at-this-language-level
        // For Android 3.0+ Go File → Project Structure → Module → app and In Properties Tab set Source Compatibility and Target Compatibility to 1.8 (Java 8)
        new TabLayoutMediator(tabLayout, viewPage2, (tab, position) -> {
            tab.setText(adapter.getText(position));
            tab.setIcon(adapter.getIcon(position));
            viewPage2.setCurrentItem(position);

        }).attach();


    }



    private void setSystemId() {
        BluetoothGattCharacteristic characteristic = btService.getCharacteristicByUUID(BtStaticVal.SYSTEM_ID);
        Log.i(TAG, "read System ID......> " + characteristic.getProperties());
        byte[] val = new byte[10];
        try {
            val = characteristic.getValue();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        final String txt = new String(val);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getSupportActionBar().setTitle(txt);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Log.i(TAG,"onBackPressed !!!!!!!!!!!");
    }

    /**
     * 请注意注册和注销接收器的位置，比方说，如果您使用 Activity 上下文在 onCreate(Bundle) 中注册接收器，则应在 onDestroy() 中注销，
     * 以防接收器从 Activity 上下文中泄露出去。如果您在 onResume() 中注册接收器，则应在 onPause() 中注销，以防多次注册接收器（如果您不想在暂停时接收广播
     * ，这样可以减少不必要的系统开销）。请勿在 onSaveInstanceState(Bundle) 中注销，因为如果用户在历史记录堆栈中后退，则不会调用此方法。
     */

    @Override
    protected void onPause() {
        super.onPause();
    }




    @Override
    public boolean onTouchEvent(MotionEvent event) {
//        return super.onTouchEvent(event);
        Log.i(TAG, "touch event return true!!!!!!");
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mGattManager !=null){
            mGattManager.close(mDevice);
            mGattManager = null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
}