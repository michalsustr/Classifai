package com.classifai.activity;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.classifai.R;
import com.classifai.tools.CameraRecognition;
import com.classifai.tools.camera.Camera;
import com.classifai.tools.camera.CroppedCameraPreview;
import com.classifai.tools.recognition.RecognitionListener;
import com.classifai.tools.recognition.RecognitionResult;
import com.classifai.tools.recognition.RecognitionService;

import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends Activity implements RecognitionListener {
    private static final String LOG_TAG = "MainActivity";

    private static final String CAFFE_MODEL_DEPLOY  = "/storage/sdcard0/caffe/gnet_full.prototxt";
    private static final String CAFFE_MODEL_WEIGHTS = "/storage/sdcard0/caffe/gnet.caffemodel";
    private static final String CAFFE_MODEL_LABELS = "/storage/sdcard0/caffe/gnet.txt";

    private TextView fpsLabel;
    private TextView scoreLabel;
    private ImageButton lightBtn;
    private CroppedCameraPreview cameraPreview;
    private RelativeLayout layout;
    private SurfaceView alphaInner;

    private RecognitionService caffeService;
    private Camera camera;

    private Boolean lightOn = false;
    private CameraRecognition cameraRecognition;

    /**
     * Interval for recognition capture
     */
    private Integer captureInterval;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOG_TAG, "created");
        setContentView(R.layout.activity_main);

        scoreLabel = (TextView) findViewById(R.id.scoreLabel);
        fpsLabel   = (TextView) findViewById(R.id.fpsLabel);
//        computingProgress = (ProgressBar) findViewById(R.id.computing_progress);
        lightBtn = (ImageButton) findViewById(R.id.btnLight);
        cameraPreview = (CroppedCameraPreview) findViewById(R.id.preview_surface);
        layout = (RelativeLayout)  findViewById(R.id.layout);

        caffeService = new RecognitionService(CAFFE_MODEL_DEPLOY, CAFFE_MODEL_WEIGHTS, CAFFE_MODEL_LABELS);
        camera = new Camera(this, cameraPreview);
        cameraRecognition = new CameraRecognition(camera, caffeService, this);

        // TODO: initialize - find what is optimal FPS processing
        // so that we can take camera snapshots with good intervals
        this.captureInterval = 2000;
    }



    @Override
    protected void onPostResume() {
        super.onPostResume();
        // This is kind of hacky, but I don't know how to do it otherwise to initialize
        // the camera so the stream can be viewed.
        // Seems that the camera cannot render into textureview right away and
        // something needs to be initialized first, but I don't know what is the event
        // I should subscribe to.
        //
        // I tried OnGlobalLayoutListener on layout.getViewTreeObserver but it didn't show up
        // although logs show that the camera is streaming...
        // TODO: find better solution
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                camera.openCamera();
                cameraRecognition.startRecognition(captureInterval);
            }
        }, 100);
    }

    @Override
    protected void onPause() {
        super.onPause();
        camera.closeCamera();
        cameraRecognition.stopRecognition();
    }

    public void onLightButtonClicked(View view) {
        if(lightOn) {
            Log.d(LOG_TAG, "onLightButtonClicked turn off");
            lightOn = false;
            camera.turnLightOff();
            lightBtn.setBackground(getDrawable(R.drawable.off));
        } else {
            lightOn = true;
            Log.d(LOG_TAG, "onLightButtonClicked turn on");
            camera.turnLightOn();
            lightBtn.setBackground(getDrawable(R.drawable.on));
        }
    }

    @Override
    public void onRecognitionCompleted(RecognitionResult result) {
        Integer[] top5 = result.getTopKIndices(5);
        StringBuilder show = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            show.append(String.format("%.2f", result.getScore(top5[i])))
                    .append(" ").append(result.getLabel(top5[i])).append("\n");
        }

        fpsLabel.setText("FPS: " + String.format("%.2f", result.getFPS()));
        scoreLabel.setText(show);
//        computingProgress.getIndeterminateDrawable()
//            .setColorFilter(0xFFFF0000, android.graphics.PorterDuff.Mode.MULTIPLY);

    }
}
