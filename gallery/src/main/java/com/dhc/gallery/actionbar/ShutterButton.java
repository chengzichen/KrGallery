package com.dhc.gallery.actionbar;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import com.dhc.gallery.R;
import com.dhc.gallery.utils.AndroidUtilities;


public class ShutterButton extends View {

    private boolean isVedio ;

    public enum State {
        DEFAULT,
        RECORDING
    }

    //0是默认的两种模式都可以,1 是表示只能拍照 ,2表示只能录像
    private int type = 0;

    private final static int LONG_PRESS_TIME = 800;

    private Drawable shadowDrawable;

    private DecelerateInterpolator interpolator = new DecelerateInterpolator();
    private Paint whitePaint;
    private Paint redPaint;
    private ShutterButtonDelegate delegate;
    private State state;
    private boolean pressed;
    private float redProgress;
    private long lastUpdateTime;
    private long totalTime;
    private boolean processRelease;

    private Runnable longPressed = new Runnable() {
        public void run() {
            if (delegate != null) {
                if (!delegate.shutterLongPressed()) {
                    processRelease = false;
                }
            }
        }
    };

    public interface ShutterButtonDelegate {
        boolean shutterLongPressed();

        void shutterReleased();

        void shutterLongPressedReleased();

        void shutterCancel();
    }

    public ShutterButton(Context context,int type) {
        super(context);
        shadowDrawable = getResources().getDrawable(R.drawable.camera_btn);
        whitePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        whitePaint.setStyle(Paint.Style.FILL);
        whitePaint.setColor(0xffffffff);
        redPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        redPaint.setStyle(Paint.Style.FILL);
        redPaint.setColor(0xffcd4747);
        state = State.DEFAULT;
        this.type=(type==1||type==2)?type:0;
        isVedio=(type==2);
    }

    public void setDelegate(ShutterButtonDelegate shutterButtonDelegate) {
        delegate = shutterButtonDelegate;
    }

    public ShutterButtonDelegate getDelegate() {
        return delegate;
    }

    private void setHighlighted(boolean value) {
        AnimatorSet animatorSet = new AnimatorSet();
        if (value) {
            animatorSet.playTogether(
                    ObjectAnimator.ofFloat(this, "scaleX", 1.06f),
                    ObjectAnimator.ofFloat(this, "scaleY", 1.06f));
        } else {
            animatorSet.playTogether(
                    ObjectAnimator.ofFloat(this, "scaleX", 1.0f),
                    ObjectAnimator.ofFloat(this, "scaleY", 1.0f));
            animatorSet.setStartDelay(40);
        }
        animatorSet.setDuration(120);
        animatorSet.setInterpolator(interpolator);
        animatorSet.start();
    }

    @Override
    public void setScaleX(float scaleX) {
        super.setScaleX(scaleX);
        invalidate();
    }

    public State getState() {
        return state;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int cx = getMeasuredWidth() / 2;
        int cy = getMeasuredHeight() / 2;

        shadowDrawable.setBounds(cx - AndroidUtilities.dp(36), cy - AndroidUtilities.dp(36), cx + AndroidUtilities.dp(36), cy + AndroidUtilities.dp(36));
        shadowDrawable.draw(canvas);
        if (pressed || getScaleX() != 1.0f) {
            float scale = (getScaleX() - 1.0f) / 0.06f;
            whitePaint.setAlpha((int) (255 * scale));
            canvas.drawCircle(cx, cy, AndroidUtilities.dp(26), whitePaint);

            if (state == State.RECORDING) {
                if (redProgress != 1.0f) {
                    long dt = Math.abs(System.currentTimeMillis() - lastUpdateTime);
                    if (dt > 17) {
                        dt = 17;
                    }
                    totalTime += dt;
                    if (totalTime > 120) {
                        totalTime = 120;
                    }
                    redProgress = interpolator.getInterpolation(totalTime / 120.0f);
                    invalidate();
                }
                canvas.drawCircle(cx, cy, AndroidUtilities.dp(26) * scale * redProgress, redPaint);
            } else if (redProgress != 0) {
                canvas.drawCircle(cx, cy, AndroidUtilities.dp(26) * scale, redPaint);
            }
        } else if (redProgress != 0) {
            redProgress = 0;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(AndroidUtilities.dp(84), AndroidUtilities.dp(84));
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (type == 0) {
            return onTouchTypeOne(motionEvent);
        } else if (type == 1) {
            return onTouchTypeTwo(motionEvent);
        } else if (type == 2) {

            return onTouchTypeThree(motionEvent);
        }
        return true;
    }


    public boolean onTouchTypeOne(MotionEvent motionEvent) {
        float x = motionEvent.getX();
        float y = motionEvent.getX();
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                AndroidUtilities.runOnUIThread(longPressed, LONG_PRESS_TIME);
                pressed = true;
                processRelease = true;
                setHighlighted(true);
                break;
            case MotionEvent.ACTION_UP:
                setHighlighted(false);
                AndroidUtilities.cancelRunOnUIThread(longPressed);
                if (processRelease && x >= 0 && y >= 0 && x <= getMeasuredWidth() && y <= getMeasuredHeight()) {
                    delegate.shutterReleased();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (x < 0 || y < 0 || x > getMeasuredWidth() || y > getMeasuredHeight()) {
                    AndroidUtilities.cancelRunOnUIThread(longPressed);
                    if (state == State.RECORDING) {
                        setHighlighted(false);
                        delegate.shutterCancel();
                        setState(State.DEFAULT, true);
                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                setHighlighted(false);
                pressed = false;
        }
        return true;
    }

    public boolean onTouchTypeTwo(MotionEvent motionEvent) {
        float x = motionEvent.getX();
        float y = motionEvent.getX();
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                pressed = true;
                processRelease = true;
                setHighlighted(true);
                break;
            case MotionEvent.ACTION_UP:
                setHighlighted(false);
                if (processRelease && x >= 0 && y >= 0 && x <= getMeasuredWidth() && y <= getMeasuredHeight()) {
                    delegate.shutterReleased();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (x < 0 || y < 0 || x > getMeasuredWidth() || y > getMeasuredHeight()) {
                    if (state == State.RECORDING) {
                        setHighlighted(false);
                        delegate.shutterCancel();
                        setState(State.DEFAULT, true);
                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                setHighlighted(false);
                pressed = false;
        }
        return true;
    }

    public boolean onTouchTypeThree(MotionEvent motionEvent) {
        float x = motionEvent.getX();
        float y = motionEvent.getX();
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:

                if (isVedio) {
                    isVedio = false;
                    AndroidUtilities.runOnUIThread(longPressed);
                    pressed = true;
                    processRelease = true;
                    setHighlighted(true);
                } else {
                    processRelease = true;
                    isVedio = true;
                    AndroidUtilities.cancelRunOnUIThread(longPressed);
                    if (processRelease && x >= 0 && y >= 0 && x <= getMeasuredWidth() && y <= getMeasuredHeight()) {
                        delegate.shutterLongPressedReleased();
                    }
//                    if (x < 0 || y < 0 || x > getMeasuredWidth() || y > getMeasuredHeight()) {
//                        if (state == State.RECORDING) {
//                            delegate.shutterCancel();
//                            setState(State.DEFAULT, true);
//                        }
//                    }
                    setHighlighted(false);
                    pressed = false;
                }
        }
        return true;
    }


    public void setState(State value, boolean animated) {
        if (state != value) {
            state = value;
            if(value==State.DEFAULT){
                processRelease = true;
                isVedio = true;
                setHighlighted(false);
                pressed = false;
            }
            if (animated) {
                lastUpdateTime = System.currentTimeMillis();
                totalTime = 0;
                if (state != State.RECORDING) {
                    redProgress = 0.0f;
                }
            } else {
                if (state == State.RECORDING) {
                    redProgress = 1.0f;
                } else {
                    redProgress = 0.0f;
                }
            }
            invalidate();
        }
    }
}
