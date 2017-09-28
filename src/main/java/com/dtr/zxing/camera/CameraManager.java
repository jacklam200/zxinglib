/*
 * Copyright (C) 2008 ZXing authors
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

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceHolder;

import com.dtr.zxing.Config;
import com.dtr.zxing.camera.open.OpenCamera;
import com.dtr.zxing.camera.open.OpenCameraInterface;
import com.google.zxing.PlanarYUVLuminanceSource;

import java.io.IOException;

/**
 * This object wraps the Camera service object and expects to be the only one talking to it. The
 * implementation encapsulates the steps needed to take preview-sized images, which are used for
 * both preview and decoding.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class CameraManager {

    private static final String TAG = CameraManager.class.getSimpleName();

    static final int SDK_INT; // Later we can use Build.VERSION.SDK_INT
    static {
        int sdkInt;
        try {
            sdkInt = Integer.parseInt(Build.VERSION.SDK);
        } catch (NumberFormatException nfe) {
            // Just to be safe
            sdkInt = 10000;
        }
        SDK_INT = sdkInt;
    }


    private static final int MIN_FRAME_WIDTH = 240;
    private static final int MIN_FRAME_HEIGHT = 240;
//    private static final int MAX_FRAME_WIDTH = 480;//1200; // = 5/8 * 1920
//    private static final int MAX_FRAME_HEIGHT = 360;//675; // = 5/8 * 1080

    private static final int MAX_FRAME_WIDTH = 1200; // = 5/8 * 1920
    private static final int MAX_FRAME_HEIGHT = 675; // = 5/8 * 1080

    private final Context context;
    private final Activity activity;
    private final CameraConfigurationManager configManager;
    private OpenCamera camera;
    private AutoFocusManager autoFocusManager;
    private Rect framingRect;
    private Rect framingRectInPreview;
    private boolean initialized;
    private boolean previewing;
    private int requestedCameraId = OpenCameraInterface.NO_REQUESTED_CAMERA;
    private int requestedFramingRectWidth;
    private int requestedFramingRectHeight;
    /**
     * Preview frames are delivered here, which we pass on to the registered handler. Make sure to
     * clear the handler so it will only receive one message.
     */
    private final PreviewCallback previewCallback;

    public CameraManager(Context context, Activity activity) {
        this.context = context;
        this.activity = activity;
        this.configManager = new CameraConfigurationManager(context); // for camera configuration
        previewCallback = new PreviewCallback(configManager); // create preview callback listener
    }

    public synchronized CameraConfigurationManager getCameraConfigManager() {
        return configManager;
    }

    /**
     * Opens the camera driver and initializes the hardware parameters.
     *
     * @param holder The surface object which the camera will draw preview frames into.
     * @throws IOException Indicates the camera driver failed to open.
     */
    public synchronized void openDriver(SurfaceHolder holder) throws IOException {
        OpenCamera theCamera = camera;
        if (theCamera == null) {
            theCamera = OpenCameraInterface.open(requestedCameraId);
            if (theCamera == null) {
                throw new IOException("Camera.open() failed to return object from driver");
            }
            camera = theCamera;
        }

        if (!initialized) {
            initialized = true;
            configManager.initFromCameraParameters(theCamera);
//            if (requestedFramingRectWidth > 0 && requestedFramingRectHeight > 0) {
//                setManualFramingRect(requestedFramingRectWidth, requestedFramingRectHeight);
//                requestedFramingRectWidth = 0;
//                requestedFramingRectHeight = 0;
//            }
        }

        Camera cameraObject = theCamera.getCamera();
        Camera.Parameters parameters = cameraObject.getParameters();
        String parametersFlattened = parameters == null ? null : parameters.flatten(); // Save these, temporarily
        try {
            configManager.setDesiredCameraParameters(theCamera, false);
        } catch (RuntimeException re) {
            // Driver failed
            Log.w(TAG, "Camera rejected parameters. Setting only minimal safe-mode parameters");
            Log.i(TAG, "Resetting to saved camera params: " + parametersFlattened);
            // Reset:
            if (parametersFlattened != null) {
                parameters = cameraObject.getParameters();
                parameters.unflatten(parametersFlattened);
                try {
                    cameraObject.setParameters(parameters);
                    configManager.setDesiredCameraParameters(theCamera, true);
                } catch (RuntimeException re2) {
                    // Well, darn. Give up
                    Log.w(TAG, "Camera rejected even safe-mode parameters! No configuration");
                }
            }
        }
        cameraObject.setPreviewDisplay(holder);

    }

    public synchronized boolean isOpen() {
        return camera != null;
    }

    /**
     * Closes the camera driver if still in use.
     */
    public synchronized void closeDriver() {
        if (camera != null) {
            stopPreview();
            camera.getCamera().release();
            camera = null;
            // Make sure to clear these each time we close the camera, so that any scanning rect
            // requested by intent is forgotten.
            framingRect = null;
            framingRectInPreview = null;
        }
    }

    /**
     * Asks the camera hardware to begin drawing preview frames to the screen.
     */
    public synchronized void startPreview() {
        OpenCamera theCamera = camera;
        if (theCamera != null && !previewing) {
            theCamera.getCamera().startPreview();
            if(autoFocusManager != null){
                autoFocusManager.stop();
            }
            previewing = true;
            Point p = configManager.getPreviewSizeOnScreen();

            String currentFocusMode = theCamera.getCamera().getParameters().getFocusMode();
            boolean useAutoFocus = true;
            if(!TextUtils.isEmpty(currentFocusMode)){

               if(currentFocusMode.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE) ||
                       currentFocusMode.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)  ){
                   useAutoFocus = false;
               }
            }

            if(autoFocusManager == null || useAutoFocus)
                autoFocusManager = new AutoFocusManager(context, theCamera.getCamera(), p.x/2, p.y/2, configManager, this);
        }
    }

    /**
     * Tells the camera to stop drawing preview frames.
     */
    public synchronized void stopPreview() {
        if (autoFocusManager != null) {
            autoFocusManager.stop();
            autoFocusManager = null;
        }
        if (camera != null && previewing) {
            camera.getCamera().stopPreview();
            previewCallback.setHandler(null, 0);
            previewing = false;
        }
    }

    /**
     * Convenience method for {@link com.google.zxing.client.android.CaptureActivity}
     *
     * @param newSetting if {@code true}, light should be turned on if currently off. And vice versa.
     */
    public synchronized void setTorch(boolean newSetting) {
        OpenCamera theCamera = camera;
        if (theCamera != null) {
            if (newSetting != configManager.getTorchState(theCamera.getCamera())) {
                boolean wasAutoFocusManager = autoFocusManager != null;
                if (wasAutoFocusManager) {
                    autoFocusManager.stop();
                    autoFocusManager = null;
                }
                configManager.setTorch(theCamera.getCamera(), newSetting);
                if (wasAutoFocusManager) {
                    Point p = configManager.getPreviewSizeOnScreen();
                    autoFocusManager = new AutoFocusManager(context, theCamera.getCamera(), p.x/2, p.y/2, configManager, this);
                    autoFocusManager.start(p.x/2, p.y/2);
                }
            }
        }
    }

    /**
     * A single preview frame will be returned to the handler supplied. The data will arrive as byte[]
     * in the message.obj field, with width and height encoded as message.arg1 and message.arg2,
     * respectively.
     *
     * @param handler The handler to send the message to.
     * @param message The what field of the message to be sent.
     */
    public synchronized void requestPreviewFrame(Handler handler, int message) {
        OpenCamera theCamera = camera;
        Log.v(TAG, "previewing:" + previewing);
        Log.v(TAG, "theCamera:" + theCamera);
        if (theCamera != null && previewing) {
            previewCallback.setHandler(handler, message);
            theCamera.getCamera().setOneShotPreviewCallback(previewCallback);
        }
        else{

        }
    }

    public synchronized void stopDecodeFrame(boolean isStop){
        if(previewCallback != null){
            previewCallback.setStop(isStop);
        }
    }


    /**
     * Calculates the framing rect which the UI should draw to show the user where to place the
     * barcode. This target helps with alignment as well as forces the user to hold the device
     * far enough away to ensure the image will be in focus.
     *
     * @return The rectangle to draw on screen in window coordinates.
     */
    public synchronized Rect getFramingRect() {
//        if (framingRect == null) {
//            if (camera == null) {
//                return null;
//            }
//            Point screenResolution = configManager.getScreenResolution();
//            if (screenResolution == null) {
//                // Called early, before init even finished
//                return null;
//            }
//
//
//
//            int width = findDesiredDimensionInRange(screenResolution.x, MIN_FRAME_WIDTH, MAX_FRAME_WIDTH);
//            int height = findDesiredDimensionInRange(screenResolution.y, MIN_FRAME_HEIGHT, MAX_FRAME_HEIGHT);
//            int leftOffset = (screenResolution.x - width) / 2;
//            int topOffset = (screenResolution.y - height) / 2;
//            framingRect = new Rect(leftOffset, topOffset, leftOffset + width, topOffset + height);
//
//
//            Log.d(TAG, "Calculated framing rect: " + framingRect);
//        }
//        if(mCallback != null){
//            mCallback.onCallback(framingRect);
//        }
        Log.d("jacklam", "Calculated framing rect: " + framingRect);
        return framingRect;
    }

    public static interface Callback{
        void onCallback(Rect rect);
    }
    private Callback mCallback;

    public void setCallback(Callback callback){
        mCallback = callback;
    }

    public synchronized void setFramingRect(Rect rect) {
        if (rect != null)
            framingRect = rect;
    }

    private static int findDesiredDimensionInRange(int resolution, int hardMin, int hardMax) {
        int dim = 5 * resolution / 8; // Target 5/8 of each dimension
        if (dim < hardMin) {
            return hardMin;
        }
        if (dim > hardMax) {
            return hardMax;
        }
        return dim;
    }

    /**
     * Like {@link #getFramingRect} but coordinates are in terms of the preview frame,
     * not UI / screen.
     *
     * @return {@link Rect} expressing barcode scan area in terms of the preview size
     */
    public synchronized Rect getFramingRectInPreview() {

        if (framingRectInPreview == null) {
            Rect framingRect = getFramingRect();
            if (framingRect == null) {
                return null;
            }
            Rect rect = new Rect(framingRect);
            Point cameraResolution = configManager.getCameraResolution();
            Point screenResolution = configManager.getScreenResolution();
            if (cameraResolution == null || screenResolution == null) {
                // Called early, before init even finished
                return null;
            }



            if (activity != null && (activity.getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT ||
                    activity.getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT)) {

                rect.left =rect.left * cameraResolution.y / screenResolution.x;

                rect.right =rect.right * cameraResolution.y / screenResolution.x;

                rect.top =rect.top * cameraResolution.x / screenResolution.y;

                rect.bottom =rect.bottom * cameraResolution.x / screenResolution.y;
//                rect.left = rect.left * cameraResolution.x / screenResolution.x;
//                rect.right = rect.right * cameraResolution.x / screenResolution.x;
//                rect.top = rect.top * cameraResolution.y / screenResolution.y;
//                rect.bottom = rect.bottom * cameraResolution.y / screenResolution.y;




            } else {
                rect.left = rect.left * cameraResolution.x / screenResolution.x;
                rect.right = rect.right * cameraResolution.x / screenResolution.x;
                rect.top = rect.top * cameraResolution.y / screenResolution.y;
                rect.bottom = rect.bottom * cameraResolution.y / screenResolution.y;

            }
            framingRectInPreview = rect;

        }

        return framingRectInPreview;
    }


    /**
     * Allows third party apps to specify the camera ID, rather than determine
     * it automatically based on available cameras and their orientation.
     *
     * @param cameraId camera ID of the camera to use. A negative value means "no preference".
     */
    public synchronized void setManualCameraId(int cameraId) {
        requestedCameraId = cameraId;
    }

    /**
     * Allows third party apps to specify the scanning rectangle dimensions, rather than determine
     * them automatically based on screen resolution.
     *
     * @param width  The width in pixels to scan.
     * @param height The height in pixels to scan.
     */
    public synchronized void setManualFramingRect(int width, int height) {
        if (initialized) {
            Point screenResolution = configManager.getScreenResolution();
            if (width > screenResolution.x) {
                width = screenResolution.x;
            }
            if (height > screenResolution.y) {
                height = screenResolution.y;
            }
            int leftOffset = (screenResolution.x - width) / 2;
            int topOffset = (screenResolution.y - height) / 2;
            framingRect = new Rect(leftOffset, topOffset, leftOffset + width, topOffset + height);
            Log.d(TAG, "Calculated manual framing rect: " + framingRect);
            framingRectInPreview = null;
        } else {
            requestedFramingRectWidth = width;
            requestedFramingRectHeight = height;
        }
    }

    /**
     * A factory method to build the appropriate LuminanceSource object based on the format
     * of the preview buffers, as described by Camera.Parameters.
     *
     * @param data   A preview frame.
     * @param width  The width of the image.
     * @param height The height of the image.
     * @return A PlanarYUVLuminanceSource instance.
     */
    public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height) {

        Rect rect = getFramingRectInPreview();
        if (rect == null || rect.width() < 1 || rect.height() < 1 || width < 1 || height < 1) {
            return null;
        }
        Log.d("jacklam", "buildLuminanceSource getFramingRectInPreview: " + rect);
        Log.d("jacklam", "buildLuminanceSource width: " + width + " height:" + height);
        // Go ahead and assume it's YUV rather than die.
        return new PlanarYUVLuminanceSource(data, width, height, rect.left, rect.top,
                rect.width(), rect.height(), false);
    }

}
