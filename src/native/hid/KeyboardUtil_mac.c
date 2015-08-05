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
 * \file KeyboardUtil_unix.c
 * \brief Mac OS X specific code to press/release keys.
 * \author Sebastien Vincent
 */

#include <ApplicationServices/ApplicationServices.h>
#include <Carbon/Carbon.h>

#include "KeyboardUtil.h"

/**
 * \brief Get the keycode from a specific keyboard layout.
 * \param c ascii character
 * \param uchrHeader keyboard layout
 * \param pshift if the character need shift modifier, value will have 1 otherwise 0
 * \param palt if the character need alt modifier, value will have 1 otherwise 0
 */
void checkModifiers(const char c, const UCKeyboardLayout* uchrHeader, CGKeyCode keycode, int* pshift, int* palt)
{
  int alt = (optionKey >> 8) & 0xff;
  int shift = (shiftKey >> 8) & 0xff;
  int altShift = ((optionKey | shiftKey) >> 8) & 0xff;
  UInt32 deadKeyState = 0;
  UniCharCount count;
  char character;

  if(UCKeyTranslate(uchrHeader, keycode, kUCKeyActionDown, alt, LMGetKbdType(), 0,
        &deadKeyState, 1, &count, &character) == 0 && character == c)
  {
    *palt = 1;
  }
  else if(UCKeyTranslate(uchrHeader, keycode, kUCKeyActionDown, shift, LMGetKbdType(), 0,
        &deadKeyState, 1, &count, &character) == 0 && character == c)
  {
    *pshift = 1;
  }
  else if(UCKeyTranslate(uchrHeader, keycode, kUCKeyActionDown, altShift, LMGetKbdType(), 0,
        &deadKeyState, 1, &count, &character) == 0 && character == c)
  {
    *pshift = 1;
    *palt = 1;
  }
}

/* following two functions has been taken from
 * http://stackoverflow.com/questions/1918841/how-to-convert-ascii-character-to-cgkeycode
 */

/**
 * \brief Get the keycode from a specific keyboard layout.
 * \param c ascii character
 * \param uchrHeader keyboard layout
 * \param shift if the character needs shift modifier, value will have 1 otherwise 0
 * \param alt if the character needs alt modifier, value will have 1 otherwise 0
 * \return keycode or UINT16_MAX if not found
 * \note Function taken from http://stackoverflow.com/questions/1918841/how-to-convert-ascii-character-to-cgkeycode
 */
CGKeyCode keyCodeForCharWithLayout(const char c,
    const UCKeyboardLayout *uchrHeader, int* shift, int* alt)
{
  uint8_t *uchrData = (uint8_t *)uchrHeader;
  UCKeyboardTypeHeader *uchrKeyboardList = (UCKeyboardTypeHeader*)uchrHeader->keyboardTypeList;

  /* Loop through the keyboard type list. */
  ItemCount i, j;
  for (i = 0; i < uchrHeader->keyboardTypeCount; ++i)
  {
    /* Get a pointer to the keyToCharTable structure. */
    UCKeyToCharTableIndex *uchrKeyIX = (UCKeyToCharTableIndex *)
      (uchrData + (uchrKeyboardList[i].keyToCharTableIndexOffset));

    /* Not sure what this is for but it appears to be a safeguard... */
    UCKeyStateRecordsIndex *stateRecordsIndex;
    if(uchrKeyboardList[i].keyStateRecordsIndexOffset != 0)
    {
      stateRecordsIndex = (UCKeyStateRecordsIndex *)
        (uchrData + (uchrKeyboardList[i].keyStateRecordsIndexOffset));

      if((stateRecordsIndex->keyStateRecordsIndexFormat) !=
          kUCKeyStateRecordsIndexFormat)
      {
        stateRecordsIndex = NULL;
      }
    }
    else
    {
      stateRecordsIndex = NULL;
    }

    /* Make sure structure is a table that can be searched. */
    if((uchrKeyIX->keyToCharTableIndexFormat) != kUCKeyToCharTableIndexFormat)
    {
      continue;
    }

    /* Check the table of each keyboard for character */
    for (j = 0; j < uchrKeyIX->keyToCharTableCount; ++j)
    {
      UCKeyOutput *keyToCharData =
        (UCKeyOutput *)(uchrData + (uchrKeyIX->keyToCharTableOffsets[j]));

      /* Check THIS table of the keyboard for the character. */
      UInt16 k;
      for (k = 0; k < uchrKeyIX->keyToCharTableSize; ++k)
      {
        /* Here's the strange safeguard again... */
        if((keyToCharData[k] & kUCKeyOutputTestForIndexMask) ==
            kUCKeyOutputStateIndexMask)
        {
          long keyIndex = (keyToCharData[k] & kUCKeyOutputGetIndexMask);
          if(stateRecordsIndex != NULL &&
              keyIndex <= (stateRecordsIndex->keyStateRecordCount))
          {
            UCKeyStateRecord *stateRecord = (UCKeyStateRecord *)
              (uchrData +
               (stateRecordsIndex->keyStateRecordOffsets[keyIndex]));

            if((stateRecord->stateZeroCharData) == c)
            {
              checkModifiers(c, uchrHeader, k, shift, alt);
              return (CGKeyCode)k;
            }
          }
          else if(keyToCharData[k] == c)
          {
            checkModifiers(c, uchrHeader, k, shift, alt);
            return (CGKeyCode)k;
          }
        }
        else if(((keyToCharData[k] & kUCKeyOutputTestForIndexMask)
              != kUCKeyOutputSequenceIndexMask) &&
            keyToCharData[k] != 0xFFFE &&
            keyToCharData[k] != 0xFFFF &&
            keyToCharData[k] == c)
        {
          /* try to see if character is obtained with modifiers such as 
           * Option (alt) or shift
           */
          checkModifiers(c, uchrHeader, k, shift, alt);
          return (CGKeyCode)k;
        }
      }
    }
  }

  return UINT16_MAX;
}

/**
 * \brief Get the keycode from a specific keyboard layout.
 * \param c ascii character
 * \param shift if the character need shift modifier, value will have 1 otherwise 0
 * \param alt if the character need shift modifier, value will have 1 otherwise 0
 * \return keycode or UINT16_MAX if not found
 * \note Function taken from http://stackoverflow.com/questions/1918841/how-to-convert-ascii-character-to-cgkeycode
 */
CGKeyCode keyCodeForChar(const char c, int* shift, int* alt)
{
  CFDataRef currentLayoutData;
  TISInputSourceRef currentKeyboard = TISCopyCurrentKeyboardInputSource();

  if(currentKeyboard == NULL)
  {
    printf("Could not find keyboard layout\n");
    return UINT16_MAX;
  }

  currentLayoutData = TISGetInputSourceProperty(currentKeyboard,
      kTISPropertyUnicodeKeyLayoutData);
  CFRelease(currentKeyboard);
  if(currentLayoutData == NULL)
  {
    printf("Could not find layout data\n");
    return UINT16_MAX;
  }

  return keyCodeForCharWithLayout(c,
      (const UCKeyboardLayout *)CFDataGetBytePtr(currentLayoutData), shift, alt);
}

void generateSymbol(const char* symbol, jboolean pressed)
{
  /* avoid warnings */
  symbol = symbol;
  pressed = pressed;
}

void generateKey(jchar key, jboolean pressed)
{
  int keycode = -1; 
  int shift = 0;
  int alt = 0;
  CGEventRef e = NULL;
  int flags = 0;

  keycode = keyCodeForChar((char)key, &shift, &alt);

  if(keycode == UINT16_MAX)
  {
    return;
  }

  e = CGEventCreateKeyboardEvent(NULL, keycode, pressed ? 1 : 0);

  if(pressed && shift)
  {
    flags |= kCGEventFlagMaskShift;
  }

  if(pressed && alt)
  {
    flags |= kCGEventFlagMaskAlternate;
  }

  CGEventSetFlags(e, flags);

  CGEventPost(kCGSessionEventTap, e);
  CFRelease(e);
}

