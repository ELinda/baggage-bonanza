package com.example.linda.funhouse;

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
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    public static final int ACTIVATED_A_COLOR = Color.parseColor("#4499CC");
    public static final int WAITING_Q_COLOR = Color.parseColor("#ff2277");
    public static final int CANDIDATE_A_COLOR = Color.parseColor("#2277AA");
    public static final int RIGHT_MATCH_COLOR = Color.parseColor("#00AA44");
    public static final int WRONG_MATCH_COLOR = Color.parseColor("#AA4400");
    public static final int TEXT_COLOR = Color.parseColor("#FFFFFF");
    private Handler mHandler;
    public class MoveActivatedTextView implements Runnable {
        TextView activated;
        public MoveActivatedTextView(TextView activated){
            this.activated = activated;
        }
        public void run(){
            activated.setBackgroundColor(ACTIVATED_A_COLOR);
            activated.setX(activated.getX() + 5 * SCROLL_LEFT_BUTTON_RATE);
            if (activated.getX() < BUTTON_WIDTH){
                mHandler.postDelayed(this, SCROLL_LEFT_BUTTON_TASK_INTERVAL);
            }else{
                visibleA.remove(activated);
                float maxDistance = BUTTON_HEIGHT/2 + BUTTON_MARGIN;
                for (TextView q : visibleQ){
                    float y_1 = activated.getY();
                    float y_2 = q.getY();
                    System.out.println(String.format("candidate %s with %s",
                            activated.getText(), q.getText()));
                    if (y_1 > y_2 - maxDistance && y_1 < y_2 + maxDistance){
                        System.out.println(String.format("And matched %s with %s",
                                           activated.getText(), q.getText()));
                        if (AQ.get(activated.getText()).equals(q.getText())){
                            q.setBackgroundColor(RIGHT_MATCH_COLOR);
                        }else{
                            q.setBackgroundColor(WRONG_MATCH_COLOR);
                        }
                        break;
                    }
                }
            }
        }
    }
    Runnable addRightButtonTask = new Runnable() {
        @Override
        public void run() {
            if (visibleQ.size() >= estimateNumberOfSpots())
                return;
            TextView textView = (TextView)findViewById(R.id.hello);
            RelativeLayout layout = (RelativeLayout) findViewById(R.id.main);
            int belowID = visibleQ.size() == 0 ? -1 : maxId;
            int newID = incrementAndReturnMaxId();
            String unusedRandomA = getRandomElementFrom(visibleAUnused);
            visibleAUnused.remove(unusedRandomA);
            String question = AQ.get(unusedRandomA);
            TextView box = addNewButtonRight(layout, BUTTON_WIDTH, newID, belowID, question);
            visibleQ.add(box);
            mHandler.postDelayed(this, ADD_RIGHT_BUTTON_TASK_INTERVAL);
        }
    };
    Runnable flattenAllAs = new Runnable(){
        @Override
        public void run() {
            for (View a : visibleA) {
                flattenLocation(a);
            }
        }
    };
    Runnable scrollLeftButtonTask = new Runnable() {
        @Override
        public void run() {
            TextView textView = (TextView)findViewById(R.id.hello);
            RelativeLayout layout = (RelativeLayout) findViewById(R.id.main);

            TextView highestA = (TextView)visibleA.get(0);

            int[] xy = new int[2];
            highestA.getLocationOnScreen(xy);
            if (xy[1] < -1 * BUTTON_HEIGHT) {
                float offsetBelow = BUTTON_HEIGHT + 2 * BUTTON_MARGIN;
                float lowestY = visibleA.get(visibleA.size() - 1).getY();
                highestA.setY(lowestY + offsetBelow);
                visibleA.remove(0);
                visibleA.add(highestA);
            }else {
                for (View answerView : visibleA) {
                    answerView.setY(answerView.getY() - SCROLL_LEFT_BUTTON_RATE);
                }
            }
            mHandler.postDelayed(this, SCROLL_LEFT_BUTTON_TASK_INTERVAL);
        }
    };
    private boolean hasNoSpaceForNextQ(){
        if (visibleQ == null || visibleQ.size() == 0) return false;
        int[] xy = new int[2];
        visibleQ.get(visibleQ.size()-1).getLocationInWindow(xy);
        int two_button_space = 2 * (MainActivity.BUTTON_HEIGHT + 4 * MainActivity.BUTTON_MARGIN);
        return xy[1] + two_button_space > getScreenDims().y;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        mHandler = new Handler(Looper.getMainLooper());
    }
    @Override
    protected void onStart() {
        super.onStart();
        if (isExternalStorageWritable()) {

        }
        TextView textView = (TextView)findViewById(R.id.hello);


        this.BUTTON_WIDTH = getScreenDims().x/3;
        populateQandA();
        Point screenDims = getScreenDims();

        textView.setText(textView.getText() + String.format("/ height is %s", screenDims.y));
        this.maxQs = estimateNumberOfSpots();
        fillAnswers((RelativeLayout) findViewById(R.id.main));

        mHandler.postDelayed(addRightButtonTask, ADD_RIGHT_BUTTON_TASK_INTERVAL);
        mHandler.postDelayed(scrollLeftButtonTask, SCROLL_LEFT_BUTTON_TASK_INTERVAL);
        mHandler.postDelayed(flattenAllAs, 100);
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
    private HashMap<String, String> QA = new HashMap<String, String>();
    private HashMap<String, String> AQ = new HashMap<String, String>();
    private ArrayList<String> Q = new ArrayList<String>();
    private ArrayList<String> A = new ArrayList<String>();
    private int maxId = 10;
    private static final int BUTTON_HEIGHT = 150;
    private int BUTTON_WIDTH;
    private static final int BUTTON_MARGIN = 10;
    private static final int ADD_RIGHT_BUTTON_TASK_INTERVAL = 6000;
    private static final int SCROLL_LEFT_BUTTON_TASK_INTERVAL = 125;
    private static final int SCROLL_LEFT_BUTTON_RATE = 8;
    private HashMap<TextView, TextView> QsTakenByA = new HashMap<>();
    ArrayList<TextView> visibleQ = new ArrayList<>();
    ArrayList<TextView> visibleA = new ArrayList<>();
    ArrayList<String> visibleAUnused = new ArrayList<String>();
    private int maxQs = 0;

    public void addA(TextView answerView){
        visibleA.add(answerView);
        visibleAUnused.add((String)answerView.getText());
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
                    new InputStreamReader( getAssets().open("animals_flipped.txt")));
            for(String line; (line = br.readLine()) != null; ){
                String[] qa = line.split(" ", 2);
                if (qa.length > 1) {
                    this.QA.put(qa[0], qa[1]);
                    this.AQ.put(qa[1], qa[0]);
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
    public TextView addAndReturnButton(ViewGroup vg, int width, int newId, int belowId,
                                       String text, boolean addToRight){
        final TextView button = new TextView(this);
        RelativeLayout.LayoutParams lp = getRelativeLayoutParams(width, belowId);
        if (addToRight){
            lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            button.setBackgroundColor(WAITING_Q_COLOR);
        } else {
            button.setBackgroundColor(CANDIDATE_A_COLOR);
            button.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    mHandler.postDelayed(new MoveActivatedTextView((TextView)v),
                                         SCROLL_LEFT_BUTTON_RATE);
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
