package com.example.linda.funhouse;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    public static final int ACTIVATED_A_COLOR = Color.parseColor("#4499CC");
    public static final int WAITING_Q_COLOR = Color.parseColor("#ff2277");
    public static final int CANDIDATE_A_COLOR = Color.parseColor("#2277AA");
    public static final int RIGHT_MATCH_COLOR = Color.parseColor("#00AA44");
    public static final int WRONG_MATCH_COLOR = Color.parseColor("#AA4400");
    public static final int TEXT_COLOR = Color.parseColor("#FFFFFF");
    private static final int BUTTON_MARGIN = 10;
    private static final int ADD_RIGHT_BUTTON_TASK_INTERVAL = 6000;
    private static final int SCROLL_LEFT_BUTTON_TASK_INTERVAL = 125;
    private static final int SCROLL_LEFT_BUTTON_RATE = 8;
    private static final int FILE_CHOOSER_REQUEST_CODE = 100;
    Intent fileIntent = null;
    private StopWatch watch = null;
    private HashMap<String, String> AQ = new HashMap<String, String>();
    private HashMap<String, String> QA = new HashMap<String, String>();
    private ArrayList<String> Q = new ArrayList<String>();
    private ArrayList<String> A = new ArrayList<String>();
    private int maxId = 10;
    private static final int BUTTON_HEIGHT = 150;
    private int BUTTON_WIDTH;
    private HashMap<TextView, TextView> QsTakenByA = new HashMap<>();
    private HashMap<TextView, AnswerState> AStates = new HashMap<>();
    ArrayList<TextView> rightSideQs = new ArrayList<>();
    ArrayList<TextView> upwardsMovingAs = new ArrayList<>();
    ArrayList<String> AsWithoutQs = new ArrayList<String>();
    private int maxQs = 0;
    private Handler mHandler;
    public enum AnswerState {
        CANDIDATE, SELECTED, MATCHED_CORRECT, MATCHED_WRONG
    }
    private void clearState(){
        RelativeLayout layout = (RelativeLayout) findViewById(R.id.main);
        layout.removeViews(1, upwardsMovingAs.size() + rightSideQs.size());
        QsTakenByA.clear();
        upwardsMovingAs.clear();
        AStates.clear();
        rightSideQs.clear();
        AsWithoutQs.clear();
        watch.reset();

        mHandler.removeCallbacks(scrollLeftButtonTask);
        mHandler.removeCallbacks(addRightButtonTask);
    }
    public class MoveActivatedTextView implements Runnable {
        TextView activated;
        public MoveActivatedTextView(TextView activated){
            this.activated = activated;
        }
        public void run() {
            float maxDistance = BUTTON_HEIGHT/2 + BUTTON_MARGIN;
            float lowestQuestionY = rightSideQs.get(rightSideQs.size()-1).getY();
            activated.setBackgroundColor(ACTIVATED_A_COLOR);
            // If the activated answer is too low in the screen to match with anything,
            // keep shifting it upwards until it's as high as the lowest box on the right.
            // Thereafter, proceed as normal.
            if (activated.getY() > lowestQuestionY + maxDistance) {
                activated.setY(activated.getY() - 2 * SCROLL_LEFT_BUTTON_RATE);
                mHandler.postDelayed(this, SCROLL_LEFT_BUTTON_TASK_INTERVAL);
                return;
            }else{
                activated.setX(activated.getX() + 5 * SCROLL_LEFT_BUTTON_RATE);
            }
            // If at very top of the screen, then go to the very bottom
            if (activated.getY() < -0.5 * BUTTON_HEIGHT) {
                activated.setY(getScreenDims().y - BUTTON_HEIGHT);
            }

            // Keep going, or stop, if far enough to the right
            if (activated.getX() < BUTTON_WIDTH) {
                mHandler.postDelayed(this, SCROLL_LEFT_BUTTON_TASK_INTERVAL);
            } else {
                matchQAndA(activated, maxDistance);
            }
        }//upwardsMovingAs contains a <==> a is still moving up
        public void matchQAndA(TextView candidateA, float maxDistance){
            upwardsMovingAs.remove(activated);
            for (TextView q : rightSideQs){
                if (isClosestChoice(activated, q, maxDistance)){
                    /*System.out.println(String.format("Matched %s with %s",
                            activated.getText(), q.getText()));*/
                    // if the Q is already matched with an A, return old A to the left column
                    if (QsTakenByA.containsKey(q)){
                        placeAReturningInBelt(QsTakenByA.get(q));
                    }
                    QsTakenByA.put(q, activated);
                    if (AQ.get(activated.getText()).equals(q.getText())){
                        AStates.put(candidateA, AnswerState.MATCHED_CORRECT);
                        q.setBackgroundColor(RIGHT_MATCH_COLOR);
                    }else{
                        AStates.put(candidateA, AnswerState.MATCHED_WRONG);
                        q.setBackgroundColor(WRONG_MATCH_COLOR);
                    }
                    break;
                }
            }
            // if all answers are taken but some are wrongly assigned, reset all the wrong ones
            // otherwise, win
            if (upwardsMovingAs.size() == 0){
                winOrResetAllWrongAnswers();
            }
        }
        public void winOrResetAllWrongAnswers(){
            // questions wrongly answered
            ArrayList<TextView> wrongQs = new ArrayList<>();
            ArrayList<TextView> wrongAs = new ArrayList<>();
            for(Map.Entry<TextView, TextView> qa : QsTakenByA.entrySet()){
                TextView a = qa.getValue();
                //System.out.println(String.format("Q->A is %s, %s", q.getText(), a.getText()));
                if (AStates.get(a).equals(AnswerState.MATCHED_WRONG)){
                    wrongQs.add(qa.getKey());
                    wrongAs.add(a);
                }
            }
            multiPlaceAReturningInBelt(wrongAs);
            // if no questions wrongly answered, you have won! Otherwise, finish the reset
            if (wrongQs.size() == 0){
                TextView hello = (TextView) findViewById(R.id.hello);
                hello.setWidth(getScreenDims().x/3);
                hello.setTextSize(20);
                hello.setTextColor(WAITING_Q_COLOR);
                watch.stop();
                hello.setText(String.format("You have won in %s seconds!!!!11",
                                            watch.getTimeElapsedSeconds()));
            }else{
                for (TextView q : wrongQs){
                    QsTakenByA.remove(q);
                }
            }
        }
        public boolean isClosestChoice(View v1, View v2, float maxDistance){
            float y_1 = v1.getY();
            float y_2 = v2.getY();
            return y_1 >= y_2 - maxDistance && y_1 <= y_2 + maxDistance;
        }
    }
    Runnable addRightButtonTask = new Runnable() {
        @Override
        public void run() {
            if (rightSideQs.size() >= estimateNumberOfSpots() || upwardsMovingAs.size() == 0)
                return;
            TextView textView = (TextView)findViewById(R.id.hello);
            RelativeLayout layout = (RelativeLayout) findViewById(R.id.main);
            int belowID = rightSideQs.size() == 0 ? -1 : maxId;
            int newID = incrementAndReturnMaxId();
            String unusedRandomA = getRandomElementFrom(AsWithoutQs);
            AsWithoutQs.remove(unusedRandomA);
            String question = AQ.get(unusedRandomA);
            TextView box = addNewButtonRight(layout, BUTTON_WIDTH, newID, belowID, question);
            rightSideQs.add(box);
            mHandler.postDelayed(this, ADD_RIGHT_BUTTON_TASK_INTERVAL);
        }
    };
    Runnable flattenAllAs = new Runnable(){
        @Override
        public void run() {
            for (View a : upwardsMovingAs) {
                flattenLocation(a);
            }
        }
    };
    Runnable scrollLeftButtonTask = new Runnable() {
        @Override
        public void run() {
            if (upwardsMovingAs.size() == 0)
                return;
            TextView textView = (TextView)findViewById(R.id.hello);
            RelativeLayout layout = (RelativeLayout) findViewById(R.id.main);

            TextView highestA = (TextView)upwardsMovingAs.get(0);

            int[] xy = new int[2];
            highestA.getLocationOnScreen(xy);
            if (xy[1] < -1 * BUTTON_HEIGHT) {
                upwardsMovingAs.remove(0);
                placeAReturningInBelt(highestA);
            }else {
                for (View answerView : upwardsMovingAs) {
                    answerView.setY(answerView.getY() - SCROLL_LEFT_BUTTON_RATE);
                }
            }
            mHandler.postDelayed(this, SCROLL_LEFT_BUTTON_TASK_INTERVAL);
        }
    };
    private void multiPlaceAReturningInBelt(List<TextView> aList){
        upwardsMovingAs.addAll(aList);
        float offsetBelow = BUTTON_HEIGHT + 2 * BUTTON_MARGIN;
        float lowestY = upwardsMovingAs.size() > 0 ?
                            upwardsMovingAs.get(upwardsMovingAs.size() - 1).getY() : 0;
        float firstOffset = Math.max(lowestY + offsetBelow, getScreenDims().y - offsetBelow);
        for(int i=0; i < aList.size(); ++i){
            aList.get(i).setY(firstOffset + i * offsetBelow);
            aList.get(i).setX(0);
            aList.get(i).setBackgroundColor(CANDIDATE_A_COLOR);
            AStates.put(aList.get(i), AnswerState.CANDIDATE);
        }
    }
    private void placeAReturningInBelt(TextView a){
        List<TextView> aList = new ArrayList<>();
        aList.add(a);
        multiPlaceAReturningInBelt(aList);
    }
    private boolean hasNoSpaceForNextQ(){
        if (rightSideQs == null || rightSideQs.size() == 0) return false;
        int[] xy = new int[2];
        rightSideQs.get(rightSideQs.size()-1).getLocationInWindow(xy);
        int two_button_space = 2 * (MainActivity.BUTTON_HEIGHT + 4 * MainActivity.BUTTON_MARGIN);
        return xy[1] + two_button_space > getScreenDims().y;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHandler = new Handler(Looper.getMainLooper());
        start();
    }
    private void restart(){
        System.out.println(String.format("restarting with maxid=%s", maxId));

        clearState();
        start();
        System.out.println(String.format("restarted and maxid=%s", maxId));
    }

    private void setupBottomButtons(){
        Button restartButton = (Button)findViewById(R.id.restart);
        System.out.println(String.format("Rbutton %s", restartButton.getText()));
        restartButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                System.out.println("getting text:" + ((Button)(v)).getText());
                restart();
            }
        });
        Button chooseButton = (Button)findViewById(R.id.choose);
        chooseButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                startActivityForResult(fileIntent, FILE_CHOOSER_REQUEST_CODE);
            }
        });

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_CHOOSER_REQUEST_CODE) {
            if(resultCode == RESULT_OK) {
                String fileName = data.getStringExtra(FileBrowserActivity.returnFileParameter);
                Toast.makeText(
                        this,
                        "Received FILE path from file browser:\n"+fileName,
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(
                        this,
                        "Received NO result from file browser",
                        Toast.LENGTH_LONG).show();
            }
        }
    }
    private void start(){
        if (isExternalStorageWritable()) {

        }
        TextView textView = (TextView)findViewById(R.id.hello);


        this.BUTTON_WIDTH = getScreenDims().x/3;
        populateQandA();
        Point screenDims = getScreenDims();

        this.maxQs = estimateNumberOfSpots();
        fillAnswers((RelativeLayout) findViewById(R.id.main));

        // add the first right button pretty soon, but have others add gradually
        mHandler.postDelayed(addRightButtonTask, 150);
        mHandler.postDelayed(scrollLeftButtonTask, SCROLL_LEFT_BUTTON_TASK_INTERVAL);
        mHandler.postDelayed(flattenAllAs, 100);
        watch = new StopWatch((TextView)findViewById(R.id.timer));
        watch.start();
        setupBottomButtons();

        if (fileIntent == null) {
            fileIntent = new Intent(FileBrowserActivity.INTENT_ACTION_SELECT_FILE,
                                    null,
                                    this,
                                    FileBrowserActivity.class);
            ArrayList<String> extensions = new ArrayList<String>();
            extensions.add(".pdf");
            extensions.add(".txt");
            fileIntent.putStringArrayListExtra("filterFileExtension", extensions);
        }
    }
    @Override
    protected void onStart() {
        super.onStart();
    }
    @Override
    public void onStop() {
        super.onStop();
        mHandler.removeCallbacks(addRightButtonTask);
    }
    @Override
    protected void onResume() {
        super.onResume();
    }

    public void addA(TextView answerView){
        upwardsMovingAs.add(answerView);
        AsWithoutQs.add((String)answerView.getText());
        AStates.put(answerView, AnswerState.CANDIDATE);
    }
    public void fillAnswers(ViewGroup layout){
        for (int i=0; i < maxQs; ++i) {
            int belowID = maxId;
            int newID = incrementAndReturnMaxId();
            String answer = getRandomElementFrom(A);
            int width = this.BUTTON_WIDTH;
            TextView box = addAndReturnButton(layout, width, newID, belowID, answer, false);

            addA(box);
        }

    }
    public int estimateNumberOfSpots(){
        int buttonSpace = MainActivity.BUTTON_HEIGHT + 2 * MainActivity.BUTTON_MARGIN;
        return (int)((getScreenDims().y - 300)/ buttonSpace);
    }
    public int incrementAndReturnMaxId(){
        maxId += 1;
        return maxId;
    }
    public void populateQandA(){
        TextView textView = (TextView)findViewById(R.id.hello);
        BufferedReader br = null;
        try {
            br = new BufferedReader(
                    new InputStreamReader( getAssets().open("lesscommon.txt")));
            for(String line; (line = br.readLine()) != null; ){
                String[] qa = line.split(" ", 2);
                if (qa.length > 1) {
                    this.AQ.put(qa[1], qa[0]);
                    this.QA.put(qa[0], qa[1]);
                    Q.add(qa[0]);
                    A.add(qa[1]);
                }
            }
        }catch(IOException e){
            textView.setText(String.format("Failed to open file:", e.getMessage()));
        }
    }
    public Point getScreenDims(){
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
    }
    public int getHeight(View view){
        view.measure(RelativeLayout.LayoutParams.MATCH_PARENT,RelativeLayout.LayoutParams.MATCH_PARENT);
        return view.getMeasuredHeight();
    }
    public int getWidth(View view){
        view.measure(RelativeLayout.LayoutParams.MATCH_PARENT,RelativeLayout.LayoutParams.MATCH_PARENT);
        return view.getMeasuredWidth();
    }
    public String getRandomElementFrom(ArrayList<String> a){
        return a.get((int)(Math.random() * a.size()));
    }
    public RelativeLayout.LayoutParams getRelativeLayoutParams(int width, int belowId){
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                width, RelativeLayout.LayoutParams.WRAP_CONTENT);
        int margin = this.BUTTON_MARGIN;
        lp.setMargins(margin, margin, margin, margin);
        if (belowId > 0) {
            lp.addRule(RelativeLayout.BELOW, belowId);
        }
        return lp;
    }
    OnClickListener clickedQ = new OnClickListener() {
        public void onClick(View v){
            TextView QView = ((TextView)v);
            String answer = QA.get(QView.getText());
            RelativeLayout layout = (RelativeLayout) findViewById(R.id.main);
            for (int i=0; i < layout.getChildCount(); ++i){
            }
            /*mHandler.postDelayed(new ResetBackgroundColorTo((TextView)v),
                    WAITING_Q_COLOR);*/
        }
    };
    public TextView addAndReturnButton(ViewGroup vg, int width, int newId, int belowId,
                                       String text, boolean addToRight){
        final TextView button = new TextView(this);
        RelativeLayout.LayoutParams lp = getRelativeLayoutParams(width, belowId);
        if (addToRight){
            lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            button.setBackgroundColor(WAITING_Q_COLOR);
            button.setOnClickListener(clickedQ);
        } else {
            button.setBackgroundColor(CANDIDATE_A_COLOR);
            button.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    if (AStates.get(v).equals(AnswerState.CANDIDATE)){
                        AStates.put((TextView)v, AnswerState.SELECTED);
                        mHandler.postDelayed(new MoveActivatedTextView((TextView)v),
                                SCROLL_LEFT_BUTTON_RATE);
                    }
                }
            });
        }
        button.setText(text);
        button.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        button.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        button.setTextSize((int)(100/(3 + .5*text.length())));
        button.setTextColor(TEXT_COLOR);
        button.setId(newId);
        button.setPadding(0,0,0,0);
        final float scale = getResources().getDisplayMetrics().density;
        button.setHeight(this.BUTTON_HEIGHT);
        vg.addView(button, lp);
        return button;
    }
    public void flattenLocation(View v){
        int[] xy = new int[2];
        v.getLocationOnScreen(xy);
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams)v.getLayoutParams();
        lp.topMargin = xy[1];
        lp.removeRule(RelativeLayout.BELOW);
    }
    public TextView addNewButtonRight(ViewGroup vg, int width, int newId, int belowId, String text){
        return addAndReturnButton(vg, width, newId, belowId, text, true);
    }
    /* Checks if external storage is available for read and write */
    public void showToast(View v) {
        TextView textView = (TextView)findViewById(R.id.hello);
        v.setY(v.getY() - 10);
        textView.setText(String.format("%s,%s", "oh", v));
    }
    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

}
