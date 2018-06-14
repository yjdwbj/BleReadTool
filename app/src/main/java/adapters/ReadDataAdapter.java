package adapters;


import android.content.Context;


import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import bt.lcy.btread.R;

public class ReadDataAdapter extends ArrayAdapter<String> {

    static private List<String> readList = new ArrayList<>();
    private boolean isHexMode = false;

    private final LayoutInflater inflater;
    public  ReadDataAdapter(Context context)
    {

        super(context,R.layout.list_single_item,readList);
        inflater = LayoutInflater.from(context);

    }

    public void setHexMode(final  boolean mode)
    {
        isHexMode = mode;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return super.getCount();
    }


    @Override
    public String getItem(int position) {
        return super.getItem(position);
    }

    @Override
    public void add( String object) {
        super.add(object);
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
    }

    private String getHexString(final byte[] arr) {
        String sTemp;
        StringBuffer sb = new StringBuffer(arr.length);
        for (int i = 0; i < arr.length; i++) {
            sTemp = Integer.toHexString(0xFF & arr[i]);
            if (sTemp.length() < 2)
                sb.append(0);
            sb.append(sTemp.toUpperCase());
            sb.append(" ");

        }
        return sb.toString();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder viewHolder;
        if(convertView == null) {

            convertView = inflater.inflate(R.layout.list_single_item, null);
            viewHolder = new ViewHolder();
            viewHolder.data = (TextView)convertView.findViewById(R.id.read_context);
            convertView.setTag(viewHolder);
        }else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        if(isHexMode)
        {

            viewHolder.data.setText(getHexString(getItem(position).getBytes()));
        }else{
            viewHolder.data.setText(getItem(position));
        }


        return convertView;
    }

    private static  class ViewHolder {
        public TextView data;
    }
}
