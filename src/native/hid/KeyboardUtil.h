/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

/**
 * \file KeyboardUtil.h
 * \brief Prototypes of the function to press/release keys.
 * \author Sebastien Vincent
 */

#ifndef KEYBOARD_UTIL_H
#define KEYBOARD_UTIL_H

#include <jni.h>

/**
 * \brief Press or release a key.
 * \param key ascii code of the key
 * \param pressed if the key have to be pressed or released
 */
void generateKey(jchar key, jboolean pressed);

/**
 * \brief Press or release a symbol.
 * \param symbol symbol name
 * \param pressed if the key have to be pressed or released
 */
void generateSymbol(const char* symbol, jboolean pressed);

#endif /* KEYBOARD_UTIL_H */

