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

