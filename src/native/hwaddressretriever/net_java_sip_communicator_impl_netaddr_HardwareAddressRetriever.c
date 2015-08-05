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
 * \file net_java_sip_communicator_impl_netaddr_HardwareAddressRetriever.h
 * \brief Hardware address retriever.
 * \author Sebastien Vincent
 * \date 2010
 */

#include <stdio.h>
#include <stdlib.h>

#include <jni.h>

#include "HardwareAddressRetriever.h"

/**
 * \brief Returns the byte array representing hardware address.
 * \param env JVM environment
 * \param clazz Java class
 * \param ifName name of the interface
 * \return byte array representing the hardware address or NULL if anything
 * goes wrong
 */
JNIEXPORT jbyteArray JNICALL Java_net_java_sip_communicator_impl_netaddr_HardwareAddressRetriever_getHardwareAddress
  (JNIEnv* env, jclass clazz, jstring ifName)
{
  return getHardwareAddress(env, ifName);
}

