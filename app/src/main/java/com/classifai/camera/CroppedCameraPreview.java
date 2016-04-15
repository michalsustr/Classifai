package com.classifai.camera;

import android.content.Context;
import android.util.Log;
import android.view.TextureView;
import android.view.ViewGroup;

/**
 * Created by Michal Sustr [michal.sustr@gmail.com] on 4/15/16.
 */
public class CroppedCameraPreview extends ViewGroup {
    private static final String LOG_TAG = "CroppedCameraPreview";
    private TextureView textureView;
    private int croppedWidth = 656;
    private int croppedHeight = 656;
    private int actualPreviewWidth = 640;
    private int actualPreviewHeight = 480;

    public CroppedCameraPreview( Context context ) {
        super( context );

        textureView = new TextureView(context);
    }
    @Override
    protected void onMeasure( int widthMeasureSpec, int heightMeasureSpec ) {
        setMeasuredDimension( croppedWidth, croppedHeight );
    }
    @Override
    protected void onLayout( boolean changed, int l, int t, int r, int b ) {
        if ( textureView != null ) {
            textureView.layout( 0, 0, actualPreviewWidth, actualPreviewHeight );
        }
    }

    public TextureView getTextureView(int croppedWidth, int croppedHeight, int actualPreviewWidth, int actualPreviewHeight) {
//        this.croppedWidth = croppedWidth;
//        this.croppedHeight = croppedHeight;
//        this.actualPreviewWidth = actualPreviewWidth;
//        this.actualPreviewHeight = actualPreviewHeight;

        Log.d(LOG_TAG, "initialize texture view with "+
            "actualPreviewWidth=" + actualPreviewWidth +
            ", actualPreviewHeight=" + actualPreviewHeight +
            ", croppedWidth=" + croppedWidth +
            ", croppedHeight=" + croppedHeight
        );
        return textureView;
    }

    public int getActualPreviewHeight() {
        return actualPreviewHeight;
    }
    public void setActualPreviewHeight(int actualPreviewHeight) {
        this.actualPreviewHeight = actualPreviewHeight;
    }
    public int getActualPreviewWidth() {
        return actualPreviewWidth;
    }
    public void setActualPreviewWidth(int actualPreviewWidth) {
        this.actualPreviewWidth = actualPreviewWidth;
    }
    public int getCroppedHeight() {
        return croppedHeight;
    }
    public void setCroppedHeight(int croppedHeight) {
        this.croppedHeight = croppedHeight;
    }
    public int getCroppedWidth() {
        return croppedWidth;
    }
    public void setCroppedWidth(int croppedWidth) {
        this.croppedWidth = croppedWidth;
    }
}
