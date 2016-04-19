package com.classifai.ui;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.animation.DecelerateInterpolator;

import com.mikhaellopez.circularprogressbar.CircularProgressBar;

/**
 * Created by Michal Sustr [michal.sustr@gmail.com] on 4/18/16.
 */
public class CircleProgress extends CircularProgressBar {

    public CircleProgress(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    ObjectAnimator objectAnimator;
    @Override
    public void setProgressWithAnimation(float progress, int duration) {
        objectAnimator = ObjectAnimator.ofFloat(this, "progress", new float[]{progress});
        objectAnimator.setDuration((long)duration);
        objectAnimator.setInterpolator(new DecelerateInterpolator());
        objectAnimator.start();
    }

    public void animateProgress(int oneCycleDuration) {
        objectAnimator = ObjectAnimator.ofFloat(this, "progress", new float[]{100});
        objectAnimator.setDuration((long)oneCycleDuration);
        objectAnimator.setInterpolator(new DecelerateInterpolator());
        objectAnimator.setRepeatMode(ObjectAnimator.INFINITE);
        objectAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        objectAnimator.start();
    }

    public void stopAnimation() {
        objectAnimator.cancel();
        objectAnimator = null;
    }
}
