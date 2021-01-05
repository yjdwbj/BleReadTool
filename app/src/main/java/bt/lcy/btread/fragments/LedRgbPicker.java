package bt.lcy.btread.fragments;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.skydoves.colorpickerview.AlphaTileView;
import com.skydoves.colorpickerview.ColorEnvelope;
import com.skydoves.colorpickerview.ColorPickerView;
import com.skydoves.colorpickerview.flag.FlagView;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;
import com.skydoves.colorpickerview.sliders.BrightnessSlideBar;

import java.util.UUID;

import bt.lcy.btread.R;
import bt.lcy.gatt.GattManager;
import bt.lcy.gatt.operations.GattCharacteristicWriteOperation;

import static bt.lcy.btread.TiMsp432ProjectZeroActivity.UUID_TI_PROJECT_ZERO_LED_CHAR;
import static bt.lcy.btread.TiMsp432ProjectZeroActivity.UUID_TI_PROJECT_ZERO_LED_SERIVCE;
import static com.skydoves.colorpickerview.ActionMode.LAST;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link LedRgbPicker#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LedRgbPicker extends ItemFragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String TAG = LedRgbPicker.class.getName();


    private static GattManager mGattManager;
    private static BluetoothDevice mDevice;
    private boolean tx_reday_falg = true;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    ColorPickerView colorPickerView;

    public  LedRgbPicker(GattManager manager, BluetoothDevice btdevice) {
        mGattManager = manager;
        mDevice = btdevice;
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment LedRgbPicker.
     */
    // TODO: Rename and change types and number of parameters
    public static LedRgbPicker newInstance(String param1, String param2) {
        LedRgbPicker fragment = new LedRgbPicker(mGattManager,mDevice);
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

        return inflater.inflate(R.layout.fragment_led_rgb_picker, container, false);

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        BrightnessSlideBar brightnessSlideBar = view.findViewById(R.id.brightnessSlideBar);
        // https://github.com/skydoves/ColorPickerPreference
        colorPickerView = view.findViewById(R.id.colorPickerView);
        colorPickerView.attachBrightnessSlider(brightnessSlideBar);
        colorPickerView.setActionMode(LAST);
//        colorPickerView.setFlagView(new CustomFlag(this.getContext(),R.layout.flagview));
        // 使用这个控件出来的图像是与TI的APP是镜像的，所以这里反转一下。
        colorPickerView.setScaleX(-1);
        colorPickerView.setScaleY(1);
        colorPickerView.setTranslationX(1);

//        colorPickerView.setColorListener(new ColorListener() {
//            @Override
//            public void onColorSelected(int color, boolean fromUser) {
//                Log.i(TAG," selected color : " + color);
//                LinearLayout linearLayout = view.findViewById(R.id.colorline);
//                linearLayout.setBackgroundColor(color);
//            }
//        });
        colorPickerView.setColorListener(new ColorEnvelopeListener() {
            @Override
            public void onColorSelected(ColorEnvelope envelope, boolean fromUser) {
                LinearLayout linearLayout = view.findViewById(R.id.colorline);
                linearLayout.setBackgroundColor(envelope.getColor());
                String hexColor = envelope.getHexCode();
                byte[] value = hexStringToByteArray(hexColor.substring(2,hexColor.length()));
                if(tx_reday_falg){
                    mGattManager.queue(new GattCharacteristicWriteOperation(
                            mDevice, UUID.fromString(UUID_TI_PROJECT_ZERO_LED_SERIVCE),
                            UUID.fromString(UUID_TI_PROJECT_ZERO_LED_CHAR),value));
                    tx_reday_falg = false;
                    new Handler().postDelayed(
                            new Runnable() {
                                @Override
                                public void run() {
                                    tx_reday_falg = true;
                                }
                            },20
                    );
                }
            }
        });
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len/2];
        for (int i = 0; i < len; i += 2) {
            data[i/2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }




}

class CustomFlag extends FlagView{
    private TextView textView;
    private AlphaTileView alphaTileView;

    public CustomFlag(Context context, int layout) {
        super(context, layout);
        textView = findViewById(R.id.flag_color_code);
        alphaTileView = findViewById(R.id.flag_color_layout);
    }

    @Override
    public void onRefresh(ColorEnvelope colorEnvelope) {
        textView.setText("#" + colorEnvelope.getHexCode());
        alphaTileView.setPaintColor(colorEnvelope.getColor());
    }
}

