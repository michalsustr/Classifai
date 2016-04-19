package com.classifai.tools.recognition;

/**
 * Created by shiro on 2014/9/22.
 */
public interface RecognitionListener {
    void onRecognitionStart();
    void onRecognitionCompleted(RecognitionResult result);
    void onRecognitionCanceled();
}
