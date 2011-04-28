/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#ifndef _ORG_JITSI_WINDOWS_SETUP_LASTERROR_H_
#define _ORG_JITSI_WINDOWS_SETUP_LASTERROR_H_

#include <tchar.h>
#include <windows.h>

DWORD LastError_error();
LPCTSTR LastError_file();
int LastError_line();
void LastError_setLastError(DWORD error, LPCTSTR file, int line);

#endif /* #ifndef _ORG_JITSI_WINDOWS_SETUP_LASTERROR_H_ */
