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

/**
 * \file HardwareAddressRetriever_win.c
 * \brief Hardware address retriever (Windows specific code).
 * \author Sebastien Vincent
 * \date 2010
 */

#if defined(_WIN32) || defined(_WIN64)

/**
 * \def WIN32_LEAN_AND_MEAN
 * \brief Exclude not commonly used headers from win32 API.
 *
 * It excludes some unused stuff from windows headers and
 * by the way code compiles faster.
 */
#define WIN32_LEAN_AND_MEAN

#include <stdlib.h>
#include <windows.h>

#include <ws2tcpip.h>
#include <iphlpapi.h>

#include "HardwareAddressRetriever.h"

jbyteArray getHardwareAddress(JNIEnv* env, jstring ifName)
{
    MIB_IFTABLE* ifTable = NULL;
    ULONG size = 15000;
    int found = 0;
    jbyteArray hwaddr = NULL;
    DWORD ret = 0;
    DWORD i = 0;
    WCHAR* wname = NULL;
    MIB_IFROW ifi;
    jclass clazz = (*env)->GetObjectClass(env, ifName);
    jmethodID method = (*env)->GetMethodID(env, clazz, "compareTo", "(Ljava/lang/String;)I");

    if(method == NULL)
    {
        return NULL;
    }

    memset(&ifi, 0x00, sizeof(MIB_IFROW));

    do
    {
        ifTable = malloc(size);

        if(!ifTable)
        {
            /* out of memory */
            return NULL;
        }

        ret = GetIfTable(ifTable, &size, 1);

    }while(ret == ERROR_INSUFFICIENT_BUFFER);

    if(ret != ERROR_SUCCESS)
    {
        free(ifTable);
        return NULL;
    }

    for(i = 0 ; i < ifTable->dwNumEntries ; i++)
    {
        jstring tmp = NULL;
        ifi = ifTable->table[i];

        if(ifi.dwType == IF_TYPE_OTHER)
        {
            continue;
        }

        /* jstring created by NewStringUTF will be garbage collected at 
         * the end of the function 
         */
        tmp = (*env)->NewStringUTF(env, ifi.bDescr);

        if(!tmp)
        {
            /* printf("error\n"); */
            continue;
        }

        if((*env)->CallIntMethod(env, ifName, method, tmp) == 0)
        {
            found = 1;
            break;
        }
    }

    if(found)
    {
        DWORD hwlen = ifi.dwPhysAddrLen;

        if(hwlen > 0)
        {
            hwaddr = (*env)->NewByteArray(env, hwlen);

            if(hwaddr)
            {
                /* copy the hardware address and return it */
                (*env)->SetByteArrayRegion(env, hwaddr, 0, hwlen, ifi.bPhysAddr);
            }
        }
    }

    /* cleanup */
    free(ifTable);

    return hwaddr;
}

#endif

