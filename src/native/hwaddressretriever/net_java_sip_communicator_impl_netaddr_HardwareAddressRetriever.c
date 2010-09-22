/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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

