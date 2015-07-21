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
#include "WeakReference.h"

STDMETHODIMP WeakReference::QueryInterface(REFIID iid, PVOID *obj)
{
    HRESULT hr;

    if (!obj)
        hr = E_POINTER;
    else if (IID_IWeakReferenceSource == iid)
    {
        /*
         * While a weak reference to a weak reference should technically be
         * possible, such functionality does not seem necessary at the time of
         * this writing.
         */
        *obj = NULL;
        hr = E_NOINTERFACE;
    }
    else
        hr = UnknownImpl::QueryInterface(iid, obj);
    return hr;
}

STDMETHODIMP WeakReference::Resolve(REFIID iid, PVOID *obj)
{
    HRESULT hr;

    if (obj)
    {
        if (_iUnknown)
            hr = _iUnknown->QueryInterface(iid, obj);
        else
        {
            *obj = NULL;
            hr = E_FAIL;
        }
    }
    else
        hr = E_POINTER;
    return hr;
}
