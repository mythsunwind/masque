package de.defector.masque.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ActionMode;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import de.defector.masque.R;
import de.defector.masque.models.Layer;

import static android.content.ContentValues.TAG;

public class LayerView extends AppCompatImageView {

    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private List<Layer> layers = new ArrayList<>();
    private Bitmap background;
    private ActionMode mActionMode;

    private GestureDetectorCompat gestureDetectorCompat;
    private ScaleGestureDetector scaleGestureDetector;

    final int semiTransparentGrey = Color.argb(155, 185, 185, 185);
    final ColorFilter greyFilter = new PorterDuffColorFilter(semiTransparentGrey, PorterDuff.Mode.SRC_ATOP);

    final Bitmap emojiBitmap = BitmapFactory.decodeResource(getResources(), R.raw.face2);

    public LayerView(Context context) {
        super(context);
        initialize(context);
    }

    public LayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public LayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    private void initialize(Context context) {
        // initialize listeners
        this.gestureDetectorCompat = new GestureDetectorCompat(context, new TapsListener());
        this.scaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());

        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (gestureDetectorCompat != null && scaleGestureDetector != null) {
                    gestureDetectorCompat.onTouchEvent(event);
                    scaleGestureDetector.onTouchEvent(event);
                }
                return true;
            }
        });
    }

    public void setBackground(Bitmap background) {
        this.background = background;

        this.invalidate();
    }

    public Bitmap getOriginalBackground() {
        return this.background;
    }

    public boolean hasBackground() {
        return this.background != null;
    }

    public void setLayers(List<Layer> layers) {
        this.layers = layers;

        this.invalidate();
    }

    public List<Layer> getLayers() {
        return this.layers;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (this.background != null) {
            setImageBitmap(this.background);
        }

        Matrix m = getImageMatrix();
        float[] values = new float[9];
        m.getValues(values);

        float scaleX = values[0];
        float scaleY = values[4];
        int transformX = (int) values[2];
        int transformY = (int) values[5];

        /*
        Log.d(TAG, String.format("Matrix offset: left %s top %s scalex %s scaley %s",
                transformX, transformY, scaleX, scaleY));
                */

        for (Layer layer : layers) {
            if (layer.isSelected()) {
                paint.setColorFilter(greyFilter);
            } else {
                paint.setColorFilter(null);
            }
            if (layer.getType() == Layer.TYPE.BLACK) {
                canvas.drawRect((transformX + layer.getBounds().left) * scaleX,
                        transformY + layer.getBounds().top * scaleY,
                        (transformX + layer.getBounds().right) * scaleX,
                        transformY + layer.getBounds().bottom * scaleY,
                        paint);
                /*
                Log.d(TAG, String.format("Layer: left %s top %s right %s actionmenu %s",
                        (transformX + layer.getBounds().left)*scaleX,
                        transformY + layer.getBounds().top*scaleY,
                        (transformX + layer.getBounds().right)*scaleX,
                        transformY + layer.getBounds().bottom*scaleY));
                        */
            } else if (layer.getType() == Layer.TYPE.EMOJI) {
                Rect transformedLayerRect = new Rect(
                        (int) ((transformX + layer.getBounds().left) * scaleX),
                        (int) (transformY + layer.getBounds().top * scaleY),
                        (int) ((transformX + layer.getBounds().right) * scaleX),
                        (int) (transformY + layer.getBounds().bottom * scaleY)
                );
                canvas.drawBitmap(emojiBitmap,
                        null,
                        transformedLayerRect,
                        paint);
            }
        }
    }

    private boolean selectLayerAtPoint(int x, int y) {
        boolean anyLayerSelected = false;

        for (Layer layer : layers) {
            Matrix m = getImageMatrix();
            float[] values = new float[9];
            m.getValues(values);

            float scaleX = values[0];
            float scaleY = values[4];
            int transformX = (int) values[2];
            int transformY = (int) values[5];

            Rect transformedLayerRect = new Rect(
                    (int) ((transformX + layer.getBounds().left) * scaleX),
                    (int) (transformY + layer.getBounds().top * scaleY),
                    (int) ((transformX + layer.getBounds().right) * scaleX),
                    (int) (transformY + layer.getBounds().bottom * scaleY)
            );

            if (transformedLayerRect.contains(x, y)) {
                layer.setSelected(true);
                anyLayerSelected = true;
            } else {
                layer.setSelected(false);
            }
        }
        return anyLayerSelected;
    }

    private Point transformEventPointToLayerPoint(int x, int y) {
        Matrix m = getImageMatrix();
        float[] values = new float[9];
        m.getValues(values);

        float scaleX = values[0];
        float scaleY = values[4];
        int transformX = (int) values[2];
        int transformY = (int) values[5];

        Log.d(TAG, String.format("scaleX %s scaleY %s transformX %s transformY %s",
                scaleX, scaleY, transformX, transformY));

        // TODO: transformX needs to be applied too
        return new Point((int) ((x) * (1 / scaleX)), (int) ((y - transformY) * (1 / scaleY)));
    }

    private Point scaleEventDistanceToLayerDistance(int x, int y) {
        Matrix m = getImageMatrix();
        float[] values = new float[9];
        m.getValues(values);

        float scaleX = values[0];
        float scaleY = values[4];

        return new Point((int) (x * (1 / scaleX)), (int) (y * (1 / scaleY)));
    }

    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.actionmenu, menu);

            Drawable emojiIcon = getResources().getDrawable(R.drawable.ic_insert_emoticon_black_24dp);
            Drawable boxIcon = getResources().getDrawable(R.drawable.ic_box_black_24dp);

            for (Layer layer : layers) {
                if (layer.isSelected()) {
                    switch (layer.getType()) {
                        case EMOJI:
                            emojiIcon.setColorFilter(getResources().getColor(R.color.colorAccent), PorterDuff.Mode.SRC_IN);
                            menu.findItem(R.id.chooseEmoji).setIcon(emojiIcon);
                            boxIcon.setColorFilter(null);
                            menu.findItem(R.id.chooseBoxStyle).setIcon(boxIcon);
                            break;
                        case BLACK:
                            boxIcon.setColorFilter(getResources().getColor(R.color.colorAccent), PorterDuff.Mode.SRC_IN);
                            menu.findItem(R.id.chooseBoxStyle).setIcon(boxIcon);
                            emojiIcon.setColorFilter(null);
                            menu.findItem(R.id.chooseEmoji).setIcon(emojiIcon);
                            break;
                        default:
                            break;
                    }
                }
            }
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            Drawable emojiIcon = getResources().getDrawable(R.drawable.ic_insert_emoticon_black_24dp);
            Drawable boxIcon = getResources().getDrawable(R.drawable.ic_box_black_24dp);

            LayerView layerView = findViewById(R.id.layerView);
            switch (item.getItemId()) {
                case R.id.delete:
                    layerView.deleteSelectedLayer();
                    mode.finish();
                    return true;
                case R.id.chooseEmoji:
                    layerView.setSelectedLayerType(Layer.TYPE.EMOJI);
                    emojiIcon.setColorFilter(getResources().getColor(R.color.colorAccent), PorterDuff.Mode.SRC_IN);
                    item.setIcon(emojiIcon);
                    boxIcon.setColorFilter(null);
                    mode.getMenu().findItem(R.id.chooseBoxStyle).setIcon(boxIcon);
                    return true;
                case R.id.chooseBoxStyle:
                    layerView.setSelectedLayerType(Layer.TYPE.BLACK);
                    boxIcon.setColorFilter(getResources().getColor(R.color.colorAccent), PorterDuff.Mode.SRC_IN);
                    item.setIcon(boxIcon);
                    emojiIcon.setColorFilter(null);
                    mode.getMenu().findItem(R.id.chooseEmoji).setIcon(emojiIcon);
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
        }
    };

    private class TapsListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            if (mActionMode != null) {
                mActionMode.finish();
            }
            if (selectLayerAtPoint((int) e.getX(), (int) e.getY())) {
                mActionMode = startActionMode(mActionModeCallback);
            }
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            if (mActionMode != null) {
                mActionMode.finish();
            }

            if (selectLayerAtPoint((int) e.getX(), (int) e.getY())) {
                mActionMode = startActionMode(mActionModeCallback);
                return;
            } else {
                Point transformedPoint = transformEventPointToLayerPoint((int) e.getX(), (int) e.getY());
                int x = transformedPoint.x;
                int y = transformedPoint.y;
                Log.d(TAG, String.format("transformedPoint: left %s top %s", x, y));
                layers.add(new Layer(Layer.TYPE.BLACK, new Rect(x - 50, y - 50, x + 50, y + 50)));
            }
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                                float distanceY) {
            for (Layer layer : layers) {
                if (layer.isSelected()) {
                    Point scaledDistance = scaleEventDistanceToLayerDistance((int) distanceX,
                            (int) distanceY);
                    layer.move(scaledDistance.x, scaledDistance.y);
                }
            }
            return true;
        }

    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        private float scaleFactor = 1f;

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();

            // Don't let the object get too small or too large.
            scaleFactor = Math.max(0.1f, Math.min(scaleFactor, 5f));

            for (Layer layer : layers) {
                if (layer.isSelected()) {
                    layer.setScale(scaleFactor);
                }
            }

            invalidate();
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            Log.d(TAG, String.format("scaleFactor: %s", scaleFactor));
            for (Layer layer : layers) {
                Log.d(TAG, layer.toString());
                if (layer.isSelected()) {
                    layer.normalize();
                }
            }
            scaleFactor = 1f;
        }
    }

    public void finishActionMode() {
        if (mActionMode != null) {
            mActionMode.finish();
        }
    }

    public void setSelectedLayerType(Layer.TYPE type) {
        for (Layer layer : layers) {
            if (layer.isSelected()) {
                layer.setType(type);
            }
        }
        this.invalidate();
    }

    public void deleteSelectedLayer() {
        for (Layer layer : layers) {
            if (layer.isSelected()) {
                layers.remove(layer);
            }
        }
        this.invalidate();
    }
}
