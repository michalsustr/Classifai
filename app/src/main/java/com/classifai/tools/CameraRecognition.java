package com.classifai.tools;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Handler;
import android.util.Log;

import com.classifai.tools.camera.Camera;
import com.classifai.tools.camera.CameraSnapshotListener;
import com.classifai.tools.recognition.RecognitionListener;
import com.classifai.tools.recognition.RecognitionResult;
import com.classifai.tools.recognition.RecognitionService;

import java.io.File;
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
    private final Activity activity;

    private Handler handler;
    private SnapshotRunnable timedSnapshot;
    private RecognitionRunnable timedRecognition;
    private Integer snapshotNum = 0;

    public CameraRecognition(
            Camera camera,
            RecognitionService recognitionService,
            RecognitionListener recognitionListener,
            Activity activity) {
        handler = new Handler();
        this.camera = camera;
        this.recognitionService = recognitionService;
        this.recognitionListener = recognitionListener;
        this.activity = activity;
    }

    @Override
    public void processCapturedJpeg(Bitmap bitmap) {
        try {
            String snapshotFile = getSnapshotFileName(snapshotNum++);
            File file = new File(snapshotFile);
            file.createNewFile();
            FileOutputStream snapshot = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, snapshot);
            snapshot.close();
            Log.d(TAG, "CameraRecognition.processCapturedJpeg saved snapshot to " + snapshotFile);

        } catch (IOException e) {
            Log.e(TAG, "CameraRecognition.processCapturedJpeg error writing: "+e);
            e.printStackTrace();
        }
    }

    private String getSnapshotFileName(Integer num) {
        return "/storage/sdcard0/caffe/snapshot_"+(num)+".jpg";
    }


    public void startRecognition(int captureInterval, int recognitionPause) {
        Log.d(TAG, "CameraRecognition.startRecognition [thread " + Thread.currentThread().getName() + "]");
        if (timedSnapshot == null) {
            Log.d(TAG, "CameraRecognition.startRecognition new snapshot runnable");
            timedSnapshot = new SnapshotRunnable(captureInterval);
            timedRecognition = new RecognitionRunnable(recognitionPause);
            handler.post(timedSnapshot);
            handler.post(timedRecognition);
        }

        timedSnapshot.setCaptureInterval(captureInterval);
    }

    public void stopRecognition() {
        Log.d(TAG, "CameraRecognition.stopRecognition [thread "+Thread.currentThread().getName()+"]");
        if(timedSnapshot != null) {
            timedSnapshot.stopRunning();
        }
        if(timedRecognition != null) {
            timedRecognition.stopRunning();
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
            if(!doRun) {
                Log.d(TAG, "CameraRecognition.SnapshotRunnable.run canceled [thread "+Thread.currentThread().getName()+"]");
                return;
            }
            try {
                Log.d(TAG, "CameraRecognition.SnapshotRunnable.run taking snapshot with interval "+captureInterval+" [thread "+Thread.currentThread().getName()+"]");
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

    private class RecognitionRunnable implements Runnable {
        private volatile boolean doRun = true;
        private int recognitionPause;

        public RecognitionRunnable(int recognitionPause) {
            this.recognitionPause = recognitionPause;
        }

        @Override
        public void run() {
            if(!doRun) {
                Log.d(TAG, "CameraRecognition.RecognitionRunnable.run canceled [thread "+Thread.currentThread().getName()+"]");
                return;
            }
            if(snapshotNum > 0) {
                Log.d(TAG, "CameraRecognition.RecognitionRunnable.run recognize snapshot #"+snapshotNum+"  [thread " + Thread.currentThread().getName() + "]");
                recognitionService.classifyImage(getSnapshotFileName(snapshotNum), new RecognitionListener() {
                    @Override
                    public void onRecognitionStart() {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                recognitionListener.onRecognitionStart();
                            }
                        });
                    }

                    @Override
                    public void onRecognitionCompleted(final RecognitionResult result) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                recognitionListener.onRecognitionCompleted(result);
                            }
                        });
                        // start new recognition immediately
                        handler.postDelayed(RecognitionRunnable.this, recognitionPause);
                    }

                    @Override
                    public void onRecognitionCanceled() {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                recognitionListener.onRecognitionCanceled();
                            }
                        });
                    }
                });
            } else {
                Log.d(TAG, "CameraRecognition.RecognitionRunnable.run skip [thread " + Thread.currentThread().getName() + "]");
                handler.postDelayed(this, (int) (timedSnapshot.captureInterval*1.5f));
            }
        }

        public void stopRunning() {
            doRun = false;
        }
    }
}
