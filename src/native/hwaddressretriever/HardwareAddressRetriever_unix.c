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
 * \file HardwareAddressRetriever_unix.c
 * \brief Hardware address retriever (Unix specific code).
 * \author Sebastien Vincent
 * \date 2010
 */

#if !defined(_WIN32) && !defined(_WIN64)

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <unistd.h>

#include <sys/socket.h>
#include <sys/types.h>
#include <sys/ioctl.h>

#include <net/if.h>

#if defined(__FreeBSD__) || defined(__APPLE__)
#include <ifaddrs.h>
#include <net/if_dl.h>
#include <net/route.h>
#include <net/if_types.h>
#endif

#include "HardwareAddressRetriever.h"

jbyteArray getHardwareAddress(JNIEnv* env, jstring ifName)
{
    int sock = -1;
    struct ifreq ifr;
    jbyteArray hwaddr = NULL;
    char* name = NULL;
    jbyte* addr = NULL;
    int hwlen = 6;

    name = (char*)(*env)->GetStringUTFChars(env, ifName, NULL);

    if(!name)
    {
        return NULL;
    }

#ifdef __linux__

    sock = socket(AF_INET, SOCK_DGRAM, 0);

    if(sock == -1)
    {
        (*env)->ReleaseStringUTFChars(env, ifName, name);
        return NULL;
    }
    
    memset(&ifr, 0x00, sizeof(struct ifreq));

    strncpy(ifr.ifr_name, name, IFNAMSIZ - 1);
    ifr.ifr_name[IFNAMSIZ - 1] = 0x00;

    if(ioctl(sock, SIOCGIFHWADDR, &ifr) != 0)
    {
        (*env)->ReleaseStringUTFChars(env, ifName, name);
        close(sock);
        return NULL;
    }
    
    close(sock);
    addr = (const jbyte*)ifr.ifr_hwaddr.sa_data;

#else /* BSD like */

    struct ifaddrs* addrs = NULL;
    struct ifaddrs* ifa = NULL;
    jbyte buf[hwlen];

    if(getifaddrs(&addrs) != -1)
    {
        for(ifa = addrs ; ifa != NULL ; ifa = ifa->ifa_next)
        {
            if(ifa->ifa_addr->sa_family == AF_LINK && !strcmp(ifa->ifa_name, name))
            {
                struct sockaddr_dl* sdl = (struct sockaddr_dl*)ifa->ifa_addr;

                if(sdl->sdl_type == IFT_ETHER)
                {
                    memcpy(buf, LLADDR(sdl), hwlen);
                    addr = buf;
                    break;
                }
            }
        }

        freeifaddrs(addrs);
    }

#endif

    if(addr)
    {
        hwaddr = (*env)->NewByteArray(env, hwlen);

        if(hwaddr)
        {
            /* copy the hardware address and return it */
            (*env)->SetByteArrayRegion(env, hwaddr, 0, hwlen, addr);
        }
    }

    /* cleanup */
    (*env)->ReleaseStringUTFChars(env, ifName, name);
    return hwaddr;
}

#endif

