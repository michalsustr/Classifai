package com.classifai.camera;

import android.content.Context;
import android.util.AttributeSet;
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
    private int actualPreviewWidth = 600;
    private int actualPreviewHeight = 800;

    public CroppedCameraPreview( Context context ) {
        super( context );
        createTextureView();
    }
    public CroppedCameraPreview(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        createTextureView();
    }
    public CroppedCameraPreview(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        createTextureView();
    }

    private void createTextureView() {
        textureView = new TextureView(this.getContext());
        this.addView(textureView);
    }

    /**
     * Any layout manager that doesn't scroll will want this.
     */
    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }


    @Override
    protected void onMeasure( int widthMeasureSpec, int heightMeasureSpec ) {
        setMeasuredDimension( croppedWidth, croppedHeight );
    }
    @Override
    protected void onLayout( boolean changed, int l, int t, int r, int b ) {
        if ( textureView != null ) {
            int offsetA = (actualPreviewWidth - croppedWidth) / 2;
            int offsetB = (actualPreviewHeight- croppedHeight) / 2;
            textureView.layout(
                -offsetA,
                -offsetB,
                offsetA + croppedWidth,
                offsetB + croppedHeight
            );
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
}
