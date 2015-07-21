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
#ifndef _JMSOFFICECOMM_UNKNOWNIMPL_H_
#define _JMSOFFICECOMM_UNKNOWNIMPL_H_

#include "Log.h"
#include "OutOfProcessServer.h"
#include "StringUtils.h"
#include "WeakReferenceSource.h"

#define STDMETHODIMP_E_NOTIMPL_STUB \
    { \
        Log::d(_T("%s\n"), __PRETTY_FUNCTION__); \
        return E_NOTIMPL; \
    }

/**
 * Represents a base implementation of the <tt>IUnknown</tt> interface.
 *
 * @author Lyubomir Marinov
 */
template <class T, REFIID IID_T>
class UnknownImpl
    : public T
{
public:
    // IUnknown
    STDMETHODIMP QueryInterface(REFIID iid, PVOID *obj)
        {
            HRESULT hr;

            if (!obj)
                hr = E_POINTER;
            else if (IID_IUnknown == iid)
            {
                AddRef();
                *obj = static_cast<LPUNKNOWN>(this);
                hr = S_OK;
            }
            else if (IID_T == iid)
            {
                AddRef();
                *obj = static_cast<T *>(this);
                hr = S_OK;
            }
            else if (IID_IWeakReferenceSource == iid)
            {
                if (!_weakReferenceSource)
                    _weakReferenceSource = new WeakReferenceSource(this);
                _weakReferenceSource->AddRef();
                *obj = static_cast<IWeakReferenceSource *>(_weakReferenceSource);
                hr = S_OK;
            }
            else
            {
                *obj = NULL;
                hr = E_NOINTERFACE;
            }

            if (FAILED(hr))
            {
                LPOLESTR olestr;

                if (SUCCEEDED(::StringFromIID(iid, &olestr)))
                {
                    LPTSTR tstr
#ifdef _UNICODE
                        = olestr;
#else
                        = StringUtils::WideCharToMultiByte(olestr);
#endif /* #ifdef _UNICODE */

                    if (tstr)
                    {
                        Log::d(
                                _T("UnknownImpl::QueryInterface: this=%p; iid=%s;\n"),
                                (PVOID) this,
                                tstr);
                        if (tstr != olestr)
                            ::free(tstr);
                    }
                    ::CoTaskMemFree(olestr);
                }
            }

            return hr;
        }

    STDMETHODIMP_(ULONG) AddRef() { return ++_refCount; }

    STDMETHODIMP_(ULONG) Release()
        {
            ULONG refCount = --_refCount;

            if (!refCount)
                delete this;
            Log::d(
                    _T("UnknownImpl::Release: this=%p; refCount=%lu;\n"),
                    (PVOID) this,
                    refCount);
            return refCount;
        }

protected:
    UnknownImpl()
            : _refCount(1),
              _weakReferenceSource(NULL)
        {
            OutOfProcessServer::addRef();
        }

    virtual ~UnknownImpl()
        {
            if (_weakReferenceSource)
                delete _weakReferenceSource;

            OutOfProcessServer::release();
        }

private:
    ULONG _refCount;
    WeakReferenceSource *_weakReferenceSource;
};

#endif /* #ifndef _JMSOFFICECOMM_UNKNOWNIMPL_H_ */
