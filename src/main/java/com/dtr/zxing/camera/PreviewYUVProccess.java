package com.dtr.zxing.camera;

/**
 * Created by LinZaixiong on 2016/10/7.
 */

public class PreviewYUVProccess {



    /**
     * Changes the brightness and the contrast of the image.
     * \param brightness: can be from -255 to 255, if brightness is negative, the image becomes dark.
     * \param contrast: can be from -100 to 100, the neutral value is 0.
     * \return true if everything is ok
     */

    public byte[] light(long brightness , long contrast ){

        byte cTable[] = null;
        float c=(100 + contrast)/100.0f;
        brightness+=128;
        cTable = new byte[256];
        for ( int i = 0; i < 256; i++) {
            cTable[ i] = ( byte) Math.max(0, Math.min(255,( int)(( i-128)* c + brightness + 0.5f)));
        }

        return cTable;
    }

    /**
     * Adjusts the color balance of the image
     * \param gamma can be from 0.1 to 5.
     * \return true if everything is ok
     * \sa GammaRGB
     */

    public byte[] Gamma(float gamma){
        byte cTable[] = null;

        if (gamma <= 0.0f) return cTable;

        double dinvgamma = 1/gamma;
        double dMax = Math.pow(255.0, dinvgamma) / 255.0;

        cTable = new byte[256];
        for (int i=0;i<256;i++)	{
            cTable[i] = (byte)Math.max(0,Math.min(255,(int)( Math.pow((double)i, dinvgamma) / dMax)));
        }

        return cTable;

    }


    public byte[] processLight(byte[] data, int srcWidth, int srcHeight, long brightness , long contrast){

        int iter = 0;

        byte[] ret = new byte[srcWidth*srcHeight];
        byte[] cTable = light(brightness , contrast);
        for ( int height=0; height< srcHeight; height++) {
            for ( int width=0; width< srcWidth; width++){
                ret[iter] = cTable[Math.abs(data[iter])];
                iter++;
            }
        }

        return ret;
    }


}
