/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#include "javah/org_jitsi_cef_CefBrowserSettings.h"

#include <include/cef_base.h> /* CefBrowserSettings */

JNIEXPORT void JNICALL
Java_org_jitsi_cef_CefBrowserSettings__1delete_1
    (JNIEnv *env, jclass clazz, jlong thiz)
{
    delete reinterpret_cast<CefBrowserSettings *>(thiz);
}

JNIEXPORT jlong JNICALL
Java_org_jitsi_cef_CefBrowserSettings__1new_1(JNIEnv *env, jclass clazz)
{
    return reinterpret_cast<jlong>(new CefBrowserSettings());
}
