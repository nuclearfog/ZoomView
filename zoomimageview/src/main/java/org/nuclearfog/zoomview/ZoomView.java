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

    // Default values
    private static final float DEF_MAX_ZOOM_IN = 0.5f;
    private static final float DEF_MAX_ZOOM_OUT = 3.0f;
    private static final boolean DEF_ENABLE_MOVE = true;

    // Layout Attributes
    private float max_zoom_in = DEF_MAX_ZOOM_IN;
    private float max_zoom_out = DEF_MAX_ZOOM_OUT;
    private boolean enableMove = DEF_ENABLE_MOVE;

    // intern flags
    private PointF pos = new PointF(0.0f, 0.0f);
    private PointF dist = new PointF(0.0f, 0.0f);
    private boolean moveLock = false;


    public ZoomView(Context context) {
        super(context);
    }


    public ZoomView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setAttributes(attrs);
    }


    public ZoomView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setAttributes(attrs);
    }


    @Override
    public boolean performClick() {
        return super.performClick();
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (getScaleType() != ScaleType.MATRIX)
            setScaleType(ScaleType.MATRIX);

        if (event.getPointerCount() == 1 && enableMove) {
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
     * return if image is movable
     *
     * @return if image is movable
     */
    public boolean isMovable() {
        return enableMove;
    }


    /**
     * set Image movable
     *
     * @param enableMove set image movable
     */
    public void setMovable(boolean enableMove) {
        this.enableMove = enableMove;
    }


    /**
     * get maximum zoom in
     *
     * @return maximum zoom value
     */
    public float getMaxZoomIn() {
        return max_zoom_in;
    }


    /**
     * set maximum zoom in
     *
     * @param max_zoom_in maximum zoom value
     */
    public void setMaxZoomIn(float max_zoom_in) {
        if (max_zoom_in < 1.0f)
            throw new AssertionError("value should be more 1.0!");
        this.max_zoom_in = max_zoom_in;
    }


    /**
     * get maximum zoom in
     *
     * @return maximum zoom value
     */
    public float getMaxZoomOut() {
        return max_zoom_out;
    }


    /**
     * set maximum zoom in
     *
     * @param max_zoom_out maximum zoom value
     */
    public void setMaxZoomOut(float max_zoom_out) {
        if (max_zoom_out < 1.0f)
            throw new AssertionError("value should be less 1.0!");
        this.max_zoom_out = max_zoom_out;
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

        if (scale > max_zoom_in)                        // scale limit exceeded?
            m.postScale(max_zoom_in / scale, max_zoom_in / scale, getWidth() / 2.0f, getHeight() / 2.0f);    // undo scale setting
        else if (scale < max_zoom_out)
            m.postScale(max_zoom_out / scale, max_zoom_out / scale, getWidth() / 2.0f, getHeight() / 2.0f);  // undo scale setting
        setImageMatrix(m);                              // set Image matrix
    }


    /**
     * Get attributes
     *
     * @param attrs set of attributes
     */
    private void setAttributes(AttributeSet attrs) {
        if (attrs != null) {
            setMaxZoomIn(attrs.getAttributeFloatValue(0, DEF_MAX_ZOOM_IN));
            setMaxZoomOut(max_zoom_out = attrs.getAttributeFloatValue(1, DEF_MAX_ZOOM_OUT));
            setMovable(attrs.getAttributeBooleanValue(2, DEF_ENABLE_MOVE));
        }
        if (max_zoom_out < 1.0f || max_zoom_in > 1.0f)
            throw new AssertionError("Attribute error!");
    }
}