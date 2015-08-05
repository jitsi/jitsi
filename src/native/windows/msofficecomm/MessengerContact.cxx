/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#include "MessengerContact.h"

EXTERN_C const GUID DECLSPEC_SELECTANY IID_IMessengerContact
    = { 0xE7479A0F, 0xBB19, 0x44a5, { 0x96, 0x8F, 0x6F, 0x41, 0xD9, 0x3E, 0xE0, 0xBC } };

EXTERN_C const GUID DECLSPEC_SELECTANY IID_IMessengerContactAdvanced
    = { 0x086F69C0, 0x2FBD, 0x46b3, { 0xBE, 0x50, 0xEC, 0x40, 0x1A, 0xB8, 0x60, 0x99 } };

jclass    MessengerContact::_jclass = NULL;
jmethodID MessengerContact::_jctorMethodID = NULL;
jmethodID MessengerContact::_jgetPhoneNumberMethodID = NULL;
jmethodID MessengerContact::_jgetStatusMethodID = NULL;
jmethodID MessengerContact::_jisSelfMethodID = NULL;

MessengerContact::MessengerContact(IMessenger *messenger, LPCOLESTR signinName)
    : _messenger(messenger),
      _jobject(NULL)
{
    _messenger->AddRef();
    if (signinName)
    {
        _signinName = ::_wcsdup(signinName);
        constructJobject(_signinName);
    }
    else
        _signinName = NULL;
}

MessengerContact::~MessengerContact()
{
    if (_jobject)
        destructJobject();
    _messenger->Release();
    if (_signinName)
        ::free(_signinName);
}

HRESULT MessengerContact::constructJobject(LPCOLESTR signinName)
{
    HRESULT hr;

    if (signinName)
    {
        JavaVM *vm = OutOfProcessServer::getJavaVM();
        JNIEnv *env;

        if (vm && !(vm->AttachCurrentThreadAsDaemon((void **) &env, NULL)))
        {
            jstring jsigninName
                = env->NewString(
                        (const jchar *) signinName,
                        ::wcslen(signinName));

            if (jsigninName)
            {
                jobject o = env->NewObject(_jclass, _jctorMethodID, jsigninName);

                if (o)
                {
                    o = env->NewGlobalRef(o);
                    if (o)
                    {
                        if (_jobject)
                            env->DeleteGlobalRef(_jobject);
                        _jobject = o;
                        hr = S_OK;
                    }
                    else
                        hr = E_OUTOFMEMORY;
                }
                else
                    hr = E_FAIL;
            }
            else
                hr = E_OUTOFMEMORY;

            /*
             * The constructJobject(LPCOLESTR) method is called as part of the
             * MessengerContact constructor which does not return an error code
             * so any Java exception should be cleared in order to prevent
             * unexpected behavior on the side of the Java VM.
             */
            if (FAILED(hr))
                env->ExceptionClear();
        }
        else
            hr = E_UNEXPECTED;
    }
    else
        hr = E_INVALIDARG;
    return hr;
}

HRESULT MessengerContact::destructJobject()
{
    JavaVM *vm = OutOfProcessServer::getJavaVM();
    JNIEnv *env;
    HRESULT hr;

    if (vm && !(vm->AttachCurrentThreadAsDaemon((void **) &env, NULL)))
    {
        env->DeleteGlobalRef(_jobject);
        _jobject = NULL;
        hr = S_OK;
    }
    else
        hr = E_UNEXPECTED;
    return hr;
}

STDMETHODIMP MessengerContact::get_Blocked(VARIANT_BOOL *pBoolBlock)
    STDMETHODIMP_E_NOTIMPL_STUB

STDMETHODIMP MessengerContact::get_CanPage(VARIANT_BOOL *pBoolPage)
    STDMETHODIMP_E_NOTIMPL_STUB

STDMETHODIMP MessengerContact::get_FriendlyName(BSTR *pbstrFriendlyName)
    STDMETHODIMP_E_NOTIMPL_STUB

STDMETHODIMP MessengerContact::get_IsSelf(VARIANT_BOOL *pBoolSelf)
{
    HRESULT hr;

    if (pBoolSelf)
    {
        if (_jobject)
        {
            JavaVM *vm = OutOfProcessServer::getJavaVM();
            JNIEnv *env;

            if (vm && !(vm->AttachCurrentThreadAsDaemon((void **) &env, NULL)))
            {
                jboolean jself
                    = env->CallBooleanMethod(_jobject, _jisSelfMethodID);

                if (env->ExceptionCheck())
                {
                    env->ExceptionClear();
                    hr = E_FAIL;
                }
                else
                {
                    *pBoolSelf
                        = (JNI_TRUE == jself) ? VARIANT_TRUE : VARIANT_FALSE;
                    hr = S_OK;
                }
            }
            else
                hr = E_UNEXPECTED;
        }
        else
            hr = E_FAIL;
    }
    else
        hr = RPC_X_NULL_REF_POINTER;
    return hr;
}

STDMETHODIMP MessengerContact::get_IsTagged(VARIANT_BOOL *pBoolIsTagged)
    STDMETHODIMP_E_NOTIMPL_STUB

STDMETHODIMP MessengerContact::get_PhoneNumber(MPHONE_TYPE PhoneType, BSTR *bstrNumber)
{
    HRESULT hr;

    if (bstrNumber)
    {
        if (_jobject)
        {
            JavaVM *vm = OutOfProcessServer::getJavaVM();
            JNIEnv *env;

            if (vm && !(vm->AttachCurrentThreadAsDaemon((void **) &env, NULL)))
            {
                jobject jobj
                    = env->CallObjectMethod(
                            _jobject,
                            _jgetPhoneNumberMethodID,
                            (jint) PhoneType);

                if (env->ExceptionCheck())
                {
                    env->ExceptionClear();
                    *bstrNumber = NULL;
                    hr = E_FAIL;
                }
                else
                {
                    jstring jstr = (jstring) jobj;
                    const jchar *jchars
                        = jstr ? env->GetStringChars(jstr, NULL) : NULL;

                    if (jchars)
                    {
                        *bstrNumber = ::SysAllocString((LPOLESTR) jchars);
                        env->ReleaseStringChars(jstr, jchars);
                        hr = *bstrNumber ? S_OK : E_OUTOFMEMORY;
                    }
                    else
                    {
                        *bstrNumber = NULL;
                        hr = E_FAIL;
                    }
                    if (env->ExceptionCheck())
                        env->ExceptionClear();
                }
            }
            else
            {
                *bstrNumber = NULL;
                hr = E_UNEXPECTED;
            }
        }
        else
        {
            *bstrNumber = NULL;
            hr = E_FAIL;
        }
    }
    else
        hr = RPC_X_NULL_REF_POINTER;
    return hr;
}

STDMETHODIMP
MessengerContact::get_PresenceProperties(VARIANT *pvPresenceProperties)
{
    HRESULT hr;

    if (pvPresenceProperties)
    {
        MISTATUS status;

        hr = get_Status(&status);
        if (SUCCEEDED(hr))
        {
            hr
                = (VT_EMPTY == pvPresenceProperties->vt)
                    ? S_OK
                    : (::VariantClear(pvPresenceProperties));
            if (SUCCEEDED(hr))
            {
                SAFEARRAY *sa
                    = ::SafeArrayCreateVector(VT_VARIANT, 0, PRESENCE_PROP_MAX);

                if (sa)
                {
                    LONG i;
                    VARIANT v;

                    i = PRESENCE_PROP_MSTATE;
                    ::VariantInit(&v);
                    v.vt = VT_I4;
                    v.lVal = status;
                    hr = ::SafeArrayPutElement(sa, &i, &v);
                    if (SUCCEEDED(hr))
                    {
                        LONG availability;

                        switch (status)
                        {
                        case MISTATUS_AWAY:
                        case MISTATUS_OUT_OF_OFFICE:
                            availability = 15000;
                            break;
                        case MISTATUS_BE_RIGHT_BACK:
                            availability = 12000;
                            break;
                        case MISTATUS_BUSY:
                        case MISTATUS_IN_A_CONFERENCE:
                        case MISTATUS_ON_THE_PHONE:
                            availability = 6000;
                            break;
                        case MISTATUS_DO_NOT_DISTURB:
                        case MISTATUS_ALLOW_URGENT_INTERRUPTIONS:
                            availability = 9000;
                            break;
                        case MISTATUS_INVISIBLE:
                            availability = 18000;
                            break;
                        case MISTATUS_ONLINE:
                            availability = 3000;
                            break;
                        default:
                            availability = 0;
                            break;
                        }
                        if (availability)
                        {
                            i = PRESENCE_PROP_AVAILABILITY;
                            v.lVal = availability;
                            hr = ::SafeArrayPutElement(sa, &i, &v);
                        }
                    }
                    if (SUCCEEDED(hr))
                    {
                        pvPresenceProperties->vt = VT_ARRAY;
                        pvPresenceProperties->parray = sa;
                    }
                    else
                        ::SafeArrayDestroy(sa);
                }
                else
                    hr = E_FAIL;
            }
        }
    }
    else
        hr = E_INVALIDARG;
    return hr;
}

STDMETHODIMP MessengerContact::get_Property(MCONTACTPROPERTY ePropType, VARIANT *pvPropVal)
    STDMETHODIMP_E_NOTIMPL_STUB

STDMETHODIMP MessengerContact::get_ServiceId(BSTR *pbstrServiceID)
{
    return _messenger->get_MyServiceId(pbstrServiceID);
}

STDMETHODIMP MessengerContact::get_ServiceName(BSTR *pbstrServiceName)
{
    return _messenger->get_MyServiceName(pbstrServiceName);
}

STDMETHODIMP MessengerContact::get_SigninName(BSTR *pbstrSigninName)
{
    HRESULT hr;

    if (pbstrSigninName)
    {
        if (_signinName)
        {
            hr
                = ((*pbstrSigninName = ::SysAllocString(_signinName)))
                    ? S_OK
                    : E_OUTOFMEMORY;
        }
        else
        {
            *pbstrSigninName = NULL;
            hr = E_FAIL;
        }
    }
    else
        hr = RPC_X_NULL_REF_POINTER;
    return hr;
}

STDMETHODIMP MessengerContact::get_Status(MISTATUS *pMstate)
{
    HRESULT hr;

    if (pMstate)
    {
        if (_jobject)
        {
            JavaVM *vm = OutOfProcessServer::getJavaVM();
            JNIEnv *env;

            if (vm && !(vm->AttachCurrentThreadAsDaemon((void **) &env, NULL)))
            {
                jint jstatus
                    = env->CallIntMethod(_jobject, _jgetStatusMethodID);

                if (env->ExceptionCheck())
                {
                    env->ExceptionClear();
                    hr = E_FAIL;
                }
                else
                {
                    *pMstate = (MISTATUS) jstatus;
                    hr = S_OK;
                }
            }
            else
                hr = E_UNEXPECTED;
        }
        else
            hr = E_FAIL;
    }
    else
        hr = RPC_X_NULL_REF_POINTER;
    return hr;
}

STDMETHODIMP MessengerContact::put_Blocked(VARIANT_BOOL pBoolBlock)
    STDMETHODIMP_E_NOTIMPL_STUB

STDMETHODIMP MessengerContact::put_IsTagged(VARIANT_BOOL pBoolIsTagged)
    STDMETHODIMP_E_NOTIMPL_STUB

STDMETHODIMP MessengerContact::put_PresenceProperties(VARIANT vPresenceProperties)
    STDMETHODIMP_E_NOTIMPL_STUB

STDMETHODIMP MessengerContact::put_Property(MCONTACTPROPERTY ePropType, VARIANT vPropVal)
    STDMETHODIMP_E_NOTIMPL_STUB

STDMETHODIMP MessengerContact::QueryInterface(REFIID iid, PVOID *obj)
{
    HRESULT hr;

    if (obj)
    {
        if (IID_IMessengerContact == iid)
        {
            AddRef();
            *obj = static_cast<IMessengerContact *>(this);
            hr = S_OK;
        }
        else
            hr = DispatchImpl::QueryInterface(iid, obj);
    }
    else
        hr = E_POINTER;
    return hr;
}

BOOL MessengerContact::signinNameEquals(LPDISPATCH contact, BSTR signinName)
{
    IMessengerContact *iMessengerContact;
    HRESULT hr
        = contact->QueryInterface(
                IID_IMessengerContact,
                (PVOID *) &iMessengerContact);
    BOOL b;

    if (SUCCEEDED(hr))
    {
        BSTR contactSigninName;

        hr = iMessengerContact->get_SigninName(&contactSigninName);
        iMessengerContact->Release();
        if (SUCCEEDED(hr))
        {
            b
                = (VARCMP_EQ
                    == ::VarBstrCmp(contactSigninName, signinName, 0, 0));
            ::SysFreeString(contactSigninName);
        }
        else
            b = FALSE;
    }
    else
        b = FALSE;
    return b;
}

HRESULT MessengerContact::start(JNIEnv *env)
{
    LPSTR className = OutOfProcessServer::getClassName("MessengerContact");
    HRESULT hr;

    if (className)
    {
        jclass clazz = env->FindClass(className);

        ::free(className);

        if (clazz)
        {
            jmethodID ctorMethodID
                = env->GetMethodID(clazz, "<init>", "(Ljava/lang/String;)V");

            if (ctorMethodID)
            {
                jmethodID getPhoneNumberMethodID
                    = env->GetMethodID(
                            clazz,
                            "getPhoneNumber",
                            "(I)Ljava/lang/String;");

                if (getPhoneNumberMethodID)
                {
                    jmethodID getStatusMethodID
                        = env->GetMethodID(clazz, "getStatus", "()I");

                    if (getStatusMethodID)
                    {
                        jmethodID isSelfMethodID
                            = env->GetMethodID(clazz, "isSelf", "()Z");

                        if (isSelfMethodID)
                        {
                            clazz = (jclass) env->NewGlobalRef(clazz);
                            if (clazz)
                            {
                                _jclass = clazz;
                                _jctorMethodID = ctorMethodID;
                                _jgetPhoneNumberMethodID
                                    = getPhoneNumberMethodID;
                                _jgetStatusMethodID = getStatusMethodID;
                                _jisSelfMethodID = isSelfMethodID;
                                hr = S_OK;
                            }
                            else
                                hr = E_OUTOFMEMORY;
                        }
                        else
                            hr = E_FAIL;
                    }
                    else
                        hr = E_FAIL;
                }
                else
                    hr = E_FAIL;
            }
            else
                hr = E_FAIL;
        }
        else
            hr = E_FAIL;
    }
    else
        hr = E_OUTOFMEMORY;
    return hr;
}

HRESULT MessengerContact::stop(JNIEnv *env)
{
    jclass clazz = _jclass;

    _jclass = NULL;
    _jctorMethodID = NULL;
    _jgetPhoneNumberMethodID = NULL;
    _jgetStatusMethodID = NULL;
    _jisSelfMethodID = NULL;
    if (clazz)
        env->DeleteGlobalRef(clazz);

    return S_OK;
}
