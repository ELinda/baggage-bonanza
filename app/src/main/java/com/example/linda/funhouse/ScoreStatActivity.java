package com.example.linda.funhouse;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

public class ScoreStatActivity extends Activity{
    public static final String PREFS_NAME = "FileBrowserPrefs";
    public static final int TITLE_TEXT_COLOR = Color.parseColor("#FFFFFF");
    public static final int TITLE_COLOR = Color.parseColor("#2277AA");

    SharedPreferences settings = null;

    /***
     * @return View for title of high scores
     */
    private TextView getTitleView(){
        TextView listTitle = new TextView(this);
        listTitle.setTextSize(45);
        listTitle.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        listTitle.setBackgroundColor(this.TITLE_COLOR);
        listTitle.setTextColor(this.TITLE_TEXT_COLOR);
        listTitle.setText("Fastest Times");
        return listTitle;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = getSharedPreferences(PREFS_NAME, 0);
        setContentView( R.layout.score_list );
        ListView listView=(ListView)findViewById(R.id.scoreListView);
        ArrayList<ScoreEntry> data = new ArrayList<ScoreEntry>();
        ScoreEntry se = new ScoreEntry("name1", 99L);
        data.add(se);
        ScoreStatListAdapter adapter = new ScoreStatListAdapter(this, data);
        listView.setAdapter(adapter);
        listView.refreshDrawableState();

        listView.addHeaderView(getTitleView());
    }
}
