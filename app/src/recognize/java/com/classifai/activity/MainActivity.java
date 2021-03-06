package com.classifai.activity;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.classifai.R;
import com.classifai.tools.CameraRecognition;
import com.classifai.tools.camera.Camera;
import com.classifai.tools.camera.CroppedCameraPreview;
import com.classifai.tools.recognition.RecognitionListener;
import com.classifai.tools.recognition.RecognitionResult;
import com.classifai.tools.recognition.RecognitionService;
import com.classifai.ui.CircleProgress;

import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends Activity implements RecognitionListener {
    private static final String TAG = "classifai";

    private static final String CAFFE_MODEL_DEPLOY  = "/storage/sdcard0/caffe/gnet.prototxt";
    private static final String CAFFE_MODEL_WEIGHTS = "/storage/sdcard0/caffe/gnet.caffemodel";
    private static final String CAFFE_MODEL_LABELS  = "/storage/sdcard0/caffe/gnet.txt";

    private TextView fpsLabel;
    private TextView scoreLabel;
    private TextView statusText;
    private ImageButton lightBtn;
    private CroppedCameraPreview cameraPreview;
    private RelativeLayout layout;
    private CircleProgress circularProgressBar;
    private ImageView snapshotImage;
    private ImageView labelImage;
    private TextView labelImageText;

    private RecognitionService recognitionService;

    private Camera camera;
    private Boolean lightOn = false;

    private CameraRecognition cameraRecognition;
    /**
     * Interval for recognition capture
     */
    private Integer captureInterval;
    private boolean isResumed = false;
    private int animationPause = 300;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "MainActivity.onCreate");
        setContentView(R.layout.activity_main);

        scoreLabel = (TextView) findViewById(R.id.scoreLabel);
        statusText = (TextView) findViewById(R.id.statusText);
        fpsLabel   = (TextView) findViewById(R.id.fpsLabel);
//        computingProgress = (ProgressBar) findViewById(R.id.computing_progress);
        lightBtn = (ImageButton) findViewById(R.id.btnLight);
        cameraPreview = (CroppedCameraPreview) findViewById(R.id.preview_surface);
        layout = (RelativeLayout)  findViewById(R.id.layout);
        snapshotImage = (ImageView) findViewById(R.id.snapshotImage);
        labelImage =  (ImageView) findViewById(R.id.labelImage);
        labelImageText = (TextView) findViewById(R.id.labelImageText);

        recognitionService = new RecognitionService(
                CAFFE_MODEL_DEPLOY, CAFFE_MODEL_WEIGHTS, CAFFE_MODEL_LABELS, getApplicationContext());
        camera = new Camera(this, cameraPreview);
        cameraRecognition = new CameraRecognition(camera, recognitionService, this, this);

        circularProgressBar = (CircleProgress) findViewById(R.id.pb);

        // TODO: initialize - find what is optimal FPS processing
        // so that we can take camera snapshots with good intervals
        this.captureInterval = 4000;
    }



    @Override
    protected void onPostResume() {
        Log.d(TAG, "MainActivity.onPostResume");
//        circularProgressBar.animateProgress(2000);
        scoreLabel.setText("");
        statusText.setText(getString(R.string.loading_model_wait));
        snapshotImage.setVisibility(View.INVISIBLE);
        labelImageText.setVisibility(View.INVISIBLE);
        labelImage.setVisibility(View.INVISIBLE);

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

                    recognitionService.warmUp(new RecognitionListener() {
                        @Override
                        public void onRecognitionStart() {
                            Log.d(TAG, "MainActivity.onPostResume start FPS count [thread "+Thread.currentThread().getName()+"]");
                            statusText.setText(getString(R.string.estimateFps));
                        }

                        @Override
                        public void onRecognitionCompleted(RecognitionResult result) {
                            Log.d(TAG, "MainActivity.onPostResume completed FPS count [thread "+Thread.currentThread().getName()+"]");
//                            circularProgressBar.stopAnimation();
                            statusText.setText(String.format(getString(R.string.model_loaded_text),
                                    (result.getExecutionTime() / 1000.0)));
                            fpsLabel.setText("FPS: " + String.format("%.2f", result.getFPS()));
                            captureInterval = (int) (result.getExecutionTime());
                            Log.d(TAG, "MainActivity.onPostResume start recognition with capture interval "+captureInterval+" [thread "+Thread.currentThread().getName()+"]");
                            cameraRecognition.startRecognition(captureInterval, animationPause);
                        }

                        @Override
                        public void onRecognitionCanceled() {
                            Log.d(TAG, "MainActivity.onPostResume canceled FPS count [thread "+Thread.currentThread().getName()+"]");
                        }
                    });
                }
            }, 500);
        }

        super.onPostResume();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "MainActivity.onPause");
        super.onPause();
        cameraRecognition.stopRecognition();
        camera.closeCamera();
        isResumed = false;
    }

    public void onLightButtonClicked(View view) {
        if(lightOn) {
            Log.d(TAG, "MainActivity.onLightButtonClicked turn off");
            lightOn = false;
//            camera.turnLightOff();
            camera.startCapturingVideo();
            lightBtn.setBackground(getDrawable(R.drawable.off));
        } else {
            Log.d(TAG, "MainActivity.onLightButtonClicked turn on");
            lightOn = true;
            camera.stopCapturingVideo();
//            camera.turnLightOn();
            lightBtn.setBackground(getDrawable(R.drawable.on));
        }
    }

    @Override
    /**
     * @run
     */
    public void onRecognitionStart() {
        Log.d(TAG, "MainActivity.onRecognitionStart [thread "+Thread.currentThread().getName()+"]");

        circularProgressBar.setProgress(0);
        circularProgressBar.setProgressWithAnimation(95, captureInterval);
    }

    @Override
    public void onRecognitionCompleted(RecognitionResult result) {
        Log.d(TAG, "MainActivity.onRecognitionCompleted [thread "+Thread.currentThread().getName()+"]");

        Integer[] top5 = result.getTopKIndices(5);
        StringBuilder show = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            show.append(String.format("%.2f", result.getScore(top5[i])))
                    .append(" ").append(result.getLabel(top5[i])).append("\n");
        }

        // finish progress bar to 100% if we are faster
        circularProgressBar.stopAnimation();
        circularProgressBar.setProgressWithAnimation(100, animationPause);
        fpsLabel.setText("FPS: " + String.format("%.2f", result.getFPS()));
        scoreLabel.setText(show);
        statusText.setText("");

        snapshotImage.setVisibility(View.VISIBLE);
        labelImageText.setVisibility(View.VISIBLE);
        labelImage.setVisibility(View.VISIBLE);
        String lastSnapshot = cameraRecognition.getLastSnapshotRecognized();
        snapshotImage.setImageBitmap(BitmapFactory.decodeFile(lastSnapshot));
        Bitmap preview = result.getPreview(top5[0]);
        if(preview == null) {
            labelImage.setImageDrawable(getDrawable(R.drawable.question_mark));
        } else {
            labelImage.setImageBitmap(preview);
        }
    }

    @Override
    public void onRecognitionCanceled() {
        Log.d(TAG, "MainActivity.onRecognitionCanceled [thread "+Thread.currentThread().getName()+"]");
//        circularProgressBar.setProgress(0);
    }
}
