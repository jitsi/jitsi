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

