package com.classifai.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.util.Log;

import com.ragnarok.rxcamera.RxCamera;
import com.ragnarok.rxcamera.RxCameraData;
import com.ragnarok.rxcamera.config.RxCameraConfig;
import com.ragnarok.rxcamera.config.RxCameraConfigChooser;
import com.ragnarok.rxcamera.request.Func;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

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

    public Camera(Context context, CroppedCameraPreview textureView) {
        this.cameraPreview = textureView;
        this.context = context;
    }

    public void openCamera() {
        RxCameraConfig config = RxCameraConfigChooser.obtain().
                useBackCamera().
                setAutoFocus(true).
                setPreferPreviewFrameRate(15, 30).
                setPreferPreviewSize(new Point(480, 456)).
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
                return rxCamera.bindTexture(cameraPreview.getTextureView(
                    cameraPreview.getWidth(), cameraPreview.getHeight(), cameraSize.width, cameraSize.height
                ));
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


    public void snapshot() {
        Log.d(LOG_TAG, "try snapshot");

        android.hardware.Camera.Size mSize = camera.getNativeCamera().getParameters().getPreviewSize();
        final int mWidth = mSize.width;
        final int mHeight = mSize.height;

        Log.d(LOG_TAG, "size of preview: "+mWidth+" "+mHeight);

        camera.request().oneShotRequest().subscribe(new Action1<RxCameraData>() {
            @Override
            public void call(RxCameraData rxCameraData) {
                Log.d(LOG_TAG, "call snapshot");
                Log.d(LOG_TAG, "data " + rxCameraData.cameraData.length);
                // pWidth and pHeight define the size of the preview Frame
                ByteArrayOutputStream out = new ByteArrayOutputStream();

                // Alter the second parameter of this to the actual format you are receiving
                YuvImage yuv = new YuvImage(rxCameraData.cameraData, ImageFormat.YUY2, mWidth, mHeight, null);

                // bWidth and bHeight define the size of the bitmap you wish the fill with the preview image
                yuv.compressToJpeg(new Rect(0, 0, mWidth, mHeight), 50, out);

                byte[] bytes = out.toByteArray();

                try {
                    FileOutputStream snapshot = new FileOutputStream("/storage/sdcard0/caffe/snapshot.jpg");
                    snapshot.write(bytes);
                    snapshot.close();
                    Log.d(LOG_TAG, "saved snapshot");
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });
    }

    public  void _snapshot() {
        Log.d(LOG_TAG, "try snapshot");
        camera.request().takePictureRequest(true, new Func() {
            @Override
            public void call() {
                Log.d(LOG_TAG, "call snapshot");
            }
        }, 480, 640, ImageFormat.JPEG).subscribe(new Action1<RxCameraData>() {
            @Override
            public void call(RxCameraData rxCameraData) {
                String path = "/storage/sdcard0/caffe/test.jpg";
                File file = new File(path);
                Bitmap bitmap = BitmapFactory.decodeByteArray(rxCameraData.cameraData, 0, rxCameraData.cameraData.length);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(),
                        rxCameraData.rotateMatrix, false);
                try {
                    file.createNewFile();
                    FileOutputStream fos = new FileOutputStream(file);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
