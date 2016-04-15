package com.classifai.activity;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.classifai.R;
import com.classifai.camera.Camera;
import com.classifai.camera.CroppedCameraPreview;
import com.classifai.recognition.RecognitionListener;
import com.classifai.recognition.RecognitionResult;
import com.classifai.recognition.RecognitionService;

import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends Activity implements RecognitionListener {
    private static final String LOG_TAG = "MainActivity";

    private static final String CAFFE_MODEL_DEPLOY  = "/storage/sdcard0/caffe/gnet_full.prototxt";
    private static final String CAFFE_MODEL_WEIGHTS = "/storage/sdcard0/caffe/gnet.caffemodel";
    private static final String CAFFE_MODEL_LABELS = "/storage/sdcard0/caffe/gnet.txt";

    private TextView fpsLabel;
    private TextView scoreLabel;
    private ProgressBar computingProgress;
    private ImageButton lightBtn;
    private CroppedCameraPreview cameraPreview;
//    private TextureView cameraPreview;
    private RelativeLayout layout;
    private SurfaceView alphaInner;

    private RecognitionService caffeService;
    private Camera camera;

    private Boolean lightOn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOG_TAG, "created");
        setContentView(R.layout.activity_main);

        scoreLabel = (TextView) findViewById(R.id.scoreLabel);
        fpsLabel   = (TextView) findViewById(R.id.fpsLabel);
        computingProgress = (ProgressBar) findViewById(R.id.computing_progress);
        lightBtn = (ImageButton) findViewById(R.id.btnLight);
        cameraPreview = (CroppedCameraPreview) findViewById(R.id.preview_surface);
//        cameraPreview = (TextureView) findViewById(R.id.preview_surface);
        layout = (RelativeLayout)  findViewById(R.id.layout);

        caffeService = new RecognitionService(CAFFE_MODEL_DEPLOY, CAFFE_MODEL_WEIGHTS, CAFFE_MODEL_LABELS);
        camera = new Camera(this, cameraPreview);

        // TODO: initialize - find what is optimal FPS processing
        // so that we can take camera snapshots with good intervals
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
            }
        }, 100);
    }

    @Override
    protected void onPause() {
        super.onPause();
        camera.closeCamera();
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

    public void onLightButtonClicked(View view) {
        camera.snapshot();

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
}
