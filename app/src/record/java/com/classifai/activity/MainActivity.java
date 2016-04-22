package com.classifai.activity;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.classifai.R;
import com.classifai.tools.camera.Camera;
import com.classifai.tools.camera.CroppedCameraPreview;

import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends Activity {
    private static final String TAG = "classifai";

    private CroppedCameraPreview cameraPreview;
    private Camera camera;

    private boolean isResumed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "MainActivity.onCreate");
        setContentView(R.layout.activity_main);

        cameraPreview = (CroppedCameraPreview) findViewById(R.id.preview_surface);
        camera = new Camera(this, cameraPreview);
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

        super.onPostResume();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "MainActivity.onPause");
        super.onPause();
        camera.closeCamera();
        isResumed = false;
    }

}
