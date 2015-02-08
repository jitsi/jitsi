/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#include "javah/org_jitsi_cef_CefApp.h"

#include <include/cef_app.h> /* CefApp, CefInitialize */
#include "org_jitsi_cef_CefBase.h"

class org_jitsi_cef_CefApp
    : public org_jitsi_cef_CefBase, CefApp
{
};

JNIEXPORT jboolean JNICALL
Java_org_jitsi_cef_CefApp_CefInitialize
    (JNIEnv *env, jclass clazz, jlong args, jlong settings, jlong application)
{
    CefMainArgs *args_ = reinterpret_cast<CefMainArgs *>(args);
    CefSettings *settings_ = reinterpret_cast<CefSettings *>(settings);
    CefApp *application_ = reinterpret_cast<CefApp *>(application);
    bool b = CefInitialize(*args_, *settings_, application_);

    return (true == b) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jlong JNICALL
Java_org_jitsi_cef_CefApp__1alloc_1(JNIEnv *env, jobject thiz)
{
    return
        reinterpret_cast<jlong>(
                static_cast<CefBase *>(
                        new org_jitsi_cef_CefApp()));
}
