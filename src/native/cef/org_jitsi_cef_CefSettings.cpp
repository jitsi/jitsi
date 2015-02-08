/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#include "javah/org_jitsi_cef_CefSettings.h"

#include <include/cef_base.h> /* CefSettings */

JNIEXPORT void JNICALL
Java_org_jitsi_cef_CefSettings__1delete_1(JNIEnv *env, jclass clazz, jlong thiz)
{
    delete reinterpret_cast<CefSettings *>(thiz);
}

JNIEXPORT jlong JNICALL
Java_org_jitsi_cef_CefSettings__1new_1(JNIEnv *env, jclass clazz)
{
    return reinterpret_cast<jlong>(new CefSettings());
}

JNIEXPORT jboolean JNICALL
Java_org_jitsi_cef_CefSettings_isMultiThreadedMessageLoop
    (JNIEnv *env, jclass clazz, jlong thiz)
{
    CefSettings *thiz_ = reinterpret_cast<CefSettings *>(thiz);

    return (true == thiz_->multi_threaded_message_loop) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_org_jitsi_cef_CefSettings_isSingleProcess
    (JNIEnv *env, jclass clazz, jlong thiz)
{
    CefSettings *thiz_ = reinterpret_cast<CefSettings *>(thiz);

    return (true == thiz_->single_process) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_org_jitsi_cef_CefSettings_setMultiThreadedMessageLoop
    (JNIEnv *env, jclass clazz, jlong thiz, jboolean b)
{
    CefSettings *thiz_ = reinterpret_cast<CefSettings *>(thiz);

    thiz_->multi_threaded_message_loop = (JNI_TRUE == b) ? true : false;
}

JNIEXPORT void JNICALL
Java_org_jitsi_cef_CefSettings_setSingleProcess
    (JNIEnv *env, jclass clazz, jlong thiz, jboolean b)
{
    CefSettings *thiz_ = reinterpret_cast<CefSettings *>(thiz);

    thiz_->single_process = (JNI_TRUE == b) ? true : false;
}
