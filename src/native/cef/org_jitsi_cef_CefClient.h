/*
* Jitsi, the OpenSource Java VoIP and Instant Messaging client.
*
* Distributable under LGPL license.
* See terms of license at gnu.org.
*/
#ifndef _ORG_JITSI_CEF_CEFCLIENT_H_
#define _ORG_JITSI_CEF_CEFCLIENT_H_

#include <include/cef_client.h> /* CefClient */
#include "org_jitsi_cef_CefBase.h"

class org_jitsi_cef_CefClient
    : public org_jitsi_cef_CefBase, CefClient
{
public:
    CefRefPtr<CefLifeSpanHandler> GetLifeSpanHandler();

private:
    static jint JNI_OnLoad(JavaVM *vm, JNIEnv *env);
    static void JNI_OnUnload(JavaVM *vm, JNIEnv *env);

    static jclass _jCefClientClass;
    static jmethodID _jCefClientGetLifeSpanHandlerMethodID;

    friend jint JNICALL JNI_OnLoad(JavaVM *, void *);
    friend void JNICALL JNI_OnUnload(JavaVM *, void *);
};

#endif /* #ifndef _ORG_JITSI_CEF_CEFCLIENT_H_ */
