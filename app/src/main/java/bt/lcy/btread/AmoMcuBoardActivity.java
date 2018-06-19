package bt.lcy.btread;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.util.HexDumpUtils;

import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;

public class AmoMcuBoardActivity extends AppCompatActivity {

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


    private static final  String TAG = AmoMcuBoardActivity.class.getSimpleName();

    static public BtService btService;
    private boolean isConnected = false;
    private SeekBar seekBar;

    byte[] ledx_value = new byte[1];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_amo_mcu_board);


        textTemp = (TextView) findViewById(R.id.text_temp);
        textHumidity = (TextView)findViewById(R.id.text_humidity);
        btnReadAdc45 = (Button)findViewById(R.id.btn_read_adc45);
        textPwn = (TextView)findViewById(R.id.text_pwm);
        seekBar = (SeekBar)findViewById(R.id.seekBar);

        textAdc4 = (TextView)findViewById(R.id.text_adc4);
        textAdc5 = (TextView)findViewById(R.id.text_adc5);

        btService.readCharateristic(btService.getAmoBoardCharacteristic(BtStaticVal.SYSTEM_ID));

        ((SeekBar)findViewById(R.id.seekBar)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int i= seekBar.getProgress();
                Log.i(TAG, "seekBar_pwmvalue = " + i );

                byte[] pwmValue = new byte[4];
                pwmValue[0] = pwmValue[1] = pwmValue[2] = pwmValue[3] = (byte) i;

                ByteBuffer bb = ByteBuffer.wrap(pwmValue);
                textPwn.setText(Integer.toHexString(bb.getInt()));
                final BluetoothGattCharacteristic characteristic = btService.getAmoBoardCharacteristic(BtStaticVal.UUID_CHARA);
                characteristic.setValue(pwmValue);
                btService.writeCharacteristic(characteristic);
                // 这里延时一下，避免发送得太快
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
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

                btService.readCharateristic(btService.getAmoBoardCharacteristic(BtStaticVal.UUID_CHAR9));
            }
        });



         final CompoundButton.OnCheckedChangeListener onCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
             @Override
             public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                 final BluetoothGattCharacteristic characteristic = btService.getAmoBoardCharacteristic(BtStaticVal.UUID_CHAR1);
                 final String text = buttonView.getText().toString();
                 if(text.equals(getResources().getString(R.string.led1)))
                 {
                     ledx_value[0] = isChecked ? (byte)0x11 : (byte)0x10;
                 }else if(text.equals(getResources().getString(R.string.led2))){
                     ledx_value[0] = isChecked ? (byte)0x21 : (byte)0x20;
                 }else if(text.equals(getResources().getString(R.string.led3)))
                 {
                     ledx_value[0] = isChecked ? (byte)0x41 : (byte)0x40;
                 }else if(text.equals(getResources().getString(R.string.led4)))
                 {
                     ledx_value[0] = isChecked ? (byte)0x44 : (byte)0x43;
                 }


                 characteristic.setValue(ledx_value);

                 btService.writeCharacteristic(characteristic);
                 try {
                     Thread.sleep(100);
                 } catch (InterruptedException e) {
                     e.printStackTrace();
                 }
                 btService.writeCharacteristic(characteristic);
             }
         };



        switchLed1 = (Switch)findViewById(R.id.switch_led1);
        switchLed1.setOnCheckedChangeListener(onCheckedChangeListener);
        switchLed2= (Switch)findViewById(R.id.switch_led2);
        switchLed2.setOnCheckedChangeListener(onCheckedChangeListener);
        ((Switch)findViewById(R.id.switch_led3)).setOnCheckedChangeListener(onCheckedChangeListener);
        ((Switch)findViewById(R.id.switch_led4)).setOnCheckedChangeListener(onCheckedChangeListener);
        ((Button)findViewById(R.id.btn_stop_pwn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                byte[] pwmValue = new byte[4];
                pwmValue[0] = pwmValue[1] = pwmValue[2] = pwmValue[3] = 0;
                switchLed1.setChecked(false);
                switchLed2.setChecked(false);
                seekBar.setProgress(0);
                final BluetoothGattCharacteristic characteristic = btService.getAmoBoardCharacteristic(BtStaticVal.UUID_CHARA);
                characteristic.setValue(pwmValue);
                btService.writeCharacteristic(characteristic);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                btService.writeCharacteristic(characteristic);
            }
        });



        btService.readCharateristic(btService.getAmoBoardCharacteristic(BtStaticVal.SYSTEM_ID));
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        btService.readCharateristic(btService.getAmoBoardCharacteristic(BtStaticVal.SYSTEM_ID));
        btnReadAdc45.callOnClick();


        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        readAdcTimer = new Timer();

        readAdcTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                btnReadAdc45.callOnClick();
            }
        },1000,3000);

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
    static public void setBtService(final  BtService btService)
    {
        AmoMcuBoardActivity.btService = btService;
    }

    private final BroadcastReceiver gattBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final  String action = intent.getAction();

//            Log.i(TAG,"!!!!!Got a BroadCast: " + action);
            if(BtService.ACTION_GATT_DISCONNECTED.equals(action))
            {
                isConnected = false;
                Toast.makeText(getApplicationContext(),R.string.srv_disconnect,Toast.LENGTH_SHORT).show();
                invalidateOptionsMenu();
                onStop();

            }
            else if(BtService.ACTION_DATA_AVAILABLE.equals(action))
            {
                final String uuid = intent.getStringExtra(BtService.EXTRA_SERVICE_UUID);
                final String text = intent.getStringExtra(BtService.EXTRA_TEXT);
                final String cuuid = intent.getStringExtra(BtService.EXTRA_CHARACTERISTIC_UUID);
                if(cuuid.equals(BtStaticVal.UUID_CHAR4))
                {
                    updateTempAndHumidity(cuuid);
                }else if(cuuid.equals(BtStaticVal.UUID_CHAR9))
                {
                    updateAdc45Value();
                }else if(cuuid.equals(BtStaticVal.SYSTEM_ID))
                {
                    setSystemId();
                }

                Log.i(TAG, " ACTION_DATA_AVAILABLE uuid: " + uuid + " cuuid :" + cuuid + " text:" + text);
            }
        }
    };

    private void setSystemId()
    {
        BluetoothGattCharacteristic characteristic =  btService.getAmoBoardCharacteristic(BtStaticVal.SYSTEM_ID);
        Log.i(TAG,"read System ID......>");
        byte[] val = new byte[10];
        try{
            val = characteristic.getValue();
        }catch (NullPointerException  e ) {
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
        registerReceiver(gattBroadcastReceiver,makeGattUpdateIntenFilter());
        if (btService != null) {
            final boolean result = btService.connect();
            Log.d(TAG, "Connect request result=" + result);
        }
        btService.setCharacteristicNotification(btService.getAmoBoardCharacteristic(BtStaticVal.UUID_CHAR4), true);

    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(gattBroadcastReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
        readAdcTimer.cancel();

        if(btService != null) {
            btService.setCharacteristicNotification(btService.getAmoBoardCharacteristic(BtStaticVal.UUID_CHAR4), false);
            btService.onDestroy();
            btService = null;
        }


    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
    }

    @SuppressLint("StringFormatInvalid")
    private void updateAdc45Value()
    {
        final BluetoothGattCharacteristic characteristic= btService.getAmoBoardCharacteristic(BtStaticVal.UUID_CHAR9);

        byte[] adc45 = new byte[4];
        try{
            adc45 = characteristic.getValue();
        }catch (NullPointerException  e ) {
            e.printStackTrace();
        }
        final ByteBuffer wrapped = ByteBuffer.wrap(adc45);
        if(wrapped == null ) return;

        int adcval = wrapped.getInt();
        Log.i(TAG,"read ADC4 ADC5 values :" + adcval );
        int adc4int = (adcval & 0xffff);
        int adc5int = (adcval & 0xffff0000) >> 0x10;

        final String adc4 = String.format(getResources().getString(R.string.adc4),adc4int,(adc4int*3.3 /8192));
        final String adc5 = String.format(getResources().getString(R.string.adc5),adc5int,(adc5int*3.3 /8192));

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textAdc4.setText(adc4);
                textAdc5.setText(adc5);
            }
        });

    }

    private void updateTempAndHumidity(final String uuid)
    {
        final BluetoothGattCharacteristic characteristic= btService.getAmoBoardCharacteristic(uuid);

        ByteBuffer wrapped = null;
        try{
           wrapped = ByteBuffer.wrap(characteristic.getValue());
        }catch (NullPointerException  e ) {
            e.printStackTrace();
        }

        if(wrapped == null ) return;
        final int th = wrapped.getInt();
//        Log.i(TAG,"get Temp humidity is :"  +th);
        final String temp = getResources().getString(R.string.temp) + Integer.toString( (th & 0xffff) >> 0x8) + "℃";
        final String humidity = getResources().getString(R.string.humidity) +  Integer.toString ((th & 0xffff0000) >> 0x18 ) + "%";
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                textTemp.setText(temp);
                textHumidity.setText(  humidity);
            }
        });

    }

    private static IntentFilter makeGattUpdateIntenFilter(){
        final IntentFilter intentFilter = new IntentFilter();
//        intentFilter.addAction(BtService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BtService.ACTION_GATT_DISCONNECTED);
//        intentFilter.addAction(BtService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BtService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }


}
