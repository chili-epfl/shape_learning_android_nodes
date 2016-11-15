package org.ros.android.shape_learner;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.util.Log;

import org.ros.android.MessageCallable;

import java.util.ArrayList;

/**
 * View for capturing input from stylus and/or fingertip drawings.
 * from http://corner.squareup.com/2010/07/smooth-signatures.html apache 2.0 license
 * Modified by deanna on 7/05/14.
 */
public class UserDrawingView extends View {
    private static final java.lang.String TAG = "UserDrawingView";
    private MessageCallable<Integer, ArrayList<double[]> > stylusStrokeFinishedCallable;
    private MessageCallable<Integer, ArrayList<double[]> > fingerStrokeFinishedCallable;
    private boolean respondToFinger = false;
    private boolean respondToStylus = false;
    private static final float STROKE_WIDTH = 10f;
    public long respTime;
    public long writingTime;
    public long firstPoint;
    public long lastPoint;
    public boolean respTime_flag = true;

    /** Need to track this so the dirty region can accommodate the stroke. **/
    private static final float HALF_STROKE_WIDTH = STROKE_WIDTH / 2;

    private Paint paint = new Paint();
    private Path path = new Path();
    private ArrayList<double[]> pointsOnPath = new ArrayList<double[]>();

    /**
     * Optimizes painting by invalidating the smallest possible area.
     */
    private float lastTouchX;
    private float lastTouchY;
    private final RectF dirtyRect = new RectF();

    public UserDrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);

        paint.setAntiAlias(true);
        paint.setColor(Color.argb(255, 67, 77, 88));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeWidth(STROKE_WIDTH);
    }

    /**
     * Set which function will be called when a stroke is finished (pen-up detected).
     */
    public void setStylusStrokeFinishedCallable(MessageCallable<Integer, ArrayList<double[]> > callable) {
        this.stylusStrokeFinishedCallable = callable;
    }
    /**
     * Set which function will be called when a stroke is finished (pen-up detected).
     */
    public void setFingerStrokeFinishedCallable(MessageCallable<Integer, ArrayList<double[]> > callable) {
        this.fingerStrokeFinishedCallable = callable;
    }

    /**
     * Set paint color of path
     * @param color color to set to e.g. Color.GREEN
     */
    public void setColor(int color){
        paint.setColor(color);
    }

    public void setRespondToFinger(boolean _respondToFinger){ respondToFinger = _respondToFinger;}
    public void setRespondToStylus(boolean _respondToStylus){ respondToStylus = _respondToStylus;}

    /**
     * Erases the signature.
     */
    public void clear() {
        path.reset();
        // Repaints the entire view.
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawPath(path, paint);
        //Log.e(TAG,"Drawing now");

    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float eventX = event.getX();
        float eventY = event.getY();
        if(event.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER){ //allow another to process touch events from finger
            if(respondToFinger){
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    path.moveTo(eventX, eventY);
                    lastTouchX = eventX;
                    lastTouchY = eventY;
                    // There is no end point yet, so don't waste cycles invalidating.
                    return true;

                case MotionEvent.ACTION_MOVE:
                    drawRecentPoints(event);
                    break;
                case MotionEvent.ACTION_UP:

                    drawRecentPoints(event);
                    if(fingerStrokeFinishedCallable != null){
                        fingerStrokeFinishedCallable.call(pointsOnPath);
                    }
                    pointsOnPath = new ArrayList<double[]>();
                    break;

                default:
                    Log.e(TAG,"Ignored touch event: " + event.toString());
                    return false;
            }

            // Include half the stroke width to avoid clipping.
            invalidate(
                    (int) (dirtyRect.left - HALF_STROKE_WIDTH),
                    (int) (dirtyRect.top - HALF_STROKE_WIDTH),
                    (int) (dirtyRect.right + HALF_STROKE_WIDTH),
                    (int) (dirtyRect.bottom + HALF_STROKE_WIDTH));

            lastTouchX = eventX;
            lastTouchY = eventY;
            return true;
            }
            else{
                return false;
            }
        }
        else if(event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS){//only respond to inputs from the stylus
            if(respondToStylus){

                if(respTime_flag) {
                    firstPoint = System.currentTimeMillis();
                    respTime = firstPoint - BoxesViewNode.startTime;
                    respTime_flag = false;
                }

                lastPoint = System.currentTimeMillis();
                writingTime = lastPoint - firstPoint;

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        path.moveTo(eventX, eventY);
                        lastTouchX = eventX;
                        lastTouchY = eventY;
                        // There is no end point yet, so don't waste cycles invalidating.
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        drawRecentPoints(event);
                        break;
                    case MotionEvent.ACTION_UP:
                        drawRecentPoints(event);
                        if(stylusStrokeFinishedCallable != null){
                            stylusStrokeFinishedCallable.call(pointsOnPath);
                        }
                        pointsOnPath = new ArrayList<double[]>();
                        break;

                    default:
                        Log.e(TAG,"Ignored touch event: " + event.toString());
                        return false;
                }

                // Include half the stroke width to avoid clipping.
                invalidate(
                        (int) (dirtyRect.left - HALF_STROKE_WIDTH),
                        (int) (dirtyRect.top - HALF_STROKE_WIDTH),
                        (int) (dirtyRect.right + HALF_STROKE_WIDTH),
                        (int) (dirtyRect.bottom + HALF_STROKE_WIDTH));

                lastTouchX = eventX;
                lastTouchY = eventY;
                return true;
            }
            else{
                return false;
            }
        }
        return false;
    }

    /**
     * Called when new points need to be drawn (during motion or on pen-up).
     */
    private void drawRecentPoints(MotionEvent event){
        float eventX = event.getX();
        float eventY = event.getY();

        // Start tracking the dirty region.
        resetDirtyRect(eventX, eventY);

        // When the hardware tracks events faster than they are delivered, the
        // event will contain a history of those skipped points.
        int historySize = event.getHistorySize();

        for (int i = 0; i < historySize; i++) {
            float historicalX = event.getHistoricalX(i);
            float historicalY = event.getHistoricalY(i);
            expandDirtyRect(historicalX, historicalY);
            path.lineTo(historicalX, historicalY);
            double[] point = {historicalX, historicalY};
            pointsOnPath.add(point);
        }

        // After replaying history, connect the line to the touch point.
        path.lineTo(eventX, eventY);
    }

    /**
     * Called when replaying history to ensure the dirty region includes all
     * points.
     */
    private void expandDirtyRect(float historicalX, float historicalY) {
        if (historicalX < dirtyRect.left) {
            dirtyRect.left = historicalX;
        } else if (historicalX > dirtyRect.right) {
            dirtyRect.right = historicalX;
        }
        if (historicalY < dirtyRect.top) {
            dirtyRect.top = historicalY;
        } else if (historicalY > dirtyRect.bottom) {
            dirtyRect.bottom = historicalY;
        }
    }

    /**
     * Resets the dirty region when the motion event occurs.
     */
    private void resetDirtyRect(float eventX, float eventY) {

        // The lastTouchX and lastTouchY were set when the ACTION_DOWN
        // motion event occurred.
        dirtyRect.left = Math.min(lastTouchX, eventX);
        dirtyRect.right = Math.max(lastTouchX, eventX);
        dirtyRect.top = Math.min(lastTouchY, eventY);
        dirtyRect.bottom = Math.max(lastTouchY, eventY);
    }

    public void requestClear(){
        post(new Runnable() {
            @Override
            public void run() {
                clear();
            }
        });
    }
}

