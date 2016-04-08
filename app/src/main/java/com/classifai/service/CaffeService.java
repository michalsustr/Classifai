package com.classifai.service;

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
public class CaffeService {
    private static final String LOG_TAG = "CaffeService";
    private final CaffeMobile caffeMobile;
    private CNNTask cnnTask;
    private String caffeModelDeploy;
    private String caffeModelWeights;
    private String caffeModelLabels;

    private String[] class2label;

    static {
        System.loadLibrary("caffe");
        System.loadLibrary("caffe_jni");
    }



    public CaffeService(String caffeModelDeploy, String caffeModelWeights, String caffeModelLabels) {
        this.caffeModelDeploy  = caffeModelDeploy;
        this.caffeModelWeights = caffeModelWeights;
        this.caffeModelLabels  = caffeModelLabels;

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
            e.printStackTrace();
        }
    }

    public final CaffeMobile getCaffeMobile() {
        return caffeMobile;
    }

    public void classifyImage(String imgPath, CNNListener result) {
        cnnTask = new CNNTask(result);
        cnnTask.execute(imgPath);
//        cnnTask.
    }

    private class CNNTask extends AsyncTask<String, Void, CaffeResult> {
        private CNNListener listener;
        private long startTime;

        public CNNTask(CNNListener listener) {
            this.listener = listener;
        }

        @Override
        protected CaffeResult doInBackground(String... strings) {
            startTime = SystemClock.uptimeMillis();
            Log.i(LOG_TAG, "started processing");
            CaffeResult result = new CaffeResult(caffeMobile.getConfidenceScore(strings[0]), class2label);
            Log.i(LOG_TAG, "done processing");
            return result;
        }

        @Override
        protected void onPostExecute(CaffeResult result) {
            long executionTime = SystemClock.uptimeMillis() - startTime;
            Log.i(LOG_TAG, String.format("elapsed wall time: %d ms", executionTime));
            Log.i(LOG_TAG, result.toString());

            result.setExecutionTime(executionTime);
            listener.onRecognitionCompleted(result);
            super.onPostExecute(result);
        }
    }
}
