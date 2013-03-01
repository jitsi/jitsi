/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#include "net_java_sip_communicator_plugin_addrbook_msoutlook_MsOutlookAddrBookContactQuery.h"

#include "../AddrBookContactQuery.h"
#include "MAPINotification.h"
#include "MAPISession.h"
#include "MsOutlookMAPIHResultException.h"
#include <stdio.h>
#include <string.h>

#define PR_ATTACHMENT_CONTACTPHOTO PROP_TAG(PT_BOOLEAN, 0x7FFF)

typedef
    jboolean (*MsOutlookAddrBookContactQuery_ForeachRowInTableCallback)
        (LPUNKNOWN iUnknown,
        ULONG entryIDByteCount, LPENTRYID entryID, ULONG objType,
        JNIEnv *jniEnv,
        jstring query,
        jobject callback, jmethodID callbackMethodID);

static ULONG MsOutlookAddrBookContactQuery_openEntryUlFlags = MAPI_BEST_ACCESS;

static HRESULT MsOutlookAddrBookContactQuery_HrGetOneProp
    (LPMAPIPROP mapiProp, ULONG propTag, LPSPropValue *prop);

static jboolean MsOutlookAddrBookContactQuery_foreachContactInMsgStoresTable
    (LPMAPISESSION mapiSession,
    JNIEnv *jniEnv,
    jstring query,
    jobject callback, jmethodID callbackMethodID);
static jboolean MsOutlookAddrBookContactQuery_foreachMailUser
    (ULONG objType, LPUNKNOWN iUnknown,
    JNIEnv *jniEnv,
    jstring query,
    jobject callback, jmethodID callbackMethodID);
static jboolean MsOutlookAddrBookContactQuery_foreachMailUserInAddressBook
    (LPMAPISESSION mapiSession,
    JNIEnv *jniEnv,
    jstring query,
    jobject callback, jmethodID callbackMethodID);
static jboolean MsOutlookAddrBookContactQuery_foreachMailUserInContainerTable
    (LPMAPICONTAINER mapiContainer, LPMAPITABLE mapiTable,
    JNIEnv *jniEnv,
    jstring query,
    jobject callback, jmethodID callbackMethodID);
static jboolean MsOutlookAddrBookContactQuery_foreachRowInTable
    (LPMAPITABLE mapiTable,
    MsOutlookAddrBookContactQuery_ForeachRowInTableCallback rowCallback,
    LPUNKNOWN iUnknown,
    JNIEnv *jniEnv, jstring query, jobject callback, jmethodID callbackMethodID);
static void MsOutlookAddrBookContactQuery_freeSRowSet(LPSRowSet rows);
static jbyteArray MsOutlookAddrBookContactQuery_getAttachmentContactPhoto
    (LPMESSAGE message,
    JNIEnv *jniEnv);
static HRESULT MsOutlookAddrBookContactQuery_getContactsFolderEntryID
    (LPMDB msgStore,
    ULONG folderEntryIDByteCount, LPENTRYID folderEntryID,
    ULONG *contactsFolderEntryIDByteCount, LPENTRYID *contactsFolderEntryID);
static ULONG MsOutlookAddrBookContactQuery_getPropTagFromLid
    (LPMAPIPROP mapiProp, LONG lid);
static jboolean MsOutlookAddrBookContactQuery_mailUserMatches
    (LPMAPIPROP mailUser, JNIEnv *jniEnv, jstring query);
static jboolean MsOutlookAddrBookContactQuery_onForeachContactInMsgStoresTableRow
    (LPUNKNOWN mapiSession,
    ULONG entryIDByteCount, LPENTRYID entryID, ULONG objType,
    JNIEnv *jniEnv, jstring query, jobject callback, jmethodID callbackMethodID);
static jboolean MsOutlookAddrBookContactQuery_onForeachMailUserInContainerTableRow
    (LPUNKNOWN mapiContainer,
    ULONG entryIDByteCount, LPENTRYID entryID, ULONG objType,
    JNIEnv *jniEnv, jstring query, jobject callback, jmethodID callbackMethodID);
LPUNKNOWN MsOutlookAddrBookContactQuery_openEntryId(const char* entryId);
static jbyteArray MsOutlookAddrBookContactQuery_readAttachment
    (LPMESSAGE message, LONG method, ULONG num, JNIEnv *jniEnv, ULONG cond);

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_plugin_addrbook_msoutlook_MsOutlookAddrBookContactQuery_foreachMailUser(
        JNIEnv *jniEnv,
        jclass clazz,
        jstring query,
        jobject callback)
{
    jmethodID callbackMethodID;

    LPMAPISESSION mapiSession = MAPISession_getMapiSession();

    callbackMethodID
        = AddrBookContactQuery_getPtrCallbackMethodID(jniEnv, callback);
    if (!callbackMethodID || jniEnv->ExceptionCheck())
        return;

    if (mapiSession)
    {
        jboolean proceed
            = MsOutlookAddrBookContactQuery_foreachContactInMsgStoresTable(
                    mapiSession,
                    jniEnv,
                    query,
                    callback, callbackMethodID);

        if (proceed && !(jniEnv->ExceptionCheck()))
        {
            MsOutlookAddrBookContactQuery_foreachMailUserInAddressBook(
                    mapiSession,
                    jniEnv,
                    query,
                    callback, callbackMethodID);
        }

    }
}

/**
 * Deletes one property from a contact.
 *
 * @param jniEnv The Java native interface environment.
 * @param clazz A Java class Object.
 * @param propId The outlook property identifier.
 * @param entryId The identifer of the outlook entry to modify.
 *
 * @return JNI_TRUE if the deletion succeded. JNI_FALSE otherwise.
 */
JNIEXPORT jboolean JNICALL
Java_net_java_sip_communicator_plugin_addrbook_msoutlook_MsOutlookAddrBookContactQuery_IMAPIProp_1DeleteProp
  (JNIEnv *jniEnv, jclass clazz, jlong propId, jstring entryId)
{
    const char *nativeEntryId = jniEnv->GetStringUTFChars(entryId, NULL);
    LPUNKNOWN mapiProp;
    if((mapiProp = MsOutlookAddrBookContactQuery_openEntryId(nativeEntryId))
            == NULL)
    {
        return JNI_FALSE;
    }
    jniEnv->ReleaseStringUTFChars(entryId, nativeEntryId);

    ULONG baseGroupEntryIdProp = 0;
    switch(propId)
    {
        case 0x00008083: // dispidEmail1EmailAddress
            baseGroupEntryIdProp = 0x00008080;
            break;
        case 0x00008093: // dispidEmail2EmailAddress
            baseGroupEntryIdProp = 0x00008090;
            break;
        case 0x000080A3: // dispidEmail3EmailAddress
            baseGroupEntryIdProp = 0x000080A0;
            break;
    }
    // If this is a special entry (for email only), then deletes all the
    // corresponding properties to make it work.
    if(baseGroupEntryIdProp != 0)
    {
        ULONG nbProps = 5;
        ULONG propIds[] =
        {
            (baseGroupEntryIdProp + 0), //0x8080 PidLidEmail1DisplayName
            (baseGroupEntryIdProp + 2), // 0x8082 PidLidEmail1AddressType
            (baseGroupEntryIdProp + 3), // 0x8083 PidLidEmail1EmailAddress
            (baseGroupEntryIdProp + 4), // 0x8084 PidLidEmail1OriginalDisplayName
            (baseGroupEntryIdProp + 5)  // 0x8085 PidLidEmail1OriginalEntryID
        };
        ULONG propTag;
        LPSPropTagArray propTagArray;
        MAPIAllocateBuffer(
                CbNewSPropTagArray(nbProps),
                (void **) &propTagArray);
        propTagArray->cValues = nbProps;
        for(unsigned int i = 0; i < nbProps; ++i)
        {
            propTag = MsOutlookAddrBookContactQuery_getPropTagFromLid(
                    (LPMAPIPROP) mapiProp,
                    propIds[i]);
            *(propTagArray->aulPropTag + i) = propTag;
        }

        HRESULT hResult
            = ((LPMAPIPROP)  mapiProp)->DeleteProps(
                    propTagArray,
                    NULL);

        if (HR_SUCCEEDED(hResult))
        {
            hResult
                = ((LPMAPIPROP)  mapiProp)->SaveChanges(
                        FORCE_SAVE | KEEP_OPEN_READWRITE);

            if (HR_SUCCEEDED(hResult))
            {
                MAPIFreeBuffer(propTagArray);
                ((LPMAPIPROP)  mapiProp)->Release();
                return JNI_TRUE;
            }
        }
        MAPIFreeBuffer(propTagArray);
        ((LPMAPIPROP)  mapiProp)->Release();
        return JNI_FALSE;
    }

    SPropTagArray propToDelete;
    propToDelete.cValues = 1;
    propToDelete.aulPropTag[0] = PROP_TAG(PT_UNICODE, propId);

    HRESULT hResult
        = ((LPMAPIPROP)  mapiProp)->DeleteProps(
                (LPSPropTagArray) &propToDelete,
                NULL);

    if (HR_SUCCEEDED(hResult))
    {
        hResult
            = ((LPMAPIPROP)  mapiProp)->SaveChanges(
                    FORCE_SAVE | KEEP_OPEN_READWRITE);

        if (HR_SUCCEEDED(hResult))
        {
            ((LPMAPIPROP)  mapiProp)->Release();
            return JNI_TRUE;
        }
    }
    ((LPMAPIPROP)  mapiProp)->Release();
    return JNI_FALSE;
}

JNIEXPORT jobjectArray JNICALL
Java_net_java_sip_communicator_plugin_addrbook_msoutlook_MsOutlookAddrBookContactQuery_IMAPIProp_1GetProps(
        JNIEnv *jniEnv,
        jclass clazz,
        jlong mapiProp,
        jlongArray propIds,
        jlong flags)
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
            if (jniEnv->ExceptionCheck())
            {
                MAPIFreeBuffer(propTagArray);
                propTagArray = NULL;
                break;
            }
            else
            {
                ULONG propTag;

                if (propId < 0x8000)
                {
                    if (propId == PROP_ID(PR_ATTACHMENT_CONTACTPHOTO))
                        propTag = PR_HASATTACH;
                    else
                        propTag = PROP_TAG(PT_UNSPECIFIED, propId);
                }
                else
                {
                    propTag
                        = MsOutlookAddrBookContactQuery_getPropTagFromLid(
                                (LPMAPIPROP) mapiProp,
                                propId);
                }
                *(propTagArray->aulPropTag + i) = propTag;
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
                        case PT_BOOLEAN:
                        {
                            if ((PR_HASATTACH == prop->ulPropTag)
                                    && prop->Value.b)
                            {
                                jbyteArray value
                                    = MsOutlookAddrBookContactQuery_getAttachmentContactPhoto(
                                            (LPMESSAGE) mapiProp,
                                            jniEnv);

                                if (value)
                                {
                                    jniEnv->SetObjectArrayElement(
                                            props, j,
                                            value);
                                    if (jniEnv->ExceptionCheck())
                                        props = NULL;
                                }
                            }
                            break;
                        }

                        case PT_LONG:
                        {
                            jclass longClass
                                = jniEnv->FindClass("java/lang/Long");

                            if (longClass)
                            {
                                jmethodID longMethodID
                                    = jniEnv->GetMethodID(
                                            longClass,
                                            "<init>", "(J)V");

                                if (longMethodID)
                                {
                                    jlong l = prop->Value.l;
                                    jobject value
                                        = jniEnv->NewObject(
                                                longClass, longMethodID,
                                                l);

                                    if (value)
                                    {
                                        jniEnv->SetObjectArrayElement(
                                                props, j,
                                                value);
                                        if (jniEnv->ExceptionCheck())
                                            props = NULL;
                                    }
                                }
                            }
                            break;
                        }

                        case PT_STRING8:
                        {
                            if (prop->Value.lpszA)
                            {
                                jstring value;

                                value = jniEnv->NewStringUTF(prop->Value.lpszA);
                                if (value)
                                {
                                    jniEnv->SetObjectArrayElement(
                                            props,
                                            j, value);
                                    if (jniEnv->ExceptionCheck())
                                        props = NULL;
                                }
                            }
                            break;
                        }

                        case PT_UNICODE:
                        {
                            if (prop->Value.lpszW)
                            {
                                jstring value;

                                value
                                    = jniEnv->NewString(
                                            (const jchar *) (prop->Value.lpszW),
                                            wcslen(prop->Value.lpszW));
                                if (value)
                                {
                                    jniEnv->SetObjectArrayElement(
                                            props,
                                            j, value);
                                    if (jniEnv->ExceptionCheck())
                                        props = NULL;
                                }
                            }
                            break;
                        }

                        case PT_BINARY:
                        {
                            char entryIdStr[prop->Value.bin.cb * 2 + 1];

                            HexFromBin(
                                    prop->Value.bin.lpb,
                                    prop->Value.bin.cb,
                                    entryIdStr);

                            jstring value;
                            value = jniEnv->NewStringUTF(entryIdStr);
                            if(value)
                            {
                                jniEnv->SetObjectArrayElement(
                                        props,
                                        j, value);
                                if (jniEnv->ExceptionCheck())
                                    props = NULL;
                            }
                            break;
                        }
                        }
                    }
                    propArray++;
                    MAPIFreeBuffer(prop);
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

/**
 * Saves one contact property.
 *
 * @param jniEnv The Java native interface environment.
 * @param clazz A Java class Object.
 * @param propId The outlook property identifier.
 * @param value The value to set to the outlook property.
 * @param entryId The identifer of the outlook entry to modify.
 *
 * @return JNI_TRUE if the modification succeded. JNI_FALSE otherwise.
 */
JNIEXPORT jboolean JNICALL
Java_net_java_sip_communicator_plugin_addrbook_msoutlook_MsOutlookAddrBookContactQuery_IMAPIProp_1SetPropString
  (JNIEnv *jniEnv, jclass clazz, jlong propId, jstring value,
   jstring entryId)
{
    HRESULT hResult;

    const char *nativeEntryId = jniEnv->GetStringUTFChars(entryId, NULL);
    LPUNKNOWN mapiProp;
    if((mapiProp = MsOutlookAddrBookContactQuery_openEntryId(nativeEntryId))
            == NULL)
    {
        return JNI_FALSE;
    }
    jniEnv->ReleaseStringUTFChars(entryId, nativeEntryId);

    const char *nativeValue = jniEnv->GetStringUTFChars(value, NULL);
    size_t valueLength = strlen(nativeValue);
    wchar_t wCharValue[valueLength + 1];
    if(mbstowcs(wCharValue, nativeValue, valueLength + 1)
            != valueLength)
    {
        fprintf(stderr,
                "setPropUnicode (addrbook/MsOutlookAddrBookContactQuery.c): \
                    \n\tmbstowcs\n");
        fflush(stderr);
        jniEnv->ReleaseStringUTFChars(value, nativeValue);
        return JNI_FALSE;
    }
    jniEnv->ReleaseStringUTFChars(value, nativeValue);

    ULONG baseGroupEntryIdProp = 0;
    switch(propId)
    {
        case 0x00008083: // dispidEmail1EmailAddress
            baseGroupEntryIdProp = 0x00008080;
            break;
        case 0x00008093: // dispidEmail2EmailAddress
            baseGroupEntryIdProp = 0x00008090;
            break;
        case 0x000080A3: // dispidEmail3EmailAddress
            baseGroupEntryIdProp = 0x000080A0;
            break;
    }
    // If this is a special entry (for email only), then updates all the
    // corresponding properties to make it work.
    if(baseGroupEntryIdProp != 0)
    {
        ULONG nbProps = 7;
        ULONG propIds[] =
        {
            0x8028, // PidLidAddressBookProviderEmailList
            0x8029, // PidLidAddressBookProviderArrayType
            (baseGroupEntryIdProp + 0), //0x8080 PidLidEmail1DisplayName
            (baseGroupEntryIdProp + 2), // 0x8082 PidLidEmail1AddressType
            (baseGroupEntryIdProp + 3), // 0x8083 PidLidEmail1EmailAddress
            (baseGroupEntryIdProp + 4), // 0x8084 PidLidEmail1OriginalDisplayName
            (baseGroupEntryIdProp + 5)  // 0x8085 PidLidEmail1OriginalEntryID
        };
        ULONG propTag;
        ULONG propCount;
        LPSPropValue propArray;
        LPSPropTagArray propTagArray;
        MAPIAllocateBuffer(
                CbNewSPropTagArray(nbProps),
                (void **) &propTagArray);
        propTagArray->cValues = nbProps;
        for(unsigned int i = 0; i < nbProps; ++i)
        {
            propTag = MsOutlookAddrBookContactQuery_getPropTagFromLid(
                    (LPMAPIPROP) mapiProp,
                    propIds[i]);
            *(propTagArray->aulPropTag + i) = propTag;
        }
        hResult = ((LPMAPIPROP)  mapiProp)->GetProps(
                    propTagArray,
                    MAPI_UNICODE,
                    &propCount,
                    &propArray);

        if(SUCCEEDED(hResult))
        {
            propArray[2].Value.lpszW = wCharValue;
            propArray[4].Value.lpszW = wCharValue;
            propArray[5].Value.lpszW = wCharValue;

            if(SUCCEEDED(hResult))
            {
                hResult = ((LPMAPIPROP)  mapiProp)->SetProps(
                        nbProps,
                        propArray,
                        NULL);
                if(SUCCEEDED(hResult))
                {
                    hResult = ((LPMAPIPROP)  mapiProp)->SaveChanges(
                                FORCE_SAVE | KEEP_OPEN_READWRITE);
                    if (HR_SUCCEEDED(hResult))
                    {
                        MAPIFreeBuffer(propTagArray);
                        ((LPMAPIPROP)  mapiProp)->Release();
                        return JNI_TRUE;
                    }
                }
            }
        }
        MAPIFreeBuffer(propTagArray);
        ((LPMAPIPROP)  mapiProp)->Release();
        return JNI_FALSE;
    }

    SPropValue updateValue;
    updateValue.ulPropTag = PROP_TAG(PT_UNICODE, propId);
    updateValue.Value.lpszW = wCharValue;

    hResult = ((LPMAPIPROP)  mapiProp)->SetProps(
            1,
            (LPSPropValue) &updateValue,
            NULL);

    if (HR_SUCCEEDED(hResult))
    {
        HRESULT hResult
            = ((LPMAPIPROP)  mapiProp)->SaveChanges(
                    FORCE_SAVE | KEEP_OPEN_READWRITE);

        if (HR_SUCCEEDED(hResult))
        {
            ((LPMAPIPROP)  mapiProp)->Release();
            return JNI_TRUE;
        }
    }

    ((LPMAPIPROP)  mapiProp)->Release();
    return JNI_FALSE;
}

static HRESULT
MsOutlookAddrBookContactQuery_HrGetOneProp(
        LPMAPIPROP mapiProp,
        ULONG propTag,
        LPSPropValue *prop)
{
    SPropTagArray propTagArray;
    HRESULT hResult;
    ULONG valueCount;
    LPSPropValue values;

    propTagArray.cValues = 1;
    propTagArray.aulPropTag[0] = propTag;

    hResult = mapiProp->GetProps(&propTagArray, 0, &valueCount, &values);
    if (HR_SUCCEEDED(hResult))
    {
        ULONG i;
        jboolean propHasBeenAssignedTo = JNI_FALSE;

        for (i = 0; i < valueCount; i++)
        {
            LPSPropValue value = values;

            values++;
            if (value->ulPropTag == propTag)
            {
                *prop = value;
                propHasBeenAssignedTo = JNI_TRUE;
            }
            else
                MAPIFreeBuffer(value);
        }
        if (!propHasBeenAssignedTo)
            hResult = MAPI_E_NOT_FOUND;
    }
    return hResult;
}

static jboolean
MsOutlookAddrBookContactQuery_foreachContactInMsgStoresTable
    (LPMAPISESSION mapiSession,
    JNIEnv *jniEnv, jstring query, jobject callback, jmethodID callbackMethodID)
{
    HRESULT hResult;
    LPMAPITABLE msgStoresTable = NULL;
    jboolean proceed;

    hResult = mapiSession->GetMsgStoresTable(0, &msgStoresTable);
    if (HR_SUCCEEDED(hResult) && msgStoresTable)
    {
        proceed
            = MsOutlookAddrBookContactQuery_foreachRowInTable(
                    msgStoresTable,
                    MsOutlookAddrBookContactQuery_onForeachContactInMsgStoresTableRow,
                    (LPUNKNOWN) mapiSession,
                    jniEnv, query, callback, callbackMethodID);
        msgStoresTable->Release();
    }
    else
    {
        MsOutlookMAPIHResultException_throwNew(
                jniEnv,
                hResult,
                __FILE__, __LINE__);
        proceed = JNI_TRUE;
    }
    return proceed;
}

static jboolean
MsOutlookAddrBookContactQuery_foreachMailUser
    (ULONG objType, LPUNKNOWN iUnknown,
    JNIEnv *jniEnv, jstring query, jobject callback, jmethodID callbackMethodID)
{
    jboolean proceed = JNI_TRUE;

    switch (objType)
    {
    case MAPI_ABCONT:
    case MAPI_FOLDER:
    {
        LPMAPICONTAINER mapiContainer = (LPMAPICONTAINER) iUnknown;

        HRESULT hResult;
        LPMAPITABLE mapiTable;

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
        if (proceed)
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
    case MAPI_MESSAGE:
    {
        if (MsOutlookAddrBookContactQuery_mailUserMatches(
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
            if (proceed && jniEnv->ExceptionCheck())
                proceed = JNI_FALSE;
        }
        break;
    }
    }
    return proceed;
}

static jboolean
MsOutlookAddrBookContactQuery_foreachMailUserInAddressBook
    (LPMAPISESSION mapiSession,
    JNIEnv *jniEnv, jstring query, jobject callback, jmethodID callbackMethodID)
{
    HRESULT hResult;
    LPADRBOOK adrBook;
    jboolean proceed;

    hResult = mapiSession->OpenAddressBook(0, NULL, AB_NO_DIALOG, &adrBook);
    if (HR_SUCCEEDED(hResult))
    {
        ULONG objType;
        LPUNKNOWN iUnknown;

        hResult = adrBook->OpenEntry(
                0,
                NULL,
                NULL,
                MsOutlookAddrBookContactQuery_openEntryUlFlags,
                &objType,
                &iUnknown);

        if (HR_SUCCEEDED(hResult))
        {
            proceed
                = MsOutlookAddrBookContactQuery_foreachMailUser(
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
            proceed = JNI_TRUE;
        }

        adrBook->Release();
    }
    else
    {
        MsOutlookMAPIHResultException_throwNew(
                jniEnv,
                hResult,
                __FILE__, __LINE__);
        proceed = JNI_TRUE;
    }
    return proceed;
}

static jboolean
MsOutlookAddrBookContactQuery_foreachMailUserInContainerTable
    (LPMAPICONTAINER mapiContainer, LPMAPITABLE mapiTable,
    JNIEnv *jniEnv, jstring query, jobject callback, jmethodID callbackMethodID)
{
    return
        MsOutlookAddrBookContactQuery_foreachRowInTable(
            mapiTable,
            MsOutlookAddrBookContactQuery_onForeachMailUserInContainerTableRow,
            (LPUNKNOWN) mapiContainer,
            jniEnv, query, callback, callbackMethodID);
}

static jboolean
MsOutlookAddrBookContactQuery_foreachRowInTable
    (LPMAPITABLE mapiTable,
    MsOutlookAddrBookContactQuery_ForeachRowInTableCallback rowCallback,
    LPUNKNOWN iUnknown,
    JNIEnv *jniEnv, jstring query, jobject callback, jmethodID callbackMethodID)
{
    HRESULT hResult;
    jboolean proceed;

    hResult = mapiTable->SeekRow(BOOKMARK_BEGINNING, 0, NULL);
    if (HR_SUCCEEDED(hResult))
    {
        proceed = JNI_TRUE;
        while (proceed)
        {
            LPSRowSet rows;

            hResult = mapiTable->QueryRows(1, 0, &rows);
            if (HR_FAILED(hResult))
                break;

            if (rows->cRows == 1)
            {
                ULONG i;
                LPSRow row = rows->aRow;
                ULONG objType = 0;
                SBinary entryIDBinary = { 0, NULL };

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
                        CopyMemory(
                            entryID,
                            entryIDBinary.lpb,
                            entryIDBinary.cb);

                        /*
                         * We no longer need the rows at this point so free them
                         * before we drill down the hierarchy and allocate even more rows.
                         */
                        MsOutlookAddrBookContactQuery_freeSRowSet(rows);

                        proceed
                            = rowCallback(
                                    iUnknown,
                                    entryIDBinary.cb, entryID, objType,
                                    jniEnv, query, callback, callbackMethodID);

                        MAPIFreeBuffer(entryID);

                        if (proceed && jniEnv->ExceptionCheck())
                            proceed = JNI_FALSE;
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

static jbyteArray
MsOutlookAddrBookContactQuery_getAttachmentContactPhoto
    (LPMESSAGE message, JNIEnv *jniEnv)
{
    HRESULT hResult;
    LPMAPITABLE attachmentTable;
    jbyteArray attachmentContactPhoto = NULL;

    hResult = message->GetAttachmentTable(0, &attachmentTable);
    if (HR_SUCCEEDED(hResult))
    {
        hResult = attachmentTable->SeekRow(BOOKMARK_BEGINNING, 0, NULL);
        if (HR_SUCCEEDED(hResult))
        {
            while (1)
            {
                LPSRowSet rows;

                hResult = attachmentTable->QueryRows(1, 0, &rows);
                if (HR_FAILED(hResult))
                    break;

                if (rows->cRows == 1)
                {
                    ULONG i;
                    LPSRow row = rows->aRow;
                    jboolean isAttachmentContactPhotoRow = JNI_FALSE;
                    jboolean hasAttachmentContactPhoto = JNI_FALSE;
                    ULONG attachNum = 0;
                    LONG attachMethod = NO_ATTACHMENT;

                    for (i = 0; i < row->cValues; i++)
                    {
                        LPSPropValue prop = (row->lpProps) + i;

                        switch (prop->ulPropTag)
                        {
                        case PR_ATTACHMENT_CONTACTPHOTO:
                            isAttachmentContactPhotoRow = JNI_TRUE;
                            hasAttachmentContactPhoto
                                = prop->Value.b ? JNI_TRUE : JNI_FALSE;
                            break;
                        case PR_ATTACH_METHOD:
                            attachMethod = prop->Value.l;
                            break;
                        case PR_ATTACH_NUM:
                            attachNum = prop->Value.l;
                            break;
                        }
                    }

                    MsOutlookAddrBookContactQuery_freeSRowSet(rows);

                    /*
                     * As the reference says and as discovered in practice,
                     * PR_ATTACHMENT_CONTACTPHOTO is sometimes in IAttach.
                     */
                    if ((isAttachmentContactPhotoRow
                                && hasAttachmentContactPhoto)
                            || !isAttachmentContactPhotoRow)
                    {
                        attachmentContactPhoto
                            = MsOutlookAddrBookContactQuery_readAttachment(
                                    message,
                                    attachMethod, attachNum,
                                    jniEnv,
                                    (!isAttachmentContactPhotoRow)
                                        ? PR_ATTACHMENT_CONTACTPHOTO
                                        : PROP_TAG(PT_UNSPECIFIED, 0));
                    }
                    if (isAttachmentContactPhotoRow
                            || attachmentContactPhoto)
                    {
                        /*
                         * The reference says there can be only 1
                         * PR_ATTACHMENT_CONTACTPHOTO.
                         */
                        break;
                    }
                }
                else
                {
                    MAPIFreeBuffer(rows);
                    break;
                }
            }
        }

        attachmentTable->Release();
    }
    return attachmentContactPhoto;
}

static HRESULT
MsOutlookAddrBookContactQuery_getContactsFolderEntryID
    (LPMDB msgStore,
    ULONG folderEntryIDByteCount, LPENTRYID folderEntryID,
    ULONG *contactsFolderEntryIDByteCount, LPENTRYID *contactsFolderEntryID)
{
    HRESULT hResult;
    ULONG objType;
    LPUNKNOWN folder;

    hResult = msgStore->OpenEntry(
            folderEntryIDByteCount,
            folderEntryID,
            NULL,
            MsOutlookAddrBookContactQuery_openEntryUlFlags,
            &objType,
            &folder);

    if (HR_SUCCEEDED(hResult))
    {
        LPSPropValue prop;

        hResult
            = MsOutlookAddrBookContactQuery_HrGetOneProp(
                    (LPMAPIPROP) folder,
                    0x36D10102 /* PR_IPM_CONTACT_ENTRYID */,
                    &prop);
        if (HR_SUCCEEDED(hResult))
        {
            LPSBinary bin = &(prop->Value.bin);
            if (S_OK
                    == MAPIAllocateBuffer(
                            bin->cb,
                            (void **) contactsFolderEntryID))
            {
                hResult = S_OK;
                *contactsFolderEntryIDByteCount = bin->cb;
                CopyMemory(*contactsFolderEntryID, bin->lpb, bin->cb);
            }
            else
                hResult = MAPI_E_NOT_ENOUGH_MEMORY;
            MAPIFreeBuffer(prop);
        }
        folder->Release();
    }
    return hResult;
}

static ULONG
MsOutlookAddrBookContactQuery_getPropTagFromLid(LPMAPIPROP mapiProp, LONG lid)
{
    GUID PSETID_Address
        = {0x00062004, 0x0000, 0x0000, {0xC0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x46}};
    MAPINAMEID propName;
    LPMAPINAMEID propNamePtr;
    HRESULT hResult;
    LPSPropTagArray propTagArray;

    propName.lpguid = (LPGUID) &PSETID_Address;
    propName.ulKind = MNID_ID;
    propName.Kind.lID = lid;
    propNamePtr = &propName;
    hResult
        = mapiProp->GetIDsFromNames(
                1, &propNamePtr,
                0,
                &propTagArray);
    if (HR_SUCCEEDED(hResult) && (1 == propTagArray->cValues))
    {
        ULONG propTag = propTagArray->aulPropTag[0];

        if (PT_ERROR == PROP_TYPE(propTag))
            propTag = PROP_TAG(PT_UNSPECIFIED, lid);
        MAPIFreeBuffer(propTagArray);
        return propTag;
    }
    else
        return PROP_TAG(PT_UNSPECIFIED, lid);
}

static jboolean
MsOutlookAddrBookContactQuery_mailUserMatches
    (LPMAPIPROP mailUser, JNIEnv *jniEnv, jstring query)
{
    // TODO Auto-generated method stub
    return JNI_TRUE;
}

static jboolean
MsOutlookAddrBookContactQuery_onForeachContactInMsgStoresTableRow
    (LPUNKNOWN mapiSession,
    ULONG entryIDByteCount, LPENTRYID entryID, ULONG objType,
    JNIEnv *jniEnv, jstring query, jobject callback, jmethodID callbackMethodID)
{
    HRESULT hResult;
    LPMDB msgStore;
    jboolean proceed;

    hResult
        = ((LPMAPISESSION) mapiSession)->OpenMsgStore(
                0,
                entryIDByteCount, entryID,
                NULL,
                MDB_NO_MAIL | MsOutlookAddrBookContactQuery_openEntryUlFlags,
                &msgStore);
    if (HR_SUCCEEDED(hResult))
    {
        LPENTRYID receiveFolderEntryID;
        ULONG contactsFolderEntryIDByteCount;
        LPENTRYID contactsFolderEntryID;

        hResult
            = msgStore->GetReceiveFolder(
                    NULL,
                    0,
                    &entryIDByteCount, &receiveFolderEntryID,
                    NULL);
        if (HR_SUCCEEDED(hResult))
        {
            hResult
                = MsOutlookAddrBookContactQuery_getContactsFolderEntryID(
                        msgStore,
                        entryIDByteCount, receiveFolderEntryID,
                        &contactsFolderEntryIDByteCount, &contactsFolderEntryID);
            MAPIFreeBuffer(receiveFolderEntryID);
        }
        if (HR_FAILED(hResult))
        {
            hResult
                = MsOutlookAddrBookContactQuery_getContactsFolderEntryID(
                        msgStore,
                        0, NULL,
                        &contactsFolderEntryIDByteCount, &contactsFolderEntryID);
        }
        if (HR_SUCCEEDED(hResult))
        {
            ULONG contactsFolderObjType;
            LPUNKNOWN contactsFolder;

            hResult
                = msgStore->OpenEntry(
                    contactsFolderEntryIDByteCount, contactsFolderEntryID,
                    NULL,
                    MsOutlookAddrBookContactQuery_openEntryUlFlags,
                    &contactsFolderObjType, &contactsFolder);
            if (HR_SUCCEEDED(hResult))
            {
                proceed
                    = MsOutlookAddrBookContactQuery_foreachMailUser(
                            contactsFolderObjType, contactsFolder,
                            jniEnv, query, callback, callbackMethodID);
                contactsFolder->Release();
            }
            else
            {
                /*
                 * We've failed but other parts of the hierarchy may still
                 * succeed.
                 */
                proceed = JNI_TRUE;
            }
            MAPIFreeBuffer(contactsFolderEntryID);
        }
        else
        {
            /*
             * We've failed but other parts of the hierarchy may still succeed.
             */
            proceed = JNI_TRUE;
        }
        msgStore->Release();
    }
    else
    {
        /* We've failed but other parts of the hierarchy may still succeed. */
        proceed = JNI_TRUE;
    }
    return proceed;
}

static jboolean
MsOutlookAddrBookContactQuery_onForeachMailUserInContainerTableRow
    (LPUNKNOWN mapiContainer,
    ULONG entryIDByteCount, LPENTRYID entryID, ULONG objType,
    JNIEnv *jniEnv, jstring query, jobject callback, jmethodID callbackMethodID)
{
    HRESULT hResult;
    LPUNKNOWN iUnknown;
    jboolean proceed;

    // Make write failed and image load.
    hResult
        = ((LPMAPICONTAINER) mapiContainer)->OpenEntry(
                entryIDByteCount, entryID,
                NULL,
                MsOutlookAddrBookContactQuery_openEntryUlFlags,
                &objType, &iUnknown);
    if (HR_SUCCEEDED(hResult))
    {
        proceed
            = MsOutlookAddrBookContactQuery_foreachMailUser(
                    objType, iUnknown,
                    jniEnv, query, callback, callbackMethodID);
        iUnknown->Release();
    }
    else
    {
        /* We've failed but other parts of the hierarchy may still succeed. */
        proceed = JNI_TRUE;
    }
    return proceed;
}

/**
 * Opens an object based on the string representation of its entry id.
 *
 * @param entryId The identifier of the entry to open.
 *
 * @return A pointer to the opened entry. NULL if anything goes wrong.
 */
LPUNKNOWN MsOutlookAddrBookContactQuery_openEntryId(const char* entryId)
{
    ULONG tmpEntryIdSize = strlen(entryId) / 2;
    LPENTRYID tmpEntryId = (LPENTRYID) malloc(tmpEntryIdSize * sizeof(char));
    if(FBinFromHex((LPSTR) entryId, (LPBYTE) tmpEntryId))
    {
        LPMAPISESSION mapiSession = MAPISession_getMapiSession();
        ULONG objType;
        LPUNKNOWN iUnknown;
        HRESULT hResult = mapiSession->OpenEntry(
                tmpEntryIdSize,
                tmpEntryId,
                NULL,
                MAPI_BEST_ACCESS,
                &objType,
                &iUnknown);
        if(hResult == S_OK)
        {
            free(tmpEntryId);
            return iUnknown;
        }
    }
    free(tmpEntryId);
    return NULL;
}

static jbyteArray
MsOutlookAddrBookContactQuery_readAttachment
    (LPMESSAGE message, LONG method, ULONG num, JNIEnv *jniEnv, ULONG cond)
{
    jbyteArray attachment = NULL;

    if (ATTACH_BY_VALUE == method)
    {
        HRESULT hResult;
        LPATTACH attach;

        hResult = message->OpenAttach(num, NULL, 0, &attach);
        if (HR_SUCCEEDED(hResult))
        {
            IStream *stream;

            if (PT_BOOLEAN == PROP_TYPE(cond))
            {
                LPSPropValue condValue;

                hResult = MsOutlookAddrBookContactQuery_HrGetOneProp(
                        (LPMAPIPROP) attach,
                        cond,
                        &condValue);
                if (HR_SUCCEEDED(hResult))
                {
                    if ((PT_BOOLEAN != PROP_TYPE(condValue->ulPropTag))
                            || !(condValue->Value.b))
                        hResult = MAPI_E_NOT_FOUND;
                    MAPIFreeBuffer(condValue);
                }
            }

            if (HR_SUCCEEDED(hResult))
            {
                hResult
                    = ((LPMAPIPROP) attach)->OpenProperty(
                            PR_ATTACH_DATA_BIN,
                            &IID_IStream, 0,
                            0,
                            (LPUNKNOWN *) &stream);
            }
            if (HR_SUCCEEDED(hResult))
            {
                STATSTG statstg;
                ULONGLONG length;

                hResult = stream->Stat(&statstg, STATFLAG_NONAME);
                if ((S_OK == hResult) && ((length = statstg.cbSize.QuadPart)))
                {
                    attachment = jniEnv->NewByteArray((jsize) length);
                    if (attachment)
                    {
                        jbyte *bytes
                            = jniEnv->GetByteArrayElements(attachment, NULL);

                        if (bytes)
                        {
                            ULONG read;
                            jint mode;

                            hResult
                                = stream->Read(bytes, (ULONG) length, &read);
                            mode
                                = ((S_OK == hResult) || (S_FALSE == hResult))
                                    ? 0
                                    : JNI_ABORT;
                            jniEnv->ReleaseByteArrayElements(
                                    attachment, bytes,
                                    mode);
                            if (0 != mode)
                                attachment = NULL;
                        }
                    }
                }

                stream->Release();
            }

            attach->Release();
        }
    }
    return attachment;
}
