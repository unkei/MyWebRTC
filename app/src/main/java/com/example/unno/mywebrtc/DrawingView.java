package com.example.unno.mywebrtc;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by unno on 9/15/15.
 */
public class DrawingView extends View {

    public interface DrawingViewListener {
        void onUpdated(Motion motion);
    }

    //drawing path
    private Path drawPath;
    //drawing and canvas paint
    private Paint drawPaint, canvasPaint;
    //initial color
    private int paintLocalColor = 0xFF660000;
    private int paintRemoteColor = 0xFF000066;
    //canvas
    private Canvas drawCanvas;
    //canvas bitmap
    private Bitmap canvasBitmap;

    private DrawingViewListener mListener;
    private boolean enableTouch = false;

    public DrawingView(Context context, AttributeSet attrs){
        super(context, attrs);
        setupDrawing();
    }

    public void setListener(DrawingViewListener listener) {
        mListener = listener;
    }

    private void notifyListener(Motion motion) {
        if (mListener != null && motion != null) {
            mListener.onUpdated(motion);
        }
    }

    private void setupDrawing(){
        // get drawing area setup for interaction
        drawPath = new Path();
        drawPaint = new Paint();

        drawPaint.setColor(paintLocalColor);

        drawPaint.setAntiAlias(true);
        drawPaint.setStrokeWidth(20);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);

        canvasPaint = new Paint(Paint.DITHER_FLAG);

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // view given size
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        drawCanvas = new Canvas(canvasBitmap);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // draw view
        canvas.drawBitmap(canvasBitmap, 0, 0, canvasPaint);
        canvas.drawPath(drawPath, drawPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!enableTouch) {
            return super.onTouchEvent(event);
        }

        int action = event.getAction();
        float touchX = event.getX();
        float touchY = event.getY();

        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE || action == MotionEvent.ACTION_UP) {
            Motion motion = new Motion(action, touchX, touchY);
            handleMotion(motion, paintLocalColor);
            notifyListener(motion);
            return true;
        }
        return false;
    }

    public void enableTouch(boolean enabled) {
        enableTouch = enabled;
    }

    public void handleMotion(Motion motion) {
        handleMotion(motion, paintRemoteColor);
    }

    public void handleMotion(Motion motion, int color ) {
        if (motion == null) {
            return;
        }

        int action = motion.getAction();
        float touchX = motion.getTouchX();
        float touchY = motion.getTouchY();

        drawPaint.setColor(color);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                drawPath.moveTo(touchX, touchY);
                break;
            case MotionEvent.ACTION_MOVE:
                drawPath.lineTo(touchX, touchY);
                break;
            case MotionEvent.ACTION_UP:
                drawCanvas.drawPath(drawPath, drawPaint);
                drawPath.reset();
                break;
            default:
                break;
        }
        invalidate();
        resetClearTimer();
    }

    private void clearCanvas() {
        this.post(new Runnable() {
            @Override
            public void run() {
                drawCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
                invalidate();
            }
        });
    }

    Timer clearTimer = null;
    private void resetClearTimer() {
        if (clearTimer != null) {
            clearTimer.cancel();
        }
        clearTimer = new Timer();
        clearTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                clearCanvas();
            }
        }, 3000);
    }
}
