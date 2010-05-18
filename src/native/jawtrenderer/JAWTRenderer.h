/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#ifndef _JAWTRENDERER_H_
#define _JAWTRENDERER_H_

#include <jni.h>
#include <jawt.h>

#ifdef __cplusplus
extern "C" {
#endif

void JAWTRenderer_close
    (JNIEnv *jniEnv, jclass clazz, jlong handle, jobject component);
jlong JAWTRenderer_open(JNIEnv *jniEnv, jclass clazz, jobject component);
jboolean JAWTRenderer_paint
    (JAWT_DrawingSurfaceInfo *dsi, jclass clazz, jlong handle, jobject g);
jboolean JAWTRenderer_process
    (JNIEnv *jniEnv, jclass clazz,
     jlong handle, jobject component,
     jint *data, jint length,
     jint width, jint height);

#ifdef __cplusplus
} /* extern "C" { */
#endif

#endif /* _JAWTRENDERER_H_ */
