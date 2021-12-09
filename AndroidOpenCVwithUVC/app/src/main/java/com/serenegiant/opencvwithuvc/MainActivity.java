/*
 *  UVCCamera
 *  library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2014-2018 saki t_saki@serenegiant.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *  All files in the folder are under this Apache License, Version 2.0.
 *  Files in the libjpeg-turbo, libusb, libuvc, rapidjson folder
 *  may have a different license, see the respective files.
 */

package com.serenegiant.opencvwithuvc;

import android.animation.Animator;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.graphics.Typeface;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.serenegiant.common.BaseActivity;
import com.serenegiant.opencv.ImageProcessor;
import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.USBMonitor.OnDeviceConnectListener;
import com.serenegiant.usb.USBMonitor.UsbControlBlock;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.usbcameracommon.UVCCameraHandlerMultiSurface;
import com.serenegiant.utils.CpuMonitor;
import com.serenegiant.utils.ViewAnimationHelper;
import com.serenegiant.widget.UVCCameraTextureView;

import java.nio.ByteBuffer;
import java.util.Locale;

public final class MainActivity extends BaseActivity
	implements CameraDialog.CameraDialogParent {
	
	private static final boolean DEBUG = true;	// TODO set false on release
	private static final String TAG = "MainActivity";

	/**
	 * set true if you want to record movie using MediaSurfaceEncoder
	 * (writing frame data into Surface camera from MediaCodec
	 *  by almost same way as USBCameratest2)
	 * set false if you want to record movie using MediaVideoEncoder
	 */
    private static final boolean USE_SURFACE_ENCODER = false;

    /**
     * preview resolution(width)
     * if your camera does not support specific resolution and mode,
     * {@link UVCCamera#setPreviewSize(int, int, int)} throw exception
     */
    private static final int PREVIEW_WIDTH = 160;
    /**
     * preview resolution(height)
     * if your camera does not support specific resolution and mode,
     * {@link UVCCamera#setPreviewSize(int, int, int)} throw exception
     */
    private static final int PREVIEW_HEIGHT = 120;
    /**
     * preview mode
     * if your camera does not support specific resolution and mode,
     * {@link UVCCamera#setPreviewSize(int, int, int)} throw exception
     * 0:YUYV, other:MJPEG
     */
    private static final int PREVIEW_MODE = 0;

	protected static final int SETTINGS_HIDE_DELAY_MS = 2500;

	/**
	 * for accessing USB
	 */
	private USBMonitor mUSBMonitor;
	/**
	 * Handler to execute camera related methods sequentially on private thread
	 */
	private UVCCameraHandlerMultiSurface mCameraHandler;
	/**
	 * for camera preview display
	 */
	private UVCCameraTextureView mUVCCameraView;
	/**
	 * for display resulted images
 	 */
	protected SurfaceView mResultView;
	/**
	 * for open&start / stop&close camera preview
	 */
	private ToggleButton mCameraButton;
	/**
	 * button for start/stop recording
	 */
	private ImageButton mCaptureButton;

	private View mBrightnessButton, mContrastButton;
	private View mResetButton;
	private View mToolsLayout, mValueLayout;
	private SeekBar mSettingSeekbar;

	protected ImageProcessor mImageProcessor;
	private TextView mCpuLoadTv;
	private TextView mFpsTv;
	private final CpuMonitor cpuMonitor = new CpuMonitor();

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (DEBUG) Log.v(TAG, "onCreate:");
		setContentView(R.layout.activity_main);
		mCameraButton = findViewById(R.id.camera_button);
		mCameraButton.setOnCheckedChangeListener(mOnCheckedChangeListener);
		mCaptureButton = findViewById(R.id.capture_button);
		mCaptureButton.setOnClickListener(mOnClickListener);
		mCaptureButton.setVisibility(View.INVISIBLE);
		
		mUVCCameraView = findViewById(R.id.camera_view);
		mUVCCameraView.setOnLongClickListener(mOnLongClickListener);
		mUVCCameraView.setAspectRatio(PREVIEW_WIDTH / (float)PREVIEW_HEIGHT);

		mResultView = findViewById(R.id.result_view);
		
		mBrightnessButton = findViewById(R.id.brightness_button);
		mBrightnessButton.setOnClickListener(mOnClickListener);
		mContrastButton = findViewById(R.id.contrast_button);
		mContrastButton.setOnClickListener(mOnClickListener);
		mResetButton = findViewById(R.id.reset_button);
		mResetButton.setOnClickListener(mOnClickListener);
		mSettingSeekbar = findViewById(R.id.setting_seekbar);
		mSettingSeekbar.setOnSeekBarChangeListener(mOnSeekBarChangeListener);

		mToolsLayout = findViewById(R.id.tools_layout);
		mToolsLayout.setVisibility(View.INVISIBLE);
		mValueLayout = findViewById(R.id.value_layout);
		mValueLayout.setVisibility(View.INVISIBLE);

		mCpuLoadTv = findViewById(R.id.cpu_load_textview);
		mCpuLoadTv.setTypeface(Typeface.MONOSPACE);
		//
		mFpsTv = findViewById(R.id.fps_textview);
		mFpsTv.setText(null);
		mFpsTv.setTypeface(Typeface.MONOSPACE);

		mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);
		mCameraHandler = UVCCameraHandlerMultiSurface.createHandler(this, mUVCCameraView,
			USE_SURFACE_ENCODER ? 0 : 1, PREVIEW_WIDTH, PREVIEW_HEIGHT, PREVIEW_MODE);
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (DEBUG) Log.v(TAG, "onStart:");
		mUSBMonitor.register();
		queueEvent(mCPUMonitorTask, 1000);
		runOnUiThread(mFpsTask, 1000);
	}

	@Override
	protected void onStop() {
		if (DEBUG) Log.v(TAG, "onStop:");
		removeEvent(mCPUMonitorTask);
		removeFromUiThread(mFpsTask);
		stopPreview();
		mCameraHandler.close();
		setCameraButton(false);
		super.onStop();
	}

	@Override
	public void onDestroy() {
		if (DEBUG) Log.v(TAG, "onDestroy:");
        if (mCameraHandler != null) {
	        mCameraHandler.release();
	        mCameraHandler = null;
        }
        if (mUSBMonitor != null) {
	        mUSBMonitor.destroy();
	        mUSBMonitor = null;
        }
        mUVCCameraView = null;
        mCameraButton = null;
        mCaptureButton = null;
		super.onDestroy();
	}

	/**
	 * event handler when click camera / capture button
	 */
	private final OnClickListener mOnClickListener = new OnClickListener() {
		@Override
		public void onClick(final View view) {
			switch (view.getId()) {
			case R.id.capture_button:
				if (mCameraHandler.isOpened()) {
					if (checkPermissionWriteExternalStorage() && checkPermissionAudio()) {
						if (!mCameraHandler.isRecording()) {
							mCaptureButton.setColorFilter(0xffff0000);	// turn red
							mCameraHandler.startRecording();
						} else {
							mCaptureButton.setColorFilter(0);	// return to default color
							mCameraHandler.stopRecording();
						}
					}
				}
				break;
			case R.id.brightness_button:
				showSettings(UVCCamera.PU_BRIGHTNESS);
				break;
			case R.id.contrast_button:
				showSettings(UVCCamera.PU_CONTRAST);
				break;
			case R.id.reset_button:
				resetSettings();
				break;
			}
		}
	};

	private final CompoundButton.OnCheckedChangeListener mOnCheckedChangeListener
		= new CompoundButton.OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(
			final CompoundButton compoundButton, final boolean isChecked) {
			
			switch (compoundButton.getId()) {
			case R.id.camera_button:
				if (isChecked && !mCameraHandler.isOpened()) {
					CameraDialog.showDialog(MainActivity.this);
				} else {
					stopPreview();
				}
				break;
			}
		}
	};

	/**
	 * capture still image when you long click on preview image(not on buttons)
	 */
	private final OnLongClickListener mOnLongClickListener = new OnLongClickListener() {
		@Override
		public boolean onLongClick(final View view) {
			switch (view.getId()) {
			case R.id.camera_view:
				if (mCameraHandler.isOpened()) {
					if (checkPermissionWriteExternalStorage()) {
						mCameraHandler.captureStill();
					}
					return true;
				}
			}
			return false;
		}
	};

	private void setCameraButton(final boolean isOn) {
		if (DEBUG) Log.v(TAG, "setCameraButton:isOn=" + isOn);
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (mCameraButton != null) {
					try {
						mCameraButton.setOnCheckedChangeListener(null);
						mCameraButton.setChecked(isOn);
					} finally {
						mCameraButton.setOnCheckedChangeListener(mOnCheckedChangeListener);
					}
				}
				if (!isOn && (mCaptureButton != null)) {
					mCaptureButton.setVisibility(View.INVISIBLE);
				}
			}
		}, 0);
		updateItems();
	}

	private int mPreviewSurfaceId;
	private void startPreview() {
		if (DEBUG) Log.v(TAG, "startPreview:");
		mUVCCameraView.resetFps();
		mCameraHandler.startPreview();
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				try {
					final SurfaceTexture st = mUVCCameraView.getSurfaceTexture();
					if (st != null) {
						final Surface surface = new Surface(st);
						mPreviewSurfaceId = surface.hashCode();
						mCameraHandler.addSurface(mPreviewSurfaceId, surface, false);
					}
					mCaptureButton.setVisibility(View.VISIBLE);
					mResultView.setVisibility(View.GONE); //[John] Invisible the image process view
					startImageProcessor(PREVIEW_WIDTH, PREVIEW_HEIGHT); //[John] Start image processor
				} catch (final Exception e) {
					Log.w(TAG, e);
				}
			}
		});
		updateItems();
	}

	private void stopPreview() {
		if (DEBUG) Log.v(TAG, "stopPreview:");
		stopImageProcessor();
		if (mPreviewSurfaceId != 0) {
			mCameraHandler.removeSurface(mPreviewSurfaceId);
			mPreviewSurfaceId = 0;
		}
		mCameraHandler.close();
		setCameraButton(false);
	}
	
	private final OnDeviceConnectListener mOnDeviceConnectListener
		= new OnDeviceConnectListener() {
		
		@Override
		public void onAttach(final UsbDevice device) {
			Toast.makeText(MainActivity.this,
				"USB_DEVICE_ATTACHED", Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onConnect(final UsbDevice device,
			final UsbControlBlock ctrlBlock, final boolean createNew) {
			
			if (DEBUG) Log.v(TAG, "onConnect:");
			mCameraHandler.open(ctrlBlock);
			startPreview();
			updateItems();
		}

		@Override
		public void onDisconnect(final UsbDevice device,
			final UsbControlBlock ctrlBlock) {
			
			if (DEBUG) Log.v(TAG, "onDisconnect:");
			if (mCameraHandler != null) {
				queueEvent(new Runnable() {
					@Override
					public void run() {
						stopPreview();
					}
				}, 0);
				updateItems();
			}
		}
		@Override
		public void onDettach(final UsbDevice device) {
			Toast.makeText(MainActivity.this,
				"USB_DEVICE_DETACHED", Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onCancel(final UsbDevice device) {
			setCameraButton(false);
		}
	};

	/**
	 * to access from CameraDialog
	 * @return
	 */
	@Override
	public USBMonitor getUSBMonitor() {
		return mUSBMonitor;
	}

	@Override
	public void onDialogResult(boolean canceled) {
		if (DEBUG) Log.v(TAG, "onDialogResult:canceled=" + canceled);
		if (canceled) {
			setCameraButton(false);
		}
	}

//================================================================================
	private boolean isActive() {
		return mCameraHandler != null && mCameraHandler.isOpened();
	}

	private boolean checkSupportFlag(final int flag) {
		return mCameraHandler != null && mCameraHandler.checkSupportFlag(flag);
	}

	private int getValue(final int flag) {
		return mCameraHandler != null ? mCameraHandler.getValue(flag) : 0;
	}

	private int setValue(final int flag, final int value) {
		return mCameraHandler != null ? mCameraHandler.setValue(flag, value) : 0;
	}

	private int resetValue(final int flag) {
		return mCameraHandler != null ? mCameraHandler.resetValue(flag) : 0;
	}

	private void updateItems() {
		runOnUiThread(mUpdateItemsOnUITask, 100);
	}

	private final Runnable mUpdateItemsOnUITask = new Runnable() {
		@Override
		public void run() {
			if (isFinishing()) return;
			final int visible_active = isActive() ? View.VISIBLE : View.INVISIBLE;
			mToolsLayout.setVisibility(visible_active);
			mBrightnessButton.setVisibility(
		    	checkSupportFlag(UVCCamera.PU_BRIGHTNESS)
		    	? visible_active : View.INVISIBLE);
			mContrastButton.setVisibility(
		    	checkSupportFlag(UVCCamera.PU_CONTRAST)
		    	? visible_active : View.INVISIBLE);
		}
	};

	private int mSettingMode = -1;
	/**
	 * show setting view
	 * @param mode
	 */
	private final void showSettings(final int mode) {
		if (DEBUG) Log.v(TAG, String.format("showSettings:%08x", mode));
		hideSetting(false);
		if (isActive()) {
			switch (mode) {
			case UVCCamera.PU_BRIGHTNESS:
			case UVCCamera.PU_CONTRAST:
				mSettingMode = mode;
				mSettingSeekbar.setProgress(getValue(mode));
				ViewAnimationHelper.fadeIn(mValueLayout, -1, 0, mViewAnimationListener);
				break;
			}
		}
	}

	private void resetSettings() {
		if (isActive()) {
			switch (mSettingMode) {
			case UVCCamera.PU_BRIGHTNESS:
			case UVCCamera.PU_CONTRAST:
				mSettingSeekbar.setProgress(resetValue(mSettingMode));
				break;
			}
		}
		mSettingMode = -1;
		ViewAnimationHelper.fadeOut(mValueLayout, -1, 0, mViewAnimationListener);
	}

	/**
	 * hide setting view
	 * @param fadeOut
	 */
	protected final void hideSetting(final boolean fadeOut) {
		removeFromUiThread(mSettingHideTask);
		if (fadeOut) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					ViewAnimationHelper.fadeOut(mValueLayout, -1, 0, mViewAnimationListener);
				}
			}, 0);
		} else {
			try {
				mValueLayout.setVisibility(View.GONE);
			} catch (final Exception e) {
				// ignore
			}
			mSettingMode = -1;
		}
	}

	protected final Runnable mSettingHideTask = new Runnable() {
		@Override
		public void run() {
			hideSetting(true);
		}
	};

	/**
	 * callback listener to change camera control values
	 */
	private final SeekBar.OnSeekBarChangeListener mOnSeekBarChangeListener
		= new SeekBar.OnSeekBarChangeListener() {
		
		@Override
		public void onProgressChanged(final SeekBar seekBar,
			final int progress, final boolean fromUser) {
			
			if (fromUser) {
				runOnUiThread(mSettingHideTask, SETTINGS_HIDE_DELAY_MS);
			}
		}

		@Override
		public void onStartTrackingTouch(final SeekBar seekBar) {
		}

		@Override
		public void onStopTrackingTouch(final SeekBar seekBar) {
			runOnUiThread(mSettingHideTask, SETTINGS_HIDE_DELAY_MS);
			if (isActive() && checkSupportFlag(mSettingMode)) {
				switch (mSettingMode) {
				case UVCCamera.PU_BRIGHTNESS:
				case UVCCamera.PU_CONTRAST:
					setValue(mSettingMode, seekBar.getProgress());
					break;
				}
			}	// if (active)
		}
	};

	private final ViewAnimationHelper.ViewAnimationListener
		mViewAnimationListener = new ViewAnimationHelper.ViewAnimationListener() {
		@Override
		public void onAnimationStart(@NonNull final Animator animator,
			@NonNull final View target, final int animationType) {
			
//			if (DEBUG) Log.v(TAG, "onAnimationStart:");
		}

		@Override
		public void onAnimationEnd(@NonNull final Animator animator,
			@NonNull final View target, final int animationType) {
			
			final int id = target.getId();
			switch (animationType) {
			case ViewAnimationHelper.ANIMATION_FADE_IN:
			case ViewAnimationHelper.ANIMATION_FADE_OUT:
			{
				final boolean fadeIn = animationType == ViewAnimationHelper.ANIMATION_FADE_IN;
				if (id == R.id.value_layout) {
					if (fadeIn) {
						runOnUiThread(mSettingHideTask, SETTINGS_HIDE_DELAY_MS);
					} else {
						mValueLayout.setVisibility(View.GONE);
						mSettingMode = -1;
					}
				} else if (!fadeIn) {
//					target.setVisibility(View.GONE);
				}
				break;
			}
			}
		}

		@Override
		public void onAnimationCancel(@NonNull final Animator animator,
			@NonNull final View target, final int animationType) {
			
//			if (DEBUG) Log.v(TAG, "onAnimationStart:");
		}
	};

//================================================================================
	private final Runnable mCPUMonitorTask = new Runnable() {
		@Override
		public void run() {
			if (cpuMonitor.sampleCpuUtilization()) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						mCpuLoadTv.setText(String.format(Locale.US, "CPU:%3d/%3d/%3d",
							cpuMonitor.getCpuCurrent(),
							cpuMonitor.getCpuAvg3(),
							cpuMonitor.getCpuAvgAll()));
					}
				});
			}
			queueEvent(this, 1000);
		}
	};
	
	private final Runnable mFpsTask = new Runnable() {
		@Override
		public void run() {
			float srcFps, resultFps;
			if (mUVCCameraView != null) {
				mUVCCameraView.updateFps();
				srcFps = mUVCCameraView.getFps();
			} else {
				srcFps = 0.0f;
			}
			if (mImageProcessor != null) {
				mImageProcessor.updateFps();
				resultFps = mImageProcessor.getFps();
			} else {
				resultFps = 0.0f;
			}
			mFpsTv.setText(String.format(Locale.US, "FPS:%4.1f->%4.1f", srcFps, resultFps));
			runOnUiThread(this, 1000);
		}
	};

//================================================================================
	private volatile boolean mIsRunning;
	private int mImageProcessorSurfaceId;
	
	/**
	 * start image processing
	 * @param processing_width
	 * @param processing_height
	 */
	protected void startImageProcessor(final int processing_width, final int processing_height) {
		if (DEBUG) Log.v(TAG, "startImageProcessor:");
		mIsRunning = true;
		if (mImageProcessor == null) {
			mImageProcessor = new ImageProcessor(PREVIEW_WIDTH, PREVIEW_HEIGHT,	// src size
				new MyImageProcessorCallback(processing_width, processing_height));	// processing size
			mImageProcessor.start(processing_width, processing_height);	// processing size
			final Surface surface = mImageProcessor.getSurface();
			mImageProcessorSurfaceId = surface != null ? surface.hashCode() : 0;
			if (mImageProcessorSurfaceId != 0) {
				mCameraHandler.addSurface(mImageProcessorSurfaceId, surface, false);
			}
		}
	}
	
	/**
	 * stop image processing
	 */
	protected void stopImageProcessor() {
		if (DEBUG) Log.v(TAG, "stopImageProcessor:");
		if (mImageProcessorSurfaceId != 0) {
			mCameraHandler.removeSurface(mImageProcessorSurfaceId);
			mImageProcessorSurfaceId = 0;
		}
		if (mImageProcessor != null) {
			mImageProcessor.release();
			mImageProcessor = null;
		}
	}
	
	/**
	 * callback listener from `ImageProcessor`
	 */
	protected class MyImageProcessorCallback implements ImageProcessor.ImageProcessorCallback {
		private final int width, height;
		private final Matrix matrix = new Matrix();
		private Bitmap mFrame;
		protected MyImageProcessorCallback(
			final int processing_width, final int processing_height) {
			
			width = processing_width;
			height = processing_height;
			Log.i(TAG, "MyImageProcessorCallback width: " + width + ", height: " + height);
		}

		@Override
		public void onFrame(final ByteBuffer frame) {
			if (mResultView != null) {
				final SurfaceHolder holder = mResultView.getHolder();
				if ((holder == null)
					|| (holder.getSurface() == null)
					|| (frame == null)) return;

//--------------------------------------------------------------------------------
// Using SurfaceView and Bitmap to draw resulted images is inefficient way,
// but functions onOpenCV are relatively heavy and expect slower than source
// frame rate. So currently just use the way to simply this sample app.
// If you want to use much efficient way, try to use as same way as
// UVCCamera class use to receive images from UVC camera.
//--------------------------------------------------------------------------------
				if (mFrame == null) {
					mFrame = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
					final float scaleX = mResultView.getWidth() / (float)width;
					final float scaleY = mResultView.getHeight() / (float)height;
					matrix.reset();
					matrix.postScale(scaleX, scaleY);
				}
				try {
					frame.clear();
					mFrame.copyPixelsFromBuffer(frame);
					final Canvas canvas = holder.lockCanvas();
					if (canvas != null) {
						try {
							canvas.drawBitmap(mFrame, matrix, null);
						} catch (final Exception e) {
							Log.w(TAG, e);
						} finally {
							holder.unlockCanvasAndPost(canvas);
						}
					}
				} catch (final Exception e) {
					Log.w(TAG, e);
				}
			}
		}

		@Override
		public void onResult(final int type, final float[] result) {
			// do something
		}
		
	}

}
