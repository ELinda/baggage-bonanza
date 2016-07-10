package com.example.linda.funhouse;

import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.widget.TextView;

import java.util.concurrent.TimeUnit;

/**
 * Created by linda on 6/8/16.
 */
public class StopWatch {
    private static final int UPDATE_INTERVAL = 1000;
    private long startTime;
    private TextView textView;

    public StopWatch(TextView textView) {
        this.textView = textView;
    }

    public int getTimeElapsedSeconds(){
        long elapsedTime = SystemClock.elapsedRealtime() - startTime;
        return (int) TimeUnit.MILLISECONDS.toSeconds(elapsedTime);
    }
    public void start() {
        startTime = SystemClock.elapsedRealtime();
        handler.sendMessage(handler.obtainMessage(MSG));
    }

    public void addTime(int milliseconds) {
        startTime -= milliseconds;
    }
    public void stop() {
        handler.removeMessages(MSG);
    }

    public void reset() {
        startTime = SystemClock.elapsedRealtime();
    }
    private Handler mHandler;

    private static final int MSG = 1;

    public static String getTimeString(Long elapsedTime){
        int hours = (int)TimeUnit.MILLISECONDS.toHours(elapsedTime);
        int mins =  (int)(TimeUnit.MILLISECONDS.toMinutes(elapsedTime)
                - TimeUnit.HOURS.toMinutes(hours));
        int secs = (int)(TimeUnit.MILLISECONDS.toSeconds(elapsedTime)
                - TimeUnit.HOURS.toSeconds(hours)
                - TimeUnit.MINUTES.toSeconds(mins));
        String hour_part = hours > 0 ? String.format("%02d:", hours) : "";
        String minute_part = mins > 0 ? String.format("%02d:", mins) : "";
        String second_part = String.format("%02d", secs);
        return hour_part + minute_part + second_part;
    }
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            long elapsedTime = SystemClock.elapsedRealtime() - startTime;
            textView.setText(getTimeString(elapsedTime));
            sendMessageDelayed(obtainMessage(MSG), UPDATE_INTERVAL);
        }
    };
}
