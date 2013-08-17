/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#include "MsOutlookAddrBookContactQuery.h"

#include "MsOutlookAddrBookContactSourceService.h"

#include "MAPIBitness.h"
#include "MAPISession.h"

#include <initguid.h>
#include <jni.h>
#include <Mapidefs.h>
#include <Mapix.h>
#include <windows.h>

#define BODY_ENCODING_TEXT_AND_HTML	((ULONG) 0x00100000)
#define DELETE_HARD_DELETE          ((ULONG) 0x00000010)
#define ENCODING_PREFERENCE			((ULONG) 0x00020000)
#define ENCODING_MIME				((ULONG) 0x00040000)
#define OOP_DONT_LOOKUP             ((ULONG) 0x10000000)
#define PR_ATTACHMENT_CONTACTPHOTO PROP_TAG(PT_BOOLEAN, 0x7FFF)

DEFINE_OLEGUID(PSETID_Address, MAKELONG(0x2000+(0x04),0x0006),0,0);

const MAPIUID MsOutlookAddrBookContactQuery_MuidOneOffEntryID =
{{
    0x81, 0x2b, 0x1f, 0xa4,     0xbe, 0xa3, 0x10, 0x19,
    0x9d, 0x6e, 0x00, 0xdd,     0x01, 0x0f, 0x54, 0x02
}};

typedef struct MsOutlookAddrBookContactQuery_OneOffEntryID
{
	ULONG	ulFlags;
	MAPIUID muid;
	ULONG   ulBitMask;
	BYTE    bData[];
} ONEOFFENTRYID;

typedef UNALIGNED ONEOFFENTRYID *MsOutlookAddrBookContactQuery_LPONEOFFENTRYID;

typedef
    jboolean (*MsOutlookAddrBookContactQuery_ForeachRowInTableCallback)
        (LPUNKNOWN iUnknown,
        ULONG entryIDByteCount, LPENTRYID entryID, ULONG objType,
        const char * query, void * callback, void * callbackObject);

static ULONG MsOutlookAddrBookContactQuery_openEntryUlFlags = MAPI_BEST_ACCESS;

static HRESULT MsOutlookAddrBookContactQuery_HrGetOneProp
    (LPMAPIPROP mapiProp, ULONG propTag, LPSPropValue *prop);

HRESULT MsOutlookAddrBookContactQuery_buildOneOff
    (LPWSTR displayName, LPWSTR addressType, LPWSTR emailAddress,
     ULONG* oneOffEntryIdLength, LPBYTE* oneOffEntryId);
HRESULT MsOutlookAddrBookContactQuery_createEmailAddress
    (LPMESSAGE contact, LPWSTR displayName, LPWSTR addressType,
     LPWSTR emailAddress, LPWSTR originalDisplayName, LONG providerEmailList[],
     LONG providerArrayType, ULONG propIds[], int nbPropId);
static jboolean MsOutlookAddrBookContactQuery_foreachContactInMsgStoresTable
    (LPMAPISESSION mapiSession, const char * query,
     void * callback, void * callbackObject);
static jboolean MsOutlookAddrBookContactQuery_foreachMailUser
    (ULONG objType, LPUNKNOWN iUnknown,
     const char * query, void * callback, void * callbackObject);
static jboolean MsOutlookAddrBookContactQuery_foreachMailUserInContainerTable
    (LPMAPICONTAINER mapiContainer, LPMAPITABLE mapiTable,
    const char * query, void * callback, void * callbackObject);
static jboolean MsOutlookAddrBookContactQuery_foreachRowInTable
    (LPMAPITABLE mapiTable,
    MsOutlookAddrBookContactQuery_ForeachRowInTableCallback rowCallback,
    LPUNKNOWN iUnknown,
    const char * query, void * callback, void * callbackObject);
static void MsOutlookAddrBookContactQuery_freeSRowSet(LPSRowSet rows);
static void* MsOutlookAddrBookContactQuery_getAttachmentContactPhoto
    (LPMESSAGE message, ULONGLONG * length);
void MsOutlookAddrBookContactQuery_getBinaryProp
    (LPMAPIPROP entry, ULONG propId, LPSBinary binaryProp);
static HRESULT MsOutlookAddrBookContactQuery_getContactsFolderEntryID
    (LPMDB msgStore,
    ULONG folderEntryIDByteCount, LPENTRYID folderEntryID,
    ULONG *contactsFolderEntryIDByteCount, LPENTRYID *contactsFolderEntryID);
LPSTR MsOutlookAddrBookContactQuery_getContactId(LPMAPIPROP contact);
LPMAPIFOLDER MsOutlookAddrBookContactQuery_getDefaultContactFolderId(void);
LPMDB MsOutlookAddrBookContactQuery_getDefaultMsgStores(void);
ULONG MsOutlookAddrBookContactQuery_getPropTag
    (LPMAPIPROP mapiProp, long propId, long propType);
static ULONG MsOutlookAddrBookContactQuery_getPropTagFromLid
    (LPMAPIPROP mapiProp, LONG lid);
static jboolean MsOutlookAddrBookContactQuery_mailUserMatches
    (LPMAPIPROP mailUser, const char * query);
static jboolean MsOutlookAddrBookContactQuery_onForeachContactInMsgStoresTableRow
    (LPUNKNOWN mapiSession,
    ULONG entryIDByteCount, LPENTRYID entryID, ULONG objType,
    const char * query, void * callback, void * callbackObject);
static jboolean MsOutlookAddrBookContactQuery_onForeachMailUserInContainerTableRow
    (LPUNKNOWN mapiContainer,
    ULONG entryIDByteCount, LPENTRYID entryID, ULONG objType,
    const char * query, void * callback, void * callbackObject);
LPUNKNOWN MsOutlookAddrBookContactQuery_openEntryId
    (ULONG entryIdSize, LPENTRYID entryId);
LPUNKNOWN MsOutlookAddrBookContactQuery_openEntryIdStr(const char* entryId);
static void* MsOutlookAddrBookContactQuery_readAttachment
    (LPMESSAGE message, LONG method, ULONG num, ULONG cond, ULONGLONG * length);

/**
 * Creates a One-Off entry to register an email address.
 *
 * @param displayName The display name corresponding to the new email address.
 * @param addressType The address type corresponding to the new email address
 * (SMTP).
 * @param emailAddress The email address string.
 * @param oneOffEntryIdLength The length of the One-Off entry id once created.
 * @param oneOffEntriId Pointer used to store the One-Off entry id created.
 *
 * @return S_OK if everything was fine. MAPI_E_INVALID_PARAMETER, if there was
 * an invalid argument. MAPI_E_CALL_FAILED otherwise.
 */
HRESULT MsOutlookAddrBookContactQuery_buildOneOff
    (LPWSTR displayName, LPWSTR addressType, LPWSTR emailAddress,
     ULONG* oneOffEntryIdLength, LPBYTE* oneOffEntryId)
{
	if (!displayName || !addressType || !emailAddress
            || !oneOffEntryIdLength || !oneOffEntryId)
    {
        return MAPI_E_INVALID_PARAMETER;
    }

	// Calculate how large our EID will be
	size_t cbDisplayName
        = wcslen(displayName) * sizeof(WCHAR) + sizeof(WCHAR);
	size_t cbAddressType
        = wcslen(addressType) * sizeof(WCHAR) + sizeof(WCHAR);
	size_t cbEmailAddress
        = wcslen(emailAddress) * sizeof(WCHAR) + sizeof(WCHAR);
	size_t cbEID = sizeof(ONEOFFENTRYID)
        + cbDisplayName + cbAddressType + cbEmailAddress;

	// Allocate our buffer
	MsOutlookAddrBookContactQuery_LPONEOFFENTRYID lpEID
        = (MsOutlookAddrBookContactQuery_LPONEOFFENTRYID)
            malloc(cbEID * sizeof(BYTE));

	// Populate it
	if (lpEID)
	{
		memset(lpEID,0,cbEID);
		lpEID->muid = MsOutlookAddrBookContactQuery_MuidOneOffEntryID;
		lpEID->ulBitMask |= MAPI_UNICODE; // Set U, the unicode bit
		lpEID->ulBitMask |= OOP_DONT_LOOKUP; // Set L, the no lookup bit
		lpEID->ulBitMask |= MAPI_SEND_NO_RICH_INFO; // Set M, the mime bit
        // Set the encoding format
		lpEID->ulBitMask |=
            ENCODING_PREFERENCE | ENCODING_MIME | BODY_ENCODING_TEXT_AND_HTML;

		LPBYTE pb = lpEID->bData;
		// this will copy the string and the NULL terminator together
		memcpy(pb, displayName, cbDisplayName);
		pb += cbDisplayName;
		memcpy(pb, addressType, cbAddressType);
		pb += cbAddressType;
		memcpy(pb, emailAddress, cbEmailAddress);
		pb += cbEmailAddress;

		// Return it
		*oneOffEntryIdLength = cbEID;
		*oneOffEntryId = (LPBYTE) lpEID;

		return S_OK;
	}

	return MAPI_E_CALL_FAILED;
}

/**
 * Creates a new contact from the outlook database.
 *
 * @return The identifer of the created outlook contact. NULL on failure.
 */
char* MsOutlookAddrBookContactQuery_createContact(void)
{
    char* messageIdStr = NULL;

    LPMAPIFOLDER parentEntry
        = MsOutlookAddrBookContactQuery_getDefaultContactFolderId();

    LPMESSAGE message;
    if(parentEntry->CreateMessage(NULL, 0, &message) == S_OK)
    {
        SPropValue updateValue;

        // PR_MESSAGE_CLASS_W
        updateValue.ulPropTag = PROP_TAG(PT_UNICODE, 0x001A); 
        updateValue.Value.lpszW = (LPWSTR) L"IPM.Contact";
        if(((LPMAPIPROP)  message)->SetProps(
                    1,
                    (LPSPropValue) &updateValue,
                    NULL) == S_OK)
        {
            ((LPMAPIPROP) message)->SaveChanges(
                FORCE_SAVE | KEEP_OPEN_READWRITE);
        }

        updateValue.ulPropTag = PROP_TAG(PT_LONG, 0x1080); // PR_ICON_INDEX
        updateValue.Value.l = 512;
        if(((LPMAPIPROP)  message)->SetProps(
                    1,
                    (LPSPropValue) &updateValue,
                    NULL) == S_OK)
        {
            ((LPMAPIPROP) message)->SaveChanges(
                FORCE_SAVE | KEEP_OPEN_READWRITE);
        }

        messageIdStr
            = MsOutlookAddrBookContactQuery_getContactId((LPMAPIPROP) message);

        ((LPMAPIPROP)  message)->Release();
    }
    parentEntry->Release();

    return messageIdStr;
}

/**
 * Creates or modifies an email address.
 *
 * @param contact The contact to add the email address.
 * @param displayName The display name for the email address.
 * @param addressType the address type for the email address (SMTP).
 * @param emailAddress The email address.
 * @param originalDisplayName The original display name for the email address.
 * @param providerEmailList A list of values used to define which email address
 * is set.
 * @param providerArrayType A bitsmask used to define which email address is
 * set.
 * @param propIds A list of property to set for this email address.
 * @param nbPropId The number of properties contained in propIds.
 *
 * @return S_OK if the email address was created/modified. 
 */
HRESULT MsOutlookAddrBookContactQuery_createEmailAddress
    (LPMESSAGE contact, LPWSTR displayName, LPWSTR addressType,
     LPWSTR emailAddress, LPWSTR originalDisplayName, LONG providerEmailList[],
     LONG providerArrayType, ULONG propIds[], int nbPropId)
{
    SBinary parentId;
    parentId.cb = 0;
    MsOutlookAddrBookContactQuery_getBinaryProp(
            (LPMAPIPROP) contact,
            0x0E09, //PR_PARENT_ENTRYID,
            &parentId);
    LPMAPIFOLDER parentEntry
        = (LPMAPIFOLDER) MsOutlookAddrBookContactQuery_openEntryId(
                parentId.cb, (LPENTRYID) parentId.lpb);
	HRESULT hRes = S_OK;
    MAPINAMEID  rgnmid[nbPropId];
    LPMAPINAMEID rgpnmid[nbPropId];
    LPSPropTagArray lpNamedPropTags = NULL;

    for(int i = 0 ; i < nbPropId ; i++)
    {
        rgnmid[i].lpguid = (LPGUID) &PSETID_Address;
        rgnmid[i].ulKind = MNID_ID;
        rgnmid[i].Kind.lID = propIds[i];
        rgpnmid[i] = &rgnmid[i];
    }
    hRes = parentEntry->GetIDsFromNames(
            nbPropId,
            (LPMAPINAMEID*) &rgpnmid,
            0,
            &lpNamedPropTags);

    if (SUCCEEDED(hRes) && lpNamedPropTags)
    {
        SPropValue spvProps[nbPropId];
        spvProps[0].ulPropTag
            = CHANGE_PROP_TYPE(lpNamedPropTags->aulPropTag[0], PT_MV_LONG);
        spvProps[1].ulPropTag
            = CHANGE_PROP_TYPE(lpNamedPropTags->aulPropTag[1], PT_LONG);
        spvProps[2].ulPropTag
            = CHANGE_PROP_TYPE(lpNamedPropTags->aulPropTag[2], PT_UNICODE);
        spvProps[3].ulPropTag
            = CHANGE_PROP_TYPE(lpNamedPropTags->aulPropTag[3], PT_UNICODE);
        spvProps[4].ulPropTag
            = CHANGE_PROP_TYPE(lpNamedPropTags->aulPropTag[4], PT_UNICODE);
        spvProps[5].ulPropTag
            = CHANGE_PROP_TYPE(lpNamedPropTags->aulPropTag[5], PT_UNICODE);
        spvProps[6].ulPropTag
            = CHANGE_PROP_TYPE(lpNamedPropTags->aulPropTag[6], PT_BINARY);

        spvProps[0].Value.MVl.cValues = 1;
        spvProps[0].Value.MVl.lpl = providerEmailList;

        spvProps[1].Value.l = providerArrayType;

        spvProps[2].Value.lpszW = displayName;
        spvProps[3].Value.lpszW = addressType;
        spvProps[4].Value.lpszW = emailAddress;
        spvProps[5].Value.lpszW = originalDisplayName;

        hRes = MsOutlookAddrBookContactQuery_buildOneOff(
                displayName,
                addressType,
                emailAddress,
                &spvProps[6].Value.bin.cb,
                &spvProps[6].Value.bin.lpb);

        if (SUCCEEDED(hRes))
        {
            hRes = contact->SetProps(nbPropId, spvProps, NULL);
            if (SUCCEEDED(hRes))
            {
                hRes = contact->SaveChanges(FORCE_SAVE);
            }
        }

        if (spvProps[6].Value.bin.lpb)
        {
            free(spvProps[6].Value.bin.lpb);
        }
        MAPIFreeBuffer(lpNamedPropTags);
    }

    MAPIFreeBuffer(parentId.lpb);
    parentEntry->Release();

	return hRes;
}

/**
 * Delete the given contact from the outlook database.
 *
 * @param nativeEntryId The identifer of the outlook contact to remove.
 *
 * @return 1 if the deletion succeded. 0 otherwise.
 */
int MsOutlookAddrBookContactQuery_deleteContact(const char * nativeEntryId)
{
    int res = 0;

    LPUNKNOWN mapiProp;
    if((mapiProp = MsOutlookAddrBookContactQuery_openEntryIdStr(nativeEntryId))
            == NULL)
    {
        return 0;
    }

    SBinary contactId;
    contactId.cb = 0;
    MsOutlookAddrBookContactQuery_getBinaryProp(
            (LPMAPIPROP) mapiProp,
            0x0FFF,
            &contactId);

    SBinary parentId;
    parentId.cb = 0;
    MsOutlookAddrBookContactQuery_getBinaryProp(
            (LPMAPIPROP) mapiProp,
            0x0E09, //PR_PARENT_ENTRYID,
            &parentId);
    LPUNKNOWN parentEntry = MsOutlookAddrBookContactQuery_openEntryId(
            parentId.cb, (LPENTRYID) parentId.lpb);

    SBinaryArray deleteIdArray;
    deleteIdArray.cValues = 1;
    deleteIdArray.lpbin = &contactId;
    res = (((LPMAPIFOLDER) parentEntry)->DeleteMessages(
                &deleteIdArray,
                0,
                NULL,
                DELETE_HARD_DELETE) == S_OK);

    ((LPMAPIPROP)  parentEntry)->Release();
    MAPIFreeBuffer(parentId.lpb);
    MAPIFreeBuffer(contactId.lpb);
    ((LPMAPIPROP)  mapiProp)->Release();

    return res;
}

void MsOutlookAddrBookContactQuery_foreachMailUser(
        const char * query, void * callback, void * callbackObject)
{
    MAPISession_lock();
    LPMAPISESSION mapiSession = MAPISession_getMapiSession();
    if (!mapiSession)
    {
        MAPISession_unlock();
        return;
    }

    MsOutlookAddrBookContactQuery_foreachContactInMsgStoresTable(
            mapiSession,
            query,
            callback,
            callbackObject);

    MAPISession_unlock();
}

static jboolean
MsOutlookAddrBookContactQuery_foreachContactInMsgStoresTable
    (LPMAPISESSION mapiSession,
    const char * query, void * callback, void * callbackObject)
{
    HRESULT hResult;
    LPMAPITABLE msgStoresTable = NULL;
    jboolean proceed = JNI_FALSE;

    hResult = mapiSession->GetMsgStoresTable(0, &msgStoresTable);
    if (HR_SUCCEEDED(hResult) && msgStoresTable)
    {
        proceed
            = MsOutlookAddrBookContactQuery_foreachRowInTable(
                    msgStoresTable,
                    MsOutlookAddrBookContactQuery_onForeachContactInMsgStoresTableRow,
                    (LPUNKNOWN) mapiSession,
                    query, callback, callbackObject);
        msgStoresTable->Release();
    }

    return proceed;
}

static jboolean
MsOutlookAddrBookContactQuery_foreachMailUser
    (ULONG objType, LPUNKNOWN iUnknown,
    const char * query, void * callback, void * callbackObject)
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
                        query, callback, callbackObject);
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
                            query, callback, callbackObject);
                mapiTable->Release();
            }
        }

        break;
    }

    case MAPI_MAILUSER:
    case MAPI_MESSAGE:
    {
        if (MsOutlookAddrBookContactQuery_mailUserMatches(
                    (LPMAPIPROP) iUnknown, query))
        {
            LPSTR contactId = MsOutlookAddrBookContactQuery_getContactId(
                    (LPMAPIPROP) iUnknown);

            boolean(*cb)(LPSTR, void*) = (boolean(*)(LPSTR, void*)) callback;
            proceed = cb(contactId, callbackObject);

            ::free(contactId);
            contactId = NULL;
        }
        break;
    }
    }
    return proceed;
}

static jboolean
MsOutlookAddrBookContactQuery_foreachMailUserInContainerTable
    (LPMAPICONTAINER mapiContainer, LPMAPITABLE mapiTable,
    const char * query, void * callback, void * callbackObject)
{
    return
        MsOutlookAddrBookContactQuery_foreachRowInTable(
            mapiTable,
            MsOutlookAddrBookContactQuery_onForeachMailUserInContainerTableRow,
            (LPUNKNOWN) mapiContainer,
            query, callback, callbackObject);
}

static jboolean
MsOutlookAddrBookContactQuery_foreachRowInTable
    (LPMAPITABLE mapiTable,
    MsOutlookAddrBookContactQuery_ForeachRowInTableCallback rowCallback,
    LPUNKNOWN iUnknown,
    const char * query, void * callback, void * callbackObject)
{
    HRESULT hResult;
    // In case,  that we have failed but other parts of the hierarchy may still
    // succeed.
    jboolean proceed = JNI_TRUE;

    hResult = mapiTable->SeekRow(BOOKMARK_BEGINNING, 0, NULL);
    if (HR_SUCCEEDED(hResult))
    {
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
                    LPENTRYID entryID = NULL;

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
                         * before we drill down the hierarchy and allocate even
                         * more rows.
                         */
                        MsOutlookAddrBookContactQuery_freeSRowSet(rows);

                        proceed
                            = rowCallback(
                                    iUnknown,
                                    entryIDBinary.cb, entryID, objType,
                                    query, callback, callbackObject);

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

static void*
MsOutlookAddrBookContactQuery_getAttachmentContactPhoto
    (LPMESSAGE message, ULONGLONG * length)
{
    HRESULT hResult;
    LPMAPITABLE attachmentTable;
    void* attachmentContactPhoto = NULL;

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
                                    (!isAttachmentContactPhotoRow)
                                        ? PR_ATTACHMENT_CONTACTPHOTO
                                        : PROP_TAG(PT_UNSPECIFIED, 0),
                                    length);
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

/**
 * Gets a binary property for a given entry.
 *
 * @param entry The entry to red the property from.
 * @param propId The property identifier.
 * @param binaryProp A pointer to a SBinary to store the property value
 * retrieved.
 */
void MsOutlookAddrBookContactQuery_getBinaryProp
    (LPMAPIPROP entry, ULONG propId, LPSBinary binaryProp)
{
    binaryProp->cb = 0;

    SPropTagArray tagArray;
    tagArray.cValues = 1;
    tagArray.aulPropTag[0] = PROP_TAG(PT_BINARY, propId);

    ULONG propCount;
    LPSPropValue propArray;
    HRESULT hResult = entry->GetProps(
            &tagArray,
            0x80000000, // MAPI_UNICODE.
            &propCount,
            &propArray);

    if (HR_SUCCEEDED(hResult))
    {
        SPropValue prop = propArray[0];
        if(MAPIAllocateBuffer(prop.Value.bin.cb, (void **) &binaryProp->lpb)
                == S_OK)
        {
            binaryProp->cb = prop.Value.bin.cb;
            memcpy(binaryProp->lpb, prop.Value.bin.lpb, binaryProp->cb);
        }
        MAPIFreeBuffer(propArray);
    }
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

/**
 * Retrieves a string representation of the contact id. This string must be
 * freed by the caller.
 *
 * @param contact A pointer to the instance of the contact.
 *
 * @return A string representation of the contact id. NULL if failed. This
 * string must be freed by yhe caller.
 */
LPSTR MsOutlookAddrBookContactQuery_getContactId(LPMAPIPROP contact)
{
    LPSTR entryId = NULL;

    SBinary binaryProp;
    binaryProp.cb = 0;
    MsOutlookAddrBookContactQuery_getBinaryProp(contact, 0x0FFF, &binaryProp);

    if(binaryProp.cb != 0)
    {
        entryId = (LPSTR)::malloc(binaryProp.cb * 2 + 1);
        HexFromBin(binaryProp.lpb, binaryProp.cb, entryId);
        MAPIFreeBuffer(binaryProp.lpb);
    }

    return entryId;
}

/**
 * Returns a pointer to the default contact folder.
 *
 * @return A pointer to the default contact folder. Or NULL if unavailable.
 */
LPMAPIFOLDER MsOutlookAddrBookContactQuery_getDefaultContactFolderId(void)
{
    LPMAPIFOLDER rootFolder = NULL;
    LPMDB msgStore = MsOutlookAddrBookContactQuery_getDefaultMsgStores();

    if(msgStore != NULL)
    {
        ULONG entryIdLength = 0;
        LPENTRYID receiveFolderEntryID = NULL;

        ULONG contactEntryIdLength = 0;
        LPENTRYID contactsFolderEntryID = NULL;

        HRESULT hResult = msgStore->GetReceiveFolder(
                    NULL,
                    0,
                    &entryIdLength,
                    &receiveFolderEntryID,
                    NULL);

        if(HR_SUCCEEDED(hResult))
        {
            hResult = MsOutlookAddrBookContactQuery_getContactsFolderEntryID(
                        msgStore,
                        entryIdLength,
                        receiveFolderEntryID,
                        &contactEntryIdLength,
                        &contactsFolderEntryID);
            MAPIFreeBuffer(receiveFolderEntryID);
        }

        ULONG objType;
        hResult = msgStore->OpenEntry(
                contactEntryIdLength,
                contactsFolderEntryID,
                NULL,
                MsOutlookAddrBookContactQuery_openEntryUlFlags,
                &objType,
                (LPUNKNOWN *) &rootFolder);
        if(contactsFolderEntryID != NULL)
        {
            MAPIFreeBuffer(contactsFolderEntryID);
        }

        msgStore->Release();
    }

    return rootFolder;
}

/**
 * Open the default message store.
 *
 * @return The default message store. Or NULL if unavailable.
 */
LPMDB MsOutlookAddrBookContactQuery_getDefaultMsgStores(void)
{
    LPMDB msgStore = NULL;
    LPMAPITABLE msgStoresTable;
    LPMAPISESSION mapiSession = MAPISession_getMapiSession();
    HRESULT hResult;

    hResult = mapiSession->GetMsgStoresTable(0, &msgStoresTable);
    if(HR_SUCCEEDED(hResult) && msgStoresTable)
    {
        hResult = msgStoresTable->SeekRow(BOOKMARK_BEGINNING, 0, NULL);
        if (HR_SUCCEEDED(hResult))
        {
            LPSRowSet rows;

            SBitMaskRestriction bitMaskRestriction;
            bitMaskRestriction.relBMR = BMR_NEZ;
            bitMaskRestriction.ulPropTag = PR_RESOURCE_FLAGS;
            bitMaskRestriction.ulMask = STATUS_DEFAULT_STORE;

            SRestriction defaultFolderRestriction;
            memset(
                    &defaultFolderRestriction,
                    0,
                    sizeof(defaultFolderRestriction));
            defaultFolderRestriction.rt = RES_BITMASK;
            defaultFolderRestriction.res.resBitMask = bitMaskRestriction;
            hResult = HrQueryAllRows(
                    msgStoresTable,
                    NULL,
                    &defaultFolderRestriction, // restriction
                    NULL,
                    0,
                    &rows);
            if (HR_SUCCEEDED(hResult) && rows->cRows == 1)
            {
                SRow row = rows->aRow[0];
                SBinary entryIDBinary = { 0, NULL };

                for(ULONG i = 0; i < row.cValues; ++i)
                {
                    LPSPropValue prop = (row.lpProps) + i;
                    switch (prop->ulPropTag)
                    {
                        case PR_ENTRYID:
                            entryIDBinary = prop->Value.bin;
                            break;
                    }
                }

                if(entryIDBinary.cb && entryIDBinary.lpb)
                {
                    hResult = mapiSession->OpenMsgStore(
                            0,
                            entryIDBinary.cb,
                            (LPENTRYID) entryIDBinary.lpb,
                            NULL,
                            MDB_NO_MAIL
                            | MsOutlookAddrBookContactQuery_openEntryUlFlags,
                            &msgStore);
                }
            }
            FreeProws(rows);
        }
        msgStoresTable->Release();
    }

    return msgStore;
}

/**
 * Returns the property tag associated for the given identifier and type.
 *
 * @param mapiProp The MAPI object from which we need to get the property tag
 * for a given identifier.
 * @param propId The identifier to resolve into a tag.
 * @param propType The type of the property (PT_UNSPECIFIED, PT_UNICODE, etc.).
 *
 * @return The property tag associated for the given identifier and type.
 */
ULONG MsOutlookAddrBookContactQuery_getPropTag
    (LPMAPIPROP mapiProp, long propId, long propType)
{
    ULONG propTag;

    if (propId < 0x8000)
    {
        if (propId == PROP_ID(PR_ATTACHMENT_CONTACTPHOTO))
            propTag = PR_HASATTACH;
        else
            propTag = PROP_TAG(propType, propId);
    }
    else
    {
        propTag = MsOutlookAddrBookContactQuery_getPropTagFromLid(
                (LPMAPIPROP) mapiProp,
                (LONG)propId);
        propTag = CHANGE_PROP_TYPE(propTag, propType);
    }

    return propTag;
}

static ULONG
MsOutlookAddrBookContactQuery_getPropTagFromLid(LPMAPIPROP mapiProp, LONG lid)
{
    GUID PSETID_Address
        = {0x00062004, 0x0000, 0x0000,
            {0xC0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x46}};
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

/**
 * Get one property for a given contact.
 *
 * @param mapiProp A pointer to the contact.
 * @param propTag The tag of the property to get.
 * @param prop The memory location to store the property value.
 *
 * @return S_OK if everything work fine. Any other value is a failure.
 */
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
        MAPIFreeBuffer(values);
    }
    return hResult;
}

/**
 * Deletes one property from a contact.
 *
 * @param propId The outlook property identifier.
 * @param nativeEntryId The identifer of the outlook entry to modify.
 *
 * @return 1 if the deletion succeded. 0 otherwise.
 */
int MsOutlookAddrBookContactQuery_IMAPIProp_1DeleteProp
    (long propId, const char * nativeEntryId)
{
    LPUNKNOWN mapiProp;
    if((mapiProp = MsOutlookAddrBookContactQuery_openEntryIdStr(nativeEntryId))
            == NULL)
    {
        return 0;
    }

    ULONG baseGroupEntryIdProp = 0;
    switch(propId)
    {
        case 0x00008084: // PidLidEmail1OriginalDisplayName
            baseGroupEntryIdProp = 0x00008080;
            break;
        case 0x00008094: // PidLidEmail2OriginalDisplayName
            baseGroupEntryIdProp = 0x00008090;
            break;
        case 0x000080A4: // PidLidEmail3OriginalDisplayName
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
            propTag = MsOutlookAddrBookContactQuery_getPropTag(
                    (LPMAPIPROP) mapiProp,
                    propIds[i],
                    PT_UNICODE);
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

                return 1;
            }
        }
        MAPIFreeBuffer(propTagArray);
        ((LPMAPIPROP)  mapiProp)->Release();

        return 0;
    }

    SPropTagArray propToDelete;
    propToDelete.cValues = 1;
    propToDelete.aulPropTag[0] = MsOutlookAddrBookContactQuery_getPropTag(
            (LPMAPIPROP) mapiProp,
            propId,
            PT_UNICODE);

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

            return 1;
        }
    }
    ((LPMAPIPROP)  mapiProp)->Release();

    return 0;
}

HRESULT MsOutlookAddrBookContactQuery_IMAPIProp_1GetProps(
        const char* nativeEntryId,
        int propIdCount,
        long * propIds,
        long flags,
        void ** props,
        unsigned long* propsLength,
        char * propsType)
{
    HRESULT hr = E_FAIL;
    LPSPropTagArray propTagArray;

    LPUNKNOWN mapiProp;
    if((mapiProp = MsOutlookAddrBookContactQuery_openEntryIdStr(nativeEntryId))
            == NULL)
    {
        return hr;
    }

    if (S_OK
            == MAPIAllocateBuffer(
                    CbNewSPropTagArray(propIdCount),
                    (void **) &propTagArray))
    {
        propTagArray->cValues = propIdCount;
        for(int i = 0; i < propIdCount; ++i)
        {
            propsLength[i] = 0;
            props[i] = NULL;
            propsType[i] = '\0';

            long propId = propIds[i];

            ULONG propTag = MsOutlookAddrBookContactQuery_getPropTag(
                    (LPMAPIPROP) mapiProp,
                    propId,
                    PT_UNSPECIFIED);
            *(propTagArray->aulPropTag + i) = propTag;
        }

        if (propTagArray)
        {
            ULONG propCount;
            LPSPropValue propArray;

            hr = ((LPMAPIPROP)  mapiProp)->GetProps(
                    propTagArray,
                    (ULONG) flags,
                    &propCount,
                    &propArray);

            if(HR_SUCCEEDED(hr))
            {
                ULONG j;

                for (j = 0; j < propCount; j++)
                {
                    LPSPropValue prop = propArray;

                    if(prop)
                    {
                        switch (PROP_TYPE(prop->ulPropTag))
                        {
                        case PT_BOOLEAN:
                        {
                            if ((PR_HASATTACH == prop->ulPropTag)
                                    && prop->Value.b)
                            {
                                props[j]
                                    = MsOutlookAddrBookContactQuery_getAttachmentContactPhoto(
                                            (LPMESSAGE) mapiProp,
                                            (ULONGLONG*) &propsLength[j]);
                                propsType[j] = 'b'; // byte array
                            }
                            break;
                        }

                        case PT_LONG:
                        {
                            propsLength[j] = sizeof(long);
                            if((props[j] = malloc(propsLength[j]))
                                    != NULL)
                            {
                                memcpy(
                                        props[j],
                                        &prop->Value.l,
                                        propsLength[j]);
                                propsType[j] = 'l'; // long
                            }
                            break;
                        }

                        case PT_STRING8:
                        {
                            if (prop->Value.lpszA)
                            {
                                propsLength[j] = strlen(prop->Value.lpszA) + 1;
                                if((props[j] = malloc(propsLength[j]))
                                        != NULL)
                                {
                                    memcpy(
                                            props[j],
                                            prop->Value.lpszA,
                                            propsLength[j]);
                                    propsType[j] = 's'; // 8 bits string
                                }
                            }
                            break;
                        }

                        case PT_UNICODE:
                        {
                            if (prop->Value.lpszW)
                            {
                                propsLength[j] =
                                    (wcslen(prop->Value.lpszW) + 1) * 2;
                                if((props[j] = malloc(propsLength[j]))
                                        != NULL)
                                {
                                    memcpy(
                                            props[j],
                                            prop->Value.lpszW,
                                            propsLength[j]);
                                    propsType[j] = 'u'; // 16 bits string
                                }
                            }
                            break;
                        }

                        case PT_BINARY:
                        {
                            propsLength[j] = prop->Value.bin.cb * 2 + 1;
                            if((props[j] = malloc(propsLength[j]))
                                    != NULL)
                            {
                                HexFromBin(
                                        prop->Value.bin.lpb,
                                        prop->Value.bin.cb,
                                        (LPSTR) props[j]);

                                propsType[j] = 's'; // 16 bits string
                            }
                            break;
                        }
                        }
                    }
                    propArray++;
                    MAPIFreeBuffer(prop);
                }
                MAPIFreeBuffer(propTagArray);
            }
        }
    }
    ((LPMAPIPROP)  mapiProp)->Release();

    return hr;
}

/**
 * Saves one contact property.
 *
 * @param propId The outlook property identifier.
 * @param nativeValue The value to set to the outlook property.
 * @param nativeEntryId The identifer of the outlook entry to modify.
 *
 * @return 1 if the modification succeded. 0 otherwise.
 */
int MsOutlookAddrBookContactQuery_IMAPIProp_1SetPropString
    (long propId, const wchar_t* nativeValue, const char* nativeEntryId)
{
    HRESULT hResult;

    LPUNKNOWN mapiProp;
    if((mapiProp = MsOutlookAddrBookContactQuery_openEntryIdStr(nativeEntryId))
            == NULL)
    {
        return 0;
    }

    size_t valueLength = wcslen(nativeValue);
    LPWSTR wCharValue = (LPWSTR)::malloc((valueLength + 1) * sizeof(wchar_t));
    memcpy(wCharValue, nativeValue, (valueLength + 1) * sizeof(wchar_t));

    ULONG baseGroupEntryIdProp = 0;
    switch(propId)
    {
        case 0x00008084: // PidLidEmail1OriginalDisplayName
            baseGroupEntryIdProp = 0x00008080;
            break;
        case 0x00008094: // PidLidEmail2OriginalDisplayName
            baseGroupEntryIdProp = 0x00008090;
            break;
        case 0x000080A4: // PidLidEmail3OriginalDisplayName
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
            (baseGroupEntryIdProp + 5) // 0x8085 PidLidEmail1OriginalEntryID
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
            propTag = MsOutlookAddrBookContactQuery_getPropTag(
                    (LPMAPIPROP) mapiProp,
                    propIds[i],
                    PT_UNSPECIFIED);
            *(propTagArray->aulPropTag + i) = propTag;
        }
        hResult = ((LPMAPIPROP)  mapiProp)->GetProps(
                    propTagArray,
                    MAPI_UNICODE,
                    &propCount,
                    &propArray);

        if(SUCCEEDED(hResult))
        {
            LPWSTR addressType = (LPWSTR) L"SMTP";
            LONG providerEmailList[1];
            switch(propId)
            {
                case 0x00008084: // PidLidEmail1OriginalDisplayName
                    providerEmailList[0] = 0x00000000;
                    propArray[1].Value.l |= 0x00000001;
                    break;
                case 0x00008094: // PidLidEmail2OriginalDisplayName
                    providerEmailList[0] = 0x00000001;
                    propArray[1].Value.l |= 0x00000002;
                    break;
                case 0x000080A4: // PidLidEmail3OriginalDisplayName
                    providerEmailList[0] = 0x00000002;
                    propArray[1].Value.l |= 0x00000004;
                    break;
            }

            propArray[0].Value.MVl.cValues = 1;
            propArray[0].Value.MVl.lpl = providerEmailList;

            if(propArray[2].ulPropTag == PT_ERROR
                    || propArray[2].Value.err == MAPI_E_NOT_FOUND
                    || propArray[2].Value.lpszW == NULL)
            {
                propArray[2].Value.lpszW = wCharValue;
            }
            if(propArray[3].ulPropTag == PT_ERROR
                    || propArray[3].Value.err == MAPI_E_NOT_FOUND
                    || propArray[3].Value.lpszW == NULL)
            {
                propArray[3].Value.lpszW = addressType;
            }
            if(propArray[4].ulPropTag == PT_ERROR
                    || propArray[4].Value.err == MAPI_E_NOT_FOUND
                    || propArray[4].Value.lpszW == NULL
                    || wcsncmp(propArray[3].Value.lpszW, addressType, 4) == 0)
            {
                propArray[4].Value.lpszW = wCharValue;
            }
            propArray[5].Value.lpszW = wCharValue;

            if(MsOutlookAddrBookContactQuery_createEmailAddress(
                    (LPMESSAGE) mapiProp,
                    wCharValue, // displayName
                    addressType, // addressType
                    wCharValue, // emailAddress
                    wCharValue, // originalDisplayName
                    providerEmailList,
                    propArray[1].Value.l,
                    propIds,
                    7) == S_OK)
            {
                MAPIFreeBuffer(propArray);
                MAPIFreeBuffer(propTagArray);
                ((LPMAPIPROP)  mapiProp)->Release();
                ::free(wCharValue);
                wCharValue = NULL;
                return 1;
            }
        }
        MAPIFreeBuffer(propTagArray);
        ((LPMAPIPROP)  mapiProp)->Release();
        ::free(wCharValue);
        wCharValue = NULL;
        return 0;
    }

    SPropValue updateValue;
    updateValue.ulPropTag = MsOutlookAddrBookContactQuery_getPropTag(
            (LPMAPIPROP) mapiProp,
            propId,
            PT_UNICODE);
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
            ::free(wCharValue);
            wCharValue = NULL;

            return 1;
        }
    }

    ((LPMAPIPROP)  mapiProp)->Release();
    ::free(wCharValue);
    wCharValue = NULL;
    return 0;
}

static jboolean
MsOutlookAddrBookContactQuery_mailUserMatches
    (LPMAPIPROP mailUser, const char * query)
{
    // TODO Auto-generated method stub
    return JNI_TRUE;
}

static jboolean
MsOutlookAddrBookContactQuery_onForeachContactInMsgStoresTableRow
    (LPUNKNOWN mapiSession,
    ULONG entryIDByteCount, LPENTRYID entryID, ULONG objType,
    const char * query, void * callback, void * callbackObject)
{
    HRESULT hResult;
    LPMDB msgStore;
    // In case, that we've failed but other parts of the hierarchy may still
    // succeed.
    jboolean proceed = JNI_TRUE;

    hResult = ((LPMAPISESSION) mapiSession)->OpenMsgStore(
                0,
                entryIDByteCount, entryID,
                NULL,
                MDB_NO_MAIL | MsOutlookAddrBookContactQuery_openEntryUlFlags,
                &msgStore);
    if (HR_SUCCEEDED(hResult))
    {
        LPENTRYID receiveFolderEntryID = NULL;
        ULONG contactsFolderEntryIDByteCount = 0;
        LPENTRYID contactsFolderEntryID = NULL;

        hResult = msgStore->GetReceiveFolder(
                    NULL,
                    0,
                    &entryIDByteCount,
                    &receiveFolderEntryID,
                    NULL);
        if (HR_SUCCEEDED(hResult))
        {
            hResult = MsOutlookAddrBookContactQuery_getContactsFolderEntryID(
                        msgStore,
                        entryIDByteCount,
                        receiveFolderEntryID,
                        &contactsFolderEntryIDByteCount,
                        &contactsFolderEntryID);
            MAPIFreeBuffer(receiveFolderEntryID);
        }
        if (HR_FAILED(hResult))
        {
            hResult = MsOutlookAddrBookContactQuery_getContactsFolderEntryID(
                        msgStore,
                        0,
                        NULL,
                        &contactsFolderEntryIDByteCount,
                        &contactsFolderEntryID);
        }
        if (HR_SUCCEEDED(hResult))
        {
            ULONG contactsFolderObjType;
            LPUNKNOWN contactsFolder;

            hResult = msgStore->OpenEntry(
                    contactsFolderEntryIDByteCount,
                    contactsFolderEntryID,
                    NULL,
                    MsOutlookAddrBookContactQuery_openEntryUlFlags,
                    &contactsFolderObjType,
                    &contactsFolder);
            if (HR_SUCCEEDED(hResult))
            {
                proceed = MsOutlookAddrBookContactQuery_foreachMailUser(
                            contactsFolderObjType,
                            contactsFolder,
                            query,
                            callback,
                            callbackObject);
                contactsFolder->Release();
            }
            MAPIFreeBuffer(contactsFolderEntryID);
        }
        msgStore->Release();
    }

    return proceed;
}

static jboolean
MsOutlookAddrBookContactQuery_onForeachMailUserInContainerTableRow
    (LPUNKNOWN mapiContainer,
    ULONG entryIDByteCount, LPENTRYID entryID, ULONG objType,
    const char * query, void * callback, void * callbackObject)
{
    HRESULT hResult;
    LPUNKNOWN iUnknown;
    jboolean proceed;

    // Make write failed and image load.
    hResult = ((LPMAPICONTAINER) mapiContainer)->OpenEntry(
                entryIDByteCount,
                entryID,
                NULL,
                MsOutlookAddrBookContactQuery_openEntryUlFlags,
                &objType,
                &iUnknown);
    if (HR_SUCCEEDED(hResult))
    {
        proceed = MsOutlookAddrBookContactQuery_foreachMailUser(
                    objType,
                    iUnknown,
                    query,
                    callback,
                    callbackObject);
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
 * @param entryIdStr The identifier of the entry to open.
 *
 * @return A pointer to the opened entry. NULL if anything goes wrong.
 */
LPUNKNOWN MsOutlookAddrBookContactQuery_openEntryIdStr(const char* entryIdStr)
{
    LPUNKNOWN entry = NULL;
    ULONG entryIdSize = strlen(entryIdStr) / 2;
    LPENTRYID entryId = (LPENTRYID) malloc(entryIdSize * sizeof(char));

    if(entryId != NULL)
    {
        if(FBinFromHex((LPSTR) entryIdStr, (LPBYTE) entryId))
        {
            entry = MsOutlookAddrBookContactQuery_openEntryId(
                    entryIdSize,
                    entryId);
        }
        ::free(entryId);
    }
    return entry;
}

/**
 * Opens an object based on its entry id.
 *
 * @param entryIdSize The size of the identifier of the entry to open.
 * @param entryId The identifier of the entry to open.
 *
 * @return A pointer to the opened entry. NULL if anything goes wrong.
 */
LPUNKNOWN MsOutlookAddrBookContactQuery_openEntryId
    (ULONG entryIdSize, LPENTRYID entryId)
{
    LPMAPISESSION mapiSession = MAPISession_getMapiSession();
    ULONG objType;
    LPUNKNOWN entry = NULL;

    mapiSession->OpenEntry(
            entryIdSize,
            entryId,
            NULL,
            MAPI_BEST_ACCESS,
            &objType,
            &entry);

    return entry;
}

static void*
MsOutlookAddrBookContactQuery_readAttachment
    (LPMESSAGE message, LONG method, ULONG num, ULONG cond, ULONGLONG * length)
{
    void* attachment = NULL;

    if (ATTACH_BY_VALUE == method)
    {
        HRESULT hResult;
        LPATTACH attach;

        hResult = message->OpenAttach(num, NULL, 0, &attach);
        if (HR_SUCCEEDED(hResult))
        {
            IStream *stream = NULL;

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
            if (HR_SUCCEEDED(hResult) && stream)
            {
                STATSTG statstg;

                hResult = stream->Stat(&statstg, STATFLAG_NONAME);
                if ((S_OK == hResult) && ((*length = statstg.cbSize.QuadPart)))
                {
                    if((attachment = (void*) malloc(*length)) != NULL)
                    {
                        ULONG read;
                        jint mode;

                        hResult = stream->Read(
                                attachment, (ULONG) (*length), &read);
                        mode = ((S_OK == hResult) || (S_FALSE == hResult))
                            ? 0
                            : JNI_ABORT;
                        if(0 != mode)
                        {
                            free(attachment);
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

/**
 * Gets a string property for a given entry.
 *
 * @param entry The entry to red the property from.
 * @param propId The property identifier.
 *
 * @return A string representation of the property value retrieved. Must be
 * freed by the caller.
 */
char* MsOutlookAddrBookContactQuery_getStringUnicodeProp
    (LPUNKNOWN entry, ULONG propId)
{
    SPropTagArray tagArray;
    tagArray.cValues = 1;
    tagArray.aulPropTag[0] = PROP_TAG(PT_UNICODE, propId);

    ULONG propCount;
    LPSPropValue propArray;
    HRESULT hResult = ((LPMAPIPROP)entry)->GetProps(
            &tagArray,
            0x80000000, // MAPI_UNICODE.
            &propCount,
            &propArray);

    if (HR_SUCCEEDED(hResult))
    {
        unsigned int length = wcslen(propArray->Value.lpszW);
        char * value;
        if((value = (char*) malloc((length + 1) * sizeof(char)))
            == NULL)
        {
            fprintf(stderr,
                    "getStringUnicodeProp (addrbook/MsOutlookAddrBookContactQuery.c): \
                    \n\tmalloc\n");
            fflush(stderr);
        }
        if(wcstombs(value, propArray->Value.lpszW, length + 1) != length)
        {
            fprintf(stderr,
                    "getStringUnicodeProp (addrbook/MsOutlookAddrBookContactQuery.c): \
                        \n\tmbstowcs\n");
            fflush(stderr);
            MAPIFreeBuffer(propArray);
            ::free(value);
            value = NULL;
            return NULL;
        }
        MAPIFreeBuffer(propArray);
        return value;
    }

    return NULL;
}

/**
 * Compares two identifiers to determine if they are part of the same
 * Outlook contact.
 *
 * @param id1 The first identifier.
 * @param id2 The second identifier.
 *
 * @result True if id1 and id2 are two identifiers of the same contact.  False
 * otherwise.
 */
int MsOutlookAddrBookContactQuery_compareEntryIds(
       LPSTR id1,
       LPSTR id2)
{
    int result = 0;
    LPMAPISESSION session = MAPISession_getMapiSession();

    LPMAPIPROP mapiId1;
    if((mapiId1 = (LPMAPIPROP)
                MsOutlookAddrBookContactQuery_openEntryIdStr(id1))
            == NULL)
    {
        return result;
    }
    SBinary contactId1;
    contactId1.cb = 0;
    MsOutlookAddrBookContactQuery_getBinaryProp(mapiId1, 0x0FFF, &contactId1);

    LPMAPIPROP mapiId2;
    if((mapiId2 = (LPMAPIPROP)
                MsOutlookAddrBookContactQuery_openEntryIdStr(id2))
            == NULL)
    {
        mapiId1->Release();
        MAPIFreeBuffer(contactId1.lpb);
        return result;
    }
    SBinary contactId2;
    contactId2.cb = 0;
    MsOutlookAddrBookContactQuery_getBinaryProp(mapiId2, 0x0FFF, &contactId2);

    if(session != NULL)
    {
        ULONG res;
        if(session->CompareEntryIDs(
                contactId1.cb,
                (LPENTRYID) contactId1.lpb,
                contactId2.cb,
                (LPENTRYID) contactId2.lpb,
                0,
                &res) != S_OK)
        {
            fprintf(stderr,
                    "compareEntryIds (addrbook/MsOutlookAddrBookContactQuery.c): \
                        \n\tMAPISession::CompareEntryIDs\n");
            fflush(stderr);
            mapiId1->Release();
            MAPIFreeBuffer(contactId1.lpb);
            mapiId2->Release();
            MAPIFreeBuffer(contactId2.lpb);
            return result;
        }
        result = res;
    }

    mapiId1->Release();
    MAPIFreeBuffer(contactId1.lpb);
    mapiId2->Release();
    MAPIFreeBuffer(contactId2.lpb);
    return result;
}
