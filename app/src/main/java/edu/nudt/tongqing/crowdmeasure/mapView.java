package edu.nudt.tongqing.crowdmeasure;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

/**
 * Created by tongqing on 2016/10/24.
 */

public class mapView extends ImageView {

    private Bitmap mBitmap = null;
    private Bitmap mBitmapBK = null;
    private Paint mPaint;

    private float init_x = 0;
    private float init_y = 0;
    private long init_time;
    private float offsetThreshold = 20;
    private long intervalThreshold = 1000;

    private int locMark = 0;
    private boolean un_handle_flag = false;
    private boolean view_Lock = false;

    public mapView(Context context){
        super(context);
    }

    public mapView(Context context, AttributeSet attrs){
        super(context, attrs);

        // 初始化一个画笔，笔触宽度为5，颜色为红色
        mPaint = new Paint();
        mPaint.setStrokeWidth(10);
        mPaint.setColor(Color.RED);
    }

    public mapView(Context context, AttributeSet attrs, int defStyleAttr){
        super(context, attrs, defStyleAttr);
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    public void setImageBitmap(Bitmap bm){
        super.setImageBitmap(bm);
        mBitmap = bm;
        setVisibility(VISIBLE);
    }

    public boolean onTouchEvent(MotionEvent event) {
        //仅有加载背景图之后，且view未被锁定时（测量进行中）才响应点击事件
        if(mBitmap != null & !this.view_Lock) {
            //继承了View的onTouchEvent方法，直接监听点击事件
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                //当手指按下的时候
                init_x = event.getX();
                init_y = event.getY();
                init_time = event.getEventTime();
                //在action_down时返回true，view才会响应action_up
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_UP) {
                //当手指离开的时候
                if (isLongPress(event)) {
                    //未进行测量即进行下一次位置标定
                    //清除待测量标识，并加载上次标定之前的map
                    if(getunHandleFlag()) {
                        clearunHandleFlag(0);
                        locMark = locMark - 1;
                    }
                    //mBitmap = Bitmap.createBitmap(floor_plan_image.getWidth(),floor_plan_image.getHeight(),Bitmap.Config.ARGB_8888);
                    Bitmap mTempBitmap = mBitmap.copy(Bitmap.Config.ARGB_8888, true);
                    mBitmapBK = mBitmap.copy(Bitmap.Config.ARGB_8888, true);
                    Canvas mCanvas = new Canvas(mTempBitmap);
                    float ratio_X = (float) this.getWidth()/(float) mTempBitmap.getWidth();
                    float ratio_Y = (float) this.getHeight()/(float) mTempBitmap.getHeight();

                    //因为是按照宽度扩展的，所以使用宽度比例（ratio_X）作为位置点平移的比例
                    mCanvas.drawPoint(event.getX()/ratio_X, event.getY()/ratio_X, mPaint);
                    setImageBitmap(mTempBitmap);
                    setScaleType(ImageView.ScaleType.FIT_START);

                    un_handle_flag = true;
                    locMark = locMark + 1;
                }
            }
        }
        return super.onTouchEvent(event);
    }

    //判断是否为长按事件
    private boolean isLongPress(MotionEvent event){
        float offsetX = Math.abs(init_x - event.getX());
        float offsetY = Math.abs(init_y - event.getY());
        long intervalTime = event.getEventTime() - init_time;
        return offsetX < offsetThreshold & offsetY < offsetThreshold & intervalTime >= intervalThreshold;
    }

    public boolean getunHandleFlag(){
        return this.un_handle_flag;
    }

    public void clearunHandleFlag(int state){
        if(state == 1)
            this.un_handle_flag = false;
        //state=0表示测量无效，用备份的map替换包含当前位置点的map
        if(state == 0) {
            setImageBitmap(mBitmapBK);
            this.un_handle_flag = false;
        }
    }

    public void setViewLock(boolean lock){
        this.view_Lock = lock;
    }

    public int getCurrentLoc(){
        return this.locMark;
    }
}