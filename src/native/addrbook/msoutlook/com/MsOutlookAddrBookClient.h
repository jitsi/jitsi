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
#ifndef __MSOUTLOOKADDRBOOKCOM_MSOUTLOOKADDRBOOKCLIENT_H
#define __MSOUTLOOKADDRBOOKCOM_MSOUTLOOKADDRBOOKCLIENT_H

#include <stdio.h>
#include <unknwn.h>

#include "IMsOutlookAddrBookClient.h"

EXTERN_C const GUID DECLSPEC_SELECTANY CLSID_MsOutlookAddrBookClient
    = {0x867BD590, 0xB1AC, 0x11E2,
        {0x9E, 0x96, 0x08, 0x00, 0x20, 0x0C, 0x9A, 0x66}}; // generated

EXTERN_C const GUID DECLSPEC_SELECTANY IID_IMsOutlookAddrBookClient
    = {0xD579E840, 0xB1A6, 0x11E2,
        {0x9E, 0x96, 0x08, 0x00, 0x20, 0x0C, 0x9A, 0x66}}; // generated

/**
 * @author Vincent Lucas
 */
class MsOutlookAddrBookClient:
    public IMsOutlookAddrBookClient
{
    public:
        MsOutlookAddrBookClient();

        // IUnknown
        STDMETHODIMP QueryInterface(REFIID iid, PVOID *obj);
        STDMETHODIMP_(ULONG) AddRef();
        STDMETHODIMP_(ULONG) Release();

        // IMsOutlookAddrBookClient
        HRESULT STDMETHODCALLTYPE foreachMailUserCallback(
                BSTR id, long callback);
		HRESULT STDMETHODCALLTYPE foreachCalendarItemCallback(
				BSTR id, long callback);

        HRESULT STDMETHODCALLTYPE deleted(BSTR id, ULONG type);
        HRESULT STDMETHODCALLTYPE inserted(BSTR id, ULONG type);
        HRESULT STDMETHODCALLTYPE updated(BSTR id, ULONG type);

    protected:
            virtual ~MsOutlookAddrBookClient();

    private:
            ULONG _refCount;
};

#endif
