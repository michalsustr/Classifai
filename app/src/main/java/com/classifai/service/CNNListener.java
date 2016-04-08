package com.classifai.service;

/**
 * Created by shiro on 2014/9/22.
 */
public interface CNNListener {
    void onRecognitionCompleted(CaffeResult result);
}
