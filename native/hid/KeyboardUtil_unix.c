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
 * \brief Unix specific code to press/release keys.
 * \author Sebastien Vincent
 */

#include "KeyboardUtil.h"

#include <stdio.h>
#include <string.h>

#include <X11/X.h>
#include <X11/Xlib.h>
#include <X11/extensions/XTest.h>

/**
 * \struct keysymcharmap
 * \brief Map structure between the string representation of the keysym and
 * the ascii character.
 */
typedef struct keysymcharmap
{
  char *keysym; /**< String representation of the keysym */
  char key; /**< Ascii character */
} keysymcharmap_t;

/**
 * \var g_symbolmap
 * \brief Map between symbol name and X11 string representation of the keysym.
 */
static char *g_symbolmap[] =
{
  "alt", "Alt_L",
  "altgr", "ISO_Level3_Shift",
  "ctrl", "Control_L",
  "control", "Control_L",
  "meta", "Meta_L",
  "super", "Super_L",
  "shift", "Shift_L",
  NULL, NULL,
};

/**
 * \var g_specialcharmap
 * \brief Map of the special characters.
 */
static keysymcharmap_t g_specialcharmap[] =
{
  {"Return", '\n'},
  {"ampersand", '&'},
  {"apostrophe", '\''},
  {"asciicircum", '^'},
  {"asciitilde", '~'},
  {"asterisk", '*'},
  {"at", '@'},
  {"backslash", '\\'},
  {"bar", '|'},
  {"braceleft", '{'},
  {"braceright", '}'},
  {"bracketleft", '['},
  {"bracketright", ']'},
  {"colon", ':'},
  {"comma", ','},
  {"dollar", '$'},
  {"equal", '='},
  {"exclam", '!'},
  {"grave", '`'},
  {"greater", '>'},
  {"less", '<'},
  {"minus", '-'},
  {"numbersign", '#'},
  {"parenleft", '('},
  {"parenright", ')'},
  {"percent", '%'},
  {"period", '.'},
  {"plus", '+'},
  {"question", '?'},
  {"quotedbl", '"'},
  {"semicolon", ';'},
  {"slash", '/'},
  {"space", ' '},
  {"tab", '\t'},
  {"underscore", '_'},
  {NULL, 0},
};

/**
 * \brief Find X11 string representation of a symbol.
 * \param k human string representation of the symbol
 * \return X11 string representation of the symbol
 */
static char* find_symbol(const char* k)
{
  size_t i = 0;

  while(g_symbolmap[i])
  {
    if(!strcmp(g_symbolmap[i], k))
    {
      return g_symbolmap[i + 1];
    }

    i += 2;
  }
  
  return NULL;
}

/**
 * \brief Find X11 string representation of a special character.
 * \param k ascii representation of the special character
 * \return X11 string representation of the special character
 */
static char* find_keysym(char k)
{
  size_t i = 0;

  while(g_specialcharmap[i].key)
  {
    keysymcharmap_t ks = g_specialcharmap[i];

    if(ks.key == k)
    {
      return ks.keysym;
    }

    i++;
  }

  return NULL;
}

void generateSymbol(const char* symbol, jboolean pressed)
{
  Display *dpy = NULL;
  char* s = NULL;

  dpy = XOpenDisplay(NULL);

  if(!dpy)
  {
    return;
  }

  s = find_symbol(symbol);

  if(!s)
  {
    /* printf("no symbol %s\n", s); */
    XCloseDisplay(dpy);
    return;
  }

  XTestFakeKeyEvent(dpy, XKeysymToKeycode(dpy, XStringToKeysym(s)), pressed ? True : False, 1);

  XFlush(dpy);
  XCloseDisplay(dpy);
}

void generateKey(jchar key, jboolean pressed)
{
  Display *dpy = NULL;

  dpy = XOpenDisplay(NULL);

  if(!dpy)
  {
    return;
  }

  KeySym sym = XStringToKeysym((const char*)&key);
  KeyCode code;

  if(sym == NoSymbol)
  {
    char* special = find_keysym(key);

    if(special)
    {
      sym = XStringToKeysym(special);
    }
    else
    {
      XCloseDisplay(dpy);
      return;
    }
  }

  code = XKeysymToKeycode(dpy, sym);

  XTestFakeKeyEvent(dpy, code, pressed ? True : False, 1);

  XFlush(dpy);
  XCloseDisplay(dpy);
}

