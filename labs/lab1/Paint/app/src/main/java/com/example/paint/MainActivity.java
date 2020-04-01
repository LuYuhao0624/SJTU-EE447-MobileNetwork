package com.example.paint;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import static com.example.paint.PaintView.CASUAL;
import static com.example.paint.PaintView.ERASE;
import static com.example.paint.PaintView.STRAIGHT;

public class MainActivity extends AppCompatActivity {

    PaintView paint_view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        paint_view = findViewById(R.id.paint_view);


        Button button_casual = findViewById(R.id.mode_casual);
        button_casual.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                paint_view.setMode(CASUAL);
            }
        });

        final Button button_straight = findViewById(R.id.mode_straight);
        button_straight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                paint_view.setMode(STRAIGHT);
                if (paint_view.intel)
                    button_straight.setText(R.string.mode_intel_straight);
                else
                    button_straight.setText(R.string.mode_straight);
            }
        });

        Button button_erase = findViewById(R.id.mode_erase);
        button_erase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                paint_view.setMode(ERASE);
            }
        });
        button_erase.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                paint_view.clearAll();
                return true;
            }
        });
    }


}
