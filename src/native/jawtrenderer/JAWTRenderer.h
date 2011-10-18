/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#ifndef _JAWTRENDERER_H_
#define _JAWTRENDERER_H_

#include <jawt.h>
#include <jni.h>

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

#ifdef __APPLE__
void JAWTRenderer_addNotifyLightweightComponent
    (jlong handle, jobject component, jlong parentHandle);
jboolean JAWTRenderer_paintLightweightComponent
    (jlong handle, jobject component, jobject g);
void JAWTRenderer_processLightweightComponentEvent
    (jlong handle, jint x, jint y, jint width, jint height);
void JAWTRenderer_removeNotifyLightweightComponent
    (jlong handle, jobject component);
jstring JAWTRenderer_sysctlbyname(JNIEnv *jniEnv, jstring name);
#endif /* #ifdef __APPLE__ */

#ifdef __cplusplus
} /* extern "C" { */
#endif

#endif /* _JAWTRENDERER_H_ */
