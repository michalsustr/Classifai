package com.classifai.tools.camera;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.MediaRecorder;
import android.util.Log;

import com.classifai.R;
import com.ragnarok.rxcamera.RxCamera;
import com.ragnarok.rxcamera.RxCameraData;
import com.ragnarok.rxcamera.config.RxCameraConfig;
import com.ragnarok.rxcamera.config.RxCameraConfigChooser;

import java.io.ByteArrayOutputStream;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * Created by Michal Sustr [michal.sustr@gmail.com] on 4/8/16.
 */
public class Camera {
    private static final String TAG = "classifai";

    private final Context context;
    private CroppedCameraPreview cameraPreview;
    private RxCamera camera;
    private android.hardware.Camera.Size cameraSize;
    private MediaRecorder recorder;

    private final int cameraMinFPS;
    private final int cameraMaxFPS;
    private final int cameraNativeWidth;
    private final int cameraNativeHeight;
    private final int cameraDisplayWidth;
    private final int cameraDisplayHeight;
    private final int captureCropWidth;
    private final int captureCropHeight;
    private final int captureSaveWidth;
    private final int captureSaveHeight;

    public Camera(Context context, CroppedCameraPreview textureView) {
        this.cameraPreview = textureView;
        this.context = context;

//        recorder = new MediaRecorder();
//        recorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);
//        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
//        recorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);
//        recorder.setOutputFile("/storage/sdcard0/caffe/video.mp4");

        // load settings
        cameraNativeWidth = context.getResources().getInteger(R.integer.cameraNativeWidth);
        cameraNativeHeight = context.getResources().getInteger(R.integer.cameraNativeHeight);
        cameraDisplayWidth = context.getResources().getInteger(R.integer.cameraDisplayWidth);
        cameraDisplayHeight = context.getResources().getInteger(R.integer.cameraDisplayHeight);
        cameraMinFPS = context.getResources().getInteger(R.integer.cameraMinFPS);
        cameraMaxFPS = context.getResources().getInteger(R.integer.cameraMaxFPS);
        captureCropWidth = context.getResources().getInteger(R.integer.captureCropWidth);
        captureCropHeight = context.getResources().getInteger(R.integer.captureCropHeight);
        captureSaveWidth = context.getResources().getInteger(R.integer.captureSaveWidth);
        captureSaveHeight = context.getResources().getInteger(R.integer.captureSaveHeight);
    }

    public void openCamera() {
        RxCameraConfig config = RxCameraConfigChooser.obtain().
                useBackCamera().
                setAutoFocus(true).
                setPreferPreviewFrameRate(cameraMinFPS, cameraMaxFPS).
                setPreferPreviewSize(new Point(cameraNativeWidth, cameraNativeHeight)).
                setPreviewFormat(ImageFormat.YUY2).
                setHandleSurfaceEvent(true).
                get();
        Log.d(TAG, "Camera.openCamera config: " + config);

        RxCamera.open(context, config).flatMap(new Func1<RxCamera, Observable<RxCamera>>() {
            @Override
            public Observable<RxCamera> call(RxCamera rxCamera) {
                Log.d(TAG, "Camera.openCamera isopen: " + rxCamera.isOpenCamera() + ", thread: " + Thread.currentThread());
                camera = rxCamera;
                cameraSize = camera.getNativeCamera().getParameters().getPreviewSize();
//                recorder.setCamera(camera.getNativeCamera());

                return rxCamera.bindTexture(cameraPreview.getTextureView());
//                return rxCamera.bindTexture(cameraPreview);
            }
        }).flatMap(new Func1<RxCamera, Observable<RxCamera>>() {
            @Override
            public Observable<RxCamera> call(RxCamera rxCamera) {
                Log.d(TAG, "Camera.openCamera isbindsurface: " + rxCamera.isBindSurface() + ", thread: " + Thread.currentThread());
                return rxCamera.startPreview();
            }
        }).observeOn(AndroidSchedulers.mainThread()).subscribe(new Subscriber<RxCamera>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "Camera.openCamera open camera error: " + e.getMessage());
            }

            @Override
            public void onNext(final RxCamera rxCamera) {
                camera = rxCamera;
                Log.d(TAG, "Camera.openCamera open camera success: " + camera);
            }
        });
    }

    public void closeCamera() {
        if (camera != null) {
            camera.closeCameraWithResult().subscribe(new Action1<Boolean>() {
                @Override
                public void call(Boolean aBoolean) {
                    Log.d(TAG, "Camera.closeCamera close camera finished, success: " + aBoolean);
                }
            });
        }
    }

    public void turnLightOn() {
        if (!checkCamera()) {
            return;
        }
//        camera.action().flashAction(true).subscribe(new Subscriber<RxCamera>() {
//            @Override
//            public void onCompleted() {
//
//            }
//
//            @Override
//            public void onError(Throwable e) {
//                Log.e(LOG_TAG, "open flash error: " + e.getMessage());
//            }
//
//            @Override
//            public void onNext(RxCamera rxCamera) {
//                Log.d(LOG_TAG, "open flash");
//            }
//        });
    }

    public void turnLightOff() {
        if (!checkCamera()) {
            return;
        }
//        camera.action().flashAction(false).subscribe(new Subscriber<RxCamera>() {
//            @Override
//            public void onCompleted() {
//
//            }
//
//            @Override
//            public void onError(Throwable e) {
//                Log.e(LOG_TAG, "close flash error: " + e.getMessage());
//            }
//
//            @Override
//            public void onNext(RxCamera rxCamera) {
//                Log.d(LOG_TAG, "close flash");
//            }
//        });
    }

    private boolean checkCamera() {
//        if (camera == null || !camera.isOpenCamera()) {
//            return false;
//        }
//        return true;
        return true;
    }


    public void takeSnapshot(final CameraSnapshotListener listener) {
        Log.d(TAG, "Camera.takeSnapshot");

        android.hardware.Camera.Size mSize = camera.getNativeCamera().getParameters().getPreviewSize();
        final int nativeWidth = mSize.width;
        final int nativeHeight = mSize.height;

        camera.request().oneShotRequest().subscribe(new Action1<RxCameraData>() {
            @Override
            public void call(RxCameraData rxCameraData) {
                Log.d(TAG, "Camera.takeSnapshot call takeSnapshot");
                Log.d(TAG, "Camera.takeSnapshot data of length " + rxCameraData.cameraData.length);
                // convert to something normal than the weird camera format, and get proper capture square
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                YuvImage yuv = new YuvImage(rxCameraData.cameraData, ImageFormat.YUY2, nativeWidth, nativeHeight, null);

                int offsetA = (nativeWidth - captureCropWidth) / 2;
                int offsetB = (nativeHeight- captureCropHeight) / 2;
                int cropW = captureCropWidth;
                int cropH = captureCropHeight;
                yuv.compressToJpeg(new Rect(offsetA, offsetB, cropW+offsetA, cropH+offsetB), 100, out);

                byte[] bytes = out.toByteArray();
                listener.processCapturedJpeg(bytes);
            }
        });
    }

    public void startCapturingVideo() {
//        try {
//            recorder.prepare();
//            recorder.start();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    public void stopCapturingVideo() {
//        recorder.stop();
    }
}
