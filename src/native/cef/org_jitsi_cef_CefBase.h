/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#ifndef _ORG_JITSI_CEF_CEFBASE_H_
#define _ORG_JITSI_CEF_CEFBASE_H_

#include <include/cef_base.h> /* CefBase */
#include "javah/org_jitsi_cef_CefBase.h"

class org_jitsi_cef_CefBase
    : public virtual CefBase
{
public:
    static JNIEnv *getJNIEnv();
    static CefBase *getPtr(JNIEnv *env, jobject jobj);

protected:
    org_jitsi_cef_CefBase() : _jobj(NULL) {}
    virtual ~org_jitsi_cef_CefBase();

    jobject CallObjectMethodA(JNIEnv *env, jmethodID methodID, jvalue *args);
    void CallVoidMethodA(JNIEnv *env, jmethodID methodID, jvalue *args);

private:
    static jint JNI_OnLoad(JavaVM *vm, JNIEnv *env);
    static void JNI_OnUnload(JavaVM *vm, JNIEnv *env);

    static jclass _jCefBaseClass;
    static jmethodID _jCefBaseGetPtrMethodID;
    static JavaVM *_vm;

    jobject _jobj;

IMPLEMENT_REFCOUNTING(org_jitsi_cef_CefBase)

    friend jlong JNICALL Java_org_jitsi_cef_CefBase__1init_1
        (JNIEnv *, jobject, jlong);
    friend jint JNICALL JNI_OnLoad(JavaVM *, void *);
    friend void JNICALL JNI_OnUnload(JavaVM *, void *);
};

#endif /* #ifndef _ORG_JITSI_CEF_CEFBASE_H_ */
