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
#include "TypeLib.h"

/**
 * Un/Register the typeLib for the COM server and client.
 *
 * @author Vincent Lucas
 */

/**
 * Register the typeLib for the COM server and client.
 *
 * @return A pointer to the loaded typeLib. NULL if failed.
 */
LPTYPELIB TypeLib_loadRegTypeLib()
{
    HMODULE hCurrentModule = nullptr;
    if (::GetModuleHandleEx(
            GET_MODULE_HANDLE_EX_FLAG_FROM_ADDRESS | GET_MODULE_HANDLE_EX_FLAG_UNCHANGED_REFCOUNT,
            (LPCTSTR) &TypeLib_loadRegTypeLib, &hCurrentModule) == 0)
    {
        return nullptr;
    }

    TCHAR szCurrentModuleFilename[MAX_PATH];
    if (::GetModuleFileName(hCurrentModule, szCurrentModuleFilename, sizeof(szCurrentModuleFilename)) == 0)
    {
        return nullptr;
    }

    LPTYPELIB lpTypeLib = nullptr;
    HRESULT hr = ::LoadTypeLibEx(szCurrentModuleFilename, REGKIND_NONE, &lpTypeLib);
    if (FAILED(hr))
    {
        return nullptr;
    }

    hr = ::RegisterTypeLibForUser(lpTypeLib, szCurrentModuleFilename, nullptr);
    if (FAILED(hr))
    {
        lpTypeLib->Release();
        return nullptr;
    }

    return lpTypeLib;
}

/**
 * Unegister the typeLib for the COM server and client.
 *
 * @param A pointer to the loaded typeLib.
 */
void TypeLib_releaseTypeLib(LPTYPELIB iTypeLib)
{
    LPTLIBATTR typeLibAttr;
    if(iTypeLib->GetLibAttr(&typeLibAttr) == S_OK)
    {
        UnRegisterTypeLibForUser(
                typeLibAttr->guid,
                typeLibAttr->wMajorVerNum,
                typeLibAttr->wMinorVerNum,
                typeLibAttr->lcid,
                typeLibAttr->syskind);
    }

    iTypeLib->ReleaseTLibAttr(typeLibAttr);
    iTypeLib->Release();
}
