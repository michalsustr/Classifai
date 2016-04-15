package com.classifai.tools;

import android.content.Context;
import android.util.Log;

import com.classifai.tools.camera.CameraSnapshotListener;
import com.classifai.tools.recognition.RecognitionListener;
import com.classifai.tools.recognition.RecognitionResult;

import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Michal Sustr [michal.sustr@gmail.com] on 4/15/16.
 */
public class CameraRecognition implements CameraSnapshotListener {
    private static final String LOG_TAG = "CameraRecognition";

    public CameraRecognition(Context context) {

    }



    @Override
    public void processCapturedJpeg(byte[] bytes) {
        try {
            FileOutputStream snapshot = new FileOutputStream("/storage/sdcard0/caffe/takeSnapshot.jpg");
            snapshot.write(bytes);
            snapshot.close();
            Log.d(LOG_TAG, "saved takeSnapshot");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
