package com.classifai.tools.camera;

/**
 * Created by Michal Sustr [michal.sustr@gmail.com] on 4/15/16.
 */
public interface CameraSnapshotListener {
    public void processCapturedJpeg(byte[] bytes);
}
