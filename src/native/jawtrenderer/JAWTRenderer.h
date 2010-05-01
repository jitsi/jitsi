#ifndef _JAWTRENDERER_H_
#define _JAWTRENDERER_H_

#include <jni.h>
#include <jawt.h>

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

#endif /* _JAWTRENDERER_H_ */
