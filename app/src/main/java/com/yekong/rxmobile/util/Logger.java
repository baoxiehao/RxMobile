package com.yekong.rxmobile.util;

import android.util.Log;

/**
 * Created by baoxiehao on 16/1/17.
 */
public class Logger {
    private static final String TAG = "RxMobile";

    public static void d(String tag, String msg) {
        Log.i(String.format("%s/%s", TAG, tag), msg);
    }

    public static void d(String tag, String msg, Throwable t) {
        Log.i(String.format("%s/%s", TAG, tag), msg, t);
    }
}
