/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

/**
 * \file HardwareAddressRetriever.h
 * \brief Hardware address retriever.
 * \author Sebastien Vincent
 * \date 2010
 */

#ifndef HARDWARE_ADDRESS_RETRIEVER_H
#define HARDWARE_ADDRESS_RETRIEVER_H

#include <jni.h>

/**
 * \brief Returns the hardware address for the specified interface.
 * \param env JNI environment
 * \param ifName name of the interface that we want to get the hardware address
 * \return byte array representing hardware address or NULL if not found or
 * system related errors happen.
 */
jbyteArray getHardwareAddress(JNIEnv* env, jstring ifName);

#endif /* HARDWARE_ADDRESS_RETRIEVER_H */

