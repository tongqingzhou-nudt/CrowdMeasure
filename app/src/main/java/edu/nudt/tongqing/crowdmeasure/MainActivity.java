package edu.nudt.tongqing.crowdmeasure;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.net.wifi.WifiManager.WIFI_STATE_ENABLED;

public class MainActivity extends AppCompatActivity{

    private static final int PHOTO_REQUEST_GALLERY = 2;
    //private static final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 3;
    //private static final int REQUEST_CODE_ACCESS_WIFI_STATE = 4;
    private mapView floor_plan_view;
    private Button measuring_button;
    private Button loading_button;

    private WifiManager mWiFiManager;
    private CharSequence AP_prefix = null;
    private int measure_cycles_per_loc = 10;    //默认测量次数为10次，会进行动态的调整
    private String strFileNamePrefix = "CMResults";
    private int measureRound = 1;
    private int lastLoc = 0;

    private Handler mHandler;
    private HandlerThread mThread;
    private int progress = 1;
    private Handler mMsgHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.floor_plan_view = (mapView) this.findViewById(R.id.floor_plan);
        this.measuring_button = (Button) this.findViewById(R.id.measuring);
        //加载floor plan之后才生效
        this.measuring_button.setEnabled(false);
        this.loading_button = (Button) this.findViewById(R.id.loading_floor_plan);
        /*if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_WIFI_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_WIFI_STATE},
                    REQUEST_CODE_ACCESS_WIFI_STATE);
            Log.i("MainActivity","request the wifi permission");
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CODE_WRITE_EXTERNAL_STORAGE);
            Log.i("MainActivity","request the storage permission");
        }*/
        Log.i("MainActivity","building");
        this.mWiFiManager = (WifiManager) this.getSystemService(this.WIFI_SERVICE);

        mThread = new HandlerThread("rssMeasure");
        mThread.start();
        mHandler = new Handler(mThread.getLooper());

        //处理测量线程中对Activity UI的更新——1) 更新测量button的text；2) 更新地图view的标记
        mMsgHandler = new Handler(){
            public void handleMessage(Message msg){
                super.handleMessage(msg);
                int tmp = (Integer)msg.obj;
                measuring_button.setText( "MEASURING" + "(" + Integer.toString(tmp) + "%)");
                if(tmp == measure_cycles_per_loc){
                    //测量结束，测量按钮重新置为可用,并将其text改为初始值
                    measuring_button.setEnabled(true);
                    measuring_button.setText( "MEASURE");
                    loading_button.setEnabled(true);   //可重新加载map
                    //测量结束，可以重新进行位置标定
                    floor_plan_view.setViewLock(false);
                    //针对该位置的测量成功，使用state = 1进行待测量标记清除
                    // 再次点击测量按钮需要重新标定一个位置
                    floor_plan_view.clearunHandleFlag(1);

                    lastLoc = floor_plan_view.getCurrentLoc();
                    Toast.makeText(MainActivity.this, "Measuring task for AP " + AP_prefix + "* succeed", Toast.LENGTH_SHORT).show();
                }
            }
        };
    }


    //授权结果处理
    /*
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ACCESS_WIFI_STATE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //授权成功，直接操作
                } else {
                    Log.w("MainActivity","wifi request denied");
                }
                break;
            }
            case REQUEST_CODE_WRITE_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //授权成功，直接操作
                } else {
                    Log.w("MainActivity","storage request denied");
                }
                break;
            }
        }
    }*/

    //show the picture gallery and choose one as the floor plan
    public void onLoading(View view){
        /*if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Toast.makeText(this, "Please grant the permission this time", Toast.LENGTH_LONG).show();
            }
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CODE_WRITE_EXTERNAL_STORAGE);
            Log.i("MainActivity","request the storage permission");
        }else{
            Log.i("mainActivity","write external already granted");
        }*/

        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");

        //重新加载map时，将所有标记还原为初始值
        this.floor_plan_view.setViewLock(false);
        this.floor_plan_view.clearunHandleFlag(1);

        startActivityForResult(intent,PHOTO_REQUEST_GALLERY);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if (requestCode == PHOTO_REQUEST_GALLERY) {
            if(data != null){
                //Bitmap floor_plan_image = data.getParcelableExtra("data");
                try{
                    Bitmap floor_plan_image = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData());
                    /*int dstHeight;
                    int dstWidth;
                    float ratio_of_view = (float) this.floor_plan_view.getHeight()/(float) this.floor_plan_view.getWidth();
                    float ratio_of_bitmap = (float) floor_plan_image.getHeight()/(float) floor_plan_image.getWidth();
                    if(ratio_of_view < ratio_of_bitmap){
                        dstHeight =  this.floor_plan_view.getHeight();
                        dstWidth = (int)((float) dstHeight/ratio_of_bitmap);
                    }
                    else{
                        dstWidth =  this.floor_plan_view.getWidth();
                        dstHeight = (int)((float) dstWidth * ratio_of_bitmap);
                    }
                    Bitmap floor_plan_image_scaled = Bitmap.createScaledBitmap(floor_plan_image,dstWidth,dstHeight,false);*/
                    this.floor_plan_view.setImageBitmap(floor_plan_image);
                    Log.i("mainActivity","setting complete");
                    this.floor_plan_view.setScaleType(ImageView.ScaleType.FIT_START);
                    this.measuring_button.setEnabled(true);
                }catch(Exception e){
                    Log.e("mainActivity","convert error: "+ e.getMessage());
                }
                //this.floor_plan_view.setImageBitmap(floor_plan_image);
                //this.image_view.setImageBitmap(floor_plan_image);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        mHandler.removeCallbacks(mRunnable);
        super.onDestroy();
    }
    @Override
    protected void onResume() {
        super.onResume();
    }
    @Override
    protected void onStop() {
        super.onStop();
    }

    //进行目标AP rss测量的线程
    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            //while (mRunning) {
                Calendar c = Calendar.getInstance();
                String timestamp = Integer.toString(c.get(Calendar.YEAR))
                        + "-" + Integer.toString(c.get(Calendar.MONTH))
                        + "-" + Integer.toString(c.get(Calendar.DAY_OF_MONTH))
                        + "-" + Integer.toString(c.get(Calendar.HOUR_OF_DAY));

                Map<String, Integer> ssid_to_id = new HashMap<String, Integer>();
                int numbered_ap = 0;

                if(floor_plan_view.getCurrentLoc() == lastLoc){
                    measureRound = measureRound + 1;
                }else {
                    measureRound = 1;
                }

                for (int i = 0; i < measure_cycles_per_loc; i++) {
                    // sleep for 1 sec, then measure, then store
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {
                        Toast.makeText(MainActivity.this, "thread.sleep error", Toast.LENGTH_SHORT).show();
                    }

                    if(mWiFiManager.startScan()) {
                        List<ScanResult> mWiFiManagerScanResults = mWiFiManager.getScanResults();
                        for (int index = 0; index < mWiFiManagerScanResults.size(); index++) {
                            ScanResult sample = mWiFiManagerScanResults.get(index);
                            if (sample.SSID.toUpperCase().contains(AP_prefix)) {
                                //如果无此ssid的映射条目，为其增加一个映射关系
                                if (!ssid_to_id.containsKey(sample.SSID)) {
                                    numbered_ap = numbered_ap + 1;
                                    ssid_to_id.put(sample.SSID, numbered_ap);
                                }
                                Log.i("WiFi_INFO", sample.SSID + Integer.toString(sample.level));
                                //将记录存储到本地
                                store_Measurement_to_Local(ssid_to_id.get(sample.SSID), sample, timestamp);
                            }
                        }
                        //显示测量进度，无法在线程中进行UI的更新，所以需要使用handler将消息传递至主进程中处理
                        progress = i + 1;
                        Message msg = new Message();
                        msg.obj = progress;
                        mMsgHandler.sendMessage(msg);
                    }else{
                        Log.i("WiFi_INFO", "WiFiManager.startscan failure");
                    }
                }
            }
        //}
    };

    public void onMeasuring(View view){
         //判断测量参数是否配置
        // 如果没有，则弹窗请求prefix和测量cycles的输入
        if(this.AP_prefix == null){
            //final EditText inputPrefix = new EditText(this);
            final View dialogView = getLayoutInflater().inflate(R.layout.dialog_prefix_cycles,(ViewGroup) findViewById(R.id.dialog_para));
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Enter the AP prefix").setIcon(android.R.drawable.ic_dialog_info).setView(dialogView)
                    .setNegativeButton("Cancel", null);
            builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                    //AP_prefix = inputPrefix.getText().toString().toUpperCase();
                    EditText mPrefix = (EditText) dialogView.findViewById(R.id.AP_prefix_et);
                    EditText mCycles = (EditText) dialogView.findViewById(R.id.measuring_cycles_et);
                    AP_prefix = mPrefix.getText().toString().toUpperCase();
                    measure_cycles_per_loc = Integer.parseInt(mCycles.getText().toString());
                    Log.i("mainActivity", "Input Parse succeeds. Prefix: "+ AP_prefix + "/ Cycles: " + Integer.toString(measure_cycles_per_loc));
                }
            });
            builder.show();
        }else {//已经完成测量参数的配置
            //判断是否有未处理的标定点
            if (this.floor_plan_view.getCurrentLoc() != 0) {
                //锁住mapView，测量期间不支持点击
                this.floor_plan_view.setViewLock(true);
                //判断wifi是否可用
                if (this.mWiFiManager.getWifiState() == WIFI_STATE_ENABLED) {
                    List<ScanResult> mWiFiManagerScanResults = this.mWiFiManager.getScanResults();
                    //扫描到的无线热点的个数
                    //Log.i("mainActivity",Integer.toString(mWiFiManagerScanResults.size()));
                    //判断扫描的wifi列表是否为空
                    if (!mWiFiManagerScanResults.isEmpty()) {
                        //如果不为空，则可以开展测量，完成之前两个按钮均不可点击
                        this.measuring_button.setEnabled(false);
                        this.loading_button.setEnabled(false);

                        int size = mWiFiManagerScanResults.size();
                        boolean target_AP_found = false;
                        for (int index = 0; index < size; index++) {
                            ScanResult sample = mWiFiManagerScanResults.get(index);
                            //如果发现目标AP，则进入正式的测量
                            if (sample.SSID.toUpperCase().contains(AP_prefix)) {
                                target_AP_found = true;
                                //rss_Measure();
                                mHandler.post(mRunnable);
                                break;
                            }
                        }
                        if (!target_AP_found) {
                            Toast.makeText(MainActivity.this, "未扫描到接入点" + this.AP_prefix + "*", Toast.LENGTH_SHORT).show();
                            //可能是AP前缀输入错误，将其置空，下次点击重新输入
                            this.AP_prefix = null;
                            //未为扫描到目标接入点，可能需要换个位置进行测量，所以解锁位置标定
                            this.floor_plan_view.setViewLock(false);
                            //针对该位置的测量无效，使用state = 0进行待测量标记清除
                            this.floor_plan_view.clearunHandleFlag(0);
                            //恢复测量button为可用，以供再次测量
                            this.measuring_button.setEnabled(true);
                            this.loading_button.setEnabled(true);   //可重新加载map
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "未扫描到无线接入点", Toast.LENGTH_SHORT).show();
                        //未扫描到任何接入点，可能需要换个位置进行测量，所以解锁位置标定
                        this.floor_plan_view.setViewLock(false);
                        //针对该位置的测量无效，使用state = 0进行待测量标记清除
                        this.floor_plan_view.clearunHandleFlag(0);
                    }
                } else {
                    Toast.makeText(MainActivity.this, "请打开wifi开关", Toast.LENGTH_SHORT).show();
                    //开启开关后，针对该位置进行重新测量
                    //所以，不需要解锁位置标定，待测量标记也不清除
                }
            } else {
                Toast.makeText(MainActivity.this, "未发现测量点", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void store_Measurement_to_Local(int id, ScanResult sample, String timestamp){
        //将写入的数据汇总至一个String中
        //String measure_item = sample.SSID + "," + sample.BSSID + "," + Integer.toString(sample.level) + "," + timestamp;
        String measure_item = Integer.toString(sample.level);//仅记录信号强度
        measure_item = measure_item + " ";
        boolean newFile = false;

        try {
            //// TODO: 2016/10/16 在文件名上加测量位置
            //getExternalFilesDir(null)获得本应用在android文件夹下的根目录
            //目录是/storage/emulated/0/Android/data/edu.nudt.tongqing.crowdmeasure/files/
            //通过/mnt/sdcard等方式无法访问，必须使用上述目录才可以
            //格式path/CMResults_AP_?_Loc_?
            //String strFileName = getExternalFilesDir(null) + "/" + strFileNamePrefix + "_AP_" + Integer.toString(id) + "_Loc_" + Integer.toString(measureRound);
            //String strFileName = getExternalFilesDir(null) + "/" + AP_prefix + "_" + Integer.toString(id) + "_Loc_" + Integer.toString(measureRound);
            int currentLoc = this.floor_plan_view.getCurrentLoc();
            String strFileName = getExternalFilesDir(null) + "/" + sample.SSID + "_at_" + Integer.toString(currentLoc) + "_" + Integer.toString(measureRound);
            File file = new File(strFileName);
            if (!file.exists()) {
                Log.i("LOG_MEASUREMENT", "Create the file:" + strFileName);
                if(!file.createNewFile()) {
                    Log.i("LOG_MEASUREMENT", "Create file fails");
                }
                newFile = true;
            }
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            raf.seek(file.length());
            //添加说明行
            if(newFile) {
                //String firstColumn = "SSID," + "BSSID,"+ "RSS(dBm)," + "timestamp" + "\r\n";
                String firstColumn = sample.BSSID + "\t" + timestamp + "\r\n";
                raf.write(firstColumn.getBytes());
            }
            raf.write(measure_item.getBytes());
            raf.close();
        } catch (Exception e) {
            Log.e("LOG_MEASUREMENT", "Create file fails");
        }
    }
}