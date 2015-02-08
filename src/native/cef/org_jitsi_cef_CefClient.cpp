/*
* Jitsi, the OpenSource Java VoIP and Instant Messaging client.
*
* Distributable under LGPL license.
* See terms of license at gnu.org.
*/
#include "org_jitsi_cef_CefClient.h"

#include "javah/org_jitsi_cef_CefClient.h"

JNIEXPORT jlong JNICALL
Java_org_jitsi_cef_CefClient__1alloc_1(JNIEnv *env, jobject thiz)
{
    return
        reinterpret_cast<jlong>(
                static_cast<CefBase *>(
                        new org_jitsi_cef_CefClient()));
}

jclass org_jitsi_cef_CefClient::_jCefClientClass = NULL;
jmethodID org_jitsi_cef_CefClient::_jCefClientGetLifeSpanHandlerMethodID = NULL;

CefRefPtr<CefLifeSpanHandler> org_jitsi_cef_CefClient::GetLifeSpanHandler()
{
    JNIEnv *env = getJNIEnv();
    CefLifeSpanHandler *lifeSpanHandler = NULL;

    if (env)
    {
        jmethodID jCefClientGetLifeSpanHandlerMethodID
            = _jCefClientGetLifeSpanHandlerMethodID;

        if (jCefClientGetLifeSpanHandlerMethodID)
        {
            jobject jlifeSpanHandler
                = CallObjectMethodA(
                        env,
                        jCefClientGetLifeSpanHandlerMethodID,
                        NULL);

            if (jlifeSpanHandler)
            {
                lifeSpanHandler
                    = dynamic_cast<CefLifeSpanHandler *>(
                            getPtr(env, jlifeSpanHandler));
            }
        }
    }
    return lifeSpanHandler;
}

jint
org_jitsi_cef_CefClient::JNI_OnLoad(JavaVM *vm, JNIEnv *env)
{
    jclass jCefClientClass = env->FindClass("org/jitsi/cef/CefClient");
    jint ver = JNI_ERR;

    if (jCefClientClass)
    {
        jCefClientClass
            = reinterpret_cast<jclass>(env->NewGlobalRef(jCefClientClass));

        if (jCefClientClass)
        {
            jmethodID jCefClientGetLifeSpanHandlerMethodID
                = env->GetMethodID(
                        jCefClientClass,
                        "GetLifeSpanHandler",
                        "()Lorg/jitsi/cef/CefLifeSpanHandler;");

            if (jCefClientGetLifeSpanHandlerMethodID)
            {
                _jCefClientClass = jCefClientClass;
                _jCefClientGetLifeSpanHandlerMethodID
                    = jCefClientGetLifeSpanHandlerMethodID;
                ver = JNI_VERSION_1_6;
            }
            else
                env->DeleteGlobalRef(jCefClientClass);
        }
    }
    return ver;
}

void
org_jitsi_cef_CefClient::JNI_OnUnload(JavaVM *vm, JNIEnv *env)
{
    jclass jCefClientClass = _jCefClientClass;

    if (jCefClientClass)
    {
        _jCefClientClass = NULL;
        _jCefClientGetLifeSpanHandlerMethodID = NULL;

        env->DeleteGlobalRef(jCefClientClass);
    }
}
