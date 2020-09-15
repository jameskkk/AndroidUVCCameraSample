package com.k2jstudio.uvccam;

import android.graphics.Bitmap;

public interface UVCcam {
	public boolean isOpenCamera();
    public Bitmap getFrame();
    public void stop();
    public boolean isAttached();
}
