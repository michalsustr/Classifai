package com.classifai.tools.recognition;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Created by Michal Sustr [michal.sustr@gmail.com] on 4/8/16.
 */
public class RecognitionResult {
    private long executionTime;
    private float[] confidenceScore;
    private Integer[] sortedIndices;
    private String[] class2label;

    public RecognitionResult(float[] confidenceScore, String[] class2label) {
        this.confidenceScore = confidenceScore;
        this.class2label = class2label;

        FloatIndexComparator comparator = new FloatIndexComparator(confidenceScore);
        sortedIndices = comparator.createIndexArray();
        Arrays.sort(sortedIndices, comparator);
    }

    public void setExecutionTime(long executionTime) {
        this.executionTime = executionTime;
    }
    public long getExecutionTime() {
        return executionTime;
    }

    public Integer[] getTopKIndices(int k) {
        Integer[] idx = new Integer[k];
        for (int i = 0; i < k; i++) {
            idx[i] = sortedIndices[i];
        }
        return idx;
    }
    public float getScore(int index) {
        return confidenceScore[index];
    }
    public String getLabel(int index) {
        return class2label[index];
    }

    /**
     * @param index
     * @return null|Bitmap
     */
    public Bitmap getPreview(int index) {
        String preview = "/storage/sdcard0/caffe/preview/"+index+".jpg";
        if(new File(preview).exists()) {
            return BitmapFactory.decodeFile(preview);
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuilder stringBuffer = new StringBuilder();
        for (int i = 0; i< confidenceScore.length; i++) {
            stringBuffer.append(class2label[i])
                .append(": ").append(String.format("%.2f", confidenceScore[i])).append("; ");
        }
        return stringBuffer.toString();
    }

    public String top5toString() {
        StringBuilder stringBuffer = new StringBuilder();
        Integer[] top5idx = getTopKIndices(5);
        for (int i = 0; i< 5; i++) {
            stringBuffer.append(class2label[top5idx[i]])
                    .append(": ").append(String.format("%.2f", confidenceScore[top5idx[i]])).append("; ");
        }
        return stringBuffer.toString();
    }

    public Float getFPS() {
        return (float) (1000.0 / executionTime);
    }


    private class FloatIndexComparator implements Comparator<Integer>
    {
        private final float[] array;

        public FloatIndexComparator(float[] array)
        {
            this.array = array;
        }

        public Integer[] createIndexArray()
        {
            Integer[] indexes = new Integer[array.length];
            for (int i = 0; i < array.length; i++)
            {
                indexes[i] = i; // Autoboxing
            }
            return indexes;
        }

        @Override
        public int compare(Integer index1, Integer index2)
        {
            // Autounbox from Integer to int to use as array indexes
            if(array[index1] - array[index2] < 1e-20) return 0;
            return array[index1] < array[index2] ? 1 : -1;
        }
    }
}
