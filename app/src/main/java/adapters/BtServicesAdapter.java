package adapters;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import bt.lcy.btread.BtStaticVal;
import bt.lcy.btread.ConsoleActivity;
import bt.lcy.btread.R;



public class BtServicesAdapter extends BaseExpandableListAdapter {

    private static final String TAG = BtServicesAdapter.class.getSimpleName();




    public interface onServiceItemClickListener {
        public  void onItemClick(BluetoothGattService  service);
        public  void onServiceEnabled(BluetoothGattService service,boolean enabled);
        public void  onServiceUpdate(BluetoothGattService service);
    }
    private onServiceItemClickListener serviceItemClickListener;
    private  Context context;

    private final LayoutInflater inflater;
    private final ArrayList<BluetoothGattService> serviceArrayList;
    private final HashMap<BluetoothGattService,ArrayList<BluetoothGattCharacteristic>> hashMap;

    public BtServicesAdapter(Context context, List<BluetoothGattService> gattServiceList)
    {
        inflater = LayoutInflater.from(context);
        this.context = context;
        serviceArrayList = new ArrayList<>(gattServiceList.size());
        hashMap = new HashMap<BluetoothGattService, ArrayList<BluetoothGattCharacteristic>>(gattServiceList.size());


        for(BluetoothGattService service : gattServiceList){
            final  List<BluetoothGattCharacteristic> gattCharacteristics = service.getCharacteristics();
            hashMap.put(service,new ArrayList<BluetoothGattCharacteristic>(gattCharacteristics));
            serviceArrayList.add(service);
        }
        Log.i(TAG,"hashMap size: " + hashMap.size());

    }


    public ArrayList<BluetoothGattService> getGattServices()
    {
        return serviceArrayList;
    }

    public void setServiceListener(onServiceItemClickListener serviceListener)
    {
        this.serviceItemClickListener = serviceListener;
    }


    @Override
    public int getGroupCount() {
        return serviceArrayList.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return hashMap.get(getGroup(groupPosition)).size();
    }

    @Override
    public BluetoothGattService getGroup(int groupPosition) {
       return serviceArrayList.get(groupPosition);
    }

    @Override
    public BluetoothGattCharacteristic getChild(int groupPosition, int childPosition) {

//        Log.i(TAG, "Group: " + groupPosition + " child: " + childPosition);
//        Log.i(TAG,"uuid: " + hashMap.get(getGroup(groupPosition)).get(childPosition).getUuid());
        return hashMap.get(getGroup(groupPosition)).get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return groupPosition * 100 + childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        final  GroupViewHolder viewHolder;
        if(convertView == null){
            convertView = inflater.inflate(R.layout.list_group,parent,false);
            viewHolder = new GroupViewHolder();

            viewHolder.cname = (TextView)convertView.findViewById(R.id.group_name);
            viewHolder.uuid = (TextView)convertView.findViewById(R.id.group_uuid);
            viewHolder.imageView = (ImageView)convertView.findViewById(R.id.group_img);
//
            convertView.setTag(viewHolder);
        }else{
            viewHolder = (GroupViewHolder)convertView.getTag();
        }


        final BluetoothGattService service = serviceArrayList.get(groupPosition);
        final String uuid = service.getUuid().toString();
        final String subuuid = uuid.substring(0,8);
        viewHolder.uuid.setText(uuid);
        viewHolder.cname.setText(BtStaticVal.getServices(subuuid));
        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        final  ChildViewHolder holder;

        final BluetoothGattCharacteristic characteristic =  hashMap.get(serviceArrayList.get(groupPosition)).get(childPosition);
        final String uuid = characteristic.getUuid().toString();


        final int properties = characteristic.getProperties();
//        Log.i(TAG,"Open ChildView.... " );
        if(convertView == null)
        {
            holder = new ChildViewHolder();

            convertView = inflater.inflate(R.layout.list_characteristic,parent,false);
            holder.cname = (TextView)convertView.findViewById(R.id.c_name);
            holder.uuid = (TextView)convertView.findViewById(R.id.c_uuid);
            holder.imageView = (ImageView) convertView.findViewById(R.id.c_image);
            holder.attr = (TextView)convertView.findViewById(R.id.c_attr);
            convertView.setTag(holder);
        }else{
            holder = (ChildViewHolder) convertView.getTag();
        }


        String attr ="";

        if(0 != (properties & BluetoothGattCharacteristic.PROPERTY_READ))
        {
            attr = "Read";
        }

        if(0 != (properties & BluetoothGattCharacteristic.PROPERTY_WRITE))
        {
           attr +=  attr.isEmpty() ?  "Write" : " Write";
        }

        if(0 != (properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY))
        {
            attr +=  attr.isEmpty() ?  "Notify" : " Notify";
        }

        final String uuidstr = characteristic.getUuid().toString();
        final String subuuid = uuidstr.substring(0,8);
//        Log.i(TAG,"sub uuid is :" + subuuid);
        holder.attr.setText(attr);
        holder.cname.setText(BtStaticVal.getCharacteristics(subuuid));

        holder.uuid.setText(uuidstr);
        return convertView;
    }


    // 这里必须返回true,子项目才能点击
    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }


    private static class GroupViewHolder{
        public TextView cname;
        public TextView uuid;
        public ImageView imageView;
//        public View view;
    }

    private static  class ChildViewHolder{
        public BluetoothGattService service;
        public TextView cname;
        public TextView uuid;
        public TextView attr;
        public ImageView imageView;

    }
}
