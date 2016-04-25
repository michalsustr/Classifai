package com.classifai.tools.recognition;

import android.content.Context;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;

import com.sh1r0.caffe_android_lib.CaffeMobile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Created by Michal Sustr [michal.sustr@gmail.com] on 4/8/16.
 */
public class RecognitionService {
    private static final String TAG = "classifai";
    private final CaffeMobile caffeMobile;
    private final Context context;
    private CNNTask cnnTask;
    private String caffeModelDeploy;
    private String caffeModelWeights;
    private String caffeModelLabels;

    private String[] class2label;

    static {
        System.loadLibrary("caffe");
        System.loadLibrary("caffe_jni");
    }

    private RecognitionResult lastResult;


    public RecognitionService(String caffeModelDeploy, String caffeModelWeights, String caffeModelLabels, Context context) {
        this.caffeModelDeploy  = caffeModelDeploy;
        this.caffeModelWeights = caffeModelWeights;
        this.caffeModelLabels  = caffeModelLabels;
        this.context = context;

        if(!new File(caffeModelDeploy).exists()
                || !new File(caffeModelWeights).exists()
                || !new File(caffeModelLabels).exists()
                ) {
            throw new IllegalArgumentException(
                "caffe model files do not exist: "+caffeModelDeploy+" or "+caffeModelWeights+" or "+caffeModelLabels);
        }

        caffeMobile = new CaffeMobile();
        caffeMobile.setNumThreads(4);
        caffeMobile.loadModel(this.caffeModelDeploy, this.caffeModelWeights);

        float[] meanValues = {104, 117, 123};
        caffeMobile.setMean(meanValues);

        loadLabels();
    }

    private void loadLabels() {
        if(caffeModelLabels == null) {
            throw new IllegalArgumentException("label file is not set");
        }

        try {
            InputStream is = new FileInputStream(new File(caffeModelLabels));
            Scanner sc = new Scanner(is);
            List<String> lines = new ArrayList<String>();
            while (sc.hasNextLine()) {
                final String temp = sc.nextLine();
                lines.add(temp.substring(temp.indexOf(" ") + 1));
            }
            class2label = lines.toArray(new String[0]);
        } catch (IOException e) {
            Log.e(TAG, "RecognitionService.loadLabels error reading: "+e);
            e.printStackTrace();
        }
    }

    /**
     * synchronized so that there can't be multiple calls of warmup from other threads until
     * the execution of this one is finished. Warm-up is intended to run once anyway.
     */
    public synchronized void warmUp(final RecognitionListener warmupListener) {
        final String snapshotFile = "/storage/sdcard0/caffe/snapshot_0.jpg";
        Log.d(TAG, "RecognitionService.warmUp call [thread "+Thread.currentThread().getName()+"]");

        // This is warmup
        classifyImage(snapshotFile, new RecognitionListener() {
            @Override
            public void onRecognitionStart() {
                Log.d(TAG, "RecognitionService.warmUp start [thread "+Thread.currentThread().getName()+"]");
            }

            @Override
            public void onRecognitionCanceled() {
                Log.d(TAG, "RecognitionService.warmUp canceled [thread "+Thread.currentThread().getName()+"]");
            }

            @Override
            public void onRecognitionCompleted(RecognitionResult result) {
                Log.d(TAG, "RecognitionService.warmUp completed [thread "+Thread.currentThread().getName()+"]");
                // here we get the FPS we should use
                classifyImage(snapshotFile, warmupListener);
            }
        });
    }

    public final CaffeMobile getCaffeMobile() {
        return caffeMobile;
    }

    public void classifyImage(String imgPath, RecognitionListener result) {
        Log.i(TAG, "RecognitionService.classifyImage "+imgPath);
        // there might be already a running instance, if yes cancel it
        if(cnnTask != null) {
            if(cnnTask.getStatus() == AsyncTask.Status.RUNNING
            || cnnTask.getStatus() == AsyncTask.Status.PENDING) {
                Log.i(TAG, "RecognitionService.classifyImage asyncTask canceled "+cnnTask );
                cnnTask.cancel(true);
            }
            cnnTask = null;
        }

        cnnTask = new CNNTask(result);
        cnnTask.execute(imgPath);
    }

    public Float getFPSProcessingPower() {
        if(lastResult == null) return null;
        return lastResult.getFPS();
    }

    private class CNNTask extends AsyncTask<String, Void, RecognitionResult> {
        private RecognitionListener listener;
        private long startTime;

        public CNNTask(RecognitionListener listener) {
            this.listener = listener;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.i(TAG, "RecognitionService.CNNTask.onPreExecute [thread "+Thread.currentThread().getName()+"]");
            if(listener != null) {
                listener.onRecognitionStart();
            }
        }

        @Override
        protected RecognitionResult doInBackground(String... strings) {
            startTime = SystemClock.uptimeMillis();
            Log.i(TAG, "RecognitionService.CNNTask.doInBackground started processing [thread "+Thread.currentThread().getName()+"]");
            RecognitionResult result = new RecognitionResult(
                caffeMobile.getConfidenceScore(strings[0]), class2label);
            Log.i(TAG, "RecognitionService.CNNTask.doInBackground done processing [thread "+Thread.currentThread().getName()+"]");
            return result;
        }

        @Override
        protected void onPostExecute(RecognitionResult result) {
            long executionTime = SystemClock.uptimeMillis() - startTime;
            Log.i(TAG, "RecognitionService.CNNTask.onPostExecute "+String.format("elapsed wall time: %d ms", executionTime) + " [thread "+Thread.currentThread().getName()+"]");
            Log.i(TAG, "RecognitionService.CNNTask.onPostExecute top5 result: "+result.top5toString());

            lastResult = result;
            result.setExecutionTime(executionTime);

            cnnTask = null;
            if(listener != null) {
                listener.onRecognitionCompleted(result);
            }
        }

        @Override
        protected void onCancelled() {
            Log.i(TAG, "RecognitionService.CNNTask.onCancelled [thread "+Thread.currentThread().getName()+"]");
            listener.onRecognitionCanceled();
        }


    }
}
