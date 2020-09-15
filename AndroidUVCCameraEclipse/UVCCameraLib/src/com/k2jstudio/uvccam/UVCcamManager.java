package com.k2jstudio.uvccam;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class UVCcamManager extends Service {
    private static String TAG = "WebcamManager";
	private static String DEFAULT_CAMERA_DEVICE = "/dev/video0";
    
    private IBinder mBinder = new WebcamBinder();
    private UVCcam mUVCcam;
    private String mCameraDevice;
    
    public class WebcamBinder extends Binder {
        public UVCcamManager getService() {
            return UVCcamManager.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Service starting");
       
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Service being destroyed");
        
        if (mUVCcam != null) {
        	mUVCcam.stop();
        }
    }

    @SuppressLint("NewApi")
	@Override
	@Deprecated
	public void onStart(Intent intent, int arg1) {
		super.onStart(intent, arg1);
		Log.i(TAG, "Service start in response to " + intent);
		
		Bundle bundle = intent.getExtras();
		if (bundle != null) {
			mCameraDevice = bundle.getString("CAMERA_DEVICE", DEFAULT_CAMERA_DEVICE);
		}
		mUVCcam = new NativeUVCcam(mCameraDevice);
	}

	@SuppressLint("NewApi")
	@Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "Service binding in response to " + intent);
        
        Bundle bundle = intent.getExtras();
		if (bundle != null) {
			mCameraDevice = bundle.getString("CAMERA_DEVICE", DEFAULT_CAMERA_DEVICE);
		}
		mUVCcam = new NativeUVCcam(mCameraDevice);
		
        return mBinder;
    }

	public boolean isCameraExist() {
		if (mUVCcam == null || !mUVCcam.isAttached() || !mUVCcam.isOpenCamera()) {
			return false;
		}
		
		return true;
	}
	
    public Bitmap getFrame() {
        if(!mUVCcam.isAttached()) {
            stopSelf();
        }
        return mUVCcam.getFrame();
    }
}
