package com.example.unno.mywebrtc;

import java.nio.ByteBuffer;

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
            action = buffer.getInt();
            touchX = buffer.getFloat();
            touchY = buffer.getFloat();

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
        ByteBuffer buffer = ByteBuffer.allocate((Integer.SIZE + Float.SIZE * 2) / Byte.SIZE);
        buffer.putInt(mAction);
        buffer.putFloat(mTouchX);
        buffer.putFloat(mTouchY);
        buffer.position(0);
        return buffer;
    }
}
