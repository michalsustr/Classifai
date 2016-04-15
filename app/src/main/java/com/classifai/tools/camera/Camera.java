package com.classifai.tools.camera;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.YuvImage;
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
    private static final String LOG_TAG = "RxCamera";

    private final Context context;
    private final CroppedCameraPreview cameraPreview;
    private RxCamera camera;
    private android.hardware.Camera.Size cameraSize;
    private int cameraMinFPS;
    private int cameraMaxFPS;


    private int cameraNativeWidth;
    private int cameraNativeHeight;
    private int cameraDisplayWidth;
    private int cameraDisplayHeight;
    private int captureCropWidth;
    private int captureCropHeight;
    private final int captureSaveWidth;
    private final int captureSaveHeight;

    public Camera(Context context, CroppedCameraPreview textureView) {
        this.cameraPreview = textureView;
        this.context = context;

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
        Log.d(LOG_TAG, "config: " + config);

        RxCamera.open(context, config).flatMap(new Func1<RxCamera, Observable<RxCamera>>() {
            @Override
            public Observable<RxCamera> call(RxCamera rxCamera) {
                Log.d(LOG_TAG, "isopen: " + rxCamera.isOpenCamera() + ", thread: " + Thread.currentThread());
                camera = rxCamera;

                cameraSize = camera.getNativeCamera().getParameters().getPreviewSize();
                return rxCamera.bindTexture(cameraPreview.getTextureView());
//                return rxCamera.bindTexture(cameraPreview);
            }
        }).flatMap(new Func1<RxCamera, Observable<RxCamera>>() {
            @Override
            public Observable<RxCamera> call(RxCamera rxCamera) {
                Log.d(LOG_TAG, "isbindsurface: " + rxCamera.isBindSurface() + ", thread: " + Thread.currentThread());
                return rxCamera.startPreview();
            }
        }).observeOn(AndroidSchedulers.mainThread()).subscribe(new Subscriber<RxCamera>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
                Log.e(LOG_TAG, "open camera error: " + e.getMessage());
            }

            @Override
            public void onNext(final RxCamera rxCamera) {
                camera = rxCamera;
                Log.d(LOG_TAG, "open camera success: " + camera);
            }
        });
    }

    public void closeCamera() {
        if (camera != null) {
            camera.closeCameraWithResult().subscribe(new Action1<Boolean>() {
                @Override
                public void call(Boolean aBoolean) {
                    Log.d(LOG_TAG, "close camera finished, success: " + aBoolean);
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
        Log.d(LOG_TAG, "try takeSnapshot");

        android.hardware.Camera.Size mSize = camera.getNativeCamera().getParameters().getPreviewSize();
        final int nativeWidth = mSize.width;
        final int nativeHeight = mSize.height;

        camera.request().oneShotRequest().subscribe(new Action1<RxCameraData>() {
            @Override
            public void call(RxCameraData rxCameraData) {
                Log.d(LOG_TAG, "call takeSnapshot");
                Log.d(LOG_TAG, "data of length " + rxCameraData.cameraData.length);
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
}
