/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#include "MsOutlookAddrBookClient.h"

#include <stdio.h>
#include <wchar.h>

#include "../MAPINotification.h"
#include "../net_java_sip_communicator_plugin_addrbook_msoutlook_MsOutlookAddrBookContactQuery.h"
#include "../StringUtils.h"

/**
 * Instanciates a new MsOutlookAddrBookClient.
 */
MsOutlookAddrBookClient::MsOutlookAddrBookClient():
    _refCount(1)
{
}

/**
 * Deletes this MsOutlookAddrBookClient.
 */
MsOutlookAddrBookClient::~MsOutlookAddrBookClient()
{
    Release();
}

/**
 * Returns an instance of the interface requested, if available.
 *
 * @param iid The identifier of the interface requested.
 * @param obj A pointer use to return an object instance implementing the
 * interface requested.
 *
 * @return S_OK if the corresponding interface has been found. E_POINTER, or
 * E_NOINTERFACE otherwise.
 */
STDMETHODIMP MsOutlookAddrBookClient::QueryInterface(REFIID iid, PVOID *obj)
{
    HRESULT hr;

    if (!obj)
    {
        hr = E_POINTER;
    }
    else if (IID_IUnknown == iid)
    {
        AddRef();
        *obj = static_cast<IUnknown *>(
                static_cast<IMsOutlookAddrBookClient *>(this));
        hr = S_OK;
    }
    else if (IID_IMsOutlookAddrBookClient == iid)
    {
        AddRef();
        *obj = static_cast<IMsOutlookAddrBookClient *>(this);
        hr = S_OK;
    }
    else
    {
        hr = E_NOINTERFACE;
    }

    return hr;
}

/**
 * Increment the number of reference.
 *
 * @return The number of reference.
 */
STDMETHODIMP_(ULONG) MsOutlookAddrBookClient::AddRef()
{
    return ++_refCount;
}

/**
 * Decrement the number of reference.
 *
 * @return The number of reference.
 */
STDMETHODIMP_(ULONG) MsOutlookAddrBookClient::Release()
{
    ULONG refCount = --_refCount;

    if(!refCount)
    {
        delete this;
    }

    return refCount;
}

/**
 * Callback which receives each time the COM server found a contact when making
 * a search via the foreachMailUser function.
 *
 * @param id The contact identifier.
 *
 * @return S_OK.
 */
HRESULT STDMETHODCALLTYPE MsOutlookAddrBookClient::foreachMailUserCallback(
        BSTR id)
{
    char * charId = StringUtils::WideCharToMultiByte(id);
    MAPINotification_callCallbackMethod(charId, NULL);
    free(charId);

    return S_OK;
}

/**
 * Callback called each time the COM server forward a contact deleted notify
 * event from MAPI.
 *
 * @param id The contact identifier.
 *
 * @return S_OK.
 */
HRESULT STDMETHODCALLTYPE MsOutlookAddrBookClient::deleted(BSTR id)
{
    char * charId = StringUtils::WideCharToMultiByte(id);
    MAPINotification_jniCallDeletedMethod(charId);
    free(charId);

    return S_OK;
}

/**
 * Callback called each time the COM server forward a contact inserted notify
 * event from MAPI.
 *
 * @param id The contact identifier.
 *
 * @return S_OK.
 */
HRESULT STDMETHODCALLTYPE MsOutlookAddrBookClient::inserted(BSTR id)
{
    char * charId = StringUtils::WideCharToMultiByte(id);
    MAPINotification_jniCallInsertedMethod(charId);
    free(charId);

    return S_OK;
}

/**
 * Callback called each time the COM server forward a contact updated notify
 * event from MAPI.
 *
 * @param id The contact identifier.
 *
 * @return S_OK.
 */
HRESULT STDMETHODCALLTYPE MsOutlookAddrBookClient::updated(BSTR id)
{
    char * charId = StringUtils::WideCharToMultiByte(id);
    MAPINotification_jniCallUpdatedMethod(charId);
    free(charId);

    return S_OK;
}
