/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#include "net_java_sip_communicator_plugin_addrbook_msoutlook_MsOutlookAddrBookContactSourceService.h"

#include "MsOutlookMAPI.h"
#include "MsOutlookMAPIHResultException.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static LPMAPIALLOCATEBUFFER
    MsOutlookAddrBookContactSourceService_mapiAllocateBuffer;
static LPMAPIFREEBUFFER MsOutlookAddrBookContactSourceService_mapiFreeBuffer;
static LPMAPIINITIALIZE MsOutlookAddrBookContactSourceService_mapiInitialize;
static LPMAPILOGONEX MsOutlookAddrBookContactSourceService_mapiLogonEx;
static LPMAPIUNINITIALIZE
    MsOutlookAddrBookContactSourceService_mapiUninitialize;

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_plugin_addrbook_msoutlook_MsOutlookAddrBookContactSourceService_MAPIInitialize
    (JNIEnv *jniEnv, jclass clazz, jlong version, jlong flags)
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
                255 /* The size limit of key name as documented in MSDN */
                    + 20 /* \Outlook\InstallRoot */
                    + 1 /* The terminating null character */ ];

        while (1)
        {
            LONG regEnumKeyEx;
            DWORD subkeyNameLength = 255 + 1;
            LPTSTR str;
            HKEY installRootKey;
            DWORD pathValueType;
            DWORD pathValueSize;

            regEnumKeyEx
                = RegEnumKeyEx(
                        regKey,
                        i,
                        installRootKeyName, &subkeyNameLength,
                        NULL,
                        NULL, NULL,
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
                            NULL, &pathValueSize))
                && (REG_SZ == pathValueType)
                && pathValueSize)
            {
                LPTSTR pathValue;

                /*
                 * MSDN says "the string may not have been stored with the
                 * proper terminating null characters."
                 */
                pathValueSize
                    += sizeof(TCHAR)
                        * (12 /* \Outlook.exe */
                            + 1 /* The terminating null character */);

                if (pathValueSize <= sizeof(installRootKeyName))
                    pathValue = installRootKeyName;
                else
                {
                    pathValue = (TCHAR *) malloc(pathValueSize);
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
                        memcpy(str, "\\Outlook.exe", 12 * sizeof(TCHAR));
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

        /*
         * Make sure that Microsoft Outlook is the default mail client in order
         * to prevent its dialog in the case of it not being the default mail
         * client.
         */
        if (HR_SUCCEEDED(hResult))
        {
            DWORD defaultValueType;
            /*
             * The buffer installRootKeyName is long enough to receive
             * "Microsoft Outlook" so use it in order to not have to allocate
             * more memory.
             */
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
                LONG regQueryValueEx
                    = RegQueryValueEx(
                            regKey,
                            NULL,
                            NULL,
                            &defaultValueType,
                            (LPBYTE) defaultValue, &defaultValueSize);

                switch (regQueryValueEx)
                {
                case ERROR_SUCCESS:
                {
                    if (REG_SZ == defaultValueType)
                    {
                        DWORD defaultValueLength
                            = defaultValueSize / sizeof(TCHAR);

                        if ((0 == defaultValueLength) || (0 == defaultValue[0]))
                            checkHKeyLocalMachine = JNI_TRUE;
                        else
                        {
                            checkHKeyLocalMachine = JNI_FALSE;
                            if (_tcsnicmp(
                                        _T("Microsoft Outlook"), defaultValue,
                                        defaultValueLength)
                                    == 0)
                                hResult = S_OK;
                        }
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

                    if (_tcsnicmp(
                                _T("Microsoft Outlook"), defaultValue,
                                defaultValueLength)
                            == 0)
                        hResult = S_OK;
                }
                RegCloseKey(regKey);
            }
        }
    }

    /* If we've determined that we'd like to go on with MAPI, try to load it. */
    if (HR_SUCCEEDED(hResult))
    {
        HMODULE lib = LoadLibrary(_T("mapi32.dll"));

        hResult = MAPI_E_NO_SUPPORT;
        if (lib)
        {
            MsOutlookAddrBookContactSourceService_mapiInitialize
                = (LPMAPIINITIALIZE) GetProcAddress(lib, "MAPIInitialize");
            MsOutlookAddrBookContactSourceService_mapiUninitialize
                = (LPMAPIUNINITIALIZE) GetProcAddress(lib, "MAPIUninitialize");

            if (MsOutlookAddrBookContactSourceService_mapiInitialize
                    && MsOutlookAddrBookContactSourceService_mapiUninitialize)
            {
                MAPIINIT_0 mapiInit = { (ULONG) version, (ULONG) flags };

                hResult
                    = MsOutlookAddrBookContactSourceService_mapiInitialize(
                            &mapiInit);
                if (HR_SUCCEEDED(hResult))
                {
                    MsOutlookAddrBookContactSourceService_mapiAllocateBuffer
                        = (LPMAPIALLOCATEBUFFER)
                            GetProcAddress(lib, "MAPIAllocateBuffer");
                    MsOutlookAddrBookContactSourceService_mapiFreeBuffer
                        = (LPMAPIFREEBUFFER)
                            GetProcAddress(lib, "MAPIFreeBuffer");
                    MsOutlookAddrBookContactSourceService_mapiLogonEx
                        = (LPMAPILOGONEX) GetProcAddress(lib, "MAPILogonEx");
                    if (MsOutlookAddrBookContactSourceService_mapiAllocateBuffer
                            && MsOutlookAddrBookContactSourceService_mapiFreeBuffer
                            && MsOutlookAddrBookContactSourceService_mapiLogonEx)
                        hResult = S_OK;
                    else
                        MsOutlookAddrBookContactSourceService_mapiUninitialize();
                }
            }
            if (HR_FAILED(hResult))
                FreeLibrary(lib);
        }
    }

    /* Report any possible error regardless of where it has come from. */
    if (HR_FAILED(hResult))
    {
        MsOutlookMAPIHResultException_throwNew(
                jniEnv,
                hResult,
                __FILE__, __LINE__);
    }
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_plugin_addrbook_msoutlook_MsOutlookAddrBookContactSourceService_MAPIUninitialize
    (JNIEnv *jniEnv, jclass clazz)
{
    MsOutlookAddrBookContactSourceService_mapiUninitialize();
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
    LPSTR profileName, LPSTR password,
    FLAGS flags,
    LPMAPISESSION FAR *mapiSession)
{
    return
        MsOutlookAddrBookContactSourceService_mapiLogonEx(
                uiParam,
                profileName, password,
                flags,
                mapiSession);
}
