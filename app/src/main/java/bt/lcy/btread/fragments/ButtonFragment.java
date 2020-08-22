package bt.lcy.btread.fragments;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.skydoves.colorpickerview.sliders.BrightnessSlideBar;

import java.util.UUID;

import adapters.ViewPagerFragmentAdapter;
import bt.lcy.btread.BtStaticVal;
import bt.lcy.btread.R;
import bt.lcy.btread.TiMsp432ProjectZeroActivity;
import bt.lcy.gatt.CharacteristicChangeListener;
import bt.lcy.gatt.GattManager;
import bt.lcy.gatt.GattOperationBundle;
import bt.lcy.gatt.operation.GattSetNotificationOperation;

import static android.media.ToneGenerator.TONE_CDMA_ONE_MIN_BEEP;
import static com.skydoves.colorpickerview.ActionMode.ALWAYS;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ButtonFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ButtonFragment extends Fragment {
    // 参考 https://developer.android.com/guide/navigation/navigation-swipe-view-2#java
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String TAG = ButtonFragment.class.getName();

    private static GattManager mGattManager;
    private static BluetoothDevice mDevice;

    private ToneGenerator button1ToneRun;
    private ToneGenerator button2ToneRun;
    ImageView button1;
    ImageView button2;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;


    public ButtonFragment(GattManager manager, BluetoothDevice btdevice) {
        mGattManager = manager;
        mDevice = btdevice;
        button1ToneRun = new ToneGenerator(AudioManager.STREAM_VOICE_CALL, 90);
        button2ToneRun = new ToneGenerator(AudioManager.STREAM_VOICE_CALL, 80);
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ButtonFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ButtonFragment newInstance(String param1, String param2) {
        ButtonFragment fragment = new ButtonFragment(mGattManager,mDevice);
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_button, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        button1 = view.findViewById(R.id.button_sw1);
        button2 = view.findViewById(R.id.button_sw2);

        GattOperationBundle bundle = new GattOperationBundle();
        bundle.addOperation(new GattSetNotificationOperation(
                mDevice, UUID.fromString(BtStaticVal.UUID_TI_PROJECT_ZERO_SW),
                UUID.fromString(BtStaticVal.UUID_TI_PROJECT_ZERO_SW1_STATUS),
                UUID.fromString(BtStaticVal.CCC_DESCRIPTOR_UUID),
                true
        ));
        bundle.addOperation(new GattSetNotificationOperation(
                mDevice, UUID.fromString(BtStaticVal.UUID_TI_PROJECT_ZERO_SW),
                UUID.fromString(BtStaticVal.UUID_TI_PROJECT_ZERO_SW2_STATUS),
                UUID.fromString(BtStaticVal.CCC_DESCRIPTOR_UUID),
                true
        ));

        mGattManager.queue(bundle);

        mGattManager.addCharacteristicChangeListener(UUID.fromString(BtStaticVal.UUID_TI_PROJECT_ZERO_SW1_STATUS), new CharacteristicChangeListener() {
            @Override
            public void onCharacteristicChanged(String deviceAddress, BluetoothGattCharacteristic characteristic) {
                final int lsb = characteristic.getValue()[0] & 0xff;
                if (lsb == 1) {
                    button1ToneRun.startTone(ToneGenerator.TONE_DTMF_C, 1000000);
                    button1.setBackgroundResource(R.drawable.ic_button_s1_touched);
                } else {
                    button1.setBackgroundResource(R.drawable.ic_button_s1_released);
                    button1ToneRun.stopTone();
                }
            }
        });

        mGattManager.addCharacteristicChangeListener(UUID.fromString(BtStaticVal.UUID_TI_PROJECT_ZERO_SW2_STATUS), new CharacteristicChangeListener() {
            @Override
            public void onCharacteristicChanged(String deviceAddress, BluetoothGattCharacteristic characteristic) {
                final int lsb = characteristic.getValue()[0] & 0xff;
                if (lsb == 1) {
                    button2ToneRun.startTone(ToneGenerator.TONE_DTMF_A, 1000000);
                    button2.setBackgroundResource(R.drawable.ic_button_s2_touched);
                } else {
                    button2.setBackgroundResource(R.drawable.ic_button_s2_released);
                    button2ToneRun.stopTone();
                }
            }
        });

    }



}