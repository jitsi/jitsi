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

#include "MsOutlookAddrBookContactSourceService.h"

#include "com/ComClient.h"
#include "com/MsOutlookAddrBookServerClassFactory.h"
#include "com/MsOutlookAddrBookClientClassFactory.h"
#include "MAPINotification.h"
#include "MAPISession.h"
#include "MAPIBitness.h"
#include "MsOutlookUtils.h"
#include <Tchar.h>
#include "StringUtils.h"

typedef BOOL (STDAPICALLTYPE *LPFBINFROMHEX)(LPSTR, LPBYTE);
typedef void (STDAPICALLTYPE *LPFREEPROWS)(LPSRowSet);
typedef void (STDAPICALLTYPE *LPHEXFROMBIN)(LPBYTE, int, LPSTR);
typedef HRESULT (STDAPICALLTYPE *LPHRALLOCADVISESINK)(LPNOTIFCALLBACK, LPVOID, LPMAPIADVISESINK FAR *);
typedef HRESULT (STDAPICALLTYPE *LPHRQUERYALLROWS)(LPMAPITABLE, LPSPropTagArray,
LPSRestriction, LPSSortOrderSet, LONG, LPSRowSet FAR *);

static HANDLE MsOutlookAddrBookContactSourceService_comServerHandle = NULL;
static LPFBINFROMHEX MsOutlookAddrBookContactSourceService_fBinFromHex;
static LPFREEPROWS MsOutlookAddrBookContactSourceService_freeProws;
static LPHEXFROMBIN MsOutlookAddrBookContactSourceService_hexFromBin;
static LPHRALLOCADVISESINK MsOutlookAddrBookContactSourceService_hrAllocAdviseSink;
static LPHRQUERYALLROWS MsOutlookAddrBookContactSourceService_hrQueryAllRows;
static LPMAPIALLOCATEBUFFER
    MsOutlookAddrBookContactSourceService_mapiAllocateBuffer;
static LPMAPIFREEBUFFER MsOutlookAddrBookContactSourceService_mapiFreeBuffer;
static LPMAPIINITIALIZE MsOutlookAddrBookContactSourceService_mapiInitialize;
static LPMAPILOGONEX MsOutlookAddrBookContactSourceService_mapiLogonEx;
static LPMAPIUNINITIALIZE
    MsOutlookAddrBookContactSourceService_mapiUninitialize;
static HMODULE MsOutlookAddrBookContactSourceService_hMapiLib = NULL;

HRESULT MsOutlookAddrBookContactSourceService_startComServer(void);

HRESULT MsOutlookAddrBookContactSourceService_MAPIInitialize
    (jlong version, jlong flags)
{
    HKEY regKey;
    HRESULT hResult = MAPI_E_NO_SUPPORT;

    /*
     * In the absence of a default e-mail program, MAPIInitialize may show a
     * dialog to notify of the fact. The dialog is undesirable here. Because we
     * implement ContactSourceService for Microsoft Outlook, we will try to
     * mitigate the problem by implementing an ad-hoc check whether Microsoft
     * Outlook is installed.
     */
    if (ERROR_SUCCESS
            == RegOpenKeyEx(
                    HKEY_LOCAL_MACHINE,
                    _T("Software\\Microsoft\\Office"),
                    0,
                    KEY_ENUMERATE_SUB_KEYS,
                    &regKey))
    {
        DWORD i = 0;
        TCHAR installRootKeyName[
                255 // The size limit of key name as documented in MSDN
                    + 20 // \Outlook\InstallRoot
                    + 1]; // The terminating null character
        MsOutlookUtils_logInfo("Searching for outlook InstallRoot.");
        while (1)
        {
            LONG regEnumKeyEx;
            DWORD subkeyNameLength = 255 + 1;
            LPTSTR str;
            HKEY installRootKey;
            DWORD pathValueType;
            DWORD pathValueSize;

            regEnumKeyEx = RegEnumKeyEx(
                        regKey,
                        i,
                        installRootKeyName,
                        &subkeyNameLength,
                        NULL,
                        NULL,
                        NULL,
                        NULL);
            if (ERROR_NO_MORE_ITEMS == regEnumKeyEx)
			{
            	MsOutlookUtils_logInfo("No more Software\\Microsoft\\Office items.");
                break;
			}

            i++;
            if (ERROR_SUCCESS != regEnumKeyEx)
			{
            	MsOutlookUtils_logInfo("Error quering the next Software\\Microsoft\\Office item.");
                continue;
			}

            str = installRootKeyName + subkeyNameLength;
            memcpy(str, _T("\\Outlook\\InstallRoot"), 20 * sizeof(TCHAR));
            *(str + 20) = 0;
			MsOutlookUtils_log("Trying to open the following key:");
			MsOutlookUtils_log(installRootKeyName);
            if (ERROR_SUCCESS
                    == RegOpenKeyEx(
                            regKey,
                            installRootKeyName,
                            0,
                            KEY_QUERY_VALUE,
                            &installRootKey))
            {
            	MsOutlookUtils_logInfo("The key is opened successfully.");
                if ((ERROR_SUCCESS
                        == RegQueryValueEx(
                                installRootKey,
                                _T("Path"),
                                NULL,
                                &pathValueType,
                                NULL,
                                &pathValueSize))
                    && (REG_SZ == pathValueType)
                    && pathValueSize)
                {
                	MsOutlookUtils_logInfo("Path value found.");
                    LPTSTR pathValue;

                    // MSDN says "the string may not have been stored with the
                    // proper terminating null characters."
                    pathValueSize
                        += sizeof(TCHAR)
                            * (12 // \Outlook.exe
                                + 1); // The terminating null character

                    if (pathValueSize <= sizeof(installRootKeyName))
                        pathValue = installRootKeyName;
                    else
                    {
                        pathValue = (LPTSTR)::malloc(pathValueSize);
                        if (!pathValue)
						{
                        	MsOutlookUtils_logInfo("Error with memory allocation for the pathValue.");
                            continue;
						}
                    }

                    if (ERROR_SUCCESS
                            == RegQueryValueEx(
                                    installRootKey,
                                    _T("Path"),
                                    NULL,
                                    NULL,
                                    (LPBYTE) pathValue, &pathValueSize))
                    {
                    	MsOutlookUtils_logInfo("The path value is retrieved");
                        DWORD pathValueLength = pathValueSize / sizeof(TCHAR);

                        if (pathValueLength)
                        {
                        	MsOutlookUtils_logInfo("The path value is retrieved successfully. The length is not 0.");
                            DWORD fileAttributes;

                            str = pathValue + (pathValueLength - 1);
                            if (*str)
                                str++;
                            memcpy(str, _T("\\Outlook.exe"), 12 * sizeof(TCHAR));
                            *(str + 12) = 0;
                            MsOutlookUtils_logInfo("Trying to retrieve atributes for:");
                            MsOutlookUtils_logInfo(pathValue);
                            fileAttributes = GetFileAttributes(pathValue);
                            if (INVALID_FILE_ATTRIBUTES != fileAttributes)
							{
                            	MsOutlookUtils_logInfo("The file exists.");
                                hResult = S_OK;
							}
							else
							{
								MsOutlookUtils_logInfo("The file doesn't exists");
							}
                        }
						else
						{
							MsOutlookUtils_logInfo("Error - the length of the path value is 0.");
						}
                    }
					else
					{
						MsOutlookUtils_logInfo("Error retrieving the pathValue.");
					}

                    if (pathValue != installRootKeyName)
                        free(pathValue);
                }
				else
				{
					MsOutlookUtils_logInfo("Error Path value not found.");
				}
                RegCloseKey(installRootKey);
            }
			else
			{
				MsOutlookUtils_logInfo("Error openning the key.");
			}
        }
        RegCloseKey(regKey);

        // Make sure that Microsoft Outlook is the default mail client in order
        // to prevent its dialog in the case of it not being the default mail
        // client.
        if (HR_SUCCEEDED(hResult))
        {
        	if(MsOutlookUtils_isOutlookDefaultMailClient())
        	{
        		hResult = S_OK;
        	}
        	else
        	{
        		hResult = MAPI_E_NO_SUPPORT;
        	}
        }
		else
		{
			MsOutlookUtils_logInfo("Outlook is not installed.");
		}
    }
	else
	{
		MsOutlookUtils_logInfo("Error opening HKLM\\Software\\Microsoft\\Office registry.");
	}

    // If we've determined that we'd like to go on with MAPI, try to load it.
    if (HR_SUCCEEDED(hResult))
    {
    	MsOutlookUtils_logInfo("Loading MAPI.");
        MsOutlookAddrBookContactSourceService_hMapiLib
            = ::LoadLibrary(_T("mapi32.dll"));

        hResult = MAPI_E_NO_SUPPORT;
        if(MsOutlookAddrBookContactSourceService_hMapiLib)
        {
        	MsOutlookUtils_logInfo("Loading MAPI functions");
            // get and check function pointers
            MsOutlookAddrBookContactSourceService_mapiInitialize
                = (LPMAPIINITIALIZE) GetProcAddress(
                        MsOutlookAddrBookContactSourceService_hMapiLib,
                        "MAPIInitialize");
            MsOutlookAddrBookContactSourceService_mapiUninitialize
                = (LPMAPIUNINITIALIZE) GetProcAddress(
                            MsOutlookAddrBookContactSourceService_hMapiLib,
                            "MAPIUninitialize");
            MsOutlookAddrBookContactSourceService_mapiAllocateBuffer
                = (LPMAPIALLOCATEBUFFER) GetProcAddress(
                        MsOutlookAddrBookContactSourceService_hMapiLib,
                        "MAPIAllocateBuffer");
            MsOutlookAddrBookContactSourceService_mapiFreeBuffer
                = (LPMAPIFREEBUFFER) GetProcAddress(
                        MsOutlookAddrBookContactSourceService_hMapiLib,
                        "MAPIFreeBuffer");
            MsOutlookAddrBookContactSourceService_mapiLogonEx
                = (LPMAPILOGONEX) GetProcAddress(
                        MsOutlookAddrBookContactSourceService_hMapiLib,
                        "MAPILogonEx");

            // Depending on mapi32.dll version the following functions must be
            // loaded with or without "...@#".
            MsOutlookAddrBookContactSourceService_fBinFromHex
                = (LPFBINFROMHEX) GetProcAddress(
                        MsOutlookAddrBookContactSourceService_hMapiLib,
                        "FBinFromHex");
            if(MsOutlookAddrBookContactSourceService_fBinFromHex == NULL)
            {
                MsOutlookAddrBookContactSourceService_fBinFromHex
                    = (LPFBINFROMHEX) GetProcAddress(
                            MsOutlookAddrBookContactSourceService_hMapiLib,
                            "FBinFromHex@8");
            }
            MsOutlookAddrBookContactSourceService_freeProws
                = (LPFREEPROWS) GetProcAddress(
                        MsOutlookAddrBookContactSourceService_hMapiLib,
                        "FreeProws");
            if(MsOutlookAddrBookContactSourceService_freeProws == NULL)
            {
                MsOutlookAddrBookContactSourceService_freeProws
                    = (LPFREEPROWS) GetProcAddress(
                            MsOutlookAddrBookContactSourceService_hMapiLib,
                            "FreeProws@4");
            }
            MsOutlookAddrBookContactSourceService_hexFromBin
                = (LPHEXFROMBIN) GetProcAddress(
                        MsOutlookAddrBookContactSourceService_hMapiLib,
                        "HexFromBin");
            if(MsOutlookAddrBookContactSourceService_hexFromBin == NULL)
            {
                MsOutlookAddrBookContactSourceService_hexFromBin
                    = (LPHEXFROMBIN) GetProcAddress(
                            MsOutlookAddrBookContactSourceService_hMapiLib,
                            "HexFromBin@12");
            }
            MsOutlookAddrBookContactSourceService_hrAllocAdviseSink
                = (LPHRALLOCADVISESINK)
                    GetProcAddress(
                            MsOutlookAddrBookContactSourceService_hMapiLib,
                            "HrAllocAdviseSink");
            if(MsOutlookAddrBookContactSourceService_hrAllocAdviseSink == NULL)
            {
                MsOutlookAddrBookContactSourceService_hrAllocAdviseSink
                    = (LPHRALLOCADVISESINK)
                    GetProcAddress(
                            MsOutlookAddrBookContactSourceService_hMapiLib,
                            "HrAllocAdviseSink@12");
            }
            MsOutlookAddrBookContactSourceService_hrQueryAllRows
                = (LPHRQUERYALLROWS) GetProcAddress(
                            MsOutlookAddrBookContactSourceService_hMapiLib,
                            "HrQueryAllRows");
            if(MsOutlookAddrBookContactSourceService_hrQueryAllRows == NULL)
            {
                MsOutlookAddrBookContactSourceService_hrQueryAllRows
                    = (LPHRQUERYALLROWS) GetProcAddress(
                            MsOutlookAddrBookContactSourceService_hMapiLib,
                            "HrQueryAllRows@24");
            }

            if (MsOutlookAddrBookContactSourceService_mapiInitialize
                && MsOutlookAddrBookContactSourceService_mapiUninitialize
                && MsOutlookAddrBookContactSourceService_mapiAllocateBuffer
                && MsOutlookAddrBookContactSourceService_mapiFreeBuffer
                && MsOutlookAddrBookContactSourceService_mapiLogonEx
                && MsOutlookAddrBookContactSourceService_fBinFromHex
                && MsOutlookAddrBookContactSourceService_freeProws
                && MsOutlookAddrBookContactSourceService_hexFromBin
                && MsOutlookAddrBookContactSourceService_hrAllocAdviseSink
                && MsOutlookAddrBookContactSourceService_hrQueryAllRows)
            {
                MAPIINIT_0 mapiInit = { (ULONG) version, (ULONG) flags };

                // Opening MAPI changes the working directory. Make a backup of
                // the current directory, login to MAPI and restore it
                DWORD dwSize = ::GetCurrentDirectory(0, NULL);
                if (dwSize > 0)
                {
                    LPTSTR lpszWorkingDir
                        = (LPTSTR)::malloc(dwSize*sizeof(TCHAR));
                    DWORD dwResult
                        = ::GetCurrentDirectory(dwSize, lpszWorkingDir);
                    if (dwResult != 0)
                    {
                        MAPISession_lock();
                        hResult
                            = MsOutlookAddrBookContactSourceService_mapiInitialize(
                                    &mapiInit);

                        if(HR_SUCCEEDED(hResult)
                                && MAPISession_getMapiSession() == NULL)
                        {
                        	MsOutlookUtils_logInfo("MAPI logon.");
                            LPMAPISESSION mapiSession = NULL;
                            hResult = MsOutlookAddrBook_mapiLogonEx(
                                    0,
                                    NULL, NULL,
                                    MAPI_EXTENDED
                                        | MAPI_NO_MAIL
                                        | MAPI_USE_DEFAULT,
                                    &mapiSession);
                            if(HR_SUCCEEDED(hResult))
                            {
                            	MsOutlookUtils_logInfo("MAPI logon success.");
                                // Register the notification of contact changed,
                                // created and deleted.
                                MAPINotification_registerNotifyAllMsgStores(
                                        mapiSession);
                            }
                            else
                            {
                            	MsOutlookUtils_logInfo("MAPI logon error.");
                            }
                        }
                        else
                        {
                        	MsOutlookUtils_logInfo("Error calling MAPI init from MAPI library.");
                        }
                        ::SetCurrentDirectory(lpszWorkingDir);
                        MAPISession_unlock();
                    }
                    else
                    {
                        hResult = HRESULT_FROM_WIN32(::GetLastError());
                        MsOutlookUtils_logInfo("Error getting current directory.[1]");
                    }

                    ::free(lpszWorkingDir);
                }
                else
                {
                    hResult = HRESULT_FROM_WIN32(::GetLastError());
                    MsOutlookUtils_logInfo("Error getting current directory.[2]");
                }
            }
            else
            {
            	MsOutlookUtils_logInfo("Cannot get MAPI functions.");
            }
        }
        else
        {
        	MsOutlookUtils_logInfo("Error while loading MAPI library.");
        }
    }
    else
    {
    	MsOutlookUtils_logInfo("ERROR - we won't load MAPI.");
    }

    if (HR_FAILED(hResult))
    {
    	MsOutlookUtils_logInfo("ERROR - in MAPI native init.");
        if(MsOutlookAddrBookContactSourceService_hMapiLib)
        {
        	MsOutlookUtils_logInfo("ERROR - free MAPI library.");
            FreeLibrary(MsOutlookAddrBookContactSourceService_hMapiLib);
            MsOutlookAddrBookContactSourceService_hMapiLib = NULL;
        }
    }

    return hResult;
}

/**
 * Starts the COM server.
 *
 * @return S_OK if eveything was fine. E_FAIL otherwise.
 */
HRESULT MsOutlookAddrBookContactSourceService_MAPIInitializeCOMServer(void)
{
    HRESULT hr = E_FAIL;

    MAPISession_lock();
    MsOutlookUtils_log("Init com server.");

    IMsOutlookAddrBookServer * ComClient_iServer = NULL;
    if((hr = CoInitializeEx(NULL, COINIT_MULTITHREADED)) == S_OK
                || hr == S_FALSE)
	{
		if((hr = CoCreateInstance(
				CLSID_MsOutlookAddrBookServer,
				NULL,
				CLSCTX_LOCAL_SERVER,
				IID_IMsOutlookAddrBookServer,
				(void**) &ComClient_iServer)) == S_OK)
		{
			MsOutlookUtils_log("COM Server already started");
			if(ComClient_iServer)
			{
				ComClient_iServer->Release();
				ComClient_iServer = NULL;
			}
			return E_FAIL;
		}
	}

    // Start COM service
    if((hr = MsOutlookAddrBookContactSourceService_startComServer()) == S_OK)
    {
    	MsOutlookUtils_log("COM Server started.");
        // Start COM client
        ComClient_start();
    }
    else
    {
    	MsOutlookUtils_log("Failed to start COM Server.");
    }

    MAPISession_unlock();

    return hr;
}

void MsOutlookAddrBookContactSourceService_MAPIUninitialize(void)
{
    MAPISession_lock();

    LPMAPISESSION mapiSession = MAPISession_getMapiSession();
    if(mapiSession != NULL)
    {
        MAPINotification_unregisterNotifyAllMsgStores();
        mapiSession->Logoff(0, 0, 0);
        mapiSession->Release();
        MAPISession_setMapiSession(NULL);
    }

    if(MsOutlookAddrBookContactSourceService_hMapiLib)
    {
        MsOutlookAddrBookContactSourceService_mapiUninitialize();

        MsOutlookAddrBookContactSourceService_mapiInitialize = NULL;
        MsOutlookAddrBookContactSourceService_mapiUninitialize = NULL;
        MsOutlookAddrBookContactSourceService_mapiAllocateBuffer = NULL;
        MsOutlookAddrBookContactSourceService_mapiFreeBuffer = NULL;
        MsOutlookAddrBookContactSourceService_mapiLogonEx = NULL;
        MsOutlookAddrBookContactSourceService_fBinFromHex = NULL;
        MsOutlookAddrBookContactSourceService_freeProws = NULL;
        MsOutlookAddrBookContactSourceService_hexFromBin = NULL;
        MsOutlookAddrBookContactSourceService_hrAllocAdviseSink = NULL;
        MsOutlookAddrBookContactSourceService_hrQueryAllRows = NULL;
        ::FreeLibrary(MsOutlookAddrBookContactSourceService_hMapiLib);
        MsOutlookAddrBookContactSourceService_hMapiLib = NULL;
    }

    MAPISession_unlock();
}

/**
 * Stops the COM server.
 */
void MsOutlookAddrBookContactSourceService_MAPIUninitializeCOMServer(void)
{
    if(MsOutlookAddrBookContactSourceService_comServerHandle != NULL)
    {
        TerminateProcess(
                MsOutlookAddrBookContactSourceService_comServerHandle,
                1);

        CloseHandle(MsOutlookAddrBookContactSourceService_comServerHandle);
        MsOutlookAddrBookContactSourceService_comServerHandle = NULL;
    }
    ComClient_stop();
}

/**
 * Initializes the plugin but from the COM server point of view: natif side, no
 * java available here.
 *
 * @param version The version of MAPI to load.
 * @param flags The option choosen to load the MAPI to lib.
 * @param deletedMethod A function pointer used as a callback on notification
 * from outlook when a contact has been removed.
 * @param insertedMethod A function pointer used as a callback on notification
 * from outlook when a contact has been added.
 * @param updatedMethod A function pointer used as a callback on notification
 * from outlook when a contact has been modified.
 *
 * @return  S_OK if everything was alright.
 */
HRESULT MsOutlookAddrBookContactSourceService_NativeMAPIInitialize
    (jlong version, jlong flags,
     void * deletedMethod, void * insertedMethod, void * updatedMethod)
{
	MsOutlookUtils_logInfo("MAPI native init.");
    MAPINotification_registerNativeNotificationsDelegate(
            deletedMethod, insertedMethod, updatedMethod);

    return MsOutlookAddrBookContactSourceService_MAPIInitialize(version, flags);
}

void MsOutlookAddrBookContactSourceService_NativeMAPIUninitialize(void)
{
    MAPINotification_unregisterNativeNotificationsDelegate();

    MsOutlookAddrBookContactSourceService_MAPIUninitialize();
}


/**
 * Starts the COM server.
 *
 * @param S_OK if the server started correctly. E_FAIL otherwise.
 */
HRESULT MsOutlookAddrBookContactSourceService_startComServer(void)
{
    int bitness = MAPIBitness_getOutlookBitnessVersion();
    if(bitness != -1)
    {
        // Start COM service
        char applicationName32[] = "jmsoutlookaddrbookcomserver32.exe";
        char applicationName64[] = "jmsoutlookaddrbookcomserver64.exe";
        char * applicationName = applicationName32;
        if(bitness == 64)
        {
            applicationName = applicationName64;
        }
        int applicationNameLength = strlen(applicationName);
        char currentDirectory[FILENAME_MAX - applicationNameLength - 8];
        GetCurrentDirectory(
                FILENAME_MAX - applicationNameLength - 8,
                currentDirectory);
        char comServer[FILENAME_MAX];
        sprintf(comServer, "%s/native/%s", currentDirectory, applicationName);

        STARTUPINFO startupInfo;
        PROCESS_INFORMATION processInfo;
        memset(&startupInfo, 0, sizeof(startupInfo));
        memset(&processInfo, 0, sizeof(processInfo));
        startupInfo.dwFlags = STARTF_USESHOWWINDOW;
        startupInfo.wShowWindow = SW_HIDE;
        char* loggerPath = MsOutlookUtils_getLoggerPath();
        int loggerPathLenght = 0;
        char* comServerWithLogger;
        char* appNameWithLogger;
        char* loggerPathEscaped = NULL;
        if(loggerPath != NULL)
        {
        	int loggerLevel = MsOutlookUtils_getLoggerLevel();
        	char* loggerPathEscaped = (char* ) malloc(strlen(loggerPath) *
        			sizeof(char) * 2);
        	int i = 0;
        	while(*loggerPath != '\0')
        	{
        		*(loggerPathEscaped + i) = *loggerPath;
        		i++;
        		if(*loggerPath == '\\')
        		{
        			*(loggerPathEscaped + i) = '\\';
					i++;
        		}
        		loggerPath++;
        	}
        	*(loggerPathEscaped + i) = '\0';
        	loggerPathLenght = strlen(loggerPathEscaped);
			comServerWithLogger
				= (char*) malloc(
						(FILENAME_MAX + loggerPathLenght) * sizeof(char));
			appNameWithLogger
				= (char*) malloc(
						(FILENAME_MAX + loggerPathLenght) * sizeof(char));
        	sprintf(comServerWithLogger, "%s \"%s\" %d", comServer,
        			loggerPathEscaped, loggerLevel);
        	sprintf(appNameWithLogger, "%s \"%s\" %d", applicationName
        			, loggerPathEscaped, loggerLevel);
        }
        else
        {
        	comServerWithLogger = comServer;
        	appNameWithLogger = applicationName;
        }
        // Test 2 files: 0 for the build version, 1 for the git source version.
        char * serverExec[2];
        serverExec[0] = comServerWithLogger;
        serverExec[1] = appNameWithLogger;
        for(int i = 0; i < 2; ++i)
        {
            // Create the COM server
            if(CreateProcess(
                        NULL,
                        serverExec[i],
                        NULL, NULL, false, 0, NULL, NULL,
                        &startupInfo,
                        &processInfo))
            {
                MsOutlookAddrBookContactSourceService_comServerHandle
                    = processInfo.hProcess;
                MsOutlookUtils_logInfo(serverExec[i]);
                MsOutlookUtils_logInfo("COM Server started successful.[1]");
                if(loggerPath != NULL)
				{
                	free(comServerWithLogger);
                	free(appNameWithLogger);
				}
                MsOutlookUtils_logInfo("COM Server started successful.[2]");
                return S_OK;
            }
        }
        if(loggerPath != NULL)
		{
			free(comServerWithLogger);
			free(appNameWithLogger);
			free(loggerPathEscaped);
		}
    }

    return E_FAIL;
}

BOOL MsOutlookAddrBook_fBinFromHex(LPSTR lpsz, LPBYTE lpb)
{
    return MsOutlookAddrBookContactSourceService_fBinFromHex(lpsz, lpb);
}

void MsOutlookAddrBook_freeProws(LPSRowSet lpRows)
{
    MsOutlookAddrBookContactSourceService_freeProws(lpRows);
}

void MsOutlookAddrBook_hexFromBin(LPBYTE pb, int cb, LPSTR sz)
{
    MsOutlookAddrBookContactSourceService_hexFromBin(pb, cb, sz);
}

void
MsOutlookAddrBook_hrAllocAdviseSink
    (LPNOTIFCALLBACK lpfnCallback, LPVOID lpvContext, LPMAPIADVISESINK*
      lppAdviseSink)
{
    MsOutlookAddrBookContactSourceService_hrAllocAdviseSink(
            lpfnCallback,
            lpvContext,
            lppAdviseSink);
}

HRESULT
MsOutlookAddrBook_hrQueryAllRows
    (LPMAPITABLE lpTable, LPSPropTagArray lpPropTags,
     LPSRestriction lpRestriction, LPSSortOrderSet lpSortOrderSet,
     LONG crowsMax, LPSRowSet* lppRows)
{
    return MsOutlookAddrBookContactSourceService_hrQueryAllRows(
            lpTable,
            lpPropTags,
            lpRestriction,
            lpSortOrderSet,
            crowsMax,
            lppRows);
}

SCODE
MsOutlookAddrBook_mapiAllocateBuffer(ULONG size, LPVOID FAR *buffer)
{
    return
        MsOutlookAddrBookContactSourceService_mapiAllocateBuffer(size, buffer);
}

ULONG
MsOutlookAddrBook_mapiFreeBuffer(LPVOID buffer)
{
    return MsOutlookAddrBookContactSourceService_mapiFreeBuffer(buffer);
}

HRESULT
MsOutlookAddrBook_mapiLogonEx
    (ULONG_PTR uiParam,
    LPTSTR profileName, LPTSTR password,
    FLAGS flags,
    LPMAPISESSION FAR *mapiSession)
{
    HRESULT hResult;

    MAPISession_lock();
    LPMAPISESSION currentMapiSession = MAPISession_getMapiSession();
    if (currentMapiSession != NULL)
        hResult = S_OK;
    else
    {
        hResult
            = MsOutlookAddrBookContactSourceService_mapiLogonEx(
                    uiParam,
                    profileName, password,
                    flags,
                    &currentMapiSession);

        MAPISession_setMapiSession(currentMapiSession);
    }

    if (HR_SUCCEEDED(hResult))
    {
        *mapiSession = currentMapiSession;
    }

    MAPISession_unlock();
    return hResult;
}
