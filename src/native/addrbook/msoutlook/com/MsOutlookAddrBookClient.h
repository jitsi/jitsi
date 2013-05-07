/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
        HRESULT STDMETHODCALLTYPE foreachMailUserCallback(BSTR id);

        HRESULT STDMETHODCALLTYPE deleted(BSTR id);
        HRESULT STDMETHODCALLTYPE inserted(BSTR id);
        HRESULT STDMETHODCALLTYPE updated(BSTR id);

    protected:
            virtual ~MsOutlookAddrBookClient();

    private:
            ULONG _refCount;
};

#endif
