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
 * The number of registries known for the different Outlook version.
 */
int nbOutlookRegister = 4;


/**
 * The registries known for the different Outlook version.
 */
TCHAR outlookRegister[][MAX_PATH] = {
    TEXT("{E83B4360-C208-4325-9504-0D23003A74A5}"), // Outlook 2013
    TEXT("{1E77DE88-BCAB-4C37-B9E5-073AF52DFD7A}"), // Outlook 2010
    TEXT("{24AAE126-0911-478F-A019-07B875EB9996}"), // Outlook 2007
    TEXT("{BC174BAD-2F53-4855-A1D5-0D575C19B1EA}")  // Outlook 2003
};

/**
 * Returns the bitness of the Outlook installation.
 *
 * @return 64 if Outlook 64 bits version is installed. 32 if Outlook 32 bits
 * version is installed. -1 otherwise.
 */
int MAPIBitness_getOutlookBitnessVersion(void)
{
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

/**
 * Returns the Outlook version installed.
 *
 * @return 2013 for "Outlook 2013", 2010 for "Outlook 2010", 2007 for "Outlook
 * 2007" or 2003 for "Outlook 2003". -1 otherwise.
 */
int MAPIBitness_getOutlookVersion(void)
{
    int outlookVersions[] = {
        2013, // Outlook 2013
        2010, // Outlook 2010
        2007, // Outlook 2007
        2003 // Outlook 2003
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
            return outlookVersions[i];
        }
        else if(MsiProvideQualifiedComponent(
                    outlookRegister[i],
                    TEXT("outlook.exe"),
                    (DWORD) INSTALLMODE_DEFAULT,
                    NULL,
                    &pathLength)
                == ERROR_SUCCESS)
        {
            return outlookVersions[i];
        }
    }

    return -1;
}
