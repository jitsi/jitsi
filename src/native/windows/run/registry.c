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

#include "registry.h"

#include <stdlib.h>

LONG
Run_getRegSzValue(HKEY key, LPCTSTR name, LPTSTR *data)
{
    LONG error;
    DWORD type;
    DWORD size;

    error = RegQueryValueEx(key, name, NULL, &type, NULL, &size);
    if (ERROR_SUCCESS == error)
    {
        if ((REG_SZ == type) && size)
        {
            *data = (LPTSTR) malloc(size + sizeof(TCHAR));

            if (*data)
            {
                error
                    = RegQueryValueEx(
                            key,
                            name,
                            NULL,
                            &type,
                            (LPBYTE) (*data),
                            &size);
                if (ERROR_SUCCESS == error)
                {
                    if ((REG_SZ == type) && size)
                        *((*data) + (size / sizeof(TCHAR))) = 0;
                    else
                    {
                        error = ERROR_FILE_NOT_FOUND;
                        free(*data);
                        *data = NULL;
                    }
                }
            }
            else
                error = ERROR_NOT_ENOUGH_MEMORY;
        }
        else
            error = ERROR_FILE_NOT_FOUND;
    }
    return error;
}
