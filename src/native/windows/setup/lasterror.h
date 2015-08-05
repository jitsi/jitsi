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

#ifndef _ORG_JITSI_WINDOWS_SETUP_LASTERROR_H_
#define _ORG_JITSI_WINDOWS_SETUP_LASTERROR_H_

#include <tchar.h>
#include <windows.h>

DWORD LastError_error();
LPCTSTR LastError_file();
int LastError_line();
void LastError_setLastError(DWORD error, LPCTSTR file, int line);

#endif /* #ifndef _ORG_JITSI_WINDOWS_SETUP_LASTERROR_H_ */
