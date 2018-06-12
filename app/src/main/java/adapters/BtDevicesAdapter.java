package adapters;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

import bt.lcy.btread.R;

public class BtDevicesAdapter extends BaseAdapter {
    private final LayoutInflater inflater;
    private final ArrayList<BluetoothDevice> btDevices;
    private final HashMap<BluetoothDevice,Integer> rssiMap = new HashMap<>();

    public BtDevicesAdapter(Context context){
        btDevices = new ArrayList<>();
        inflater = LayoutInflater.from(context);
    }



    public void addDevice(BluetoothDevice device,int rssi){
        if(!btDevices.contains(device)){
            btDevices.add(device);
        }
        rssiMap.put(device,rssi);
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
        if(convertView == null){
            convertView = inflater.inflate(R.layout.list_devices,null);

            viewHolder = new ViewHolder();
            viewHolder.deviceAddress = (TextView)convertView.findViewById(R.id.device_address);
            viewHolder.deviceImage = (ImageView) convertView.findViewById(R.id.device_image);
            viewHolder.deviceName = (TextView)convertView.findViewById(R.id.device_name);
            viewHolder.deviceRssi = (TextView)convertView.findViewById(R.id.device_rssi);
            convertView.setTag(viewHolder);
        }else{
            viewHolder = (ViewHolder)convertView.getTag();
        }

        viewHolder.deviceImage.setVisibility(View.VISIBLE);

        BluetoothDevice device = btDevices.get(position);
        final String deviceName = device.getName();
        if(deviceName != null && deviceName.length() >0)
        {
            viewHolder.deviceName.setText(deviceName);
        }else{
            viewHolder.deviceName.setText(R.string.unknown_devices);
        }

        viewHolder.deviceAddress.setText(device.getAddress());
        viewHolder.deviceRssi.setText(" " + rssiMap.get(device) + " dBm");
        return convertView;
    }

    private static class ViewHolder{
        ImageView deviceImage;
        TextView deviceName;
        TextView deviceAddress;
        TextView deviceRssi;
    }
}
