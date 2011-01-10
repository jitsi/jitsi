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
                    _TEXT("Software\\Microsoft\\Office"),
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
            memcpy(str, _TEXT("\\Outlook\\InstallRoot"), 20 * sizeof(TCHAR));
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
                            _TEXT("Path"),
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
                                _TEXT("Path"),
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
                        {
                            MAPIINIT_0 mapiInit
                                = { (ULONG) version, (ULONG) flags };

                            hResult = MAPIInitialize(&mapiInit);
                        }
                    }
                }

                if (pathValue != installRootKeyName)
                    free(pathValue);
            }
        }
    }

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
    MAPIUninitialize();
}
