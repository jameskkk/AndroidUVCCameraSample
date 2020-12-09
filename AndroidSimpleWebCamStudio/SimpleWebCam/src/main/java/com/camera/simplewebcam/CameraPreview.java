package com.camera.simplewebcam;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue.IdleHandler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.camera.simplewebcam.Main.takePicture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, Runnable {

	private static final String TAG = MJpegHttpStreamer.class.getSimpleName();
	private static final boolean DEBUG = false;
	protected Context context;
	private SurfaceHolder holder;
    Thread mainLoop = null;
	private Bitmap bmp=null;
	private Handler handler;
	private takePicture buttonObject;
	private final VideoHandler videoHandler = new VideoHandler(this);

	private boolean cameraExists=false;
	private boolean shouldStop=false;

	// /dev/videox (x=cameraId+cameraBase) is used.
	// In some omap devices, system uses /dev/video[0-3],
	// so users must use /dev/video[4-].
	// In such a case, try cameraId=0 and cameraBase=4
	private int cameraId=0;
	private int cameraBase=0;

	// This definition also exists in ImageProc.h.
	// Webcam must support the resolution 640x480 with YUYV format.
	static final int IMG_WIDTH=640;
	static final int IMG_HEIGHT=480;

	// The following variables are used to draw camera images.
    private int winWidth=0;
    private int winHeight=0;
    private Rect rect;
    private int dw, dh;
    private float rate;
    // for manipulation of original image
	public Matrix mx = new Matrix();
	// for rendering to canvas
    private float scale_x, scale_y, pos_x;
	private Matrix mx_canvas = new Matrix();
	private MemoryOutputStream mJpegOutputStream = null;
	private MJpegHttpStreamer mMJpegHttpStreamer = null;
	private long lastExposureChange = System.currentTimeMillis();
	private int exposureIndex = -1;
	private long lastExposureSearch = 0;
	private boolean exposureSearching = false;
	private int exposures[] = {10, 100, 170, 400, 500};
	private double exposureLuminanceResults[] = {0,0,0,0,0};

    // JNI functions
    public native int prepareCamera(int videoid);
    public native int prepareCameraWithBase(int videoid, int camerabase);
    public native void processCamera();
    public native void stopCamera();
    public native void pixeltobmp(Bitmap bitmap);
    static {
        System.loadLibrary("ImageProc");
    }

    void setButtonObject(takePicture buttonObject)
    {
    	this.buttonObject = buttonObject;
    }

    public CameraPreview(Context context, AttributeSet attributeset) {
		super(context,attributeset);
		this.context = context;
		if(DEBUG) Log.d("WebCam","CameraPreview constructed");
		Log.d(TAG, "make camera preview");
		setFocusable(true);


		holder = getHolder();
		holder.addCallback(this);
		holder.setType(SurfaceHolder.SURFACE_TYPE_NORMAL);

		mJpegOutputStream = new MemoryOutputStream(9000000);
		mMJpegHttpStreamer = new MJpegHttpStreamer(2000, 9000000);
		mMJpegHttpStreamer.start();
	}


    private class VideoHandler implements IdleHandler
    {

    	CameraPreview mCp;
    	Matrix canvas_pos_scale = new Matrix();

    	public VideoHandler(CameraPreview cp) {
    		mCp = cp;
		}

		@Override
		public boolean queueIdle() {
			/*
			 * loop in the idle queue unless there are new messages
			 */
			while(true &&
				  (cameraExists || DEBUG)&&
				  !(handler.hasMessages(0)))
			{
	        	//Log.d("runnable","inside");
	        	//obtaining display area to draw a large image
	        	if(winWidth==0)
	        	{
	        		winWidth=mCp.getWidth();
	        		winHeight=mCp.getHeight();

	        		if(winWidth*3/4<=winHeight)
	        		{
	        			scale_x = ((float)(dw+winWidth-1)/(float)CameraPreview.IMG_WIDTH);
	        			scale_y = ((float)(dh+winWidth*3/4-1)/(float)CameraPreview.IMG_HEIGHT);
	        		}
	        		else
	        		{
	        			scale_x = ((float)(dw+winHeight*4/3 -1)/(float)CameraPreview.IMG_WIDTH);
	        			scale_y = ((float)(dh+winHeight-1)/(float)CameraPreview.IMG_HEIGHT);
	        		}
		        	canvas_pos_scale.setScale(scale_x, scale_y);
	        	}


	            Canvas canvas = getHolder().lockCanvas();
	            if (canvas != null)
	            {

		        	if(DEBUG)
		        	{
		        		bmp = Bitmap.createBitmap(mCp.winWidth, mCp.winHeight,Config.ARGB_8888);
		        	}
		        	else
		        	{
			        	// obtaining a camera image (pixel data are stored in an array in JNI).
			        	processCamera();
			        	// camera image to bmp
			        	pixeltobmp(bmp);
		        	}

					Log.d(TAG, "bmp size " + bmp.getWidth() + "x" + bmp.getHeight());
					updateExposure();

	            	mx_canvas.reset();
	            	// first apply flipping etc.
	            	mx_canvas.postConcat(mx);
	            	// second scale the image to fit the screen
	            	mx_canvas.postConcat(canvas_pos_scale);
	        		Log.d("canvas matrix", mx_canvas.toString());

	            	// draw camera bmp on canvas
	            	canvas.drawBitmap(bmp, mx_canvas, null);
					bmp.compress(Bitmap.CompressFormat.JPEG, 25, mJpegOutputStream);
					Log.d(TAG, "length of stream: " + mJpegOutputStream.getLength());
					mMJpegHttpStreamer.streamJpeg(mJpegOutputStream.getBuffer(), mJpegOutputStream.getLength(),
							System.currentTimeMillis());

					// Clean up
					mJpegOutputStream.seek(0);


					getHolder().unlockCanvasAndPost(canvas);
	            }
	            else
	            {
	            	Log.e("idleQueue","Canvas empty");
	            }

	            if(shouldStop){
	            	shouldStop = false;
	            }
	        }

			return true;
		}
    }

	public void chooseExposure(int index) {
		exposureIndex = index;
		try {
			Runtime.getRuntime().exec("/system/v4l2-ctl -d /dev/video4 -c exposure_absolute=" + exposures[index]);
		} catch (IOException e) {
			e.printStackTrace();
		}
		lastExposureChange = System.currentTimeMillis();
	}

	public void updateExposure() {
		double lum = getAverageLuminance(bmp);
		Log.d(TAG, "luminance: " + lum);

		if (!exposureSearching) {
			boolean poorExposure = ((lum > 210 && exposureIndex > 0) || (lum < 40 && exposureIndex < exposures.length - 1));
			if (System.currentTimeMillis() - lastExposureSearch > 5000 && poorExposure) {
				Log.d(TAG, "Run exposure search");
				chooseExposure(0);
				exposureSearching = true;
				lastExposureSearch = System.currentTimeMillis();
			}
		} else if (System.currentTimeMillis() - lastExposureChange > 300) {
			exposureLuminanceResults[exposureIndex] = lum;
			if (exposureIndex >= exposures.length - 1) {
				exposureSearching = false;
				int bestIndex = 0;
				Log.d(TAG, "exposureLuminanceResults" + Arrays.toString(exposureLuminanceResults));
				for (int i = 1; i < exposureLuminanceResults.length; i++) {
					if (Math.abs(exposureLuminanceResults[i] - 160) < Math.abs(exposureLuminanceResults[bestIndex] - 160)) {
						bestIndex = i;
					}
				}
				chooseExposure(bestIndex);
			} else {
				chooseExposure(exposureIndex + 1);
			}
		}
	}

	public double getAverageLuminance(Bitmap b) {
		int size = Math.min(b.getWidth(), b.getHeight());
		final double GS_RED = 0.35;
		final double GS_GREEN = 0.55;
		final double GS_BLUE = 0.1;
		int res = 0;
		int padding = 20;
		int count = 0;
		for (int x = padding; x < size - padding; x+=10) {
			for (int y = padding; y < size - padding; y+=10) {
				int p = b.getPixel(x, y);
				int brightness = (int) (GS_RED * Color.red(p) + GS_GREEN * Color.blue(p) + GS_BLUE * Color.green(p));
				res += brightness;
				count += 1;
			}
		}
		return (double) res / count;
	}

    @Override
    public void run() {

	Looper.prepare();

	/*
	 * currently every message will trigger saving an image
	 */
	 handler = new Handler() {
            public void handleMessage(Message msg) {

            	if(cameraExists)
            	{
	        		Date date = new Date();

	        		File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + File.separator + "simplewebcam");
	        		directory.mkdirs();
	            	String filename = directory.getAbsoluteFile() + File.separator + String.valueOf(date.getTime()) + ".jpg";

	            	try {
	         	       FileOutputStream out = new FileOutputStream(filename);
	         	       bmp.compress(Bitmap.CompressFormat.JPEG, 90, out);
	         	       Toast.makeText(context,"Saved image: " + filename, Toast.LENGTH_SHORT).show();
	         		} catch (Exception e) {
	         		       e.printStackTrace();
         		       Toast.makeText(context,"Failed to save image: " + filename, Toast.LENGTH_SHORT).show();
	         		}

            	}
            	else
            	{
            		Toast.makeText(context,"No Camera", Toast.LENGTH_LONG).show();
            	}
            }
        };
		Log.d(TAG, "run handler");

        /*
         * add idle handler, this is where the video is processed
         */
        Looper.myQueue().addIdleHandler(new VideoHandler(this));

        /*
         * sent message with our handler to the image button
         * so we can receive events like 'take a picture'
         */
		Message msg = buttonObject.getHandler().obtainMessage();
		msg.arg1 = 1;
		msg.obj = this.handler;
		buttonObject.getHandler().sendMessage(msg);

		Looper.loop();
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.d("WebCam", "surfaceCreated");
		if(bmp==null){
			bmp = Bitmap.createBitmap(IMG_WIDTH, IMG_HEIGHT, Bitmap.Config.ARGB_8888);
		}
		// /dev/videox (x=cameraId + cameraBase) is used
		int ret = prepareCameraWithBase(cameraId, cameraBase);

		if(ret!=-1) cameraExists = true;

		mainLoop = new Thread(this);
        mainLoop.start();
	}



	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		Log.d("WebCam", "surfaceChanged");
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.d("WebCam", "surfaceDestroyed");
		if(cameraExists){
			shouldStop = true;
			while(shouldStop){
				try{
					Thread.sleep(100); // wait for thread stopping
				}catch(Exception e){}
			}
		}
		stopCamera();
	}
}
