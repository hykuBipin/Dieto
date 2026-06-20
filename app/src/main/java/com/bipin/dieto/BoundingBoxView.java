package com.bipin.dieto;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class BoundingBoxView extends View {

    public static class Box {
        public float left, top, right, bottom;
        public String label;
        public float confidence;
    }

    private final List<Box> boxes = new ArrayList<>();
    private final Paint paint = new Paint();
    private final Paint textPaint = new Paint();
    private final Paint badgePaint = new Paint();

    public BoundingBoxView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(34f);
        textPaint.setFakeBoldText(true);

        badgePaint.setStyle(Paint.Style.FILL);
    }

    public void setBoxes(List<Box> newBoxes) {
        boxes.clear();
        boxes.addAll(newBoxes);
        postInvalidate();
    }

    public List<Box> getBoxes() {
        return boxes;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (Box b : boxes) {
            // Classify food items for custom color-coding
            int color = Color.parseColor("#4DD0E1"); // standard cyan
            String lower = b.label.toLowerCase();
            if (lower.contains("banana") || lower.contains("apple") || lower.contains("orange") || 
                lower.contains("broccoli") || lower.contains("carrot")) {
                color = Color.parseColor("#66BB6A"); // soft green for fruits/veg
            } else if (lower.contains("donut") || lower.contains("cake")) {
                color = Color.parseColor("#EC407A"); // soft pink for sweets
            } else if (lower.contains("pizza") || lower.contains("hot dog") || lower.contains("sandwich")) {
                color = Color.parseColor("#FFA726"); // orange/amber for main meals
            }

            paint.setColor(color);
            badgePaint.setColor(color);

            // Draw premium rounded bounding box
            canvas.drawRoundRect(b.left, b.top, b.right, b.bottom, 16f, 16f, paint);

            // Calculate label dimensions
            float textWidth = textPaint.measureText(b.label);
            float badgeHeight = 42f;

            // Draw solid label background badge just above the bounding box
            canvas.drawRoundRect(
                    b.left,
                    b.top - badgeHeight,
                    b.left + textWidth + 20f,
                    b.top,
                    8f, 8f,
                    badgePaint
            );

            // Draw white label text inside the badge
            canvas.drawText(
                    b.label,
                    b.left + 10f,
                    b.top - 10f,
                    textPaint
            );
        }
    }
}