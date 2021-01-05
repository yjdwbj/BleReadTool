package adapters;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.provider.ContactsContract;
import android.text.Html;
import android.util.Log;
import android.util.MonthDisplayHelper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.w3c.dom.Text;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import bt.lcy.btread.BtDeviceServicesActivity;
import bt.lcy.btread.BtStaticVal;
import bt.lcy.btread.ConsoleActivity;
import bt.lcy.btread.MainActivity;
import bt.lcy.btread.R;
import bt.lcy.db.BleDataBase;
import bt.lcy.gatt.CharacteristicChangeListener;
import bt.lcy.gatt.GattCharacteristicReadCallback;
import bt.lcy.gatt.GattManager;
import bt.lcy.gatt.operations.GattCharacteristicReadOperation;
import bt.lcy.gatt.operations.GattSetNotificationOperation;

import static adapters.BtDevicesAdapter.getHexData;
import static adapters.BtDevicesAdapter.getUUID16;
import static bt.lcy.btread.BtStaticVal.CLIENT_CHARACTERISTIC_CONFIGURATION;
import static bt.lcy.btread.BtStaticVal.USER_DESCRIPTOR_UUID;
import static bt.lcy.btread.BtStaticVal.getAppearance;
import static bt.lcy.btread.BtStaticVal.APPEARANCE;
import static bt.lcy.btread.BtStaticVal.DEVICE_NAME;
import static bt.lcy.btread.BtStaticVal.PNP_ID;
import static bt.lcy.btread.BtStaticVal.getDescriptor;
import static bt.lcy.btread.BtStaticVal.getFrom16bitUUID;
import static bt.lcy.btread.BtStaticVal.getServices;


public class BtServicesAdapter extends BaseExpandableListAdapter {

    private static final String TAG = BtServicesAdapter.class.getSimpleName();

    private Context mContext;
    private BluetoothDevice mDevice;
    private GattManager mGattManager;
    private BleDataBase database;

    private final LayoutInflater inflater;
    private final ArrayList<BluetoothGattService> serviceArrayList;

    public BtServicesAdapter(Context context, List<BluetoothGattService> gattServiceList) {
        inflater = LayoutInflater.from(context);
        mContext = context;

        mDevice = ((BtDeviceServicesActivity) mContext).getDevice();
        serviceArrayList = new ArrayList<>(gattServiceList.size());
        mGattManager = ((BtDeviceServicesActivity) mContext).getGattManager();

        database = BleDataBase.getInstance(mContext);

        //  iterator search every services .
        for (BluetoothGattService service : gattServiceList) {
            final List<BluetoothGattCharacteristic> gattCharacteristics = service.getCharacteristics();
            serviceArrayList.add(service);
        }
    }

    @Override
    public int getGroupCount() {
        return serviceArrayList.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return serviceArrayList.get(groupPosition).getCharacteristics().size();
    }

    @Override
    public BluetoothGattService getGroup(int groupPosition) {
        return serviceArrayList.get(groupPosition);
    }

    @Override
    public BluetoothGattCharacteristic getChild(int groupPosition, int childPosition) {
        return getGroup(groupPosition).getCharacteristics().get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return 2000000 + groupPosition + childPosition;
    }

    @Override
    public boolean hasStableIds() {
//        https://stackoverflow.com/questions/24385416/hasstableids-in-expandable-listview/24386460
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        final GroupViewHolder viewHolder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_group, parent, false);
            viewHolder = new GroupViewHolder();

            viewHolder.cname = (TextView) convertView.findViewById(R.id.group_name);
            viewHolder.uuid = (TextView) convertView.findViewById(R.id.group_uuid);
            viewHolder.imageView = (ImageView) convertView.findViewById(R.id.group_img);
            viewHolder.type = (TextView)convertView.findViewById(R.id.group_type);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (GroupViewHolder) convertView.getTag();
        }


        final BluetoothGattService service = getGroup(groupPosition);
        final int uuid = getUUID16(service.getUuid());

        viewHolder.uuid.setText(Html.fromHtml(String.format("<font color=\"gray\">UUID:</font> <font color=\"#6200EE\">0x%04X</font>", uuid)));
        viewHolder.cname.setText(getServices(uuid));
        viewHolder.type.setText(service.getType() == BluetoothGattService.SERVICE_TYPE_PRIMARY ?  "PRIMARY SERVICE" : "SECONDARY SERVICE");

        return convertView;
    }

    private String readCharacteristicValue(int cname, final byte[] characteristic) {
        String value = "";
        switch (cname) {
            case DEVICE_NAME:
                value = new String(characteristic);
                break;
            case APPEARANCE:
                value = getAppearance(getUUID16(characteristic));
                break;
            case PNP_ID:
                int pos = 0;
                byte type = characteristic[0];
                int vendor = getUUID16(Arrays.copyOfRange(characteristic, 1, 3));
                if (type == 1) {
                    String.format("Bluetooth SIG assigned Vendor ID: %d (0x%04X)", vendor, vendor);
                } else {
                    String company = database.UsbVendorIdsDao().getCompanyById(vendor).company;
                    value = String.format("USB Implementer's Forum Vendor ID: 0x%04X<br> %s", vendor, company);
                }
                break;
            default:
                value = getHexData(characteristic).toUpperCase();
        }
        return  getKeyValFormat("Value",value);
    }

    private UUID getDiscriptorCCC(final BluetoothGattCharacteristic characteristic) {
        int duuid = 0x2902;
        for (BluetoothGattDescriptor bluetoothGattDescriptor : characteristic.getDescriptors()) {
            duuid = (int) (bluetoothGattDescriptor.getUuid().getMostSignificantBits() >> 32);
            if (duuid == 0x2902) {
                break;
            }
        }
        return getFrom16bitUUID((short) duuid);
    }

    private String getDiscriptors(final  BluetoothGattCharacteristic characteristic,boolean enabled){
        String descriptors = "";
        for (BluetoothGattDescriptor bluetoothGattDescriptor : characteristic.getDescriptors()) {
            final int duuid = (int) (bluetoothGattDescriptor.getUuid().getMostSignificantBits() >> 32);
            descriptors += getDescriptor(duuid) + "<br>";
            descriptors +=getKeyValFormat("UUID", duuid) + "<br>";
            if(duuid == CLIENT_CHARACTERISTIC_CONFIGURATION)
            {
                descriptors += getKeyValFormat("Value",String.format("Notifications %s <br>", enabled ? "enabled" : "disabled"));
            }
        }
        return descriptors;
    }

    private String getKeyValFormat(String key,String val){
        return String.format("<font color=\"gray\">%s:</font> <font color=\"black\"> %s </font>",key,val);
    }

    private String getKeyValFormat(String key,int val){
        return String.format("<font color=\"gray\">%s:</font> <font color=\"black\"> 0x%04X</font>",key,val);
    }

    private  void showInputDataDialog(View view,final BluetoothGattCharacteristic characteristic)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());

        final  View dialogLayout = inflater.inflate(R.layout.data_input_dialog,null);
        TextView addvalue = (TextView)dialogLayout.findViewById(R.id.data_addvalue);

        ListView listView = (ListView)dialogLayout.findViewById(R.id.data_input_list);
        List<String> listdata = new ArrayList<>();

        final WriteDataAdapter writeDataAdapter = new WriteDataAdapter(mContext,R.id.data_input_list,listdata);

        addvalue.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if(MotionEvent.ACTION_DOWN == event.getAction())
                {
                    listdata.add("");
                    writeDataAdapter.notifyDataSetChanged();
                    Log.i(TAG,"added new item !!!!!!!!!!! size : " + listdata.size());
                }
                return true;
            }
        });

        listView.setAdapter(writeDataAdapter);
        builder.setView(dialogLayout).setNegativeButton("Cancel",null).setPositiveButton("Send",null);
        AlertDialog dialog = builder.create();
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.show();
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        final ChildViewHolder holder;
        final BluetoothGattCharacteristic characteristic = getChild(groupPosition, childPosition);
        final int uuid16 = getUUID16(characteristic.getUuid());
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_characteristic, parent, false);
            holder = new ChildViewHolder();
            convertView.setTag(holder);
        } else {
            holder = (ChildViewHolder) convertView.getTag();
        }

        holder.cname = (TextView) convertView.findViewById(R.id.c_name);
        holder.uuid = (TextView) convertView.findViewById(R.id.c_uuid);
        holder.imageView = (ImageView) convertView.findViewById(R.id.c_image);
        holder.attr = (TextView) convertView.findViewById(R.id.c_attr);
        holder.value = (TextView) convertView.findViewById(R.id.c_value);
        holder.descriptor = (TextView) convertView.findViewById(R.id.c_descriptor);
        holder.read = (ImageView) convertView.findViewById(R.id.opt_read);
        holder.write = (ImageView) convertView.findViewById(R.id.opt_write);
        holder.notification = (ImageView) convertView.findViewById(R.id.opt_notify);
        String attr = "";

        // add characteristic properties icons.
        final int properties = characteristic.getProperties();

        boolean hasRead = 0 != (properties & BluetoothGattCharacteristic.PROPERTY_READ);
        boolean hasWrite = 0 != (properties & BluetoothGattCharacteristic.PROPERTY_WRITE) ||
                0 != (properties & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE);
        boolean hasNotify = 0 != (properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY);
        holder.value.setVisibility(!hasNotify && !hasRead ? View.GONE : View.VISIBLE);

        holder.read.setVisibility(hasRead ? View.VISIBLE : View.GONE);
        holder.write.setVisibility(hasWrite ? View.VISIBLE : View.GONE);
        holder.notification.setVisibility( hasNotify ? View.VISIBLE : View.GONE);

        // https://stackoverflow.com/questions/3216294/programmatically-add-id-to-r-id
        holder.notification.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    Drawable notifyicon = mContext.getDrawable(R.drawable.dev_notification_im);

                    boolean isEnabled  = notifyicon.getConstantState().equals(v.getBackground().getConstantState());
                    Log.i(TAG,"toggle notification " + isEnabled);
                    mGattManager.queue(new GattSetNotificationOperation(
                            mDevice,
                            serviceArrayList.get(groupPosition).getUuid(),
                            characteristic.getUuid(),
                            getFrom16bitUUID((short) CLIENT_CHARACTERISTIC_CONFIGURATION),
                            isEnabled
                    ));
                    ((AppCompatActivity) mContext).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            holder.value.setVisibility(isEnabled ? View.VISIBLE : View.GONE);
                            holder.notification.setBackgroundResource(isEnabled ? R.drawable.dev_notification :
                                    R.drawable.dev_notification_im);
                            String descriptors = getDiscriptors(characteristic,isEnabled);
                            holder.descriptor.setText(Html.fromHtml("<b color=\"black\">Discriptors:</b><br>" + descriptors));
                        }
                    });
                }
                return true;
            }
        });
        mGattManager.addCharacteristicChangeListener(characteristic.getUuid(),
                new CharacteristicChangeListener() {
                    @Override
                    public void onCharacteristicChanged(String deviceAddress, BluetoothGattCharacteristic characteristic) {
                        ((AppCompatActivity) mContext).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                holder.value.setText(Html.fromHtml(readCharacteristicValue(uuid16, characteristic.getValue())));
                            }
                        });
                    }
                });

        holder.read.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    Log.i(TAG, "readImg touch event !!!!!!!!!!! ");

                    mGattManager.queue(new GattCharacteristicReadOperation(
                            mDevice,
                            serviceArrayList.get(groupPosition).getUuid(),
                            characteristic.getUuid(),
                            new GattCharacteristicReadCallback() {
                                @Override
                                public void call(final byte[] characteristic) {
                                    Log.i(TAG, "read characteristic hexdump: " + getHexData(characteristic));
                                    ((AppCompatActivity) mContext).runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            holder.value.setText(Html.fromHtml(readCharacteristicValue(uuid16, characteristic)));
                                        }
                                    });
                                }
                            })
                    );

                }
                return true;
            }
        });

        holder.write.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                   showInputDataDialog(v,characteristic);
                }
                return true;
            }
        });

        if (hasRead) {
            attr = attr.isEmpty() ? "Read" : "Read, " + attr;
        }

        // added write icon


        if (hasWrite) {
            // https://stackoverflow.com/questions/3216294/programmatically-add-id-to-r-id
            if (0 != (properties & BluetoothGattCharacteristic.PROPERTY_WRITE)) {
                attr = attr.isEmpty() ? "Write" : "Write, " + attr;
            }
            if (0 != (properties & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) {
                attr = attr.isEmpty() ? "Write_no_repsponse" : "Write_no_repsponse, " + attr;
            }
        }
        if (hasNotify) {
            attr = "Notify";
        }

        holder.attr.setText(Html.fromHtml(getKeyValFormat("Properties", attr.toUpperCase())));

        String dname = "";
        try {
            dname = database.GattCharacteristicDao().getCharacteristicById(uuid16).descriptor;
        } catch (NullPointerException e) {
            dname = "Unknown Characteristic";
        }
        holder.cname.setText(Html.fromHtml(String.format("<b color=\"black\"> %s </b>", dname)));

        holder.uuid.setText(Html.fromHtml(getKeyValFormat("UUID", uuid16)));

        String descriptors = getDiscriptors(characteristic,false);
        holder.descriptor.setVisibility(descriptors.isEmpty() ? View.GONE :View.VISIBLE);
        holder.descriptor.setText(Html.fromHtml("<b color=\"black\">Discriptors:</b><br>" + descriptors));

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;
    }

    private static class GroupViewHolder {
        public TextView cname;
        public TextView uuid;
        public ImageView imageView;
        public TextView type;
//        public View view;
    }

    private static class ChildViewHolder {
        public BluetoothGattService service;
        public TextView cname;
        public TextView uuid;
        public TextView attr;
        public ImageView imageView;
        public TextView value;
        public TextView descriptor;
        public ImageView read;
        public ImageView write;
        public ImageView notification;

    }
}
