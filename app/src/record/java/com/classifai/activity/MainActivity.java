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
import com.classifai.tools.camera.CameraSnapshotListener;
import com.classifai.tools.camera.CroppedCameraPreview;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends Activity implements CameraSnapshotListener {
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
    private int numRecordings = 0;
    private int numFrames = 0;
    private boolean isRecording = false;
    private CharSequence labelName;
    private SnapshotRunnable timedSnapshot;
    private volatile boolean snapshotProcessed;
    private Long recordingStartTime;
    private boolean doCreatePreview = false;


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
        labelText.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                InputMethodManager in = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                in.hideSoftInputFromWindow(view.getApplicationWindowToken(), 0);
                MainActivity.this.changeLabel(suggestionsAdapter.getItem(position));
            }
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
        Log.d(TAG, "MainActivity.changeLabel "+s);


        // if is recording, stop
        if(isRecording) {
            stopRecording();
        }
        labelName = s.toString().trim();
        numFrames = 0;

        numRecordings = getNumRecordings(labelName);
        showLabelPreview();
        updateLabelInfo();
    }

    private void showLabelPreview() {
        File preview = new File(getPreviewFileName());
        if(preview.exists()) {
            Bitmap bmp = BitmapFactory.decodeFile(preview.getAbsolutePath());
            labelPreview.setImageBitmap(bmp);
            Log.d(TAG, "MainActivity.changeLabel update label preview");
        } else {
            Bitmap bmp = BitmapFactory.decodeResource(getResources(),
                    R.drawable.question_mark);
            labelPreview.setImageBitmap(bmp);
            Log.d(TAG, "MainActivity.changeLabel update label no preview");
        }
    }

    private boolean doesLabelPreviewExist() {
        File preview = new File(getPreviewFileName());
        return preview.exists();
    }

    private int getNumRecordings(CharSequence labelName) {
        File candidateDir = new File(SAVE_DIR +"/"+labelName);
        if(!candidateDir.exists() || !candidateDir.isDirectory()) {
            return 0;
        }
        FileFilter fileFilter = new FileFilter() {
            public boolean accept(File file) {
                return file.isDirectory() && TextUtils.isDigitsOnly(file.getName());
            }
        };

        return candidateDir.listFiles(fileFilter).length;
    }

    private void updateLabelInfo() {
        int currentFps = 0;
        if(recordingStartTime != null) {
            currentFps = (int) (numFrames / ((System.currentTimeMillis() - recordingStartTime) / 1000.0f));
        }
        capturedFrames.setText(String.format("Frames: %d\nRecords: %d\nFPS: %d", numFrames, numRecordings, currentFps));
    }

    public void onRecordClick(View view) {
        if(isRecording) {
            stopRecording();
        } else {
            startRecording();
        }
    }

    private void initRecording() {
        Log.d(TAG, "MainActivity.initRecording");
        isRecording = false;
        recordBtn.setText(getString(R.string.start_recording));
        recordBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.start, 0,0,0);
    }

    private void startRecording() {
        Log.d(TAG, "MainActivity.startRecording");
        isRecording = true;
        recordBtn.setText(getString(R.string.stop_recording));
        recordBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.stop, 0, 0, 0);

        // update info
        numFrames = 0;
        numRecordings = getNumRecordings(labelName)+1;
        updateLabelInfo();

        // add to suggestions
        if(suggestionsAdapter.getPosition(labelName.toString()) < 0) {
            suggestionsAdapter.add(labelName.toString());
            suggestionsAdapter.notifyDataSetChanged();
        }

        // create dirs if necessary
        new File(SAVE_DIR +"/"+labelName+"/"+numRecordings+"/").mkdirs();

        // manage creating preview
        doCreatePreview = !doesLabelPreviewExist();

        Log.d(TAG, "CameraRecognition.startRecognition new snapshot runnable");
        snapshotProcessed = true;
        timedSnapshot = new SnapshotRunnable();
        recordingStartTime = System.currentTimeMillis();
        new Thread(timedSnapshot).start();
    }

    private void stopRecording() {
        Log.d(TAG, "MainActivity.stopRecording");
        isRecording = false;
        recordBtn.setText(getString(R.string.start_recording));
        recordBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.start, 0,0,0);
        if(timedSnapshot != null) {
            timedSnapshot.stopRunning();
            timedSnapshot = null;
        }
    }

    @Override
    public void processCapturedJpeg(Bitmap bitmap) {
        Log.d(TAG, "MainActivity.processCapturedJpeg [thread "+Thread.currentThread().getName()+"]");
        try {
            String snapshotFile = getSnapshotFileName(numFrames++);
            File file = new File(snapshotFile);
            file.createNewFile();
            FileOutputStream snapshot = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, snapshot);
            snapshot.close();
            Log.d(TAG, "MainActivity.processCapturedJpeg saved snapshot to " + snapshotFile);

            // do not create it out of first frame
            if(doCreatePreview && numFrames == 10) {
                FileOutputStream preview = new FileOutputStream(getPreviewFileName());
                bitmap.compress(Bitmap.CompressFormat.JPEG, 50, preview);
                preview.close();
                Log.d(TAG, "MainActivity.processCapturedJpeg saved preview");
                showLabelPreview();
            }

            snapshotProcessed = true;
            updateLabelInfo();
        } catch (IOException e) {
            Log.e(TAG, "MainActivity.processCapturedJpeg error writing: "+e);
            e.printStackTrace();
        }
    }

    private String getSnapshotFileName(int i) {
        return SAVE_DIR +"/"+labelName+"/"+numRecordings+"/"+i+".jpg";
    }

    public String getPreviewFileName() {
        return SAVE_DIR +"/"+labelName+"/preview.jpg";
    }

    private class SnapshotRunnable implements Runnable {
        private volatile boolean doRun = true;

        @Override
        public void run() {
            while(doRun) {
                // do not invoke unnecessarily too many snapshot requests
                if(snapshotProcessed) {
                    Log.d(TAG, "MainActivity.SnapshotRunnable.run taking snapshot [thread "+Thread.currentThread().getName()+"]");
                    snapshotProcessed = false;
                    camera.takeSnapshot(MainActivity.this);
                }
//                try {
//                    Thread.sleep(100, 0);
//                } catch (InterruptedException e) {
//                    Log.e(TAG, "MainActivity.SnapshotRunnable.run interrupted [thread "+Thread.currentThread().getName()+"]");
//                    e.printStackTrace();
//                }
            }
            Log.d(TAG, "MainActivity.SnapshotRunnable.run canceled [thread "+Thread.currentThread().getName()+"]");
        }
        public void stopRunning() {
            doRun = false;
        }


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
        initRecording();

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
