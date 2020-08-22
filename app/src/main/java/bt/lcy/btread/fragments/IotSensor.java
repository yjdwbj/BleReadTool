package bt.lcy.btread.fragments;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.opengl.ETC1Util;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.textfield.TextInputEditText;

import java.net.URI;
import java.util.UUID;

import bt.lcy.btread.BtStaticVal;
import bt.lcy.btread.R;
import bt.lcy.gatt.CharacteristicChangeListener;
import bt.lcy.gatt.GattManager;
import bt.lcy.gatt.GattOperationBundle;
import bt.lcy.gatt.operation.GattSetNotificationOperation;
import lcy.gles3d.IotGLSurfaceView;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link IotSensor#newInstance} factory method to
 * create an instance of this fragment.
 */
public class IotSensor extends Fragment {

    // https://guides.codepath.com/android/creating-and-using-fragments
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final double  PRESSURE_OF_SEA = 101325.0;

    private  static final String TAG = IotSensor.class.getName();
    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private static GattManager mGattManager;
    private static BluetoothDevice mDevice;
    private TextInputEditText temp;
    private TextInputEditText  humi;
    private TextInputEditText alt;

    private TextInputEditText aX,aY,aZ;

    private IotGLSurfaceView gLView;
    //        private ModelViewerGUI gui;
    /**
     * Type of model if file name has no extension (provided though content provider)
     */
    private int paramType;
    /**
     * The file to load. Passed as input parameter
     */
    private URI paramUri;


    public IotSensor(GattManager manager, BluetoothDevice btdevice) {
        mGattManager = manager;
        mDevice = btdevice;
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment IotSensor.
     */
    // TODO: Rename and change types and number of parameters
    public static IotSensor newInstance(String param1, String param2) {
        IotSensor fragment = new IotSensor(mGattManager,mDevice);
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

        return  inflater.inflate(R.layout.fragment_iot_sensor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.setVisibility(View.VISIBLE);
        float []mMMatrix = new float[16];
        // openGL ES 参考： https://developer.android.com/guide/topics/graphics/opengl#gl-extension-query

        humi = view.findViewById(R.id.iot_edit_humi);
        temp = view.findViewById(R.id.iot_edit_temp);
        alt = view.findViewById(R.id.iot_edit_altitude);


        aX = view.findViewById(R.id.iot_input_x);
        aY = view.findViewById(R.id.iot_input_y);
        aZ = view.findViewById(R.id.iot_input_z);



        gLView = view.findViewById(R.id.glsurfaceview);

//        SceneRenderer renderer = new SceneRenderer();
//        AirHockeyRenderer renderer = new AirHockeyRenderer(getContext());
//        MyGLRenderer renderer = new MyGLRenderer();
//        gLView.setRenderer(renderer);

        GattOperationBundle hmc5883 = new GattOperationBundle();
        hmc5883.addOperation(new GattSetNotificationOperation(
                mDevice, UUID.fromString(BtStaticVal.UUID_TI_PROJECT_ZERO_DATA),
                UUID.fromString(BtStaticVal.UUID_TI_PROJECT_ZERO_DATA_HMC5883L),
                UUID.fromString(BtStaticVal.CCC_DESCRIPTOR_UUID),
                true
        ));
        mGattManager.queue(hmc5883);
        GattOperationBundle bmp180 = new GattOperationBundle();
        bmp180.addOperation(new GattSetNotificationOperation(
                mDevice, UUID.fromString(BtStaticVal.UUID_TI_PROJECT_ZERO_DATA),
                UUID.fromString(BtStaticVal.UUID_TI_PROJECT_ZERO_DATA_BMP180),
                UUID.fromString(BtStaticVal.CCC_DESCRIPTOR_UUID),
                true
        ));
        mGattManager.queue(bmp180);

        mGattManager.addCharacteristicChangeListener(UUID.fromString(BtStaticVal.UUID_TI_PROJECT_ZERO_DATA_HMC5883L),
                new CharacteristicChangeListener() {
            @Override
            public void onCharacteristicChanged(String deviceAddress, BluetoothGattCharacteristic characteristic) {
                byte [] raw = characteristic.getValue();
                String val = String.format("%02x %02x %02x %02x %02x %02x",raw[0],raw[1],raw[2],raw[3],raw[4],raw[5]);
                float x = raw[0] << 8 | raw[1];
                float z = raw[2] << 8 | raw[3];
                float y = raw[4] << 8 | raw[5];

                if(x > 0x7fff)
                    x-=0xffff;
                if(y > 0x7fff)
                    y-=0xffff;
                if(z > 0x7fff)
                    z-=0xffff;

                final float xx =x;
                final float yy =y;
                final float zz =z;
                ((AppCompatActivity)view.getContext()).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        aX.setText(String.format("%.2f",xx));
                        aY.setText(String.format("%.2f",yy));
                        aZ.setText(String.format("%.2f",zz));
                    }
                });

//                Log.i(TAG,"Read Iot data : x " + x + " ,y " + y + ",z " + z );
//                Matrix.setRotateM(mMMatrix,0,0,x,y,z);
                if(gLView !=null)
                    gLView.updateXYZ(x,y,z);
            }
        });

        mGattManager.addCharacteristicChangeListener(UUID.fromString(BtStaticVal.UUID_TI_PROJECT_ZERO_DATA_BMP180),
                new CharacteristicChangeListener() {
                    @Override
                    public void onCharacteristicChanged(String deviceAddress, BluetoothGattCharacteristic characteristic) {
                        String raw = new String(characteristic.getValue());
//                        Log.i(TAG,"BMP raw data : " + raw);
                        String [] list =  raw.split(",");
                        Float temperature =  Float.parseFloat(list[0]);
                        Float pressure = Float.parseFloat(list[1]);
                        Double altitude = 44330 * ( 1- Math.pow(pressure / PRESSURE_OF_SEA ,1.0 / 5.255));
                        ((AppCompatActivity)view.getContext()).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                temp.setText(temperature.toString()+"℃");
                                humi.setText(pressure.toString()+"㎩");
                                alt.setText(String.format("%.2f",altitude)+"米");
                            }
                        });

                    }
                });

        Log.i(TAG,"isETC1Supported() : " + ETC1Util.isETC1Supported());


    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }



    @Override
    public void onPause() {
        super.onPause();
        if( gLView != null) {
            gLView.onPause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if( gLView != null) {
            gLView.onResume();
        }
    }


}



