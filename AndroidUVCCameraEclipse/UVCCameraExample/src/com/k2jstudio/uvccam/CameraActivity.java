package com.k2jstudio.uvccam;

import java.util.HashMap;
import java.util.Iterator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class CameraActivity extends Activity {
	UVCcamPreview mWebcamPreview;
	
	private static final String TAG = "CameraActivity";

	
	@SuppressWarnings("static-access")
	@SuppressLint("InlinedApi")
	public void getExtenalUSBDevice() {
//		UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

		UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
		HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
		Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
		while(deviceIterator.hasNext()){
		    UsbDevice device = deviceIterator.next();
		    Log.e(TAG, "UsbDevice() Id: " + device.getDeviceId()
		    	+ ", Protocol: " + device.getDeviceProtocol()
		    	+ ", DeviceName: " + device.getDeviceName());
		}

	}
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        getExtenalUSBDevice();
        
        mWebcamPreview = (UVCcamPreview) findViewById(R.id.uvccamPreview);

        final Button btnExit = (Button) findViewById(R.id.button_exit);
		btnExit.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
    }
}
