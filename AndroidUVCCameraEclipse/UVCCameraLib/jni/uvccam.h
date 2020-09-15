#ifndef __UVCCAM_H__
#define __UVCCAM_H__

#include <jni.h>

#include "util.h"

static int DEVICE_DESCRIPTOR = -1;
int* RGB_BUFFER = NULL;
int* Y_BUFFER = NULL;

// These are documented on the Java side, in NativeWebcam
jint Java_com_k2jstudio_uvccam_NativeUVCcam_startCamera(JNIEnv* env,
        jobject thiz, jstring deviceName, jint width, jint height);

void Java_com_k2jstudio_uvccam_NativeUVCcam_loadNextFrame(JNIEnv* env,
        jobject thiz, jobject bitmap);

jboolean Java_com_k2jstudio_uvccam_NativeUVCcam_cameraAttached(JNIEnv* env,
        jobject thiz);

void Java_com_k2jstudio_uvccam_NativeUVCcam_stopCamera(JNIEnv* env,
        jobject thiz);

#endif // __UVCCAM_H__
