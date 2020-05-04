package edu.sjtu.zhusy54.qrcode;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            Bundle bundle = data.getExtras();
            String scanResult = bundle.getString("result");
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Decoding result")
                    .setMessage(scanResult)
                    .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button encoder = (Button)findViewById(R.id.btn_encoder);
        Button decoder = (Button)findViewById(R.id.btn_decoder);
        View.OnClickListener myListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent;
                switch (v.getId()){
                    case R.id.btn_encoder:
                        intent = new Intent(MainActivity.this, TestEncoder.class);
                        startActivity(intent);
                        break;
                    case R.id.btn_decoder:
                        intent = new Intent(MainActivity.this, TestDecoder.class);
                        startActivityForResult(intent, 0);
                        break;
                    default:
                        break;
                }
            }
        };
        encoder.setOnClickListener(myListener);
        decoder.setOnClickListener(myListener);
    }
}
