/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#include "MAPIBitness.h"

#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <Msi.h>
#include <stdio.h>

/**
 * Checks the bitness of the Outlook installation and of the Jitsi executable.
 *
 * @author Vincent Lucas
 */

/**
 * Returns the bitness of the Outlook installation.
 *
 * @return 64 if Outlook 64 bits version is installed. 32 if Outlook 32 bits
 * version is installed. -1 otherwise.
 */
int MAPIBitness_getOutlookBitnessVersion(void)
{
    int nbOutlookRegister = 3;
    TCHAR outlookRegister[][MAX_PATH] = {
        TEXT("{E83B4360-C208-4325-9504-0D23003A74A5}"), // Outlook 2013
        TEXT("{1E77DE88-BCAB-4C37-B9E5-073AF52DFD7A}"), // Outlook 2010
        TEXT("{24AAE126-0911-478F-A019-07B875EB9996}"), // Outlook 2007
        TEXT("{BC174BAD-2F53-4855-A1D5-0D575C19B1EA}")  // Outlook 2003
    };

    DWORD pathLength = 0;

    for(int i = 0; i < nbOutlookRegister; ++i)
    {
        if(MsiProvideQualifiedComponent(
                    outlookRegister[i],
                    TEXT("outlook.x64.exe"),
                    (DWORD) INSTALLMODE_DEFAULT,
                    NULL,
                    &pathLength)
                == ERROR_SUCCESS)
        {
            return 64;
        }
        else if(MsiProvideQualifiedComponent(
                    outlookRegister[i],
                    TEXT("outlook.exe"),
                    (DWORD) INSTALLMODE_DEFAULT,
                    NULL,
                    &pathLength)
                == ERROR_SUCCESS)
        {
            return 32;
        }
    }

    return -1;
}
