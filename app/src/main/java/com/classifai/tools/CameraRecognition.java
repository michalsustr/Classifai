package com.classifai.tools;

import android.os.Handler;
import android.util.Log;

import com.classifai.tools.camera.Camera;
import com.classifai.tools.camera.CameraSnapshotListener;
import com.classifai.tools.recognition.RecognitionListener;
import com.classifai.tools.recognition.RecognitionResult;
import com.classifai.tools.recognition.RecognitionService;

import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Michal Sustr [michal.sustr@gmail.com] on 4/15/16.
 */
public class CameraRecognition implements CameraSnapshotListener {
    private static final String TAG = "classifai";
    private final Camera camera;
    private final RecognitionService recognitionService;
    private final RecognitionListener recognitionListener;

    private Integer captureInterval;
    private SnapshotRunnable timedSnapshot;
    private Handler handler;
    private Integer snapshotNum = 0;

    public CameraRecognition(
            Camera camera,
            RecognitionService recognitionService,
            RecognitionListener recognitionListener) {
        handler = new Handler();
        this.camera = camera;
        this.recognitionService = recognitionService;
        this.recognitionListener = recognitionListener;
    }

    public void warmUpCaffe(final RecognitionListener warmupListener) {
        final String snapshotFile = "/storage/sdcard0/caffe/snapshot_0.jpg";
        // This is warmup
        recognitionService.classifyImage(snapshotFile, new RecognitionListener() {
            @Override
            public void onRecognitionStart() {}
            @Override
            public void onRecognitionCanceled() {}
            @Override
            public void onRecognitionCompleted(RecognitionResult result) {
                // here we get the FPS we should use
                recognitionService.classifyImage(snapshotFile, warmupListener);
            }
        });
    }


    @Override
    public void processCapturedJpeg(byte[] bytes) {
        try {
            String snapshotFile = "/storage/sdcard0/caffe/snapshot_"+(snapshotNum++)+".jpg";
            FileOutputStream snapshot = new FileOutputStream(snapshotFile);
            snapshot.write(bytes);
            snapshot.close();
            Log.d(TAG, "CameraRecognition.processCapturedJpeg saved snapshot to "+snapshotFile);

            recognitionService.classifyImage(snapshotFile, recognitionListener);
        } catch (IOException e) {
            Log.e(TAG, "CameraRecognition.processCapturedJpeg error writing: "+e);
            e.printStackTrace();
        }
    }


    public void startRecognition(int captureInterval) {
        if(timedSnapshot == null) {
            timedSnapshot = new SnapshotRunnable(captureInterval);
            handler.post(timedSnapshot);
        } else {
            timedSnapshot.setCaptureInterval(captureInterval);
        }
    }

    public void stopRecognition() {
        if(timedSnapshot != null) {
            timedSnapshot.stopRunning();
        }
    }

    private class SnapshotRunnable implements Runnable {
        private int captureInterval;
        private volatile boolean doRun = true;
        public SnapshotRunnable(int captureInterval) {
            this.captureInterval = captureInterval;
        }

        @Override
        public void run() {
            if(!doRun) return;
            try {
                Log.d(TAG, "CameraRecognition.SnapshotRunnable.run taking snapshot with interval "+captureInterval);
                camera.takeSnapshot(CameraRecognition.this);
            } finally {
                handler.postDelayed(this, captureInterval);
            }
        }

        public void setCaptureInterval(int captureInterval) {
            this.captureInterval = captureInterval;
        }

        public void stopRunning() {
            doRun = false;
        }
    }
}
