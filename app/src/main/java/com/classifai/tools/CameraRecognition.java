package com.classifai.tools;

import android.app.Activity;
import android.graphics.Bitmap;
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
public class CameraRecognition {
    private static final String TAG = "classifai";
    private final Camera camera;
    private final RecognitionService recognitionService;
    private final RecognitionListener recognitionListener;
    private final Activity activity;

    private SnapshotRunnable timedSnapshot;
    private RecognitionRunnable timedRecognition;
    private Integer snapshotNum = 0;

    public CameraRecognition(
            Camera camera,
            RecognitionService recognitionService,
            RecognitionListener recognitionListener,
            Activity activity) {
        this.camera = camera;
        this.recognitionService = recognitionService;
        this.recognitionListener = recognitionListener;
        this.activity = activity;
    }

    public void startRecognition(int captureInterval, int recognitionPause) {
        Log.d(TAG, "CameraRecognition.startRecognition [thread " + Thread.currentThread().getName() + "]");
        if (timedSnapshot == null) {
            Log.d(TAG, "CameraRecognition.startRecognition new runnables");
            timedSnapshot = new SnapshotRunnable(captureInterval);
            timedRecognition = new RecognitionRunnable(recognitionPause);
            new Thread(timedSnapshot).start();
            new Thread(timedRecognition).start();
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

    private String getSnapshotFileName(Integer num) {
        return "/storage/sdcard0/caffe/snapshot_"+(num)+".jpg";
    }

    private class SnapshotRunnable implements Runnable, CameraSnapshotListener {
        private int captureInterval;
        private volatile boolean doRun = true;
        public SnapshotRunnable(int captureInterval) {
            this.captureInterval = captureInterval;
        }

        @Override
        public void run() {
            while(doRun) {
                Log.d(TAG, "CameraRecognition.SnapshotRunnable.run taking snapshot with interval " + captureInterval + " [thread " + Thread.currentThread().getName() + "]");
                camera.takeSnapshot(this);

                try {
                    Thread.sleep(captureInterval);
                } catch (InterruptedException e) {
                    Log.e(TAG, "CameraRecognition.SnapshotRunnable.run interrupted "+e);
                    e.printStackTrace();
                }
            }
            Log.d(TAG, "CameraRecognition.SnapshotRunnable.run canceled [thread "+Thread.currentThread().getName()+"]");
        }

        @Override
        public void processCapturedJpeg(Bitmap bitmap) {
            try {
                String snapshotFile = getSnapshotFileName(++snapshotNum);
                Log.d(TAG, "CameraRecognition.processCapturedJpeg call " + snapshotFile + " [thread "+Thread.currentThread().getName()+"]");
                File file = new File(snapshotFile);
                file.createNewFile();
                FileOutputStream snapshot = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 50, snapshot);
                snapshot.close();
                Log.d(TAG, "CameraRecognition.processCapturedJpeg saved snapshot to " + snapshotFile + " [thread "+Thread.currentThread().getName()+"]");

            } catch (IOException e) {
                Log.e(TAG, "CameraRecognition.processCapturedJpeg error writing: "+e+" [thread "+Thread.currentThread().getName()+"]");
                e.printStackTrace();
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
        private volatile boolean doRecognize = true;
        private volatile int lastSnapshotRecognized = -1;

        public RecognitionRunnable(int recognitionPause) {
            this.recognitionPause = recognitionPause;
        }

        @Override
        public void run() {
            while(doRun) {
                if(!doRecognize) continue;
                if (snapshotNum <= 0 || lastSnapshotRecognized == snapshotNum) continue;

                // something like a semaphore
                Log.d(TAG, "CameraRecognition.RecognitionRunnable.run recognize snapshot #" + snapshotNum + "  [thread " + Thread.currentThread().getName() + "]");
                doRecognize = false;
                lastSnapshotRecognized = snapshotNum;
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
//                        Log.d(TAG, "CameraRecognition.RecognitionRunnable.run recognition pause [thread " + Thread.currentThread().getName() + "]");
//                        try {
//                            Thread.sleep(recognitionPause);
//                        } catch (InterruptedException e) {
//                            Log.e(TAG, "CameraRecognition.RecognitionRunnable.run interrupted "+e);
//                            e.printStackTrace();
//                        }
                        doRecognize = true;
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
            }

            Log.d(TAG, "CameraRecognition.RecognitionRunnable.run canceled [thread "+Thread.currentThread().getName()+"]");
        }

        public void stopRunning() {
            doRun = false;
        }
    }
}
