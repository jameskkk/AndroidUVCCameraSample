package com.calcomp.usbcamera;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.calcomp.libusbcamera.R;
import org.easydarwin.sw.TxtOverlay;
import com.serenegiant.usb.DeviceFilter;
import com.serenegiant.usb.Size;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.usb.common.AbstractUVCCameraHandler;
import com.serenegiant.usb.common.UVCCameraHandler;
import com.serenegiant.usb.encoder.RecordParams;
import com.serenegiant.usb.widget.CameraViewInterface;


import java.io.File;
import java.util.List;
import java.util.Objects;

/** UVCCamera Helper class
 *
 * Created by John Chen on 2021/09/30.
 */

public class UVCCameraHelper {
    public static final String ROOT_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()
            + File.separator;
    public static final String SUFFIX_JPEG = ".jpg";
    public static final String SUFFIX_MP4 = ".mp4";
    private static final String TAG = "UVCCameraHelper";
    private static final int REQ_PERMISSION_CAMERA = 18;
    private int previewWidth = 640;
    private int previewHeight = 480;
    public static final int FRAME_FORMAT_YUYV = UVCCamera.FRAME_FORMAT_YUYV;
    // Default using MJPEG
    // if your device is connected,but have no images
    // please try to change it to FRAME_FORMAT_YUYV
    public static final int FRAME_FORMAT_MJPEG = UVCCamera.FRAME_FORMAT_MJPEG;
    public static final int MODE_BRIGHTNESS = UVCCamera.PU_BRIGHTNESS;
    public static final int MODE_CONTRAST = UVCCamera.PU_CONTRAST;
    private int mFrameFormat = FRAME_FORMAT_MJPEG;

    private static UVCCameraHelper mCameraHelper;
    // USB Manager
    private USBMonitor mUSBMonitor;
    // Camera Handler
    private UVCCameraHandler mCameraHandler;
    private USBMonitor.UsbControlBlock mCtrlBlock;

    private Activity mActivity;
    private CameraViewInterface mCamView;

    private UVCCameraHelper() {
    }

    public static UVCCameraHelper getInstance() {
        if (mCameraHelper == null) {
            mCameraHelper = new UVCCameraHelper();
        }
        return mCameraHelper;
    }

    public void closeCamera() {
        if (mCameraHandler != null) {
            mCameraHandler.close();
        }
    }

    public interface OnMyDevConnectListener {
        void onAttachDev(UsbDevice device);

        void onDettachDev(UsbDevice device);

        void onConnectDev(UsbDevice device, boolean isConnected);

        void onDisConnectDev(UsbDevice device);
    }

    public void initUSBMonitor(Activity activity, CameraViewInterface cameraView, final OnMyDevConnectListener listener) {
        this.mActivity = activity;
        this.mCamView = cameraView;

        mUSBMonitor = new USBMonitor(activity.getApplicationContext(), new USBMonitor.OnDeviceConnectListener() {

            // called by checking usb device
            // do request device permission
            @Override
            public void onAttach(UsbDevice device) {
                if (listener != null) {
                    listener.onAttachDev(device);
                }
            }

            // called by taking out usb device
            // do close camera
            @Override
            public void onDettach(UsbDevice device) {
                if (listener != null) {
                    listener.onDettachDev(device);
                }
            }

            // called by connect to usb camera
            // do open camera,start previewing
            @Override
            public void onConnect(final UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock, boolean createNew) {
                mCtrlBlock = ctrlBlock;
                openCamera(ctrlBlock);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // wait for camera created
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        // start previewing
                        startPreview(mCamView);
                    }
                }).start();
                if(listener != null) {
                    listener.onConnectDev(device,true);
                }
            }

            // called by disconnect to usb camera
            // do nothing
            @Override
            public void onDisconnect(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock) {
                if (listener != null) {
                    listener.onDisConnectDev(device);
                }
            }

            @Override
            public void onCancel(UsbDevice device) {
            }
        });

        createUVCCamera();
    }

    public void createUVCCamera() {
        if (mCamView == null)
            throw new NullPointerException("CameraViewInterface cannot be null!");

        // release resources for initializing camera handler
        if (mCameraHandler != null) {
            mCameraHandler.release();
            mCameraHandler = null;
        }
        // initialize camera handler
        mCamView.setAspectRatio(previewWidth / (float)previewHeight);
        mCameraHandler = UVCCameraHandler.createHandler(mActivity, mCamView, 2,
                previewWidth, previewHeight, mFrameFormat);
    }

    public void updateResolution(int width, int height) {
        if (previewWidth == width && previewHeight == height) {
            return;
        }
        this.previewWidth = width;
        this.previewHeight = height;
        if (mCameraHandler != null) {
            mCameraHandler.release();
            mCameraHandler = null;
        }
        mCamView.setAspectRatio(previewWidth / (float)previewHeight);
        mCameraHandler = UVCCameraHandler.createHandler(mActivity,mCamView, 2,
                previewWidth, previewHeight, mFrameFormat);
        openCamera(mCtrlBlock);
        new Thread(new Runnable() {
            @Override
            public void run() {
                // wait for camera created
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // start previewing
                startPreview(mCamView);
            }
        }).start();
    }

    public void registerUSB() {
        if (mUSBMonitor != null) {
            mUSBMonitor.register();
        }
    }

    public void unregisterUSB() {
        if (mUSBMonitor != null) {
            mUSBMonitor.unregister();
        }
    }

    public boolean checkSupportFlag(final int flag) {
        return mCameraHandler != null && mCameraHandler.checkSupportFlag(flag);
    }

    public int getModelValue(final int flag) {
        return mCameraHandler != null ? mCameraHandler.getValue(flag) : 0;
    }

    public int setModelValue(final int flag, final int value) {
        return mCameraHandler != null ? mCameraHandler.setValue(flag, value) : 0;
    }

    public int resetModelValue(final int flag) {
        return mCameraHandler != null ? mCameraHandler.resetValue(flag) : 0;
    }

    public boolean requestPermission(int index) {
        boolean success = false;
        List<UsbDevice> devList = getUsbDeviceList();
        if (devList == null || devList.size() == 0) {
            return false;
        }
        int count = devList.size();
        if (index >= count) {
            new IllegalArgumentException("index illegal,should be < devList.size()");
        }
        if (mUSBMonitor != null) {
            // 20211008 Add by John, check CAMERA permission
            int hasPermission= ActivityCompat.checkSelfPermission(this.mActivity, Manifest.permission.CAMERA);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // Android 9 and later needs CAMERA permission to access UVC devices
                if (hasPermission == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "CAMERA permission was already granted");
                } else {
                    requestCameraPermission();
                }
            } else {
                Log.i(TAG, "Building version before Android 9, CAMERA permission: " + hasPermission);
            }

            success = mUSBMonitor.requestPermission(getUsbDeviceList().get(index));
        }

        return success;
    }

    private void requestCameraPermission() {
        Log.v(TAG, "Call requestCameraPermission()...");
        if (ActivityCompat.shouldShowRequestPermissionRationale(this.mActivity, Manifest.permission.CAMERA)) {
            if (this.mActivity != null) {
                // Request the permission
                Log.i(TAG, "CAMERA permission request...");
                ActivityCompat.requestPermissions(
                    this.mActivity,
                    new String[]{Manifest.permission.CAMERA},
                    REQ_PERMISSION_CAMERA);
            }
        } else {
            Log.i(TAG, "CAMERA permission request...");
            ActivityCompat.requestPermissions(
                this.mActivity,
                new String[]{Manifest.permission.CAMERA},
                REQ_PERMISSION_CAMERA
			);
        }
    }

    public int getUsbDeviceCount() {
        List<UsbDevice> devList = getUsbDeviceList();
        if (devList == null || devList.size() == 0) {
            return 0;
        }
        return devList.size();
    }

    public List<UsbDevice> getUsbDeviceList() {
        List<DeviceFilter> deviceFilters = DeviceFilter
                .getDeviceFilters(mActivity.getApplicationContext(), R.xml.device_filter);
        if (mUSBMonitor == null || deviceFilters == null)
//            throw new NullPointerException("mUSBMonitor ="+mUSBMonitor+"deviceFilters=;"+deviceFilters);
            return null;
        // matching all of filter devices
        return mUSBMonitor.getDeviceList(deviceFilters);
    }

    public void capturePicture(String savePath,AbstractUVCCameraHandler.OnCaptureListener listener) {
        if (mCameraHandler != null && mCameraHandler.isOpened()) {

            File file = new File(savePath);
            if(! Objects.requireNonNull(file.getParentFile()).exists()) {
                file.getParentFile().mkdirs();
            }
            mCameraHandler.captureStill(savePath,listener);
        }
    }

    public void startPusher(AbstractUVCCameraHandler.OnEncodeResultListener listener) {
        if (mCameraHandler != null && !isPushing()) {
            mCameraHandler.startRecording(null, listener);
        }
    }

    public void startPusher(RecordParams params, AbstractUVCCameraHandler.OnEncodeResultListener listener) {
        if (mCameraHandler != null && !isPushing()) {
            if(params.isSupportOverlay()) {
                TxtOverlay.install(mActivity.getApplicationContext());
            }
            mCameraHandler.startRecording(params, listener);
        }
    }

    public void stopPusher() {
        if (mCameraHandler != null && isPushing()) {
            mCameraHandler.stopRecording();
        }
    }

    public boolean isPushing() {
        if (mCameraHandler != null) {
            return mCameraHandler.isRecording();
        }
        return false;
    }

    public boolean isCameraOpened() {
        if (mCameraHandler != null) {
            return mCameraHandler.isOpened();
        }
        return false;
    }

    public void release() {
        if (mCameraHandler != null) {
            mCameraHandler.release();
            mCameraHandler = null;
        }
        if (mUSBMonitor != null) {
            mUSBMonitor.destroy();
            mUSBMonitor = null;
        }
    }

    public USBMonitor getUSBMonitor() {
        return mUSBMonitor;
    }

    public void setOnPreviewFrameListener(AbstractUVCCameraHandler.OnPreViewResultListener listener) {
        if(mCameraHandler != null) {
            mCameraHandler.setOnPreViewResultListener(listener);
        }
    }

    private void openCamera(USBMonitor.UsbControlBlock ctrlBlock) {
        if (mCameraHandler != null) {
            mCameraHandler.open(ctrlBlock);
        }
    }

    public void startPreview(CameraViewInterface cameraView) {
        SurfaceTexture st = cameraView.getSurfaceTexture();
        if (mCameraHandler != null) {
            mCameraHandler.startPreview(st);
        }
    }

    public void stopPreview() {
        if (mCameraHandler != null) {
            mCameraHandler.stopPreview();
        }
    }

    public void startCameraFoucs() {
        if (mCameraHandler != null) {
            mCameraHandler.startCameraFoucs();
        }
    }

    public List<Size> getSupportedPreviewSizes() {
        if (mCameraHandler == null)
            return null;
        return mCameraHandler.getSupportedPreviewSizes();
    }

    public void setDefaultPreviewSize(int defaultWidth,int defaultHeight) {
        if(mUSBMonitor != null) {
            throw new IllegalStateException("setDefaultPreviewSize should be call before initMonitor");
        }
        this.previewWidth = defaultWidth;
        this.previewHeight = defaultHeight;
    }

    public void setDefaultFrameFormat(int format) {
        if(mUSBMonitor != null) {
            throw new IllegalStateException("setDefaultFrameFormat should be call before initMonitor");
        }
        this.mFrameFormat = format;
    }

    public int getPreviewWidth() {
        return previewWidth;
    }

    public int getPreviewHeight() {
        return previewHeight;
    }
}
