/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
        }
        else
            error = ERROR_FILE_NOT_FOUND;
    }
    return error;
}
