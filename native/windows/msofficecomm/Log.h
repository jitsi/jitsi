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
#ifndef _JMSOFFICECOMM_LOG_H_
#define _JMSOFFICECOMM_LOG_H_

#include <stdio.h>
#include <tchar.h>
#include <windows.h>

class Log
{
public:
    static void close()
        {
            if (_stderr && (_stderr != stderr))
            {
                ::fclose(_stderr);
                _stderr = stderr;
            }
        }

#ifdef _UNICODE
    static int d(LPCTSTR format, LPCSTR str);
#endif /* #ifdef _UNICODE */
    static int d(LPCTSTR format, ...);
    static FILE *open();

private:
    static LPTSTR getModuleFileName();

    static FILE *_stderr;
};

#endif /* #ifndef _JMSOFFICECOMM_LOG_H_ */
