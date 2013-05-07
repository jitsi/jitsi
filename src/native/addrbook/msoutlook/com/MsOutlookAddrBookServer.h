/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#ifndef __MSOUTLOOKADDRBOOKCOM_MSOUTLOOKADDRBOOKSERVER_H
#define __MSOUTLOOKADDRBOOKCOM_MSOUTLOOKADDRBOOKSERVER_H

#include <stdio.h>
#include <unknwn.h>

#include "IMsOutlookAddrBookServer.h"

EXTERN_C const GUID DECLSPEC_SELECTANY CLSID_MsOutlookAddrBookServer
    = {0x22435A40, 0xAB57, 0x11E2,
        {0x9E, 0x96, 0x08, 0x00, 0x20, 0x0C, 0x9A, 0x66}}; // generated.

EXTERN_C const GUID DECLSPEC_SELECTANY IID_IMsOutlookAddrBookServer
    = {0x5DDE9FF0, 0xAC48, 0x11E2,
        {0x9E, 0x96, 0x08, 0x00, 0x20, 0x0C, 0x9A, 0x66}}; // generated

/**
 * @author Vincent Lucas
 */
class MsOutlookAddrBookServer:
    public IMsOutlookAddrBookServer
{
    public:
        MsOutlookAddrBookServer();

        // IUnknown
        STDMETHODIMP QueryInterface(REFIID, PVOID *);
        STDMETHODIMP_(ULONG) AddRef();
        STDMETHODIMP_(ULONG) Release();

        // IMsOutlookAddrBookServer
        HRESULT STDMETHODCALLTYPE foreachMailUser(BSTR query);

        HRESULT STDMETHODCALLTYPE IMAPIProp_GetProps( 
                BSTR entryId,
                int nbPropIds,
                SAFEARRAY * propIds,
                long flags,
                SAFEARRAY ** props,
                SAFEARRAY ** propsLength,
                SAFEARRAY ** propsType);

        HRESULT STDMETHODCALLTYPE createContact( 
                BSTR *id);

        HRESULT STDMETHODCALLTYPE deleteContact( 
                BSTR id);

        HRESULT STDMETHODCALLTYPE IMAPIProp_DeleteProp( 
                long propId,
                BSTR entryId);

        HRESULT STDMETHODCALLTYPE IMAPIProp_SetPropString( 
                long propId,
                BSTR value,
                BSTR entryId);

    protected:
            virtual ~MsOutlookAddrBookServer();

    private:
            ULONG _refCount;

            static boolean foreachMailUserCallback(
                    LPSTR iUnknown,
                    void * object);
};

#endif



