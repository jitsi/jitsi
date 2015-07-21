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

#include "net_java_sip_communicator_impl_globalshortcut_NativeKeyboardHook.h"

#include <cstdlib>
#include <cstring>
#include <list>

#include "javakey.h"

/* MS Windows specific code */

/**
 * \def WIN32_LEAN_AND_MEAN
 * \brief Exclude not commonly used headers from win32 API.
 *
 * It excludes some unused stuff from windows headers and
 * by the way code compiles faster.
 */
#define WIN32_LEAN_AND_MEAN

#include <windows.h>
#include <process.h>

/**
 * \def WINDOW_SHORTCUT_NAME
 * \brief Name of the window.
 */
#define WINDOW_SHORTCUT_NAME L"Jitsi Global Shortcut hook"

/**
 * \class keystrok
 * \brief keystrok class.
 */
class keystrok
{
  public:
    DWORD vkcode; /**< Virtual keycode. */
    int modifiers; /**< Modifiers (ALT, CTLR, ...). */
    int id; /**< ID. */
    int active; /**< If the hotkey is active. */
    bool is_on_key_release; /**< If java object delegate should be notified
                                 when the hotkey is released. */
};

/**
 * \class keystroke_special
 * \brief keystroke class for special keystrokes.
 */
class keystroke_special
{
  public:
    int keycode; /**< Keycode. */
    bool is_on_key_release; /**< If java object delegate should be notified
                                 when the hotkey is released. */
};
/**
 * \class hotkey_release
 * \brief stores information about hotkey that will be realesed.
 */
class hotkey_release
{
  public:
    std::list<DWORD> vkcodes; /**< Virtual keycode that is pressed. */
    int java_keycode; /**< keycode of the hotkey that is pressed. */
    int java_modifiers; /**< modifiers (ALT, CTLR, ...) of the hotkey that is pressed. */
};

/**
 * \class keyboard_hook
 * \brief keyboard_hook class.
 */
class keyboard_hook
{
  public:
    int thread_id; /**< Thread ID. */
    jobject delegate; /**< Java object delegate. */
    JavaVM* jvm; /**< Java VM. */
    int running; /**< Running state. */
    HHOOK hook; /**< Windows keyboard hook. */
    HWND hwnd; /**< Handle of the window that will receive event. */
    int hotkey_next_id; /**< Next ID to use for a  new hotkey. */ 
    std::list<keystrok> keystrokes; /**< List of keystrokes registered. */
    std::list<keystroke_special> specials; /**< List of special keystrokes registered. */
    bool detect; /**< If we are in detection mode. */
    hotkey_release  hk_release; /**< hotkey that is pressed and it will notify 
                                     the java object delegate on release. */
};

/**
 * \enum windows_keycode
 * \brief Windows keycode.
 */
enum jwindows_keycode 
{
  //VK_ADD = 0xC1,
  //VK_BACK = 0x08,
  //VK_CLEAR = 0x0C,
  //VK_DIVIDE = 0x6F,
  //VK_ICO_HELP = 0xE3,
  VK_KEY_0 = 0x30,
  VK_KEY_1 = 0x31,
  VK_KEY_2 = 0x32,
  VK_KEY_3 = 0x33,
  VK_KEY_4 = 0x34,
  VK_KEY_5 = 0x35,
  VK_KEY_6 = 0x36,
  VK_KEY_7 = 0x37,
  VK_KEY_8 = 0x38,
  VK_KEY_9 = 0x39,
  VK_KEY_A = 0x41,
  VK_KEY_B = 0x42,
  VK_KEY_C = 0x43,
  VK_KEY_D = 0x44,
  VK_KEY_E = 0x45,
  VK_KEY_F = 0x46,
  VK_KEY_G = 0x47,
  VK_KEY_H = 0x48,
  VK_KEY_I = 0x49,
  VK_KEY_J = 0x4A,
  VK_KEY_K = 0x4B,
  VK_KEY_L = 0x4C,
  VK_KEY_M = 0x4D,
  VK_KEY_N = 0x4E,
  VK_KEY_O = 0x4F,
  VK_KEY_P = 0x50,
  VK_KEY_Q = 0x51,
  VK_KEY_R = 0x52,
  VK_KEY_S = 0x53,
  VK_KEY_T = 0x54,
  VK_KEY_U = 0x55,
  VK_KEY_V = 0x56,
  VK_KEY_W = 0x57,
  VK_KEY_X = 0x58,
  VK_KEY_Y = 0x59,
  VK_KEY_Z = 0x5A,
  //VK_MULTIPLY = 0x6A
  //VK_OEM_1 = 0xBA,
  //VK_OEM_2 = 0xBF,
  //VK_OEM_3 = 0xC0,
  //VK_OEM_4 = 0xDB,
  //VK_OEM_5 = 0xDC,
  //VK_OEM_6 = 0xDD,
  //VK_OEM_7 = 0xDE,
  //VK_OEM_8 = 0xDF,
  //VK_OEM_PERIOD = 0xBE,
  //VK_OEM_PLUS = 0xBB,
  //VK_F1 = 0x70,
  //VK_F10 = 0x79,
  //VK_F11 = 0x7A,
  //VK_F12 = 0x7B,
  //VK_F2 = 0x71,
  //VK_F3 = 0x72,
  //VK_F4 = 0x73,
  //VK_F5 = 0x74,
  //VK_F6 = 0x75,
  //VK_F7 = 0x76,
  //VK_F8 = 0x77,
  //VK_F9 = 0x78,
  //VK_DELETE = 0x2E,
  //VK_HELP = 0x2F,
  //VK_HOME = 0x24,
  //VK_PRIOR = 0x21
  //VK_NEXT = 0x22
  //VK_END = 0x23,
  //VK_DOWN = 0x28,
  //VK_RIGHT = 0x27,
  //VK_UP = 0x26,
  //VK_LEFT = 0x25,
};

static const int VK_COLON = VkKeyScan(':');
static const int VK_SEMICOLON = VkKeyScan(';');
static const int VK_PERIOD = VkKeyScan('.');
static const int VK_DOLLAR = VkKeyScan('$');
static const int VK_GREATER = VkKeyScan('>');
static const int VK_LESS = VkKeyScan('<');
static const int VK_SLASH = VkKeyScan('/');

/**
 * \var g_keyboard_hook
 * \brief Pointer to keyboard_hook.
 */
static keyboard_hook g_keyboard_hook;

/**
 * \brief Convert Java keycode to native ones. 
 * \param keycode Java keycode
 * \return native Windows keycode
 */
static int convertJavaKeycodeToWindows(int keycode)
{
    int ret = -1;
    switch(keycode)
    {
       /* 0 - 9 */
        case JVK_0:
            ret = VK_KEY_0;
            break;
        case JVK_1:
            ret = VK_KEY_1;
            break;
        case JVK_2:
            ret = VK_KEY_2;
            break;
        case JVK_3:
            ret = VK_KEY_3;
            break;
        case JVK_4:
            ret = VK_KEY_4;
            break;
        case JVK_5:
            ret = VK_KEY_5;
            break;
        case JVK_6:
            ret = VK_KEY_6;
            break;
        case JVK_7:
            ret = VK_KEY_7;
            break;
        case JVK_8:
            ret = VK_KEY_8;
            break;
        case JVK_9:
            ret = VK_KEY_9;
            break;
        /* A - Z */
        case JVK_A:
            ret = VK_KEY_A;
            break;
        case JVK_B:
            ret = VK_KEY_B;
            break;
        case JVK_C:
            ret = VK_KEY_C;
            break;
        case JVK_D:
            ret = VK_KEY_D;
            break;
        case JVK_E:
            ret = VK_KEY_E;
            break;
        case JVK_F:
            ret = VK_KEY_F;
            break;
        case JVK_G:
            ret = VK_KEY_G;
            break;
        case JVK_H:
            ret = VK_KEY_H;
            break;
        case JVK_I:
            ret = VK_KEY_I;
            break;
        case JVK_J:
            ret = VK_KEY_J;
            break;
        case JVK_K:
            ret = VK_KEY_K;
            break;
        case JVK_L:
            ret = VK_KEY_L;
            break;
        case JVK_M:
            ret = VK_KEY_M;
            break;
        case JVK_N:
            ret = VK_KEY_N;
            break;
        case JVK_O:
            ret = VK_KEY_O;
            break;
        case JVK_P:
            ret = VK_KEY_P;
            break;
        case JVK_Q:
            ret = VK_KEY_Q;
            break;
        case JVK_R:
            ret = VK_KEY_R;
            break;
        case JVK_S:
            ret = VK_KEY_S;
            break;
        case JVK_T:
            ret = VK_KEY_T;
            break;
        case JVK_U:
            ret = VK_KEY_U;
            break;
        case JVK_V:
            ret = VK_KEY_V;
            break;
        case JVK_W:
            ret = VK_KEY_W;
            break;
        case JVK_X:
            ret = VK_KEY_X;
            break;
        case JVK_Y:
            ret = VK_KEY_Y;
            break;
        case JVK_Z:
            ret = VK_KEY_Z;
            break;
        /* F1 - F12 */
        case JVK_F1:
            ret = VK_F1;
            break;
        case JVK_F2:
            ret = VK_F2;
            break;
        case JVK_F3:
            ret = VK_F3;
            break;
        case JVK_F4:
            ret = VK_F4;
            break;
        case JVK_F5:
            ret = VK_F5;
            break;
        case JVK_F6:
            ret = VK_F6;
            break;
        case JVK_F7:
            ret = VK_F7;
            break;
        case JVK_F8:
            ret = VK_F8;
            break;
        case JVK_F9:
            ret = VK_F9;
            break;
        case JVK_F10:
            ret = VK_F10;
            break;
        case JVK_F11:
            ret = VK_F11;
            break;
        case JVK_F12:
            ret = VK_F12;
            break;
        /* arrows (left, right, up, down) */
        case JVK_LEFT:
            ret = VK_LEFT;
            break;
        case JVK_RIGHT:
            ret = VK_RIGHT;
            break;
        case JVK_UP:
            ret = VK_UP;
            break;
        case JVK_DOWN:
            ret = VK_DOWN;
            break;
        case JVK_COMMA:
            ret = VK_OEM_COMMA;
            break;
        case JVK_MINUS:
            ret = VK_OEM_MINUS;
            break;
        case JVK_PLUS:
            ret = VK_ADD;
            break;
        case JVK_PERIOD:
            ret = VK_PERIOD;
            break;
        case JVK_SLASH:
            ret = VK_SLASH;
            break;
        case JVK_BACK_SLASH:
            ret = VK_OEM_5;
            break;
        case JVK_SEMICOLON:
            ret = VK_SEMICOLON;
            break;
        case JVK_EQUALS:
            ret = VK_OEM_PLUS;
            break;
        case JVK_COLON:
            ret = VK_COLON;
            break;
        case JVK_UNDERSCORE:
            ret = -1;
            break;
        case JVK_DOLLAR:
            ret = VK_DOLLAR; 
            break;
        case JVK_EXCLAMATION_MARK:
            ret = VK_OEM_8;
            break;
        case JVK_LESS:
            ret = VK_LESS;
            break;
        case JVK_GREATER:
            ret = VK_GREATER;
            break;
        case JVK_QUOTE:
            ret = -1;
            break;
        case JVK_BACK_QUOTE:
            ret = -1;
            break;
        case JVK_INSERT:
            ret = VK_INSERT;
            break;
        case JVK_HELP:
            ret = VK_ICO_HELP;
            break;
        case JVK_HOME:
            ret = VK_HOME;
            break;
        case JVK_END:
            ret = VK_END;
            break;
        case JVK_PAGE_UP:
            ret = VK_PRIOR;
            break;
        case JVK_PAGE_DOWN:
            ret = VK_NEXT;
            break;
        case JVK_OPEN_BRACKET:
            ret = VK_OEM_4;
            break;
        case JVK_CLOSE_BRACKET:
            ret = VK_OEM_6;
            break;
        case 0x08:
            ret = VK_DELETE;
            break;
        default:
            break;
    }

    return ret;
}

/**
 * \brief Convert Windows keycode to Java ones. 
 * \param keycode Windows keycode
 * \return Java keycode
 */
static int convertWindowsKeycodeToJava(int keycode)
{
    int ret = -1;
    switch(keycode)
    {
      /* 0 - 9 */
        case VK_KEY_0:
            ret = JVK_0;
            break;
        case VK_KEY_1:
            ret = JVK_1;
            break;
        case VK_KEY_2:
            ret = JVK_2;
            break;
        case VK_KEY_3:
            ret = JVK_3;
            break;
        case VK_KEY_4:
            ret = JVK_4;
            break;
        case VK_KEY_5:
            ret = JVK_5;
            break;
        case VK_KEY_6:
            ret = JVK_6;
            break;
        case VK_KEY_7:
            ret = JVK_7;
            break;
        case VK_KEY_8:
            ret = JVK_8;
            break;
        case VK_KEY_9:
            ret = JVK_9;
            break;
        /* A - Z */
        case VK_KEY_A:
            ret = JVK_A;
            break;
        case VK_KEY_B:
            ret = JVK_B;
            break;
        case VK_KEY_C:
            ret = JVK_C;
            break;
        case VK_KEY_D:
            ret = JVK_D;
            break;
        case VK_KEY_E:
            ret = JVK_E;
            break;
        case VK_KEY_F:
            ret = JVK_F;
            break;
        case VK_KEY_G:
            ret = JVK_G;
            break;
        case VK_KEY_H:
            ret = JVK_H;
            break;
        case VK_KEY_I:
            ret = JVK_I;
            break;
        case VK_KEY_J:
            ret = JVK_J;
            break;
        case VK_KEY_K:
            ret = JVK_K;
            break;
        case VK_KEY_L:
            ret = JVK_L;
            break;
        case VK_KEY_M:
            ret = JVK_M;
            break;
        case VK_KEY_N:
            ret = JVK_N;
            break;
        case VK_KEY_O:
            ret = JVK_O;
            break;
        case VK_KEY_P:
            ret = JVK_P;
            break;
        case VK_KEY_Q:
            ret = JVK_Q;
            break;
        case VK_KEY_R:
            ret = JVK_R;
            break;
        case VK_KEY_S:
            ret = JVK_S;
            break;
        case VK_KEY_T:
            ret = JVK_T;
            break;
        case VK_KEY_U:
            ret = JVK_U;
            break;
        case VK_KEY_V:
            ret = JVK_V;
            break;
        case VK_KEY_W:
            ret = JVK_W;
            break;
        case VK_KEY_X:
            ret = JVK_X;
            break;
        case VK_KEY_Y:
            ret = JVK_Y;
            break;
        case VK_KEY_Z:
            ret = JVK_Z;
            break;
        /* F1 - F12 */
        case VK_F1:
            ret = JVK_F1;
            break;
        case VK_F2:
            ret = JVK_F2;
            break;
        case VK_F3:
            ret = JVK_F3;
            break;
        case VK_F4:
            ret = JVK_F4;
            break;
        case VK_F5:
            ret = JVK_F5;
            break;
        case VK_F6:
            ret = JVK_F6;
            break;
        case VK_F7:
            ret = JVK_F7;
            break;
        case VK_F8:
            ret = JVK_F8;
            break;
        case VK_F9:
            ret = JVK_F9;
            break;
        case VK_F10:
            ret = JVK_F10;
            break;
        case VK_F11:
            ret = JVK_F11;
            break;
        case VK_F12:
            ret = JVK_F12;
            break;
        /* arrows (left, right, up, down) */
        case VK_LEFT:
            ret = JVK_LEFT;
            break;
        case VK_RIGHT:
            ret = JVK_RIGHT;
            break;
        case VK_UP:
            ret = JVK_UP;
            break;
        case VK_DOWN:
            ret = JVK_DOWN;
            break;
        case VK_OEM_COMMA:
            ret = JVK_COMMA;
            break;
        case VK_OEM_MINUS:
            ret = JVK_MINUS;
            break;
        case VK_ADD:
            ret = JVK_PLUS;
            break;
        //case VK_OEM_PERIOD:
        //    ret = JVK_PERIOD;
        //    break;
        case VK_DIVIDE:
            ret = JVK_SLASH;
            break;
        case VK_OEM_5:
            ret = JVK_BACK_SLASH;
            break;
        //case VK_OEM_1:
        //   ret = JVK_SEMICOLON;
        //    break;
        case VK_OEM_PLUS:
            ret = JVK_EQUALS;
            break;
        case VK_OEM_8:
            ret = JVK_EXCLAMATION_MARK;
            break;
        //case VK_OEM_PERIOD:
        //    ret = JVK_GREATER;
        //    break;
        //case VK_OEM_COMMA:
        //    ret = JVK_LESS;
        //    break;
        case VK_INSERT:
            ret = JVK_INSERT;
            break;
        case VK_HELP:
            ret = JVK_HELP;
            break;
        case VK_HOME:
            ret = JVK_HOME;
            break;
         case VK_END:
            ret = JVK_END;
            break;
        case VK_PRIOR:
            ret = JVK_PAGE_UP;
            break;
        case VK_NEXT:
            ret = JVK_PAGE_DOWN;
            break;
        case VK_OEM_4:
            ret = JVK_OPEN_BRACKET;
            break;
        case VK_OEM_6:
            ret = JVK_CLOSE_BRACKET;
            break;
        case VK_DELETE:
            ret = 0x08;
            break;
        default:
            break;
    }
    if(ret == -1)
    {
        if(keycode == VK_COLON)
            ret = JVK_COLON;
        else if(keycode == VK_SEMICOLON)
            ret = JVK_SEMICOLON;
        else if(keycode == VK_PERIOD)
            ret = JVK_PERIOD;
        else if(keycode == VK_DOLLAR)
            ret = JVK_DOLLAR;  
        else if(keycode == VK_LESS)
            ret = JVK_LESS;  
        else if(keycode == VK_GREATER)
            ret = JVK_GREATER;  
        else if(keycode == VK_SLASH)
            ret = JVK_SLASH;  
    }
    return ret;
}

/**
 * \brief Convert Windows modifiers to our Java user-defined ones.
 * \param modifiers Windows modifiers (MOD_CONTROL, ...)
 * \return Java user-defined modifiers
 */
static int convertWindowsModifiersToJavaUserDefined(int modifiers)
{ 
  int javaModifiers = 0;

  if(modifiers & MOD_CONTROL)
  {
    /* CTRL */
    javaModifiers |= 0x01;
  }
  if(modifiers & MOD_ALT)
  {
    /* ALT */
    javaModifiers |= 0x02;
  }
  if(modifiers & MOD_SHIFT)
  {
    /* SHIFT */
    javaModifiers |= 0x04;
  }
  if(modifiers & MOD_WIN)
  {
    /* LOGO */
    javaModifiers |= 0x08;
  }

  return javaModifiers;
}

/**
 * \brief Convert Java user-defined modifiers to Windows ones.
 * \param modifiers Java user-defined modifiers
 * \return Windows modifiers
 */
static int convertJavaUserDefinedModifiersToWindows(int modifiers)
{
  int winModifiers = 0;

  if(modifiers & 0x01)
  {
    /* CTRL */
    winModifiers |= MOD_CONTROL;
  }
  if(modifiers & 0x02)
  {
    /* ALT */
    winModifiers |= MOD_ALT;
  }
  if(modifiers & 0x04)
  {
    /* SHIFT */
    winModifiers |= MOD_SHIFT;
  }
  if(modifiers & 0x08)
  {
    /* LOGO */
    winModifiers |= MOD_WIN;
  }

  return winModifiers;
}

/**
 * \brief Sets the current pressed hotkey in g_keyboard_hook.
 * \param modifiers modifiers of the hotkey
 * \param keycode keycode of the hotkey
 * \param java_modifiers original modifiers which were used to call notify function
 * \param java_keycode original keycode which wes used to call notify function
 */
static void setHotkeyRealese(int modifiers, DWORD keycode, int java_modifiers, int java_keycode)
{

  hotkey_release *hk_release = &(g_keyboard_hook.hk_release);
  if(modifiers & MOD_CONTROL)
  {
    /* CTRL */
    hk_release->vkcodes.push_back(VK_RCONTROL);
    hk_release->vkcodes.push_back(VK_CONTROL);
  }
  if(modifiers & MOD_ALT)
  {
    /* ALT */
    hk_release->vkcodes.push_back(VK_MENU);
  }
  if(modifiers & MOD_SHIFT)
  {
    /* SHIFT */
    hk_release->vkcodes.push_back(VK_SHIFT);
    hk_release->vkcodes.push_back(VK_LSHIFT);
    hk_release->vkcodes.push_back(VK_RSHIFT);
  }
  if(modifiers & MOD_WIN)
  {
    /* LOGO */
    hk_release->vkcodes.push_back(VK_LWIN);
    hk_release->vkcodes.push_back(VK_RWIN);
  }
  hk_release->vkcodes.push_back(keycode);
  hk_release->java_modifiers = java_modifiers;
  hk_release->java_keycode = java_keycode;
}

/**
 * \brief Notify Java side about key pressed (keycode + modifiers).
 * \param keycode keycode
 * \param modifiers modifiers used (SHIFT, CTRL, ALT, LOGO)
 * \param on_key_release true - if the hotkey is released 
 */
static void notify(jint keycode, jint modifiers, jboolean on_key_release)
{
  JNIEnv *jniEnv = NULL;
  jclass delegateClass = NULL;

  if(!g_keyboard_hook.delegate)
  {
    return;
  }

  if(0 != g_keyboard_hook.jvm->AttachCurrentThreadAsDaemon((void **)&jniEnv, NULL))
  {
    return;
  }
  delegateClass = jniEnv->GetObjectClass(g_keyboard_hook.delegate);

  if(delegateClass)
  {
    jmethodID methodid = NULL;

    methodid = jniEnv->GetMethodID(delegateClass, "receiveKey", "(IIZ)V");
    if(methodid)
    {
      jniEnv->CallVoidMethod(g_keyboard_hook.delegate, methodid, keycode, modifiers, on_key_release);
    }
  }
  jniEnv->ExceptionClear();
}

/**
 * \brief Called in WndProc to check the event received.
 * \param wParam wparam
 * \param lParam lparam
 * \return 0 if hotkey is processed, -1 otherwise
 */ 
HRESULT callback(UINT msg, WPARAM wParam, LPARAM lParam)
{
  
  if(msg == WM_HOTKEY)
  {
    keyboard_hook* keyboard = &g_keyboard_hook;
    if(keyboard->hwnd)
    {
        for(std::list<keystrok>::iterator it = keyboard->keystrokes.begin() ; it != keyboard->keystrokes.end() ; ++it)
        {
            keystrok& ks = (*it);
    
            if(ks.active == 0)
            {
                /* hotkey to add */
                ks.active = 1;

                if(!RegisterHotKey(keyboard->hwnd, ks.id, ks.modifiers | MOD_NOREPEAT, ks.vkcode))
                {
                  fprintf(stderr, "[LOOP] Problem with RegisterHotKey: %d\n", GetLastError());fflush(stderr);
                  ks.active = -1;
                }
            }
            else if(ks.active == -1)
            {
                /* hotkey to remove */
                if(!UnregisterHotKey(keyboard->hwnd, ks.id))
                {
                //fprintf(stderr, "[LOOP] Error when UnregisterHotKey: %d\n", GetLastError());fflush(stderr);
                }
                it = keyboard->keystrokes.erase(it)--;
            }
        }
    }
    for(std::list<keystrok>::iterator it = g_keyboard_hook.keystrokes.begin() ; it != g_keyboard_hook.keystrokes.end() ; ++it)
    {
      keystrok ks = (*it);
      /* check via hotkey id */
      if(ks.id == wParam)
      {
        int javaModifiers = 0;
        int javaKeycode = 0;
        javaKeycode = convertWindowsKeycodeToJava(HIWORD(lParam)); //ks.vkcode);
        javaModifiers = convertWindowsModifiersToJavaUserDefined(ks.modifiers);
        notify(javaKeycode, javaModifiers, false);
        if(ks.is_on_key_release)
        {
          setHotkeyRealese(ks.modifiers, ks.vkcode, javaModifiers, javaKeycode);
        }
        return 0;
      }
    }
  }
  return  -1;
}

/**
 * \brief Windows UI procedure.
 * \param hWnd handle of the window
 * \param wParam wparam
 * \param lParam lparam
 * \return 0
 */
LRESULT CALLBACK WndProcW(HWND hWnd, UINT msg, WPARAM wParam, LPARAM lParam)
{
  long res = callback(msg, wParam, lParam);

  if(res != -1)
  {
    return res;
  }

  return DefWindowProcW(hWnd, msg, wParam, lParam);
}

/**
 * \brief Register the Win32 Window class.
 * \param hInstance instance of the program
 */
static void RegisterWindowClassW(HINSTANCE hInstance)
{
  WNDCLASSEXW wcex;
  memset(&wcex, 0x00, sizeof(WNDCLASSEXW));
  wcex.cbSize = sizeof(WNDCLASSEXW);
  wcex.style          = CS_HREDRAW | CS_VREDRAW;
  wcex.lpfnWndProc    = WndProcW;
  wcex.cbClsExtra     = 0;
  wcex.cbWndExtra     = 0;
  wcex.hInstance      = hInstance;
  wcex.hIcon          = 0;
  wcex.hCursor        = 0;
  wcex.hbrBackground  = (HBRUSH)(COLOR_WINDOW + 1);
  wcex.lpszMenuName   = 0;
  wcex.lpszClassName  = WINDOW_SHORTCUT_NAME;
  wcex.hIconSm        = 0;

  if(RegisterClassExW(&wcex) == 0)
  {
    fprintf(stderr, "Failed to register window class: %d", GetLastError());
    fflush(stderr);
  }
}

/**
 * \brief Callback handler for low-level keyboard press/release.
 * \param nCode code
 * \param wParam wParam
 * \param lParam lParam
 * \return CallNextHookEx return value
 */
LRESULT CALLBACK keyHandler(int nCode, WPARAM wParam, LPARAM lParam)
{

  KBDLLHOOKSTRUCT* kbd = (KBDLLHOOKSTRUCT*)lParam;
  int pressed = wParam == WM_KEYDOWN;
  if(pressed && kbd->vkCode > 160)
  {
    if(g_keyboard_hook.detect)
    {
      notify(kbd->vkCode, 16367, false);
    }
    else
    {
      for(std::list<keystroke_special>::const_iterator it = g_keyboard_hook.specials.begin() ; it != g_keyboard_hook.specials.end() ; ++it)
      {
        if(it->keycode == kbd->vkCode)
        {
          notify(it->keycode, 16367, false);
          if(it->is_on_key_release)
          {
            setHotkeyRealese(0, it->keycode, 16367, it->keycode);
          }
          break;
        }
      }
    }
  }
  
  if(!pressed)
  {
    hotkey_release* hk = &(g_keyboard_hook.hk_release);
    for(
      std::list<DWORD>::iterator it = hk->vkcodes.begin() ;
      it != hk->vkcodes.end() ; 
      ++it
    )
    {
        if((*it) == kbd->vkCode)
        {
          notify(hk->java_keycode, hk->java_modifiers, true);
          hk->vkcodes.clear();
          break;
        }
    }
  }
  
  return CallNextHookEx(g_keyboard_hook.hook, nCode, wParam, lParam);
}

/**
 * \brief Thread that create window and that monitor event related to it.
 * \param pThreadParam thread parameter
 * \return 0
 */
static unsigned WINAPI CreateWndThreadW(LPVOID pThreadParam)
{
  HINSTANCE hInstance = GetModuleHandle(NULL);
  keyboard_hook* keyboard = (keyboard_hook*)pThreadParam;

  RegisterWindowClassW(hInstance);

  HWND hWnd = CreateWindowW(WINDOW_SHORTCUT_NAME, NULL, WS_OVERLAPPEDWINDOW,
      CW_USEDEFAULT, CW_USEDEFAULT, CW_USEDEFAULT, CW_USEDEFAULT,
      NULL, NULL, hInstance, NULL);
  keyboard->hook = SetWindowsHookEx(WH_KEYBOARD_LL, keyHandler, NULL, 0);

  if(hWnd == NULL)
  {
    fprintf(stderr, "Failed to create window: %d\n", GetLastError());
    fflush(stderr);
    return 0;
  }
  else
  {
    MSG msg;

    keyboard->hwnd = hWnd;
    PostMessage(keyboard->hwnd, WM_HOTKEY, 0, 0);

    while(GetMessageW(&msg, hWnd, 0, 0))
    {
      TranslateMessage(&msg);
      DispatchMessageW(&msg);
    }
    return msg.wParam;
  }
}

JNIEXPORT jlong JNICALL Java_net_java_sip_communicator_impl_globalshortcut_NativeKeyboardHook_init
 (JNIEnv* jniEnv, jclass clazz)
{
  g_keyboard_hook.jvm = 0;
  g_keyboard_hook.delegate = 0;
  g_keyboard_hook.running = 0;
  g_keyboard_hook.hotkey_next_id = 0xC000;
  return (jlong)&g_keyboard_hook;
}

JNIEXPORT void JNICALL Java_net_java_sip_communicator_impl_globalshortcut_NativeKeyboardHook_start
 (JNIEnv* jniEnv, jclass clazz, jlong ptr)
{
  keyboard_hook* keyboard = (keyboard_hook*)ptr;
  HANDLE hThread = NULL;
  UINT uThreadId = 0;

  keyboard->detect = false;
  hThread = (HANDLE)_beginthreadex(NULL, 0, &CreateWndThreadW, keyboard, 0, &uThreadId);
  if(!hThread)
  {
    fprintf(stderr, "Problem creating globalshortcut thread\n");fflush(stderr);
    keyboard->running = 0;
    return;
  }
}

JNIEXPORT void JNICALL Java_net_java_sip_communicator_impl_globalshortcut_NativeKeyboardHook_stop
 (JNIEnv* jniEnv, jclass clazz, jlong ptr)
{
  keyboard_hook* keyboard = (keyboard_hook*)ptr;

  UnhookWindowsHookEx(keyboard->hook);
  keyboard->hook = 0;
  keyboard->running = 0;
}

JNIEXPORT void JNICALL Java_net_java_sip_communicator_impl_globalshortcut_NativeKeyboardHook_setDelegate
 (JNIEnv* jniEnv, jclass clazz, jlong ptr, jobject delegate)
{
  keyboard_hook* keyboard = (keyboard_hook*)ptr;

  if(keyboard->delegate)
  {
    jniEnv->DeleteGlobalRef(keyboard->delegate);
    keyboard->delegate = NULL;
  }

  if(delegate)
  {
    jobject delegate2 = jniEnv->NewGlobalRef(delegate);
    if(delegate2)
    {
      jniEnv->GetJavaVM(&keyboard->jvm);
      keyboard->delegate = delegate2;
    }
  }
}

JNIEXPORT jboolean JNICALL Java_net_java_sip_communicator_impl_globalshortcut_NativeKeyboardHook_registerShortcut
 (JNIEnv* jniEnv, jclass clazz, jlong ptr, jint keycode, jint modifiers, jboolean is_on_key_release)
{
  keyboard_hook* keyboard = (keyboard_hook*)ptr;
  if(keyboard)
  {
    int winModifiers = 0;
    int winKeycode = 0;
    keystrok ks;

    winKeycode = convertJavaKeycodeToWindows(keycode);
    winModifiers = convertJavaUserDefinedModifiersToWindows(modifiers);

    ks.vkcode = winKeycode;
    ks.modifiers = winModifiers;
    ks.id = keyboard->hotkey_next_id;
    ks.active = 0;
    ks.is_on_key_release = is_on_key_release;
    keyboard->keystrokes.push_back(ks);
    keyboard->hotkey_next_id++;
    if(keyboard->hwnd)
    {
        PostMessage(keyboard->hwnd, WM_HOTKEY, 0, 0);
        return true;
    }
    return true;
  }
  return false;
}

JNIEXPORT void JNICALL Java_net_java_sip_communicator_impl_globalshortcut_NativeKeyboardHook_unregisterShortcut
 (JNIEnv* jniEnv, jclass clazz, jlong ptr, jint keycode, jint modifiers)
{
  keyboard_hook* keyboard = (keyboard_hook*)ptr;

  if(keyboard && keyboard->hwnd != NULL)
  {
    int winModifiers = 0;
    int winKeycode = 0;

    winKeycode = convertJavaKeycodeToWindows(keycode);
    winModifiers = convertJavaUserDefinedModifiersToWindows(modifiers);

    for(std::list<keystrok>::iterator it = keyboard->keystrokes.begin() ; it != keyboard->keystrokes.end() ; ++it)
    {
      keystrok& ks = (*it);
      if(ks.vkcode == winKeycode && ks.modifiers == winModifiers)
      {
          ks.active = -1;
          PostMessage(keyboard->hwnd, WM_HOTKEY, 0, 0);
      }
    }
  }
}

JNIEXPORT jboolean JNICALL Java_net_java_sip_communicator_impl_globalshortcut_NativeKeyboardHook_registerSpecial
 (JNIEnv* jniEnv, jclass clazz, jlong ptr, jint keycode, jboolean is_on_key_release)
{
  struct keyboard_hook* keyboard = (struct keyboard_hook*)ptr;

  if(keyboard && keyboard->hook != NULL)
  {
    keystroke_special kss;
    kss.keycode = keycode;
    kss.is_on_key_release = is_on_key_release;
    keyboard->specials.push_back(kss);
    return JNI_TRUE;
  }
  return JNI_FALSE;
}

JNIEXPORT void JNICALL Java_net_java_sip_communicator_impl_globalshortcut_NativeKeyboardHook_unregisterSpecial
  (JNIEnv* jniEnv, jclass clazz, jlong ptr, jint keycode)
{
  struct keyboard_hook* keyboard = (struct keyboard_hook*)ptr;

  if(keyboard)
  {
    for(std::list<keystroke_special>::iterator it = keyboard->specials.begin() ; 
        it != keyboard->specials.end() ; ++it)
    {
      if(it->keycode == keycode)
      {
        keyboard->specials.erase(it);
        return;
      }
    }
  }
}

/*
 * Class:     net_java_sip_communicator_impl_globalshortcut_NativeKeyboardHook
 * Method:    detectSpecialKeyPress
 * Signature: (JZ)V
 */
JNIEXPORT void JNICALL Java_net_java_sip_communicator_impl_globalshortcut_NativeKeyboardHook_detectSpecialKeyPress
 (JNIEnv* jniEnv, jclass clazz, jlong ptr, jboolean enable)
{
  struct keyboard_hook* keyboard = (struct keyboard_hook*)ptr;

  if(keyboard)
  {
    keyboard->detect = (enable == JNI_TRUE) ? true : false;
  }
}
