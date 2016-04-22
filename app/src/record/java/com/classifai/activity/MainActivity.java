package com.classifai.activity;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.classifai.R;
import com.classifai.tools.camera.Camera;
import com.classifai.tools.camera.CroppedCameraPreview;

import java.io.File;
import java.io.FileFilter;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends Activity {
    private static final String TAG = "classifai";

    private static final String SAVE_DIR  = "/storage/sdcard0/caffe/record/";

    private CroppedCameraPreview cameraPreview;
    private AutoCompleteTextView labelText;
    private TextView capturedFrames;
    private Button recordBtn;
    private ImageView labelPreview;

    private Camera camera;
    private boolean isResumed = false;

    private ArrayAdapter<String> suggestionsAdapter;
    private int numFrames = 0;
    private int numRecordings = 0;
    private boolean isRecording = false;
    private CharSequence labelName;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "MainActivity.onCreate");
        setContentView(R.layout.activity_main);

        cameraPreview = (CroppedCameraPreview) findViewById(R.id.preview_surface);
        camera = new Camera(this, cameraPreview);
        labelText = (AutoCompleteTextView) findViewById(R.id.labelText);
        labelText.setThreshold(0);
        capturedFrames = (TextView) findViewById(R.id.capturedFrames);
        recordBtn = (Button) findViewById(R.id.recordBtn);
        labelPreview = (ImageView) findViewById(R.id.labelPreview);

        suggestionsAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, getSuggestions());
        labelText.setAdapter(suggestionsAdapter);
        labelText.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                hideKeyboard();
                MainActivity.this.changeLabel(suggestionsAdapter.getItem(position));
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        labelText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRecording();
            }
        });
        labelText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                MainActivity.this.changeLabel(s);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        labelText.setText(suggestionsAdapter.getItem(0));
    }

    private String[] getSuggestions() {
        File dir = new File(SAVE_DIR);
        FileFilter fileFilter = new FileFilter() {
            public boolean accept(File file) {
                return file.isDirectory();
            }
        };
        File[] dirs = dir.listFiles(fileFilter);
        String[] staticSuggestions = getResources().getStringArray(R.array.label_suggestions);
        String[] suggestions = new String[staticSuggestions.length + dirs.length];
        for (int i = 0; i < dirs.length; i++) {
            suggestions[i] = dirs[i].getName();
        }
        for (int i = dirs.length; i < staticSuggestions.length + dirs.length; i++) {
            suggestions[i] = staticSuggestions[i-dirs.length];
        }
        return suggestions;
    }

    public void changeLabel(CharSequence s) {
        Log.d(TAG, "changeLabel "+s);
        // if is recording, stop
        if(isRecording) {
            stopRecording();
        }
        labelName = s;
        numFrames = 0;

        // try to find folder and info
        File candidateDir = new File(SAVE_DIR +"/"+s);
        if(!candidateDir.exists() || !candidateDir.isDirectory()) {
            numRecordings = 0;
        } else {
            FileFilter fileFilter = new FileFilter() {
                public boolean accept(File file) {
                    return file.isDirectory() && TextUtils.isDigitsOnly(file.getName());
                }
            };

            numRecordings = candidateDir.listFiles(fileFilter).length;
        }

        // set up preview
        File preview = new File(SAVE_DIR +"/"+s+"/preview.png");
        if(preview.exists()) {
            Bitmap bmp = BitmapFactory.decodeFile(preview.getAbsolutePath());
            labelPreview.setImageBitmap(bmp);
            Log.d(TAG, "changeLabel update label preview");
        } else {
            Bitmap bmp = BitmapFactory.decodeResource(getResources(),
                    R.drawable.question_mark);
            labelPreview.setImageBitmap(bmp);
            Log.d(TAG, "changeLabel update label no preview");
        }

        capturedFrames.setText(String.format("%d frames\n%d recordings", numFrames, numRecordings));
    }

    public void onRecordClick(View view) {
        if(isRecording) {
            stopRecording();
        } else {
            startRecording();
        }
    }

    private void startRecording() {
        isRecording = true;
        recordBtn.setText(getString(R.string.stop_recording));
        recordBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.stop, 0, 0, 0);
    }

    private void stopRecording() {
        isRecording = false;
        recordBtn.setText(getString(R.string.start_recording));
        recordBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.start, 0,0,0);
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    protected void onPostResume() {
        Log.d(TAG, "MainActivity.onPostResume");
        // This is kind of hacky, but I don't know how to do it otherwise to initialize
        // the camera so the stream can be viewed.
        // Seems that the camera cannot render into textureview right away and
        // something needs to be initialized first, but I don't know what is the event
        // I should subscribe to.
        //
        // I tried OnGlobalLayoutListener on layout.getViewTreeObserver but it didn't show up
        // although logs show that the camera is streaming...
        //
        // Also, for some reason, onPostResume is called twice!! in the app - this shouldn't happen.
        // I found on stackoverflow that it can be caused sometime by calling super.setContentView
        // instead of setContentView, but it isn't the case here. Argh, these faulty state machines
        // are pain in the ass. If it could be nicely visualized with something like timed automata
        // that would've been very nice.
        // TODO: find better solution
        if(!isResumed) {
            isResumed = true;
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    Log.d(TAG, "MainActivity.onPostResume open camera [thread "+Thread.currentThread().getName()+"]");
                    camera.openCamera();

                }
            }, 500);
        }

        // set initial state correctly
        stopRecording();

        super.onPostResume();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "MainActivity.onPause");
        super.onPause();
        stopRecording();
        camera.closeCamera();
        isResumed = false;
    }
}
