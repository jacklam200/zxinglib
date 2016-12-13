package com.dtr.zxing;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.dtr.zxing.camera.CameraManager;


import java.lang.reflect.Field;

/**
 * Created by LinZaixiong on 2016/9/21.
 */

public class ZXingCaptureActivity extends CaptureActivity implements CameraManager.Callback {

    public ViewGroup scanContainer;
    public View scanCropView;
    public ImageView scanLine;
    public ImageView mFlash;


    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        int layoutId  = getContentView();
        if (layoutId != 0){
            setContentView(layoutId);
        }else{
            setContentView(R.layout.activity_qr_scan);
        }

        initSimpleView();
    }

    public void initSimpleView(){
        scanContainer = (RelativeLayout) findViewById(R.id.capture_container);
        scanCropView = (RelativeLayout) findViewById(R.id.capture_crop_view);
//        (this.getResources().getDisplayMetrics().widthPixels - scanCropView.getLayoutParams().width)/2;
        scanLine = (ImageView) findViewById(R.id.capture_scan_line);
//        mFlash = (ImageView) findViewById(R.id.capture_flash);
//
//        mFlash.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                light();
//            }
//        });

        playAnimator(scanLine);
        initView();
    }

    public   int getContentView(){
        return 0;
    }
    public void initView(){

    }

    public  void playAnimator(View view){

    }
    /*
    * 初始化截取的矩形区域
    */
    public void initCrop() {
        int cameraWidth = getCameraManager().getCameraConfigManager().getCameraResolution().y;
        int cameraHeight = getCameraManager().getCameraConfigManager().getCameraResolution().x;
        int screenWidth = getCameraManager().getCameraConfigManager().getScreenResolution().x;
        int screenHeight = getCameraManager().getCameraConfigManager().getScreenResolution().y;
        /** 获取布局中扫描框的位置信息 */
        int[] location = new int[2];
        scanCropView.getLocationOnScreen(location);
//        Rect r = new Rect();
//        scanCropView.getGlobalVisibleRect(r);


        int cropLeft = location[0] ;
        int cropTop = location[1] - getStatusBarHeight();
        ViewGroup.LayoutParams lp = scanCropView.getLayoutParams();
        int cropWidth = scanCropView.getMeasuredWidth() == 0 ? lp.width :  scanCropView.getMeasuredWidth();
        int cropHeight = scanCropView.getMeasuredHeight() == 0 ? lp.height : scanCropView.getMeasuredHeight();

        /** 获取布局容器的宽高 */
//              lp = scanContainer.getLayoutParams();
        int containerWidth = screenWidth;
        int containerHeight = screenHeight;

        /** 计算最终截取的矩形的左上角顶点x坐标 */
        int x = cropLeft /** cameraWidth / containerWidth*/;
        /** 计算最终截取的矩形的左上角顶点y坐标 */
        int y = cropTop /** cameraHeight / containerHeight*/;

        /** 计算最终截取的矩形的宽度 */
        int width = cropWidth /** cameraWidth / containerWidth*/;
        /** 计算最终截取的矩形的高度 */
        int height = cropHeight/* * cameraHeight / containerHeight*/;

        /** 生成最终的截取的矩形 */
        mCropRect = new Rect(x, y, width + x, height + y);
        getCameraManager().setCallback(this);
        getCameraManager().setFramingRect(mCropRect);
    }

    @Override
    protected void displayFrameworkBugMessageAndExit() {
        // camera error
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("错误信息");
        builder.setMessage("相机打开出错，请稍后重试");
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }

        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                finish();
            }
        });
        builder.show();
    }

    public int getStatusBarHeight() {
        try {
            Class<?> c = Class.forName("com.android.internal.R$dimen");
            Object obj = c.newInstance();
            Field field = c.getField("status_bar_height");
            int x = Integer.parseInt(field.get(obj).toString());
            return getResources().getDimensionPixelSize(x);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public void onCallback(Rect rect) {



        ViewGroup.LayoutParams lp = scanCropView.getLayoutParams();

        if(lp == null){

            lp = new ViewGroup.MarginLayoutParams(rect.width(),  rect.height());
            scanCropView.setLayoutParams(lp);
//            lp = new ViewGroup.MarginLayoutParams(rect.width(),  rect.height());
//               scanCropView.setLayoutParams(lp);
        }

        if(lp != null){

//            if(lp instanceof ViewGroup.MarginLayoutParams){
//                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams)lp;
//                mlp.leftMargin = rect.left;
//                mlp.topMargin = rect.top;
//                mlp.width = rect.width();
//                mlp.height = rect.height();
//
//                scanCropView.setLayoutParams(lp);
//            }
            lp.width = rect.width();
            lp.height = rect.height();

            scanCropView.setLayoutParams(lp);

        }
        scanCropView.requestLayout();



    }
}
