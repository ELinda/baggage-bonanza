package com.example.linda.funhouse;

import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Created by linda on 6/15/16.
 */
public class ClickedQListener implements View.OnClickListener{
    private  MainActivity ma;
    public ClickedQListener(MainActivity ma){
        this.ma = ma;
    }
    private boolean validHintState(MainActivity.AnswerState aState){
        return aState.equals(MainActivity.AnswerState.CANDIDATE)
                || aState.equals(MainActivity.AnswerState.MATCHED_WRONG)
                || aState.equals(MainActivity.AnswerState.SELECTED);
    }
    public void onClick(View v){
        TextView QView = ((TextView)v);
        String answer = ma.QA.get(QView.getText());
        RelativeLayout layout = (RelativeLayout) ma.findViewById(R.id.main);
        for (int i=0; i < layout.getChildCount(); ++i){
            TextView candidate = (TextView)layout.getChildAt(i);
            if (candidate.getText().equals(answer) && validHintState(ma.AStates.get(candidate))){
                int oldColor = ((ColorDrawable) candidate.getBackground()).getColor();
                candidate.setBackgroundColor(ma.HINT_FLASH_A_COLOR);
                SetTextViewColorTo resetColor = new SetTextViewColorTo(candidate, oldColor);
                ma.mHandler.postDelayed(resetColor, ma.HINT_DURATION);
            }
        }
        // punish HINT_PUNISHMENT_SECONDS for a hint (convert to msecs)
        ma.watch.addTime(1000 * ma.HINT_PUNISHMENT_SECONDS);
    }

}
