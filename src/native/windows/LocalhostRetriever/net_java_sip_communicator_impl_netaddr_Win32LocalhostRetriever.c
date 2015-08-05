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
 * \file net_java_sip_communicator_impl_netaddr_Win32LocalhostRetriever.c
 * \brief Get source address for a destination address.
 * \author Sebastien Vincent
 * \date 2009
 */

/**
 * \def WIN32_LEAN_AND_MEAN
 * \brief Exclude not commonly used headers from win32 API.
 *
 * It excludes some unused stuff from windows headers and 
 * by the way code compiles faster.
 */
#define WIN32_LEAN_AND_MEAN

#include <stdio.h>
#include <stdlib.h>

#include <windows.h>

#include <ws2tcpip.h>
#include <iphlpapi.h>

#include "net_java_sip_communicator_impl_netaddr_Win32LocalhostRetriever.h"

/**
 * \define ADAPTERS_DEFAULT_SIZE
 * \brief Default size for GetAdapterAddresses's SizePointer parameter.
 * 
 * We set a relatively high value to not called two times
 * GetAdapterAddresses() which is slow.
 */
#define ADAPTERS_DEFAULT_SIZE 15000

/**
 * \brief Get interface for a destination route.
 * \param addr destination address
 * \param src source address, this parameter will be filled in function succeed
 * \return 0 if success, -1 otherwise
 */
static int get_source_for_destination(struct sockaddr* dst, struct sockaddr_storage* src)
{
  DWORD ifindex = 0;
  IP_ADAPTER_ADDRESSES* allAdapters = NULL;
  IP_ADAPTER_ADDRESSES* adapter = NULL;
  ULONG size = ADAPTERS_DEFAULT_SIZE;
  ULONG ret = 0;
  BOOL found = FALSE;

  /* need a valid pointer */
  if(!src || !dst)
  {
    return -1;
  }

  /* get output interface index for specific destination address */
  if(GetBestInterfaceEx(dst, &ifindex) != NO_ERROR)
  {
    return -1;
  }

  do
  {
    /* we should loop only if host has more than 
     * (ADAPTERS_DEFAULT_SIZE / sizeof(IP_ADAPTER_ADDRESSES)) interfaces
     */
    allAdapters = malloc(size);

    if(!allAdapters)
    {
      /* out of memory */
      return -1;
    }

    /* get the list of host addresses and try to find 
     * the index
     */
    ret = GetAdaptersAddresses(dst->sa_family, /* return same address family as destination */
        GAA_FLAG_INCLUDE_ALL_INTERFACES | GAA_FLAG_INCLUDE_PREFIX | GAA_FLAG_SKIP_DNS_SERVER | 
        GAA_FLAG_SKIP_FRIENDLY_NAME | GAA_FLAG_SKIP_MULTICAST,
        NULL, /* reserved */
        allAdapters,
        &size);

    if(ret == ERROR_BUFFER_OVERFLOW)
    {
      /* free memory as the loop will allocate again with
       * proper size
       */
      free(allAdapters);
    }
  }while(ret == ERROR_BUFFER_OVERFLOW);

  if(ret != ERROR_SUCCESS)
  {
    free(allAdapters);
    return -1;
  }

  adapter = allAdapters;

  while(adapter)
  {
    /* find the right adapter for interface index return by GetBestInterface */
    if(dst->sa_family == AF_INET && (adapter->IfIndex == ifindex))
    {
      IP_ADAPTER_UNICAST_ADDRESS* unicast = adapter->FirstUnicastAddress;
      if(unicast != NULL)
      {
        struct sockaddr_in* addr = (struct sockaddr_in*)unicast->Address.lpSockaddr;

        memcpy(src, addr, sizeof(struct sockaddr_in));
        found = TRUE;

        /* found source address, break the loop */
        break;
      }
    }
    else if(dst->sa_family == AF_INET6 && (adapter->Ipv6IfIndex == ifindex))
    {
      /* XXX multihoming on IPv6 interfaces, they can have 
       * multiple global addresses (+ link-local address), handle this case 
       */
      IP_ADAPTER_UNICAST_ADDRESS* unicast = adapter->FirstUnicastAddress;
      if(unicast != NULL)
      {
        struct sockaddr_in6* addr = (struct sockaddr_in6*)unicast->Address.lpSockaddr;

        memcpy(src, addr, sizeof(struct sockaddr_in6));
        found = TRUE;

        /* found source address, break the loop */
        break;
      }
    }

    adapter = adapter->Next;
  }

  /* cleanup */
  free(allAdapters);

  return found ? 0 : -1;
}

/**
 * \brief JNI native method to get source address for a destination.
 * \param env JVM environment
 * \param class class that call method (Win32LocalhostRetriever)
 * \param dst destination address
 * \return source address for the destination
 */
JNIEXPORT jbyteArray JNICALL Java_net_java_sip_communicator_impl_netaddr_Win32LocalhostRetriever_getSourceForDestination
  (JNIEnv *env, jclass class, jbyteArray dst)
{
  jbyteArray array = NULL;
  jsize len = 0;
  struct sockaddr_storage source;
  struct sockaddr_in dstv4;
  struct sockaddr_in6 dstv6;
  struct sockaddr* destination = NULL;
  char* buf = NULL; 

  /* get the bytes */
  len = (*env)->GetArrayLength(env, dst);
  buf = (char*)(*env)->GetByteArrayElements(env, dst, NULL);

  if(len == 4)
  {
    /* IPv4 processing */
    dstv4.sin_family = AF_INET;
    memcpy(&dstv4.sin_addr, buf, 4);
    dstv4.sin_port = 0;
    destination = (struct sockaddr*)&dstv4;
  }
  else if(len == 16)
  {
    /* IPv6 processing */
    dstv6.sin6_family = AF_INET6;
    memcpy(&dstv6.sin6_addr, buf, 16);
    dstv6.sin6_scope_id = 0;
    dstv6.sin6_flowinfo = 0;
    dstv6.sin6_port = 0;
    destination = (struct sockaddr*)&dstv6;
  }
  else
  {
    /* printf("not an IPv4 or IPv6 address\n"); */
    (*env)->ReleaseByteArrayElements(env, dst, (jbyte*)buf, 0);
    return NULL;
  }
  
  /* cleanup */
  (*env)->ReleaseByteArrayElements(env, dst, (jbyte*)buf, 0);
  
  /* find the source address for a specific destination */
  if(get_source_for_destination(destination, &source) == -1)
  {
    return NULL;
  }

  /* create jbyteArray that will contains raw address */
  array = (*env)->NewByteArray(env, len);
  if(!array)
  { 
    return NULL;
  }

  /* get internal buffer */
  buf = (char*)(*env)->GetByteArrayElements(env, array, NULL);
  if(!buf)
  {
    return NULL;
  }

  /* copy raw address */
  if(source.ss_family == AF_INET)
  {
    /* IPv4 */
    memcpy(buf, &((struct sockaddr_in*)&source)->sin_addr, len);
  }
  else
  {
    /* IPv6 */
    memcpy(buf, &((struct sockaddr_in6*)&source)->sin6_addr, len);
  }

  /* ReleaseByteArrayElements() updates array with buf's content 
   * when the last parameter is 0.
   */
  (*env)->ReleaseByteArrayElements(env, array, (jbyte*)buf, 0);
  return array;
}

/* test */
#if 0

#include <winsock2.h>

int main(int argc, char** argv)
{
  struct sockaddr_in addr4;
  struct sockaddr_in6 addr6;
  struct sockaddr_storage st;
  struct sockaddr_storage st2;
  struct sockaddr_in* src4 = (struct sockaddr_in*)&st;
  struct sockaddr_in6* src6 = (struct sockaddr_in6*)&st2;
  char buf[64];
  DWORD len = 0;
  WSADATA wsaData;

  /* initialization stuff */
  WSAStartup(MAKEWORD(2, 0), &wsaData);

  /* IPv4 destination */

  printf("Try to find source address for destination 130.79.200.1\n");
  len = sizeof(addr4);

  if(WSAStringToAddressA("130.79.200.1", AF_INET, NULL, (struct sockaddr*)&addr4, &len) != 0)
  {
    printf("WSAStringToAddressA failed: %ld\n", WSAGetLastError());
    exit(EXIT_FAILURE); 
  }

  if(get_source_for_destination((struct sockaddr*)&addr4, &st) == 0)
  {
    len = sizeof(buf);
    if(WSAAddressToStringA((struct sockaddr*)src4, sizeof(*src4), NULL, buf, &len) != 0)
    {
      printf("WSAAddressToString failed: %ld\n", WSAGetLastError());
      exit(EXIT_FAILURE); 
    }

    printf("Source address is %s\n", buf);
  }
  else
  {
    printf("get_source_for_destination failed for IPv4!\n");
  }

  printf("================================================================\n");
  
  /* IPv6 destination */
  
  printf("Try to find source address for destination 2001:660:4701:1001::3\n");
  len = sizeof(addr6);

  if(WSAStringToAddressA("2001:660:4701:1001::3", AF_INET6, NULL, (struct sockaddr*)&addr6, &len) != 0)
  {
    printf("WSAStringToAddressA failed: %ld\n", WSAGetLastError());
    exit(EXIT_FAILURE); 
  }

  if(get_source_for_destination((struct sockaddr*)&addr6, &st2) == 0)
  {
    len = sizeof(buf);

    if(WSAAddressToStringA((struct sockaddr*)src6, sizeof(*src6), NULL, buf, &len) != 0)
    {
      printf("WSAAddressToString failed: %ld\n", WSAGetLastError());
      exit(EXIT_FAILURE); 
    }

    printf("Source address is %s\n", buf);
  }
  else
  {
    printf("get_source_for_destination failed for IPv6!\n");
  }
  
  return EXIT_SUCCESS;
}

#endif

