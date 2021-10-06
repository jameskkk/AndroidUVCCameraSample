package com.calcomp.usbcamera.application;

import android.app.Application;

import com.calcomp.usbcamera.utils.CrashHandler;

/**application class
 *
 * Created by John Chen on 2020/9/28.
 */

public class MyApplication extends Application {
    private CrashHandler mCrashHandler;
    // File Directory in sd card
    public static final String DIRECTORY_NAME = "NKGUSBCamera";

    @Override
    public void onCreate() {
        super.onCreate();
        mCrashHandler = CrashHandler.getInstance();
        mCrashHandler.init(getApplicationContext(), getClass());
    }
}
