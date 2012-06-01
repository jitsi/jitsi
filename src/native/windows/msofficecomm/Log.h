/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
