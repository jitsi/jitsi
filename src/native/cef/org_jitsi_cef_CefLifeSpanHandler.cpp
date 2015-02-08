/*
* Jitsi, the OpenSource Java VoIP and Instant Messaging client.
*
* Distributable under LGPL license.
* See terms of license at gnu.org.
*/
#include "org_jitsi_cef_CefLifeSpanHandler.h"

#include "javah/org_jitsi_cef_CefLifeSpanHandler.h"

JNIEXPORT jlong JNICALL
Java_org_jitsi_cef_CefLifeSpanHandler__1alloc_1(JNIEnv *env, jobject thiz)
{
    return
        reinterpret_cast<jlong>(
                static_cast<CefBase *>(
                        new org_jitsi_cef_CefLifeSpanHandler()));
}

jclass org_jitsi_cef_CefLifeSpanHandler::_jCefLifeSpanHandlerClass = NULL;
jmethodID org_jitsi_cef_CefLifeSpanHandler::_jCefLifeSpanHandlerOnAfterCreatedMethodID = NULL;
jmethodID org_jitsi_cef_CefLifeSpanHandler::_jCefLifeSpanHandlerOnBeforeCloseMethodID = NULL;

void
org_jitsi_cef_CefLifeSpanHandler::CallVoidMethodA(jmethodID methodID, jvalue *args)
{
    if (methodID)
    {
        JNIEnv *env = getJNIEnv();

        if (env)
            org_jitsi_cef_CefBase::CallVoidMethodA(env, methodID, args);
    }
}

jint
org_jitsi_cef_CefLifeSpanHandler::JNI_OnLoad(JavaVM *vm, JNIEnv *env)
{
    jclass jCefLifeSpanHandlerClass
        = env->FindClass("org/jitsi/cef/CefLifeSpanHandler");
    jint ver = JNI_ERR;

    if (jCefLifeSpanHandlerClass)
    {
        jCefLifeSpanHandlerClass
            = reinterpret_cast<jclass>(
                    env->NewGlobalRef(jCefLifeSpanHandlerClass));

        if (jCefLifeSpanHandlerClass)
        {
            jmethodID jCefLifeSpanHandlerOnAfterCreatedMethodID
                = env->GetMethodID(
                        jCefLifeSpanHandlerClass,
                        "OnAfterCreated",
                        "(J)V");

            if (jCefLifeSpanHandlerOnAfterCreatedMethodID)
            {
                jmethodID jCefLifeSpanHandlerOnBeforeCloseMethodID
                    = env->GetMethodID(
                            jCefLifeSpanHandlerClass,
                            "OnBeforeClose",
                            "(J)V");

                if (jCefLifeSpanHandlerOnBeforeCloseMethodID)
                {
                    _jCefLifeSpanHandlerClass = jCefLifeSpanHandlerClass;
                    _jCefLifeSpanHandlerOnAfterCreatedMethodID
                        = jCefLifeSpanHandlerOnAfterCreatedMethodID;
                    _jCefLifeSpanHandlerOnBeforeCloseMethodID
                        = jCefLifeSpanHandlerOnBeforeCloseMethodID;
                    ver = JNI_VERSION_1_6;
                }
            }
            if (ver <= 0)
                env->DeleteGlobalRef(jCefLifeSpanHandlerClass);
        }
    }
    return ver;
}

void
org_jitsi_cef_CefLifeSpanHandler::JNI_OnUnload(JavaVM *vm, JNIEnv *env)
{
    jclass jCefLifeSpanHandlerClass = _jCefLifeSpanHandlerClass;

    if (jCefLifeSpanHandlerClass)
    {
        _jCefLifeSpanHandlerClass = NULL;
        _jCefLifeSpanHandlerOnAfterCreatedMethodID = NULL;

        env->DeleteGlobalRef(jCefLifeSpanHandlerClass);
    }
}

void
org_jitsi_cef_CefLifeSpanHandler::OnAfterCreated(CefRefPtr<CefBrowser> browser)
{
    jvalue arg;

    arg.j = reinterpret_cast<jlong>(browser.get());
    CallVoidMethodA(_jCefLifeSpanHandlerOnAfterCreatedMethodID, &arg);
}

void
org_jitsi_cef_CefLifeSpanHandler::OnBeforeClose(CefRefPtr<CefBrowser> browser)
{
    jvalue arg;

    arg.j = reinterpret_cast<jlong>(browser.get());
    CallVoidMethodA(_jCefLifeSpanHandlerOnBeforeCloseMethodID, &arg);
}
