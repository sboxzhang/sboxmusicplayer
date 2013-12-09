package com.sbox.musicserviceplayer;

import java.util.ArrayList;
import java.util.HashMap;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class PlayerListAdapter extends BaseAdapter
{
    private ArrayList<HashMap<String, Object>> musicList;
    private Context mContext;
    private LayoutInflater inflater = null;

    PlayerListAdapter(Context context, ArrayList<HashMap<String, Object>> c)
    {
        this.musicList = c;
        this.mContext = context;
        inflater = LayoutInflater.from(mContext);

    }

    @Override
    public int getCount()
    {
        if (musicList != null)
        {
            return musicList.size();
        }
        return 0;
    }

    @Override
    public Object getItem(int arg0)
    {
        return musicList.get(arg0);
    }

    @Override
    public long getItemId(int position)
    {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        ViewHolder holder = null;
        if (convertView == null)
        {
            convertView = inflater.inflate(R.layout.listview, null);
            holder = new ViewHolder();
            holder.title = (TextView) convertView
                    .findViewById(R.id.listview_music_title);
            holder.artist = (TextView) convertView
                    .findViewById(R.id.listview_artist);
            holder.date = (TextView) convertView
                    .findViewById(R.id.listview_date);
            holder.size = (TextView) convertView
                    .findViewById(R.id.listview_music_size);
            convertView.setTag(holder);
        }
        else
        {
            holder = (ViewHolder) convertView.getTag();
        }
        if (position >= 0 && position < getCount())
        {
            holder.date.setText((String) musicList.get(position).get(
                    UserHelper.Duration));

            holder.title.setText((String) musicList.get(position).get(
                    UserHelper.Title));
            holder.size.setText((String) musicList.get(position).get(
                    UserHelper.Size));
            holder.artist.setText((String) musicList.get(position).get(
                    UserHelper.Artist)
                    + ",");

        }
        return convertView;
    }

    public class ViewHolder
    {
        private TextView title;

        private TextView artist;

        private TextView date;

        private TextView size;
    }

}
