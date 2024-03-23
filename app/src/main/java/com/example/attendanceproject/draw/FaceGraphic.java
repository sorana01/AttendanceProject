package com.example.attendanceproject.draw;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.Log;

import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceLandmark;

import java.util.Locale;

/**
 * Simplified Graphic instance for rendering face position within the associated
 * graphic overlay view.
 */
public class FaceGraphic extends GraphicOverlay.Graphic {
    private static final float BOX_STROKE_WIDTH = 5.0f;
    private final Paint boxPaint;
    public RectF faceBoundingBox;
    private Face face;

    public FaceGraphic(GraphicOverlay overlay, Face face, int width, int height) {
        super(overlay, width, height);
        this.face = face;
        this.faceBoundingBox = transform(face.getBoundingBox());

        boxPaint = new Paint();
        boxPaint.setColor(Color.WHITE);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(BOX_STROKE_WIDTH);
    }

    /** Draws the bounding box around the detected face on the supplied canvas. */
    @Override
    public void draw(Canvas canvas) {
        if (faceBoundingBox == null) {
            return;
        }

        // Draw the bounding box around the face.
        float left = faceBoundingBox.left;
        float top = faceBoundingBox.top;
        float right = faceBoundingBox.right;
        float bottom = faceBoundingBox.bottom;

        canvas.drawRect(left, top, right, bottom, boxPaint);
    }
}
