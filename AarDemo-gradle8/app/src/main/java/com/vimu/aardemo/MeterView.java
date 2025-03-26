package com.vimu.aardemo;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.LinkedList;

public class MeterView extends View {

    private static final String TAG = "MeterView";

    protected float mDrawLine1dp = 1;
    private boolean mIsLayout = false;
    private Rect m_clientRect = new Rect();
    private Rect m_plotRect = new Rect();

    double mTextValue=0;
    String mText = new String("000");
    String mTop = new String("DC");
    String mBottom = new String("V");

    public MeterView(Context context) {
        super(context);
        init(context);
    }

    public MeterView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MeterView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public MeterView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        Log.d(TAG, "init");
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        m_clientRect.left = 0;
        m_clientRect.top = 0;
        m_clientRect.right = r-l;
        m_clientRect.bottom = b-t;

        m_plotRect.left = m_clientRect.left;
        m_plotRect.top = m_clientRect.top;
        m_plotRect.right = m_clientRect.right - m_clientRect.width()/3;
        m_plotRect.bottom = m_clientRect.bottom;

        Log.d(TAG,"onLayout: " + m_clientRect);
        mIsLayout = true;
    }

    public boolean isLayout(){return mIsLayout;}

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        Log.d(TAG,"onSizeChanged: " +"w "+w + " h "+h + " oldw "+ oldw + " oldh "+ oldh);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        Resources myResources = getResources();
        float SystemFontSize = myResources.getDimension(R.dimen.osc_font_size);
        mDrawLine1dp = myResources.getDimension(R.dimen.draw_line_1dp);

        //初始化画笔
        Paint paint = new Paint();
        paint.setStrokeWidth(mDrawLine1dp);//设置画笔宽度

        paint.setColor(getResources().getColor(R.color.colorPlotBack));
        canvas.drawRect(m_clientRect, paint);

        paint.setTextSize(SystemFontSize*3); //设置文字大小
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.LEFT); //设置文字对齐方式

        Rect bounds = new Rect();
        paint.getTextBounds(mText, 0, mText.length(), bounds);
        canvas.drawText(mText, m_plotRect.centerX()-bounds.width()/2, m_plotRect.centerY()+bounds.height()/2, paint);

        paint.setTextSize(SystemFontSize*2); //设置文字大小
        paint.setTextAlign(Paint.Align.RIGHT);
        paint.getTextBounds(mTop, 0, mTop.length(), bounds);
        canvas.drawText(mTop, m_plotRect.right+bounds.width(), m_plotRect.centerY()-mDrawLine1dp*4, paint);

        paint.getTextBounds(mBottom, 0, mBottom.length(), bounds);
        canvas.drawText(mBottom, m_plotRect.right+bounds.width(), m_plotRect.centerY()+bounds.height()+mDrawLine1dp*4, paint);
        super.onDraw(canvas);
    }

    boolean UpdateLine(Canvas canvas, LineInfo info){
        double nYRange = info.MaxValue - info.MinValue;
        //小于1V的按照1V范围绘制
        //If it is less than 1V, it will be plotted in the 1V range
        if(nYRange<1.0)
            nYRange = 1.0;

        //Log.d(TAG,"UpdateLine: " + info.LineName + " rect " + info.mRect);
        info.mPath.rewind();

        int index = 0;
        for (int i = 0; i < info.PixelNum; i++) {
            if (info.Pixels[i] < 0)
                continue;
            //Log.d(TAG, String.format("[%d]=%d     min=%2f max=%2f     %2f", i, info.Pixels[i], info.PixelsMinValue[i], info.PixelsMaxValue[i],
             //       (float) (info.mRect.top + (info.MaxValue - info.PixelsMinValue[i]) * info.mRect.height() / nYRange)));
            if (index == 0)
                info.mPath.moveTo(info.Pixels[i], (float) ((info.MaxValue - info.PixelsMinValue[i]) * info.mRect.height() / nYRange));
            else
                info.mPath.lineTo(info.Pixels[i], (float) ((info.MaxValue - info.PixelsMinValue[i]) * info.mRect.height() / nYRange));
            index++;

            if (info.PixelsMinValue[i] != info.PixelsMaxValue[i]) {
                info.mPath.lineTo(info.Pixels[i], (float) ((info.MaxValue - info.PixelsMaxValue[i]) * info.mRect.height() / nYRange));
                index++;
            }
        }

        info.mDrawable.getPaint().setColor(info.LineClr);
        info.mDrawable.getPaint().setStyle(Paint.Style.STROKE);
        info.mDrawable.getPaint().clearShadowLayer();
        info.mDrawable.getPaint().setStrokeWidth(mDrawLine1dp);

        canvas.save();
        canvas.clipRect(info.mRect);
        info.mDrawable.draw(canvas);
        canvas.restore();
        return true;
    }

    boolean UpdateDatas(double text_value, String text, String top, String bottom, boolean is_bar, boolean bar_sign, int bar_value){
        mTextValue = text_value;
        mText = text;
        mTop = top;
        mBottom = bottom;
        return true;
    }

    // 重绘窗口
    public void Redraw() {
        invalidate();
    }
}
