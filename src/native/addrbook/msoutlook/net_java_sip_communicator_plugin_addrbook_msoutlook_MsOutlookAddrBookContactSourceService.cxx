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
    MsOutlookAddrBookContactSourceService_MAPIAllocateBuffer;
static LPMAPIFREEBUFFER MsOutlookAddrBookContactSourceService_MAPIFreeBuffer;
static LPMAPIINITIALIZE MsOutlookAddrBookContactSourceService_MAPIInitialize;
static LPMAPILOGONEX MsOutlookAddrBookContactSourceService_MAPILogonEx;
static LPMAPIUNINITIALIZE
    MsOutlookAddrBookContactSourceService_MAPIUninitialize;

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_plugin_addrbook_msoutlook_MsOutlookAddrBookContactSourceService_MAPIInitialize
    (JNIEnv *jniEnv, jclass clazz, jlong version, jlong flags)
{
    HKEY officeKey;
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
                    &officeKey))
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
                        officeKey,
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
            if ((ERROR_SUCCESS
                    == RegOpenKeyEx(
                            officeKey,
                            installRootKeyName,
                            0,
                            KEY_QUERY_VALUE,
                            &installRootKey))
                && (ERROR_SUCCESS
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
        }
    }

    /* If we've determined that we'd like to go on with MAPI, try to load it. */
    if (HR_SUCCEEDED(hResult))
    {
        HMODULE lib = LoadLibrary(_T("mapi32.dll"));

        if (lib)
        {
            MsOutlookAddrBookContactSourceService_MAPIInitialize
                = (LPMAPIINITIALIZE) GetProcAddress(lib, "MAPIInitialize");
            MsOutlookAddrBookContactSourceService_MAPIUninitialize
                = (LPMAPIUNINITIALIZE) GetProcAddress(lib, "MAPIUninitialize");

            if (MsOutlookAddrBookContactSourceService_MAPIInitialize
                    && MsOutlookAddrBookContactSourceService_MAPIUninitialize)
            {
                MAPIINIT_0 mapiInit = { (ULONG) version, (ULONG) flags };

                hResult
                    = MsOutlookAddrBookContactSourceService_MAPIInitialize(
                            &mapiInit);
                if (HR_SUCCEEDED(hResult))
                {
                    MsOutlookAddrBookContactSourceService_MAPIAllocateBuffer
                        = (LPMAPIALLOCATEBUFFER)
                            GetProcAddress(lib, "MAPIAllocateBuffer");
                    MsOutlookAddrBookContactSourceService_MAPIFreeBuffer
                        = (LPMAPIFREEBUFFER)
                            GetProcAddress(lib, "MAPIFreeBuffer");
                    MsOutlookAddrBookContactSourceService_MAPILogonEx
                        = (LPMAPILOGONEX) GetProcAddress(lib, "MAPILogonEx");
                    if (!MsOutlookAddrBookContactSourceService_MAPIAllocateBuffer
                            || !MsOutlookAddrBookContactSourceService_MAPIFreeBuffer
                            || !MsOutlookAddrBookContactSourceService_MAPILogonEx)
                    {
                        MsOutlookAddrBookContactSourceService_MAPIUninitialize();
                        hResult = MAPI_E_NOT_FOUND;
                    }
                }
            }
            else
                hResult = MAPI_E_NOT_FOUND;
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
    MsOutlookAddrBookContactSourceService_MAPIUninitialize();
}

SCODE
MsOutlookAddrBook_MAPIAllocateBuffer(ULONG size, LPVOID FAR *buffer)
{
    return
        MsOutlookAddrBookContactSourceService_MAPIAllocateBuffer(size, buffer);
}

ULONG
MsOutlookAddrBook_MAPIFreeBuffer(LPVOID buffer)
{
    return MsOutlookAddrBookContactSourceService_MAPIFreeBuffer(buffer);
}

HRESULT
MsOutlookAddrBook_MAPILogonEx
    (ULONG_PTR uiParam,
    LPSTR profileName, LPSTR password,
    FLAGS flags,
    LPMAPISESSION FAR *mapiSession)
{
    return
        MsOutlookAddrBookContactSourceService_MAPILogonEx(
                uiParam,
                profileName, password,
                flags,
                mapiSession);
}
