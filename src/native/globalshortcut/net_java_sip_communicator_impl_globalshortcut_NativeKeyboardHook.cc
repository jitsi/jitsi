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

/* Linux specific code */

#include <cstdio>
#include <cstdlib>
#include <cstring>

#include <list>

#include <unistd.h>
#include <pthread.h>

#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <X11/keysymdef.h>

#include "javakey.h"

/**
 * \class keystrok
 * \brief keystrok class.
 */
class keystrok
{
  public:
    int vkcode; /**< Virtual keycode. */
    int modifiers; /**< Modifiers (ALT, CTLR, ...). */
    int active; /**< If the hotkey is active. */
    bool onrelease; /**< If the java should be notified on release. */
};

/**
 * \class keyboard_hook
 * \brief keyboard_hook structure.
 */
class keyboard_hook
{
  public:
    Display* display; /**< X11 display. */
    Window root; /**< X11 root window. */
    jobject delegate; /**< Java object delegate. */
    JavaVM* jvm; /**< Java VM. */
    volatile int running; /**< Running state. */
    std::list<keystrok> keystrokes; /**< List of keystrokes registered. */
};

/**
 * \brief Convert Java keycode to native ones. 
 * \param keycode Java keycode
 * \return native X11 keycode
 */
static int convertJavaKeycodeToX11(int keycode)
{
    int ret = -1;
    switch(keycode)
    {
       /* 0 - 9 */
        case JVK_0:
            ret = XK_0;
            break;
        case JVK_1:
            ret = XK_1;
            break;
        case JVK_2:
            ret = XK_2;
            break;
        case JVK_3:
            ret = XK_3;
            break;
        case JVK_4:
            ret = XK_4;
            break;
        case JVK_5:
            ret = XK_5;
            break;
        case JVK_6:
            ret = XK_6;
            break;
        case JVK_7:
            ret = XK_7;
            break;
        case JVK_8:
            ret = XK_8;
            break;
        case JVK_9:
            ret = XK_9;
            break;
        /* A - Z */
        case JVK_A:
            ret = XK_A;
            break;
        case JVK_B:
            ret = XK_B;
            break;
        case JVK_C:
            ret = XK_C;
            break;
        case JVK_D:
            ret = XK_D;
            break;
        case JVK_E:
            ret = XK_E;
            break;
        case JVK_F:
            ret = XK_F;
            break;
        case JVK_G:
            ret = XK_G;
            break;
        case JVK_H:
            ret = XK_H;
            break;
        case JVK_I:
            ret = XK_I;
            break;
        case JVK_J:
            ret = XK_J;
            break;
        case JVK_K:
            ret = XK_K;
            break;
        case JVK_L:
            ret = XK_L;
            break;
        case JVK_M:
            ret = XK_M;
            break;
        case JVK_N:
            ret = XK_N;
            break;
        case JVK_O:
            ret = XK_O;
            break;
        case JVK_P:
            ret = XK_P;
            break;
        case JVK_Q:
            ret = XK_Q;
            break;
        case JVK_R:
            ret = XK_R;
            break;
        case JVK_S:
            ret = XK_S;
            break;
        case JVK_T:
            ret = XK_T;
            break;
        case JVK_U:
            ret = XK_U;
            break;
        case JVK_V:
            ret = XK_V;
            break;
        case JVK_W:
            ret = XK_W;
            break;
        case JVK_X:
            ret = XK_X;
            break;
        case JVK_Y:
            ret = XK_Y;
            break;
        case JVK_Z:
            ret = XK_Z;
            break;
        /* F1 - F12 */
        case JVK_F1:
            ret = XK_F1;
            break;
        case JVK_F2:
            ret = XK_F2;
            break;
        case JVK_F3:
            ret = XK_F3;
            break;
        case JVK_F4:
            ret = XK_F4;
            break;
        case JVK_F5:
            ret = XK_F5;
            break;
        case JVK_F6:
            ret = XK_F6;
            break;
        case JVK_F7:
            ret = XK_F7;
            break;
        case JVK_F8:
            ret = XK_F8;
            break;
        case JVK_F9:
            ret = XK_F9;
            break;
        case JVK_F10:
            ret = XK_F10;
            break;
        case JVK_F11:
            ret = XK_F11;
            break;
        case JVK_F12:
            ret = XK_F12;
            break;
        /* arrows (left, right, up, down) */
        case JVK_LEFT:
            ret = XK_Left;
            break;
        case JVK_RIGHT:
            ret = XK_Right;
            break;
        case JVK_UP:
            ret = XK_Up;
            break;
        case JVK_DOWN:
            ret = XK_Down;
            break;
        case JVK_COMMA:
            ret = XK_comma;
            break;
        case JVK_MINUS:
            ret = XK_minus;
            break;
        case JVK_PLUS:
            ret = XK_plus;
            break;
        case JVK_PERIOD:
            ret = XK_period;
            break;
        case JVK_SLASH:
            ret = XK_slash;
            break;
        case JVK_BACK_SLASH:
            ret = XK_backslash;
            break;
        case JVK_SEMICOLON:
            ret = XK_semicolon;
            break;
        case JVK_EQUALS:
            ret = XK_equal;
            break;
        case JVK_COLON:
            ret = XK_colon;
            break;
        case JVK_UNDERSCORE:
            ret = XK_underscore;
            break;
        case JVK_DOLLAR:
            ret = XK_dollar; 
            break;
        case JVK_EXCLAMATION_MARK:
            ret = XK_exclam;
            break;
        case JVK_GREATER:
            ret = XK_greater;
            break;
        case JVK_LESS:
            ret = XK_less;
            break;
        case JVK_QUOTE:
            ret = XK_quoteleft;
            break;
        case JVK_BACK_QUOTE:
            ret = XK_quoteright;
            break;
        case JVK_INSERT:
            ret = XK_Insert;
            break;
        case JVK_HELP:
            ret = XK_Help;
            break;
        case JVK_HOME:
            ret = XK_Home;
            break;
        case JVK_END:
            ret = XK_End;
            break;
        case JVK_PAGE_UP:
            ret = XK_Page_Up;
            break;
        case JVK_PAGE_DOWN:
            ret = XK_Page_Down;
            break;
        case JVK_OPEN_BRACKET:
            ret = XK_bracketleft;
            break;
        case JVK_CLOSE_BRACKET:
            ret = XK_bracketright;
            break;
        case JVK_LEFT_PARENTHESIS:
            ret = XK_parenleft;
            break;
        case JVK_RIGHT_PARENTHESIS:
            ret = XK_parenright;
            break;
        case 0x08:
            ret = XK_Delete;
            break;
        default:
            break;
    }
    return ret;
}

/**
 * \brief Convert X11 keycode to Java ones. 
 * \param keycode X11 keycode
 * \return Java keycode
 */
static int convertX11KeycodeToJava(int keycode)
{
    int ret = -1;
    switch(keycode)
    {
      /* 0 - 9 */
        case XK_0:
            ret = JVK_0;
            break;
        case XK_1:
            ret = JVK_1;
            break;
        case XK_2:
            ret = JVK_2;
            break;
        case XK_3:
            ret = JVK_3;
            break;
        case XK_4:
            ret = JVK_4;
            break;
        case XK_5:
            ret = JVK_5;
            break;
        case XK_6:
            ret = JVK_6;
            break;
        case XK_7:
            ret = JVK_7;
            break;
        case XK_8:
            ret = JVK_8;
            break;
        case XK_9:
            ret = JVK_9;
            break;
        /* A - Z */
        case XK_A:
        case XK_a:
            ret = JVK_A;
            break;
        case XK_B:
        case XK_b:
            ret = JVK_B;
            break;
        case XK_C:
        case XK_c:
            ret = JVK_C;
            break;
        case XK_D:
        case XK_d:
            ret = JVK_D;
            break;
        case XK_E:
        case XK_e:
            ret = JVK_E;
            break;
        case XK_F:
        case XK_f:
            ret = JVK_F;
            break;
        case XK_G:
        case XK_g:
            ret = JVK_G;
            break;
        case XK_H:
        case XK_h:
            ret = JVK_H;
            break;
        case XK_I:
        case XK_i:
            ret = JVK_I;
            break;
        case XK_J:
        case XK_j:
            ret = JVK_J;
            break;
        case XK_K:
        case XK_k:
            ret = JVK_K;
            break;
        case XK_L:
        case XK_l:
            ret = JVK_L;
            break;
        case XK_M:
        case XK_m:
            ret = JVK_M;
            break;
        case XK_N:
        case XK_n:
            ret = JVK_N;
            break;
        case XK_O:
        case XK_o:
            ret = JVK_O;
            break;
        case XK_P:
        case XK_p:
            ret = JVK_P;
            break;
        case XK_Q:
        case XK_q:
            ret = JVK_Q;
            break;
        case XK_R:
        case XK_r:
            ret = JVK_R;
            break;
        case XK_S:
        case XK_s:
            ret = JVK_S;
            break;
        case XK_T:
        case XK_t:
            ret = JVK_T;
            break;
        case XK_U:
        case XK_u:
            ret = JVK_U;
            break;
        case XK_V:
        case XK_v:
            ret = JVK_V;
            break;
        case XK_W:
        case XK_w:
            ret = JVK_W;
            break;
        case XK_X:
        case XK_x:
            ret = JVK_X;
            break;
        case XK_Y:
        case XK_y:
            ret = JVK_Y;
            break;
        case XK_Z:
        case XK_z:
            ret = JVK_Z;
            break;
        /* F1 - F12 */
        case XK_F1:
            ret = JVK_F1;
            break;
        case XK_F2:
            ret = JVK_F2;
            break;
        case XK_F3:
            ret = JVK_F3;
            break;
        case XK_F4:
            ret = JVK_F4;
            break;
        case XK_F5:
            ret = JVK_F5;
            break;
        case XK_F6:
            ret = JVK_F6;
            break;
        case XK_F7:
            ret = JVK_F7;
            break;
        case XK_F8:
            ret = JVK_F8;
            break;
        case XK_F9:
            ret = JVK_F9;
            break;
        case XK_F10:
            ret = JVK_F10;
            break;
        case XK_F11:
            ret = JVK_F11;
            break;
        case XK_F12:
            ret = JVK_F12;
            break;
        /* arrows (left, right, up, down) */
        case XK_Left:
            ret = JVK_LEFT;
            break;
        case XK_Right:
            ret = JVK_RIGHT;
            break;
        case XK_Up:
            ret = JVK_UP;
            break;
        case XK_Down:
            ret = JVK_DOWN;
            break;
        case XK_comma:
          ret = JVK_COMMA;
            break;
        case XK_minus:
          ret = JVK_MINUS;
            break;
        case XK_plus:
          ret = JVK_PLUS;
            break;
        case XK_period:
          ret = JVK_PERIOD;
            break;
        case XK_slash:
          ret = JVK_SLASH;
            break;
        case XK_backslash:
          ret = JVK_BACK_SLASH;
            break;
        case XK_semicolon:
          ret = JVK_SEMICOLON;
            break;
        case XK_equal:
          ret = JVK_EQUALS;
            break;
        case XK_colon:
          ret = JVK_COLON;
            break;
        case XK_underscore:
          ret = JVK_UNDERSCORE;
            break;
        case XK_dollar:
          ret = JVK_DOLLAR;
            break;
        case XK_exclam:
          ret = JVK_EXCLAMATION_MARK;
            break;
        case XK_greater:
          ret = JVK_GREATER;
            break;
        case XK_less:
          ret = JVK_LESS;
            break;
        case XK_quoteleft:
          ret = JVK_QUOTE;
            break;
        case XK_quoteright:
          ret = JVK_BACK_QUOTE;
            break;
        case XK_Insert:
          ret = JVK_INSERT;
            break;
        case XK_Help:
          ret = JVK_HELP;
            break;
        case XK_Home:
          ret = JVK_HOME;
            break;
        case XK_End:
          ret = JVK_END;
            break;
        case XK_Page_Up:
          ret = JVK_PAGE_UP;
            break;
        case XK_Page_Down:
          ret = JVK_PAGE_DOWN;
            break;
        case JVK_OPEN_BRACKET:
            ret = XK_bracketleft;
            break;
        case XK_bracketright:
          ret = JVK_CLOSE_BRACKET;
            break;
        case XK_parenleft:
            ret = JVK_LEFT_PARENTHESIS;
            break;
        case XK_parenright:
            ret = JVK_RIGHT_PARENTHESIS;
            break;
        case XK_Delete:
          ret = 0x08;
            break;
        default:
            break;
    }
    return ret;
}


/**
 * \brief Convert X11 modifiers to Java user-defined ones.
 * \param modfiers X11 modifiers
 * \return Java user-defined modifiers
 */
static int X11ModifiersToJavaUserDefined(int modifiers)
{
  int javaModifiers = 0;

  if(modifiers & ControlMask)
  {
    javaModifiers |= 0x01;
  }
  if(modifiers & Mod1Mask)
  {
    /* Alt */
    javaModifiers |= 0x02;
  }
  if(modifiers & ShiftMask)
  {
    javaModifiers |= 0x04;
  }
  if(modifiers & Mod4Mask)
  {
    /* Super */
    javaModifiers |= 0x08;
  }

  return javaModifiers;
}

/**
 * \brief Convert Java user-defined modifiers to X11 ones.
 * \param modifiers Java user-defined modifiers
 * \return X11 modifiers
 */
static int JavaUserDefinedModifiersToX11(int modifiers)
{
  int x11Modifiers = 0;

  if(modifiers & 0x01)
  {
    x11Modifiers |= ControlMask;
  }
  if(modifiers & 0x02)
  {
    /* Alt */
    x11Modifiers |= Mod1Mask;
  }
  if(modifiers & 0x04)
  {
    x11Modifiers |= ShiftMask;
  }
  if(modifiers & 0x08)
  {
    /* Super */
    x11Modifiers |= Mod4Mask;
  }

  return x11Modifiers;
}

/**
 * \brief Notify Java side about key pressed (keycode + modifiers).
 * \param keycode keycode
 * \param modifiers modifiers used (SHIFT, CTRL, ALT, LOGO)
 */
static void notify(struct keyboard_hook* keyboard, jint keycode, jint modifiers, jboolean on_key_release)
{
  JNIEnv *jniEnv = NULL;
  jclass delegateClass = NULL;

  if(!keyboard->delegate)
  {
    return;
  }

  if(0 != keyboard->jvm->AttachCurrentThreadAsDaemon((void **)&jniEnv, NULL))
  {
    return;
  }

  delegateClass = jniEnv->GetObjectClass(keyboard->delegate);

  if(delegateClass)
  {
    jmethodID methodid = NULL;

    methodid = jniEnv->GetMethodID(delegateClass, "receiveKey", "(IIZ)V");

    if(methodid)
    {
      jniEnv->CallVoidMethod(keyboard->delegate, methodid, keycode, modifiers, on_key_release);
    }
  }
  jniEnv->ExceptionClear();
}

/**
 * \brief X11 event loop thread entry point.
 * \param arg thread argument
 * \return NULL
 */
static void* x11_event_loop_thread(void* arg)
{
  struct keyboard_hook* keyboard = (struct keyboard_hook*)arg;
  keystrok activeHotKey;
  bool hotKeyActivated = false;
  XSelectInput(keyboard->display, keyboard->root, KeyPressMask | KeyReleaseMask);

  while(keyboard->running)
  {
    XEvent ev, next_ev;
    while(XCheckMaskEvent(keyboard->display, 0xFFFFFFFF, &ev))
    {
      switch (ev.type)
      {
        case KeyPress:
        case KeyRelease:
          for(std::list<keystrok>::iterator it = keyboard->keystrokes.begin() ; it != keyboard->keystrokes.end() ; ++it)
          {
            keystrok& ks = (*it);
            XKeyEvent* keyEvent = (XKeyEvent*)&ev.xkey;
            unsigned long keycode = -1;
            //XKeycodeToKeysym(keyboard->display, keyEvent->keycode, 1);
            
            XLookupString(keyEvent, NULL, 0, &keycode, NULL);
            
            keycode = convertX11KeycodeToJava(keycode);
            int modifiers = X11ModifiersToJavaUserDefined(keyEvent->state);
           
            if(ks.vkcode == keycode && ks.modifiers == modifiers)
            {
			  if(ev.type == KeyRelease)
			  {
				if(hotKeyActivated && activeHotKey.vkcode == keycode && activeHotKey.modifiers == modifiers)
				{
					if(XEventsQueued(keyboard->display, QueuedAfterReading))
					{
						XPeekEvent(keyboard->display, &next_ev);
						if(next_ev.type == KeyPress
						     && next_ev.xkey.time == keyEvent->time
							 && next_ev.xkey.keycode == keyEvent->keycode
							 && next_ev.xkey.state == keyEvent->state)
						{
							//disable the autorepeat event from the queue and continue with next messages
							XCheckMaskEvent(keyboard->display, 0xFFFFFFFF, &ev);
							continue;
						}
					}
					hotKeyActivated = false;
				}
			    if(!ks.onrelease)
			    {
				  continue;
			    }
			  }
			  else
			  {
				hotKeyActivated = true;
			    activeHotKey = *it;
			  }
              notify(keyboard, ks.vkcode, ks.modifiers, (ev.type == KeyRelease));
            }
          }

          break;
        default:
          break;
      }
    }

    for(std::list<keystrok>::iterator it = keyboard->keystrokes.begin() ; it != keyboard->keystrokes.end() ; ++it)
    {
      keystrok& ks = (*it);

      if(ks.active == 0)
      {
        /* hotkey to add */
        int x11Keycode = convertJavaKeycodeToX11(ks.vkcode);
        if(x11Keycode != -1)
        {
          x11Keycode = XKeysymToKeycode(keyboard->display, x11Keycode);
        }
        else
        {
          printf("failed\n");fflush(stdout);
          ks.active = -1;
          continue;
        }
        int x11Modifiers = JavaUserDefinedModifiersToX11(ks.modifiers);
        ks.active = 1;
        if(XGrabKey(keyboard->display, x11Keycode, x11Modifiers, keyboard->root, False, GrabModeAsync, GrabModeAsync) > 1)
        {
          fprintf(stderr, "[LOOP] Error when XGrabKey\n");fflush(stderr);
          ks.active = -1;
        }
      }
      else if(ks.active == -1)
      {
        /* hotkey to remove */
        int x11Keycode = XKeysymToKeycode(keyboard->display, 
            convertJavaKeycodeToX11(ks.vkcode));
        int x11Modifiers = JavaUserDefinedModifiersToX11(ks.modifiers);
        
        if(XUngrabKey(keyboard->display, x11Keycode, x11Modifiers, keyboard->root) > 1)
        {
          fprintf(stderr, "[LOOP] Error when XUngrabKey\n");fflush(stderr);
        }
        it = keyboard->keystrokes.erase(it)--;
      }
    }

    usleep(1000 * 1000);
    pthread_yield();
  }

  return NULL;
}

JNIEXPORT jlong JNICALL Java_net_java_sip_communicator_impl_globalshortcut_NativeKeyboardHook_init
 (JNIEnv* jniEnv, jclass clazz)
{
  struct keyboard_hook* keyboard = NULL;
  (void)jniEnv;
  (void)clazz;

  keyboard = new keyboard_hook();
  if(!keyboard)
  {
    return (jlong)NULL;
  }

  keyboard->display = XOpenDisplay(NULL);

  if(!keyboard->display)
  {
    free(keyboard);
    return (jlong)NULL;
  }

  keyboard->root = DefaultRootWindow(keyboard->display);

  return (jlong)keyboard;
}

/* XXX release JNI method */

JNIEXPORT void JNICALL Java_net_java_sip_communicator_impl_globalshortcut_NativeKeyboardHook_start
 (JNIEnv* jniEnv, jclass clazz, jlong ptr)
{
  struct keyboard_hook* keyboard = (struct keyboard_hook*)ptr;
  pthread_t id;
  pthread_attr_t attr;

  (void)jniEnv;
  (void)clazz;

  if(keyboard->running)
  {
    return;
  }

  pthread_attr_init(&attr);

  if(pthread_attr_setdetachstate(&attr, 1) != 0)
  {
    perror("pthread_attr_setdetachstate");fflush(stderr);
    return;
  }

  keyboard->running = 1;

  if(pthread_create(&id, &attr, x11_event_loop_thread, keyboard) != 0)
  {
    perror("pthread_create");fflush(stderr);
    return;
  }
}

JNIEXPORT void JNICALL Java_net_java_sip_communicator_impl_globalshortcut_NativeKeyboardHook_stop
 (JNIEnv* jniEnv, jclass clazz, jlong ptr)
{
  struct keyboard_hook* keyboard = (struct keyboard_hook*)ptr;

  (void)jniEnv;
  (void)clazz;

  keyboard->running = 0;
}

JNIEXPORT void JNICALL Java_net_java_sip_communicator_impl_globalshortcut_NativeKeyboardHook_setDelegate
 (JNIEnv* jniEnv, jclass clazz, jlong ptr, jobject delegate)
{
  struct keyboard_hook* keyboard = (struct keyboard_hook*)ptr;

  (void)clazz;

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
  struct keyboard_hook* keyboard = (struct keyboard_hook*)ptr;
  (void)jniEnv;
  (void)clazz;

  if(keyboard)
  {
    keystrok ks;
    ks.vkcode = keycode;
    ks.modifiers = modifiers;
    ks.active = 0;
    ks.onrelease = is_on_key_release;
    keyboard->keystrokes.push_back(ks);

    return JNI_TRUE;
  }

  return JNI_FALSE;
}

JNIEXPORT void JNICALL Java_net_java_sip_communicator_impl_globalshortcut_NativeKeyboardHook_unregisterShortcut
 (JNIEnv* jniEnv, jclass clazz, jlong ptr, jint keycode, jint modifiers)
{
  struct keyboard_hook* keyboard = (struct keyboard_hook*)ptr;

  (void)jniEnv;
  (void)clazz;

  if(keyboard)
  {
    for(std::list<keystrok>::iterator it = keyboard->keystrokes.begin() ; it != keyboard->keystrokes.end() ; ++it)
    {
      keystrok& ks = (*it);
      if(ks.vkcode == keycode && ks.modifiers == modifiers)
      {
        ks.active = -1;
      }
    }
  }
}

JNIEXPORT jboolean JNICALL Java_net_java_sip_communicator_impl_globalshortcut_NativeKeyboardHook_registerSpecial
 (JNIEnv* jniEnv, jclass clazz, jlong ptr, jint keycode, jboolean is_on_key_release)
{
  (void)jniEnv;
  (void)clazz;
  (void)ptr;
  (void)keycode;
  return JNI_FALSE;
}

JNIEXPORT void JNICALL Java_net_java_sip_communicator_impl_globalshortcut_NativeKeyboardHook_unregisterSpecial
 (JNIEnv* jniEnv, jclass clazz, jlong ptr, jint keycode)
{
  (void)jniEnv;
  (void)clazz;
  (void)ptr;
  (void)keycode;
}

JNIEXPORT void JNICALL Java_net_java_sip_communicator_impl_globalshortcut_NativeKeyboardHook_detectSpecialKeyPress
 (JNIEnv* jniEnv, jclass clazz, jlong ptr, jboolean enable)
{
  (void)jniEnv;
  (void)clazz;
  (void)ptr;
  (void)enable;
}

