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
#ifndef _JMSOFFICECOMM_DISPATCHIMPL_H_
#define _JMSOFFICECOMM_DISPATCHIMPL_H_

#include <oaidl.h>
#include "UnknownImpl.h"

/**
 * Represents a base implementation of the <tt>IDispatch</tt> interface.
 *
 * @author Lyubomir Marinov
 */
template <class T, REFIID IID_T>
class DispatchImpl
    : public UnknownImpl<T, IID_T>
{
public:
    // IUnknown
    STDMETHODIMP QueryInterface(REFIID iid, PVOID *obj)
        {
            HRESULT ret;

            if (obj)
            {
                if (IID_IDispatch == iid)
                {
                    static_cast<LPUNKNOWN>(this)->AddRef();
                    *obj = static_cast<LPDISPATCH>(this);
                    ret = S_OK;
                }
                else
                    ret = UnknownImpl<T, IID_T>::QueryInterface(iid, obj);
            }
            else
                ret = E_POINTER;
            return ret;
        }

    // IDispatch
    STDMETHODIMP GetTypeInfoCount(UINT *pctinfo)
#ifdef DISPATCHIMPL_CREATESTDDISPATCH
        {
            LPDISPATCH iDispatch = getIDispatch();
            HRESULT hr;

            if (iDispatch)
                hr = iDispatch->GetTypeInfoCount(pctinfo);
            else if (pctinfo)
            {
                *pctinfo = 0;
                hr = S_OK;
            }
            else
                hr = E_INVALIDARG;
            return hr;
        }
#else
        STDMETHODIMP_E_NOTIMPL_STUB
#endif /* #ifdef DISPATCHIMPL_CREATESTDDISPATCH */

    STDMETHODIMP GetTypeInfo(UINT iTInfo, LCID lcid, LPTYPEINFO *ppTInfo)
#ifdef DISPATCHIMPL_CREATESTDDISPATCH
        {
            LPDISPATCH iDispatch = getIDispatch();
            HRESULT hr;

            if (iDispatch)
                hr = iDispatch->GetTypeInfo(iTInfo, lcid, ppTInfo);
            else if (ppTInfo)
            {
                *ppTInfo = NULL;
                hr = TYPE_E_ELEMENTNOTFOUND;
            }
            else
                hr = E_INVALIDARG;
            return hr;
        }
#else
        STDMETHODIMP_E_NOTIMPL_STUB
#endif /* #ifdef DISPATCHIMPL_CREATESTDDISPATCH */

    STDMETHODIMP GetIDsOfNames(REFIID riid, LPOLESTR *rgszNames, UINT cNames, LCID lcid, DISPID *rgDispId)
#ifdef DISPATCHIMPL_CREATESTDDISPATCH
        {
            LPDISPATCH iDispatch = getIDispatch();
            HRESULT hr;

            if (iDispatch)
                hr = iDispatch->GetIDsOfNames(riid, rgszNames, cNames, lcid, rgDispId);
            else
                hr = TYPE_E_ELEMENTNOTFOUND;
            return hr;
        }
#else
        STDMETHODIMP_E_NOTIMPL_STUB
#endif /* #ifdef DISPATCHIMPL_CREATESTDDISPATCH */

    STDMETHODIMP Invoke(DISPID dispIdMember, REFIID riid, LCID lcid, WORD wFlags, DISPPARAMS *pDispParams, VARIANT *pVarResult, EXCEPINFO *pExcepInfo, UINT *puArgErr)
#ifdef DISPATCHIMPL_CREATESTDDISPATCH
        {
            LPDISPATCH iDispatch = getIDispatch();
            HRESULT hr;

            if (iDispatch)
                hr = iDispatch->Invoke(dispIdMember, riid, lcid, wFlags, pDispParams, pVarResult, pExcepInfo, puArgErr);
            else
                hr = TYPE_E_ELEMENTNOTFOUND;
            return hr;
        }
#else
        STDMETHODIMP_E_NOTIMPL_STUB
#endif /* #ifdef DISPATCHIMPL_CREATESTDDISPATCH */

protected:
    DispatchImpl() : _iDispatch(NULL) {};

    virtual ~DispatchImpl()
        {
            if (_iDispatch)
            {
                _iDispatch->Release();
                _iDispatch = NULL;
            }
        }

private:
#ifdef DISPATCHIMPL_CREATESTDDISPATCH
    LPDISPATCH getIDispatch()
        {
            if (!_iDispatch)
            {
                LPTYPEINFO iTypeInfo;

                if (SUCCEEDED(
                        OutOfProcessServer::getTypeInfoOfGuid(
                                IID_T,
                                &iTypeInfo)))
                {
                    LPUNKNOWN iUnknown;

                    if (SUCCEEDED(
                            ::CreateStdDispatch(
                                    this,
                                    this,
                                    iTypeInfo,
                                    &iUnknown)))
                    {
                        LPDISPATCH iDispatch;

                        if (SUCCEEDED(
                                iUnknown->QueryInterface(
                                        IID_IDispatch,
                                        (PVOID *) &iDispatch)))
                            _iDispatch = iDispatch;
                        iUnknown->Release();
                    }
                    iTypeInfo->Release();
                }
            }
            return _iDispatch;
        }
#endif /* #ifdef DISPATCHIMPL_CREATESTDDISPATCH */

    LPDISPATCH _iDispatch;
};

#endif /* #ifndef _JMSOFFICECOMM_DISPATCHIMPL_H_ */
