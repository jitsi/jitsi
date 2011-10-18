/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#include "net_java_sip_communicator_impl_neomedia_jmfext_media_renderer_video_JAWTRenderer.h"
#include "JAWTRenderer.h"

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_jmfext_media_renderer_video_JAWTRenderer_addNotifyLightweightComponent
    (JNIEnv *jniEnv, jclass clazz,
    jlong handle, jobject component,
    jlong parentHandle)
{
#ifdef __APPLE__
    JAWTRenderer_addNotifyLightweightComponent(handle, component, parentHandle);
#endif /* #ifdef __APPLE__ */
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_jmfext_media_renderer_video_JAWTRenderer_close
    (JNIEnv *jniEnv, jclass clazz, jlong handle, jobject component)
{
    JAWTRenderer_close(jniEnv, clazz, handle, component);
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_jmfext_media_renderer_video_JAWTRenderer_open
    (JNIEnv *jniEnv, jclass clazz, jobject component)
{
    return JAWTRenderer_open(jniEnv, clazz, component);
}

JNIEXPORT jboolean JNICALL
Java_net_java_sip_communicator_impl_neomedia_jmfext_media_renderer_video_JAWTRenderer_paint
    (JNIEnv *jniEnv, jclass clazz, jlong handle, jobject component, jobject g)
{
    JAWT awt;
    jboolean wantsPaint;

    awt.version = JAWT_VERSION_1_3;
    wantsPaint = JNI_TRUE;
    if (JAWT_GetAWT(jniEnv, &awt) != JNI_FALSE)
    {
        JAWT_DrawingSurface *ds;

        ds = awt.GetDrawingSurface(jniEnv, component);
        if (ds)
        {
            jint dsLock;

            dsLock = ds->Lock(ds);
            if (0 == (dsLock & JAWT_LOCK_ERROR))
            {
                JAWT_DrawingSurfaceInfo *dsi;

                dsi = ds->GetDrawingSurfaceInfo(ds);
                if (dsi && dsi->platformInfo)
                {
                    /*
                     * The function arguments jniEnv and component are now
                     * available as the fields env and target, respectively, of
                     * the JAWT_DrawingSurface which is itself the value of the
                     * field ds of the JAWT_DrawingSurfaceInfo.
                     */
                    wantsPaint = JAWTRenderer_paint(dsi, clazz, handle, g);
                    ds->FreeDrawingSurfaceInfo(dsi);
                }
                ds->Unlock(ds);
            }
            awt.FreeDrawingSurface(ds);
        }
    }
    return wantsPaint;
}

JNIEXPORT jboolean JNICALL
Java_net_java_sip_communicator_impl_neomedia_jmfext_media_renderer_video_JAWTRenderer_paintLightweightComponent
    (JNIEnv *jniEnv, jclass clazz, jlong handle, jobject component, jobject g)
{
    jboolean wantsPaint;

#ifdef __APPLE__
    wantsPaint = JAWTRenderer_paintLightweightComponent(handle, component, g);
#else /* #ifdef __APPLE__ */
    /*
     * There is really no point in delivering any paint events/notifications
     * because there is no implementation.
     */
    wantsPaint = JNI_FALSE;
#endif /* #ifdef __APPLE__ */
    return wantsPaint;
}

JNIEXPORT jboolean JNICALL
Java_net_java_sip_communicator_impl_neomedia_jmfext_media_renderer_video_JAWTRenderer_process
    (JNIEnv *jniEnv, jclass clazz,
     jlong handle, jobject component,
     jintArray data, jint offset, jint length,
     jint width, jint height)
{
    jint *dataPtr;
    jboolean processed;

    dataPtr = (*jniEnv)->GetPrimitiveArrayCritical(jniEnv, data, NULL);
    if (dataPtr)
    {
        processed
            = JAWTRenderer_process(
                    jniEnv, clazz,
                    handle, component,
                    dataPtr + offset, length,
                    width, height);
        (*jniEnv)->ReleasePrimitiveArrayCritical(
                jniEnv,
                data, dataPtr,
                JNI_ABORT);
    }
    else
        processed = JNI_FALSE;
    return processed;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_jmfext_media_renderer_video_JAWTRenderer_processLightweightComponentEvent
    (JNIEnv *jniEnv, jclass clazz,
    jlong handle,
    jint x, jint y, jint width, jint height)
{
#ifdef __APPLE__
    JAWTRenderer_processLightweightComponentEvent(handle, x, y, width, height);
#endif /* #ifdef __APPLE__ */
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_jmfext_media_renderer_video_JAWTRenderer_removeNotifyLightweightComponent
    (JNIEnv *jniEnv, jclass clazz, jlong handle, jobject component)
{
#ifdef __APPLE__
    JAWTRenderer_removeNotifyLightweightComponent(handle, component);
#endif /* #ifdef __APPLE__ */
}

JNIEXPORT jstring JNICALL
Java_net_java_sip_communicator_impl_neomedia_jmfext_media_renderer_video_JAWTRenderer_sysctlbyname
    (JNIEnv *jniEnv, jclass clazz, jstring name)
{
#ifdef __APPLE__
    return JAWTRenderer_sysctlbyname(jniEnv, name);
#else /* #ifdef __APPLE__ */
    return NULL;
#endif /* #ifdef __APPLE__ */
}
