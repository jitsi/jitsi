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

#include "net_java_sip_communicator_plugin_addrbook_msoutlook_MsOutlookAddrBookContactQuery.h"

#include "MsOutlookAddrBookContactQuery.h"
#include "MsOutlookUtils.h"
#include "com/ComClient.h"
#include "com/MsOutlookAddrBookServer.h"
#include "MAPIBitness.h"
#include "MAPINotification.h"
#include "MsOutlookMAPIHResultException.h"
#include "StringUtils.h"

#include <Mapix.h>

/**
 * Creates a new contact from the outlook database.
 *
 * @param jniEnv The Java native interface environment.
 * @param clazz A Java class Object.
 *
 * @return The identifer of the created outlook contact. NULL on failure.
 */
JNIEXPORT jstring JNICALL Java_net_java_sip_communicator_plugin_addrbook_msoutlook_MsOutlookAddrBookContactQuery_createContact
  (JNIEnv *jniEnv, jclass clazz)
{
    jstring value = NULL;
    char* messageIdStr = NULL;

    IMsOutlookAddrBookServer * iServer = ComClient_getIServer();
    if(iServer)
    {
        BSTR id;
        iServer->createContact(&id);
        if(id != NULL)
        {
            messageIdStr = StringUtils::WideCharToMultiByte(id);
            SysFreeString(id);
        }
    }

    if(messageIdStr != NULL)
    {
        value = jniEnv->NewStringUTF(messageIdStr);
        ::free(messageIdStr);
        messageIdStr = NULL;
    }

    return value;
}

/**
 * Delete the given contact from the outlook database.
 *
 * @param jniEnv The Java native interface environment.
 * @param clazz A Java class Object.
 * @param id The identifer of the outlook contact to remove.
 *
 * @return JNI_TRUE if the deletion succeded. JNI_FALSE otherwise.
 */
JNIEXPORT jboolean JNICALL Java_net_java_sip_communicator_plugin_addrbook_msoutlook_MsOutlookAddrBookContactQuery_deleteContact
  (JNIEnv *jniEnv, jclass clazz, jstring id)
{
    const char *nativeEntryId = jniEnv->GetStringUTFChars(id, NULL);
    jboolean res = JNI_FALSE;

    IMsOutlookAddrBookServer * iServer = ComClient_getIServer();
    if(iServer)
    {
        LPWSTR unicodeId = StringUtils::MultiByteToWideChar(nativeEntryId);
        BSTR comId = SysAllocString(unicodeId);

        res = (iServer->deleteContact(comId) == S_OK);

        SysFreeString(comId);
        free(unicodeId);
    }

    jniEnv->ReleaseStringUTFChars(id, nativeEntryId);

    return res;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_plugin_addrbook_msoutlook_MsOutlookAddrBookContactQuery_foreachMailUser(
        JNIEnv *jniEnv,
        jclass clazz,
        jstring query,
        jobject callback)
{
	MsOutlookUtils_log("Executing query.");
    const char *nativeQuery = jniEnv->GetStringUTFChars(query, NULL);

    IMsOutlookAddrBookServer * iServer = ComClient_getIServer();
    if(iServer)
    {
        LPWSTR unicodeQuery = StringUtils::MultiByteToWideChar(nativeQuery);
        BSTR comQuery = SysAllocString(unicodeQuery);
        MsOutlookUtils_log("Sending the query to server.");
        iServer->foreachMailUser(comQuery, (long)(intptr_t)callback);
        SysFreeString(comQuery);
        free(unicodeQuery);
    }
    else
    {
    	MsOutlookUtils_log("Failed to execute the query because the COM Server is not available.");
    }

    jniEnv->ReleaseStringUTFChars(query, nativeQuery);
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
    jboolean res = JNI_FALSE;

    const char *nativeEntryId = jniEnv->GetStringUTFChars(entryId, NULL);

    IMsOutlookAddrBookServer * iServer = ComClient_getIServer();
    if(iServer)
    {
        LPWSTR unicodeId = StringUtils::MultiByteToWideChar(nativeEntryId);
        BSTR comId = SysAllocString(unicodeId);

        res = (iServer->IMAPIProp_DeleteProp(propId, comId) == S_OK);

        SysFreeString(comId);
        free(unicodeId);
    }

    jniEnv->ReleaseStringUTFChars(entryId, nativeEntryId);

    return res;
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
    jboolean res = JNI_FALSE;
    const char *nativeEntryId = jniEnv->GetStringUTFChars(entryId, NULL);

    IMsOutlookAddrBookServer * iServer = ComClient_getIServer();
    if(iServer)
    {
        const LPWSTR unicodeValue
            = (const LPWSTR) jniEnv->GetStringChars(value, NULL);
        BSTR comValue = SysAllocString(unicodeValue);
        LPWSTR unicodeId = StringUtils::MultiByteToWideChar(nativeEntryId);
        BSTR comId = SysAllocString(unicodeId);

        res = (iServer->IMAPIProp_SetPropString(propId, comValue, comId)
                == S_OK);

        SysFreeString(comId);
        free(unicodeId);
        SysFreeString(comValue);
        jniEnv->ReleaseStringChars(value, (const jchar*) unicodeValue);
    }

    jniEnv->ReleaseStringUTFChars(entryId, nativeEntryId);

    return res;
}

JNIEXPORT jobjectArray JNICALL
Java_net_java_sip_communicator_plugin_addrbook_msoutlook_MsOutlookAddrBookContactQuery_IMAPIProp_1GetProps(
        JNIEnv *jniEnv,
        jclass clazz,
        jstring entryId,
        jlongArray propIds,
        jlong flags)
{
	return MsOutlookUtils_IMAPIProp_GetProps(jniEnv, clazz, entryId, propIds,
			flags, MsOutlookAddrBookContactQuery_UUID_Address);
}


/**
 * Compares two identifiers to determine if they are part of the same
 * Outlook contact.
 *
 * @param jniEnv The Java native interface environment.
 * @param clazz A Java class Object.
 * @param id1 The first identifier.
 * @param id2 The second identifier.
 *
 * @return True if id1 and id2 are two identifiers of the same contact.
 * False otherwise.
 */
JNIEXPORT jboolean JNICALL
Java_net_java_sip_communicator_plugin_addrbook_msoutlook_MsOutlookAddrBookContactQuery_compareEntryIds
  (JNIEnv *jniEnv, jclass clazz, jstring id1, jstring id2)
{
    jboolean res = JNI_FALSE;

    if(id1 == NULL || id2 == NULL)
    {
        return res;
    }

    const char *nativeId1 = jniEnv->GetStringUTFChars(id1, NULL);
    const char *nativeId2 = jniEnv->GetStringUTFChars(id2, NULL);

    IMsOutlookAddrBookServer * iServer = ComClient_getIServer();
    if(iServer)
    {
        LPWSTR unicodeId1 = StringUtils::MultiByteToWideChar(nativeId1);
        LPWSTR unicodeId2 = StringUtils::MultiByteToWideChar(nativeId2);
        BSTR comId1 = SysAllocString(unicodeId1);
        BSTR comId2 = SysAllocString(unicodeId2);

        int result = 0;
        if(iServer->compareEntryIds(comId1, comId2, &result) == S_OK)
        {
            res = (result == 1);
        }

        SysFreeString(comId1);
        SysFreeString(comId2);
        free(unicodeId1);
        free(unicodeId2);
    }

    jniEnv->ReleaseStringUTFChars(id1, nativeId1);
    jniEnv->ReleaseStringUTFChars(id2, nativeId2);

    return res;
}
