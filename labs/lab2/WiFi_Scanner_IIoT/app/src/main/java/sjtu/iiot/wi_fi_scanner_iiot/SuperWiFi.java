package sjtu.iiot.wi_fi_scanner_iiot;

/*****************************************************************************************************************
 * Created by HelloShine on 2019-3-24.
 * ***************************************************************************************************************/
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log; //Log can be utilized for debug.

public class SuperWiFi extends MainActivity{

    /*****************************************************************************************************************
     * When you run the APP in your mobile phone, you can utilize the following code for debug:
     * Log.d("TEST_INFO","Your Own String Type Content Here");
     * You can also generate the String via ("String" + int/double value). for example, "CurTime " + 20 = "CurTime 20"
     * ***************************************************************************************************************/
    private String FileLabelName = "MyPos";// Define the file Name
    /*****************************************************************************************************************
     * You can define the Wi-Fi SSID to be measured in FileNameGroup, more than 2 SSIDs are OK.
     * It is noting that multiple Wi-Fi APs might share the same SSID such as SJTU.
     * ***************************************************************************************************************/
    private String FileNameGroup[] = {"56-303", "TP-LINK_F6B7", "TP-LINK_5G_F6B7"};

    private int TestTime = 10;//Number of measurement
    private int ScanningTime = 1000;//Wait for (?) ms for next scan

    private int NumberOfWiFi = FileNameGroup.length;

    // RSS_Value_Record and RSS_Measurement_Number_Record are used to record RSSI values
    private int[] RSS_Value_Record = new int[NumberOfWiFi];
    private int[] RSS_Measurement_Number_Record = new int[NumberOfWiFi];


    private WifiManager mWiFiManager = null;
    private Vector<String> scanned = null;
    boolean isScanning = false;

    public SuperWiFi(Context context)
    {
        this.mWiFiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        this.scanned = new Vector<String>();
    }

    private void startScan()//The start of scanning
    {
        this.isScanning = true;
        Thread scanThread = new Thread(new Runnable()
        {
            public void run() {
                scanned.clear();//Clear last result
                for(int index = 1;index <= NumberOfWiFi; index++){
                    RSS_Value_Record[index - 1] = 0;
                    RSS_Measurement_Number_Record[index - 1] = 1;
                }
                int CurTestTime = 1; //Record the test time and write into the SD card
                SimpleDateFormat formatter = new SimpleDateFormat
                        ("yyyy-MM-dd HH:mm:ss");
                Date curDate = new Date(System.currentTimeMillis()); //Get the current time
                String CurTimeString = formatter.format(curDate);
                for(int index = 1;index <= NumberOfWiFi; index++){
                    write2file(FileLabelName + "-" + FileNameGroup[index - 1] + ".txt","Test_ID: " + testID + " TestTime: " + CurTimeString + " BEGIN\r\n");
                }
                //Scan for a certain times
                while(CurTestTime++ <= TestTime) performScan();

                for(int index = 1;index <= NumberOfWiFi; index++){//Record the average of the result
                    scanned.add(FileLabelName + "-" + FileNameGroup[index - 1] + " = " + RSS_Value_Record[index - 1]/ RSS_Measurement_Number_Record[index - 1] + "\r\n");
                }
                /*****************************************************************************************************************

                 You can insert your own code here for localization.

                 * ***************************************************************************************************************/
                for(int index = 1;index <= NumberOfWiFi; index++){//Mark the end of the test in the file
                    write2file(FileLabelName + "-" + FileNameGroup[index - 1] + ".txt","testID:"+testID+"END\r\n");
                }
                isScanning=false;
            }
        });
        scanThread.start();
    }


    private void performScan()//The realization of the test
    {
        if(mWiFiManager == null)
            return;
        try
        {
            if(!mWiFiManager.isWifiEnabled())
            {
                mWiFiManager.setWifiEnabled(true);
            }
            mWiFiManager.startScan();//Start to scan
            try {
                Thread.sleep(ScanningTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            this.scanned.clear();
            List<ScanResult> sr = mWiFiManager.getScanResults();
            Iterator<ScanResult> it = sr.iterator();
            while(it.hasNext())
            {
                ScanResult ap = it.next();
                for(int index = 1;index <= FileNameGroup.length; index++){
                    if (ap.SSID.equals(FileNameGroup[index - 1])){//Write the result to the file
                        RSS_Value_Record[index-1] = RSS_Value_Record[index-1] + ap.level;
                        RSS_Measurement_Number_Record[index - 1]++;
                        write2file(FileLabelName + "-" + FileNameGroup[index - 1] + ".txt",ap.level+"\r\n");
                    }
                }
            }
        }
        catch (Exception e)
        {
            this.isScanning = false;
            this.scanned.clear();
        }
    }




    public void ScanRss(){
        startScan();
    }
    public boolean isscan(){
        return isScanning;
    }
    public Vector<String> getRSSlist(){
        return scanned;
    }

    private void write2file(String filename, String a){//Write to the SD card
        try {
            File file = new File("/sdcard/"+filename);
            if (!file.exists()){
                file.createNewFile();} // Open a random filestream by Read&Write
            RandomAccessFile randomFile = new
                    RandomAccessFile("/sdcard/"+filename, "rw"); // The length of the file(byte)
            long fileLength = randomFile.length(); // Put the writebyte to the end of the file
            randomFile.seek(fileLength);
            randomFile.writeBytes(a);
            randomFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}