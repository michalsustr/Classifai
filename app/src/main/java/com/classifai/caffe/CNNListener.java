package com.classifai.caffe;

/**
 * Created by shiro on 2014/9/22.
 */
public interface CNNListener {
    void onRecognitionCompleted(CaffeResult result);
}
