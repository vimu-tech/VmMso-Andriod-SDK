package com.vimu.aardemo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.LinkedList;

public class ScopeView extends View {

    private static final String TAG = "ScopeView";

    protected float mDrawLine1dp = 1;
    private boolean mIsLayout = false;
    private Rect m_clientRect = new Rect();
    private Rect m_plotRect = new Rect();
    private int grid_resolution_x = 0;// 网格 X、Y 方向间距
    private int grid_resolution_y = 0;
    private int mGridSamll1 = 1;
    private int mGridLarge1 = 2;
    private int mSmallAxisCount = 5; //小刻度的数量
    private static final int mGridCount = 10;
    private int grid_count_x = 10; //网格X轴方向的线数
    private int grid_count_y = 10;  //网格Y轴方向的线数

    private final ShapeDrawable mDrawableGrid = new ShapeDrawable();
    private final Path mPathGrid = new Path();

    LinkedList<LineInfo> mLineList = new LinkedList<>(); //记录axis,不产生

    public ScopeView(Context context) {
        super(context);
        init(context);
    }

    public ScopeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ScopeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public ScopeView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        mDrawLine1dp = getResources().getDimension(R.dimen.draw_line_1dp);

        Log.d(TAG, "init");
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private void initDrawable(ShapeDrawable drawable, Path path, Rect rt) {
        drawable.setShape(new PathShape(path, rt.width(), rt.height()));
        drawable.setBounds(rt);

        setLayerType(LAYER_TYPE_SOFTWARE, drawable.getPaint());
    }

    private void initGrid(ShapeDrawable drawable, Path path, Rect rt) {
        path.rewind();

        //画边框
        path.addRect(0, 0, rt.width(), rt.height(), Path.Direction.CW);

        // 网格竖线
        float line_large = mDrawLine1dp;
        for (int l = 0; l <= grid_count_x; l++) {
            int x = l * grid_resolution_x;
            //画大步进线
            //画小步进线用到和确定字体的大小
            int smallaxis = grid_resolution_x / mSmallAxisCount;
            for (int i = 0; i <= grid_count_y; i++) {
                int y = i * grid_resolution_x;
                if (l > 0 && l < grid_count_x) {
                    path.moveTo((l == grid_count_x / 2) || (i == grid_count_y / 2) ? x - line_large * mGridLarge1 : x - line_large, y);
                    path.lineTo((l == grid_count_x / 2) || (i == grid_count_y / 2) ? x + line_large * mGridLarge1 : x + line_large, y);
                }

                // 画小步进线
                if (y < rt.height()) {
                    if (smallaxis <= 0) break;  //防止因图太小出现死循环
                    for (int j = 1; j < mSmallAxisCount; j++) {
                        path.moveTo(l == grid_count_x / 2 ? x - line_large * mGridSamll1 : x - line_large, y + smallaxis * j);
                        path.lineTo(l == grid_count_x / 2 ? x + line_large * mGridSamll1 : x + line_large, y + smallaxis * j);
                    }
                }
            }
        }

        // 网格横线
        for (int l = 0; l <= grid_count_y; l++) {
            float y = l * grid_resolution_y;
            if (y > rt.height() + 1)
                continue;

            //画小步进线用到
            int smallaxis = grid_resolution_x / mSmallAxisCount;
            //画大步进线
            for (int i = 0; i <= grid_count_x; i++) //为了消除启动网格移动后最后一个刻度超出界限
            {
                int x = i * grid_resolution_x;
                if (l > 0 && l < grid_count_y && (x >= 0)) {
                    path.moveTo((int) x, (l == grid_count_y / 2) || (i == grid_count_x / 2) ? y - line_large * mGridLarge1 : y - line_large);
                    path.lineTo((int) x, (l == grid_count_y / 2) || (i == grid_count_x / 2) ? y + line_large * mGridLarge1 : y + line_large);
                }
                //画小步进线
                if (i != grid_count_x + 1) {
                    if (smallaxis <= 0) break;  //防止因图太小出现死循环
                    for (int j = 0; j < mSmallAxisCount; j++) {
                        if (x + smallaxis * j < 0) continue;
                        if (x + smallaxis * j > rt.width()) break;
                        path.moveTo(x + smallaxis * j, l == grid_count_y / 2 ? y - line_large * mGridSamll1 : y - line_large);
                        path.lineTo(x + smallaxis * j, l == grid_count_y / 2 ? y + line_large * mGridSamll1 : y + line_large);
                    }
                }
            }
        }

        drawable.setShape(new PathShape(path, rt.width(), rt.height()));
        drawable.getPaint().setStyle(Paint.Style.STROKE);
        drawable.getPaint().setStrokeWidth(mDrawLine1dp);
        drawable.setBounds(rt);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        m_clientRect.left = l;
        m_clientRect.top = t;
        m_clientRect.right = r;
        m_clientRect.bottom = b;

        m_plotRect.left = m_clientRect.left;
        m_plotRect.top = m_clientRect.top;
        m_plotRect.right = m_clientRect.right;
        m_plotRect.bottom = m_clientRect.bottom;

        int mGridResolution = (int) (Math.min(m_plotRect.width(), m_plotRect.height()) / mGridCount);
        grid_resolution_y = grid_resolution_x = mGridResolution;
        grid_count_x = (int) (m_plotRect.width() / mGridResolution) / 2 * 2;
        grid_count_y = (int) (m_plotRect.height() / mGridResolution) / 2 * 2;

        int center_x = m_plotRect.centerX();
        int center_y = m_plotRect.centerY();
        m_plotRect.left = center_x - grid_resolution_x * grid_count_x / 2;
        m_plotRect.right = center_x + grid_resolution_x * grid_count_x / 2;
        int height = grid_resolution_y * grid_count_y;
        m_plotRect.top = center_y - height/2;
        m_plotRect.bottom = center_y + height/2;

        initGrid(mDrawableGrid, mPathGrid, m_plotRect);

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
        DrawBackgroud(canvas);
        DrawGrid(canvas);
        for(LineInfo info : mLineList) {
            UpdateLine(canvas, info);
        }
        super.onDraw(canvas);
    }

    boolean DrawBackgroud(Canvas canvas) {
        Paint p = new Paint();
        p.setColor(getResources().getColor(R.color.colorPlotBack));
        canvas.drawRect(m_clientRect, p);
        p.setColor(getResources().getColor(R.color.colorPlotBack));
        canvas.drawRect(m_plotRect, p);
        return true;
    }

    boolean DrawGrid(Canvas canvas) {
        mDrawableGrid.getPaint().setColor(getResources().getColor(R.color.colorGrid));
        mDrawableGrid.draw(canvas);
        return true;
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

    boolean AddLine(String name, double pos_index, int clr){
        for(LineInfo info : mLineList) {
            if(info.LineName.equals(name)) {
                mLineList.remove(info);
                break;
            }
        }
        LineInfo info = new LineInfo(name, pos_index, clr, m_plotRect.width());
        info.mRect = new Rect(m_plotRect.left, (int)(m_plotRect.top + (info.YPosIndex-1)*m_plotRect.height()/10),
                m_plotRect.right, (int)(m_plotRect.top + (info.YPosIndex+1)*m_plotRect.height()/10));
        initDrawable(info.mDrawable, info.mPath, info.mRect);
        mLineList.add(info);
        return true;
    }

    boolean UpdateDatas(String name, double[] buffer, int length, double minv, double maxv){
        for(LineInfo info : mLineList) {
            if(info.LineName.equals(name)) {
                info.UpdateDatas(buffer, length, minv, maxv);
                break;
            }
        }
        return true;
    }

    // 重绘窗口
    public void Redraw() {
        invalidate();
    }
}
