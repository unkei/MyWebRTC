package com.example.unno.mywebrtc;

import android.view.MotionEvent;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by unno on 9/15/15.
 */
public class Motion {
    int mAction;
    float mTouchX;
    float mTouchY;

    public static Motion fromBytes(ByteBuffer buffer) {
        int action;
        float touchX;
        float touchY;
        try {
            action = toAction(buffer.getInt());
            touchX = (float)buffer.getDouble();
            touchY = (float)buffer.getDouble();

            return new Motion(action, touchX, touchY);
        } catch (Exception e) {
            return null;
        }
    }

    public Motion(int action, float touchX, float touchY) {
        mAction = action;
        mTouchX = touchX;
        mTouchY = touchY;
    }

    public int getAction() {
        return mAction;
    }

    public float getTouchX() {
        return mTouchX;
    }

    public float getTouchY() {
        return mTouchY;
    }

    public ByteBuffer toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate((Integer.SIZE + Double.SIZE * 2) / Byte.SIZE);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(fromAction(mAction));
        buffer.putDouble(mTouchX);
        buffer.putDouble(mTouchY);
        buffer.position(0);
        return buffer;
    }

    private static int fromAction(int action) {
        int ret = 0;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                ret = 1;
                break;
            case MotionEvent.ACTION_MOVE:
                ret = 2;
                break;
            case MotionEvent.ACTION_UP:
                ret = 3;
                break;
        }
        return ret;
    }

    private static int toAction(int action) {
        int ret = 0;
        switch (action) {
            case 1:
                ret = MotionEvent.ACTION_DOWN;
                break;
            case 2:
                ret = MotionEvent.ACTION_MOVE;
                break;
            case 3:
                ret = MotionEvent.ACTION_UP;
                break;
        }
        return ret;
    }
}
