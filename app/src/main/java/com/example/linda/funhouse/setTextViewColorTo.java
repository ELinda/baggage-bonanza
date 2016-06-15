package com.example.linda.funhouse;

import android.util.Log;
import android.widget.TextView;


class SetTextViewColorTo implements Runnable{
    private static final String LOGTAG = "F_PATH";
    private TextView textView;
    private int color;
    public SetTextViewColorTo(TextView textView, int color){
        this.textView = textView;
        this.color = color;
    }
    @Override
    public void run() {
        Log.d(LOGTAG, String.format("resetting color of %s to %s", this.textView.getText(), this.color));
        this.textView.setBackgroundColor(this.color);
    }
}
