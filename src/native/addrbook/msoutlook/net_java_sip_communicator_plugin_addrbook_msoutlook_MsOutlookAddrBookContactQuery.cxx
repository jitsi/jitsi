/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#include "net_java_sip_communicator_plugin_addrbook_msoutlook_MsOutlookAddrBookContactQuery.h"

#include "MsOutlookAddrBookContactQuery.h"
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
    const char *nativeQuery = jniEnv->GetStringUTFChars(query, NULL);

    IMsOutlookAddrBookServer * iServer = ComClient_getIServer();
    if(iServer)
    {
        LPWSTR unicodeQuery = StringUtils::MultiByteToWideChar(nativeQuery);
        BSTR comQuery = SysAllocString(unicodeQuery);

        iServer->foreachMailUser(comQuery);
        SysFreeString(comQuery);
        free(unicodeQuery);
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
    const char *nativeValue = jniEnv->GetStringUTFChars(value, NULL);

    IMsOutlookAddrBookServer * iServer = ComClient_getIServer();
    if(iServer)
    {
        LPWSTR unicodeValue
            = StringUtils::MultiByteToWideChar(nativeValue);
        BSTR comValue = SysAllocString(unicodeValue);
        LPWSTR unicodeId = StringUtils::MultiByteToWideChar(nativeEntryId);
        BSTR comId = SysAllocString(unicodeId);

        res = (iServer->IMAPIProp_SetPropString(propId, comValue, comId)
                == S_OK);

        SysFreeString(comId);
        free(unicodeId);
        SysFreeString(comValue);
        free(unicodeValue);
    }

    jniEnv->ReleaseStringUTFChars(entryId, nativeEntryId);
    jniEnv->ReleaseStringUTFChars(value, nativeValue);

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
    HRESULT hr = E_FAIL;
    jobjectArray javaProps = NULL;
    const char *nativeEntryId = jniEnv->GetStringUTFChars(entryId, NULL);
    jsize propIdCount = jniEnv->GetArrayLength(propIds);
    long nativePropIds[propIdCount];

    for(int i = 0; i < propIdCount; ++i)
    {
        jlong propId;

        jniEnv->GetLongArrayRegion(propIds, i, 1, &propId);
        nativePropIds[i] = propId;
    }

    if(jniEnv->ExceptionCheck())
    {
        return NULL;
    }

    void ** props = NULL;
    unsigned long* propsLength = NULL;
    // b = byteArray, l = long, s = 8 bits string, u = 16 bits string.
    char * propsType = NULL;

    if((props = (void**) malloc(propIdCount * sizeof(void*))) != NULL)
    {
        memset(props, 0, propIdCount * sizeof(void*));
        if((propsLength = (unsigned long*) malloc(
                        propIdCount * sizeof(unsigned long))) != NULL)
        {
            if((propsType = (char*) malloc(propIdCount * sizeof(char)))
                    != NULL)
            {
                IMsOutlookAddrBookServer * iServer = ComClient_getIServer();
                if(iServer)
                {
                    LPWSTR unicodeEntryId
                        = StringUtils::MultiByteToWideChar(nativeEntryId);
                    BSTR comEntryId = SysAllocString(unicodeEntryId);

                    LPSAFEARRAY comPropIds
                        = SafeArrayCreateVector(VT_I4, 0, propIdCount);
                    SafeArrayLock(comPropIds);
                    comPropIds->pvData = nativePropIds;
                    SafeArrayUnlock(comPropIds);

                    LPSAFEARRAY comProps;
                    LPSAFEARRAY comPropsLength;
                    LPSAFEARRAY comPropsType;

                    hr = iServer->IMAPIProp_GetProps(
                            comEntryId,
                            propIdCount,
                            comPropIds,
                            flags,
                            &comProps,
                            &comPropsLength,
                            &comPropsType);

                    if(HR_SUCCEEDED(hr))
                    {
                        SafeArrayLock(comPropsType);
                        memcpy(
                                propsType,
                                comPropsType->pvData,
                                propIdCount * sizeof(char));
                        SafeArrayUnlock(comPropsType);

                        SafeArrayLock(comPropsLength);
                        memcpy(
                                propsLength,
                                comPropsLength->pvData,
                                propIdCount * sizeof(unsigned long));
                        SafeArrayUnlock(comPropsLength);

                        SafeArrayLock(comProps);
                        byte * data = (byte*) comProps->pvData;
                        for(int j = 0; j < propIdCount; ++j)
                        {
                            if((props[j] = malloc(propsLength[j])) != NULL)
                            {
                                memcpy(props[j], data, propsLength[j]);
                                data += propsLength[j];
                            }
                        }
                        SafeArrayUnlock(comProps);

                        // Decode properties to java
                        jclass objectClass
                            = jniEnv->FindClass("java/lang/Object");
                        if (objectClass)
                        {
                            javaProps = jniEnv->NewObjectArray(
                                    propIdCount,
                                    objectClass,
                                    NULL);
                            for(int j = 0; j < propIdCount; ++j)
                            {
                                // byte array
                                if(propsType[j] == 'b' && props[j] != NULL)
                                {
                                    jbyteArray value = jniEnv->NewByteArray(
                                                (jsize) propsLength[j]);
                                    if(value)
                                    {
                                        jbyte *bytes
                                            = jniEnv->GetByteArrayElements(
                                                    value, NULL);

                                        if (bytes)
                                        {
                                            memcpy(
                                                    bytes,
                                                    props[j],
                                                    propsLength[j]);
                                            jniEnv->ReleaseByteArrayElements(
                                                    value,
                                                    bytes,
                                                    0);
                                            jniEnv->SetObjectArrayElement(
                                                    javaProps,
                                                    j,
                                                    value);
                                        }
                                    }
                                }
                                // long
                                else if(propsType[j] == 'l' && props[j] != NULL)
                                {
                                    jclass longClass
                                        = jniEnv->FindClass("java/lang/Long");
                                    if (longClass)
                                    {
                                        jmethodID longMethodID
                                            = jniEnv->GetMethodID(
                                                longClass,
                                                "<init>",
                                                "(J)V");

                                        if (longMethodID)
                                        {
                                            jlong l;
                                            memcpy(&l, props[j], propsLength[j]);
                                            jobject value = jniEnv->NewObject(
                                                    longClass,
                                                    longMethodID,
                                                    l);

                                            if (value)
                                            {
                                                jniEnv->SetObjectArrayElement(
                                                        javaProps,
                                                        j,
                                                        value);
                                            }
                                        }
                                    }
                                }
                                // 8 bits string
                                else if(propsType[j] == 's' && props[j] != NULL)
                                {
                                    jstring value = jniEnv->NewStringUTF(
                                            (const char*) props[j]);
                                    if (value)
                                    {
                                        jniEnv->SetObjectArrayElement(
                                                javaProps,
                                                j,
                                                value);
                                    }
                                }
                                // 16 bits string
                                else if(propsType[j] == 'u' && props[j] != NULL)
                                {
                                    jstring value
                                        = jniEnv->NewString(
                                            (const jchar *) props[j],
                                            wcslen((const wchar_t *) props[j]));
                                    if (value)
                                    {
                                        jniEnv->SetObjectArrayElement(
                                                javaProps,
                                                j,
                                                value);
                                    }
                                }

                                if(jniEnv->ExceptionCheck())
                                    javaProps = NULL;
                            }
                        }
                    }
                    else
                    {
                        MsOutlookMAPIHResultException_throwNew(
                                jniEnv,
                                hr,
                                __FILE__, __LINE__);
                    }

                    SafeArrayDestroy(comPropsType);
                    SafeArrayDestroy(comPropsLength);
                    SafeArrayDestroy(comProps);
                    SafeArrayDestroy(comPropIds);
                    SysFreeString(comEntryId);
                    free(unicodeEntryId);
                }


                for(int j = 0; j < propIdCount; ++j)
                {
                    if(props[j] != NULL)
                        free(props[j]);
                }
                free(propsType);
            }
            free(propsLength);
        }
        free(props);
    }

    jniEnv->ReleaseStringUTFChars(entryId, nativeEntryId);

    return javaProps;
}
