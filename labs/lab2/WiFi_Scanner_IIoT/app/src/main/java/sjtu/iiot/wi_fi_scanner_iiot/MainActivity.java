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
    LinkedList<Node> nodes;
    Map map;

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

        /* change settings here */
        initAPs(0, 0,        // coord A1, do not modify this,
                0.12, 0,     // coord A2, do not modify y2, i.e. 0
                0.06, 0.13,  // coord A3,
                0.08, 0.07); // coord A4
        /* change settings here */

        map = findViewById(R.id.map);
        map.setNodes(nodes);
        final LinkedList<Node> APs = new LinkedList<>(nodes);

        /* change settings here */
        final Node device = new Node(0.03, 0.1); // coord device
        /* change settings here */

        TextView true_coord = findViewById(R.id.coord_true);
        true_coord.setText("truth ("+device.x+", "+device.y+")");
        true_coord.setTextColor(Color.CYAN);
        nodes.add(device);
        map.setNodes(nodes);

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
                localize(APs, device);
            }
        });
    }

    private void initAPs(double x1, double y1, double x2, double y2,
                         double x3, double y3, double x4, double y4) {
        nodes = new LinkedList<>();
        nodes.add(new Node(x1, y1));
        nodes.add(new Node(x2, y2));
        nodes.add(new Node(x3, y3));
        nodes.add(new Node(x4, y4));
    }

    private void localize(LinkedList<Node> APs, Node device) {
        if (nodes.size() == 6) {
            nodes.removeLast();
        }
        Node posi = positioner.position(APs, device);
        TextView posi_coord = findViewById(R.id.coord_esti);
        posi_coord.setText("estimated ("+String.format("%.3f", posi.x)+", " +
                String.format("%.3f", posi.y) + ")");
        posi_coord.setTextColor(Color.MAGENTA);
        nodes.add(posi);
        map.setNodes(nodes);
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