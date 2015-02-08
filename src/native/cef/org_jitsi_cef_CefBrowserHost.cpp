/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#include "javah/org_jitsi_cef_CefBrowserHost.h"

#include <include/cef_browser.h> /* CefBrowserHost */
#include <include/cef_client.h> /* CefClient */

JNIEXPORT jboolean JNICALL
Java_org_jitsi_cef_CefBrowserHost_CreateBrowser
    (JNIEnv *env, jclass clazz, jlong windowInfo, jlong client, jstring url,
        jlong settings)
{
    CefWindowInfo *windowInfo_ = reinterpret_cast<CefWindowInfo *>(windowInfo);
    CefClient *client_
        = dynamic_cast<CefClient *>(reinterpret_cast<CefBase *>(client));
    CefString url_;
    CefBrowserSettings *settings_
        = reinterpret_cast<CefBrowserSettings *>(settings);
    CefRequestContext *request_context_ = NULL;
    bool b
        = CefBrowserHost::CreateBrowser(
                *windowInfo_,
                client_,
                url_,
                *settings_,
                request_context_);

    return (true == b) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jlong JNICALL
Java_org_jitsi_cef_CefBrowserHost_CreateBrowserSync
    (JNIEnv *env, jclass clazz, jlong windowInfo, jlong client, jstring url,
        jlong settings)
{
    CefWindowInfo *windowInfo_ = reinterpret_cast<CefWindowInfo *>(windowInfo);
    CefClient *client_
        = dynamic_cast<CefClient *>(reinterpret_cast<CefBase *>(client));
    CefString url_;
    CefBrowserSettings *settings_
        = reinterpret_cast<CefBrowserSettings *>(settings);
    CefRequestContext *request_context_ = NULL;
    CefRefPtr<CefBrowser> ref
        = CefBrowserHost::CreateBrowserSync(
                *windowInfo_,
                client_,
                url_,
                *settings_,
                request_context_);
    CefBrowser *ptr = ref.get();

    if (ptr)
        ptr->AddRef();
    return reinterpret_cast<jlong>(ptr);
}
