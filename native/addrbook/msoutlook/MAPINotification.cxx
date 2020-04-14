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
#include "MAPINotification.h"

#include "MAPISession.h"
#include "net_java_sip_communicator_plugin_addrbook_msoutlook_MsOutlookAddrBookContactSourceService.h"
#include "net_java_sip_communicator_plugin_addrbook_msoutlook_MsOutlookAddrBookContactQuery.h"
#include "MsOutlookAddrBookContactSourceService.h"
#include "MsOutlookAddrBookContactQuery.h"
#include "MsOutlookUtils.h"
#include "MsOutlookCalendar.h"

#include <mapidefs.h>
#include <stdio.h>
#include <unknwn.h>

/**
 * Manages notification for the message data base (used to get the list of
 * contact).
 *
 * @author Vincent Lucas
 */

/**
 * The List of events we want to retrieve.
 */
static ULONG MAPINotification_EVENT_MASK
    = fnevObjectCreated
        | fnevObjectDeleted
        | fnevObjectModified
        | fnevObjectMoved
        | fnevObjectCopied;

static LPMDB * MAPINotification_msgStores = NULL;
static LPMAPIADVISESINK * MAPINotification_adviseSinks = NULL;
static ULONG * MAPINotification_msgStoresConnection = NULL;
static LPMAPIADVISESINK * MAPINotification_calendarAdviseSinks = NULL;
static ULONG * MAPINotification_calendarMsgStoresConnection = NULL;
static LPMAPITABLE MAPINotification_msgStoresTable = NULL;
static LPMAPIADVISESINK MAPINotification_msgStoresTableAdviseSink = NULL;
static ULONG MAPINotification_msgStoresTableConnection = 0;
static ULONG MAPINotification_nbMsgStores = 0;
static jmethodID MAPINotification_notificationsDelegateMethodIdDeleted = NULL;
static jmethodID MAPINotification_notificationsDelegateMethodIdInserted = NULL;
static jmethodID MAPINotification_notificationsDelegateMethodIdUpdated = NULL;
static jmethodID MAPINotification_notificationsDelegateCalendarMethodIdDeleted
	= NULL;
static jmethodID MAPINotification_notificationsDelegateCalendarMethodIdInserted
	= NULL;
static jmethodID MAPINotification_notificationsDelegateCalendarMethodIdUpdated
	= NULL;
static jobject MAPINotification_notificationsDelegateObject = NULL;
static jobject MAPINotification_notificationsDelegateCalendarObject = NULL;
static ULONG MAPINotification_openEntryUlFlags = MAPI_BEST_ACCESS;
static JavaVM * MAPINotification_VM = NULL;

void (*MAPINotification_callDeletedMethod)(LPSTR iUnknown) = NULL;
void (*MAPINotification_callInsertedMethod)(LPSTR iUnknown) = NULL;
void (*MAPINotification_callUpdatedMethod)(LPSTR iUnknown) = NULL;
void (*MAPINotification_callCalendarDeletedMethod)(LPSTR iUnknown) = NULL;
void (*MAPINotification_callCalendarInsertedMethod)(LPSTR iUnknown) = NULL;
void (*MAPINotification_callCalendarUpdatedMethod)(LPSTR iUnknown) = NULL;


typedef LONG (__attribute__((__stdcall__)) MAPINotification_callback)(LPVOID, DWORD, LPNOTIFICATION);

ULONG MAPINotification_registerNotifyMessageDataBase(
        LPMDB iUnknown,
        LPMAPIADVISESINK * adviseSink,
        ULONG type,
        MAPINotification_callback callback);
ULONG MAPINotification_registerNotifyTable
    (LPMAPITABLE iUnknown, LPMAPIADVISESINK * adviseSink);
LONG STDAPICALLTYPE MAPINotification_tableChanged
    (LPVOID lpvContext, ULONG cNotifications, LPNOTIFICATION lpNotifications);

/**
 * Calls back the java side to list a contact.
 *
 * @param iUnknown The string representation of the entry id of the contact.
 * @param object The Java object used to call the callback method. If NULL, then
 * call the inserted notification method.
 *
 * @return True everything works fine and that we must continue to list the
 * other contacts. False otherwise.
 */
boolean MAPINotification_callCallbackMethod(LPSTR iUnknown, long objectAddr)
{
    if(objectAddr <= 0)
    {
    	MsOutlookUtils_log("Callback object is null. We are calling insert method");
        MAPINotification_jniCallInsertedMethod(iUnknown);
        return true;
    }

    boolean proceed = false;
    JNIEnv *tmpJniEnv = NULL;

    if(MAPINotification_VM
            ->AttachCurrentThreadAsDaemon((void**) &tmpJniEnv, NULL) == 0)
    {
        if(objectAddr > 0)
        {
            jobject object = (jobject)(intptr_t)objectAddr;
            jclass callbackClass = tmpJniEnv->GetObjectClass(object);
            if(callbackClass)
            {
                jmethodID ptrOutlookContactCallbackMethodIdCallback
                    = tmpJniEnv->GetMethodID(
                            callbackClass,
                            "callback",
                            "(Ljava/lang/String;)Z");

                if(ptrOutlookContactCallbackMethodIdCallback)
                {
                    jstring value = tmpJniEnv->NewStringUTF(iUnknown);

                    // Report the MAPI_MAILUSER to the callback.
                    proceed = tmpJniEnv->CallBooleanMethod(
                            object,
                            ptrOutlookContactCallbackMethodIdCallback,
                            value);
                }
                else
                {
                	MsOutlookUtils_log("Error in MAPI Notification.[4]");
                }

                tmpJniEnv->DeleteLocalRef(callbackClass);
            }
            else
            {
            	MsOutlookUtils_log("Error in MAPI Notification.[3]");
            }
        }
        else
        {
        	MsOutlookUtils_log("Error in MAPI Notification.[2]");
        }
        MAPINotification_VM->DetachCurrentThread();
    }
    else
    {
    	MsOutlookUtils_log("Error in MAPI Notification.");
    }

    return proceed;
}

/**
 * Calls back the java side when a contact is deleted.
 *
 * @param iUnknown The string representation of the entry id of the deleted
 * contact.
 */
void MAPINotification_jniCallDeletedMethod(LPSTR iUnknown)
{
    JNIEnv *tmpJniEnv = NULL;

    if(MAPINotification_VM
            ->AttachCurrentThreadAsDaemon((void**) &tmpJniEnv, NULL) == 0)
    {
        jstring value = tmpJniEnv->NewStringUTF(iUnknown);

        if(MAPINotification_notificationsDelegateObject != NULL)
			tmpJniEnv->CallVoidMethod(
					MAPINotification_notificationsDelegateObject,
					MAPINotification_notificationsDelegateMethodIdDeleted,
					value);

        tmpJniEnv->DeleteLocalRef(value);

        MAPINotification_VM->DetachCurrentThread();
    }
}

/**
 * Calls back the java side when a contact is inserted.
 *
 * @param iUnknown A pointer to the newly created contact.
 */
void MAPINotification_jniCallInsertedMethod(LPSTR iUnknown)
{
    JNIEnv *tmpJniEnv = NULL;

    if(MAPINotification_VM
            ->AttachCurrentThreadAsDaemon((void**) &tmpJniEnv, NULL) == 0)
    {
        jstring value = tmpJniEnv->NewStringUTF(iUnknown);

        if(MAPINotification_notificationsDelegateObject != NULL)
        {
			tmpJniEnv->CallVoidMethod(
					MAPINotification_notificationsDelegateObject,
					MAPINotification_notificationsDelegateMethodIdInserted,
					value);
        }
        else
        {
        	MsOutlookUtils_log("MAPI notification delegate is null.");
        }

        tmpJniEnv->DeleteLocalRef(value);

        MAPINotification_VM->DetachCurrentThread();
    }
}

/**
 * Calls back the java side when a contact is updated.
 *
 * @param iUnknown A pointer to the updated contact.
 */
void MAPINotification_jniCallUpdatedMethod(LPSTR iUnknown)
{
    JNIEnv *tmpJniEnv = NULL;
    if(MAPINotification_VM
            ->AttachCurrentThreadAsDaemon((void**) &tmpJniEnv, NULL) == 0)
    {
        jstring value = tmpJniEnv->NewStringUTF(iUnknown);

        if(MAPINotification_notificationsDelegateObject != NULL)
			tmpJniEnv->CallVoidMethod(
					MAPINotification_notificationsDelegateObject,
					MAPINotification_notificationsDelegateMethodIdUpdated,
					value);

        tmpJniEnv->DeleteLocalRef(value);

        MAPINotification_VM->DetachCurrentThread();
    }
}

/**
 * Calls back the java side when a calendar item is deleted.
 *
 * @param iUnknown The string representation of the entry id of the deleted
 * contact.
 */
void MAPINotification_jniCallCalendarDeletedMethod(LPSTR iUnknown)
{
    JNIEnv *tmpJniEnv = NULL;

    if(MAPINotification_VM
            ->AttachCurrentThreadAsDaemon((void**) &tmpJniEnv, NULL) == 0)
    {
        jstring value = tmpJniEnv->NewStringUTF(iUnknown);

        if(MAPINotification_notificationsDelegateCalendarObject != NULL)
        {
        	tmpJniEnv->CallVoidMethod(
				MAPINotification_notificationsDelegateCalendarObject,
				MAPINotification_notificationsDelegateCalendarMethodIdDeleted,
				value);
        }

        tmpJniEnv->DeleteLocalRef(value);

        MAPINotification_VM->DetachCurrentThread();
    }
}

/**
 * Calls back the java side when a calendar itm is inserted.
 *
 * @param iUnknown A pointer to the newly created calendar item.
 */
void MAPINotification_jniCallCalendarInsertedMethod(LPSTR iUnknown)
{
    JNIEnv *tmpJniEnv = NULL;
    if(MAPINotification_VM
            ->AttachCurrentThreadAsDaemon((void**) &tmpJniEnv, NULL) == 0)
    {
        jstring value = tmpJniEnv->NewStringUTF(iUnknown);

        if(MAPINotification_notificationsDelegateCalendarObject != NULL)
			tmpJniEnv->CallVoidMethod(
							MAPINotification_notificationsDelegateCalendarObject,
							MAPINotification_notificationsDelegateCalendarMethodIdInserted,
							value);

        tmpJniEnv->DeleteLocalRef(value);

        MAPINotification_VM->DetachCurrentThread();
    }
}

/**
 * Calls back the java side when a calendar item is updated.
 *
 * @param iUnknown A pointer to the updated calendar item.
 */
void MAPINotification_jniCallCalendarUpdatedMethod(LPSTR iUnknown)
{
    JNIEnv *tmpJniEnv = NULL;
    if(MAPINotification_VM
            ->AttachCurrentThreadAsDaemon((void**) &tmpJniEnv, NULL) == 0)
    {
        jstring value = tmpJniEnv->NewStringUTF(iUnknown);

        if(MAPINotification_notificationsDelegateCalendarObject != NULL)
			tmpJniEnv->CallVoidMethod(
					MAPINotification_notificationsDelegateCalendarObject,
					MAPINotification_notificationsDelegateCalendarMethodIdUpdated,
					value);


        tmpJniEnv->DeleteLocalRef(value);

        MAPINotification_VM->DetachCurrentThread();
    }
}

/**
 * Functions called when an event is fired from the message data base.
 *
 * @param lpvContext A pointer to the message data base.
 * @param cNotifications The number of event in this call.
 * @param lpNotifications The list of notifications.
 */
LONG
STDAPICALLTYPE MAPINotification_calendarOnNotify
    (LPVOID lpvContext, ULONG cNotifications, LPNOTIFICATION lpNotifications)
{
	return MAPINotification_onNotify(lpvContext, cNotifications, lpNotifications,
			CALENDAR_FOLDER_TYPE);
}

/**
 * Functions called when an event is fired from the message data base.
 *
 * @param lpvContext A pointer to the message data base.
 * @param cNotifications The number of event in this call.
 * @param lpNotifications The list of notifications.
 */
LONG
STDAPICALLTYPE MAPINotification_contactsOnNotify
    (LPVOID lpvContext, ULONG cNotifications, LPNOTIFICATION lpNotifications)
{
	return MAPINotification_onNotify(lpvContext, cNotifications, lpNotifications,
			CONTACTS_FOLDER_TYPE);
}

/**
 * Functions called when an event is fired from the message data base.
 *
 * @param lpvContext A pointer to the message data base.
 * @param cNotifications The number of event in this call.
 * @param lpNotifications The list of notifications.
 */
LONG
STDAPICALLTYPE MAPINotification_onNotify
    (LPVOID lpvContext, ULONG cNotifications, LPNOTIFICATION lpNotifications,
    ULONG type)
{
    for(unsigned int i = 0; i < cNotifications; ++i)
    {
        // A contact has been created (a new one or a copy).
        if(lpNotifications[i].ulEventType == fnevObjectCreated
                || lpNotifications[i].ulEventType == fnevObjectCopied)
        {
            if(lpvContext != NULL)
            {
                LPSTR entryIdStr = (LPSTR)
                    ::malloc((lpNotifications[i].info.obj.cbEntryID + 1) * 2);

                HexFromBin(
                        (LPBYTE) lpNotifications[i].info.obj.lpEntryID,
                        lpNotifications[i].info.obj.cbEntryID,
                        entryIdStr);

                if(lpNotifications[i].info.obj.ulObjType == MAPI_MESSAGE
                        && MAPINotification_callInsertedMethod != NULL
                        && type == CONTACTS_FOLDER_TYPE)
                {
                    MAPINotification_callInsertedMethod(entryIdStr);
                }
                if(lpNotifications[i].info.obj.ulObjType == MAPI_MESSAGE
						&& MAPINotification_callCalendarInsertedMethod != NULL
						&& type == CALENDAR_FOLDER_TYPE)
				{
					MAPINotification_callCalendarInsertedMethod(entryIdStr);
				}

                ::free(entryIdStr);
                entryIdStr = NULL;
            }
        }
        // A contact has been Modified
        else if(lpNotifications[i].ulEventType == fnevObjectModified)
        {
            if(lpvContext != NULL)
            {
                LPSTR entryIdStr = (LPSTR)
                    ::malloc((lpNotifications[i].info.obj.cbEntryID + 1) * 2);

                HexFromBin(
                        (LPBYTE) lpNotifications[i].info.obj.lpEntryID,
                        lpNotifications[i].info.obj.cbEntryID,
                        entryIdStr);

                if(lpNotifications[i].info.obj.ulObjType == MAPI_MESSAGE
                        && MAPINotification_callUpdatedMethod != NULL
                        && type == CONTACTS_FOLDER_TYPE)
                {
                    MAPINotification_callUpdatedMethod(entryIdStr);
                }

                if(lpNotifications[i].info.obj.ulObjType == MAPI_MESSAGE
						&& MAPINotification_callCalendarUpdatedMethod != NULL
						&& type == CALENDAR_FOLDER_TYPE)
				{
					MAPINotification_callCalendarUpdatedMethod(entryIdStr);
				}
                ::free(entryIdStr);
                entryIdStr = NULL;

                // If the entry identifier has changed, then deletes the old
                // one.
                if(lpNotifications[i].info.obj.lpOldID != NULL
                        && lpNotifications[i].info.obj.cbOldID > 0)
                {
                    LPSTR oldEntryIdStr = (LPSTR)
                        ::malloc((lpNotifications[i].info.obj.cbOldID + 1) * 2);
                    HexFromBin(
                            (LPBYTE) lpNotifications[i].info.obj.lpOldID,
                            lpNotifications[i].info.obj.cbOldID,
                            oldEntryIdStr);
                    if(lpNotifications[i].info.obj.ulObjType == MAPI_MESSAGE
                            && MAPINotification_callDeletedMethod != NULL
                            && type == CONTACTS_FOLDER_TYPE)
                    {
                        MAPINotification_callDeletedMethod(oldEntryIdStr);
                    }

                    if(lpNotifications[i].info.obj.ulObjType == MAPI_MESSAGE
							&& MAPINotification_callCalendarDeletedMethod != NULL
							&& type == CALENDAR_FOLDER_TYPE)
                    {
                    	MAPINotification_callCalendarDeletedMethod(oldEntryIdStr);
					}
                    ::free(oldEntryIdStr);
                    oldEntryIdStr = NULL;
                }
            }
        }
        // A contact has been deleted.
        else if(lpNotifications[i].ulEventType == fnevObjectDeleted)
        {
            if(lpvContext != NULL)
            {
                LPSTR entryIdStr = (LPSTR)
                    ::malloc((lpNotifications[i].info.obj.cbEntryID + 1) * 2);

                HexFromBin(
                        (LPBYTE) lpNotifications[i].info.obj.lpEntryID,
                        lpNotifications[i].info.obj.cbEntryID,
                        entryIdStr);

                if(lpNotifications[i].info.obj.ulObjType == MAPI_MESSAGE
                        && MAPINotification_callDeletedMethod != NULL
                        && type == CONTACTS_FOLDER_TYPE)
                {
                    MAPINotification_callDeletedMethod(entryIdStr);
                }

                if(lpNotifications[i].info.obj.ulObjType == MAPI_MESSAGE
						&& MAPINotification_callCalendarDeletedMethod != NULL
						&& type == CALENDAR_FOLDER_TYPE)
				{
					MAPINotification_callCalendarDeletedMethod(entryIdStr);
				}

                ::free(entryIdStr);
                entryIdStr = NULL;
            }
        }
        // A contact has been deleted (moved to trash).
        else if(lpNotifications[i].ulEventType == fnevObjectMoved)
        {
            if(lpvContext != NULL)
            {
                LPSTR entryIdStr = (LPSTR)
                    ::malloc((lpNotifications[i].info.obj.cbEntryID + 1) * 2);
                HexFromBin(
                        (LPBYTE) lpNotifications[i].info.obj.lpEntryID,
                        lpNotifications[i].info.obj.cbEntryID,
                        entryIdStr);
                LPSTR parentEntryIdStr = (LPSTR)
                    ::malloc((lpNotifications[i].info.obj.cbParentID + 1) * 2);
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
                LPSTR wasteBasketEntryIdStr = (LPSTR)
                    ::malloc((wasteBasketProps[0].Value.bin.cb + 1) * 2);
                HexFromBin(
                        (LPBYTE) wasteBasketProps[0].Value.bin.lpb,
                        wasteBasketProps[0].Value.bin.cb,
                        wasteBasketEntryIdStr);

                if(lpNotifications[i].info.obj.ulObjType == MAPI_MESSAGE
                        && strcmp(parentEntryIdStr, wasteBasketEntryIdStr) == 0
                        && MAPINotification_callDeletedMethod != NULL
                        && type == CONTACTS_FOLDER_TYPE)
                {
                    MAPINotification_callDeletedMethod(entryIdStr);
                }

                if(lpNotifications[i].info.obj.ulObjType == MAPI_MESSAGE
                		&& strcmp(parentEntryIdStr, wasteBasketEntryIdStr) == 0
						&& MAPINotification_callCalendarDeletedMethod != NULL
						&& type == CALENDAR_FOLDER_TYPE)
				{
					MAPINotification_callCalendarDeletedMethod(entryIdStr);
				}

                ::free(entryIdStr);
                entryIdStr = NULL;
                ::free(parentEntryIdStr);
                parentEntryIdStr = NULL;
                ::free(wasteBasketEntryIdStr);
                wasteBasketEntryIdStr = NULL;
                MAPIFreeBuffer(wasteBasketProps);

                // If the entry identifier has changed, then deletes the old
                // one.
                if(lpNotifications[i].info.obj.lpOldID != NULL
                        && lpNotifications[i].info.obj.cbOldID > 0)
                {
                    LPSTR oldEntryIdStr = (LPSTR)
                        ::malloc((lpNotifications[i].info.obj.cbOldID + 1) * 2);
                    HexFromBin(
                            (LPBYTE) lpNotifications[i].info.obj.lpOldID,
                            lpNotifications[i].info.obj.cbOldID,
                            oldEntryIdStr);
                    if(lpNotifications[i].info.obj.ulObjType == MAPI_MESSAGE
                            && MAPINotification_callDeletedMethod != NULL
                            && type == CONTACTS_FOLDER_TYPE)
                    {
                        MAPINotification_callDeletedMethod(oldEntryIdStr);
                    }

                    if(lpNotifications[i].info.obj.ulObjType == MAPI_MESSAGE
							&& MAPINotification_callCalendarDeletedMethod != NULL
							&& type == CALENDAR_FOLDER_TYPE)
					{
						MAPINotification_callCalendarDeletedMethod(oldEntryIdStr);
					}
                    ::free(oldEntryIdStr);
                    oldEntryIdStr = NULL;
                }
            }
        }
    }

    // A client must always return a S_OK.
    return S_OK;
}

/**
 * Registers java callback functions when a contact is deleted, inserted or
 * updated.
 *
 * @param jniEnv The Java native interface environment.
 * @param notificationsDelegate The object called when a notification is fired
 * (contact updated, inserted or deleted).
 */
void
MAPINotification_registerJniNotificationsDelegate
    (JNIEnv *jniEnv, jobject notificationsDelegate)
{
    if(jniEnv->GetJavaVM(&MAPINotification_VM) < 0)
    {
        fprintf(stderr, "Failed to get the Java VM\n");
        fflush(stderr);
    }

    // If this function is called once again, then check first to unregister
    // previous notification advises.
    MAPINotification_unregisterJniNotificationsDelegate(jniEnv);

    if(notificationsDelegate != NULL)
    {
        MAPINotification_notificationsDelegateObject
            = jniEnv->NewGlobalRef(notificationsDelegate);
        if(MAPINotification_notificationsDelegateObject != NULL)
        {
            jclass callbackClass
                = jniEnv->GetObjectClass(notificationsDelegate);
            MAPINotification_notificationsDelegateMethodIdInserted
                = jniEnv->GetMethodID(
                        callbackClass,
                        "inserted",
                        "(Ljava/lang/String;)V");
            MAPINotification_notificationsDelegateMethodIdUpdated
                = jniEnv->GetMethodID(
                        callbackClass,
                        "updated",
                        "(Ljava/lang/String;)V");
            MAPINotification_notificationsDelegateMethodIdDeleted
                = jniEnv->GetMethodID(
                        callbackClass,
                        "deleted",
                        "(Ljava/lang/String;)V");
            MAPINotification_callDeletedMethod
                = MAPINotification_jniCallDeletedMethod;
            MAPINotification_callInsertedMethod
                = MAPINotification_jniCallInsertedMethod;
            MAPINotification_callUpdatedMethod
                = MAPINotification_jniCallUpdatedMethod;

            jniEnv->DeleteLocalRef(callbackClass);
        }
    }
}


/**
 * Registers java callback functions when a calendar item is deleted, inserted or
 * updated.
 *
 * @param jniEnv The Java native interface environment.
 * @param notificationsDelegate The object called when a notification is fired
 * (contact updated, inserted or deleted).
 */
void
MAPINotification_registerCalendarJniNotificationsDelegate
    (JNIEnv *jniEnv, jobject notificationsDelegate)
{
    if(jniEnv->GetJavaVM(&MAPINotification_VM) < 0)
    {
        fprintf(stderr, "Failed to get the Java VM\n");
        fflush(stderr);
    }
    MAPINotification_unregisterJniCalendarNotificationsDelegate(jniEnv);
    if(notificationsDelegate != NULL)
    {
        MAPINotification_notificationsDelegateCalendarObject
            = jniEnv->NewGlobalRef(notificationsDelegate);
        if(MAPINotification_notificationsDelegateCalendarObject != NULL)
        {
            jclass callbackClass
                = jniEnv->GetObjectClass(notificationsDelegate);
            MAPINotification_notificationsDelegateCalendarMethodIdInserted
                = jniEnv->GetMethodID(
                        callbackClass,
                        "inserted",
                        "(Ljava/lang/String;)V");
            MAPINotification_notificationsDelegateCalendarMethodIdUpdated
                = jniEnv->GetMethodID(
                        callbackClass,
                        "updated",
                        "(Ljava/lang/String;)V");
            MAPINotification_notificationsDelegateCalendarMethodIdDeleted
                = jniEnv->GetMethodID(
                        callbackClass,
                        "deleted",
                        "(Ljava/lang/String;)V");

            MAPINotification_callCalendarDeletedMethod
                = MAPINotification_jniCallCalendarDeletedMethod;
            MAPINotification_callCalendarInsertedMethod
                = MAPINotification_jniCallCalendarInsertedMethod;
            MAPINotification_callCalendarUpdatedMethod
                = MAPINotification_jniCallCalendarUpdatedMethod;

            jniEnv->DeleteLocalRef(callbackClass);
        }
    }
}
/**
 * Registers C callback functions when a contact is deleted, inserted or
 * updated.
 *
 * @param deletedMethod The method to call when a contact has been deleted.
 * @param insertedMethod The method to call when a contact has been inserted.
 * @param updatedMethod The method to call when a contact has been updated.
 */
void
MAPINotification_registerNativeNotificationsDelegate
    (void * deletedMethod, void * insertedMethod, void *updatedMethod)
{
    // If this function is called once again, then check first to unregister
    // previous notification advises.
    MAPINotification_unregisterNativeNotificationsDelegate();

    MAPINotification_callDeletedMethod = (void (*)(char*)) deletedMethod;
    MAPINotification_callInsertedMethod = (void (*)(char*)) insertedMethod;
    MAPINotification_callUpdatedMethod = (void (*)(char*)) updatedMethod;
}

/**
 * Registers C callback functions when a contact is deleted, inserted or
 * updated.
 *
 * @param deletedMethod The method to call when a contact has been deleted.
 * @param insertedMethod The method to call when a contact has been inserted.
 * @param updatedMethod The method to call when a contact has been updated.
 */
void
MAPINotification_registerCalendarNativeNotificationsDelegate
    (void * deletedMethod, void * insertedMethod, void *updatedMethod)
{
    // If this function is called once again, then check first to unregister
    // previous notification advises.
    MAPINotification_unregisterNativeCalendarNotificationsDelegate();

    MAPINotification_callCalendarDeletedMethod = (void (*)(char*)) deletedMethod;
    MAPINotification_callCalendarInsertedMethod = (void (*)(char*)) insertedMethod;
    MAPINotification_callCalendarUpdatedMethod = (void (*)(char*)) updatedMethod;
}

/**
 * Opens all the message store and register to notifications.
 *
 * @param mapiSession The current MAPI session.
 */
void MAPINotification_registerNotifyAllMsgStores(LPMAPISESSION mapiSession)
{
    HRESULT hResult;

    hResult = mapiSession->GetMsgStoresTable(
            0, 
            &MAPINotification_msgStoresTable);
    if(HR_SUCCEEDED(hResult) && MAPINotification_msgStoresTable)
    {
        MAPINotification_msgStoresTableConnection
            = MAPINotification_registerNotifyTable(
                    MAPINotification_msgStoresTable,
                    &MAPINotification_msgStoresTableAdviseSink);
        hResult = MAPINotification_msgStoresTable->SeekRow(
                BOOKMARK_BEGINNING,
                0,
                NULL);
        if (HR_SUCCEEDED(hResult))
        {
            LPSRowSet rows;
            hResult = HrQueryAllRows(
                    MAPINotification_msgStoresTable,
                    NULL,
                    NULL,
                    NULL,
                    0,
                    &rows);
            if (HR_SUCCEEDED(hResult))
            {
                MAPINotification_nbMsgStores = rows->cRows;
                MAPINotification_msgStores
                    = (LPMDB*) malloc(rows->cRows * sizeof(LPMDB));
                memset(
                        MAPINotification_msgStores,
                        0,
                        rows->cRows * sizeof(LPMDB));
                MAPINotification_msgStoresConnection
                    = (ULONG*) malloc(rows->cRows * sizeof(ULONG));
                memset(
                        MAPINotification_msgStoresConnection,
                        0,
                        rows->cRows * sizeof(ULONG));
                MAPINotification_adviseSinks = (LPMAPIADVISESINK*)
                    malloc(rows->cRows * sizeof(LPMAPIADVISESINK));
                memset(
                        MAPINotification_adviseSinks,
                        0,
                        rows->cRows * sizeof(LPMAPIADVISESINK));

                MAPINotification_calendarMsgStoresConnection
				   = (ULONG*) malloc(rows->cRows * sizeof(ULONG));
				memset(
						MAPINotification_calendarMsgStoresConnection,
					   0,
					   rows->cRows * sizeof(ULONG));
				MAPINotification_calendarAdviseSinks = (LPMAPIADVISESINK*)
				   malloc(rows->cRows * sizeof(LPMAPIADVISESINK));
				memset(
						MAPINotification_calendarAdviseSinks,
					   0,
					   rows->cRows * sizeof(LPMAPIADVISESINK));

                if(MAPINotification_msgStores != NULL
                        && MAPINotification_msgStoresConnection != NULL
                        && MAPINotification_calendarAdviseSinks != NULL
                        && MAPINotification_calendarMsgStoresConnection != NULL)
                {
                    for(unsigned int r = 0; r < rows->cRows; ++r)
                    {
                        SRow row = rows->aRow[r];
                        ULONG i;
                        ULONG objType = 0;
                        SBinary entryIDBinary = { 0, NULL };

                        for(i = 0; i < row.cValues; ++i)
                        {
                            LPSPropValue prop = (row.lpProps) + i;

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

                        if(objType && entryIDBinary.cb && entryIDBinary.lpb)
                        {
                            hResult = mapiSession->OpenMsgStore(
                                    0,
                                    entryIDBinary.cb,
                                    (LPENTRYID) entryIDBinary.lpb,
                                    NULL,
                                    MDB_NO_MAIL
                                        | MAPINotification_openEntryUlFlags,
                                    &MAPINotification_msgStores[r]);
                            if (HR_SUCCEEDED(hResult))
                            {
                                MAPINotification_msgStoresConnection[r]
                                    = MAPINotification_registerNotifyMessageDataBase(
                                            MAPINotification_msgStores[r],
                                            &MAPINotification_adviseSinks[r],
                                            CONTACTS_FOLDER_TYPE,
                                            &MAPINotification_contactsOnNotify);

                                MAPINotification_calendarMsgStoresConnection[r]
								   = MAPINotification_registerNotifyMessageDataBase(
										   MAPINotification_msgStores[r],
										   &MAPINotification_calendarAdviseSinks[r],
										   CALENDAR_FOLDER_TYPE,
										   &MAPINotification_calendarOnNotify);
                            }
                        }
                    }
                }
                FreeProws(rows);
            }
        }
    }
}


/**
 * Registers to notification for the given message data base.
 *
 * @param iUnknown The data base to register to in order to receive events.
 * @param adviseSink The advice sink that will be generated resulting o fthis
 * function call.
 *
 * @return A unsigned long which is a token wich must be used to call the
 * unadvise function for the same message data base.
 */
ULONG MAPINotification_registerNotifyMessageDataBase(
        LPMDB iUnknown,
        LPMAPIADVISESINK * adviseSink,
        ULONG type,
        MAPINotification_callback callback)
{

    HrAllocAdviseSink(callback, iUnknown, adviseSink);

    LPENTRYID receiveFolderEntryID = NULL;
	ULONG entrySize = 0;
	LPENTRYID entryID = NULL;
	ULONG receiveFolderIDByteCount = 0;
	HRESULT hResult;
	hResult = iUnknown->GetReceiveFolder(
				NULL,
				0,
				&receiveFolderIDByteCount,
				&receiveFolderEntryID,
				NULL);
	if (HR_SUCCEEDED(hResult))
	{
		hResult = MsOutlookUtils_getFolderEntryIDByType(
					iUnknown,
					receiveFolderIDByteCount,
					receiveFolderEntryID,
					&entrySize,
					&entryID,
					0,
					type);
		MAPIFreeBuffer(receiveFolderEntryID);
	}
	if (HR_FAILED(hResult))
	{
		hResult = MsOutlookUtils_getFolderEntryIDByType(
					iUnknown,
					0,
					NULL,
					&entrySize,
					&entryID,
					0,
					type);
	}

	ULONG nbConnection = 0;
	if (HR_SUCCEEDED(hResult))
	{
		MsOutlookUtils_log(
				"Successfully found folder to register notifications.");
		iUnknown->Advise(
				(ULONG) entrySize,
				(LPENTRYID) entryID,
				MAPINotification_EVENT_MASK,
				*adviseSink,
				(ULONG *) &nbConnection);
	}
	else
	{
		if(type == CONTACTS_FOLDER_TYPE)
		{
			MsOutlookUtils_log(
					"Contacts folder for the message store doesn't exists");
		}
		else
		{
			MsOutlookUtils_log(
					"Calendar folder for the message store doesn't exists");
		}
	}

    return nbConnection;
}

/**
 * Registers a callback function for when the message store table changes.
 *
 * @param iUnknown The message store table to register to in order to receive
 * events.
 * @param adviseSink The advice sink that will be generated resulting o fthis
 * function call.
 *
 * @return A unsigned long which is a token wich must be used to call the
 * unadvise function for the same message store table.
 */
ULONG MAPINotification_registerNotifyTable(
        LPMAPITABLE iUnknown,
        LPMAPIADVISESINK * adviseSink)
{
    HrAllocAdviseSink(&MAPINotification_tableChanged, iUnknown, adviseSink);
    ULONG nbConnection = 0;
    iUnknown->Advise(fnevTableModified, *adviseSink, (ULONG *) &nbConnection);

    return nbConnection;
}

/**
 * Function called when a message store table changed.
 */
LONG
STDAPICALLTYPE MAPINotification_tableChanged
    (LPVOID lpvContext, ULONG cNotifications, LPNOTIFICATION lpNotifications)
{
    if(lpNotifications->ulEventType == fnevTableModified
            && (lpNotifications->info.tab.ulTableEvent == TABLE_CHANGED
                || lpNotifications->info.tab.ulTableEvent == TABLE_ERROR
                || lpNotifications->info.tab.ulTableEvent == TABLE_RELOAD
                || lpNotifications->info.tab.ulTableEvent == TABLE_ROW_ADDED
                || lpNotifications->info.tab.ulTableEvent == TABLE_ROW_DELETED))
    {
        // Frees and recreates all the notification for the table.
        MAPINotification_unregisterNotifyAllMsgStores();
        MAPINotification_registerNotifyAllMsgStores(
                MAPISession_getMapiSession());
    }

    // A client must always return a S_OK.
    return S_OK;
}

/**
 * Unregisters java callback functions when a contact is deleted, inserted or
 * updated.
 *
 * @param jniEnv The Java native interface environment.
 */
void MAPINotification_unregisterJniNotificationsDelegate(JNIEnv *jniEnv)
{
    if(MAPINotification_notificationsDelegateObject != NULL)
    {
        jniEnv->DeleteGlobalRef(MAPINotification_notificationsDelegateObject);
        MAPINotification_notificationsDelegateObject = NULL;
        MAPINotification_notificationsDelegateMethodIdInserted = NULL;
        MAPINotification_notificationsDelegateMethodIdUpdated = NULL;
        MAPINotification_notificationsDelegateMethodIdDeleted = NULL;
    }
}

void MAPINotification_unregisterJniCalendarNotificationsDelegate(JNIEnv *jniEnv)
{
    if(MAPINotification_notificationsDelegateCalendarObject != NULL)
    {
        jniEnv->DeleteGlobalRef(MAPINotification_notificationsDelegateCalendarObject);
        MAPINotification_notificationsDelegateCalendarObject = NULL;
        MAPINotification_notificationsDelegateCalendarMethodIdInserted = NULL;
        MAPINotification_notificationsDelegateCalendarMethodIdUpdated = NULL;
        MAPINotification_notificationsDelegateCalendarMethodIdDeleted = NULL;
    }
}

/**
 * Unregisters C callback functions when a contact is deleted, inserted or
 * updated.
 */
void MAPINotification_unregisterNativeNotificationsDelegate()
{
}

void MAPINotification_unregisterNativeCalendarNotificationsDelegate()
{
}

/**
 * Frees all memory used to keep in mind the list of the message store and
 * unregister each of them from the notifications.
 */
void MAPINotification_unregisterNotifyAllMsgStores(void)
{
    if(MAPINotification_msgStoresConnection != NULL)
    {
        for(unsigned int i = 0; i < MAPINotification_nbMsgStores; ++i)
        {
            if(MAPINotification_msgStoresConnection[i] != 0)
            {
                MAPINotification_adviseSinks[i]->Release();
                MAPINotification_msgStores[i]->Unadvise(
                        MAPINotification_msgStoresConnection[i]);
            }
        }
        free(MAPINotification_adviseSinks);
        MAPINotification_adviseSinks = NULL;
        free(MAPINotification_msgStoresConnection);
        MAPINotification_msgStoresConnection = NULL;
    }

    if(MAPINotification_calendarMsgStoresConnection != NULL)
	{
		for(unsigned int i = 0; i < MAPINotification_nbMsgStores; ++i)
		{
			if(MAPINotification_calendarMsgStoresConnection[i] != 0)
			{
				MAPINotification_calendarAdviseSinks[i]->Release();
				MAPINotification_msgStores[i]->Unadvise(
						MAPINotification_calendarMsgStoresConnection[i]);
			}
		}
		free(MAPINotification_calendarAdviseSinks);
		MAPINotification_calendarAdviseSinks = NULL;
		free(MAPINotification_calendarMsgStoresConnection);
		MAPINotification_calendarMsgStoresConnection = NULL;
	}

    if(MAPINotification_msgStores != NULL)
    {
        for(unsigned int i = 0; i < MAPINotification_nbMsgStores; ++i)
        {
            if(MAPINotification_msgStores[i] != NULL)
            {
                MAPINotification_msgStores[i]->Release();
            }
        }
        free(MAPINotification_msgStores);
        MAPINotification_msgStores = NULL;
    }

    if(MAPINotification_msgStoresTable != NULL)
    {
        MAPINotification_msgStoresTableAdviseSink->Release();
        MAPINotification_msgStoresTableAdviseSink = NULL;
        MAPINotification_msgStoresTable->Unadvise(
                MAPINotification_msgStoresTableConnection);
        MAPINotification_msgStoresTable->Release();
        MAPINotification_msgStoresTable = NULL;
    }
}

