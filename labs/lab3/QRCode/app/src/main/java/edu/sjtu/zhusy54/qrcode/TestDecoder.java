package edu.sjtu.zhusy54.qrcode;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import java.util.List;

/**
 * Created by Syman-Z on 2016/2/25.
 */
public class TestDecoder extends Activity implements SurfaceHolder.Callback {
    // 定义对象
    private SurfaceView mSurfaceview = null;  // SurfaceView对象：(视图组件)视频显示
    private SurfaceHolder mSurfaceHolder = null;  // SurfaceHolder对象：(抽象接口)SurfaceView支持类
    private Camera mCamera =null;     // Camera对象，相机预览
    private boolean bIfPreview=false, storeFlag=true, stored=false;
    private int mPreviewWidth=720, mPreviewHeight=1280, colorNum, unitC;
//    private int mPreviewWidth=480, mPreviewHeight=800, colorNum, unitC;

    //用于接收解码好的数据
    private Handler uiHandler;
    private Bundle mBundle;
    private boolean isFirstFrame=true;
    private byte[] tmpBytes;
    private int frameNum, totalFrm, byteLenth, frameCnt=0;
    //	Map<Integer, byte[]> myMap  = new SparseArray<Integer, byte[]>();
//	SparseArray<int[]> myMap = new SparseArray<int[]>();
    SparseArray<byte[]> myMap = new SparseArray<byte[]>();
    public static final int REQUEST_ALBUM_CODE = 0X01;



    // InitSurfaceView
    private void initSurfaceView()
    {
        mSurfaceview = (SurfaceView) this.findViewById(R.id.preview_view);
        mSurfaceHolder = mSurfaceview.getHolder(); // 绑定SurfaceView，取得SurfaceHolder对象
        mSurfaceHolder.addCallback(TestDecoder.this); // SurfaceHolder加入回调接口
        //mSurfaceHolder.setFixedSize(720, 1280); // 预览大小設置
//	    mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);// 設置顯示器類型，setType必须设置
        mSurfaceHolder.setFormat(PixelFormat.TRANSPARENT);
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //无title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //设置全屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.decoder);

        initSurfaceView();
        View button = createButton(533, "QRcode");
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                open_album();
            }
        });
        addContentView(button, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
    }

    /*【SurfaceHolder.Callback 回调函数】*/
    public void surfaceCreated(SurfaceHolder holder)
    // SurfaceView启动时/初次实例化，预览界面被创建时，该方法被调用。
    {
        mCamera = Camera.open();// 开启摄像头（2.3版本后支持多摄像头,需传入参数）
        try
        {
            Log.i("TAG", "SurfaceHolder.Callback：surface Created");
            mCamera.setPreviewDisplay(mSurfaceHolder);//set the surface to be used for live preview
        }catch (Exception ex)
        {
            if(null != mCamera)
            {
                mCamera.release();
                mCamera = null;
            }
            Log.i("TAG"+"initCamera", ex.getMessage());
        }
        mCamera.setPreviewCallback(new Camera.PreviewCallback(){

            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
//                try {
//                    System.out.println("decoding frame!!!");
//                    MultiFormatReader formatReader = new MultiFormatReader();
//                    LuminanceSource source = new PlanarYUVLuminanceSource(data, 720, 1280, 60, 340, 600, 600, false);
//                    Binarizer binarizer = new HybridBinarizer(source);
//                    BinaryBitmap binaryBitmap = new BinaryBitmap(binarizer);
//                    Map hints = new HashMap();
//                    hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
//                    Result result = formatReader.decode(binaryBitmap, hints);
//                    Intent resultIntent = new Intent();
//                    Bundle bundle = new Bundle();
//                    bundle.putString("result", result.toString());
//                    resultIntent.putExtras(bundle);
//                    TestDecoder.this.finish();
////                    this.setResult(RESULT_OK, resultIntent);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
                int previewWidth = camera.getParameters().getPreviewSize().width;
                int previewHeight = camera.getParameters().getPreviewSize().height;

                PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(
                        data, previewWidth, previewHeight, 0, 0, previewWidth,
                        previewHeight, false);
                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

                Reader reader = new QRCodeReader();
                // Reader reader = new MultiFormatReader();
                try {

                    Result result = reader.decode(bitmap);
                    String text = result.getText();

                    Intent intent = new Intent();
                    Bundle bundle = new Bundle();
                    bundle.putString("result", result.toString());
                    intent.putExtras(bundle);
                    setResult(RESULT_OK, intent);
                    finish();
                } catch (Exception e) {
                    e.printStackTrace();
//                    Toast.makeText(getApplicationContext(), "Not Found", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    // 当SurfaceView/预览界面的格式和大小发生改变时，该方法被调用
    {
        Log.i("TAG", "SurfaceHolder.Callback：Surface Changed");
        //mPreviewHeight = height;
        //mPreviewWidth = width;
        initCamera();
        mCamera.cancelAutoFocus();//只有加上了这一句，才会自动对焦。
    }
    public void surfaceDestroyed(SurfaceHolder holder)
    // SurfaceView销毁时，该方法被调用
    {
        Log.i("TAG", "SurfaceHolder.Callback：Surface Destroyed");
        if(null != mCamera)
        {
            mCamera.setPreviewCallback(null); //！！这个必须在前，不然退出出错
            mCamera.stopPreview();
            bIfPreview = false;
            mCamera.release();
            mCamera = null;
        }
    }

    /*【2】【相机预览】*/
    private void initCamera()//surfaceChanged中调用
    {
        Log.i("TAG", "going into initCamera");
        if (bIfPreview)
        {
            mCamera.stopPreview();//stopCamera();
        }
        if(null != mCamera)
        {
            try
            {
		        /* Camera Service settings*/
                Camera.Parameters parameters = mCamera.getParameters();
                // parameters.setFlashMode("off"); // 无闪光灯
                parameters.setPictureFormat(PixelFormat.JPEG); //Sets the image format for picture 设定相片格式为JPEG，默认为NV21
                parameters.setPreviewFormat(PixelFormat.YCbCr_420_SP); //Sets the image format for preview picture，默认为NV21
		        /*【ImageFormat】JPEG/NV16(YCrCb format，used for Video)/NV21(YCrCb format，used for Image)/RGB_565/YUY2/YU12*/

                // 【调试】获取camera支持的PictrueSize，看看能否设置？？
                List<Camera.Size> pictureSizes = mCamera.getParameters().getSupportedPictureSizes();
                List<Camera.Size> previewSizes = mCamera.getParameters().getSupportedPreviewSizes();
                List<Integer> previewFormats = mCamera.getParameters().getSupportedPreviewFormats();
                List<String> focusModes = parameters.getSupportedFocusModes();
//		        List<Integer> previewFrameRates = mCamera.getParameters().getSupportedPreviewFrameRates();
                Log.i("TAG"+"initCamera", "cyy support parameters is ");
                Camera.Size psize = null;
                for (int i = 0; i < pictureSizes.size(); i++)
                {
                    psize = pictureSizes.get(i);
                    Log.i("TAG"+"initCamera", "PictrueSize,width: " + psize.width + " height" + psize.height);
                }
                for (int i = 0; i < previewSizes.size(); i++)
                {
                    psize = previewSizes.get(i);
                    Log.i("TAG"+"initCamera", "PreviewSize,width: " + psize.width + " height" + psize.height);
                }
                Integer pf = null;
                for (int i = 0; i < previewFormats.size(); i++)
                {
                    pf = previewFormats.get(i);
                    Log.i("TAG"+"initCamera", "previewformates:" + pf);
                }
                String fm;
                for (int i = 0; i < focusModes.size(); i++)
                {
                    fm = focusModes.get(i);
                    Log.i("TAG"+"initCamera", "previewformates:" + fm);
                }

//		        List<int[]> supportedPreviewFpsRange = mCamera.getParameters().getSupportedPreviewFpsRange();
                List<Integer> supportedPreviewFrameRates = mCamera.getParameters().getSupportedPreviewFrameRates();
                int fr;
                for (int i = 0; i < supportedPreviewFrameRates.size(); i++)
                {
                    fr = supportedPreviewFrameRates.get(i);
                    Log.i("TAG"+"initCamera", "previewformates:" + fr);
                }
                List<int[]> range=mCamera.getParameters().getSupportedPreviewFpsRange();
//		        Log.i("TAG", "range:"+range.size());
                for(int j=0;j<range.size();j++) {
                    int[] r=range.get(j);
                    for(int k=0;k<r.length;k++)
                    {
                        Log.i("TAG"+"initCamera", "supportedPreviewFps:"+r[k]);
                    }
                }


//	            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);//1连续对焦
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);//自动对焦
//		        parameters.setPreviewFpsRange(29950, 30000);
                // 设置拍照和预览图片大小
//		        parameters.setPictureSize(4160, 2336); //honor 7指定拍照图片的大小
//                parameters.setPictureSize(2048, 1536); //nubia Z7 MAX指定拍照图片的大小
//		        parameters.setPictureSize(3264, 1836); //nexus4指定拍照图片的大小
	            parameters.setPictureSize(1280, 720); //最优拍照图片的大小
                parameters.setPreviewSize(mPreviewHeight, mPreviewWidth); // 指定preview的大小
//		        parameters.setPreviewFrameRate(20);
                //这两个属性 如果这两个属性设置的和真实手机的不一样时，就会报错

                // 横竖屏镜头自动调整
                if (this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE)
                {
                    parameters.set("orientation", "portrait"); //
                    parameters.set("rotation", 90); // 镜头角度转90度（默认摄像头是横拍）
                    mCamera.setDisplayOrientation(90); // 在2.2以上可以使用
                } else// 如果是横屏
                {
                    parameters.set("orientation", "landscape"); //
                    mCamera.setDisplayOrientation(0); // 在2.2以上可以使用
                }

		        /* 视频流编码处理 */
                //添加对视频流处理函数


                // 设定配置参数并开启预览
                mCamera.setParameters(parameters); // 将Camera.Parameters设定予Camera
                mCamera.startPreview(); // 打开预览画面
                mCamera.cancelAutoFocus();// 2如果要实现连续的自动对焦，这一句必须加上
                bIfPreview = true;

                // 【调试】设置后的图片大小和预览大小以及帧率
                Camera.Size csize = mCamera.getParameters().getPreviewSize();
                mPreviewHeight = csize.height; //
                mPreviewWidth = csize.width;
                Log.i("TAG"+"initCamera", "after setting, previewSize:width: " + csize.width + " height: " + csize.height);
                csize = mCamera.getParameters().getPictureSize();
                Log.i("TAG"+"initCamera", "after setting, pictruesize:width: " + csize.width + " height: " + csize.height);
                Log.i("TAG"+"initCamera", "after setting, previewformate is " + mCamera.getParameters().getPreviewFormat());
                Log.i("TAG"+"initCamera", "after setting, previewframerate is " + mCamera.getParameters().getPreviewFrameRate());
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    protected View createButton(int id, String text){
        final Button btn=new Button(this);
        btn.setId(id);
        Log.d("da","id is" + btn.getId());
        //btn.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout
        // .LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        btn.setText(text);
        return btn;
    }

    public void open_album() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_ALBUM_CODE);
    }

    @Override
    public void onActivityResult(int request_code, int result_code,
                                 Intent data) {
        super.onActivityResult(request_code, result_code, data);
        if (request_code == REQUEST_ALBUM_CODE) {
            Reader reader = new QRCodeReader();
            try {
                Uri uri = data.getData();
                if (uri == null) {
                    return;
                }
                Bitmap bitmap = BitmapFactory.decodeStream(
                        getBaseContext().getContentResolver().openInputStream(uri),
                        null, null
                );
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                int[] pixels = new int[width * height];
                bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
                // 新建一个RGBLuminanceSource对象
                RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
                // 将图片转换成二进制图片
                BinaryBitmap binaryBitmap = new BinaryBitmap(new GlobalHistogramBinarizer(source));
                Result result = null;
                try {
                    result = reader.decode(binaryBitmap);// 开始解析
                } catch (NotFoundException e) {
                    e.printStackTrace();
                } catch (ChecksumException e) {
                    e.printStackTrace();
                } catch (FormatException e) {
                    e.printStackTrace();
                }
                String text = result.getText();

                Intent intent = new Intent();
                Bundle bundle = new Bundle();
                bundle.putString("result", result.toString());
                intent.putExtras(bundle);
                setResult(RESULT_OK, intent);
                finish();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
