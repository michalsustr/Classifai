package com.classifai.camera;

import android.content.Context;
import android.graphics.Point;
import android.util.Log;
import android.view.TextureView;

import com.ragnarok.rxcamera.RxCamera;
import com.ragnarok.rxcamera.config.RxCameraConfig;
import com.ragnarok.rxcamera.config.RxCameraConfigChooser;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * Created by Michal Sustr [michal.sustr@gmail.com] on 4/8/16.
 */
public class Camera {
    private static final String LOG_TAG = "Camera";

    private final Context context;
    private final TextureView textureView;
    private RxCamera camera;

    public Camera(Context context, TextureView textureView) {
        this.textureView = textureView;
        this.context = context;
    }

    public void openCamera() {
        RxCameraConfig config = RxCameraConfigChooser.obtain().
                useBackCamera().
                setAutoFocus(true).
                setPreferPreviewFrameRate(15, 30).
                setPreferPreviewSize(new Point(456, 456)).
                setHandleSurfaceEvent(true).
                get();
        Log.d(LOG_TAG, "config: " + config);
        RxCamera.open(context, config).flatMap(new Func1<RxCamera, Observable<RxCamera>>() {
            @Override
            public Observable<RxCamera> call(RxCamera rxCamera) {
                Log.d(LOG_TAG, "isopen: " + rxCamera.isOpenCamera() + ", thread: " + Thread.currentThread());
                camera = rxCamera;
                return rxCamera.bindTexture(textureView);
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
}
