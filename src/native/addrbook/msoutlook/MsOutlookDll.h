/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#ifndef _MSOUTLOOKDLL_H
#define _MSOUTLOOKDLL_H
#include <windows.h>
#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT BOOL APIENTRY DllMain(
    HINSTANCE hDLL,
    DWORD dwReason,
    LPVOID lpReserved);

#ifdef __cplusplus
}
#endif

#endif //_MSOUTLOOKDLL_H