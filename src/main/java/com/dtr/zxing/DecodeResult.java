package com.dtr.zxing;

import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;

/**
 * Created by LinZaixiong on 2016/9/25.
 */

public class DecodeResult {

    private PlanarYUVLuminanceSource source;
    private Result rawResult;

    public Result getRawResult() {
        return rawResult;
    }

    public void setRawResult(Result rawResult) {
        this.rawResult = rawResult;
    }

    public PlanarYUVLuminanceSource getSource() {
        return source;
    }

    public void setSource(PlanarYUVLuminanceSource source) {
        this.source = source;
    }
}
