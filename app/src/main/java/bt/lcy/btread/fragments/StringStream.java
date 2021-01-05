package bt.lcy.btread.fragments;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputEditText;

import java.util.UUID;

import bt.lcy.btread.BtStaticVal;
import bt.lcy.btread.R;
import bt.lcy.btread.TiMsp432ProjectZeroActivity;
import bt.lcy.gatt.CharacteristicChangeListener;
import bt.lcy.gatt.GattManager;
import bt.lcy.gatt.operations.GattCharacteristicWriteOperation;
import bt.lcy.gatt.operations.GattSetNotificationOperation;

import static bt.lcy.btread.TiMsp432ProjectZeroActivity.UUID_TI_PROJECT_ZERO_DATA;
import static bt.lcy.btread.TiMsp432ProjectZeroActivity.UUID_TI_PROJECT_ZERO_DATA_N;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link StringStream#newInstance} factory method to
 * create an instance of this fragment.
 */
public class StringStream extends ItemFragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private TextView textArea;
    private Button sendButton;
    private boolean write_no_resp = false;
    private TextInputEditText textInputEditText;

    private static GattManager mGattManager;
    private static  BluetoothDevice mDevice;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    TiMsp432ProjectZeroActivity mActivity;

    public  StringStream(GattManager manager, BluetoothDevice btdevice) {
        mGattManager = manager;
        mDevice = btdevice;
    }

    public void setActivity(TiMsp432ProjectZeroActivity activity) {
        mActivity = activity;
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment StringStream.
     */
    // TODO: Rename and change types and number of parameters
    public static StringStream newInstance(String param1, String param2) {
        StringStream fragment = new StringStream(mGattManager,mDevice);
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
        return inflater.inflate(R.layout.fragment_string_stream, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final ScrollView scrollView = view.findViewById(R.id.log_textarea);
        sendButton = view.findViewById(R.id.send_string);
        textArea = view.findViewById(R.id.log_content);
        textArea.setMovementMethod(new ScrollingMovementMethod());
        textInputEditText = view.findViewById(R.id.string_input);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String inputext = textInputEditText.getText().toString();
                if (inputext.length() > 0) {
                    Long tsLong = System.currentTimeMillis() / 1000;
                    String text = inputext.length() > 20 ? inputext.substring(0, 20) : inputext;
                    textArea.append( "\r\n" + tsLong.toString() + ",write:" + text + ",len:" + text.length() + "\r\n");
                    textInputEditText.setText("");

                    mGattManager.queue(new GattCharacteristicWriteOperation(
                            mDevice,
                            UUID.fromString(UUID_TI_PROJECT_ZERO_DATA),
                            UUID.fromString(UUID_TI_PROJECT_ZERO_DATA_N),
                            text.getBytes())
                    );
//                        mGattManager.queue(new GattCharacteristicWriteOperation(
//                                mDevice,
//                                UUID.fromString(BtStaticVal.UUID_TI_PROJECT_ZERO_DATA),
//                                UUID.fromString(BtStaticVal.UUID_TI_PROJECT_ZERO_DATA_W),
//                                text.getBytes())
//                        );
                    // 再把它读回来。
//                        mGattManager.queue(new GattCharacteristicReadOperation(
//                                mDevice,
//                                UUID.fromString(BtStaticVal.UUID_TI_PROJECT_ZERO_DATA),
//                                UUID.fromString(BtStaticVal.UUID_TI_PROJECT_ZERO_DATA_W),
//                                new GattCharacteristicReadCallback() {
//                                    @Override
//                                    public void call(final byte[] characteristic) {
//                                        getActivity().runOnUiThread(new Runnable() {
//                                            @Override
//                                            public void run() {
//                                                Long tsLong = System.currentTimeMillis() / 1000;
//                                                String ts = tsLong.toString();
//                                                textArea.append(ts + ",读回:" + new String(characteristic) + "\r\n");
//                                            }
//                                        });
//                                    }
//                                })
//                        );
                }
            }


        });

        // set notification true
        mGattManager.queue(new GattSetNotificationOperation(
                mDevice,
                UUID.fromString(UUID_TI_PROJECT_ZERO_DATA),
                UUID.fromString(UUID_TI_PROJECT_ZERO_DATA_N),
                UUID.fromString(BtStaticVal.CCC_DESCRIPTOR_UUID),
                true
        ));

        // handle notification event.
        mGattManager.addCharacteristicChangeListener(
                UUID.fromString(UUID_TI_PROJECT_ZERO_DATA_N),
                new CharacteristicChangeListener() {
                    @Override
                    public void onCharacteristicChanged(String deviceAddress, final BluetoothGattCharacteristic characteristic) {
                        ((AppCompatActivity)view.getContext()).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
//                                    addMessage(dateFormat.format(new Date()) + ": Value of " + characteristic.getStringValue(0) + " NOTIFIED\n");
                                Long tsLong = System.currentTimeMillis() / 1000;
                                String ts = tsLong.toString();
                                String text = new String(characteristic.getValue());
                                String colortxt = "<font color=#000080>" + ts + ",read:" + text + ",len:" + text.length()+ "</font>\r\n";
                                textArea.append(Html.fromHtml(colortxt));
//                                scrollView.scrollTo(0,scrollView.getBottom());
                                scrollView.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                                    }
                                });
                            }
                        });
                    }
                }
        );

    }


}