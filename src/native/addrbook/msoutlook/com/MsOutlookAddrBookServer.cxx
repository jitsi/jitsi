/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#include "MsOutlookAddrBookServer.h"

#include "MsOutlookAddrBookClient.h"

#include <mapix.h>
#include <stdio.h>
#include <wchar.h>

#include "../StringUtils.h"
#include "../MsOutlookAddrBookContactQuery.h"

/**
 * Instanciates a new MsOutlookAddrBookServer.
 */
MsOutlookAddrBookServer::MsOutlookAddrBookServer():
    _refCount(1)
{
}

/**
 * Deletes this MsOutlookAddrBookServer.
 */
MsOutlookAddrBookServer::~MsOutlookAddrBookServer()
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
STDMETHODIMP MsOutlookAddrBookServer::QueryInterface(REFIID iid, PVOID *obj)
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
                static_cast<IMsOutlookAddrBookServer *>(this));
        hr = S_OK;
    }
    else if (IID_IMsOutlookAddrBookServer == iid)
    {
        AddRef();
        *obj = static_cast<IMsOutlookAddrBookServer *>(this);
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
STDMETHODIMP_(ULONG) MsOutlookAddrBookServer::AddRef()
{
    return ++_refCount;
}

/**
 * Decrement the number of reference.
 *
 * @return The number of reference.
 */
STDMETHODIMP_(ULONG) MsOutlookAddrBookServer::Release()
{
    ULONG refCount = --_refCount;

    if(!refCount)
    {
        delete this;
    }

    return refCount;
}

/**
 * Starts a search for contact using MAPI.A
 *
 * @param query The search pattern (unused).
 *
 * @return S_OK.
 */
HRESULT STDMETHODCALLTYPE MsOutlookAddrBookServer::foreachMailUser(BSTR query)
{
    char * charQuery = StringUtils::WideCharToMultiByte(query);

    MsOutlookAddrBookContactQuery_foreachMailUser(
            charQuery,
            (void *) MsOutlookAddrBookServer::foreachMailUserCallback,
            NULL);

    free(charQuery);

    return S_OK;
}

/**
 * Calls back the java side to list a contact.
 *
 * @param iUnknown The string representation of the entry id of the contact.
 * @param object Not used. Must be set to NULL.
 *
 * @return True everything works fine and that we must continue to list the
 * other contacts. False otherwise.
 */
boolean MsOutlookAddrBookServer::foreachMailUserCallback(
        LPSTR iUnknown,
        void * object)
{
    HRESULT hr =  E_FAIL;

    IMsOutlookAddrBookClient * msOutlookAddrBookClient = NULL;
    if((hr = CoCreateInstance(
            CLSID_MsOutlookAddrBookClient,
            NULL,
            CLSCTX_LOCAL_SERVER,
            IID_IMsOutlookAddrBookClient,
            (void**) &msOutlookAddrBookClient)) == S_OK)
    {
        LPWSTR iUnknownW = StringUtils::MultiByteToWideChar(iUnknown);
        BSTR res = SysAllocString(iUnknownW);

        hr = msOutlookAddrBookClient->foreachMailUserCallback(res);

        SysFreeString(res);
        free(iUnknownW);
        msOutlookAddrBookClient->Release();
    }

    return (hr == S_OK);
}

/**
 * Get the properties for a given contact.
 *
 * @param entryId The contact identifier.
 * @param nbPropIds The number of properties requested.
 * @param propIds The list of property identifiers requested (long).
 * @param flags A bitmask of flags that indicates the format for properties.
 * @param props Used to return the properties gathered.
 * @param propsLength Used to return the list of porperty length.
 * @param propsType Used to return the list of type of property. b = byteArray,
 * l = long, s = 8 bits string, u = 16 bits string.
 *
 * @return S_OK if eveything works fine. Any error value otherwise.
 */
HRESULT STDMETHODCALLTYPE MsOutlookAddrBookServer::IMAPIProp_GetProps( 
        BSTR entryId,
        int nbPropIds,
        SAFEARRAY * propIds,
        long flags,
        SAFEARRAY ** props,
        SAFEARRAY ** propsLength,
        SAFEARRAY ** propsType)
{
    HRESULT hr = E_FAIL;
    void ** localProps = NULL;
    unsigned long* localPropsLength = NULL;
    // b = byteArray, l = long, s = 8 bits string, u = 16 bits string.
    char * localPropsType = NULL;

    if((localProps = (void**) malloc(nbPropIds * sizeof(void*))) != NULL)
    {
        memset(localProps, 0, nbPropIds * sizeof(void*));
        if((localPropsLength = (unsigned long*) malloc(
                        nbPropIds * sizeof(unsigned long))) != NULL)
        {
            if((localPropsType = (char*) malloc(nbPropIds * sizeof(char)))
                    != NULL)
            {
                SafeArrayLock(propIds);
                long * longPropIds = (long*) propIds->pvData;
                SafeArrayUnlock(propIds);

                LPSTR id = StringUtils::WideCharToMultiByte(entryId);

                hr = MsOutlookAddrBookContactQuery_IMAPIProp_1GetProps(
                        id,
                        nbPropIds,
                        longPropIds,
                        flags,
                        localProps,
                        localPropsLength,
                        localPropsType);

                free(id);

                if(HR_SUCCEEDED(hr))
                {
                    long totalLength = 0;
                    for(int j = 0; j < nbPropIds; ++j)
                    {
                        totalLength += localPropsLength[j];
                    }
                    (*props) = SafeArrayCreateVector(VT_UI1, 0, totalLength);
                    SafeArrayLock(*props);
                    byte * data = (byte*) (*props)->pvData;
                    for(int j = 0; j < nbPropIds; ++j)
                    {
                        memcpy(data, localProps[j], localPropsLength[j]);
                        data += localPropsLength[j];
                    }
                    SafeArrayUnlock(*props);

                    (*propsLength) = SafeArrayCreateVector(VT_I4, 0, nbPropIds);
                    SafeArrayLock(*propsLength);
                    memcpy(
                            (*propsLength)->pvData,
                            localPropsLength,
                            nbPropIds * sizeof(unsigned long));
                    SafeArrayUnlock(*propsLength);

                    (*propsType) = SafeArrayCreateVector(VT_UI1, 0, nbPropIds);
                    SafeArrayLock(*propsType);
                    memcpy(
                            (*propsType)->pvData,
                            localPropsType,
                            nbPropIds * sizeof(char));
                    SafeArrayUnlock(*propsType);
                }

                for(int j = 0; j < nbPropIds; ++j)
                {
                    if(localProps[j] != NULL)
                        free(localProps[j]);
                }

                free(localPropsType);
            }
            free(localPropsLength);
        }
        free(localProps);
    }

    return hr;
}

/**
 * Creates an empty contact.
 *
 * @param id The identifier of the created contact.
 *
 * @return S_OK if the contact was correctly created. E_FAIL otherwise.
 */
HRESULT STDMETHODCALLTYPE MsOutlookAddrBookServer::createContact( 
        BSTR *id)
{
    LPSTR nativeId = MsOutlookAddrBookContactQuery_createContact();
    if(nativeId != NULL)
    {
        LPWSTR nativeWId = StringUtils::MultiByteToWideChar(nativeId);
        *id = SysAllocString(nativeWId);
        free(nativeWId);
        free(nativeId);
        return S_OK;
    }
    return E_FAIL;
}

/**
 * Deletes a contact.
 *
 * @param id The identifier of the contact to delete.
 *
 * @return S_OK if the contact was correctly deleted. E_FAIL otherwise.
 */
HRESULT STDMETHODCALLTYPE MsOutlookAddrBookServer::deleteContact( 
        BSTR id)
{
    HRESULT hr = E_FAIL;
    if(id != NULL)
    {
        LPSTR nativeId = StringUtils::WideCharToMultiByte(id);
        if(MsOutlookAddrBookContactQuery_deleteContact(nativeId) == 1)
        {
            hr = S_OK;
        }
        free(nativeId);
    }
    return hr;
}

/**
 * Deletes a given property for a contact.
 *
 * @param propId The identifier of the property to delete.
 * @param entryId The identifier of the contact.
 *
 * @return S_OK if the contact was correctly deleted. E_FAIL otherwise.
 */
HRESULT STDMETHODCALLTYPE MsOutlookAddrBookServer::IMAPIProp_DeleteProp( 
        long propId,
        BSTR entryId)
{
    HRESULT hr = E_FAIL;
    if(entryId != NULL)
    {
        LPSTR nativeId = StringUtils::WideCharToMultiByte(entryId);
        if(MsOutlookAddrBookContactQuery_IMAPIProp_1DeleteProp(
                    propId,
                    nativeId) == 1)
        {
            hr = S_OK;
        }
        free(nativeId);
    }
    return hr;
}

/**
 * Sets a given property for a contact.
 *
 * @param propId The identifier of the property to set.
 * @param value The value to set for the property.
 * @param entryId The identifier of the contact.
 *
 * @return S_OK if the contact was correctly deleted. E_FAIL otherwise.
 */
HRESULT STDMETHODCALLTYPE MsOutlookAddrBookServer::IMAPIProp_SetPropString( 
        long propId,
        BSTR value,
        BSTR entryId)
{
    HRESULT hr = E_FAIL;
    if(value != NULL && entryId != NULL)
    {
        LPSTR nativeId = StringUtils::WideCharToMultiByte(entryId);
        if(MsOutlookAddrBookContactQuery_IMAPIProp_1SetPropString(
                    propId,
                    value,
                    nativeId) == 1)
        {
            hr = S_OK;
        }
        free(nativeId);
    }
    return hr;
}

/**
 * Compares two identifiers to determine if they are part of the same
 * Outlook contact.
 *
 * @param id1 The first identifier.
 * @param id2 The second identifier.
 * @param result A boolean set to true if id1 and id2 are two identifiers of the
 * same contact.  False otherwise.
 *
 * @return S_OK if eveything works fine. E_FAIL otherwise.
 */
HRESULT STDMETHODCALLTYPE MsOutlookAddrBookServer::compareEntryIds(
       BSTR id1,
       BSTR id2,
       int * result)
{
    HRESULT hr = E_FAIL;
    if(id1 != NULL && id2 != NULL)
    {
        LPSTR nativeId1 = StringUtils::WideCharToMultiByte(id1);
        LPSTR nativeId2 = StringUtils::WideCharToMultiByte(id2);
        (*result) = MsOutlookAddrBookContactQuery_compareEntryIds(
                nativeId1,
                nativeId2);
        hr = S_OK;
        free(nativeId1);
        free(nativeId2);
    }
    return hr;
}
