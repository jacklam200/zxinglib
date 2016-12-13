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

package com.dtr.zxing;

import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.th.zbar.build.ZBarDecoder;

import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.util.Map;

public final class DecodeHandler extends Handler {

    private static final String TAG = DecodeHandler.class.getSimpleName();

    private final CaptureActivity activity;
    private final MultiFormatReader multiFormatReader;
    private boolean running = true;

    public DecodeHandler(CaptureActivity activity, Map<DecodeHintType, Object> hints) {
        multiFormatReader = new MultiFormatReader();
        multiFormatReader.setHints(hints);
        this.activity = activity;
    }

    @Override
    public void handleMessage(Message message) {
        if (!running) {
            return;
        }
        if (message.what == R.id.decode) {
            decode((byte[]) message.obj, message.arg1, message.arg2);
        } else if (message.what == R.id.quit) {
            running = false;
            Looper.myLooper().quit();
        }

    }

    /**
     * Decode the data within the viewfinder rectangle, and time how long it took. For efficiency,
     * reuse the same reader objects from one decode to the next.
     *
     * @param data   The YUV preview frame.
     * @param width  The width of the preview frame.
     * @param height The height of the preview frame.
     */
    private void decode(byte[] data, int width, int height) {

        Log.d("jacklam", "data coming width:" + width + " height:" + height);
        long start = System.currentTimeMillis();
        Log.d("jacklam", "data coming data length:" + data.length);
        byte[] rotatedData = null;
        if (activity != null && (activity.getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT ||
                activity.getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT)) {


             rotatedData= new byte[data.length];

            for (int y = 0; y< height; y++) {

                for (int x = 0; x < width; x++)

                    rotatedData[x * height + height - y -1] = data[x + y * width];

            }


            int tmp = width;

            width = height;

            height = tmp;

//            rotatedData =  reverseHorizontal(rotatedData, width, height);

        }
        Log.d("jacklam", "data invert time:" + (System.currentTimeMillis() - start));
        decode(rotatedData == null ? data : rotatedData, width, height, activity.isDual(), activity.isZbar());
        //zbar解码
//        String result = null;
//        Result rawResult = null;
//        ZBarDecoder zBarDecoder = new ZBarDecoder();
//        result = zBarDecoder.decodeCrop(rotatedData, size.width, size.height, rect.left, rect.top, rect.width(), rect.height());


    }

    private byte[] reverseHorizontal(byte[] yuvData, int dataWidth, int dataHeight) {
        Rect rect = activity.getCameraManager().getFramingRectInPreview();
        int left = rect.left;
        int top = rect.top;
        int width = rect.width();
        int height = rect.height();
        for (int y = 0, rowStart = top * dataWidth + left; y < height; y++, rowStart += dataWidth) {
            int middle = rowStart + width / 2;
            for (int x1 = rowStart, x2 = rowStart + width - 1; x1 < middle; x1++, x2--) {
                byte temp = yuvData[x1];
                yuvData[x1] = yuvData[x2];
                yuvData[x2] = temp;
            }
        }

        return yuvData;
    }

    private void decode(byte[] data, int width, int height, boolean isDual, boolean isZbar){

        DecodeResult rawResult = null;
        boolean isZxingParse = false;

        if(isDual){
            rawResult = decodeZxing(data, width, height);

            if(rawResult == null){
                rawResult = decodeZbar(data, width, height);
            }
            else{
                isZxingParse = true;
            }
        }
        else{
            if(isZbar){
                rawResult = decodeZbar(data, width, height);
            }
            else{
                isZxingParse = true;
                rawResult = decodeZxing(data, width, height);
            }
        }

        Handler handler = activity.getHandler();
        if (rawResult != null) {
            // Don't log the barcode contents for security.
            if (handler != null) {
                Message message = Message.obtain(handler, R.id.decode_succeeded, rawResult.getRawResult());

                if(rawResult.getSource() != null){

                    if(activity.isDebug){
                        Bundle bundle = new Bundle();
                        bundleThumbnail(rawResult.getSource(), bundle);
                        message.setData(bundle);
                    }

                }
                message.sendToTarget();
            }
        } else {
            if (handler != null) {
                Message message = Message.obtain(handler, R.id.decode_failed);
                message.sendToTarget();
            }
        }
    }

    private DecodeResult decodeZbar(byte[] data, int width, int height){
        long start = System.currentTimeMillis();
        DecodeResult resultDecode = null;
        //zbar解码
        String result = null;
        Result rawResult = null;
        ZBarDecoder zBarDecoder = new ZBarDecoder();
        Rect rect = activity.getCameraManager().getFramingRectInPreview();
        if(rect != null){
            result = zBarDecoder.decodeCrop(data, width, height, rect.left, rect.top, rect.width(), rect.height());
            if (result != null && !TextUtils.isEmpty(result)) {
                rawResult = new Result(result,null, null, BarcodeFormat.CODABAR);
                resultDecode = new DecodeResult();
                resultDecode.setRawResult(rawResult);
                resultDecode.setSource(null);
            }

        }
        long end = System.currentTimeMillis();
        Log.d(TAG, "Zbar Found barcode in " + (end - start) + " ms");
        return resultDecode;
    }

    private DecodeResult decodeZxing(byte[] data, int width, int height){
        long start = System.currentTimeMillis();
        DecodeResult result = null;
        Result rawResult = null;
        PlanarYUVLuminanceSource source = activity.getCameraManager().buildLuminanceSource(data, width, height);
        if (source != null) {
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            try {

                rawResult = multiFormatReader.decodeWithState(bitmap);
                result = new DecodeResult();
                result.setRawResult(rawResult);
                if(activity.isDebug)
                    result.setSource(source);
            } catch (ReaderException re) {
                // continue
            } finally {
                multiFormatReader.reset();
            }
        }
        long end = System.currentTimeMillis();
        Log.d(TAG, "Zxing Found barcode in " + (end - start) + " ms");
        return result;

    }


    private void decode(byte[] data, int width, int height, boolean isZbar){
        long start = System.currentTimeMillis();
        if(isZbar){
            //zbar解码
            String result = null;
            Result rawResult = null;
            ZBarDecoder zBarDecoder = new ZBarDecoder();
            Rect rect = activity.getCameraManager().getFramingRectInPreview();
            if(rect != null){

                result = zBarDecoder.decodeCrop(data, width, height, rect.left, rect.top, rect.width(), rect.height());

                if (result != null && !TextUtils.isEmpty(result)) {
                    if (null != activity.getHandler()) {

                        rawResult = new Result(result,null, null, BarcodeFormat.CODABAR);
                        Message message = Message.obtain(activity.getHandler(), R.id.decode_succeeded, rawResult);
                        message.sendToTarget();

                    }

                } else {

                    if (null != activity.getHandler()) {
                        if (activity.getHandler() != null) {
                            Message message = Message.obtain(activity.getHandler(), R.id.decode_failed);
                            message.sendToTarget();
                        }
                    }
                }
            }

        }
        else{

            Result rawResult = null;
            PlanarYUVLuminanceSource source = activity.getCameraManager().buildLuminanceSource(data, width, height);
            if (source != null) {
                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                try {
                    rawResult = multiFormatReader.decodeWithState(bitmap);
                } catch (ReaderException re) {
                    // continue
                } finally {
                    multiFormatReader.reset();
                }
            }

            Handler handler = activity.getHandler();
            if (rawResult != null) {
                // Don't log the barcode contents for security.
                long end = System.currentTimeMillis();
                Log.d(TAG, "Found barcode in " + (end - start) + " ms");
                if (handler != null) {
                    Message message = Message.obtain(handler, R.id.decode_succeeded, rawResult);
                    Bundle bundle = new Bundle();
                    bundleThumbnail(source, bundle);
                    message.setData(bundle);
                    message.sendToTarget();
                }
            } else {
                if (handler != null) {
                    Message message = Message.obtain(handler, R.id.decode_failed);
                    message.sendToTarget();
                }
            }
        }
    }

    private static void bundleThumbnail(PlanarYUVLuminanceSource source, Bundle bundle) {
        int[] pixels = source.renderThumbnail();
        int width = source.getThumbnailWidth();
        int height = source.getThumbnailHeight();
        Bitmap bitmap = Bitmap.createBitmap(pixels, 0, width, width, height, Bitmap.Config.ARGB_8888);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out);
        bundle.putByteArray(DecodeThread.BARCODE_BITMAP, out.toByteArray());
        bundle.putFloat(DecodeThread.BARCODE_SCALED_FACTOR, (float) width / source.getWidth());
    }

}
