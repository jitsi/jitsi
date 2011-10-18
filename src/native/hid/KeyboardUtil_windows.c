/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

/**
 * \file KeyboardUtil_unix.c
 * \brief Windows specific code to press/release keys.
 * \author Sebastien Vincent
 */

#include <windows.h>

#include "KeyboardUtil.h"

void generateSymbol(const char* symbol, jboolean pressed)
{
    /* on Windows AltGr correspond to CTRL-ALT */
    if(!strcmp(symbol, "altgr"))
    {
        int scancode = MapVirtualKey(VK_CONTROL, 0);
        keybd_event(VK_CONTROL, scancode, pressed ? 0 : KEYEVENTF_KEYUP, 0); 
        scancode = MapVirtualKey(VK_MENU, 0);
        keybd_event(VK_MENU, scancode, pressed ? 0 : KEYEVENTF_KEYUP, 0); 
    }
    else if(!strcmp(symbol, "shift"))
    {
        int scancode = MapVirtualKey(VK_SHIFT, 0);
        keybd_event(VK_SHIFT, scancode, pressed ? 0 : KEYEVENTF_KEYUP, 0); 
    }
    else if(!strcmp(symbol, "ctrl"))
    {
        int scancode = MapVirtualKey(VK_CONTROL, 0);
        keybd_event(VK_CONTROL, scancode, pressed ? 0 : KEYEVENTF_KEYUP, 0); 
    }
    else if(!strcmp(symbol, "alt"))
    {
        int scancode = MapVirtualKey(VK_MENU, 0);
        keybd_event(VK_MENU, scancode, pressed ? 0 : KEYEVENTF_KEYUP, 0); 
    }
    else if(!strcmp(symbol, "hankaku"))
    {
        /* XXX constant name for HANKAKU ? */
        /* 
        int scancode = MapVirtualKey(VK_HANKAKU, 0);
        keybd_event(VK_HANKAKU, scancode, pressed ? 0 : KEYEVENTF_KEYUP, 0); 
        */
    }
}

void generateKey(jchar key, jboolean pressed)
{
    SHORT letter = 0;
    TCHAR ch = key;
    UINT scancode = 0;
    SHORT modifiers = 0;

    letter = VkKeyScan(ch);

    if(letter == -1)
    {
        /* printf("No key found\n"); */
        return;
    }

    modifiers = HIBYTE(letter);
    letter = LOBYTE(letter);
       
    if(pressed)
    {
        /* shift */
        if(modifiers & 1)
        {
            generateSymbol("shift", JNI_TRUE);
        }

        /* ctrl */
        if(modifiers & 2)
        {
            generateSymbol("ctrl", JNI_TRUE);
        }

        /* alt */
        if(modifiers & 4)
        {
            generateSymbol("alt", JNI_TRUE);
        }

        /* hankaku */
        if(modifiers & 8)
        {
            generateSymbol("hankaku", JNI_TRUE);
        }
    }

    /* find scancode */
    scancode = MapVirtualKey(letter, 0);

    /* press and release key as well as modifiers */
    keybd_event(letter, scancode, pressed ? 0 : KEYEVENTF_KEYUP, 0);

    if(!pressed)
    {
        /* shift */
        if(modifiers & 1)
        {
            generateSymbol("shift", JNI_FALSE);
        }

        /* ctrl */
        if(modifiers & 2)
        {
            generateSymbol("ctrl", JNI_FALSE);
        }

        /* alt */
        if(modifiers & 4)
        {
            generateSymbol("alt", JNI_FALSE);
        }

        /* hankaku */
        if(modifiers & 8)
        {
            generateSymbol("hankaku", JNI_FALSE);
        }
    }
}


