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
#ifndef _JMSOFFICECOMM_CONNECTIONPOINT_H_
#define _JMSOFFICECOMM_CONNECTIONPOINT_H_

#include <ocidl.h>
#include <olectl.h>
#include <stdint.h>
#include "UnknownImpl.h"

template <class T, REFIID IID_T>
class ConnectionPoint
    : public IConnectionPoint,
      public T
{
public:
    ConnectionPoint(IConnectionPointContainer *container)
            : _container(container),
              _sinkCount(0),
              _sinks(NULL)
        {
        }

    virtual ~ConnectionPoint()
        {
            if (_sinks)
                ::free(_sinks);
        }

    // IUnknown
    STDMETHODIMP QueryInterface(REFIID iid, PVOID *obj)
        {
            HRESULT hr;

            if (!obj)
                hr = E_POINTER;
            else if (IID_IUnknown == iid)
            {
                AddRef();
                *obj
                    = static_cast<LPUNKNOWN>(
                            static_cast<IConnectionPoint *>(this));
                hr = S_OK;
            }
            else if (IID_IConnectionPoint == iid)
            {
                AddRef();
                *obj = static_cast<IConnectionPoint *>(this);
                hr = S_OK;
            }
            else
            {
                *obj = NULL;
                hr = E_NOINTERFACE;
            }
            return hr;
        }

    STDMETHODIMP_(ULONG) AddRef() { return _container->AddRef(); }
    STDMETHODIMP_(ULONG) Release() { return _container->Release(); }

    // IDispatch
    STDMETHODIMP GetTypeInfoCount(UINT *)
        STDMETHODIMP_E_NOTIMPL_STUB
    STDMETHODIMP GetTypeInfo(UINT, LCID, LPTYPEINFO *)
        STDMETHODIMP_E_NOTIMPL_STUB
    STDMETHODIMP GetIDsOfNames(REFIID, LPOLESTR *, UINT, LCID, DISPID *)
        STDMETHODIMP_E_NOTIMPL_STUB
    STDMETHODIMP Invoke(DISPID, REFIID, LCID, WORD, DISPPARAMS *, VARIANT *, EXCEPINFO *, UINT *)
        STDMETHODIMP_E_NOTIMPL_STUB

    // IConnectionPoint
    STDMETHODIMP GetConnectionInterface(IID *pIID)
        {
            HRESULT hr;

            if (pIID)
            {
                *pIID = IID_T;
                hr = S_OK;
            }
            else
                hr = E_POINTER;
            return hr;
        }

    STDMETHODIMP GetConnectionPointContainer(IConnectionPointContainer **ppCPC)
        {
            HRESULT hr;

            if (ppCPC)
            {
                _container->AddRef();
                *ppCPC = _container;
                hr = S_OK;
            }
            else
                hr = E_POINTER;
            return hr;
        }

    STDMETHODIMP Advise(IUnknown *pUnkSink, DWORD *pdwCookie)
        {
            HRESULT hr;

            if (pdwCookie)
            {
                if (pUnkSink)
                {
                    T *t;

                    if (SUCCEEDED(
                            pUnkSink->QueryInterface(IID_T, (PVOID *) &t)))
                    {
                        LPDISPATCH iDispatch;

                        if (SUCCEEDED(
                                t->QueryInterface(
                                        IID_IDispatch,
                                        (PVOID *) &iDispatch)))
                        {
                            if (addSink(iDispatch))
                            {
                                *pdwCookie
                                    = (DWORD)
                                        (((intptr_t) iDispatch) & 0xffffffff);
                                hr = S_OK;
                            }
                            else
                            {
                                *pdwCookie = 0;
                                hr = CONNECT_E_CANNOTCONNECT;
                            }
                            iDispatch->Release();
                        }
                        else
                        {
                            *pdwCookie = 0;
                            hr = CONNECT_E_CANNOTCONNECT;
                        }

                        t->Release();
                    }
                    else
                    {
                        *pdwCookie = 0;
                        hr = CONNECT_E_CANNOTCONNECT;
                    }
                }
                else
                {
                    *pdwCookie = 0;
                    hr = E_POINTER; 
                }
            }
            else
                hr = E_POINTER;
            return hr;
        }

    STDMETHODIMP Unadvise(DWORD dwCookie)
        {
            size_t i = 0;
            LPDISPATCH *ptr = _sinks;
            HRESULT hr = E_POINTER;

            for (; i < _sinkCount; i++, ptr++)
            {
                LPDISPATCH iDispatch = *ptr;

                if (iDispatch
                        && (dwCookie
                                == (DWORD)
                                    (((intptr_t) iDispatch) & 0xffffffff)))
                {
                    *ptr = NULL;
                    iDispatch->Release();

                    _sinkCount--;
                    /*
                     * Move the emptied slot of the _sinks storage at the end
                     * where it is not accessible given the value of _sinkCount.
                     * Its memory is retained but it will either be used during
                     * a subsequent addSink(LPDISPATCH) or be freed upon
                     * deleting this ConnectionPoint.
                     */
                    for (; i < _sinkCount; i++)
                    {
                        LPDISPATCH *nextPtr = ptr + 1;

                        *ptr = *nextPtr;
                        ptr = nextPtr;
                    }

                    hr = S_OK;
                    break;
                }
            }
            return hr;
        }

    STDMETHODIMP EnumConnections(IEnumConnections **ppEnum)
        STDMETHODIMP_E_NOTIMPL_STUB

protected:
    HRESULT Invoke(DISPID dispIdMember, DISPPARAMS *pDispParams)
        {
            LPDISPATCH *sinks = getSinks();
            HRESULT hr;

            if (sinks)
            {
                for (LPDISPATCH sink, *sinkIt = sinks;
                        (sink = *sinkIt);
                        sinkIt++)
                {
                    hr
                        = sink->Invoke(
                                dispIdMember,
                                IID_NULL,
                                0,
                                DISPATCH_METHOD,
                                pDispParams,
                                NULL,
                                NULL,
                                NULL);
                    sink->Release();
                }
                ::free(sinks);

                hr = S_OK;
            }
            else
                hr = E_OUTOFMEMORY;
            return hr;
        }

private:
    BOOL addSink(LPDISPATCH sink)
        {
            BOOL b;

            if (containsSink(sink))
                b = FALSE;
            else
            {
                size_t newSinkCount = _sinkCount + 1;
                LPDISPATCH *newSinks
                    = (LPDISPATCH *)
                        ::realloc(_sinks, newSinkCount * sizeof(LPDISPATCH));

                if (newSinks)
                {
                    sink->AddRef();
                    newSinks[newSinkCount - 1] = sink;
                    _sinkCount = newSinkCount;
                    _sinks = newSinks;
                    b = TRUE;
                }
                else
                    b = FALSE;
            }
            return b;
        }

    BOOL containsSink(const LPDISPATCH sink)
        {
            size_t i = 0;
            LPDISPATCH *ptr = _sinks;

            for (; i < _sinkCount; i++, ptr++)
                if (sink == *ptr)
                    return TRUE;
            return FALSE;
        }

    LPDISPATCH *getSinks()
        {
            LPDISPATCH *sinks
                = (LPDISPATCH *)
                    ::malloc((_sinkCount + 1) * sizeof(LPDISPATCH));

            if (sinks)
            {
                size_t i = 0;
                LPDISPATCH *dst = sinks;
                LPDISPATCH *src = _sinks;

                for (; i < _sinkCount; i++, src++, dst++)
                {
                    LPDISPATCH sink = *src;

                    sink->AddRef();
                    *dst = sink;
                }
                *dst = NULL;
            }
            return sinks;
        }

    IConnectionPointContainer *_container;
    size_t _sinkCount;
    LPDISPATCH *_sinks;
};

#endif /* #ifndef _JMSOFFICECOMM_CONNECTIONPOINT_H_ */
