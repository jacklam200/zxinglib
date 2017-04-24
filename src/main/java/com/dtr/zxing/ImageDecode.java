package com.dtr.zxing;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.text.TextUtils;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.common.HybridBinarizer;
import com.th.zbar.build.ZBarDecoder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

/**
 * on 2017/1/10.
 *
 * @author LinZaixiong
 */

public class ImageDecode {

    public static Result Decode(String path){

        Result result = null;
        Map<DecodeHintType,Object> hints = getDefaultHint();
        if (TextUtils.isEmpty(path)) {
            return null;
        }

        Bitmap  mScanBitmap = BitmapFactory.decodeFile(path);
        DecodeResult rawResult = decode(path, mScanBitmap, hints);
        if(rawResult != null){
            result = rawResult.getRawResult();
        }

        mScanBitmap.recycle();
        return result;
    }


    public static DecodeResult decode(String path, Bitmap mScanBitmap, Map<DecodeHintType,Object> hints) {
        DecodeResult result = null;
        Result rawResult = null;
        byte bitmapPixels[] = bitmap2Byte(mScanBitmap);
        if(bitmapPixels != null){
            PlanarYUVLuminanceSource source = buildLuminanceSource(bitmapPixels, mScanBitmap.getWidth(), mScanBitmap.getHeight());
            if (source != null) {
                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                MultiFormatReader multiFormatReader = new MultiFormatReader();
                try {

                    multiFormatReader.setHints(hints);
                    rawResult = multiFormatReader.decodeWithState(bitmap);
                    result = new DecodeResult();
                    result.setRawResult(rawResult);
                } catch (Exception re) {
                    // continue
                    re.printStackTrace();
                    Log.d("zxinglib-jack", "" + re.getMessage());
                } finally {
                    multiFormatReader.reset();
                }
            }

            if(result == null){
                //zbar解码
                String resultStr = null;
                ZBarDecoder zBarDecoder = new ZBarDecoder();
                resultStr = zBarDecoder.decodeRaw(bitmapPixels, mScanBitmap.getWidth(), mScanBitmap.getHeight());
                if (!TextUtils.isEmpty(resultStr)) {
                    rawResult = new Result(resultStr,null, null, BarcodeFormat.CODABAR);
                    result = new DecodeResult();
                    result.setRawResult(rawResult);
                    result.setSource(null);
                }

            }

            if(result == null){
                int w = mScanBitmap.getWidth();
                int h= mScanBitmap.getHeight();
                byte[] temp = bitmapPixels;
                for(int i = 0; i < 4; i++){

                    temp = bitmapInvert(temp, w , h);
                    int t = w;
                    w = h;
                    h = t;
//                    //zbar解码
//                    String resultStr = null;
//                    ZBarDecoder zBarDecoder = new ZBarDecoder();
//                    resultStr = zBarDecoder.decodeCrop(temp, w,h, 0, 0,w, h);
//                    if (!TextUtils.isEmpty(resultStr)) {
//                        rawResult = new Result(resultStr,null, null, BarcodeFormat.CODABAR);
//                        result = new DecodeResult();
//                        result.setRawResult(rawResult);
//                        result.setSource(null);
//                        break;
//                    }

                    PlanarYUVLuminanceSource source1 = buildLuminanceSource(bitmapPixels, mScanBitmap.getWidth(), mScanBitmap.getHeight());
                    if (source1 != null) {
                        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source1));
                        MultiFormatReader multiFormatReader = new MultiFormatReader();
                        try {

                            multiFormatReader.setHints(hints);
                            rawResult = multiFormatReader.decodeWithState(bitmap);
                            result = new DecodeResult();
                            result.setRawResult(rawResult);
                        } catch (ReaderException re) {
                            // continue
                        } finally {
                            multiFormatReader.reset();
                        }

                        if(rawResult != null){
                            break;
                        }
                    }

                }

            }

//            if(result == null){
//
//                int width = mScanBitmap.getWidth();
//                int height = mScanBitmap.getHeight();
//
//                for(int i = 0; i < 2; i++){
//
//                    bitmapPixels = bitmapInvert(mScanBitmap, width, height, 90 * ((i*2)+1));
//
//                    PlanarYUVLuminanceSource source1 = buildLuminanceSource(bitmapPixels, height, width);
//                    if (source1 != null) {
//                        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source1));
//                        MultiFormatReader multiFormatReader = new MultiFormatReader();
//                        try {
//
//                            multiFormatReader.setHints(hints);
//                            rawResult = multiFormatReader.decodeWithState(bitmap);
//                            result = new DecodeResult();
//                            result.setRawResult(rawResult);
//                        } catch (ReaderException re) {
//                            // continue
//                        } finally {
//                            multiFormatReader.reset();
//                        }
//                    }
//
//                    if(result != null){
//                        break;
//                    }
//                }
//
//            }

        }

        return result;
    }


    public static byte[] bitmapInvert(byte[] data, int width, int height){
        byte[] rotatedData = null;
//        Matrix matrix = new Matrix();
//        matrix.postRotate(degree);
//        Bitmap img_a = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
//        rotatedData = bitmap2Byte(img_a);
//        img_a.recycle();
        rotatedData= new byte[data.length];

        for (int y = 0; y< height; y++) {

            for (int x = 0; x < width; x++)

                rotatedData[x * height + height - y -1] = data[x + y * width];

        }

//        byte[] rotatedData = data;
//        for (int y = 0, rowStart = 0 * width + 0; y < height; y++, rowStart += width) {
//            int middle = rowStart + width / 2;
//            for (int x1 = rowStart, x2 = rowStart + width - 1; x1 < middle; x1++, x2--) {
//                byte temp = rotatedData[x1];
//                rotatedData[x1] = rotatedData[x2];
//                rotatedData[x2] = temp;
//            }
//        }
        return  rotatedData;
    }

    /**
     * YUV420sp
     *
     * @param inputWidth
     * @param inputHeight
     * @param scaled
     * @return
     */
    public static byte[] getYUV420sp(int inputWidth, int inputHeight,
                                     Bitmap scaled) {

        int mWidth = scaled.getWidth();
        int mHeight = scaled.getHeight();

        int[] mIntArray = new int[mWidth * mHeight];

        // Copy pixel data from the Bitmap into the 'intArray' array
        scaled.getPixels(mIntArray, 0, mWidth, 0, 0, mWidth, mHeight);
        byte [] yuv = new byte[inputWidth*inputHeight*3/2];
        // Call to encoding function : convert intArray to Yuv Binary data
        encodeYUV420SP(yuv, mIntArray, mWidth, mHeight);

//        int[] argb = new int[inputWidth * inputHeight];
//
//        scaled.getPixels(argb, 0, inputWidth, 0, 0, inputWidth, inputHeight);
//
//        byte[] yuv = new byte[inputWidth * inputHeight * 3 / 2];
//
//        encodeYUV420SP(yuv, argb, inputWidth, inputHeight);
//
//        scaled.recycle();

        return yuv;
    }

    /**
     * RGB转YUV420sp
     *
     * @param yuv420sp
     *            inputWidth * inputHeight * 3 / 2
     * @param argb
     *            inputWidth * inputHeight
     * @param width
     * @param height
     */
    private static void encodeYUV420SP(byte[] yuv420sp, int[] rgba, int width,
                                       int height) {
        final int frameSize = width * height;

        int[] U, V;
        U = new int[frameSize];
        V = new int[frameSize];

        final int uvwidth = width / 2;

        int r, g, b, y, u, v;
        for (int j = 0; j < height; j++) {
            int index = width * j;
            for (int i = 0; i < width; i++) {

                r = Color.red(rgba[index]);
                g = Color.green(rgba[index]);
                b = Color.blue(rgba[index]);

                // rgb to yuv
                y = (66 * r + 129 * g + 25 * b + 128) >> 8 + 16;
                u = (-38 * r - 74 * g + 112 * b + 128) >> 8 + 128;
                v = (112 * r - 94 * g - 18 * b + 128) >> 8 + 128;

                // clip y
                yuv420sp[index] = (byte) ((y < 0) ? 0 : ((y > 255) ? 255 : y));
                U[index] = u;
                V[index++] = v;
            }
        }
    }

    public static byte[] rgb2YCbCr420(int[] pixels, int width, int height) {
        int len = width * height;
        // yuv格式数组大小，y亮度占len长度，u,v各占len/4长度。
        byte[] yuv = new byte[len * 3 / 2];
        int y, u, v;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                // 屏蔽ARGB的透明度值
                int rgb = pixels[i * width + j] & 0x00FFFFFF;
                // 像素的颜色顺序为bgr，移位运算。
                int r = rgb & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = (rgb >> 16) & 0xFF;
                // 套用公式
                y = ((66 * r + 129 * g + 25 * b + 128) >> 8) + 16;
                u = ((-38 * r - 74 * g + 112 * b + 128) >> 8) + 128;
                v = ((112 * r - 94 * g - 18 * b + 128) >> 8) + 128;
                // rgb2yuv
                // y = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                // u = (int) (-0.147 * r - 0.289 * g + 0.437 * b);
                // v = (int) (0.615 * r - 0.515 * g - 0.1 * b);
                // RGB转换YCbCr
                // y = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                // u = (int) (-0.1687 * r - 0.3313 * g + 0.5 * b + 128);
                // if (u > 255)
                // u = 255;
                // v = (int) (0.5 * r - 0.4187 * g - 0.0813 * b + 128);
                // if (v > 255)
                // v = 255;
                // 调整
                y = y < 16 ? 16 : (y > 255 ? 255 : y);
                u = u < 0 ? 0 : (u > 255 ? 255 : u);
                v = v < 0 ? 0 : (v > 255 ? 255 : v);
                // 赋值
                yuv[i * width + j] = (byte) y;
                yuv[len + (i >> 1) * width + (j & ~1) + 0] = (byte) u;
                yuv[len + +(i >> 1) * width + (j & ~1) + 1] = (byte) v;
            }
        }
        return yuv;
    }

    public static Bitmap getSmallerBitmap(Bitmap bitmap){
        int size = bitmap.getWidth()*bitmap.getHeight() / 160000;
        if (size <= 1) return bitmap; // 如果小于
        else {
            Matrix matrix = new Matrix();
            matrix.postScale((float) (1 / Math.sqrt(size)), (float) (1 / Math.sqrt(size)));
            Bitmap resizeBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            return resizeBitmap;
        }
    }

    public static byte [] getNV21(int inputWidth, int inputHeight, Bitmap scaled) {

        int [] argb = new int[inputWidth * inputHeight];

        scaled.getPixels(argb, 0, inputWidth, 0, 0, inputWidth, inputHeight);

        byte [] yuv = new byte[inputWidth*inputHeight*3/2];
        encodeYUV420SP2(yuv, argb, inputWidth, inputHeight);

        scaled.recycle();

        return yuv;
    }


    public static void encodeYUV420SP2(byte[] yuv420sp, int[] argb, int width, int height) {
        final int frameSize = width * height;

        int yIndex = 0;
        int uvIndex = frameSize;

        int a, R, G, B, Y, U, V;
        int index = 0;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {

                a = (argb[index] & 0xff000000) >> 24; // a is not used obviously
                R = (argb[index] & 0xff0000) >> 16;
                G = (argb[index] & 0xff00) >> 8;
                B = (argb[index] & 0xff) >> 0;

                // well known RGB to YUV algorithm
                Y = ( (  66 * R + 129 * G +  25 * B + 128) >> 8) +  16;
                U = ( ( -38 * R -  74 * G + 112 * B + 128) >> 8) + 128;
                V = ( ( 112 * R -  94 * G -  18 * B + 128) >> 8) + 128;

                // NV21 has a plane of Y and interleaved planes of VU each sampled by a factor of 2
                //    meaning for every 4 Y pixels there are 1 V and 1 U.  Note the sampling is every other
                //    pixel AND every other scanline.
                yuv420sp[yIndex++] = (byte) ((Y < 0) ? 0 : ((Y > 255) ? 255 : Y));
                if (j % 2 == 0 && index % 2 == 0) {
                    yuv420sp[uvIndex++] = (byte)((V<0) ? 0 : ((V > 255) ? 255 : V));
                    yuv420sp[uvIndex++] = (byte)((U<0) ? 0 : ((U > 255) ? 255 : U));
                }

                index ++;
            }
        }
    }

    /*
 * 获取位图的YUV数据
 */
    public static byte[] getYUVByBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        int size = width * height;

        int pixels[] = new int[size];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

//        byte[] data = convertColorToByte(pixels);
        byte[] data = rgb2YCbCr420(pixels, width, height);

        return data;
    }

    /*
 * 像素数组转化为RGB数组
 */
    public static byte[] convertColorToByte(int color[]) {
        if (color == null) {
            return null;
        }

        byte[] data = new byte[color.length * 3];
        for (int i = 0; i < color.length; i++) {
            data[i * 3] = (byte) (color[i] >> 16 & 0xff);
            data[i * 3 + 1] = (byte) (color[i] >> 8 & 0xff);
            data[i * 3 + 2] = (byte) (color[i] & 0xff);
        }

        return data;

    }

    public static byte[] bitmap2Byte(Bitmap mScanBitmap) {
//        Bitmap bmp = Bitmap
//        return getNV21(bitmap.getWidth(), bitmap.getHeight(),bitmap);
        byte bitmapPixels[] = null;
        if(mScanBitmap != null){
        // 首先，要取得该图片的像素数组内容
            int[] data = new int[mScanBitmap.getWidth() * mScanBitmap.getHeight()];
            bitmapPixels = new byte[mScanBitmap.getWidth() * mScanBitmap.getHeight()];
            mScanBitmap.getPixels(data, 0, mScanBitmap.getWidth(), 0, 0, mScanBitmap.getWidth(), mScanBitmap.getHeight());

            // 将int数组转换为byte数组，也就是取像素值中蓝色值部分作为辨析内容
            for (int i = 0; i < data.length; i++) {
//                int grey = data[mScanBitmap.getWidth() * i + j];
                byte dest = 0;
                int  red   = (data[i] & 0x00ff0000) >> 16;  //取高两位
                int  green = (data[i] & 0x0000ff00) >> 8; //取中两位
                int  blue  =  data[i] & 0x000000ff; //取低两位
                dest = (byte)blue;
                if((byte)dest == 0){
                    dest = (byte)green;
                }
                if((byte)dest == 0){
                    dest = (byte)red;
                }

                bitmapPixels[i] = dest;
            }
        }
//        File file = new File(file_name);
//        byte[] alldata = null;
//        if (!file.exists()) {
//            return null;
//        }
//        FileInputStream is = null;
//        ByteArrayOutputStream outStream = null;
//        try {
//            is = new FileInputStream(file_name);
//            outStream = new ByteArrayOutputStream();
//            byte[] data = new byte[1024];
//            int count = -1;
//            while ((count = is.read(data, 0, 1024)) != -1)
//                outStream.write(data, 0, count);
//            // data = null;
//            alldata = outStream.toByteArray();
//        }
//        catch (Exception e){
//            e.printStackTrace();
//
//        }
//        finally {
//            if(outStream != null){
//                try {
//                    outStream.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//            if(is != null){
//                try {
//                    is.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//
//            }
//        }

        return bitmapPixels;
    }

    private static byte[] getBitmapBytes(Bitmap bitmap)
    {
        int chunkNumbers = 10;
        int bitmapSize = bitmap.getRowBytes() * bitmap.getHeight();
        byte[] imageBytes = new byte[bitmapSize];
        int rows, cols;
        int chunkHeight, chunkWidth;
        rows = cols = (int) Math.sqrt(chunkNumbers);
        chunkHeight = bitmap.getHeight() / rows;
        chunkWidth = bitmap.getWidth() / cols;

        int yCoord = 0;
        int bitmapsSizes = 0;

        for (int x = 0; x < rows; x++)
        {
            int xCoord = 0;
            for (int y = 0; y < cols; y++)
            {
                Bitmap bitmapChunk = Bitmap.createBitmap(bitmap, xCoord, yCoord, chunkWidth, chunkHeight);
                byte[] bitmapArray = getBytesFromBitmapChunk(bitmapChunk);
                System.arraycopy(bitmapArray, 0, imageBytes, bitmapsSizes, bitmapArray.length);
                bitmapsSizes = bitmapsSizes + bitmapArray.length;
                xCoord += chunkWidth;

                bitmapChunk.recycle();
                bitmapChunk = null;
            }
            yCoord += chunkHeight;
        }

        return imageBytes;
    }


    private static byte[] getBytesFromBitmapChunk(Bitmap bitmap)
    {
        int bitmapSize = bitmap.getRowBytes() * bitmap.getHeight();
        ByteBuffer byteBuffer = ByteBuffer.allocate(bitmapSize);
        bitmap.copyPixelsToBuffer(byteBuffer);
        byteBuffer.rewind();
        return byteBuffer.array();
    }

    private static  PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height) {

        // Go ahead and assume it's YUV rather than die.
        return new PlanarYUVLuminanceSource(data, width, height, 0, 0,
                width, height, false);
    }
    private static  Map<DecodeHintType,Object> getDefaultHint(){
        Map<DecodeHintType,Object> hints = new EnumMap<>(DecodeHintType.class);
        // The prefs can't change while the thread is running, so pick them up once here.
        EnumSet<BarcodeFormat> decodeFormats;

//      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        decodeFormats = EnumSet.noneOf(BarcodeFormat.class);
        if (Config.KEY_DECODE_1D_PRODUCT) {
            decodeFormats.addAll(DecodeFormatManager.PRODUCT_FORMATS);
        }
        if (Config.KEY_DECODE_1D_INDUSTRIAL) {
            decodeFormats.addAll(DecodeFormatManager.INDUSTRIAL_FORMATS);
        }
        if (Config.KEY_DECODE_QR) {
            decodeFormats.addAll(DecodeFormatManager.QR_CODE_FORMATS);
        }
        if (Config.KEY_DECODE_DATA_MATRIX) {
            decodeFormats.addAll(DecodeFormatManager.DATA_MATRIX_FORMATS);
        }
        if (Config.KEY_DECODE_AZTEC) {
            decodeFormats.addAll(DecodeFormatManager.AZTEC_FORMATS);
        }
        if (Config.KEY_DECODE_PDF417) {
            decodeFormats.addAll(DecodeFormatManager.PDF417_FORMATS);
        }

        hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);
        hints.put(DecodeHintType.CHARACTER_SET, "utf-8");

//        hints.put(DecodeHintType.NEED_RESULT_POINT_CALLBACK, resultPointCallback);

        return hints;
    }
}
