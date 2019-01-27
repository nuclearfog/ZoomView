package org.nuclearfog.zoomview;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.RemoteViews.RemoteView;

import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MOVE;
import static android.view.MotionEvent.ACTION_POINTER_DOWN;
import static android.view.MotionEvent.ACTION_UP;

@RemoteView
public class ZoomView extends ImageView {

    private static final float MAX = 2.0f;
    private static final float MIN = 0.2f;

    private PointF pos = new PointF(0.0f, 0.0f);
    private PointF dist = new PointF(0.0f, 0.0f);

    private boolean moveLock = false;


    public ZoomView(Context context) {
        super(context);
    }

    public ZoomView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ZoomView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


    @Override
    public boolean performClick() {
        return super.performClick();
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (getScaleType() != ScaleType.MATRIX)
            setScaleType(ScaleType.MATRIX);

        if (event.getPointerCount() == 1) {

            // start first Touch
            if (event.getAction() == ACTION_DOWN) {
                pos.set(event.getX(), event.getY());
                return true;
            }

            // Move finger
            if (event.getAction() == ACTION_MOVE && !moveLock) {
                Matrix m = new Matrix(getImageMatrix());
                m.postTranslate(event.getX() - pos.x, event.getY() - pos.y);
                pos.set(event.getX(), event.getY());
                apply(m);
                return true;
            }

            // Action stop
            if (event.getAction() == ACTION_UP) {
                moveLock = false;
            }

        } else if (event.getPointerCount() == 2) {

            // start 2 Finger Touch
            if ((event.getAction() & MotionEvent.ACTION_MASK) == ACTION_POINTER_DOWN) {
                float distX = event.getX(0) - event.getX(1);
                float distY = event.getY(0) - event.getY(1);
                // Distance vector
                dist.set(distX, distY);

                moveLock = true;
                return true;
            }

            // 2 Finger move
            if (event.getAction() == ACTION_MOVE) {
                float distX = event.getX(0) - event.getX(1);
                float distY = event.getY(0) - event.getY(1);

                PointF current = new PointF(distX, distY);
                float scale = current.length() / dist.length();
                Matrix m = new Matrix(getImageMatrix());
                m.postScale(scale, scale, getWidth() / 2.0f, getHeight() / 2.0f);
                dist.set(distX, distY);
                apply(m);
                return true;
            }
        }
        return super.performClick();
    }


    /**
     * Reset Image position/zoom to default
     */
    public void reset() {
        setScaleType(ScaleType.CENTER_CROP);
    }


    /**
     * Apply image matrix
     * Limit translation and focus
     *
     * @param m Image Matrix
     */
    private void apply(Matrix m) {
        float[] val = new float[9];
        m.getValues(val);

        Drawable d = getDrawable();
        if (d == null) return;

        float scale = (val[Matrix.MSCALE_X] + val[Matrix.MSCALE_Y]) / 2;    // Scale factor
        float width = d.getIntrinsicWidth() * scale;                        // image width
        float height = d.getIntrinsicHeight() * scale;                      // image height
        float leftBorder = val[Matrix.MTRANS_X];                            // distance to left border
        float rightBorder = -(val[Matrix.MTRANS_X] + width - getWidth());   // distance to right border
        float bottomBorder = val[Matrix.MTRANS_Y];                          // distance to bottom border
        float topBorder = -(val[Matrix.MTRANS_Y] + height - getHeight());   // distance to top border

        if (width > getWidth()) {                       // is image width bigger than screen width?
            if (rightBorder > 0)                        // is image on the right border?
                m.postTranslate(rightBorder, 0);    // clamp to right border
            else if (leftBorder > 0)
                m.postTranslate(-leftBorder, 0);    // clamp to left order
        } else if (leftBorder < 0 ^ rightBorder < 0) {  // does image clash with one border?
            if (rightBorder < 0)
                m.postTranslate(rightBorder, 0);    // clamp to right border
            else
                m.postTranslate(-leftBorder, 0);    // clamp to left border
        }

        if (height > getHeight()) {                     // is image height bigger than screen height?
            if (bottomBorder > 0)                       // is image on the bottom border?
                m.postTranslate(0, -bottomBorder);  // clamp to bottom border
            else if (topBorder > 0)                     // is image on the top border?
                m.postTranslate(0, topBorder);      // clamp to top border
        } else if (topBorder < 0 ^ bottomBorder < 0) {  // does image clash with one border?
            if (bottomBorder < 0)
                m.postTranslate(0, -bottomBorder);  // clamp to bottom border
            else
                m.postTranslate(0, topBorder);      // clamp to top border
        }

        if (scale > MAX)                                // scale limit exceeded?
            m.postScale(MAX / scale, MAX / scale, getWidth() / 2.0f, getHeight() / 2.0f);  // undo scale setting
        else if (scale < MIN)
            m.postScale(MIN / scale, MIN / scale, getWidth() / 2.0f, getHeight() / 2.0f);  // undo scale setting
        setImageMatrix(m);                              // set Image matrix
    }
}