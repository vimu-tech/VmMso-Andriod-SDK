package com.vimu.aardemo;

import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.ShapeDrawable;
import android.util.Log;

import java.lang.reflect.Array;
import java.util.Arrays;

public class LineInfo {

    private static final String TAG = "LineInfo";

    public String LineName;
    public double YPosIndex;
    public int LineClr;

    public int PixelNum;
    public int[] Pixels;
    public double[] PixelsMinValue;
    public double[] PixelsMaxValue;

    public double MinValue;
    public double MaxValue;

    public final ShapeDrawable mDrawable = new ShapeDrawable();
    public final Path mPath = new Path();

    public Rect mRect = null;

    public LineInfo(String name, double pos_index, int clr, int pixel_num) {
        LineName = name;
        YPosIndex = pos_index;
        LineClr = clr;
        PixelNum = pixel_num;
        Pixels = new int[pixel_num];
        PixelsMinValue = new double[pixel_num];
        PixelsMaxValue = new double[pixel_num];
        MinValue = 0;
        MaxValue = 0;
        Arrays.fill(Pixels, -1);
    }

    public boolean UpdateDatas(double[] buffer, int length, double minv, double maxv){
        MinValue = minv;
        MaxValue = maxv;

        Arrays.fill(Pixels, -1);

        int pixe_index = 0;
        Pixels[pixe_index] = 0;
        PixelsMinValue[pixe_index]=buffer[0];
        PixelsMaxValue[pixe_index]=buffer[0];
        double step = (double)(PixelNum)/(double)length;
        for(int k=0; k<length; k++) {
            int new_pixe_index = (int)(step*k);

            if(new_pixe_index != Pixels[pixe_index]) {
                pixe_index++;
                Pixels[pixe_index] = new_pixe_index;
                PixelsMinValue[pixe_index]=buffer[k];
                PixelsMaxValue[pixe_index]=buffer[k];
            }
            else {
                PixelsMinValue[pixe_index]=Math.min(PixelsMinValue[pixe_index], buffer[k]);
                PixelsMaxValue[pixe_index]=Math.max(PixelsMaxValue[pixe_index], buffer[k]);
            }
        }

        //Log.d(TAG, String.format("PixelNum = %d pixe_index = %d ", PixelNum, pixe_index));
        return true;
    }
}
