/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#include "TypeLib.h"

#include "../StringUtils.h"

#include <stdio.h>
/**
 * Un/Register the typeLib for the COM server and client.
 *
 * @author Vincent Lucas
 */

/**
 * Register the typeLib for the COM server and client.
 *
 * @param path The tlb filename.
 *
 * @return A pointer to the loaded typeLib. NULL if failed.
 */
LPTYPELIB TypeLib_loadRegTypeLib(WCHAR* path)
{
    LPTYPELIB iTypeLib = NULL;

    // Gets the path for the loading the tlb.
    char * applicationName = StringUtils::WideCharToMultiByte(path);
    int applicationNameLength = strlen(applicationName);
    char currentDirectory[FILENAME_MAX - applicationNameLength - 8];
    GetCurrentDirectory(
            FILENAME_MAX - applicationNameLength - 8,
            currentDirectory);
    char libPath[FILENAME_MAX];
    sprintf(libPath, "%s/native/%s", currentDirectory, applicationName);
    free(applicationName);
    LPWSTR libPathW = StringUtils::MultiByteToWideChar(libPath);

    // Test 2 files: 0 for the build version, 1 for the git source version.
    LPWSTR libPathList[2];
    libPathList[0] = libPathW;
    libPathList[1] = path;
    for(int i = 0; i < 2 && iTypeLib == NULL; ++i)
    {
        if(SUCCEEDED(::LoadTypeLibEx(libPathList[i], REGKIND_NONE, &iTypeLib)))
        {
            HMODULE oleaut32 = ::GetModuleHandle(_T("oleaut32.dll"));

            if (oleaut32)
            {
                typedef HRESULT (WINAPI *RTLFU)(LPTYPELIB,LPOLESTR,LPOLESTR);
                RTLFU registerTypeLibForUser = (RTLFU)
                    ::GetProcAddress(oleaut32, "RegisterTypeLibForUser");

                if (registerTypeLibForUser)
                {
                    registerTypeLibForUser(iTypeLib, libPathList[i], NULL);
                }
                else
                {
                    iTypeLib = NULL;
                }
            }
            else
            {
                iTypeLib = NULL;
            }
        }
    }
    free(libPathW);

    return iTypeLib;
}

/**
 * Unegister the typeLib for the COM server and client.
 *
 * @param A pointer to the loaded typeLib.
 */
void TypeLib_releaseTypeLib(LPTYPELIB iTypeLib)
{
    HMODULE oleaut32 = ::GetModuleHandle(_T("oleaut32.dll"));
    if(oleaut32)
    {
        typedef HRESULT (WINAPI *URTLFU)(REFGUID,WORD,WORD,LCID,SYSKIND);
        URTLFU unRegisterTypeLibForUser
            = (URTLFU) ::GetProcAddress(oleaut32, "UnRegisterTypeLibForUser");

        if(unRegisterTypeLibForUser)
        {
            LPTLIBATTR typeLibAttr;

            if(iTypeLib->GetLibAttr(&typeLibAttr) == S_OK)
            {
                unRegisterTypeLibForUser(
                        typeLibAttr->guid,
                        typeLibAttr->wMajorVerNum,
                        typeLibAttr->wMinorVerNum,
                        typeLibAttr->lcid,
                        typeLibAttr->syskind);
            }
            iTypeLib->ReleaseTLibAttr(typeLibAttr);
        }
    }

    iTypeLib->Release();
}
