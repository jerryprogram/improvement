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
    
    
    //��һ����ָ����Ļ�ϵ�����
    private int lastPointCount =0;
    
    //��һ�ε�����X
    private float lastPointX;
    
    //��һ�ε�����Y
    private float lastPointY;
    
    //�ƶ�����С����
    private float touchSlop;
    
    
    private static final  float SHRINK = 0.97f;
    
    private static final  float AMPLIFY =1.05f;
    
    private boolean isAutoScale;
    
    //�ж���һ���ƶ����������ĵ�������Ƿ������ı䡣(�ƶ���������ָ�Ƿ��б仯����3����ָ���2�������ߴ�2����ָ���3��)
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
                        //�Ŵ�
                        postDelayed(new ZoomAnimation(minScale,e.getX(), e.getY()),16);
                        isAutoScale = true;
                    }else{
                        //��С
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
     * ��ȡ���ŵ�ֵ
     * @return
     */
    private float getScale(){
        float [] values = new float[9];
        matrix.getValues(values);
        return values[Matrix.MSCALE_X];
    }
    
    /**
     * View�Ĳ����Ѿ���ɺ� �Ż�ص��÷�����
     */
    @Override
    public void onGlobalLayout() {
        Log.d(TAG, "onGlobalLayout   +++");
        if(!isOnce){
            //��ȡimageview�ؼ��Ŀ�ȡ�
            width = getWidth();
            height = getHeight();
            //��ȡͼƬ����Ŀ�ߡ�
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
            //������Է�����ImageView��drawable����matrix���á�
            setImageMatrix(matrix);
            isOnce = true;
        }
    }
    /**
     * ���ͼƬ�Ŵ��Ժ�Ŀ�͸ߡ��Լ���������ꡣ
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
     * �Ƿ�����ƶ���
     * @param dx
     * @param dy
     * @return
     */
    private boolean isCanMove(float dx,float dy){
        float moveSlop = (int) Math.sqrt(dx*dx+dy*dy);
        return moveSlop>touchSlop;
    }
    
    /**
     * �ƶ���ʱ���ֹͼƬ�����ܲ����ױߡ�
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
     * ���ŵ�ʱ���ֹͼƬ�����ܲ����ױߡ�
     */
    private void applyPicToCenterWhenScale(){
        RectF  rectF = getMatrixF();
        //ͨ��mapRect���԰�matrixת�������ꡣ
        //��ȡ�Ŵ���С��Ŀ�Ⱥ͸߶ȡ�
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
             //ͼƬ���ź�Ŀ��С�ڿؼ��Ŀ�ȡ�
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
           //ͼƬ���ź�ĸ߶�С�ڿؼ��ĸ߶ȡ�
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
        //�������ĵ��λ�á�
        x = x/pointCount;
        y = y/pointCount;
        //����һ�εĺ���һ�εĲ���ͬʱ����ʾ��ָ���ƶ��Ĺ����������б仯��
        if(lastPointCount!=pointCount){
            isLastPointChange = true;
            //��¼��λ�á�
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
            //��ԭ���е�ֵ��
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
        //�߽��飬�϶���ʱ��ʹ��ͼƬʼ�ճ���������򣬶��������ױߡ�
        applyPicFillViewWhenTranslate();
        setImageMatrix(matrix);
    }

    
    /**
     * ������ŵĻص�������
     */
    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        float scaleFactor = detector.getScaleFactor();
        float scale  =  getScale();
        
        if(getDrawable()==null){
            return true;
        }
        //���scale��ֵ��С���������ֵ�������ڷ�������scale��ֵ������С����ֵ�Ĳ���������С��
        if((scale<maxScale&&scaleFactor>1.0f)||(scale>minScale&&scaleFactor<1.0f)){
            //������ŵ�ֵ����С��С�������������С��
            if(scale*scaleFactor<minScale){
                scaleFactor = minScale/scale;
            }
            //������ŵ�ֵ�����Ĵ󣬾�����������
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
        //��Ϊ�Ŵ�֮������С�����Χ�����ױ�,������������ĺ����������bug��
        applyPicToCenterWhenScale();
        setImageMatrix(matrix);
    }
}
