/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#include "javah/org_jitsi_cef_CefWindowInfo.h"

#include <include/cef_base.h> /* CefWindowInfo */

JNIEXPORT void JNICALL
Java_org_jitsi_cef_CefWindowInfo__1delete_1
    (JNIEnv *env, jclass clazz, jlong thiz)
{
    delete reinterpret_cast<CefWindowInfo *>(thiz);
}

JNIEXPORT jlong JNICALL
Java_org_jitsi_cef_CefWindowInfo__1new_1(JNIEnv *env, jclass clazz)
{
    return reinterpret_cast<jlong>(new CefWindowInfo());
}

JNIEXPORT void JNICALL
Java_org_jitsi_cef_CefWindowInfo_SetAsChild
    (JNIEnv *env, jclass clazz, jlong thiz, jlong hWndParent, jint left,
        jint top, jint right, jint bottom)
{
    CefWindowInfo *thiz_ = reinterpret_cast<CefWindowInfo *>(thiz);
    HWND hWndParent_ = reinterpret_cast<HWND>(hWndParent);
    RECT windowRect = { left, top, right, bottom };

    thiz_->SetAsChild(hWndParent_, windowRect);
}
