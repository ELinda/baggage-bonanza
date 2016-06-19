package com.example.linda.funhouse;

import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Map;

public class MoveActivatedTextView implements Runnable {
    TextView activated;
    MainActivity ma;
    public MoveActivatedTextView(MainActivity ma, TextView activated){
        this.ma = ma;
        this.activated = activated;
    }
    public void run() {
        float maxDistance = ma.BUTTON_HEIGHT/2 + ma.BUTTON_MARGIN;
        float lowestQuestionY = ma.rightSideQs.size() >= 1 ?
                                ma.rightSideQs.get(ma.rightSideQs.size()-1).getY() : 0;
        activated.setBackgroundColor(ma.ACTIVATED_A_COLOR);
        // If the activated answer is too low in the screen to match with anything,
        // keep shifting it upwards until it's as high as the lowest box on the right.
        // Thereafter, proceed as normal.
        if (activated.getY() > lowestQuestionY + maxDistance) {
            activated.setY(activated.getY() - 2 * ma.SCROLL_LEFT_BUTTON_RATE);
            ma.mHandler.postDelayed(this, ma.SCROLL_LEFT_BUTTON_TASK_INTERVAL);
            return;
        }else{
            activated.setX(activated.getX() + 5 * ma.SCROLL_LEFT_BUTTON_RATE);
        }
        // If at very top of the screen, then go to the very bottom
        if (activated.getY() < -0.5 * ma.BUTTON_HEIGHT) {
            activated.setY(ma.getScreenDims().y - ma.BUTTON_HEIGHT);
        }

        // Keep going, or stop, if far enough to the right
        if (activated.getX() < ma.BUTTON_WIDTH) {
            ma.mHandler.postDelayed(this, ma.SCROLL_LEFT_BUTTON_TASK_INTERVAL);
        } else {
            matchQAndA(activated, maxDistance);
        }
    }//upwardsMovingAs contains a <==> a is still moving up
    public void matchQAndA(TextView candidateA, float maxDistance){
        ma.upwardsMovingAs.remove(activated);
        for (TextView q : ma.rightSideQs){
            if (isClosestChoice(activated, q, maxDistance)){
                    /*System.out.println(String.format("Matched %s with %s",
                            activated.getText(), q.getText()));*/
                // if the Q is already matched with an A, return old A to the left column
                if (ma.QsTakenByA.containsKey(q)){
                    ma.placeAReturningInBelt(ma.QsTakenByA.get(q));
                }
                ma.QsTakenByA.put(q, activated);
                if (ma.AQ.get(activated.getText()).equals(q.getText())){
                    ma.AStates.put(candidateA, MainActivity.AnswerState.MATCHED_CORRECT);
                    q.setBackgroundColor(ma.RIGHT_MATCH_COLOR);
                }else{
                    ma.AStates.put(candidateA, MainActivity.AnswerState.MATCHED_WRONG);
                    q.setBackgroundColor(ma.WRONG_MATCH_COLOR);
                }
                break;
            }
        }
        // if all answers are taken but some are wrongly assigned, reset all the wrong ones
        // otherwise, win
        if (ma.upwardsMovingAs.size() == 0){
            winOrResetAllWrongAnswers();
        }
    }
    public void winOrResetAllWrongAnswers(){
        // questions wrongly answered
        ArrayList<TextView> wrongQs = new ArrayList<>();
        ArrayList<TextView> wrongAs = new ArrayList<>();
        for(Map.Entry<TextView, TextView> qa : ma.QsTakenByA.entrySet()){
            TextView a = qa.getValue();
            //System.out.println(String.format("Q->A is %s, %s", q.getText(), a.getText()));
            if (ma.AStates.get(a).equals(MainActivity.AnswerState.MATCHED_WRONG)){
                wrongQs.add(qa.getKey());
                wrongAs.add(a);
            }
        }
        ma.multiPlaceAReturningInBelt(wrongAs);
        // if no questions wrongly answered, you have won! Otherwise, finish the reset
        if (wrongQs.size() == 0){
            TextView hello = (TextView) ma.findViewById(R.id.hello);
            hello.setWidth(ma.getScreenDims().x/3);
            hello.setTextSize(20);
            hello.setTextColor(ma.WAITING_Q_COLOR);
            ma.watch.stop();
            hello.setText(String.format("You have won in %s seconds!!!!11",
                    ma.watch.getTimeElapsedSeconds()));
        }else{
            for (TextView q : wrongQs){
                ma.QsTakenByA.remove(q);
            }
        }
    }
    public boolean isClosestChoice(View v1, View v2, float maxDistance){
        float y_1 = v1.getY();
        float y_2 = v2.getY();
        return y_1 >= y_2 - maxDistance && y_1 <= y_2 + maxDistance;
    }
}
