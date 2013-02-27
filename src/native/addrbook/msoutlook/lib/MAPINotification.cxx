/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#include "MAPINotification.h"

#define WIN32_LEAN_AND_MEAN
#include <windows.h>

#include <stdio.h>
#include <Unknwn.h>

#include <mapidefs.h>
#include <mapiutil.h>

#include "../net_java_sip_communicator_plugin_addrbook_msoutlook_MsOutlookAddrBookContactSourceService.h"
#include "../net_java_sip_communicator_plugin_addrbook_msoutlook_MsOutlookAddrBookContactQuery.h"
/**
 * Manages notification for the message data base (used to get the list of
 * contact).
 *
 * @author Vincent Lucas
 */

/**
 * The List of events we want to retrieve.
 */
static ULONG EVENT_MASK
    = fnevObjectCreated
        | fnevObjectDeleted
        | fnevObjectModified
        | fnevObjectMoved;

/**
 * Functions called when an event is fired from the message data base.
 *
 * @param lpvContext A pointer to the message data base.
 * @param cNotifications The number of event in this call.
 * @param lpNotifications The list of notifications.
 */
LONG STDAPICALLTYPE onNotify(
        LPVOID lpvContext,
        ULONG cNotifications,
        LPNOTIFICATION lpNotifications)
{
    for(unsigned int i = 0; i < cNotifications; ++i)
    {
        LPUNKNOWN iUnknown = NULL;
        if(lpvContext != NULL)
        {
            iUnknown = openEntry(
                        lpNotifications[i].info.obj.cbEntryID,
                        lpNotifications[i].info.obj.lpEntryID,
                        lpvContext);
        }

        // A contact has been created
        if(lpNotifications[i].ulEventType == fnevObjectCreated)
        {
            if(lpNotifications[i].info.obj.ulObjType == MAPI_MESSAGE)
            {
                callInsertedCallbackMethod(iUnknown);
            }
        }
        // A contact has been Modified
        else if(lpNotifications[i].ulEventType == fnevObjectModified)
        {
            if(lpNotifications[i].info.obj.ulObjType == MAPI_MESSAGE)
            {
                callUpdatedCallbackMethod(iUnknown);
            }
        }
        // A contact has been deleted.
        else if(lpNotifications[i].ulEventType == fnevObjectDeleted)
        {
            if(lpvContext != NULL)
            {
                char entryIdStr[lpNotifications[i].info.obj.cbEntryID * 2 + 1];

                HexFromBin(
                        (LPBYTE) lpNotifications[i].info.obj.lpEntryID,
                        lpNotifications[i].info.obj.cbEntryID,
                        entryIdStr);

                if(lpNotifications[i].info.obj.ulObjType == MAPI_MESSAGE)
                {
                    callDeletedCallbackMethod(entryIdStr);
                }
            }
        }
        // A contact has been deleted (moved to trash).
        else if(lpNotifications[i].ulEventType == fnevObjectMoved)
        {
            if(lpvContext != NULL)
            {
                char entryIdStr[lpNotifications[i].info.obj.cbEntryID * 2 + 1];
                HexFromBin(
                        (LPBYTE) lpNotifications[i].info.obj.lpEntryID,
                        lpNotifications[i].info.obj.cbEntryID,
                        entryIdStr);
                char parentEntryIdStr[
                    lpNotifications[i].info.obj.cbParentID * 2 + 1];
                HexFromBin(
                        (LPBYTE) lpNotifications[i].info.obj.lpParentID,
                        lpNotifications[i].info.obj.cbParentID,
                        parentEntryIdStr);
                ULONG wasteBasketTags[] = {1, PR_IPM_WASTEBASKET_ENTRYID};  
                ULONG wasteBasketNbValues = 0;  
                LPSPropValue wasteBasketProps = NULL;  
                ((LPMDB)lpvContext)->GetProps(
                        (LPSPropTagArray) wasteBasketTags,
                        MAPI_UNICODE,
                        &wasteBasketNbValues,
                        &wasteBasketProps); 
                char wasteBasketEntryIdStr[
                    wasteBasketProps[0].Value.bin.cb * 2 + 1];
                HexFromBin(
                        (LPBYTE) wasteBasketProps[0].Value.bin.lpb,
                        wasteBasketProps[0].Value.bin.cb,
                        wasteBasketEntryIdStr);

                openEntry(
                        lpNotifications[i].info.obj.cbParentID,
                        lpNotifications[i].info.obj.lpParentID,
                        lpvContext);


                if(lpNotifications[i].info.obj.ulObjType == MAPI_MESSAGE
                        && strcmp(parentEntryIdStr, wasteBasketEntryIdStr) == 0)
                {
                    callDeletedCallbackMethod(entryIdStr);
                }
            }
        }

        if(iUnknown != NULL)
        {
            iUnknown->Release();
        }
    }

    // A client must always return a S_OK.
    return S_OK;
}

/**
 * Registers to notification for the given message data base.
 *
 * @param iUnknown The data base to register to in order to receive events.
 *
 * @return A unsigned long which is a token wich must be used to call the
 * unadvise function for the same message data base.
 */
ULONG registerNotifyMessageDataBase(
        LPMDB iUnknown)
{
    LPMAPIADVISESINK adviseSink;
    HrAllocAdviseSink(
            &onNotify,
            iUnknown,
            &adviseSink);
    ULONG nbConnection = 0;
        iUnknown->Advise(
            0,
            NULL,
            EVENT_MASK,
            adviseSink,
            &nbConnection);

    return nbConnection;
}
