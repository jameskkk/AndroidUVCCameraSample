package com.camera.simplewebcam;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.Intent;
import android.graphics.Matrix;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.app.KeyguardManager;
import com.camera.simplewebcam.push.PushNotifications;
import com.camera.simplewebcam.push.GcmIntentService;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;

public class Main extends Activity {
	
	private static final String PREFERENCES_MATRIX = "com.camera.simplewebcam.flipmatrix_";
	private static final String WAKE_LOCK_TAG = "simplewebcam";
	private static final String TAG = Main.class.getSimpleName();

	private static final String MENU_ITEM_FLIPLR = "Flip Horizontal";
	private static final int MENU_ITEM_ID_FLIPLR = 1;
	private static final String MENU_ITEM_FLIPUD = "Flip Vertical";
	private static final int MENU_ITEM_ID_FLIPUD = 2;
	private static final int MENU_GROUP_FLIP = 1;

	CameraPreview cp;
	ImageButton takePictureButton;
	SharedPreferences preferences;
	SharedPreferences.Editor editor;
	private PowerManager.WakeLock mWakeLock = null;

	public Handler closeHandler;
	public Runnable closeRunnable;
	public boolean autocloseStopped = false;

	private boolean charging = false;

	@SuppressLint("InvalidWakeLockTag")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);
		final PowerManager powerManager =
				(PowerManager) getSystemService(POWER_SERVICE);
		mWakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
				WAKE_LOCK_TAG);

		preferences = this.getPreferences(MODE_PRIVATE);
		editor = preferences.edit();

		takePicture tpListener = new takePicture();
		takePictureButton = (ImageButton)findViewById(R.id.button1);
		takePictureButton.setOnClickListener(tpListener);

		cp = (CameraPreview)findViewById(R.id.cameraSurfaceView);
		cp.setButtonObject(tpListener);

		KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
		km.newKeyguardLock("name").disableKeyguard();

		String intentText = getIntent().getStringExtra(Intent.EXTRA_TEXT);
		handleIntent(intentText, true);
		if ("alive_check".equalsIgnoreCase(intentText)) {
			return;
		}

		String androidID = Settings.System.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
		new Utils.PostReq(new Utils.PostReq.Callback() {
			@Override
			public void onComplete(Boolean result) {
				Log.d(TAG, result ? "Done sending cameraOn" : "Error sending cameraOn");
			}
		}).execute(Utils.METEOR_URL + "/setGlobalState/" + androidID + "/cameraOn/true");

		if (!autocloseStopped) {
			closeHandler = new Handler();
			closeRunnable = new Runnable() {
				@Override
				public void run() {
					Intent sendIntent = new Intent(getApplicationContext(), Main.class);
					sendIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
					sendIntent.setAction(Intent.ACTION_SEND);
					sendIntent.putExtra(Intent.EXTRA_TEXT, "camera_off");
					startActivity(sendIntent);
				}
			};
		}
		if ("bumped".equals(intentText)) {
			handleBumped();
		} else {
			stopAutoclose();
		}


		(new PushNotifications(getApplicationContext(), this)).runRegisterInBackground();

		//load preferences to matrix
		Matrix tmp_mx = new Matrix();
		tmp_mx.reset();
		float[] mx_array = new float[9];
		tmp_mx.getValues(mx_array);
		for(int i=0;i<mx_array.length;i++)
		{
			mx_array[i] = preferences.getFloat(PREFERENCES_MATRIX + i,mx_array[i]);
		}
		this.cp.mx.setValues(mx_array);
	}



	public void keepChargeInRange() {
		float bat = getBatteryLevel();
		if (bat > 90) {
			setChargeState(false);
		} else if (bat < 30) {
			setChargeState(true);
		}
	}

	public void setChargeState(boolean chargeOn) {
		try {
			String chargeVal = chargeOn ? "1" : "0";
			Runtime.getRuntime().exec("su -c echo " + chargeVal + " > /sys/class/power_supply/battery/device/charge");
		} catch (IOException e) {
			Log.e(TAG, "problems with setChargeState", e);
			e.printStackTrace();
		}
	}
	// If the camera was turned on because the bike was bumped, it should only stay on temporarily
	public void handleBumped() {
		if (!autocloseStopped && closeHandler != null) {
			closeHandler.removeCallbacks(closeRunnable);
			closeHandler.postDelayed(closeRunnable, 120000);
		}
	}

    public float getBatteryLevel() {
        Intent batteryIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        // Error checking that probably isn't needed but I added just in case.
        if(level == -1 || scale == -1) {
            return 50.0f;
        }

        return ((float)level / (float)scale) * 100.0f;
    }

	public void stopAutoclose() {
		autocloseStopped = true;
		if (closeHandler != null) {
			closeHandler.removeCallbacks(closeRunnable);
		}
	}

    @Override
    protected void onResume()
    {
        super.onResume();
        mWakeLock.acquire();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        mWakeLock.release();
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		
		menu.add(MENU_GROUP_FLIP, MENU_ITEM_ID_FLIPLR, Menu.NONE, MENU_ITEM_FLIPLR);
		menu.add(MENU_GROUP_FLIP, MENU_ITEM_ID_FLIPUD, Menu.NONE, MENU_ITEM_FLIPUD);
		
		return super.onCreateOptionsMenu(menu);
	}

	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);//must store the new intent unless getIntent() will return the old one
		String intentText = intent.getStringExtra(Intent.EXTRA_TEXT);
		handleIntent(intentText, false);
	}

	private void handleIntent(String intentText, final boolean fromStartup) {
		Log.d(TAG, "got intent text" + intentText);
		if ("camera_on".equalsIgnoreCase(intentText)) {
			stopAutoclose();
		} else if ("camera_off".equalsIgnoreCase(intentText)) {
			Log.d(TAG, "TURN OFF");
			String androidID = Settings.System.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
			new Utils.PostReq(new Utils.PostReq.Callback() {
				@Override
				public void onComplete(Boolean result) {
					Log.d(TAG, "Done sending cameraOn, now killing");
					android.os.Process.killProcess(android.os.Process.myPid());
				}
			}).execute(Utils.METEOR_URL + "/setGlobalState/" + androidID + "/cameraOn/false");
		} else if ("alive_check".equalsIgnoreCase(intentText)) {
			Log.d(TAG, "Alive check: check battery and send a response back");
			keepChargeInRange();
			String androidID = Settings.System.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
			new Utils.PostReq(new Utils.PostReq.Callback() {
				@Override
				public void onComplete(Boolean result) {
					Log.d(TAG, "Done sending phoneResponded");
					if (fromStartup) {
						android.os.Process.killProcess(android.os.Process.myPid());
					}
				}
			}).execute(Utils.METEOR_URL + "/setGlobalState/" + androidID + "/bat/" + getBatteryLevel());

		} else if ("bumped".equalsIgnoreCase(intentText)) {
			handleBumped();
		}
	}


	@SuppressWarnings("null")
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		switch(item.getGroupId())
		{
			case MENU_GROUP_FLIP:
				
				switch(item.getItemId())
				{
					case MENU_ITEM_ID_FLIPLR:
						this.cp.mx.postScale(-1.0f,1.0f);
						this.cp.mx.postTranslate(CameraPreview.IMG_WIDTH, 0);
						break;
						
					case MENU_ITEM_ID_FLIPUD:
						this.cp.mx.postScale(1.0f,-1.0f);
						this.cp.mx.postTranslate(0, CameraPreview.IMG_HEIGHT);
						break;
						
					default:
						Log.e(this.getLocalClassName(), "unrecognized menu id group");
						break;
				}
				
				//save matrix to preferences
				float[] mx_array = new float[9];
				this.cp.mx.getValues(mx_array);
				for(int i=0;i<mx_array.length;i++)
				{
					editor.putFloat(PREFERENCES_MATRIX + i, mx_array[i]);
				}
				editor.commit();
				break;
				
			default:
				Log.e(this.getLocalClassName(), "unrecognized menu group");
				break;
		}
			
		
		return super.onOptionsItemSelected(item);
	}


class takePicture implements OnClickListener {

	Handler workerThreadHandler;
	
	Handler handler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			
			workerThreadHandler = (Handler)msg.obj;
				
		}
	};
	
	public Handler getHandler()
	{
		return this.handler;
	}
	
	/*
	 * Triggers a message sent to the worker thread which saves 
	 * the current image.
	 * 
	 * (non-Javadoc)
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	@Override
	public void onClick(View v) {
		
		Log.d("takePicture", "clicked");

//		Message msg = workerThreadHandler.obtainMessage();
//		msg.arg1 = 2;
//		msg.what = 0;
//		workerThreadHandler.sendMessage(msg);
		if (charging) {
			setChargeState(false);
		} else {
			setChargeState(true);
		}
		charging = !charging;

	}
	
}
	
}
