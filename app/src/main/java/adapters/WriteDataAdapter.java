package adapters;

import android.bluetooth.BluetoothGattService;
import android.content.ClipData;
import android.content.Context;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import bt.lcy.btread.R;

/**
 * 项目名称: BleReadTool
 * 开发者: yjdwbj
 * 创建时间:  9:54 PM
 */
public class WriteDataAdapter extends ArrayAdapter<String> {
    private static final String TAG = BtServicesAdapter.class.getSimpleName();

    private final LayoutInflater inflater;
    private List<String> mListData;


    public WriteDataAdapter(@NonNull Context context, int resource, @NonNull List<String> objects) {
        super(context, resource);
        inflater = LayoutInflater.from(context);
        mListData = objects;
    }

    @Override
    public int getCount() {
        return mListData.size();
    }

    @Override
    public void add(@Nullable String object) {
        mListData.add(object);
    }

    @Override
    public void remove(@Nullable String object) {
        mListData.remove(object);
    }

    @Nullable
    @Override
    public String getItem(int position) {
        return mListData.get(position);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        ViewHolder viewHolder ;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.data_type_spinner, null);
            viewHolder = new ViewHolder();
            viewHolder.prefix = (TextView)convertView.findViewById(R.id.data_type_prefix);
            viewHolder.write = (EditText) convertView.findViewById(R.id.data_input);
            viewHolder.spinlist = (Spinner) convertView.findViewById(R.id.data_type_list);
            viewHolder.remove_item = (ImageView) convertView.findViewById(R.id.data_delete);
            convertView.setTag(viewHolder);
            viewHolder.spinlist.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){

                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String value = viewHolder.spinlist.getSelectedItem().toString();
                    Log.i(TAG,"onItemSelected " + " pos " + position + " id : " + id + " value " + value);
                    if(value == "TEXT"){
                        viewHolder.write.setInputType(InputType.TYPE_CLASS_TEXT);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });

            viewHolder.remove_item.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if(MotionEvent.ACTION_DOWN == event.getAction())
                    {
                        mListData.remove(position);
                        notifyDataSetChanged();
                    }
                    return true;
                }
            });
        }else
        {
            viewHolder = (ViewHolder)convertView.getTag();
        }

        if(getCount() < 1){
            viewHolder.remove_item.setVisibility(View.GONE);
        }
        return convertView;
    }



    private static class ViewHolder {
        public TextView prefix;
        public EditText write;
        public Spinner spinlist;
        public ImageView remove_item;
    }
}
