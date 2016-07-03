package com.example.linda.funhouse;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;


public class ScoreStatListAdapter extends ArrayAdapter<ScoreEntry> {

    public ArrayList<HashMap<String, String>> list;
    Activity activity;
    TextView fileTextView, timeTextView;
    public ScoreStatListAdapter(Context context, ArrayList<ScoreEntry> users){
        super(context, 0, users);
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        ScoreEntry scoreEntry = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.score_columns, parent, false);
        }
        // Lookup view for data population
        TextView textViewFile = (TextView) convertView.findViewById(R.id.file);
        TextView textViewTime = (TextView) convertView.findViewById(R.id.time);
        // Populate the data into the template view using the data object
        textViewFile.setText(scoreEntry.file);
        textViewTime.setText(scoreEntry.time);
        // Return the completed view to render on screen
        return convertView;
    }


}