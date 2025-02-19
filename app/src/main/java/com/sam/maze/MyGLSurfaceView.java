package com.sam.maze;

import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

public class MyGLSurfaceView extends GLSurfaceView implements SensorEventListener {

    // For gyroscopic sensor :

    private static final float SENSOR_RESOLUTION = 0.0000005f;
    private float mTimestamp;
    private final SensorManager mSensorManager;
    private final Sensor mGyroscope;

    @Override
    public void onSensorChanged(SensorEvent e) {
        if (!isGyroCtrl) return;
        if (mTimestamp != 1) {
            float dt = (e.timestamp - mTimestamp) * SENSOR_RESOLUTION;
            float axisX, axisY;
            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                axisX = e.values[0] * dt;
                axisY = -e.values[1] * dt * 1.5f;
            } else {
                axisY = e.values[0] * dt;
                axisX = e.values[1] * dt * 1.5f;
            }
            game.rotateView(axisX, axisY);
        }
        mTimestamp = e.timestamp;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {}

    @Override
    public void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    public void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    private final Game game;
    private final ScaleGestureDetector scaleGestureDetector;
    private boolean isScaling;
    private boolean isGyroCtrl;
    private float oldX, oldY;

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        float last;
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            last = detector.getCurrentSpan();
            isScaling = true;
            return true;
        }
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float span = detector.getCurrentSpan();
            float diff = span-last;
            last = span;
            game.moveView(diff);
            return true;
        }
    }

    public MyGLSurfaceView(Context context) {
        super(context);
        // OpenGL
        setPreserveEGLContextOnPause(true);
        setEGLContextClientVersion(3);
        game = new Game(context, getWidth(), getHeight());
        setRenderer(game);

        // sensor:

        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        // pinch
        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());
    }

    public boolean onTouchEvent(MotionEvent e) {
        scaleGestureDetector.onTouchEvent(e);
        float x = e.getX();
        float y = e.getY();

        if (e.getPointerCount() == 3) {  // Check if three fingers are used
            if (e.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN ||
                    e.getActionMasked() == MotionEvent.ACTION_DOWN) {
                isGyroCtrl = !isGyroCtrl;
            }
        }

        switch (e.getAction()) {
            case MotionEvent.ACTION_MOVE:
                if (isScaling || isGyroCtrl) break;
                game.rotateView(x-oldX, y-oldY);
                oldX = x;
                oldY = y;
                requestRender();
                break;
            case MotionEvent.ACTION_DOWN:
                oldX = x;
                oldY = y;
                isScaling = false;
                break;
        }
        return true;
    }

    public boolean onKeyBack() {
        if (game.isAlive()) {
            game.resign();
            return true;
        }
        return false;
    }
}
