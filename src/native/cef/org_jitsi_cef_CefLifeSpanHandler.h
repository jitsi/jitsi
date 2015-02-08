/*
* Jitsi, the OpenSource Java VoIP and Instant Messaging client.
*
* Distributable under LGPL license.
* See terms of license at gnu.org.
*/
#ifndef _ORG_JITSI_CEF_CEFLIFESPANHANDLER_H_
#define _ORG_JITSI_CEF_CEFLIFESPANHANDLER_H_

#include <include/cef_life_span_handler.h> /* CefLifeSpanHandler */
#include "org_jitsi_cef_CefBase.h"

class org_jitsi_cef_CefLifeSpanHandler
    : public org_jitsi_cef_CefBase, CefLifeSpanHandler
{
public:
    void OnAfterCreated(CefRefPtr<CefBrowser> browser);
    void OnBeforeClose(CefRefPtr<CefBrowser> browser);

private:
    static jint JNI_OnLoad(JavaVM *vm, JNIEnv *env);
    static void JNI_OnUnload(JavaVM *vm, JNIEnv *env);

    static jclass _jCefLifeSpanHandlerClass;
    static jmethodID _jCefLifeSpanHandlerOnAfterCreatedMethodID;
    static jmethodID _jCefLifeSpanHandlerOnBeforeCloseMethodID;

    void CallVoidMethodA(jmethodID methodID, jvalue *args);

    friend jint JNICALL JNI_OnLoad(JavaVM *, void *);
    friend void JNICALL JNI_OnUnload(JavaVM *, void *);
};

#endif /* #ifndef _ORG_JITSI_CEF_CEFLIFESPANHANDLER_H_ */
