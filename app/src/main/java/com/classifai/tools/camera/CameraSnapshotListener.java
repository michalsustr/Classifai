package com.classifai.tools.camera;

import android.graphics.Bitmap;

/**
 * Created by Michal Sustr [michal.sustr@gmail.com] on 4/15/16.
 */
public interface CameraSnapshotListener {
    public void processCapturedJpeg(Bitmap bitmap);
}
