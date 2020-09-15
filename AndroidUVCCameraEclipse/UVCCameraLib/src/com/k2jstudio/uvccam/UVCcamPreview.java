package com.k2jstudio.uvccam;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.media.FaceDetector;
import android.media.FaceDetector.Face;
import android.os.Bundle;
import android.os.IBinder;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class UVCcamPreview extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private static String TAG = "WebcamPreview";
    private final int NUMBER_OF_FACE = 1;
    
    private Rect mViewWindow;
    private boolean mRunning = true;
    private Object mServiceSyncToken = new Object();
    private UVCcamManager mWebcamManager;
    private SurfaceHolder mHolder;
    
    private Paint mPaint;
    private FaceDetector.Face[] mFaceDetected;
    private int numberOfFaceDetected;
    
    private CharSequence mCameraDeviceId = GlobalConfig.CAMERA_DEVICE_ID;
    private boolean isFaceDetect = GlobalConfig.FACE_DETECT;
    
    public UVCcamPreview(Context context) {
        super(context);
        Log.i("UVCcamPreview1", "mCameraDeviceId = " + mCameraDeviceId);
        init();
    }
    
	public UVCcamPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.UVCcamPreview);
        if (typedArray != null) {
	        mCameraDeviceId = typedArray.getText(R.styleable.UVCcamPreview_camera_device);
	        isFaceDetect = typedArray.getBoolean(R.styleable.UVCcamPreview_face_detect, false);
	        Log.i("UVCcamPreview2", "mCameraDeviceId = " + mCameraDeviceId);
	        if (mCameraDeviceId == null || mCameraDeviceId.equals("")) {
				mCameraDeviceId = "1";
			}
	        typedArray.recycle();
        }
        
        init();
    }

    public UVCcamPreview(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		
		TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.UVCcamPreview, defStyle, 0);
		if (typedArray != null) {
	        mCameraDeviceId = typedArray.getText(R.styleable.UVCcamPreview_camera_device);
	        isFaceDetect = typedArray.getBoolean(R.styleable.UVCcamPreview_face_detect, false);
	        Log.i("UVCcamPreview3", "mCameraDeviceId = " + mCameraDeviceId);
			if (mCameraDeviceId == null || mCameraDeviceId.equals("")) {
				mCameraDeviceId = "1";
			}
			typedArray.recycle();
		}
		
		init();
	}
    
    private void init() {
        Log.d(TAG, "WebcamPreview constructed");
        setFocusable(true);

        mHolder = getHolder();
        mHolder.addCallback(this);
        
		if (isFaceDetect) {
			mPaint = new Paint();
			mPaint.setColor(Color.RED);
			mPaint.setStyle(Paint.Style.STROKE);
			mPaint.setStrokeWidth(3);
		}
    }

    private void initFaceView(Bitmap bitmap) {
    	 BitmapFactory.Options BitmapFactoryOptionsbfo = new BitmapFactory.Options();
    	 BitmapFactoryOptionsbfo.inPreferredConfig = Bitmap.Config.RGB_565;
    	 int imageWidth = bitmap.getWidth();
    	 int imageHeight = bitmap.getHeight();
    	 mFaceDetected = new FaceDetector.Face[NUMBER_OF_FACE];
    	 FaceDetector faceDetect = new FaceDetector(imageWidth, imageHeight, NUMBER_OF_FACE);
    	 numberOfFaceDetected = faceDetect.findFaces(bitmap, mFaceDetected); 
    }
    
    @Override
    public void run() {
        while(mRunning) {
            synchronized(mServiceSyncToken) {
                if(mWebcamManager == null) {
                    try {
                        mServiceSyncToken.wait();
                    } catch(InterruptedException e) {
                        break;
                    }
                }

                Bitmap bitmap = mWebcamManager.getFrame();
                Canvas canvas = mHolder.lockCanvas();
                if(canvas != null) {
                    drawOnCanvas(canvas, bitmap);
                    if (isFaceDetect) {
                    	initFaceView(bitmap);
                    	Log.i("initFaceView", "numberOfFaceDetected = " + numberOfFaceDetected);
						for (int i = 0; i < numberOfFaceDetected; i++) {
							Face face = mFaceDetected[i];
							PointF faceMidPoint = new PointF();
							face.getMidPoint(faceMidPoint);
							canvas.drawPoint(faceMidPoint.x, faceMidPoint.y, mPaint);
							canvas.drawCircle(faceMidPoint.x, faceMidPoint.y, face.eyesDistance(), mPaint);
						}
                    }
                    mHolder.unlockCanvasAndPost(canvas);
                }
            }
        }
    }

    protected void drawOnCanvas(Canvas canvas, Bitmap videoBitmap) {
        canvas.drawBitmap(videoBitmap, null, mViewWindow, null);
    }

    protected Rect getViewingWindow() {
        return mViewWindow;
    }

    public Bitmap getCaptureFrame() {
        return mWebcamManager.getFrame();
    }
    
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "Surface created");
        mRunning = true;
        
        Bundle bundle = new Bundle();
		bundle.putString("CAMERA_DEVICE", GlobalConfig.CAMERA_DEVICE_PATH + mCameraDeviceId);
		Intent intent = new Intent();
		intent.setClass(getContext(), UVCcamManager.class);
		//intent.setClassName("com.kinpo.uvccam", "com.kinpo.uvccam.UVCcamManager");
		intent.putExtras(bundle);
		
        if (getContext().bindService(intent, mConnection, Context.BIND_AUTO_CREATE)) {
        	(new Thread(this)).start();
        } else {
        	Log.e(TAG, "bindService Failed!");
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "Surface destroyed");
        mRunning = false;

        if (mWebcamManager != null) {
            Log.i(TAG, "Unbinding from webcam manager");
            getContext().unbindService(mConnection);
            mWebcamManager = null;
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int winWidth,
            int winHeight) {
        Log.d("WebCam", "surfaceChanged");
        int width, height, dw, dh;
        if(winWidth * 3 / 4 <= winHeight) {
            dw = 0;
            dh = (winHeight - winWidth * 3 / 4) / 2;
            width = dw + winWidth - 1;
            height = dh + winWidth * 3 / 4 - 1;
        } else {
            dw = (winWidth - winHeight * 4 / 3) / 2;
            dh = 0;
            width = dw + winHeight * 4 / 3 - 1;
            height = dh + winHeight - 1;
        }
        mViewWindow = new Rect(dw, dh, width, height);
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
                mRunning = false;
                mWebcamManager = null;
                mServiceSyncToken.notify();
            }
        }
    };
}
