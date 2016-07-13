package com.example.linda.funhouse;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.text.InputType;
import android.widget.EditText;

import java.util.Map;


public class ShowNameInputDialogue implements Runnable{
    MainActivity ma;
    public final String HIGH_SCORES_PREFS_PREFIX = "HighScorePrefs";
    public final String HIGH_SCORES_NOT_FULL = "HIGH_SCORES_NOT_FULL";
    public final int NUM_WINNERS_PER_FILE = 5;
    public ShowNameInputDialogue(MainActivity ma){
        this.ma = ma;
    }

    public String getHighScoresPrefsName(){
        return HIGH_SCORES_PREFS_PREFIX + "_" + this.ma.dataFileName;
    }
    public void storeHighScore(String name, long score, String kickOutName){
        SharedPreferences prefs = this.ma.getSharedPreferences(getHighScoresPrefsName(), 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(name, score);
        editor.remove(kickOutName);
        editor.commit();
    }
    public String getNameToKickOutOfTop(long score){
        SharedPreferences prefs = this.ma.getSharedPreferences(getHighScoresPrefsName(), 0);
        Map<String, ?> topWinners = prefs.getAll();
        if (topWinners.size() < NUM_WINNERS_PER_FILE){
            return HIGH_SCORES_NOT_FULL;
        }
        boolean isFastEnough = false;
        long slowestTime = Long.MIN_VALUE;
        String slowestName = null;
        for(Map.Entry<String, ?> entry : topWinners.entrySet()){
            if (entry.getValue().getClass().isAssignableFrom(long.class)){
                isFastEnough |= score < (long)entry.getValue();
                if (slowestTime < (long)entry.getValue()){
                    slowestTime = (long)entry.getValue();
                    slowestName = entry.getKey();
                }
            }
        }
        return isFastEnough ? slowestName : null;
    }
    public void run(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this.ma);
        final long score = ma.watch.getTimeElapsedSeconds();
        final String kickOutName = getNameToKickOutOfTop(score);
        if (kickOutName == null){
            return;
        }
        String wonMessage = String.format("You're in the %s fastest for %s. Enter a name.",
                                          NUM_WINNERS_PER_FILE, ma.dataFileName);
        builder.setTitle(wonMessage);

        final EditText input = new EditText(this.ma);

        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                System.out.println(input.getText().toString());
                storeHighScore(input.getText().toString(), score, kickOutName);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();

    }
}
