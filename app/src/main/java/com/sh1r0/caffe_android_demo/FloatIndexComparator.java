package com.sh1r0.caffe_android_demo;

import java.util.Comparator;

public class FloatIndexComparator implements Comparator<Integer>
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