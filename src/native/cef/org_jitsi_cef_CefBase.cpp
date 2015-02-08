/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#include "org_jitsi_cef_CefBase.h"

JNIEXPORT jlong JNICALL
Java_org_jitsi_cef_CefBase__1init_1(JNIEnv *env, jobject thiz, jlong ptr)
{
    org_jitsi_cef_CefBase *ptr_
        = dynamic_cast<org_jitsi_cef_CefBase *>(
                reinterpret_cast<CefBase *>(
                        ptr));

    thiz = env->NewGlobalRef(thiz);
    if (thiz)
    {
        ptr_->_jobj = thiz;
        ptr_->AddRef();
    }
    else
    {
        delete ptr_;
        ptr = 0;
    }
    return ptr;
}

JNIEXPORT jint JNICALL
Java_org_jitsi_cef_CefBase_AddRef(JNIEnv *env, jobject thiz, jlong ptr)
{
    return reinterpret_cast<CefBase *>(ptr)->AddRef();
}

JNIEXPORT jint JNICALL
Java_org_jitsi_cef_CefBase_GetRefCt(JNIEnv *env, jobject thiz, jlong ptr)
{
    return reinterpret_cast<CefBase *>(ptr)->GetRefCt();
}

JNIEXPORT jint JNICALL
Java_org_jitsi_cef_CefBase_Release(JNIEnv *env, jobject thiz, jlong ptr)
{
    return reinterpret_cast<CefBase *>(ptr)->Release();
}

jclass org_jitsi_cef_CefBase::_jCefBaseClass = NULL;
jmethodID org_jitsi_cef_CefBase::_jCefBaseGetPtrMethodID = NULL;
JavaVM *org_jitsi_cef_CefBase::_vm = NULL;

org_jitsi_cef_CefBase::~org_jitsi_cef_CefBase()
{
    jobject jobj = _jobj;

    if (jobj)
    {
        _jobj = NULL;

        JNIEnv *env = getJNIEnv();

        if (env)
            env->DeleteGlobalRef(jobj);
    }
}

jobject
org_jitsi_cef_CefBase::CallObjectMethodA
    (JNIEnv *env, jmethodID methodID, jvalue *args)
{
    jobject jobj = _jobj;

    if (jobj)
    {
        jobj = env->CallObjectMethodA(jobj, methodID, args);
        if (env->ExceptionCheck())
        {
            jobj = NULL;
            env->ExceptionClear();
        }
    }
    return jobj;
}

void
org_jitsi_cef_CefBase::CallVoidMethodA
    (JNIEnv *env, jmethodID methodID, jvalue *args)
{
    jobject jobj = _jobj;

    if (jobj)
    {
        env->CallVoidMethodA(jobj, methodID, args);
        if (env->ExceptionCheck())
            env->ExceptionClear();
    }
}

JNIEnv *
org_jitsi_cef_CefBase::getJNIEnv()
{
    JavaVM *vm = _vm;
    JNIEnv *env;

    if (vm)
    {
        jint i = vm->AttachCurrentThreadAsDaemon((void **) &env, NULL);

        if (JNI_OK != i)
            env = NULL;
    }
    else
        env = NULL;
    return env;
}

CefBase *
org_jitsi_cef_CefBase::getPtr(JNIEnv *env, jobject jobj)
{
    jmethodID jCefBaseGetPtrMethodID = _jCefBaseGetPtrMethodID;
    jlong ptr;

    if (jCefBaseGetPtrMethodID)
    {
        ptr = env->CallLongMethod(jobj, jCefBaseGetPtrMethodID);
        if (env->ExceptionCheck())
        {
            ptr = 0;
            env->ExceptionClear();
        }
    }
    else
        ptr = 0;
    return reinterpret_cast<CefBase *>(ptr);
}

jint
org_jitsi_cef_CefBase::JNI_OnLoad(JavaVM *vm, JNIEnv *env)
{
    _vm = vm;

    jclass jCefBaseClass = env->FindClass("org/jitsi/cef/CefBase");
    jint ver = JNI_ERR;

    if (jCefBaseClass)
    {
        jCefBaseClass
            = reinterpret_cast<jclass>(env->NewGlobalRef(jCefBaseClass));

        if (jCefBaseClass)
        {
            jmethodID jCefBaseGetPtrMethodID
                = env->GetMethodID(jCefBaseClass, "getPtr", "()J");

            if (jCefBaseGetPtrMethodID)
            {
                _jCefBaseClass = jCefBaseClass;
                _jCefBaseGetPtrMethodID = jCefBaseGetPtrMethodID;
                ver = JNI_VERSION_1_6;
            }
            else
                env->DeleteGlobalRef(jCefBaseClass);
        }
    }
    return ver;
}

void
org_jitsi_cef_CefBase::JNI_OnUnload(JavaVM *vm, JNIEnv *env)
{
    jclass jCefBaseClass = _jCefBaseClass;

    if (jCefBaseClass)
    {
        _jCefBaseClass = NULL;
        _jCefBaseGetPtrMethodID = NULL;

        env->DeleteGlobalRef(jCefBaseClass);
    }

    _vm = NULL;
}

#include "org_jitsi_cef_CefClient.h"
#include "org_jitsi_cef_CefLifeSpanHandler.h"

JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *vm, void *reserved)
{
    JNIEnv *env;
    jint maxVer;

    if (JNI_OK == vm->AttachCurrentThreadAsDaemon((void **)&env, NULL))
    {
        jint ver = org_jitsi_cef_CefBase::JNI_OnLoad(vm, env);

        if (ver > 0)
        {
            maxVer = ver;

            ver = org_jitsi_cef_CefClient::JNI_OnLoad(vm, env);
            if (ver > 0)
            {
                if (maxVer < ver)
                    maxVer = ver;

                ver = org_jitsi_cef_CefLifeSpanHandler::JNI_OnLoad(vm, env);
                if (ver > 0)
                {
                    if (maxVer < ver)
                        maxVer = ver;
                }
                else
                {
                    maxVer = JNI_ERR;
                    org_jitsi_cef_CefClient::JNI_OnUnload(vm, env);
                    org_jitsi_cef_CefBase::JNI_OnUnload(vm, env);
                }
            }
            else
            {
                maxVer = JNI_ERR;
                org_jitsi_cef_CefBase::JNI_OnUnload(vm, env);
            }
        }
        else
            maxVer = JNI_ERR;
    }
    else
        maxVer = JNI_ERR;
    return maxVer;
}

JNIEXPORT void JNICALL
JNI_OnUnload(JavaVM *vm, void *reserved)
{
    JNIEnv *env;

    if (JNI_OK == vm->AttachCurrentThreadAsDaemon((void **)&env, NULL))
    {
        org_jitsi_cef_CefLifeSpanHandler::JNI_OnUnload(vm, env);
        org_jitsi_cef_CefClient::JNI_OnUnload(vm, env);
        org_jitsi_cef_CefBase::JNI_OnUnload(vm, env);
    }
}
