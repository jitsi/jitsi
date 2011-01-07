/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#include "net_java_sip_communicator_plugin_addrbook_msoutlook_MsOutlookAddrBookContactQuery.h"

#include "AddrBookContactQuery.h"
#include "MsOutlookMAPI.h"
#include "MsOutlookMAPIHResultException.h"

#include <string.h>

#define WIND32_MEAN_AND_LEAK
#include <windows.h>

static jboolean MsOutlookAddrBookContactQuery_foreachMailUser
    (ULONG objType, LPUNKNOWN iUnknown,
    JNIEnv *jniEnv,
    jstring query,
    jobject callback, jmethodID callbackMethodID);
static jboolean MsOutlookAddrBookContactQuery_foreachMailUserInContainerTable
    (LPMAPICONTAINER mapiContainer, LPMAPITABLE mapiTable,
    JNIEnv *jniEnv,
    jstring query,
    jobject callback, jmethodID callbackMethodID);
static void MsOutlookAddrBookContactQuery_freeSRowSet(LPSRowSet rows);
static jboolean MsOutlookAddrBookContactQuery_mailUserMatches
    (LPMAPIPROP mailUser, JNIEnv *jniEnv, jstring query);

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_plugin_addrbook_msoutlook_MsOutlookAddrBookContactQuery_foreachMailUser
    (JNIEnv *jniEnv, jclass clazz, jstring query, jobject callback)
{
    jmethodID callbackMethodID;

    HRESULT hResult;
    LPMAPISESSION mapiSession;

    callbackMethodID
        = AddrBookContactQuery_getPtrCallbackMethodID(jniEnv, callback);
    if (!callbackMethodID || (JNI_TRUE == jniEnv->ExceptionCheck()))
        return;

    hResult
        = MAPILogonEx(
                0,
                NULL, NULL,
                MAPI_EXTENDED | MAPI_NO_MAIL | MAPI_USE_DEFAULT,
                &mapiSession);
    if (HR_SUCCEEDED(hResult))
    {
        LPADRBOOK adrBook;

        hResult = mapiSession->OpenAddressBook(0, NULL, AB_NO_DIALOG, &adrBook);
        if (HR_SUCCEEDED(hResult))
        {
            ULONG objType;
            LPUNKNOWN iUnknown;

            hResult = adrBook->OpenEntry(0, NULL, NULL, 0, &objType, &iUnknown);
            if (HR_SUCCEEDED(hResult))
            {
                MsOutlookAddrBookContactQuery_foreachMailUser(
                        objType, iUnknown,
                        jniEnv, query, callback, callbackMethodID);

                iUnknown->Release();
            }
            else
            {
                MsOutlookMAPIHResultException_throwNew(
                        jniEnv,
                        hResult,
                        __FILE__, __LINE__);
            }

            adrBook->Release();
        }
        else
        {
            MsOutlookMAPIHResultException_throwNew(
                    jniEnv,
                    hResult,
                    __FILE__, __LINE__);
        }

        mapiSession->Logoff(0, 0, 0);
        mapiSession->Release();
    }
    else
    {
        MsOutlookMAPIHResultException_throwNew(
                jniEnv,
                hResult,
                __FILE__, __LINE__);
    }
}

JNIEXPORT jobjectArray JNICALL
Java_net_java_sip_communicator_plugin_addrbook_msoutlook_MsOutlookAddrBookContactQuery_IMAPIProp_1GetProps
    (JNIEnv *jniEnv, jclass clazz,
    jlong mapiProp, jlongArray propIds, jlong flags)
{
    jsize propIdCount;
    LPSPropTagArray propTagArray;
    jobjectArray props = NULL;

    propIdCount = jniEnv->GetArrayLength(propIds);
    if (S_OK
            == MAPIAllocateBuffer(
                    CbNewSPropTagArray(propIdCount),
                    (void **) &propTagArray))
    {
        jsize i;

        propTagArray->cValues = propIdCount;
        for (i = 0; i < propIdCount; i++)
        {
            jlong propId;

            jniEnv->GetLongArrayRegion(propIds, i, 1, &propId);
            if (JNI_TRUE == jniEnv->ExceptionCheck())
            {
                MAPIFreeBuffer(propTagArray);
                propTagArray = NULL;
                break;
            }
            else
            {
                *(propTagArray->aulPropTag + i)
                    = PROP_TAG(PT_UNSPECIFIED, propId);
            }
        }
        if (propTagArray)
        {
            HRESULT hResult;
            ULONG propCount;
            LPSPropValue propArray;

            hResult
                = ((LPMAPIPROP)  mapiProp)->GetProps(
                        propTagArray,
                        (ULONG) flags,
                        &propCount, &propArray);
            if (HR_SUCCEEDED(hResult))
            {
                jclass objectClass;
                ULONG j;

                objectClass = jniEnv->FindClass("java/lang/Object");
                if (objectClass)
                {
                    props
                        = jniEnv->NewObjectArray(
                                (jsize) propCount,
                                objectClass,
                                NULL);
                }
                for (j = 0; j < propCount; j++)
                {
                    LPSPropValue prop = propArray;

                    if (props)
                    {
                        switch (PROP_TYPE(prop->ulPropTag))
                        {
                        case PT_STRING8:
                        {
                            jstring value;

                            value = jniEnv->NewStringUTF(prop->Value.lpszA);
                            if (value)
                            {
                                jniEnv->SetObjectArrayElement(props, j, value);
                                if (jniEnv->ExceptionCheck())
                                    props = NULL;
                            }
                            break;
                        }

                        case PT_UNICODE:
                        {
                            jstring value;

                            value
                                = jniEnv->NewString(
                                        (const jchar *) (prop->Value.lpszW),
                                        wcslen(prop->Value.lpszW));
                            if (value)
                            {
                                jniEnv->SetObjectArrayElement(props, j, value);
                                if (jniEnv->ExceptionCheck())
                                    props = NULL;
                            }
                            break;
                        }
                        }
                    }
                    MAPIFreeBuffer(prop);
                    propArray++;
                }
            }
            else
            {
                MsOutlookMAPIHResultException_throwNew(
                        jniEnv,
                        hResult,
                        __FILE__, __LINE__);
            }
            MAPIFreeBuffer(propTagArray);
        }
    }
    else
    {
        MsOutlookMAPIHResultException_throwNew(
                jniEnv,
                MAPI_E_NOT_ENOUGH_MEMORY,
                __FILE__, __LINE__);
    }
    return props;
}

static jboolean
MsOutlookAddrBookContactQuery_foreachMailUser
    (ULONG objType, LPUNKNOWN iUnknown,
    JNIEnv *jniEnv,
    jstring query,
    jobject callback, jmethodID callbackMethodID)
{
    jboolean proceed;

    switch (objType)
    {
    case MAPI_ABCONT:
    {
        LPMAPICONTAINER mapiContainer = (LPMAPICONTAINER) iUnknown;

        HRESULT hResult;
        LPMAPITABLE mapiTable;

        proceed = JNI_TRUE;

        /* Look for MAPI_MAILUSER through the contents. */
        mapiTable = NULL;
        hResult = mapiContainer->GetContentsTable(0, &mapiTable);
        if (HR_SUCCEEDED(hResult) && mapiTable)
        {
            proceed
                = MsOutlookAddrBookContactQuery_foreachMailUserInContainerTable(
                        mapiContainer, mapiTable,
                        jniEnv, query, callback, callbackMethodID);
            mapiTable->Release();
        }

        /* Drill down the hierarchy. */
        if (JNI_TRUE == proceed)
        {
            mapiTable = NULL;
            hResult = mapiContainer->GetHierarchyTable(0, &mapiTable);
            if (HR_SUCCEEDED(hResult) && mapiTable)
            {
                proceed
                    = MsOutlookAddrBookContactQuery_foreachMailUserInContainerTable(
                            mapiContainer, mapiTable,
                            jniEnv, query, callback, callbackMethodID);
                mapiTable->Release();
            }
        }

        break;
    }

    case MAPI_MAILUSER:
    {
        if (JNI_TRUE
                == MsOutlookAddrBookContactQuery_mailUserMatches(
                        (LPMAPIPROP) iUnknown,
                        jniEnv, query))
        {
            /* Report the MAPI_MAILUSER to the callback. */
            proceed
                = jniEnv->CallBooleanMethod(
                        callback, callbackMethodID,
                        iUnknown);
            /*
             * XXX When an exception is thrown in the callback, does proceed get
             * assigned JNI_FALSE?
             */
            if ((JNI_TRUE == proceed) && (JNI_TRUE == jniEnv->ExceptionCheck()))
                proceed = JNI_FALSE;
        }
        else
            proceed = JNI_TRUE;
        break;
    }
    }
    return proceed;
}

static jboolean
MsOutlookAddrBookContactQuery_foreachMailUserInContainerTable
    (LPMAPICONTAINER mapiContainer, LPMAPITABLE mapiTable,
    JNIEnv *jniEnv,
    jstring query,
    jobject callback, jmethodID callbackMethodID)
{
    HRESULT hResult;
    jboolean proceed;

    hResult = mapiTable->SeekRow(BOOKMARK_BEGINNING, 0, NULL);
    if (HR_SUCCEEDED(hResult))
    {
        LPSRowSet rows;

        proceed = JNI_TRUE;
        while ((JNI_TRUE == proceed)
                && HR_SUCCEEDED(hResult = mapiTable->QueryRows(1, 0, &rows)))
        {
            if (rows->cRows == 1)
            {
                ULONG i;
                LPSRow row = rows->aRow;
                ULONG objType = 0;
                SBinary entryIDBinary = {0, NULL};

                for (i = 0; i < row->cValues; i++)
                {
                    LPSPropValue prop = (row->lpProps) + i;

                    switch (prop->ulPropTag)
                    {
                    case PR_OBJECT_TYPE:
                        objType = prop->Value.ul;
                        break;
                    case PR_ENTRYID:
                        entryIDBinary = prop->Value.bin;
                        break;
                    }
                }

                if (objType && entryIDBinary.cb && entryIDBinary.lpb)
                {
                    LPENTRYID entryID;

                    if (S_OK
                            == MAPIAllocateBuffer(
                                    entryIDBinary.cb,
                                    (void **) &entryID))
                    {
                        LPUNKNOWN iUnknown;

                        CopyMemory(
                            entryID,
                            entryIDBinary.lpb,
                            entryIDBinary.cb);

                        /*
                         * We no longer need the rows at this point so free them
                         * before we drill down the hierarchy and allocate even more rows.
                         */
                        MsOutlookAddrBookContactQuery_freeSRowSet(rows);

                        hResult
                            = mapiContainer->OpenEntry(
                                    entryIDBinary.cb, entryID,
                                    NULL,
                                    0,
                                    &objType, &iUnknown);
                        if (HR_SUCCEEDED(hResult))
                        {
                            proceed
                                = MsOutlookAddrBookContactQuery_foreachMailUser(
                                        objType, iUnknown,
                                        jniEnv, query, callback, callbackMethodID);
                            iUnknown->Release();
                        }

                        MAPIFreeBuffer(entryID);
                    }
                    else
                        MsOutlookAddrBookContactQuery_freeSRowSet(rows);
                }
                else
                    MsOutlookAddrBookContactQuery_freeSRowSet(rows);
            }
            else
            {
                MAPIFreeBuffer(rows);
                break;
            }
        }
    }
    else
    {
        /* We've failed but other parts of the hierarchy may still succeed. */
        proceed = JNI_TRUE;
    }
    return proceed;
}

static void
MsOutlookAddrBookContactQuery_freeSRowSet(LPSRowSet rows)
{
    ULONG i;

    for (i = 0; i < rows->cRows; i++)
    {
        LPSRow row = (rows->aRow) + i;
        ULONG j;

        for (j = 0; j < row->cValues; j++)
        {
            LPSPropValue prop = (row->lpProps) + j;

            MAPIFreeBuffer(prop);
        }
    }
    MAPIFreeBuffer(rows);
}

static jboolean
MsOutlookAddrBookContactQuery_mailUserMatches
    (LPMAPIPROP mailUser, JNIEnv *jniEnv, jstring query)
{
    // TODO Auto-generated method stub
    return JNI_TRUE;
}
