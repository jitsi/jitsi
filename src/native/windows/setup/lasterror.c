/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#include "lasterror.h"

#include <stdlib.h>
#include <string.h>

static DWORD _LastError_error = ERROR_SUCCESS;
static LPTSTR _LastError_file = NULL;
static int _LastError_line = 0;

DWORD
LastError_error()
{
    return _LastError_error;
}

LPCTSTR
LastError_file()
{
    return _LastError_file;
}

int
LastError_line()
{
    return _LastError_line;
}

void
LastError_setLastError(DWORD error, LPCTSTR file, int line)
{
    size_t fileLength;

    _LastError_error = error;

    /* Make sure that LastError_file is large enough to receive file. */
    fileLength = _tcslen(file);
    if (!_LastError_file || (_tcslen(_LastError_file) < fileLength))
    {
        LPTSTR newLastErrorFile
            = realloc(_LastError_file, sizeof(TCHAR) * (fileLength + 1));

        if (newLastErrorFile)
        {
            *newLastErrorFile = 0;
            _LastError_file = newLastErrorFile;
        }
        else if (_LastError_file)
        {
            free(_LastError_file);
            _LastError_file = NULL;
        }
    }
    /* Copy file into LastError_file. */
    if (_LastError_file)
        _tcsncpy(_LastError_file, file, fileLength + 1);

    _LastError_line = line;
}
