package com.wisense.cadal;

import java.util.Arrays;

/**
 * Created by lucapernini on 12/03/15.
 */
public class Statistics
{
    float[] data;
    float size;

    public Statistics(float[] data)
    {
        this.data = data;
        size = data.length;
    }

    float getMean()
    {
        float sum = 0;
        for(double a : data)
            sum += a;
        return sum/size;
    }

    float getVariance()
    {
        float mean = getMean();
        float temp = 0;
        for(double a :data)
            temp += (mean-a)*(mean-a);
        return temp/(size-1); //USING varianza campionaria corretta
    }

    float getStdDev()
    {
        return (float) Math.sqrt(getVariance());
    }

    public float median()
    {
        float[] b = new float[data.length];
        System.arraycopy(data, 0, b, 0, b.length);
        Arrays.sort(b);

        if (data.length % 2 == 0)
        {
            return (float) ((b[(b.length / 2) - 1] + b[b.length / 2]) / 2.0);
        }
        else
        {
            return (float) b[b.length / 2];
        }
    }
}