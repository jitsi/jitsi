/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

/**
 * \file net_java_sip_communicator_util_KeyboardUtil.c
 * \brief Native method to get keycode from ascii.
 * \author Sebastien Vincent
 */

#include "net_java_sip_communicator_impl_hid_NativeKeyboard.h"

#include "KeyboardUtil.h"

/**
 * \brief Press or release a key. 
 * \param env JNI environment
 * \param clazz class
 * \param key string representation of the key
 * \param pressed true if the key has to be pressed, false if the key has to be released
 */
JNIEXPORT void JNICALL Java_net_java_sip_communicator_impl_hid_NativeKeyboard_doKeyAction
  (JNIEnv *env, jclass clazz, jchar key, jboolean pressed)
{
  /* avoid warnings */
  env = env;
  clazz = clazz;

  generateKey(key, pressed);
}

/**
 * \brief Press or release a symbol (i.e. CTRL, ALT, ...).
 * \param env JNI environment
 * \param clazz class
 * \param symbol symbol string representation
 * \param pressed true if the key has to be pressed, false if the key has to be released
 */
JNIEXPORT void JNICALL Java_net_java_sip_communicator_impl_hid_NativeKeyboard_doSymbolAction
  (JNIEnv *env, jclass clazz, jstring symbol, jboolean pressed)
{
  const char* s;
  
  /* avoid warnings */
  env = env;
  clazz = clazz;

  s = (*env)->GetStringUTFChars(env, symbol, 0);

  if(s)
  {
    generateSymbol(s, pressed);
    (*env)->ReleaseStringUTFChars(env, symbol, s);
  }
}

