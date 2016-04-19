package com.classifai.tools.camera;

import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;
import android.view.ViewGroup;

import com.classifai.R;

/**
 * Created by Michal Sustr [michal.sustr@gmail.com] on 4/15/16.
 */
public class CroppedCameraPreview extends ViewGroup {
    private TextureView textureView;
    private int cameraDisplayWidth;
    private int cameraDisplayHeight;
    private int cameraNativeWidth;
    private int cameraNativeHeight;

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
        // yes, the width and height are mixed up on purpose :)
        // my phone swaps the dimensions for some reason :/
        // TODO: find out how this is done properly
        cameraNativeWidth = getResources().getInteger(R.integer.cameraNativeHeight);
        cameraNativeHeight = getResources().getInteger(R.integer.cameraNativeWidth);
        cameraDisplayWidth = getResources().getInteger(R.integer.cameraDisplayWidth);
        cameraDisplayHeight = getResources().getInteger(R.integer.cameraDisplayHeight);

        textureView = new TextureView(getContext());
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
        setMeasuredDimension(cameraDisplayWidth, cameraDisplayHeight);
    }
    @Override
    protected void onLayout( boolean changed, int l, int t, int r, int b ) {
        if ( textureView != null ) {
            int offsetA = (cameraNativeWidth - cameraDisplayWidth) / 2;
            int offsetB = (cameraNativeHeight - cameraDisplayHeight) / 2;
            textureView.layout(
                -offsetA,
                -offsetB,
                offsetA + cameraDisplayWidth,
                offsetB + cameraDisplayHeight
            );
        }
    }

    public TextureView getTextureView() {
        return textureView;
    }
}
