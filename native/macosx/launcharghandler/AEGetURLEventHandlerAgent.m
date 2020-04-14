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

#include "AEGetURLEventHandler.h"

#include <jvmti.h>
#include <stdlib.h>
#import <Foundation/Foundation.h>

@interface AEGetURLEventHandler : NSObject
{
    jobject _listener;
    char *_url;
    JavaVM *_vm;
}

+ (AEGetURLEventHandler *)sharedAEGetURLEventHandler;

- (void)handleAEGetURLEvent:(NSAppleEventDescriptor *)event
        withReplyEvent:(NSAppleEventDescriptor *)replyEvent;
- (void)setAEGetURLListener:(jobject)listener jniEnv:(JNIEnv *)jniEnv;
- (void)setURL:(char *)url;
- (void)setVM:(JavaVM *)vm;
- (void)tryFireAEGetURLEvent;
@end

static AEGetURLEventHandler *g_sharedAEGetURLEventHandler = NULL;

@implementation AEGetURLEventHandler
+ (AEGetURLEventHandler *)sharedAEGetURLEventHandler {
    if (!g_sharedAEGetURLEventHandler) {
        g_sharedAEGetURLEventHandler = [[AEGetURLEventHandler alloc] init];
    }
    return g_sharedAEGetURLEventHandler;
}

- (void)dealloc {
    [self setURL:NULL];
    [self setAEGetURLListener:NULL jniEnv:NULL];

    [super dealloc];
}

- (void)handleAEGetURLEvent:(NSAppleEventDescriptor *)event
        withReplyEvent:(NSAppleEventDescriptor *)replyEvent {
    NSAutoreleasePool *autoreleasePool = [[NSAutoreleasePool alloc] init];
    NSString *str;

    str = [[event paramDescriptorForKeyword:keyDirectObject] stringValue];
    if (str) {
        const NSStringEncoding encoding = NSNonLossyASCIIStringEncoding;
        NSUInteger length;
        char *chars;

        length = [str lengthOfBytesUsingEncoding:encoding];
        if (length) {
            length++; // Account for the NULL termination byte.
            chars = (char *) malloc (length * sizeof (char));
            if (chars
                    && (YES == [str getCString:chars maxLength:length encoding:encoding])) {
                [[AEGetURLEventHandler sharedAEGetURLEventHandler]
                    setURL:chars];
            } else {
                free (chars);
            }
        }
    }

    [autoreleasePool release];
}

- (AEGetURLEventHandler *)init {
    self = [super init];
    if (self) {
        _listener = NULL;
        _url = NULL;
        _vm = NULL;
    }
    return self;
}

- (void)setAEGetURLListener:(jobject)listener jniEnv:(JNIEnv *)jniEnv {
    if (!jniEnv
            && (!_vm || (JNI_OK != (*_vm)->GetEnv (_vm, (void **) &jniEnv, JNI_VERSION_1_2)))) {
        return; // TODO Don't swallow the failure.
    }

    if (_listener) {
        (*jniEnv)->DeleteGlobalRef (jniEnv, _listener);
        _listener = NULL;
    }
    if (listener) {
        _listener = (*jniEnv)->NewGlobalRef (jniEnv, listener);

        [self tryFireAEGetURLEvent];
    }
}

- (void)setURL:(char *)url {
    if (_url != url) {
        if (_url) {
            free (_url);
        }

        _url = url;

        [self tryFireAEGetURLEvent];
    }
}

- (void)setVM:(JavaVM *)vm {
    _vm = vm;
}

- (void)tryFireAEGetURLEvent {
    JNIEnv *jniEnv;

    if (_vm
            && _listener
            && _url
            && (JNI_OK == (*_vm)->GetEnv (_vm, (void **) &jniEnv, JNI_VERSION_1_2))) {
        jclass clazz = (*jniEnv)->GetObjectClass (jniEnv, _listener);
        jmethodID methodID =
            (*jniEnv)->GetMethodID (
                jniEnv, clazz, "handleAEGetURLEvent", "(Ljava/lang/String;)V");

        if (methodID) {
            jstring url = (*jniEnv)->NewStringUTF (jniEnv, _url);

            if (url) {
                (*jniEnv)->CallVoidMethod (jniEnv, _listener, methodID, url);

                /*
                 * The URL should not be reported again after it has been
                 * handled.
                 */
                [self setURL:NULL];
            }
        }
    }
}
@end

JNIEXPORT jint JNICALL
Agent_OnLoad (JavaVM *vm, char *options, void *reserved) {
    NSAutoreleasePool *autoreleasePool = [[NSAutoreleasePool alloc] init];
    AEGetURLEventHandler *aeGetURLEventHandler
        = [AEGetURLEventHandler sharedAEGetURLEventHandler];

    [aeGetURLEventHandler setVM:vm];

    [[NSAppleEventManager sharedAppleEventManager]
        setEventHandler:aeGetURLEventHandler
        andSelector:@selector(handleAEGetURLEvent:withReplyEvent:)
        forEventClass:kInternetEventClass
        andEventID:kAEGetURL];

    [autoreleasePool release];

    /*
     * Non-zero will terminate the VM and we don't want that even if we fail to
     * install the event handler.
     */
    return 0;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_util_launchutils_AEGetURLEventHandler_setAEGetURLListener
    (JNIEnv *jniEnv, jclass clazz, jobject listener) {
    NSAutoreleasePool *autoreleasePool = [[NSAutoreleasePool alloc] init];

    [[AEGetURLEventHandler sharedAEGetURLEventHandler]
        setAEGetURLListener:listener jniEnv:jniEnv];

    [autoreleasePool release];
}
