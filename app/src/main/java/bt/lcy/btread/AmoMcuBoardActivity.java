package bt.lcy.btread;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.UUID;

import adapters.BtDevicesAdapter;
import bt.lcy.gatt.CharacteristicChangeListener;
import bt.lcy.gatt.GattManager;
import bt.lcy.gatt.operations.GattCharacteristicWriteOperation;
import bt.lcy.gatt.operations.GattSetNotificationOperation;
import bt.lcy.gatt.operations.GattCharacteristicReadOperation;
import bt.lcy.gatt.GattCharacteristicReadCallback;


import static bt.lcy.btread.BtStaticVal.BT_DEVICE;
import static bt.lcy.btread.BtStaticVal.UUID_KEY_DATA;
import static bt.lcy.btread.BtStaticVal.UUID_CHARA;
import static bt.lcy.btread.BtStaticVal.UUID_CHAR1;
import static bt.lcy.btread.BtStaticVal.getFrom16bitUUID;


public class AmoMcuBoardActivity extends AppCompatActivity {

    private static GattManager mGattManager;

    BluetoothDevice mDevice;

    private TextView textTemp;
    private TextView textHumidity;
    static private Button btnReadAdc45;
    private TextView textPwn;
    private TextView textAdc4;
    private TextView textAdc5;
    private Switch switchLed1;
    private Switch switchLed2;
    private Switch switchLed3;
    private Switch switchLed4;

    private Timer readAdcTimer;


    private static final String TAG = AmoMcuBoardActivity.class.getSimpleName();

    static public BtService btService;
    private boolean isConnected = false;
    private SeekBar seekBar;

    byte[] ledx_value = new byte[1];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_amo_mcu_board);
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        mDevice = (BluetoothDevice) bundle.getParcelable(BT_DEVICE);
        Object[] val = BtDevicesAdapter.getAdvertisingData(mDevice);
        mGattManager = new GattManager(this, UUID_KEY_DATA);
        mGattManager.connectToDevice(mDevice, false);


        textTemp = (TextView) findViewById(R.id.text_temp);
        textHumidity = (TextView) findViewById(R.id.text_humidity);
        btnReadAdc45 = (Button) findViewById(R.id.btn_read_adc45);
        textPwn = (TextView) findViewById(R.id.text_pwm);
        seekBar = (SeekBar) findViewById(R.id.seekBar);

        textAdc4 = (TextView) findViewById(R.id.text_adc4);
        textAdc5 = (TextView) findViewById(R.id.text_adc5);

        // read system ID
        mGattManager.queue(new GattCharacteristicReadOperation(
                mDevice,
                UUID.fromString(BtStaticVal.UUID_KEY_DATA),
                getFrom16bitUUID((short)BtStaticVal.SYSTEM_ID),
                new GattCharacteristicReadCallback() {
                    @Override
                    public void call(final byte[] characteristic) {

                        final String txt = new String(characteristic);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                getSupportActionBar().setTitle(txt);
                            }
                        });
                    }
                })
        );

        ((SeekBar) findViewById(R.id.seekBar)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int i = seekBar.getProgress();
                Log.i(TAG, "seekBar_pwmvalue = " + i);

                byte[] pwmValue = new byte[4];
                pwmValue[0] = pwmValue[1] = pwmValue[2] = pwmValue[3] = (byte) i;

                ByteBuffer bb = ByteBuffer.wrap(pwmValue);
                textPwn.setText(Integer.toHexString(bb.getInt()));

                mGattManager.queue(new GattCharacteristicWriteOperation(
                        mDevice,
                        UUID.fromString(UUID_KEY_DATA),
                        UUID.fromString(UUID_CHARA),
                        pwmValue));

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        btnReadAdc45.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mGattManager.queue(new GattCharacteristicReadOperation(
                        mDevice,
                        UUID.fromString(BtStaticVal.UUID_KEY_DATA),
                        UUID.fromString(BtStaticVal.UUID_CHAR9),
                        new GattCharacteristicReadCallback() {
                            @Override
                            public void call(final byte[] characteristic) {

                                final ByteBuffer wrapped = ByteBuffer.wrap(characteristic);
                                if (wrapped == null) return;

                                int adcval = wrapped.getInt();
                                Log.i(TAG, "read ADC4 ADC5 values :" + adcval);
                                int adc4int = (adcval & 0xffff);
                                int adc5int = (adcval & 0xffff0000) >> 0x10;

                                final String adc4 = String.format(getResources().getString(R.string.adc4), adc4int, (adc4int * 3.3 / 8192));
                                final String adc5 = String.format(getResources().getString(R.string.adc5), adc5int, (adc5int * 3.3 / 8192));

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        textAdc4.setText(adc4);
                                        textAdc5.setText(adc5);
                                    }
                                });
                            }
                        })
                );
            }
        });


        final CompoundButton.OnCheckedChangeListener onCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                final String text = buttonView.getText().toString();
                if (text.equals(getResources().getString(R.string.led1))) {
                    ledx_value[0] = isChecked ? (byte) 0x11 : (byte) 0x10;
                } else if (text.equals(getResources().getString(R.string.led2))) {
                    ledx_value[0] = isChecked ? (byte) 0x21 : (byte) 0x20;
                } else if (text.equals(getResources().getString(R.string.led3))) {
                    ledx_value[0] = isChecked ? (byte) 0x41 : (byte) 0x40;
                } else if (text.equals(getResources().getString(R.string.led4))) {
                    ledx_value[0] = isChecked ? (byte) 0x44 : (byte) 0x43;
                }

                mGattManager.queue(new GattCharacteristicWriteOperation(
                        mDevice,
                        UUID.fromString(UUID_KEY_DATA),
                        UUID.fromString(UUID_CHAR1),
                        ledx_value));
            }
        };


        switchLed1 = (Switch) findViewById(R.id.switch_led1);
        switchLed2 = (Switch) findViewById(R.id.switch_led2);

        switchLed1.setOnCheckedChangeListener(onCheckedChangeListener);
        switchLed2.setOnCheckedChangeListener(onCheckedChangeListener);

        ((Switch) findViewById(R.id.switch_led3)).setOnCheckedChangeListener(onCheckedChangeListener);
        ((Switch) findViewById(R.id.switch_led4)).setOnCheckedChangeListener(onCheckedChangeListener);



        mGattManager.queue(new GattSetNotificationOperation(
                mDevice,
                UUID.fromString(BtStaticVal.UUID_KEY_DATA),
                UUID.fromString(BtStaticVal.UUID_CHAR4),
                UUID.fromString(BtStaticVal.USER_DESCRIPTOR_UUID),
                true
        ));

        // handle DHT11 data
        mGattManager.addCharacteristicChangeListener(UUID.fromString(BtStaticVal.UUID_CHAR4),
                new CharacteristicChangeListener() {
                    @Override
                    public void onCharacteristicChanged(String deviceAddress, BluetoothGattCharacteristic characteristic) {
                        ByteBuffer wrapped = null;
                        try {
                            wrapped = ByteBuffer.wrap(characteristic.getValue());
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }

                        if (wrapped == null) return;
                        final int th = wrapped.getInt();
                        final String temp = getResources().getString(R.string.temp) + Integer.toString((th & 0xffff) >> 0x8) + "℃";
                        final String humidity = getResources().getString(R.string.humidity) + Integer.toString((th & 0xffff0000) >> 0x18) + "%";
                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                textTemp.setText(temp);
                                textHumidity.setText(humidity);
                            }
                        });
                    }
                });

        ((Button) findViewById(R.id.btn_stop_pwn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                byte[] pwmValue = new byte[4];
                pwmValue[0] = pwmValue[1] = pwmValue[2] = pwmValue[3] = 0;
                switchLed1.setChecked(false);
                switchLed2.setChecked(false);
                seekBar.setProgress(0);
                mGattManager.queue(new GattCharacteristicWriteOperation(
                        mDevice,
                        UUID.fromString(UUID_KEY_DATA),
                        UUID.fromString(UUID_CHARA),
                        pwmValue));
            }
        });


        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

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
    protected void onResume() {
        super.onResume();
    }


    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
    }

}
