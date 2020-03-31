package com.example.paint;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;

class Point {
    public float x, y;
    public Point(float x, float y) {
        this.x = x;
        this.y = y;
    }
}

public class PaintView extends View {

    public static int CASUAL = 0;
    public static int STRAIGHT = 1;
    public static int ERASE = 2;
    public static double RADIUS_SQUARE = 2500;

    public Canvas canvas;
    public Paint paint, bitmap_paint;
    private Path draw_path;
    private Bitmap bitmap, temp_bitmap;
    float x, y;
    public int mode;
    private boolean show_temp = false, end_attached = false;
    public boolean intel = false;
    private ArrayList<Point> end_points = new ArrayList<>();

    public PaintView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mode = CASUAL;
        setBackgroundColor(Color.WHITE);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        paint.setStrokeWidth(4f);
        paint.setAntiAlias(true);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        bitmap_paint = new Paint(Paint.DITHER_FLAG);
        // post() is used to create a bitmap corresponding to the size of this view
        post(
                new Runnable() {
                    @Override
                    public void run() {
                        int width = getWidth();
                        int height = getHeight();
                        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                        canvas = new Canvas(bitmap);
                    }
                }
        );
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bitmap != null) {
            if (mode == STRAIGHT && show_temp) {
                canvas.drawBitmap(temp_bitmap, 0, 0, bitmap_paint);
                show_temp = false;
            }
            else
                canvas.drawBitmap(bitmap, 0, 0, bitmap_paint);
            if (draw_path != null) {
                canvas.drawPath(draw_path, paint);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mode == CASUAL)
            return casualEvent(event);
        else if (mode == STRAIGHT)
            return straightEvent(event);
        else if (mode == ERASE)
            return eraseEvent(event);
        else
            return true;
    }

    public boolean casualEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            draw_path = new Path();
            x = event.getX();
            y = event.getY();
            draw_path.moveTo(x, y);
        }
        else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            x = event.getX();
            y = event.getY();
            draw_path.lineTo(x, y);
        }
        else if (event.getAction() == MotionEvent.ACTION_UP) {
            canvas.drawPath(draw_path, paint);
            draw_path.reset();
            draw_path = null;
        }
        invalidate();
        return true;
    }

    public boolean straightEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            // mark the start point
            x = event.getX();
            y = event.getY();
            Point attached = null;
            if (intel) {
                attached = attach(x, y);
            }
            if (attached != null) {
                x = attached.x;
                y = attached.y;
            }
            else
                end_points.add(new Point(x, y));
        }
        else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            // reset the path if not null
            if (draw_path != null) {
                draw_path.reset();
                draw_path = null;
            }
            draw_path = new Path();
            draw_path.moveTo(x, y);
            float temp_x = event.getX();
            float temp_y = event.getY();
            Point attached = null;
            if (intel) {
                attached = attach(temp_x, temp_y);
            }
            if (attached != null) {
                draw_path.lineTo(attached.x, attached.y);
                end_attached = true;
            }
            else {
                draw_path.lineTo(temp_x, temp_y);
                end_attached = false;
            }
            // show temp bitmap instead of real one
            temp_bitmap = bitmap.copy(bitmap.getConfig(), bitmap.isMutable());
            show_temp = true;
            //canvas.drawPath(draw_path, paint);
        }
        else if (event.getAction() == MotionEvent.ACTION_UP && draw_path != null) {
            canvas.drawPath(draw_path, paint);
            if (!end_attached)
                end_points.add(new Point(event.getX(), event.getY()));
        }
        invalidate();
        return true;
    }

    public boolean eraseEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            draw_path = new Path();
            x = event.getX();
            y = event.getY();
            draw_path.moveTo(x, y);
        }
        else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            x = event.getX();
            y = event.getY();
            draw_path.lineTo(x, y);
        }
        else if (event.getAction() == MotionEvent.ACTION_UP) {
            canvas.drawPath(draw_path, paint);
            draw_path.reset();
            draw_path = null;
        }
        invalidate();
        return true;
    }

    public void clearAll() {
        if (draw_path != null) {
            draw_path.reset();
            draw_path = null;
        }
        canvas.drawColor(Color.WHITE);
        end_points.clear();
        invalidate();
    }

    public void setMode(int mode) {
        if (mode == CASUAL || mode == STRAIGHT)
            paint.setColor(Color.BLACK);
        else if (mode == ERASE) {
            paint.setColor(Color.WHITE);
        }
        if (mode == STRAIGHT && this.mode == mode)
            intel = !intel;
        this.mode = mode;
    }

    private Point attach(float x, float y) {
        double min = RADIUS_SQUARE;
        int argmin = -1;
        for (int i = 0; i < end_points.size(); i++) {
            double distance = Math.pow(x - end_points.get(i).x, 2) + Math.pow(y - end_points.get(i).y, 2);
            if (distance < min) {
                min = distance;
                argmin = i;
            }
        }
        if (min < RADIUS_SQUARE)
            return end_points.get(argmin);
        return null;
    }
}

