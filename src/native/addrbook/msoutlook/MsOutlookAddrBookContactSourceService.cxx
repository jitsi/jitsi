/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#include "MsOutlookAddrBookContactSourceService.h"

#include "com/ComClient.h"
#include "MAPINotification.h"
#include "MAPISession.h"
#include "MAPIBitness.h"
#include <Tchar.h>

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

static jboolean
MsOutlookAddrBookContactSourceService_isValidDefaultMailClient
    (LPCTSTR name, DWORD nameLength);
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
                break;

            i++;
            if (ERROR_SUCCESS != regEnumKeyEx)
                continue;

            str = installRootKeyName + subkeyNameLength;
            memcpy(str, _T("\\Outlook\\InstallRoot"), 20 * sizeof(TCHAR));
            *(str + 20) = 0;
            if (ERROR_SUCCESS
                    == RegOpenKeyEx(
                            regKey,
                            installRootKeyName,
                            0,
                            KEY_QUERY_VALUE,
                            &installRootKey))
            {
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
                            continue;
                    }

                    if (ERROR_SUCCESS
                            == RegQueryValueEx(
                                    installRootKey,
                                    _T("Path"),
                                    NULL,
                                    NULL,
                                    (LPBYTE) pathValue, &pathValueSize))
                    {
                        DWORD pathValueLength = pathValueSize / sizeof(TCHAR);

                        if (pathValueLength)
                        {
                            DWORD fileAttributes;

                            str = pathValue + (pathValueLength - 1);
                            if (*str)
                                str++;
                            memcpy(str, _T("\\Outlook.exe"), 12 * sizeof(TCHAR));
                            *(str + 12) = 0;

                            fileAttributes = GetFileAttributes(pathValue);
                            if (INVALID_FILE_ATTRIBUTES != fileAttributes)
                                hResult = S_OK;
                        }
                    }

                    if (pathValue != installRootKeyName)
                        free(pathValue);
                }
                RegCloseKey(installRootKey);
            }
        }
        RegCloseKey(regKey);

        // Make sure that Microsoft Outlook is the default mail client in order
        // to prevent its dialog in the case of it not being the default mail
        // client.
        if (HR_SUCCEEDED(hResult))
        {
            DWORD defaultValueType;
            // The buffer installRootKeyName is long enough to receive
            // "Microsoft Outlook" so use it in order to not have to allocate
            // more memory.
            LPTSTR defaultValue = (LPTSTR) installRootKeyName;
            DWORD defaultValueCapacity = sizeof(installRootKeyName);
            jboolean checkHKeyLocalMachine;

            hResult = MAPI_E_NO_SUPPORT;
            if (ERROR_SUCCESS
                    == RegOpenKeyEx(
                            HKEY_CURRENT_USER,
                            _T("Software\\Clients\\Mail"),
                            0,
                            KEY_QUERY_VALUE,
                            &regKey))
            {
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
                                == MsOutlookAddrBookContactSourceService_isValidDefaultMailClient(
                                        defaultValue,
                                        defaultValueLength))
                        {
                            checkHKeyLocalMachine = JNI_FALSE;
                            if (_tcsnicmp(
                                        _T("Microsoft Outlook"), defaultValue,
                                        defaultValueLength)
                                    == 0)
                                hResult = S_OK;
                        }
                        else
                            checkHKeyLocalMachine = JNI_TRUE;
                    }
                    else
                        checkHKeyLocalMachine = JNI_FALSE;
                    break;
                }
                case ERROR_FILE_NOT_FOUND:
                    checkHKeyLocalMachine = JNI_TRUE;
                    break;
                case ERROR_MORE_DATA:
                    checkHKeyLocalMachine = JNI_FALSE;
                    break;
                default:
                    checkHKeyLocalMachine = JNI_FALSE;
                    break;
                }
                RegCloseKey(regKey);
            }
            else
                checkHKeyLocalMachine = JNI_TRUE;
            if ((JNI_TRUE == checkHKeyLocalMachine)
                    && (ERROR_SUCCESS
                            == RegOpenKeyEx(
                                    HKEY_LOCAL_MACHINE,
                                    _T("Software\\Clients\\Mail"),
                                    0,
                                    KEY_QUERY_VALUE,
                                    &regKey)))
            {
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
                                    == MsOutlookAddrBookContactSourceService_isValidDefaultMailClient(_T("Microsoft Outlook"), 17)))
                        hResult = S_OK;
                }
                RegCloseKey(regKey);
            }
        }
    }

    // If we've determined that we'd like to go on with MAPI, try to load it.
    if (HR_SUCCEEDED(hResult))
    {
        MsOutlookAddrBookContactSourceService_hMapiLib
            = ::LoadLibrary(_T("mapi32.dll"));

        hResult = MAPI_E_NO_SUPPORT;
        if(MsOutlookAddrBookContactSourceService_hMapiLib)
        {
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
                                // Register the notification of contact changed,
                                // created and deleted.
                                MAPINotification_registerNotifyAllMsgStores(
                                        mapiSession);
                            }
                        }
                        ::SetCurrentDirectory(lpszWorkingDir);
                        MAPISession_unlock();
                    }
                    else
                    {
                        hResult = HRESULT_FROM_WIN32(::GetLastError());
                    }

                    ::free(lpszWorkingDir);
                }
                else
                {
                    hResult = HRESULT_FROM_WIN32(::GetLastError());
                }
            }
        }
    }

    if (HR_FAILED(hResult))
    {
        if(MsOutlookAddrBookContactSourceService_hMapiLib)
        {
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

    // Start COM service
    if((hr = MsOutlookAddrBookContactSourceService_startComServer()) == S_OK)
    {
        // Start COM client
        ComClient_start();
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
    MAPINotification_registerNativeNotificationsDelegate(
            deletedMethod, insertedMethod, updatedMethod);

    return MsOutlookAddrBookContactSourceService_MAPIInitialize(version, flags);
}

void MsOutlookAddrBookContactSourceService_NativeMAPIUninitialize(void)
{
    MAPINotification_unregisterNativeNotificationsDelegate();

    MsOutlookAddrBookContactSourceService_MAPIUninitialize();
}

static jboolean
MsOutlookAddrBookContactSourceService_isValidDefaultMailClient
    (LPCTSTR name, DWORD nameLength)
{
    jboolean validDefaultMailClient = JNI_FALSE;

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

        if (ERROR_SUCCESS
                == RegOpenKeyEx(
                        HKEY_LOCAL_MACHINE,
                        keyName,
                        0,
                        KEY_QUERY_VALUE,
                        &key))
        {
            validDefaultMailClient = JNI_TRUE;
            RegCloseKey(key);
        }
    }
    return validDefaultMailClient;
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

        // Test 2 files: 0 for the build version, 1 for the git source version.
        char * serverExec[2];
        serverExec[0] = comServer;
        serverExec[1] = applicationName;
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

                return S_OK;
            }
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
