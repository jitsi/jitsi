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

#include "MsOutlookUtils.h"
#include "MsOutlookAddrBookContactSourceService.h"

#include "MAPIBitness.h"
#include "MAPISession.h"

#include "com/ComClient.h"
#include "com/MsOutlookAddrBookServer.h"
#include "MsOutlookMAPIHResultException.h"
#include "StringUtils.h"

#include <initguid.h>
#include <jni.h>
#include <Mapidefs.h>
#include <Mapix.h>
#include <windows.h>
#include "Logger.h"

static Logger* logger = NULL;


HRESULT
MsOutlookUtils_getFolderEntryIDByType
    (LPMDB msgStore,
    ULONG folderEntryIDByteCount, LPENTRYID folderEntryID,
    ULONG *contactsFolderEntryIDByteCount, LPENTRYID *contactsFolderEntryID,
    ULONG flags, ULONG type)
{
    HRESULT hResult;
    ULONG objType;
    LPUNKNOWN folder;

    hResult = msgStore->OpenEntry(
            folderEntryIDByteCount,
            folderEntryID,
            NULL,
            flags,
            &objType,
            &folder);

    if (HR_SUCCEEDED(hResult))
    {
        LPSPropValue prop;

        hResult
            = MsOutlookUtils_HrGetOneProp(
                    (LPMAPIPROP) folder,
                    type,
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
            {
            	MsOutlookUtils_log("MsOutlookUtils_getFolderEntryIDByType: Not enough memory.");
                hResult = MAPI_E_NOT_ENOUGH_MEMORY;
            }
            MAPIFreeBuffer(prop);
        }
        else
        {
        	MsOutlookUtils_log("MsOutlookUtils_getFolderEntryIDByType: Error getting the property.");
        }
        folder->Release();
    }
    else
    {
    	MsOutlookUtils_log("MsOutlookUtils_getFolderEntryIDByType: Error opening the folder.");
    }
    return hResult;
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
HRESULT
MsOutlookUtils_HrGetOneProp(
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
        {
        	MsOutlookUtils_log("MsOutlookUtils_HrGetOneProp: Property not found.");
            hResult = MAPI_E_NOT_FOUND;
        }
        MAPIFreeBuffer(values);
    }
    else
    {
    	MsOutlookUtils_log("MsOutlookUtils_HrGetOneProp: MAPI getProps error.");
    }
    return hResult;
}


jobjectArray
MsOutlookUtils_IMAPIProp_GetProps(
        JNIEnv *jniEnv,
        jclass clazz,
        jstring entryId,
        jlongArray propIds,
        jlong flags,
        UUID UUID_Address)
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
        jniEnv->ReleaseStringUTFChars(entryId, nativeEntryId);
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
                            UUID_Address,
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
                                            jlong l = (jlong)(*((long*)props[j]));
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
                                else if(propsType[j] == 'B' && props[j] != NULL)
                                {
                                	jclass booleanClass
                                		= jniEnv->FindClass("java/lang/Boolean");
                                	jmethodID boolMethodID
                                		= jniEnv->GetStaticMethodID(
                                				booleanClass,
                                				"valueOf",
                                				"(Z)Ljava/lang/Boolean;");
                                	bool value = false;
                                	if((bool)props[j])
                                		value = true;
                                	jobject jValue
                                		= jniEnv->CallStaticObjectMethod(
                                				booleanClass,
                                				boolMethodID,
                                				value);
									jniEnv->SetObjectArrayElement(
											javaProps,
											j,
											jValue);
                                }
                                else if(propsType[j] == 't' && props[j] != NULL)
                                {	char dateTime[20];
                                	LPSYSTEMTIME sysTime
                                		= (LPSYSTEMTIME) props[j];
                                	sprintf(dateTime,
                                			"%u-%02u-%02u %02u:%02u:%02u",
                                			sysTime->wYear, sysTime->wMonth,
                                			sysTime->wDay, sysTime->wHour,
                                			sysTime->wMinute, sysTime->wSecond);
                                	jstring value = jniEnv->NewStringUTF(
											(const char*) dateTime);
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
                    	MsOutlookUtils_log("Error in the server call for getting properties.");
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
                else
                {
                	MsOutlookUtils_log("Server is not available.");
                }


                for(int j = 0; j < propIdCount; ++j)
                {
                    if(props[j] != NULL)
                        free(props[j]);
                }
                free(propsType);
            }
            else
            {
            	MsOutlookUtils_log("Allocating memory error.[1]");
            }
            free(propsLength);
        }
        else
		{
			MsOutlookUtils_log("Allocating memory error.[2]");
		}
        free(props);
    }
    else
	{
		MsOutlookUtils_log("Allocating memory error.[3]");
	}

    jniEnv->ReleaseStringUTFChars(entryId, nativeEntryId);

    return javaProps;
}

void MsOutlookUtils_createLogger(const char* logFile, const char* logPath,
		int logLevel)
{
	logger = new Logger(logFile, logPath, logLevel);
}

void MsOutlookUtils_log(const char* message)
{
	if(logger != NULL)
		logger->log(message);
}

void MsOutlookUtils_logInfo(const char* message)
{
	if(logger != NULL)
		logger->logInfo(message);
}

void MsOutlookUtils_deleteLogger()
{
	if(logger != NULL)
		free(logger);
}

char* MsOutlookUtils_getLoggerPath()
{
	if(logger != NULL)
		return logger->getLogPath();
	return NULL;
}

int MsOutlookUtils_getLoggerLevel()
{
	if(logger != NULL)
		return logger->getLogLevel();
	return 0;
}


static jboolean
MsOutlookUtils_isValidDefaultMailClient
    (LPCTSTR name, DWORD nameLength)
{
    jboolean validDefaultMailClient = JNI_FALSE;
    MsOutlookUtils_logInfo("We are validating the default mail client.");
    if ((0 != nameLength) && (0 != name[0]))
    {
        LPTSTR str;
        TCHAR keyName[
                22 /* Software\Clients\Mail\ */
                    + 255
                    + 1 /* The terminating null character */];
        HKEY key;

        str = keyName;
        _tcsncpy(str, _T("Software\\Clients\\Mail\\"), 22);
        str += 22;
        if (nameLength > 255)
            nameLength = 255;
        _tcsncpy(str, name, nameLength);
        *(str + nameLength) = 0;

        MsOutlookUtils_logInfo("We are searching in HKLM for the key");
        MsOutlookUtils_logInfo(keyName);
        if (ERROR_SUCCESS
                == RegOpenKeyEx(
                        HKEY_LOCAL_MACHINE,
                        keyName,
                        0,
                        KEY_QUERY_VALUE,
                        &key))
        {
        	MsOutlookUtils_logInfo("The key is found");
            validDefaultMailClient = JNI_TRUE;
            RegCloseKey(key);
        }
		else
		{
			MsOutlookUtils_logInfo("The key for default mail client is not found");
		}
    }
    return validDefaultMailClient;
}

bool MsOutlookUtils_isOutlookDefaultMailClient()
{
	MsOutlookUtils_logInfo("Outlook is installed and we are checking if it is default mail client.");

	boolean result = false;
	HKEY regKey;
	DWORD defaultValueType;
	TCHAR defaultValueBuffer[261];
	LPTSTR defaultValue = (LPTSTR) defaultValueBuffer;
	DWORD defaultValueCapacity = sizeof(defaultValueBuffer);

	if (ERROR_SUCCESS
			== RegOpenKeyEx(
					HKEY_CURRENT_USER,
					_T("Software\\Clients\\Mail"),
					0,
					KEY_QUERY_VALUE,
					&regKey))
	{
		MsOutlookUtils_logInfo("HKCU\\Software\\Clients\\Mail exists.");
		DWORD defaultValueSize = defaultValueCapacity;
		LONG regQueryValueEx = RegQueryValueEx(
				regKey,
				NULL,
				NULL,
				&defaultValueType,
				(LPBYTE) defaultValue,
				&defaultValueSize);

		switch (regQueryValueEx)
		{
		case ERROR_SUCCESS:
		{
			if (REG_SZ == defaultValueType)
			{
				DWORD defaultValueLength
					= defaultValueSize / sizeof(TCHAR);

				if (JNI_TRUE
						== MsOutlookUtils_isValidDefaultMailClient(
								defaultValue,
								defaultValueLength))
				{
					if (_tcsnicmp(
								_T("Microsoft Outlook"), defaultValue,
								defaultValueLength)
							== 0)
					{
						MsOutlookUtils_logInfo("The default value of HKCU\\Software\\Clients\\Mail is Microsoft Office .");
						result = true;
					}
					else
					{
						MsOutlookUtils_logInfo("The default value of HKCU\\Software\\Clients\\Mail is not Microsoft Office .");
						MsOutlookUtils_logInfo(defaultValue);
					}
				}
				else
				{
					MsOutlookUtils_logInfo("Not valid default mail client for the default value of HKCU\\Software\\Clients\\Mail .");
				}
			}
			else
			{
				MsOutlookUtils_logInfo("Wrong type for the default value of HKCU\\Software\\Clients\\Mail .");
			}
			break;
		}
		case ERROR_FILE_NOT_FOUND:
			MsOutlookUtils_logInfo("Failed to retrieve the default value of HKCU\\Software\\Clients\\Mail . ERROR_FILE_NOT_FOUND");
			break;
		case ERROR_MORE_DATA:
			MsOutlookUtils_logInfo("Failed to retrieve the default value of HKCU\\Software\\Clients\\Mail . ERROR_MORE_DATA");
			break;
		default:
			MsOutlookUtils_logInfo("Failed to retrieve the default value of HKCU\\Software\\Clients\\Mail . Unknown error.");
			break;
		}
		RegCloseKey(regKey);
	}
	else
	{
		MsOutlookUtils_logInfo("Failed to open HKCU\\Software\\Clients\\Mail .");
	}

	if(result)
		return true;

	if (ERROR_SUCCESS
					== RegOpenKeyEx(
							HKEY_LOCAL_MACHINE,
							_T("Software\\Clients\\Mail"),
							0,
							KEY_QUERY_VALUE,
							&regKey))
	{
		MsOutlookUtils_logInfo("HKLM\\Software\\Clients\\Mail exists.");
		DWORD defaultValueSize = defaultValueCapacity;
		LONG regQueryValueEx
			= RegQueryValueEx(
					regKey,
					NULL,
					NULL,
					&defaultValueType,
					(LPBYTE) defaultValue, &defaultValueSize);

		if ((ERROR_SUCCESS == regQueryValueEx)
				&& (REG_SZ == defaultValueType))
		{
			DWORD defaultValueLength = defaultValueSize / sizeof(TCHAR);

			if ((_tcsnicmp(
							_T("Microsoft Outlook"), defaultValue,
							defaultValueLength)
						== 0)
					&& (JNI_TRUE
							== MsOutlookUtils_isValidDefaultMailClient(_T("Microsoft Outlook"), 17)))
			{
				MsOutlookUtils_logInfo("The default value of HKLM\\Software\\Clients\\Mail is Microsoft Office .");
				result = true;
			}
			else
			{
				MsOutlookUtils_logInfo("The default value of HKLM\\Software\\Clients\\Mail is not Microsoft Office .");
				MsOutlookUtils_logInfo(defaultValue);
			}
		}
		else
		{
			MsOutlookUtils_logInfo("Failed to retrieve the default value of HKLM\\Software\\Clients\\Mail .");
		}
		RegCloseKey(regKey);
	}
	else
	{
		MsOutlookUtils_logInfo("HKLM\\Software\\Clients\\Mail doesn't exists.");
	}

	return result;
}
