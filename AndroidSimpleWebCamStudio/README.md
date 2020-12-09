# About
Fork of https://bitbucket.org/droidperception/simplewebcam. This displays the video video of your usb webcam connected to your android device.

It's also the android studio version

# Requirements
1) The kernel is V4L2 enabled... I think this is true since kitkat. You can compile your own kernel with:

 CONFIG_VIDEO_DEV=y

 CONFIG_VIDEO_V4L2_COMMON=y

 CONFIG_VIDEO_MEDIA=y

 CONFIG_USB_VIDEO_CLASS=y

 CONFIG_V4L_USB_DRIVERS=y

 CONFIG_USB_VIDEO_CLASS_INPUT_EVDEV=y

2) USB WebCam is UVC camera, and it supports 640x480 resolution with YUYV format. Tested with ELP-USBFHD01M-L21 (barebones webcam.. I think most should work).

Supported platform : Iconia Tab A500.

3) Your phone should be rooted. There's a command that runs `su -c \"chmod 666 <video_loc>\"`, so it needs root permissions. I have superSU installed to manage root permissions.
   - Below android 4.4, you don't need root. You can use this patch: https://bitbucket.org/droidperception/simplewebcam/commits/415cd2b4a1a40a10f5e05a621e2ec494bb06d065

 This application will also work on V4L2-enabled pandaboard and beagleboard.

4) android studio installed, ndk and sdk installed

# Compiling/Installing
	$ cd <project-location>
	$ <path-to-ndk>/ndk-build NDK_PROJECT_PATH=.

Then compile/run on android studio. Android studio will not compile the NDK stuff, [StackOverflow](http://stackoverflow.com/questions/27453085/execution-failed-for-task-appcompiledebugndk-failed-to-run-this-command-ndk) says to disable this by adding:

	sourceSets.main {
		jniLibs.srcDir 'src/main/libs'
		jni.srcDirs = [] //disable automatic ndk-build call
	}

# Troubleshooting
Make sure you are reading from the correct video device. The code expects you're reading from `/dev/video4`. You may want to change this to `/dev/video0`. Check out the comments in `CameraPreview.java` for

		private int cameraId=0;
		private int cameraBase=4;
