package sjtu.iiot.wi_fi_scanner_iiot;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;

import java.util.LinkedList;
import java.util.Vector;
import android.app.Activity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {
    private SuperWiFi rss_scan =null;
    Vector<String> RSSList = null;
    private String testlist=null;
    public static int testID = 0;//The ID of the test result
    Positioning positioner;
    Map map;
    boolean simulated = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final EditText ipText = findViewById(R.id.ipText);//The textlist of the average of the result
        final Button changactivity = findViewById(R.id.button1);//The start button
        final Button cleanlist = findViewById(R.id.button2);//Clear the textlist
        final Button localize_button = findViewById(R.id.button_position);
        verifyStoragePermissions(this);
        rss_scan=new SuperWiFi(this);
        testlist="";
        testID=0;
        positioner = new Positioning();

        map = findViewById(R.id.map);
        map.setNodes(positioner.APs_for_beta);

        TextView true_coord = findViewById(R.id.coord_true);
        true_coord.setText(
                "truth ("+positioner.device.x+", "+positioner.device.y+")");
        true_coord.setTextColor(Color.CYAN);
        map.setNodes(positioner.APs_and_device);

        changactivity.setOnClickListener(new Button.OnClickListener(){
            public void onClick(View v) {
                testID = testID + 1;
                rss_scan.ScanRss();
                while(rss_scan.isscan()){//Wait for the end
                }
                RSSList=rss_scan.getRSSlist();//Get the test result
                final EditText ipText = findViewById(R.id.ipText);
                testlist=testlist+"testID:"+testID+"\n"+RSSList.toString()+"\n";
                ipText.setText(testlist);//Display the result in the textlist
            }
        });
        cleanlist.setOnClickListener(new Button.OnClickListener(){
            public void onClick(View v) {
                testlist="";
                ipText.setText(testlist);//Clear the textlist
                testID=0;
            }
        });
        localize_button.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View view) {
                localize();
            }
        });
    }

    private void localize() {
        Node posi;
        if (simulated) {
            posi = positioner.position();
        }
        else {
            posi = positioner.position(rss_scan.a_hats);
        }
        TextView posi_coord = findViewById(R.id.coord_esti);
        posi_coord.setText("estimated ("+String.format("%.3f", posi.x)+", " +
                String.format("%.3f", posi.y) + ")");
        posi_coord.setTextColor(Color.MAGENTA);
        map.setNodes(positioner.all_nodes);
    }

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_MULTICAST_STATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_FINE_LOCATION };
    /**
     * Checks if the app has permission to write to device storage
     * If the app does not has permission then the user will be prompted to
     * grant permissions
     * @param activity
     */
    public static void verifyStoragePermissions(Activity activity) {
// Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
// We don't have permission so prompt the user
            ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE);
        }
    }
}