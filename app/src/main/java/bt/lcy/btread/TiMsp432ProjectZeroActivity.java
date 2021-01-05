package bt.lcy.btread;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;
import androidx.viewpager2.widget.ViewPager2;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import adapters.ViewPagerFragmentAdapter;
import bt.lcy.gatt.GattManager;

import static androidx.appcompat.app.ActionBar.DISPLAY_SHOW_CUSTOM;
import static bt.lcy.btread.BtStaticVal.BT_DEVICE;

public class TiMsp432ProjectZeroActivity extends AppCompatActivity {

    public static String UUID_TI_PROJECT_ZERO = "f0001110-0451-4000-b000-000000000000";
    public static String UUID_TI_PROJECT_ZERO_LED_SERIVCE = "f0001110-0451-4000-b000-000000000000";


    //  在 simplelink_cc2640r2_sdk_4_20_00_04/examples/rtos/CC2640R2_LAUNCHXL/blestack/project_zero/README.html 里定义是 RED r/w
    //  在 simplelink_sdk_ble_plugin_3_20_00_24/examples/rtos/MSP_EXP432E401Y/bluetooth/project_zero/README.html 里定义是  LED color r/w
    public static String UUID_TI_PROJECT_ZERO_LED_CHAR = "f0001111-0451-4000-b000-000000000000";

    //  在 simplelink_cc2640r2_sdk_4_20_00_04/examples/rtos/CC2640R2_LAUNCHXL/blestack/project_zero/README.html 里定义是 GREEN r/w
    public static String UUID_TI_PROJECT_ZERO_LED_G_STATUS = "f0001112-0451-4000-b000-000000000000";
    public static String UUID_TI_PROJECT_ZERO_SW = "f0001120-0451-4000-b000-000000000000";

    //    simplelink_sdk_ble_plugin_3_20_00_24/examples/rtos/MSP_EXP432E401Y/bluetooth/project_zero/README.html 里定义 SW1 notification
    public static String UUID_TI_PROJECT_ZERO_SW1_STATUS = "f0001121-0451-4000-b000-000000000000";
    public static String UUID_TI_PROJECT_ZERO_SW2_STATUS = "f0001122-0451-4000-b000-000000000000";
    public static String UUID_TI_PROJECT_ZERO_DATA = "f0001130-0451-4000-b000-000000000000";
    //  在 simplelink_cc2640r2_sdk_4_20_00_04/examples/rtos/CC2640R2_LAUNCHXL/blestack/project_zero/README.html 里定义是 DATA r/w
    //  在 simplelink_cc2640r2_sdk_4_20_00_04/examples/rtos/CC2640R2_LAUNCHXL/blestack/project_zero/README.html 里定义是 DATA UTF8-string r/w
    public static String UUID_TI_PROJECT_ZERO_DATA_W = "f0001131-0451-4000-b000-000000000000";

    //  在 simplelink_cc2640r2_sdk_4_20_00_04/examples/rtos/CC2640R2_LAUNCHXL/blestack/project_zero/README.html 里定义是 DATA notification/W-N-R
    public static String UUID_TI_PROJECT_ZERO_DATA_N = "f0001132-0451-4000-b000-000000000000";
    // 为hmc5883l 三轴传感器,自定义特征码 0x1133.
    public static String UUID_TI_PROJECT_ZERO_DATA_HMC5883L = "f0001133-0451-4000-b000-000000000000";
    // 为DHT11 温湿度传感器,自定义特征码 0x1134.
    public static String UUID_TI_PROJECT_ZERO_DATA_DHT11 = "f0001134-0451-4000-b000-000000000000";
    // bmp180 传感器
    public static String UUID_TI_PROJECT_ZERO_DATA_BMP180 = "f0001135-0451-4000-b000-000000000000";


    private String TAG = TiMsp432ProjectZeroActivity.class.getName();
    static public BtService btService;
    // 参考 https://developer.android.com/guide/navigation/navigation-swipe-view-2
    ViewPager2 mViewPage2;
    ViewPagerFragmentAdapter mAdapter;
    private GattManager mGattManager;

    BluetoothDevice mDevice;

    public static Set<String> Profiles =
            new HashSet<String>(Arrays.asList(
                    UUID_TI_PROJECT_ZERO,
                    UUID_TI_PROJECT_ZERO_LED_SERIVCE,
                    UUID_TI_PROJECT_ZERO_SW,
                    UUID_TI_PROJECT_ZERO_DATA)
            );

    public static boolean isVaildUUID() {
        for (final BluetoothGattService service : BtService.mDeviceServices) {
            if (Profiles.contains(service.getUuid().toString())) {
                return true;
            }
        }
        return false;
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

                        if (mViewPage2 != null) {
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
        mGattManager = new GattManager(TiMsp432ProjectZeroActivity.this, UUID_TI_PROJECT_ZERO);
        mGattManager.connectToDevice(mDevice, false);

        // Detect Bluetooth on/off state
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);

        // 参考 Activity生命周期  https://developer.android.com/guide/components/activities/activity-lifecycle
        // https://pspdfkit.com/blog/2019/using-the-bottom-navigation-view-in-android/
        setContentView(R.layout.ti_msp432_project_zero_viewpager2);
        TabLayout tabLayout = findViewById(R.id.tab_layout);

        mViewPage2 = findViewById(R.id.pager2);
        mAdapter = new ViewPagerFragmentAdapter(this);
        mViewPage2.setAdapter(mAdapter);
//        viewPage2.setUserInputEnabled(false);
//        new TabLayoutMediator(tabLayout, viewPage2, new TabLayoutMediator.TabConfigurationStrategy() {
//            @Override
//            public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
//                tab.setText("Tab "+ (position+1));
//                viewPage2.setCurrentItem(position);
//            }
//        });
        // 开启Lambda支持。 https://stackoverflow.com/questions/22703412/java-lambda-expressions-not-supported-at-this-language-level
        // For Android 3.0+ Go File → Project Structure → Module → app and In Properties Tab set Source Compatibility and Target Compatibility to 1.8 (Java 8)
        new TabLayoutMediator(tabLayout, mViewPage2, (tab, position) -> {
            tab.setText(mAdapter.getText(position));
            tab.setIcon(mAdapter.getIcon(position));
//            viewPage2.setCurrentItem(position);
        }).attach();


        getSupportActionBar().setDisplayOptions(DISPLAY_SHOW_CUSTOM);
        View viewActionBar = getLayoutInflater().inflate(R.layout.dev_connected,null);
        //getSupportActionBar().setCustomView(R.layout.dev_connected);
        getSupportActionBar().setCustomView(viewActionBar);
        TextView title = (TextView) viewActionBar.findViewById(R.id.dev_title);
        title.setText(Html.fromHtml( mDevice.getName() + "<br>" + mDevice.getAddress()));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home: {
                onBackPressed();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Log.i(TAG, "onBackPressed !!!!!!!!!!!");

        try{
            if(mReceiver!=null)
                unregisterReceiver(mReceiver);
        }catch(Exception e)
        {

        }

        if (mGattManager!= null) {
            mGattManager.close(mDevice);
            mGattManager = null;
        }
        finish();
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
    protected void onDestroy() {

        try{
            if(mReceiver!=null)
                unregisterReceiver(mReceiver);
        }catch(Exception e)
        {

        }
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
}