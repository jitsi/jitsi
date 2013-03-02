/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#include "MsOutlookDll.h"
#include "MAPISession.h"
#include <jni.h>

JNIEXPORT BOOL APIENTRY DllMain(
    HINSTANCE /*hDLL*/,
    DWORD dwReason,
    LPVOID /*lpReserved*/)
{
    switch (dwReason)
    {
        case DLL_PROCESS_ATTACH:
            MAPISession_initLock();
            break;
        case DLL_PROCESS_DETACH:
            MAPISession_freeLock();
            break;
    }

    return TRUE;
}
