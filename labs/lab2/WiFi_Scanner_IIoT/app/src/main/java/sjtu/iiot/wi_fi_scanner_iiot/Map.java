package sjtu.iiot.wi_fi_scanner_iiot;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.View;

import java.util.LinkedList;

public class Map extends View {
    private Bitmap bitmap;
    public Canvas canvas;
    public Paint paint;
    public LinkedList<Node> nodes;
    private int width, height;

    public Map(Context context, AttributeSet attrs) {
        super(context, attrs);
        setBackgroundColor(Color.WHITE);
        paint = new Paint(Paint.DITHER_FLAG);
        setWillNotDraw(false);
        post(new Runnable() {
            @Override
            public void run() {
                width = getWidth();
                height = getHeight();
                bitmap = Bitmap.createBitmap(width, height,
                        Bitmap.Config.ARGB_8888);
                canvas = new Canvas(bitmap);
            }
        });
    }

    public void setNodes(LinkedList<Node> node_list) {
        nodes = new LinkedList<>(node_list);
        invalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawNodes(nodes);
        if (bitmap != null) {
            canvas.drawBitmap(bitmap, 0, 0, paint);
        }
    }

    public void drawNodes(LinkedList<Node> nodes) {
        // need to modify
        double x2 = nodes.get(1).x; // need to modify
        for (int i = 0; i < nodes.size(); i++) {
            if (i == 3) {
                paint.setColor(Color.BLUE);
            }
            else if (i == 4) {
                paint.setColor(Color.CYAN);
            }
            else if (i == 5) {
                paint.setColor(Color.MAGENTA);
            }
            drawOnCanvas(nodes.get(i), x2);
            paint.setColor(Color.BLACK);
        }
    }

    private void drawOnCanvas(Node node, double x2) {
        Pair<Float, Float> coord = mapKmToPx(node.x, node.y, x2);
        canvas.drawCircle(coord.first, coord.second,20, paint);
    }

    private Pair<Float, Float> mapKmToPx(double x, double y, double x2) {
        float px = (float) (4 * width / (5 * x2) * x + width / 10.0);
        float py = (float) (-4 * width/ (5 * x2) * y + 9 * height / 10.0);
        return new Pair<>(px, py);
    }
}
