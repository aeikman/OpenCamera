/*
The contents of this file are subject to the Mozilla Public License
Version 1.1 (the "License"); you may not use this file except in
compliance with the License. You may obtain a copy of the License at
http://www.mozilla.org/MPL/

Software distributed under the License is distributed on an "AS IS"
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
License for the specific language governing rights and limitations
under the License.

The Original Code is collection of files collectively known as Open Camera.

The Initial Developer of the Original Code is Almalence Inc.
Portions created by Initial Developer are Copyright (C) 2013
by Almalence Inc. All Rights Reserved.
*/

#include <stdio.h>
#include <string.h>
#include <jni.h>
#include <android/log.h>

#include "almashot.h"
#include "blurless.h"
#include "superzoom.h"

#include "ImageConversionUtils.h"

// currently - no concurrent processing, using same instance for all processing types
static unsigned char *yuv[MAX_FRAMES] = {NULL};
static void *instance = NULL;
static int almashot_inited = 0;
static Uint8 *OutPic = NULL;



extern "C" JNIEXPORT jstring JNICALL Java_com_almalence_plugins_processing_night_AlmaShotNight_Initialize
(
	JNIEnv* env,
	jobject thiz
)
{
	char status[1024];
	int err=0;
	long mem_used, mem_free;

	if (almashot_inited == 0)
	{
		err = AlmaShot_Initialize(0);

		if (err == 0)
			almashot_inited = 1;
	}

	sprintf (status, "init status: %d\n", err);
	return env->NewStringUTF(status);
}


extern "C" JNIEXPORT jint JNICALL Java_com_almalence_plugins_processing_night_AlmaShotNight_Release
(
	JNIEnv* env,
	jobject thiz
)
{
	int i;

	if (almashot_inited == 1)
	{
		AlmaShot_Release();

		almashot_inited = 0;
	}

	return 0;
}


extern "C" JNIEXPORT jstring JNICALL Java_com_almalence_plugins_processing_night_AlmaShotNight_ConvertFromJpeg
(
	JNIEnv* env,
	jobject thiz,
	jintArray in,
	jintArray in_len,
	jint nFrames,
	jint sx,
	jint sy
)
{
	int *jpeg_length;
	unsigned char * *jpeg;
	char status[1024];

	jpeg = (unsigned char**)env->GetIntArrayElements(in, NULL);
	jpeg_length = (int*)env->GetIntArrayElements(in_len, NULL);

	DecodeAndRotateMultipleJpegs(yuv, jpeg, jpeg_length, sx, sy, nFrames, 0, 0);

	env->ReleaseIntArrayElements(in, (jint*)jpeg, JNI_ABORT);
	env->ReleaseIntArrayElements(in_len, (jint*)jpeg_length, JNI_ABORT);

	sprintf (status, "frames total: %d\n", (int)nFrames);
	return env->NewStringUTF(status);
}


extern "C" JNIEXPORT jstring JNICALL Java_com_almalence_plugins_processing_night_AlmaShotNight_BlurLessPreview
(
	JNIEnv* env,
	jobject thiz,
	jint sx,
	jint sy,
	jint sensorGainPref,
	jint DeGhostPref,
	jint mode,
	jint saturated,
	jint nImages
)
{
	int i;
	Uint8 *pview_yuv;
	Uint32 *pview;
	int nTable[3] = {1,3,7};	// sqrt of this value is used for thresholding at <=3, <=6 and >6
	int deghTable[3] = {256/2, 256, 3*256/2};

	//__android_log_print(ANDROID_LOG_ERROR, "CameraTest", "BlurLessPreview 1");

	BlurLess_Preview(&instance, yuv, NULL, NULL, NULL,
		256*3,
		deghTable[DeGhostPref],
		mode==2 ? 2:0, mode==2 ? nImages:0, sx, sy, 0, 64*nTable[sensorGainPref], 1, 0, saturated, saturated);

	//__android_log_print(ANDROID_LOG_ERROR, "CameraTest", "BlurLessPreview 3");

	return env->NewStringUTF("ok");
}


extern "C" JNIEXPORT jint JNICALL Java_com_almalence_plugins_processing_night_AlmaShotNight_BlurLessProcess
(
	JNIEnv* env,
	jobject thiz,
	jint sx,
	jint sy,
	jintArray jcrop,
	jboolean jrot,
	jboolean jmirror
)
{
	Uint8 *OutPic, *OutNV21;
	int *crop;

	crop = (int*)env->GetIntArrayElements(jcrop, NULL);

	crop[0]=crop[1]=crop[2]=crop[3]=-1;
	BlurLess_Process(instance, &OutPic, &crop[0], &crop[1], &crop[2], &crop[3]);


	OutNV21 = OutPic;
	if (jrot)
		OutNV21 = (Uint8 *)malloc(sx*sy+2*((sx+1)/2)*((sy+1)/2));

	TransformNV21(OutPic, OutNV21, sx, sy, crop, jmirror&&jrot, jmirror&&jrot, jrot);

	if (jrot)
	{
		free(OutPic);
		OutPic = OutNV21;
	}

	env->ReleaseIntArrayElements(jcrop, (jint*)crop, JNI_ABORT);

	return (jint)OutPic;
}


extern "C" JNIEXPORT jstring JNICALL Java_com_almalence_plugins_processing_night_AlmaShotNight_SuperZoomPreview
(
	JNIEnv* env,
	jobject thiz,
	jintArray in,
	jint nFrames,
	jint sx,
	jint sy,
	jint sxo,
	jint syo,
	jint sensorGainPref,
	jint DeGhostPref,
	jint saturated,
	jint noSres
)
{
	int i;
	void * *frames;
	Uint8 *pview_yuv;
	Uint32 *pview;
	int nTable[3] = {1,3,7};	// sqrt of this value is used for thresholding at <=3, <=6 and >6
	int deghTable[3] = {256/2, 256, 3*256/2};

	frames = (void**)env->GetIntArrayElements(in, NULL);

	for (i=0; i<nFrames; ++i)
	{
		// not really sure if this copy is needed
		yuv[i] = (Uint8*)frames[i];
	}

	//__android_log_print(ANDROID_LOG_INFO, "CameraTest", "b: %d (%d %d %d %d)  %d   %dx%d", (int)yuv, (int)yuv[0], (int)yuv[1], (int)yuv[2], (int)yuv[3], sensorGainPref, sx, sy);
	//SuperZoom_Preview(&instance, yuv, pview_yuv, sx, sy, sxo, syo, -1, -1, nFrames,
	SuperZoom_Preview(&instance, yuv, NULL, NULL, sx, sy, sxo, syo, sxo/4, syo/4, nFrames,
		256*nTable[sensorGainPref],
		deghTable[DeGhostPref],
		-1, saturated, 1, 1, 0, 2, 1, NULL, 0, 0);	// hack to get brightening (pass enh. level in kelvin2 parameter)

	env->ReleaseIntArrayElements(in, (jint*)frames, JNI_ABORT);

	return env->NewStringUTF("ok");
}


extern "C" JNIEXPORT jint JNICALL Java_com_almalence_plugins_processing_night_AlmaShotNight_SuperZoomProcess
(
	JNIEnv* env,
	jobject thiz,
	jint sx,
	jint sy,
	jintArray jcrop,
	jboolean jrot,
	jboolean jmirror
)
{
	Uint8 *OutPic, *OutNV21;
	int *crop;

	crop = (int*)env->GetIntArrayElements(jcrop, NULL);

	crop[0]=crop[1]=crop[2]=crop[3]=-1;

	SuperZoom_Process(instance, &OutPic, NULL, &crop[0], &crop[1], &crop[2], &crop[3]);

	OutNV21 = OutPic;
	if (jrot)
		OutNV21 = (Uint8 *)malloc(sx*sy+2*((sx+1)/2)*((sy+1)/2));

	TransformNV21(OutPic, OutNV21, sx, sy, crop, jmirror, 0, jrot);

	if (jrot)
	{
		free(OutPic);
		OutPic = OutNV21;
	}

	env->ReleaseIntArrayElements(jcrop, (jint*)crop, JNI_ABORT);

	return (jint)OutPic;
}


extern "C" JNIEXPORT void JNICALL Java_com_almalence_plugins_processing_night_AlmaShotNight_convertPreview(
		JNIEnv *env, jclass clazz, jbyteArray ain, jbyteArray aout, jint width,	jint height, jint outWidth, jint outHeight)
{
	jbyte *cImageIn = env->GetByteArrayElements(ain, 0);
	jbyte *cImageOut = env->GetByteArrayElements(aout, 0);

	NV21_to_RGB_scaled_rotated((unsigned char*)cImageIn, width, height, 0, 0, width, height, outWidth, outHeight, 3, (unsigned char*)cImageOut);

	env->ReleaseByteArrayElements(ain, cImageIn, 0);
	env->ReleaseByteArrayElements(aout, cImageOut, 0);
}
