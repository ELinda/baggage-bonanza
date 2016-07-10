package com.example.linda.funhouse;

import android.app.Activity;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;


public class ScoreStatListAdapter extends BaseAdapter {

    public ArrayList<ScoreEntry> list;
    Activity activity;
    private LayoutInflater inflater;
    public static final int LIST_COLOR = Color.parseColor("#4499CC");
    public static final int ALT_LIST_COLOR = Color.parseColor("#55AADD");
    public ScoreStatListAdapter(Activity context, ArrayList<ScoreEntry> scores){
        super();
        this.activity=context;
        this.list=scores;
        inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public ScoreEntry getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ScoreEntry scoreEntry = getItem(position);
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.score_columns, parent, false);
        }
        TextView textViewFile = (TextView) convertView.findViewById(R.id.file);
        TextView textViewTime = (TextView) convertView.findViewById(R.id.time);
        textViewFile.setText(scoreEntry.file);
        textViewTime.setText(StopWatch.getTimeString(scoreEntry.time));
        return convertView;
    }


}