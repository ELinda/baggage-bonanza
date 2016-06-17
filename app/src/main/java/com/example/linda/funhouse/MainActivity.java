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
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public static final int ACTIVATED_A_COLOR = Color.parseColor("#4499CC");
    public static final int HINT_FLASH_A_COLOR = Color.parseColor("#5599EE");
    public static final int WAITING_Q_COLOR = Color.parseColor("#ff2277");
    public static final int CANDIDATE_A_COLOR = Color.parseColor("#2277AA");
    public static final int RIGHT_MATCH_COLOR = Color.parseColor("#00AA44");
    public static final int WRONG_MATCH_COLOR = Color.parseColor("#AA4400");
    public static final int TEXT_COLOR = Color.parseColor("#FFFFFF");
    public static final int HINT_PUNISHMENT_SECONDS = 10;
    protected static final int BUTTON_MARGIN = 10;
    protected static final int ADD_RIGHT_BUTTON_TASK_INTERVAL = 6000;
    protected static final int SCROLL_LEFT_BUTTON_TASK_INTERVAL = 125;
    protected static final int SCROLL_LEFT_BUTTON_RATE = 8;
    protected static final int HINT_DURATION = 500;
    protected static final int FILE_CHOOSER_REQUEST_CODE = 100;
    Intent fileIntent = null;
    protected StopWatch watch = null;
    protected HashMap<String, String> AQ = new HashMap<String, String>();
    protected HashMap<String, String> QA = new HashMap<String, String>();
    private ArrayList<String> Q = new ArrayList<String>();
    private ArrayList<String> A = new ArrayList<String>();
    private int maxId = 10;
    private String dataFileName = "ASSET:lesscommon.txt";    // file with q & a
    protected static final int BUTTON_HEIGHT = 150;
    protected int BUTTON_WIDTH;
    protected HashMap<TextView, TextView> QsTakenByA = new HashMap<>();
    protected HashMap<TextView, AnswerState> AStates = new HashMap<>();
    ArrayList<TextView> rightSideQs = new ArrayList<>();
    ArrayList<TextView> upwardsMovingAs = new ArrayList<>();
    ArrayList<String> AsWithoutQs = new ArrayList<String>();
    private int maxQs = 0;
    protected Handler mHandler;
    public enum AnswerState {
        CANDIDATE, SELECTED, MATCHED_CORRECT, MATCHED_WRONG
    }
    private void clearAllState() {
        Q.clear();
        A.clear();
        AQ.clear();
        QA.clear();
        clearState();
    }
    private void clearState(){
        RelativeLayout layout = (RelativeLayout) findViewById(R.id.main);
        // remove all children except the first one (the winner's notification)
        layout.removeViews(1, layout.getChildCount() - 1);
        QsTakenByA.clear();
        upwardsMovingAs.clear();
        AStates.clear();
        rightSideQs.clear();
        AsWithoutQs.clear();
        watch.reset();

        mHandler.removeCallbacks(scrollLeftButtonTask);
        mHandler.removeCallbacks(addRightButtonTask);
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
    protected void multiPlaceAReturningInBelt(List<TextView> aList){
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
    protected void placeAReturningInBelt(TextView a){
        List<TextView> aList = new ArrayList<>();
        aList.add(a);
        multiPlaceAReturningInBelt(aList);
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
        if (requestCode == FILE_CHOOSER_REQUEST_CODE && resultCode == RESULT_OK) {
            String fileName = data.getStringExtra(FileBrowserActivity.returnFileParameter);
            String justFile = "";
            if (fileName.matches("^ASSET:.*")) {
                justFile = fileName.split(":")[1];
            } else {
                justFile = fileName;
            }
            Toast.makeText(this, "Restarting with " + justFile,
                           Toast.LENGTH_LONG).show();
            clearAllState();
            start();
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
        mHandler.removeCallbacks(addRightButtonTask);
        mHandler.postDelayed(addRightButtonTask, ADD_RIGHT_BUTTON_TASK_INTERVAL);
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
        int offset = BUTTON_HEIGHT + BUTTON_MARGIN;
        if (A.size() == 0){
            ((TextView)findViewById(R.id.hello)).setText("No records found");
            return;
        }
        for (int i=0, fromTop = 0; i < maxQs; ++i, fromTop += offset) {
            int newID = incrementAndReturnMaxId();
            String answer = getRandomElementFrom(A);
            int width = this.BUTTON_WIDTH;
            TextView box = addAndReturnButton(layout, width, newID, -1, answer, false, fromTop);

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
        Reader reader = null;
        try {
            if (dataFileName.matches("^ASSET:.*")) {
                dataFileName = dataFileName.split(":")[1];
                reader = new InputStreamReader( getAssets().open(dataFileName));
            }else{
                reader = new FileReader(dataFileName);
            }
            br = new BufferedReader(reader);
            for(String line; (line = br.readLine()) != null; ){
                String[] qa = line.split("=", 2);
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
    public TextView addAndReturnButton(ViewGroup vg, int width, int newId, int belowId,
                                       String text, boolean addToRight, int fromTop){
        final TextView button = new TextView(this);
        RelativeLayout.LayoutParams lp = getRelativeLayoutParams(width, belowId);
        if (addToRight){
            lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            button.setBackgroundColor(WAITING_Q_COLOR);
            button.setOnClickListener(new ClickedQListener(this));
        } else {
            lp.topMargin = fromTop;
            button.setBackgroundColor(CANDIDATE_A_COLOR);
            button.setOnClickListener(new OnClickListener() {
                public MainActivity getMainActivity() {
                    return MainActivity.this;
                }
                public void onClick(View v) {
                    if (AStates.get(v).equals(AnswerState.CANDIDATE)){
                        AStates.put((TextView)v, AnswerState.SELECTED);
                        mHandler.postDelayed(
                                new MoveActivatedTextView(getMainActivity(), (TextView)v),
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
        button.setHeight(this.BUTTON_HEIGHT);
        vg.addView(button, lp);
        return button;
    }
    public TextView addNewButtonRight(ViewGroup vg, int width, int newId, int belowID, String text){
        return addAndReturnButton(vg, width, newId, belowID, text, true, -1);
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
