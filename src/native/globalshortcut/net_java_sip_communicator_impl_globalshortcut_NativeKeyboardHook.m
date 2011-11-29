/*
 * Jitsi Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
/bin/bash: 2: command not found
 * See terms of license at gnu.org.
 */

#include "net_java_sip_communicator_impl_globalshortcut_NativeKeyboardHook.h"

/* Mac OS X specific code */

#import <Foundation/NSAutoreleasePool.h>
#import <Foundation/Foundation.h>
#import <Foundation/NSObject.h>

#import "DDHotKeyCenter.h"
#import "javakey.h"

/* following enum comes from http://snipplr.com/view/42797/ */
enum
{
    kVK_ANSI_A = 0x00,
    kVK_ANSI_S = 0x01,
    kVK_ANSI_D = 0x02,
    kVK_ANSI_F = 0x03,
    kVK_ANSI_H = 0x04,
    kVK_ANSI_G = 0x05,
    kVK_ANSI_Z = 0x06,
    kVK_ANSI_X = 0x07,
    kVK_ANSI_C = 0x08,
    kVK_ANSI_V = 0x09,
    kVK_ANSI_B = 0x0B,
    kVK_ANSI_Q = 0x0C,
    kVK_ANSI_W = 0x0D,
    kVK_ANSI_E = 0x0E,
    kVK_ANSI_R = 0x0F,
    kVK_ANSI_Y = 0x10,
    kVK_ANSI_T = 0x11,
    kVK_ANSI_1 = 0x12,
    kVK_ANSI_2 = 0x13,
    kVK_ANSI_3 = 0x14,
    kVK_ANSI_4 = 0x15,
    kVK_ANSI_6 = 0x16,
    kVK_ANSI_5 = 0x17,
    kVK_ANSI_Equal = 0x18,
    kVK_ANSI_9 = 0x19,
    kVK_ANSI_7 = 0x1A,
    kVK_ANSI_Minus = 0x1B,
    kVK_ANSI_8 = 0x1C,
    kVK_ANSI_0 = 0x1D,
    kVK_ANSI_RightBracket = 0x1E,
    kVK_ANSI_O = 0x1F,
    kVK_ANSI_U = 0x20,
    kVK_ANSI_LeftBracket = 0x21,
    kVK_ANSI_I = 0x22,
    kVK_ANSI_P = 0x23,
    kVK_ANSI_L = 0x25,
    kVK_ANSI_J = 0x26,
    kVK_ANSI_Quote = 0x27,
    kVK_ANSI_K = 0x28,
    kVK_ANSI_Semicolon = 0x29,
    kVK_ANSI_Backslash = 0x2A,
    kVK_ANSI_Comma = 0x2B,
    kVK_ANSI_Slash = 0x2C,
    kVK_ANSI_N = 0x2D,
    kVK_ANSI_M = 0x2E,
    kVK_ANSI_Period = 0x2F,
    kVK_ANSI_Grave =JVK_2,
    kVK_ANSI_KeypadDecimal = 0x41,
    kVK_ANSI_KeypadMultiply = 0x43,
    kVK_ANSI_KeypadPlus = 0x45,
    kVK_ANSI_KeypadClear = 0x47,
    kVK_ANSI_KeypadDivide = 0x4B,
    kVK_ANSI_KeypadEnter = 0x4C,
    kVK_ANSI_KeypadMinus = 0x4E,
    kVK_ANSI_KeypadEquals = 0x51,
    kVK_ANSI_Keypad0 = 0x52,
    kVK_ANSI_Keypad1 = 0x53,
    kVK_ANSI_Keypad2 = 0x54,
    kVK_ANSI_Keypad3 = 0x55,
    kVK_ANSI_Keypad4 = 0x56,
    kVK_ANSI_Keypad5 = 0x57,
    kVK_ANSI_Keypad6 = 0x58,
    kVK_ANSI_Keypad7 = 0x59,
    kVK_ANSI_Keypad8 = 0x5B,
    kVK_ANSI_Keypad9 = 0x5C
};

/* keycodes for keys that are independent of keyboard layout*/
enum
{
    kVK_Return = 0x24,
    kVK_Tab =JVK_0,
    kVK_Space =JVK_1,
    kVK_Delete =JVK_3,
    kVK_Escape =JVK_5,
    kVK_Command =JVK_7,
    kVK_Shift =JVK_8,
    kVK_CapsLock =JVK_9,
    kVK_Option =JVK_A,
    kVK_Control =JVK_B,
    kVK_RightShift =JVK_C,
    kVK_RightOption =JVK_D,
    kVK_RightControl =JVK_E,
    kVK_Function =JVK_F,
    kVK_F17 = 0x40,
    kVK_VolumeUp = 0x48,
    kVK_VolumeDown = 0x49,
    kVK_Mute = 0x4A,
    kVK_F18 = 0x4F,
    kVK_F19 = 0x50,
    kVK_F20 = 0x5A,
    kVK_F5 = 0x60,
    kVK_F6 = 0x61,
    kVK_F7 = 0x62,
    kVK_F3 = 0x63,
    kVK_F8 = 0x64,
    kVK_F9 = 0x65,
    kVK_F11 = 0x67,
    kVK_F13 = 0x69,
    kVK_F16 = 0x6A,
    kVK_F14 = 0x6B,
    kVK_F10 = 0x6D,
    kVK_F12 = 0x6F,
    kVK_F15 = 0x71,
    kVK_Help = 0x72,
    kVK_Home = 0x73,
    kVK_PageUp = 0x74,
    kVK_ForwardDelete = 0x75,
    kVK_F4 = 0x76,
    kVK_End = 0x77,
    kVK_F2 = 0x78,
    kVK_PageDown = 0x79,
    kVK_F1 = 0x7A,
    kVK_LeftArrow = 0x7B,
    kVK_RightArrow = 0x7C,
    kVK_DownArrow = 0x7D,
    kVK_UpArrow = 0x7E
};

/* ISO keyboards only*/
enum
{
    kVK_ISO_Section = 0x0A
};

/* JIS keyboards only*/
enum
{
    kVK_JIS_Yen = 0x5D,
    kVK_JIS_Underscore = 0x5E,
    kVK_JIS_KeypadComma = 0x5F,
    kVK_JIS_Eisu = 0x66,
    kVK_JIS_Kana = 0x68
};

/**
 * \brief Convert Java keycode to native ones. 
 * Reference: http://boredzo.org/blog/wp-content/uploads/2007/05/imtx-virtual-keycodes.png
 * \param keycode Java keycode
 * \return native Mac OS X keycode
 */
static int convertJavaKeycodeToMac(int keycode)
{
    int ret = -1;
    switch(keycode)
    {
        /* 0 - 9 */
        case JVK_0:
            ret = kVK_ANSI_0;
            break;
        case JVK_1:
            ret = kVK_ANSI_1;
            break;
        case JVK_2:
            ret = kVK_ANSI_2;
            break;
        case JVK_3:
            ret = kVK_ANSI_3;
            break;
        case JVK_4:
            ret = kVK_ANSI_4;
            break;
        case JVK_5:
            ret = kVK_ANSI_5;
            break;
        case JVK_6:
            ret = kVK_ANSI_6;
            break;
        case JVK_7:
            ret = kVK_ANSI_7;
            break;
        case JVK_8:
            ret = kVK_ANSI_8;
            break;
        case JVK_9:
            ret = kVK_ANSI_9;
            break;
        /* A - Z */
        case JVK_A:
            ret = kVK_ANSI_A;
            break;
        case JVK_B:
            ret = kVK_ANSI_B;
            break;
        case JVK_C:
            ret = kVK_ANSI_C;
            break;
        case JVK_D:
            ret = kVK_ANSI_D;
            break;
        case JVK_E:
            ret = kVK_ANSI_E;
            break;
        case JVK_F:
            ret = kVK_ANSI_F;
            break;
        case JVK_G:
            ret = kVK_ANSI_G;
            break;
        case JVK_H:
            ret = kVK_ANSI_H;
            break;
        case JVK_I:
            ret = kVK_ANSI_I;
            break;
        case JVK_J:
            ret = kVK_ANSI_J;
            break;
        case JVK_K:
            ret = kVK_ANSI_K;
            break;
        case JVK_L:
            ret = kVK_ANSI_L;
            break;
        case JVK_M:
            ret = kVK_ANSI_M;
            break;
        case JVK_N:
            ret = kVK_ANSI_N;
            break;
        case JVK_O:
            ret = kVK_ANSI_O;
            break;
        case JVK_P:
            ret = kVK_ANSI_P;
            break;
        case JVK_Q:
            ret = kVK_ANSI_Q;
            break;
        case JVK_R:
            ret = kVK_ANSI_R;
            break;
        case JVK_S:
            ret = kVK_ANSI_S;
            break;
        case JVK_T:
            ret = kVK_ANSI_T;
            break;
        case JVK_U:
            ret = kVK_ANSI_U;
            break;
        case JVK_V:
            ret = kVK_ANSI_V;
            break;
        case JVK_W:
            ret = kVK_ANSI_W;
            break;
        case JVK_X:
            ret = kVK_ANSI_X;
            break;
        case JVK_Y:
            ret = kVK_ANSI_Y;
            break;
        case JVK_Z:
            ret = kVK_ANSI_Z;
            break;
        /* F1 - F12 */
        case JVK_F1:
            ret = kVK_F1;
            break;
        case JVK_F2:
            ret = kVK_F2;
            break;
        case JVK_F3:
            ret = kVK_F3;
            break;
        case JVK_F4:
            ret = kVK_F4;
            break;
        case JVK_F5:
            ret = kVK_F5;
            break;
        case JVK_F6:
            ret = kVK_F6;
            break;
        case JVK_F7:
            ret = kVK_F7;
            break;
        case JVK_F8:
            ret = kVK_F8;
            break;
        case JVK_F9:
            ret = kVK_F9;
            break;
        case JVK_F10:
            ret = kVK_F10;
            break;
        case JVK_F11:
            ret = kVK_F11;
            break;
        case JVK_F12:
            ret = kVK_F12;
            break;
        /* arrows (left, right, up, down) */
        case JVK_LEFT:
            ret = kVK_LeftArrow;
            break;
        case JVK_RIGHT:
            ret = kVK_RightArrow;
            break;
        case JVK_UP:
            ret = kVK_UpArrow;
            break;
        case JVK_DOWN:
            ret = kVK_DownArrow;
            break;
        case JVK_COMMA:
            ret = kVK_ANSI_Comma;
            break;
        case JVK_MINUS:
            ret = kVK_ANSI_Minus;
            break;
        case JVK_PLUS:
            ret = kVK_ANSI_KeypadPlus;
            break;
        case JVK_PERIOD:
            ret = kVK_ANSI_Period;
            break;
        case JVK_SLASH:
            ret = kVK_ANSI_Slash;
            break;
        case JVK_BACK_SLASH:
            ret = kVK_ANSI_Backslash;
            break;
        case JVK_SEMICOLON:
            ret = kVK_ANSI_Semicolon;
            break;
        case JVK_EQUALS:
            ret = kVK_ANSI_Equal;
            break;
        case JVK_COLON:
            ret = -1;
            break;
        case JVK_UNDERSCORE:
            ret = kVK_JIS_Underscore;
            break;
        case JVK_DOLLAR:
            ret = -1; 
            break;
        case JVK_EXCLAMATION_MARK:
            ret = -1;
            break;
        case JVK_GREATER:
            ret = -1;
            break;
        case JVK_LESS:
            ret = -1;
            break;
        case JVK_QUOTE:
            ret = kVK_ANSI_Quote;
            break;
        case JVK_BACK_QUOTE:
            ret = -1;
            break;
        case JVK_INSERT:
            ret = -1;
            break;
        case JVK_HELP:
            ret = kVK_Help;
            break;
        case JVK_HOME:
            ret = kVK_Home;
            break;
        case JVK_END:
            ret = kVK_End;
            break;
        case JVK_PAGE_UP:
            ret = kVK_PageUp;
            break;
        case JVK_PAGE_DOWN:
            ret = kVK_PageDown;
            break;
        case JVK_OPEN_BRACKET:
            ret = kVK_ANSI_LeftBracket;
            break;
        case JVK_CLOSE_BRACKET:
            ret = kVK_ANSI_RightBracket;
            break;
        case 0x08:
            ret = kVK_Delete;
            break;
        default:
            break;
    }

    return ret;
}

/**
 * \brief Convert Mac OS X keycode to Java ones. 
 * Reference: http://boredzo.org/blog/wp-content/uploads/2007/05/imtx-virtual-keycodes.png
 * \param keycode Mac OS X keycode
 * \return Java keycode
 */
static int convertMacKeycodeToJava(int keycode)
{
    int ret = -1;
    switch(keycode)
    {
        /* 0 - 9 */
        case kVK_ANSI_0:
            ret =JVK_0;
            break;
        case kVK_ANSI_1:
            ret =JVK_1;
            break;
        case kVK_ANSI_2:
            ret =JVK_2;
            break;
        case kVK_ANSI_3:
            ret =JVK_3;
            break;
        case kVK_ANSI_4:
            ret =JVK_4;
            break;
        case kVK_ANSI_5:
            ret =JVK_5;
            break;
        case kVK_ANSI_6:
            ret =JVK_6;
            break;
        case kVK_ANSI_7:
            ret =JVK_7;
            break;
        case kVK_ANSI_8:
            ret =JVK_8;
            break;
        case kVK_ANSI_9:
            ret =JVK_9;
            break;
        /* A - Z */
        case kVK_ANSI_A:
            ret =JVK_A;
            break;
        case kVK_ANSI_B:
            ret =JVK_B;
            break;
        case kVK_ANSI_C:
            ret =JVK_C;
            break;
        case kVK_ANSI_D:
            ret =JVK_D;
            break;
        case kVK_ANSI_E:
            ret =JVK_E;
            break;
        case kVK_ANSI_F:
            ret =JVK_F;
            break;
        case kVK_ANSI_G:
            ret =JVK_G;
            break;
        case kVK_ANSI_H:
            ret =JVK_H;
            break;
        case kVK_ANSI_I:
            ret =JVK_I;
            break;
        case kVK_ANSI_J:
            ret =JVK_J;
            break;
        case kVK_ANSI_K:
            ret =JVK_K;
            break;
        case kVK_ANSI_L:
            ret =JVK_L;
            break;
        case kVK_ANSI_M:
            ret =JVK_M;
            break;
        case kVK_ANSI_N:
            ret =JVK_N;
            break;
        case kVK_ANSI_O:
            ret =JVK_O;
            break;
        case kVK_ANSI_P:
            ret =JVK_P;
            break;
        case kVK_ANSI_Q:
            ret =JVK_Q;
            break;
        case kVK_ANSI_R:
            ret =JVK_R;
            break;
        case kVK_ANSI_S:
            ret =JVK_S;
            break;
        case kVK_ANSI_T:
            ret =JVK_T;
            break;
        case kVK_ANSI_U:
            ret =JVK_U;
            break;
        case kVK_ANSI_V:
            ret =JVK_V;
            break;
        case kVK_ANSI_W:
            ret =JVK_W;
            break;
        case kVK_ANSI_X:
            ret =JVK_X;
            break;
        case kVK_ANSI_Y:
            ret =JVK_Y;
            break;
        case kVK_ANSI_Z:
            ret =JVK_Z;
            break;
        /* F1 - F12 */
        case kVK_F1:
            ret =JVK_F1;
            break;
        case kVK_F2:
            ret =JVK_F2;
            break;
        case kVK_F3:
            ret =JVK_F3;
            break;
        case kVK_F4:
            ret =JVK_F4;
            break;
        case kVK_F5:
            ret =JVK_F5;
            break;
        case kVK_F6:
            ret =JVK_F6;
            break;
        case kVK_F7:
            ret =JVK_F7;
            break;
        case kVK_F8:
            ret =JVK_F8;
            break;
        case kVK_F9:
            ret =JVK_F9;
            break;
        case kVK_F10:
            ret =JVK_F10;
            break;
        case kVK_F11:
            ret =JVK_F11;
            break;
        case kVK_F12:
            ret =JVK_F12;
            break;
        /* arrows (left, right, up, down) */
        case kVK_LeftArrow:
            ret =JVK_LEFT;
            break;
        case kVK_RightArrow:
            ret =JVK_RIGHT;
            break;
        case kVK_UpArrow:
            ret =JVK_UP;
            break;
        case kVK_DownArrow:
            ret =JVK_DOWN;
            break;
        case kVK_ANSI_Comma:
            ret =JVK_COMMA;
            break;
        case kVK_ANSI_Minus:
            ret =JVK_MINUS;
            break;
        case kVK_ANSI_KeypadPlus:
            ret =JVK_PLUS;
            break;
        case kVK_ANSI_Period:
            ret =JVK_PERIOD;
            break;
        case kVK_ANSI_Slash:
            ret =JVK_SLASH;
            break;
        case kVK_ANSI_Backslash:
            ret =JVK_BACK_SLASH;
            break;
        case kVK_ANSI_Semicolon:
            ret =JVK_SEMICOLON;
            break;
        case kVK_ANSI_Equal:
            ret =JVK_EQUALS;
            break;
        case kVK_JIS_Underscore:
            ret =JVK_UNDERSCORE;
            break;
        case kVK_ANSI_Quote:
            ret =JVK_QUOTE;
            break;
        case kVK_Help:
            ret =JVK_HELP;
            break;
        case kVK_Home:
            ret =JVK_HOME;
            break;
         case kVK_End:
            ret =JVK_END;
            break;
        case kVK_PageUp:
            ret =JVK_PAGE_UP;
            break;
        case kVK_PageDown:
            ret =JVK_PAGE_DOWN;
            break;
        case kVK_ANSI_LeftBracket:
            ret =JVK_OPEN_BRACKET;
            break;
        case kVK_ANSI_RightBracket:
            ret =JVK_CLOSE_BRACKET;
            break;
        case kVK_Delete:
            ret = 0x08;
            break;
        default:
            break;
    }
    return ret;
}

/**
 * \brief Convert Java user-defined modifiers to Mac OS X ones.
 * \param modifiers Java user-defined modifiers
 * \return Mac OS X modifiers
 */
static int convertJavaUserDefinedModifiersToMac(int modifiers)
{
  int macModifiers = 0;

  if(modifiers & 0x01)
  {
    /* CTRL */
    macModifiers |= NSControlKeyMask;
  }
  if(modifiers & 0x02)
  {
    /* ALT */
    macModifiers |= NSAlternateKeyMask;
  }
  if(modifiers & 0x04)
  {
    /* SHIFT */
    macModifiers |= NSShiftKeyMask;
  }
  if(modifiers & 0x08)
  {
    /* LOGO */
    macModifiers |= NSCommandKeyMask;
  }

  return macModifiers;
}

/**
 * \brief Convert Mac modifiers to our Java user-defined ones.
 * \param modifiers Mac modifiers (MOD_CONTROL, ...)
 * \return Java user-defined modifiers
 */
static int convertMacModifiersToJavaUserDefined(int modifiers)
{
    int javaModifiers = 0;

    if(modifiers & NSControlKeyMask)
    {
        javaModifiers |= 0x01;
    }
    if(modifiers & NSAlternateKeyMask)
    {
        javaModifiers |= 0x02;
    }
    if(modifiers & NSShiftKeyMask)
    {
        javaModifiers |= 0x04;
    }
    if(modifiers & NSCommandKeyMask)
    {
        javaModifiers |= 0x08;
    }

    return javaModifiers;
}

@interface KeyboardHook: NSObject
{
    @private
        jobject delegateObject;
    JavaVM* vm;
}

-(void)dealloc;
-(id)init;
-(void) setDelegate:(jobject)delegate inJNIEnv:(JNIEnv* )jniEnv;
-(void) hotkeyAction:(NSEvent*)hotKeyEvent inObject:(id)anObject;
-(void) notify:(int) keyCode inModifiers:(int)modifiers;
-(int) registerKey:(int)keycode inModifiers:(int)modifiers;
-(void) unregisterKey:(int)keycode inModifiers:(int)modifiers;
@end

@implementation KeyboardHook
-(void) notify:(int) keyCode inModifiers:(int)modifiers;
{
    jobject delegate;
    JNIEnv* jniEnv = NULL;
    jclass delegateClass = NULL;

    delegate = self->delegateObject;
    if(!delegate)
        return;

    vm = self->vm;
    if(0 != (*vm)->AttachCurrentThreadAsDaemon(vm, (void**)&jniEnv, NULL))
        return;

    delegateClass = (*jniEnv)->GetObjectClass(jniEnv, delegate);
    if(delegateClass)
    {
        jmethodID methodid = NULL;

        methodid = (*jniEnv)->GetMethodID(jniEnv, delegateClass,"receiveKey", "(II)V");
        if(methodid)
        {
            (*jniEnv)->CallVoidMethod(jniEnv, delegate, methodid, keyCode, modifiers);
        }
    }
    (*jniEnv)->ExceptionClear(jniEnv);
}

- (void)setDelegate:(jobject) delegate inJNIEnv:(JNIEnv*)jniEnv
{
    if(self->delegateObject)
    {
        if(!jniEnv)
            (*(self->vm))->AttachCurrentThread(self->vm, (void**)&jniEnv, NULL);
        (*jniEnv)->DeleteGlobalRef(jniEnv, self->delegateObject);
        self->delegateObject = NULL;
        self->vm = NULL;
    }

    if(delegate)
    {
        delegate = (*jniEnv)->NewGlobalRef(jniEnv, delegate);
        if(delegate)
        {
            (*jniEnv)->GetJavaVM(jniEnv, &(self->vm));
            self->delegateObject = delegate;
        }
    }
}

-(void) hotkeyAction:(NSEvent*)hotKeyEvent inObject:(id)anObject;
{
    int modifiers = [hotKeyEvent modifierFlags];
    int keycode = [hotKeyEvent keyCode];
    int javaModifiers = 0;
    int javaKeycode = 0;

    (void)anObject;

    javaKeycode = convertMacKeycodeToJava(keycode);
    javaModifiers = convertMacModifiersToJavaUserDefined(modifiers);

    [self notify:javaKeycode inModifiers:javaModifiers];
}

- (id)init
{
    if((self = [super init]))
    {
        self->delegateObject = NULL;
        self->vm = NULL;
    }
    return self;
}

- (void)dealloc
{
    [self setDelegate:NULL inJNIEnv:NULL];
    [super dealloc];
}

- (int) registerKey:(int)keycode inModifiers:(int)modifiers
{
    NSAutoreleasePool* autoreleasePool = NULL;
    DDHotKeyCenter* c = NULL;

    autoreleasePool = [[NSAutoreleasePool alloc] init];
    c = [[DDHotKeyCenter alloc] init];

    /*
    DDHotKeyTask task = ^(NSEvent* hkEvent)
    {
        printf("hot task\n");fflush(stdout);
    };
    */

    if(![c registerHotKeyWithKeyCode:keycode modifierFlags:(modifiers) target:self action:@selector(hotkeyAction:inObject:) object:nil])
    {
        [c release];
        [autoreleasePool release];
        return 0;
    }
    [c release];
    [autoreleasePool release];
    return 1;
}

- (void) unregisterKey:(int)keycode inModifiers:(int)modifiers
{
    NSAutoreleasePool* autoreleasePool;
    DDHotKeyCenter* c = NULL;

    autoreleasePool = [[NSAutoreleasePool alloc] init];
    c = [[DDHotKeyCenter alloc] init];

    [c unregisterHotKeyWithKeyCode:keycode modifierFlags:(modifiers)];
    [c release];
    [autoreleasePool release];
}
@end

    JNIEXPORT jlong JNICALL Java_net_java_sip_communicator_impl_globalshortcut_NativeKeyboardHook_init
(JNIEnv* jniEnv, jclass clazz)
{
    KeyboardHook* keyboard = NULL;
    NSAutoreleasePool* autoreleasePool;
    
    (void)jniEnv;
    (void)clazz;

    autoreleasePool = [[NSAutoreleasePool alloc] init];
    keyboard = [[KeyboardHook alloc] init];

    [autoreleasePool release];

    return (jlong)keyboard;
}

JNIEXPORT void JNICALL Java_net_java_sip_communicator_impl_globalshortcut_NativeKeyboardHook_start
 (JNIEnv* jniEnv, jclass clazz, jlong ptr)
{
    (void)jniEnv;
    (void)clazz;
    (void)ptr;
    /* do nothing */
}

JNIEXPORT void JNICALL Java_net_java_sip_communicator_impl_globalshortcut_NativeKeyboardHook_stop
 (JNIEnv* jniEnv, jclass clazz, jlong ptr)
{
    (void)jniEnv;
    (void)clazz;
    (void)ptr;
    /* do nothing */
}

JNIEXPORT void JNICALL Java_net_java_sip_communicator_impl_globalshortcut_NativeKeyboardHook_setDelegate
 (JNIEnv* jniEnv, jclass clazz, jlong ptr, jobject delegate)
{
    KeyboardHook* keyboard = NULL;
    
    (void)clazz;

    if(delegate)
    {
        keyboard = (KeyboardHook*)ptr;
        [keyboard setDelegate:delegate inJNIEnv:jniEnv];
    }
    else
    {
        keyboard = NULL;
    }
}

JNIEXPORT jboolean JNICALL Java_net_java_sip_communicator_impl_globalshortcut_NativeKeyboardHook_registerShortcut
 (JNIEnv* jniEnv, jclass clazz, jlong ptr, jint keycode, jint modifiers)
{
    KeyboardHook* keyboard = (KeyboardHook*)ptr;

    (void)jniEnv;    
    (void)clazz;

    if(keyboard)
    {
        int macKeycode = convertJavaKeycodeToMac(keycode);
        int macModifiers = convertJavaUserDefinedModifiersToMac(modifiers);
        if(macKeycode == -1)
        {
            return JNI_FALSE;
        }

        if([keyboard registerKey:macKeycode inModifiers:macModifiers])
        {
            return JNI_TRUE;
        }
    }   
    return JNI_FALSE;
}

JNIEXPORT void JNICALL Java_net_java_sip_communicator_impl_globalshortcut_NativeKeyboardHook_unregisterShortcut
 (JNIEnv* jniEnv, jclass clazz, jlong ptr, jint keycode, jint modifiers)
{
    KeyboardHook* keyboard = (KeyboardHook*)ptr;

    (void)jniEnv;
    (void)clazz;

    if(keyboard)
    {
        int macKeycode = convertJavaKeycodeToMac(keycode);
        int macModifiers = convertJavaUserDefinedModifiersToMac(modifiers);
        
        if(macModifiers == -1)
        {
            return;
        }
        
        [keyboard unregisterKey:macKeycode inModifiers:macModifiers];
    }
}

