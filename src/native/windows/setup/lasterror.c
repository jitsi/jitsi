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
