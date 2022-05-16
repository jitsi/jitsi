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
#include "net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications.h"

#import <AppKit/NSWorkspace.h>
#import <Foundation/NSAutoreleasePool.h>
#import <Foundation/Foundation.h>
#import <Foundation/NSObject.h>
#import <Carbon/Carbon.h>
#import <SystemConfiguration/SystemConfiguration.h>

@interface AISleepNotification: NSObject
{
@private
    jobject delegateObject;
    JavaVM *vm;
}

-(void)dealloc;
-(id)init;
-(void)clean;
-(void)start;
-(void)stop;

-(void) receiveSleepNote: (NSNotification *) notification;
-(void) receiveWakeNote: (NSNotification *) notification;
-(void) receiveDisplaySleepNote: (NSNotification *) notification;
-(void) receiveDisplayWakeNote: (NSNotification *) notification;
-(void) screenIsLocked: (NSNotification *) notification;
-(void) screenIsUnlocked: (NSNotification *) notification;
-(void) screensaverStart: (NSNotification *) notification;
-(void) screensaverWillStop: (NSNotification *) notification;
-(void) screensaverStop: (NSNotification *) notification;
-(void) netChange: (NSNotification *) notification;

-(void) setDelegate:(jobject)delegate inJNIEnv:(JNIEnv *)jniEnv;

-(void) notify: (int) notificationType;


@end


@implementation AISleepNotification
CFRunLoopRef loopRef;
/** Our run loop source for notification used
    for network notifications. */
static CFRunLoopSourceRef rlSrc;

-(void) notify: (int) notificationType
{
    jobject delegate;
    JNIEnv *jniEnv;
    jclass delegateClass = NULL;

    delegate = self->delegateObject;
    if (!delegate)
        return;

    vm = self->vm;
    if (0 != (*vm)->AttachCurrentThreadAsDaemon(vm, (void **)&jniEnv, NULL))
        return;

    delegateClass = (*jniEnv)->GetObjectClass(jniEnv, delegate);
    if(delegateClass)
    {
        jmethodID methodid = NULL;

        methodid = (*jniEnv)->GetMethodID(jniEnv, delegateClass,"notify", "(I)V");
        if(methodid)
        {
            (*jniEnv)->CallVoidMethod(jniEnv, delegate, methodid, notificationType);
        }
    }
    (*jniEnv)->ExceptionClear(jniEnv);
}

- (void)receiveSleepNote: (NSNotification *) note
{
    [self notify:net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications_NOTIFY_SLEEP];
}

- (void)receiveWakeNote: (NSNotification *) note
{
    [self notify:net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications_NOTIFY_WAKE];
}

-(void) receiveDisplaySleepNote: (NSNotification *) note
{
    [self notify:net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications_NOTIFY_DISPLAY_SLEEP];
}

-(void) receiveDisplayWakeNote: (NSNotification *) note
{
    [self notify:net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications_NOTIFY_DISPLAY_WAKE];
}

-(void)screenIsLocked: (NSNotification *) notification
{
    [self notify:net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications_NOTIFY_SCREEN_LOCKED];
}
- (void) screenIsUnlocked: (NSNotification *) notification
{
    [self notify:net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications_NOTIFY_SCREEN_UNLOCKED];
}

-(void) screensaverStart: (NSNotification *) notification
{
    [self notify:net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications_NOTIFY_SCREENSAVER_START];
}

-(void) screensaverWillStop: (NSNotification *) notification
{
    [self notify:net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications_NOTIFY_SCREENSAVER_WILL_STOP];
}

-(void) screensaverStop: (NSNotification *) notification
{
    [self notify:net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications_NOTIFY_SCREENSAVER_STOP];
}

-(void) netChange: (NSNotification *) notification
{
    [self notify:net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications_NOTIFY_NETWORK_CHANGE];
}

-(void) dnsChange: (NSNotification *) notification
{
    [self notify:net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications_NOTIFY_DNS_CHANGE];
}

void scCallback(SCDynamicStoreRef store, CFArrayRef changedKeys, void *info)
{
    NSAutoreleasePool* localPool = [NSAutoreleasePool new];

    [[NSNotificationCenter defaultCenter] postNotificationName
        :@"NetworkConfigurationDidChangeNotification" object:(id)info];

    [localPool drain];
}

void scDnsCallback(SCDynamicStoreRef store, CFArrayRef changedKeys, void *info)
{
    NSAutoreleasePool* localPool = [NSAutoreleasePool new];

    [[NSNotificationCenter defaultCenter] postNotificationName
        :@"DnsConfigurationDidChangeNotification" object:(id)info];

    [localPool drain];
}

- (void)setDelegate:(jobject) delegate inJNIEnv:(JNIEnv *)jniEnv
{
    if (self->delegateObject)
    {
        if (!jniEnv)
            (*(self->vm))->AttachCurrentThread(self->vm, (void **)&jniEnv, NULL);
        (*jniEnv)->DeleteGlobalRef(jniEnv, self->delegateObject);
        self->delegateObject = NULL;
        self->vm = NULL;
    }
    if (delegate)
    {
        delegate = (*jniEnv)->NewGlobalRef(jniEnv, delegate);
        if (delegate)
        {
            (*jniEnv)->GetJavaVM(jniEnv, &(self->vm));
            self->delegateObject = delegate;

            // These notifications are filed on NSWorkspace's notification center,
            // not the default notification center. You will not receive
            // sleep/wake notifications if you file with the default
            // notification center.
            [[[NSWorkspace sharedWorkspace] notificationCenter] addObserver: self
                selector: @selector(receiveSleepNote:)
                name: @"NSWorkspaceWillSleepNotification" object: NULL];

            [[[NSWorkspace sharedWorkspace] notificationCenter] addObserver: self
                    selector: @selector(receiveWakeNote:)
                    name: @"NSWorkspaceDidWakeNotification" object: NULL];

            [[[NSWorkspace sharedWorkspace] notificationCenter] addObserver: self
                selector: @selector(receiveDisplayWakeNote:)
                name: @"NSWorkspaceScreensDidWakeNotification" object: NULL];

            [[[NSWorkspace sharedWorkspace] notificationCenter] addObserver: self
                    selector: @selector(receiveDisplaySleepNote:)
                    name: @"NSWorkspaceScreensDidSleepNotification" object: NULL];

            NSDistributedNotificationCenter * center =
                [NSDistributedNotificationCenter defaultCenter];

            [center addObserver:self
                    selector: @selector(screenIsLocked:)
                    name:@"com.apple.screenIsLocked" object:NULL];

            [center addObserver:self
                    selector: @selector(screenIsUnlocked:)
                    name:@"com.apple.screenIsUnlocked" object:NULL];

            [center addObserver:self
                    selector: @selector(screensaverStart:)
                    name:@"com.apple.screensaver.didstart" object:NULL];

            [center addObserver:self
                    selector: @selector(screensaverWillStop:)
                    name:@"com.apple.screensaver.willstop" object:NULL];

            [center addObserver:self
                    selector: @selector(screensaverStop:)
                    name:@"com.apple.screensaver.didstop" object:NULL];


            [[NSNotificationCenter defaultCenter] addObserver:self
                    selector: @selector(netChange:)
                    name:@"NetworkConfigurationDidChangeNotification" object:NULL];
            [[NSNotificationCenter defaultCenter] addObserver:self
                    selector: @selector(dnsChange:)
                    name:@"DnsConfigurationDidChangeNotification" object:NULL];

            {
            SCDynamicStoreRef dynStore;

            SCDynamicStoreContext context = {0, NULL, NULL, NULL, NULL};

            dynStore = SCDynamicStoreCreate(kCFAllocatorDefault,
                                  CFBundleGetIdentifier(CFBundleGetMainBundle()),
                                  scCallback,
                                  &context);

            const CFStringRef keys[1] = {
                CFSTR("State:/Network/Interface/.*/IPv.")
            };
            CFArrayRef watchedKeys = CFArrayCreate(kCFAllocatorDefault,
                                         (const void **)keys,
                                         1,
                                         &kCFTypeArrayCallBacks);
            if (!SCDynamicStoreSetNotificationKeys(dynStore,
                                         NULL,
                                         watchedKeys))
            {
                CFRelease(watchedKeys);
                fprintf(stderr, "SCDynamicStoreSetNotificationKeys() failed: %s",
                    SCErrorString(SCError()));
                CFRelease(dynStore);
                dynStore = NULL;

                return;
            }
            CFRelease(watchedKeys);


            rlSrc = SCDynamicStoreCreateRunLoopSource(
                kCFAllocatorDefault, dynStore, 0);
            CFRunLoopAddSource(
                CFRunLoopGetCurrent(), rlSrc, kCFRunLoopDefaultMode);
            CFRelease(rlSrc);
            }

            {
                SCDynamicStoreRef dynStore;

                SCDynamicStoreContext context = {0, NULL, NULL, NULL, NULL};

                dynStore = SCDynamicStoreCreate(kCFAllocatorDefault,
                                      CFBundleGetIdentifier(CFBundleGetMainBundle()),
                                      scDnsCallback,
                                      &context);

                const CFStringRef keys[1] = {
                    CFSTR("State:/Network/Global/DNS")
                };
                CFArrayRef watchedKeys = CFArrayCreate(kCFAllocatorDefault,
                                             (const void **)keys,
                                             1,
                                             &kCFTypeArrayCallBacks);
                if (!SCDynamicStoreSetNotificationKeys(dynStore,
                                             NULL,
                                             watchedKeys))
                {
                    CFRelease(watchedKeys);
                    fprintf(stderr, "SCDynamicStoreSetNotificationKeys() failed: %s",
                        SCErrorString(SCError()));
                    CFRelease(dynStore);
                    dynStore = NULL;

                    return;
                }
                CFRelease(watchedKeys);


                rlSrc = SCDynamicStoreCreateRunLoopSource(
                    kCFAllocatorDefault, dynStore, 0);
                CFRunLoopAddSource(
                    CFRunLoopGetCurrent(), rlSrc, kCFRunLoopDefaultMode);
                CFRelease(rlSrc);
                }
        }
    }
}

- (id)init
{
    if ((self = [super init]))
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

- (void)clean
{
    [[[NSWorkspace sharedWorkspace] notificationCenter] removeObserver: self];

    NSDistributedNotificationCenter * center =
                [NSDistributedNotificationCenter defaultCenter];

    [center removeObserver:self];

    [self setDelegate:NULL inJNIEnv:NULL];
}

- (void)start
{
    loopRef = CFRunLoopGetCurrent();
    CFRunLoopRun();
}

- (void)stop
{
    if(loopRef)
        CFRunLoopStop(loopRef);
}
@end

/*
 * Class:     net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications
 * Method:    setDelegate
 * Signature: (Lnet/java/sip/communicator/impl/sysactivity/SystemActivityNotifications/NotificationsDelegate;)V
 */
JNIEXPORT void JNICALL Java_net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications_setDelegate
  (JNIEnv* jniEnv, jclass clazz, jlong ptr, jobject m_delegate)
{
    AISleepNotification *oDelegate;

    if (m_delegate)
    {
        oDelegate = (AISleepNotification *) ptr;
        [oDelegate setDelegate:m_delegate inJNIEnv:jniEnv];
    }
    else
        oDelegate = nil;
}

/*
 * Class:     net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications
 * Method:    allocAndInit
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications_allocAndInit
  (JNIEnv* jniEnv, jclass clazz)
{
    AISleepNotification *oDelegate;
    NSAutoreleasePool *autoreleasePool;
    autoreleasePool = [[NSAutoreleasePool alloc] init];

    oDelegate = [[AISleepNotification alloc] init];

    [autoreleasePool release];

    return (jlong)oDelegate;
}

JNIEXPORT void JNICALL Java_net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications_release
  (JNIEnv* jniEnv, jclass clazz, jlong ptr)
{
    AISleepNotification *oDelegate;
    NSAutoreleasePool *autoreleasePool;
    autoreleasePool = [[NSAutoreleasePool alloc] init];

    oDelegate = (AISleepNotification *) ptr;
    [oDelegate clean];

    [oDelegate release];

    [autoreleasePool release];
}

/*
 * Class:     net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications
 * Method:    start
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications_start
  (JNIEnv* jniEnv, jclass clazz, jlong ptr)
{
    AISleepNotification *oDelegate;
    oDelegate = (AISleepNotification *) ptr;
    [oDelegate start];
}

/*
 * Class:     net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications
 * Method:    stop
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications_stop
  (JNIEnv* jniEnv, jclass clazz, jlong ptr)
{
    AISleepNotification *oDelegate;
    oDelegate = (AISleepNotification *) ptr;
    [oDelegate stop];
}

/*
 * Class:     net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications
 * Method:    getLastInput
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications_getLastInput
  (JNIEnv* jniEnv, jclass clazz)
{
    int64_t idlesecs = -1;
    io_iterator_t iter = 0;
    if (IOServiceGetMatchingServices(kIOMasterPortDefault, IOServiceMatching("IOHIDSystem"), &iter) == KERN_SUCCESS) {
        io_registry_entry_t entry = IOIteratorNext(iter);
        if (entry) {
            CFMutableDictionaryRef dict = NULL;
            if (IORegistryEntryCreateCFProperties(entry, &dict, kCFAllocatorDefault, 0) == KERN_SUCCESS) {
                CFNumberRef obj = CFDictionaryGetValue(dict, CFSTR("HIDIdleTime"));
                if (obj) {
                    int64_t nanoseconds = 0;
                    if (CFNumberGetValue(obj, kCFNumberSInt64Type, &nanoseconds)) {
                        idlesecs = nanoseconds / 1000000;
                    }
                }
                CFRelease(dict);
            }
            IOObjectRelease(entry);
        }
        IOObjectRelease(iter);
    }
    return idlesecs;
}
