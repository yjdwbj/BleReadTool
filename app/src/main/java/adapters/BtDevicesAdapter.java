package adapters;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

import bt.lcy.btread.BtStaticVal;
import bt.lcy.btread.R;

public class BtDevicesAdapter extends BaseAdapter {

    private final LayoutInflater inflater;
    private final ArrayList<BluetoothDevice> btDevices;
    private final HashMap<BluetoothDevice,int[]> rssiMap = new HashMap<BluetoothDevice, int[]>();
    private boolean showProgressBar =false;
    private static String TAG = BtDevicesAdapter.class.getName();
    private int showPBarPosition = -1;

    public  BtDevicesAdapter(Context context){
        btDevices = new ArrayList<>();
        inflater = LayoutInflater.from(context);
    }

    public void addDevice(BluetoothDevice device,int rssi,int company){
        if(!btDevices.contains(device)){
            Log.i(TAG,"!!!!Device info "+ device.getAddress() + " , " + device.getName() + "," + device.getUuids());
            btDevices.add(device);
        }
        int [] val = {rssi,company};
        rssiMap.put(device,val);
    }

    public BluetoothDevice getDevice(int pos){
        return btDevices.get(pos);
    }

    public void clear()
    {
        btDevices.clear();
    }

    @Override
    public int getCount() {
        return btDevices.size();
    }

    @Override
    public Object getItem(int position) {
        return btDevices.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        int rssi = 0;
        if(convertView == null){
            convertView = inflater.inflate(R.layout.list_devices,null);
            // 扫描后每一行的设备摘要信息。
            viewHolder = new ViewHolder();
            viewHolder.deviceAddress = (TextView)convertView.findViewById(R.id.device_address);
            viewHolder.deviceImage = (ImageView) convertView.findViewById(R.id.device_image);
            viewHolder.deviceName = (TextView)convertView.findViewById(R.id.device_name);
            viewHolder.deviceRssi = (TextView)convertView.findViewById(R.id.device_rssi_val);
            viewHolder.deviceRssiImg = (ImageView) convertView.findViewById(R.id.device_rssi_img);
            viewHolder.deviceCompany = (TextView) convertView.findViewById(R.id.device_company);
            convertView.setTag(viewHolder);
        }else{
            viewHolder = (ViewHolder)convertView.getTag();
        }

        final  boolean f = showProgressBar && convertView.isSelected();


        BluetoothDevice device = btDevices.get(position);
        final String deviceName = device.getName();
        if(deviceName != null && deviceName.length() >0)
        {
            viewHolder.deviceName.setText(deviceName);
        }else{
            viewHolder.deviceName.setText(R.string.unknown_devices);
        }

        viewHolder.deviceAddress.setText(device.getAddress());
        int[] val = rssiMap.get(device);
        rssi = val[0];
        int company = val[1];
        viewHolder.deviceCompany.setText(BtStaticVal.getCompany(company));
        float sigStrenght  = (rssi + 100 ) * 2;
        viewHolder.deviceRssi.setText(" " + rssi  + " dBm");

        if(sigStrenght < 20){
            viewHolder.deviceRssiImg.setImageResource(R.drawable.ic_signal_cellular_0_bar_24px);
        }else if(sigStrenght < 40){
            viewHolder.deviceRssiImg.setImageResource(R.drawable.ic_signal_cellular_1_bar_24px);
        }else if(sigStrenght < 60){
            viewHolder.deviceRssiImg.setImageResource(R.drawable.ic_signal_cellular_2_bar_24px);
        }else if(sigStrenght < 80){
            viewHolder.deviceRssiImg.setImageResource(R.drawable.ic_signal_cellular_3_bar_24px);
        }else{
            viewHolder.deviceRssiImg.setImageResource(R.drawable.ic_signal_cellular_4_bar_24px);
        }

        return convertView;
    }

    public static class ViewHolder{
        ImageView deviceImage;
        TextView deviceName;
        TextView deviceAddress;
        TextView deviceRssi;
        TextView deviceCompany;
        ImageView deviceRssiImg;
    }
}
