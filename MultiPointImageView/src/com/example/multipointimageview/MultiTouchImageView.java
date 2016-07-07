package com.example.multipointimageview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageView;

public class MultiTouchImageView extends ImageView implements OnGlobalLayoutListener,OnTouchListener, OnScaleGestureListener{

    private static final String TAG = "MultiTouchImageView";

    private boolean isOnce =false;
    
    private int width;
    
    private int height;
    
    private int drawWidth;
    
    private int drawHeight;
    
    private Matrix matrix;
    
    private float initScale;
    
    private ScaleGestureDetector  scaleGestureDetecor;
    
    private GestureDetector gestureDetector;
    
    private float maxScale;
    
    private float minScale;
    
    
    //上一次手指在屏幕上的数量
    private int lastPointCount =0;
    
    //上一次的坐标X
    private float lastPointX;
    
    //上一次的坐标Y
    private float lastPointY;
    
    //移动的最小距离
    private float touchSlop;
    
    
    private static final  float SHRINK = 0.97f;
    
    private static final  float AMPLIFY =1.05f;
    
    private boolean isAutoScale;
    
    //判断上一次移动过程中中心点的坐标是否有所改变。(移动过程中手指是否有变化，从3个手指变成2个，或者从2个手指变成3个)
    private boolean  isLastPointChange;
    
    @SuppressWarnings("deprecation")
    public MultiTouchImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnTouchListener(this);
        this.setScaleType(ScaleType.MATRIX);
        matrix = new Matrix();
        scaleGestureDetecor = new ScaleGestureDetector(context, this);
        gestureDetector = new GestureDetector(context,new GestureDetector.SimpleOnGestureListener(){

            @Override
            public boolean onDoubleTapEvent(MotionEvent e) {
                Log.d(TAG, "onDoubleTapEvent");
                if(isAutoScale)
                    return true;
                switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    float currScale  =  getScale();
                    if(currScale<minScale){
                        //放大
                        postDelayed(new ZoomAnimation(minScale,e.getX(), e.getY()),16);
                        isAutoScale = true;
                    }else{
                        //缩小
                        postDelayed(new ZoomAnimation(initScale,e.getX(), e.getY()),16);
                        isAutoScale = true;
                    }
                    break;
                default:
                    break;
                }
                return true;
            }
        
        });
        touchSlop = ViewConfiguration.getTouchSlop();
    }

    public MultiTouchImageView(Context context) {
        this(context, null);
    }
    
    
     class  ZoomAnimation implements Runnable{

        
        private float  targetScale;
        
        
        private float centerX ;
        
        private float centerY;
        
        private float tempScale;
        public ZoomAnimation(float  targetScale,float centerX,float centerY){
            this.targetScale = targetScale;
            this.centerX = centerX;
            this.centerY = centerY;
            
            if(getScale()>minScale){
                tempScale = SHRINK;
            }else{
                tempScale = AMPLIFY;
            }
        }
        
        @Override
        public void run() {
            float scale = getScale();
            setMulImageScale(tempScale,centerX,centerY);
            if((tempScale>1.0f&&scale<targetScale)||(tempScale<1.0f&&scale>targetScale)){
                postDelayed(this, 16);
            }else{
                 setMulImageScale(targetScale/scale,centerX,centerY);
                 isAutoScale = false;
            }
        }
    }
    
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }
    
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnGlobalLayoutListener(this);
    }
    
    @SuppressWarnings("deprecation")
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getViewTreeObserver().removeGlobalOnLayoutListener(this);
    }

    /**
     * 获取缩放的值
     * @return
     */
    private float getScale(){
        float [] values = new float[9];
        matrix.getValues(values);
        return values[Matrix.MSCALE_X];
    }
    
    /**
     * View的布局已经完成后 才会回调该方法。
     */
    @Override
    public void onGlobalLayout() {
        Log.d(TAG, "onGlobalLayout   +++");
        if(!isOnce){
            //获取imageview控件的宽度。
            width = getWidth();
            height = getHeight();
            //获取图片自身的宽高。
            Drawable d = getDrawable();
            int centerDeltaX =0;
            int centerDeltaY =0;
            float scale = 1.0f;
            if(d==null){
                return;
            }
            drawWidth = d.getIntrinsicWidth();
            drawHeight = d.getIntrinsicHeight();
            
            if(drawWidth>=width){
                if(drawHeight<height){
                    scale = width*1.0f/drawWidth;
                }
            }
            if(drawHeight>=height){
                if(drawWidth<width){
                    scale = height*1.0f/drawHeight;
                }
            }
            if((drawWidth>width&&drawHeight>height)||(drawWidth<width&&drawHeight<height)){
                scale = Math.min(width*1.0f/drawWidth, height*1.0f/drawHeight);
            }
            
            initScale = scale;
            minScale = 2*initScale;
            maxScale = 4*initScale;
            
            
            centerDeltaX = (getWidth()-drawWidth)/2;
            centerDeltaY = (getHeight()-drawHeight)/2;
            
            matrix.postTranslate(centerDeltaX, centerDeltaY);
            matrix.postScale(scale, scale, width/2,height/2);
            //在下面对方法对ImageView的drawable进行matrix设置。
            setImageMatrix(matrix);
            isOnce = true;
        }
    }
    /**
     * 获得图片放大以后的宽和高。以及各点的坐标。
     */
    private RectF getMatrixF(){
        Matrix matrix = this.matrix;
        RectF rectF = new RectF();
        Drawable d = getDrawable();
        if(d!=null){
            rectF.set(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
            matrix.mapRect(rectF);
        }
        return rectF;
    }
    
    /**
     * 是否可以移动。
     * @param dx
     * @param dy
     * @return
     */
    private boolean isCanMove(float dx,float dy){
        float moveSlop = (int) Math.sqrt(dx*dx+dy*dy);
        return moveSlop>touchSlop;
    }
    
    /**
     * 移动的时候防止图片的四周产生白边。
     */
    private void applyPicFillViewWhenTranslate(){
        RectF  rectF = getMatrixF();
        int width  = getWidth();
        int height = getHeight();
        float dx =0;
        float dy =0;
        if(rectF.width()>width){
            if(rectF.left>0){
                dx = -rectF.left;
            }
            if(rectF.right<width){
                dx = width- rectF.right;
            }
        }
        if(rectF.height()>height){
            if(rectF.top>0){
                dy = -rectF.top;
            }
            if(rectF.bottom<height){
               dy = height-rectF.bottom; 
            }
        }
        matrix.postTranslate(dx, dy);
    }
    
    
    /**
     * 缩放的时候防止图片的四周产生白边。
     */
    private void applyPicToCenterWhenScale(){
        RectF  rectF = getMatrixF();
        //通过mapRect可以把matrix转换成坐标。
        //获取放大缩小后的宽度和高度。
        int width  = getWidth();
        int height = getHeight();
        float deltaX =0;
        float deltaY =0;
        if(rectF.width()>=width){
            if(rectF.left>0){
                deltaX = -rectF.left;
            }
            if(rectF.right<width){
                deltaX = width-rectF.right;
            }
        }else{
             //图片缩放后的宽度小于控件的宽度。
             deltaX = width/2-rectF.right+rectF.width()/2;
        }
        if(rectF.height()>=height){
            if(rectF.top>0){
                deltaY = -rectF.top;
            }
            if(rectF.bottom<height){
                deltaY = height-rectF.bottom;
            }
        }else{
           //图片缩放后的高度小于控件的高度。
            deltaY =height/2-rectF.bottom+rectF.height()/2;
        }
        matrix.postTranslate(deltaX,deltaY);
    }

    
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if(getDrawable()==null){
            return false;
        }
        if(gestureDetector.onTouchEvent(event))
            return true;
        scaleGestureDetecor.onTouchEvent(event);
        int pointCount = event.getPointerCount();
        float x =0;
        float y =0;
        for(int i=0;i<pointCount;i++){
            x+=event.getX(i);
            y+=event.getY(i);
        }
        //计算中心点的位置。
        x = x/pointCount;
        y = y/pointCount;
        //当上一次的和这一次的不相同时，表示手指在移动的过程中数量有变化。
        if(lastPointCount!=pointCount){
            isLastPointChange = true;
            //记录此位置。
            lastPointX = x;
            lastPointY = y;
        }
        lastPointCount = pointCount;
        switch(event.getAction()){
        case MotionEvent.ACTION_DOWN:
            if(!isLastPointChange){
                lastPointX = x;
                lastPointY = y;
            }
            break;
        case MotionEvent.ACTION_MOVE:
            float dx = x-lastPointX;
            float dy = y-lastPointY;
            boolean isCanDrag = isCanMove(dx, dy);
            if(isCanDrag){
                RectF rectF = getMatrixF();
                if(rectF.width()<=getWidth()){
                    dx =0;
                }
                if(rectF.height()<=getHeight()){
                    dy =0;
                }
                setMulImageTranslate(dx, dy);
                lastPointX = x;
                lastPointY = y;
            }
            break;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
            //还原所有的值。
            lastPointCount = 0;
            lastPointX = 0;
            lastPointY = 0;
            isLastPointChange = false;
            break;
        }
        return true;
    }

    private void setMulImageTranslate(float dx, float dy) {
        matrix.postTranslate(dx, dy);
        //边界检查，拖动的时候使得图片始终充满整个相框，而不产生白边。
        applyPicFillViewWhenTranslate();
        setImageMatrix(matrix);
    }

    
    /**
     * 多点缩放的回调函数。
     */
    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        float scaleFactor = detector.getScaleFactor();
        float scale  =  getScale();
        
        if(getDrawable()==null){
            return true;
        }
        //如果scale的值还小于最大缩放值并且正在方法或者scale的值大于最小缩放值的并且正在缩小。
        if((scale<maxScale&&scaleFactor>1.0f)||(scale>minScale&&scaleFactor<1.0f)){
            //如果缩放的值比最小的小，就让其等于最小。
            if(scale*scaleFactor<minScale){
                scaleFactor = minScale/scale;
            }
            //如果缩放的值比最大的大，就让其等于最大。
            if(scale*scaleFactor>maxScale){
                scaleFactor = maxScale/scale;
            }
            setMulImageScale(scaleFactor,detector.getFocusX(),detector.getFocusY());
        }
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        return   true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        
    }

    private void setMulImageScale(float scale,float centerX,float centerY) {
        matrix.preScale(scale, scale, centerX, centerY);
        //因为放大之后再缩小会和周围产生白边,所以利用下面的函数修正这个bug。
        applyPicToCenterWhenScale();
        setImageMatrix(matrix);
    }
}
