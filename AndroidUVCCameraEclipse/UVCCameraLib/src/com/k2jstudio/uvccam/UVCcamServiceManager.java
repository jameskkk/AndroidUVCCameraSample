package com.k2jstudio.uvccam;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class UVCcamServiceManager {
	private static String TAG = "WebcamPreview";
	private CharSequence mCameraDeviceId = GlobalConfig.CAMERA_DEVICE_ID;
	
    private Object mServiceSyncToken = new Object();
    private UVCcamManager mWebcamManager;
    private Context mContext;
    private static UVCcamServiceManager instance;
    
    public UVCcamServiceManager() {
    	Log.i("UVCcamServiceManager", "UVCcamServiceManager = " + mCameraDeviceId);
    }
    
	public static UVCcamServiceManager getInstance(Context context) {
		if (null == instance) {
			instance = new UVCcamServiceManager();
		}
		instance.mContext = context;
				
		return instance;
	}
	
	public boolean startCamera() {
		try {
	        Bundle bundle = new Bundle();
			bundle.putString("CAMERA_DEVICE", GlobalConfig.CAMERA_DEVICE_PATH + mCameraDeviceId);
			Intent intent = new Intent();
			intent.setClass(mContext, UVCcamManager.class);
			//intent.setClassName("com.kinpo.uvccam", "com.kinpo.uvccam.UVCcamManager");
			intent.putExtras(bundle);	
			
			if (mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE)) {
				if (!mWebcamManager.isCameraExist()) {
					Log.i(TAG, "Camera not exist!");
					closeCamera();
					return false;
				} 
				Log.i(TAG, "Bind camera service successful!");
				return true;
			} else {
				Log.e(TAG, "Bind camera service failed!");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	public void closeCamera() {
        if (mWebcamManager != null) {
        	try {
	            Log.i(TAG, "Unbinding from webcam manager");
	            mContext.unbindService(mConnection);
	            mWebcamManager = null;
        	} catch (Exception e) {
        		e.printStackTrace();
        	}
        }
	}
	
	public boolean isOpenCamera() {
        if (mWebcamManager != null && mWebcamManager.isCameraExist()) {
        	return true;
        }
        
        return false;
	}
	
	public Bitmap getCaptureFrame() {
		synchronized(mServiceSyncToken) {
            if (mWebcamManager == null) {
                try {
                    mServiceSyncToken.wait(5000);
                } catch(InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (mWebcamManager != null) {
            	return mWebcamManager.getFrame();
            } else {
            	return null;
            }
		}
    }
	
	private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            Log.i(TAG, "Bound to WebcamManager");
            synchronized(mServiceSyncToken) {
                mWebcamManager = ((UVCcamManager.WebcamBinder)service).getService();
                mServiceSyncToken.notify();
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.w(TAG, "WebcamManager disconnected unexpectedly");
            synchronized(mServiceSyncToken) {
                mWebcamManager = null;
                mServiceSyncToken.notify();
            }
        }
    };

}
