/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#ifndef _ORG_JITSI_WINDOWS_RUN_REGISTRY_H_
#define _ORG_JITSI_WINDOWS_RUN_REGISTRY_H_

#include <tchar.h>
#include <windows.h>

LONG Run_getRegSzValue(HKEY key, LPCTSTR value, LPTSTR *data);

#endif /* #ifndef _ORG_JITSI_WINDOWS_RUN_REGISTRY_H_ */
