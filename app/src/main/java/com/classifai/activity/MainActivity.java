package com.classifai.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.classifai.service.CNNListener;
import com.classifai.utils.FloatIndexComparator;
import com.classifai.R;
import com.sh1r0.caffe_android_lib.CaffeMobile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Scanner;


public class MainActivity extends Activity implements CNNListener {
    private static final String LOG_TAG = "MainActivity";
    private static final int REQUEST_IMAGE_CAPTURE = 100;
    private static final int REQUEST_IMAGE_SELECT = 200;
    public static final int MEDIA_TYPE_IMAGE = 1;
    private static final String CAFFE_MODEL_DEPLOY  = "/storage/sdcard0/caffe/gnet_full.prototxt";
    private static final String CAFFE_MODEL_WEIGHTS = "/storage/sdcard0/caffe/gnet.caffemodel";
    private static final String CAFFE_MODEL_LABELS = "/storage/sdcard0/caffe/gnet.txt";
    private static String[] RECOGNITION_CLASSES;

    private Button btnCamera;
    private Button btnSelect;
    private ImageView ivCaptured;
    private TextView tvLabel;
    private TextView execLabel;
    private Uri fileUri;
    private ProgressDialog dialog;
    private Bitmap bmp;
    private CaffeMobile caffeMobile;

    static {
        System.loadLibrary("caffe");
        System.loadLibrary("caffe_jni");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOG_TAG, "created");
        // check model existence
        if(!new File(CAFFE_MODEL_DEPLOY).exists()
            || !new File(CAFFE_MODEL_WEIGHTS).exists()
            || !new File(CAFFE_MODEL_LABELS).exists()
        ) {
            throw new RuntimeException("caffe model files do not exist, at "+CAFFE_MODEL_DEPLOY+" and "+CAFFE_MODEL_WEIGHTS);
        }


        setContentView(R.layout.activity_main);

        ivCaptured = (ImageView) findViewById(R.id.ivCaptured);
        tvLabel = (TextView) findViewById(R.id.tvLabel);
        execLabel = (TextView) findViewById(R.id.execLabel);

        btnCamera = (Button) findViewById(R.id.btnCamera);
        btnCamera.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                initPrediction();
                fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
                Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                i.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
                startActivityForResult(i, REQUEST_IMAGE_CAPTURE);
            }
        });

        btnSelect = (Button) findViewById(R.id.btnSelect);
        btnSelect.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                initPrediction();
                Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, REQUEST_IMAGE_SELECT);
            }
        });

        // TODO: implement a splash screen(?
        caffeMobile = new CaffeMobile();
        caffeMobile.setNumThreads(4);
        caffeMobile.loadModel(CAFFE_MODEL_DEPLOY, CAFFE_MODEL_WEIGHTS);

        float[] meanValues = {104, 117, 123};
        caffeMobile.setMean(meanValues);

        try {
            InputStream is = new FileInputStream(new File(CAFFE_MODEL_LABELS));
            Scanner sc = new Scanner(is);
            List<String> lines = new ArrayList<String>();
            while (sc.hasNextLine()) {
                final String temp = sc.nextLine();
                lines.add(temp.substring(temp.indexOf(" ") + 1));
            }
            RECOGNITION_CLASSES = lines.toArray(new String[0]);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == REQUEST_IMAGE_CAPTURE || requestCode == REQUEST_IMAGE_SELECT) && resultCode == RESULT_OK) {
            String imgPath;

            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                imgPath = fileUri.getPath();
            } else {
                Uri selectedImage = data.getData();
                String[] filePathColumn = {MediaStore.Images.Media.DATA};
                Cursor cursor = MainActivity.this.getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                imgPath = cursor.getString(columnIndex);
                cursor.close();
            }

            bmp = BitmapFactory.decodeFile(imgPath);
            Log.d(LOG_TAG, imgPath);
            Log.d(LOG_TAG, String.valueOf(bmp.getHeight()));
            Log.d(LOG_TAG, String.valueOf(bmp.getWidth()));

            dialog = ProgressDialog.show(MainActivity.this, "Predicting...", "Wait for one sec...", true);

            CNNTask cnnTask = new CNNTask(MainActivity.this);
            cnnTask.execute(imgPath);
        } else {
            btnCamera.setEnabled(true);
            btnSelect.setEnabled(true);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void initPrediction() {
        btnCamera.setEnabled(false);
        btnSelect.setEnabled(false);
        tvLabel.setText("");
    }

    private class CNNTask extends AsyncTask<String, Void, float[]> {
        private CNNListener listener;
        private long startTime;

        public CNNTask(CNNListener listener) {
            this.listener = listener;
        }

        @Override
        protected float[] doInBackground(String... strings) {
            startTime = SystemClock.uptimeMillis();
            float[] scores = caffeMobile.getConfidenceScore(strings[0]);
            return scores;
        }

        @Override
        protected void onPostExecute(float[] scores) {
            long executionTime = SystemClock.uptimeMillis() - startTime;
            Log.i(LOG_TAG, String.format("elapsed wall time: %d ms", executionTime));
            listener.onTaskCompleted(scores, executionTime);
            super.onPostExecute(scores);
        }
    }

    @Override
    public void onTaskCompleted(float[] scores, long executionTime) {
        // find max
        int maxIdx = 0;
        String result = "";
        FloatIndexComparator comparator = new FloatIndexComparator(scores);
        Integer[] indexes = comparator.createIndexArray();
        Arrays.sort(indexes, comparator);
        for (int i = 0; i < 5; i++) {
            result += String.format("%.2f", scores[indexes[i]]) + " " + RECOGNITION_CLASSES[indexes[i]] + "\n";
        }
        for (int i = 0; i < scores.length; i++) {
            Log.i(LOG_TAG, "onTaskCompleted score: "+i+" "+indexes[i]+" "+ RECOGNITION_CLASSES[indexes[i]]+ " is "+scores[indexes[i]]);
        }

        ivCaptured.setImageBitmap(bmp);
        tvLabel.setText(result);
        execLabel.setText("Execution time: "+executionTime+ "ms");
        btnCamera.setEnabled(true);
        btnSelect.setEnabled(true);

        if (dialog != null) {
            dialog.dismiss();
        }
    }

    /**
     * Create a file Uri for saving an image or video
     */
    private static Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /**
     * Create a File for saving an image or video
     */
    private static File getOutputMediaFile(int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "Caffe-Android-Demo");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_" + timeStamp + ".jpg");
        } else {
            return null;
        }

        return mediaFile;
    }
}
