/*
 * Copyright (C) 2010 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dtr.zxing.camera;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import com.dtr.zxing.Config;
import com.dtr.zxing.camera.open.CameraFacing;
import com.dtr.zxing.camera.open.OpenCamera;

import java.util.List;
import java.util.regex.Pattern;


/**
 * A class which deals with reading, parsing, and setting the camera parameters which are used to
 * configure the camera hardware.
 */
public final class CameraConfigurationManager {

  private static final String TAG = "CameraConfiguration";
//  private static final int TEN_DESIRED_ZOOM = 27;
  private static final int TEN_DESIRED_ZOOM = 10;
  private static final int DESIRED_SHARPNESS = 30;
  private static final Pattern COMMA_PATTERN = Pattern.compile(",");

  private final Context context;
  private int cwNeededRotation;
  private int cwRotationFromDisplayToCamera;
  private Point screenResolution;
  private Point cameraResolution;
  private Point bestPreviewSize;
  private Point previewSizeOnScreen;

  public CameraConfigurationManager(Context context) {
    this.context = context;
  }

  /**
   * Reads, one time, values from the camera that are needed by the app.
   */
  public void initFromCameraParameters(OpenCamera camera) {
    Camera.Parameters parameters = camera.getCamera().getParameters();
    WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    Display display = manager.getDefaultDisplay();

    int displayRotation = display.getRotation();
    int cwRotationFromNaturalToDisplay;
    switch (displayRotation) {
      case Surface.ROTATION_0:
        cwRotationFromNaturalToDisplay = 0;
        break;
      case Surface.ROTATION_90:
        cwRotationFromNaturalToDisplay = 90;
        break;
      case Surface.ROTATION_180:
        cwRotationFromNaturalToDisplay = 180;
        break;
      case Surface.ROTATION_270:
        cwRotationFromNaturalToDisplay = 270;
        break;
      default:
        // Have seen this return incorrect values like -90
        if (displayRotation % 90 == 0) {
          cwRotationFromNaturalToDisplay = (360 + displayRotation) % 360;
        } else {
          throw new IllegalArgumentException("Bad rotation: " + displayRotation);
        }
    }
    Log.i(TAG, "Display at: " + cwRotationFromNaturalToDisplay);

    int cwRotationFromNaturalToCamera = camera.getOrientation();
    Log.i(TAG, "Camera at: " + cwRotationFromNaturalToCamera);

    // Still not 100% sure about this. But acts like we need to flip this:
    if (camera.getFacing() == CameraFacing.FRONT) {
      cwRotationFromNaturalToCamera = (360 - cwRotationFromNaturalToCamera) % 360;
      Log.i(TAG, "Front camera overriden to: " + cwRotationFromNaturalToCamera);
    }

    /*
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    String overrideRotationString;
    if (camera.getFacing() == CameraFacing.FRONT) {
      overrideRotationString = prefs.getString(PreferencesActivity.KEY_FORCE_CAMERA_ORIENTATION_FRONT, null);
    } else {
      overrideRotationString = prefs.getString(PreferencesActivity.KEY_FORCE_CAMERA_ORIENTATION, null);
    }
    if (overrideRotationString != null && !"-".equals(overrideRotationString)) {
      Log.i(TAG, "Overriding camera manually to " + overrideRotationString);
      cwRotationFromNaturalToCamera = Integer.parseInt(overrideRotationString);
    }
     */

    cwRotationFromDisplayToCamera =
        (360 + cwRotationFromNaturalToCamera - cwRotationFromNaturalToDisplay) % 360;
    Log.i(TAG, "Final display orientation: " + cwRotationFromDisplayToCamera);
    if (camera.getFacing() == CameraFacing.FRONT) {
      Log.i(TAG, "Compensating rotation for front camera");
      cwNeededRotation = (360 - cwRotationFromDisplayToCamera) % 360;
    } else {
      cwNeededRotation = cwRotationFromDisplayToCamera;
    }
    Log.i(TAG, "Clockwise rotation from display to camera: " + cwNeededRotation);

    Point theScreenResolution = new Point();
    //remove thefollowing


    if(android.os.Build.VERSION.SDK_INT >= 13) {
      display.getSize(theScreenResolution);
    }
    else{
      int width     = display.getWidth();
      int height    = display.getHeight();
      theScreenResolution.x = width;
      theScreenResolution.y = height;
    }
    screenResolution = theScreenResolution;

//    Point screenResolutionForCamera =new Point();
//    screenResolutionForCamera.x =screenResolution.x;
//    screenResolutionForCamera.y =screenResolution.y;
//    if (screenResolution.x< screenResolution.y) {
//      screenResolutionForCamera.x = screenResolution.y;
//      screenResolutionForCamera.y =screenResolution.x;
//    }


    cameraResolution = CameraConfigurationUtils.findCloselySize(parameters, screenResolution.x, screenResolution.y,
            parameters.getSupportedPreviewSizes());
    Log.e(TAG, "Setting preview size: " + cameraResolution.x + "-" + cameraResolution.y);
    bestPreviewSize = CameraConfigurationUtils.findCloselySize(parameters, screenResolution.x,
            screenResolution.y, parameters.getSupportedPictureSizes());
    Log.e(TAG, "Setting picture size: " + bestPreviewSize.x + "-" + bestPreviewSize.y);
//    Log.i(TAG, "Screen resolution in current orientation: " + screenResolution);
//    cameraResolution = CameraConfigurationUtils.findBestPreviewSizeValue(parameters, screenResolution/*screenResolutionForCamera*/);
//    Log.i(TAG, "Camera resolution: " + cameraResolution);
//    bestPreviewSize = CameraConfigurationUtils.findBestPreviewSizeValue(parameters, screenResolution/*screenResolutionForCamera*/);
//    Log.i(TAG, "Best available preview size: " + bestPreviewSize);
//
    boolean isScreenPortrait = screenResolution.x < screenResolution.y;
    boolean isPreviewSizePortrait = bestPreviewSize.x < bestPreviewSize.y;

    if (isScreenPortrait == isPreviewSizePortrait) {
      previewSizeOnScreen = bestPreviewSize;
    } else {
      previewSizeOnScreen = new Point(bestPreviewSize.y, bestPreviewSize.x);
    }


    Log.i(TAG, "Preview size on screen: " + previewSizeOnScreen);


  }

  public void setDesiredCameraParameters(OpenCamera camera, boolean safeMode) {

    Camera theCamera = camera.getCamera();
    Camera.Parameters parameters = theCamera.getParameters();

    if (parameters == null) {
      Log.w(TAG, "Device error: no camera parameters are available. Proceeding without configuration.");
      return;
    }

    Log.i(TAG, "Initial camera parameters: " + parameters.flatten());

    if (safeMode) {
      Log.w(TAG, "In camera config safe mode -- most settings will not be honored");
    }

    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

    initializeTorch(parameters, prefs, safeMode);

    CameraConfigurationUtils.setFocus(
        parameters,
            Config.KEY_AUTO_FOCUS,
            Config.KEY_DISABLE_CONTINUOUS_FOCUS,
        safeMode);

    if (!safeMode) {
      if (Config.KEY_INVERT_SCAN) {
        CameraConfigurationUtils.setInvertColor(parameters);
      }

      if (!Config.KEY_DISABLE_BARCODE_SCENE_MODE) {
        CameraConfigurationUtils.setBarcodeSceneMode(parameters);
      }

      if (!Config.KEY_DISABLE_METERING) {
        CameraConfigurationUtils.setVideoStabilization(parameters);
        CameraConfigurationUtils.setFocusArea(parameters);
        CameraConfigurationUtils.setMetering(parameters);
      }

    }

//    List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
//
//    int position = 0;
//
//    if(supportedPreviewSizes.size() > 2){
//
//      position = supportedPreviewSizes.size()/2 + 1;//supportedPreviewSizes.get();
//    }else {
//      position = supportedPreviewSizes.size()/2;
//    }

//    int width = supportedPreviewSizes.get(position).width;
//    int height = supportedPreviewSizes.get(position).height;

    int width = cameraResolution.x;
    int height = cameraResolution.y;

    Log.d(TAG, "Setting preview size: " + cameraResolution);
//    camera.setDisplayOrientation(90);
    cameraResolution.x = width;
    cameraResolution.y = height;
    parameters.setPreviewSize(width,height);
    setFlash(parameters);
    setZoom(parameters);



//    parameters.setPreviewSize(bestPreviewSize.x, bestPreviewSize.y);

    theCamera.setParameters(parameters);

//    theCamera.setDisplayOrientation(cwRotationFromDisplayToCamera);
//
//    Camera.Parameters afterParameters = theCamera.getParameters();
//    Camera.Size afterSize = afterParameters.getPreviewSize();
//    if (afterSize != null && (bestPreviewSize.x != afterSize.width || bestPreviewSize.y != afterSize.height)) {
//      Log.w(TAG, "Camera said it supported preview size " + bestPreviewSize.x + 'x' + bestPreviewSize.y +
//          ", but after setting it, preview size is " + afterSize.width + 'x' + afterSize.height);
//      bestPreviewSize.x = afterSize.width;
//      bestPreviewSize.y = afterSize.height;
//    }


    theCamera.setDisplayOrientation(90);
  }


  private Point findBestPictureSize(Camera.Parameters parameters) {
    int  diff = Integer.MIN_VALUE;
    String pictureSizeValueString = parameters.get("picture-size-values");

    // saw this on Xperia
    if (pictureSizeValueString == null) {
      pictureSizeValueString = parameters.get("picture-size-value");
    }

    if(pictureSizeValueString == null) {
      Point size = new Point();
      size.x = screenResolution.x;
      size.y = screenResolution.y;
      return size;
    }

    Log.d("tag", "pictureSizeValueString : " + pictureSizeValueString);
    int bestX = 0;
    int bestY = 0;


    for(String pictureSizeString : COMMA_PATTERN.split(pictureSizeValueString))
    {
      pictureSizeString = pictureSizeString.trim();

      int dimPosition = pictureSizeString.indexOf('x');
      if(dimPosition == -1){
        Log.e(TAG, "Bad pictureSizeString:"+pictureSizeString);
        continue;
      }

      int newX = 0;
      int newY = 0;

      try{
        newX = Integer.parseInt(pictureSizeString.substring(0, dimPosition));
        newY = Integer.parseInt(pictureSizeString.substring(dimPosition+1));
      }catch(NumberFormatException e){
        Log.e(TAG, "Bad pictureSizeString:"+pictureSizeString);
        continue;
      }

      int newDiff = Math.abs(newX - screenResolution.x)+Math.abs(newY- screenResolution.y);
      if(newDiff == diff)
      {
        bestX = newX;
        bestY = newY;
        break;
      } else if(newDiff > diff){
        if((3 * newX) == (4 * newY)) {
          bestX = newX;
          bestY = newY;
          diff = newDiff;
        }
      }
    }

    if (bestX > 0 && bestY > 0) {
      Point size = new Point();
      size.x = bestX;
      size.y = bestY;

      return size;
    }
    return null;
  }

  private Point findBestPreviewSize(Camera.Parameters parameters) {

    String previewSizeValueString = null;
    int diff = Integer.MAX_VALUE;
    previewSizeValueString = parameters.get("preview-size-values");

    if (previewSizeValueString == null) {
      previewSizeValueString = parameters.get("preview-size-value");
    }

    if(previewSizeValueString == null) {  // 有些手机例如m9获取不到支持的预览大小   就直接返回屏幕大小
      Point point = new Point(screenResolution.x, screenResolution.y);
      return  point;
    }
    Log.d("tag", "previewSizeValueString : " + previewSizeValueString);
    int bestX = 0;
    int bestY = 0;

    for(String prewsizeString : COMMA_PATTERN.split(previewSizeValueString))
    {
      prewsizeString = prewsizeString.trim();

      int dimPosition = prewsizeString.indexOf('x');
      if(dimPosition == -1){
        Log.e(TAG, "Bad prewsizeString:"+prewsizeString);
        continue;
      }

      int newX = 0;
      int newY = 0;

      try{
        newX = Integer.parseInt(prewsizeString.substring(0, dimPosition));
        newY = Integer.parseInt(prewsizeString.substring(dimPosition+1));
      }catch(NumberFormatException e){
        Log.e(TAG, "Bad prewsizeString:"+prewsizeString);
        continue;
      }

      int newDiff = Math.abs(newX - screenResolution.x)+Math.abs(newY- screenResolution.y);

      if(newDiff == diff)
      {
        bestX = newX;
        bestY = newY;
        break;
      } else if(newDiff < diff){
        if((3 * newX) == (4 * newY)) {
          bestX = newX;
          bestY = newY;
          diff = newDiff;
        }
      }
    }
    if (bestX > 0 && bestY > 0) {
      Point point = new Point(bestX, bestY);
      return point;
    }
    return null;
  }


  private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
    final double ASPECT_TOLERANCE = 0.1;
    double targetRatio = (double) w / h;
    if (sizes == null) return null;

    Camera.Size optimalSize = null;
    double minDiff = Double.MAX_VALUE;

    int targetHeight = h;

    // Try to find an size match aspect ratio and size
    for (Camera.Size size : sizes) {
      double ratio = (double) size.width / size.height;
      if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
      if (Math.abs(size.height - targetHeight) < minDiff) {
        optimalSize = size;
        minDiff = Math.abs(size.height - targetHeight);
      }
    }

    // Cannot find the one match the aspect ratio, ignore the requirement
    if (optimalSize == null) {
      minDiff = Double.MAX_VALUE;
      for (Camera.Size size : sizes) {
        if (Math.abs(size.height - targetHeight) < minDiff) {
          optimalSize = size;
          minDiff = Math.abs(size.height - targetHeight);
        }
      }
    }
    return optimalSize;
  }

  private void setFlash(Camera.Parameters parameters) {
    // FIXME: This is a hack to turn the flash off on the Samsung Galaxy.
    // And this is a hack-hack to work around a different value on the Behold II
    // Restrict Behold II check to Cupcake, per Samsung's advice
    //if (Build.MODEL.contains("Behold II") &&
    //    CameraManager.SDK_INT == Build.VERSION_CODES.CUPCAKE) {
    if (Build.MODEL.contains("Behold II") && CameraManager.SDK_INT == 3) { // 3 = Cupcake
      parameters.set("flash-value", 1);
    } else {
      parameters.set("flash-value", 2);
    }
    // This is the standard setting to turn the flash off that all devices should honor.
    parameters.set("flash-mode", "off");
  }

  private static int findBestMotZoomValue(CharSequence stringValues, int tenDesiredZoom) {
    int tenBestValue = 0;
    for (String stringValue : COMMA_PATTERN.split(stringValues)) {
      stringValue = stringValue.trim();
      double value;
      try {
        value = Double.parseDouble(stringValue);
      } catch (NumberFormatException nfe) {
        return tenDesiredZoom;
      }
      int tenValue = (int) (10.0 * value);
      if (Math.abs(tenDesiredZoom - value) < Math.abs(tenDesiredZoom - tenBestValue)) {
        tenBestValue = tenValue;
      }
    }
    return tenBestValue;
  }


  private void setZoom(Camera.Parameters parameters) {

    String zoomSupportedString = parameters.get("zoom-supported");
    if (zoomSupportedString != null && !Boolean.parseBoolean(zoomSupportedString)) {
      return;
    }

    int tenDesiredZoom = TEN_DESIRED_ZOOM;

    String maxZoomString = parameters.get("max-zoom");
    if (maxZoomString != null) {
      try {
        int tenMaxZoom = (int) (10.0 * Double.parseDouble(maxZoomString));
        if (tenDesiredZoom > tenMaxZoom) {
          tenDesiredZoom = tenMaxZoom;
        }
      } catch (NumberFormatException nfe) {
        Log.w(TAG, "Bad max-zoom: " + maxZoomString);
      }
    }

    String takingPictureZoomMaxString = parameters.get("taking-picture-zoom-max");
    if (takingPictureZoomMaxString != null) {
      try {
        int tenMaxZoom = Integer.parseInt(takingPictureZoomMaxString);
        if (tenDesiredZoom > tenMaxZoom) {
          tenDesiredZoom = tenMaxZoom;
        }
      } catch (NumberFormatException nfe) {
        Log.w(TAG, "Bad taking-picture-zoom-max: " + takingPictureZoomMaxString);
      }
    }

    String motZoomValuesString = parameters.get("mot-zoom-values");
    if (motZoomValuesString != null) {
      tenDesiredZoom = findBestMotZoomValue(motZoomValuesString, tenDesiredZoom);
    }

    String motZoomStepString = parameters.get("mot-zoom-step");
    if (motZoomStepString != null) {
      try {
        double motZoomStep = Double.parseDouble(motZoomStepString.trim());
        int tenZoomStep = (int) (10.0 * motZoomStep);
        if (tenZoomStep > 1) {
          tenDesiredZoom -= tenDesiredZoom % tenZoomStep;
        }
      } catch (NumberFormatException nfe) {
        // continue
      }
    }

    // Set zoom. This helps encourage the user to pull back.
    // Some devices like the Behold have a zoom parameter
    if (maxZoomString != null || motZoomValuesString != null) {
      parameters.set("zoom", String.valueOf(tenDesiredZoom / 10.0));
    }

    // Most devices, like the Hero, appear to expose this zoom parameter.
    // It takes on values like "27" which appears to mean 2.7x zoom
    if (takingPictureZoomMaxString != null) {
      parameters.set("taking-picture-zoom", tenDesiredZoom);
    }
  }


  public Point getBestPreviewSize() {
    return bestPreviewSize;
  }

  public Point getPreviewSizeOnScreen() {
    return previewSizeOnScreen;
  }

  public Point getCameraResolution() {
    return cameraResolution;
  }

  public Point getScreenResolution() {
    return screenResolution;
  }

  public int getCWNeededRotation() {
    return cwNeededRotation;
  }

  public boolean getTorchState(Camera camera) {
    if (camera != null) {
      Camera.Parameters parameters = camera.getParameters();
      if (parameters != null) {
        String flashMode = parameters.getFlashMode();
        return flashMode != null &&
            (Camera.Parameters.FLASH_MODE_ON.equals(flashMode) ||
             Camera.Parameters.FLASH_MODE_TORCH.equals(flashMode));
      }
    }
    return false;
  }

  public void setTorch(Camera camera, boolean newSetting) {
    Camera.Parameters parameters = camera.getParameters();
    doSetTorch(parameters, newSetting, false);
    camera.setParameters(parameters);
  }

  private void initializeTorch(Camera.Parameters parameters, SharedPreferences prefs, boolean safeMode) {
    boolean currentSetting = FrontLightMode.readPref(prefs) == FrontLightMode.ON;
    doSetTorch(parameters, currentSetting, safeMode);
  }

  private void doSetTorch(Camera.Parameters parameters, boolean newSetting, boolean safeMode) {
    CameraConfigurationUtils.setTorch(parameters, newSetting);
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    if (!safeMode && !Config.KEY_DISABLE_EXPOSURE) {
      CameraConfigurationUtils.setBestExposure(parameters, newSetting);
    }
  }

}
