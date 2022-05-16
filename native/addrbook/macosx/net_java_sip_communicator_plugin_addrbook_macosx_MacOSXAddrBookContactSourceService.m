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

#include "net_java_sip_communicator_plugin_addrbook_macosx_MacOSXAddrBookContactSourceService.h"

#import <AddressBook/ABGlobals.h>
#import <AddressBook/AddressBook.h>
#import <Foundation/NSAutoreleasePool.h>
#import <Foundation/NSNotification.h>
#import <Foundation/NSObject.h>

@interface MacOSXAddrBookContactSourceService : NSObject
{
@private
    jobject delegateObject;
    JavaVM *vm;
}

- (void)abDatabaseChangedExternallyNotification:(NSNotification *)notification;
- (void)abDatabaseChangedNotification:(NSNotification *)notification;

-(void)clean;
-(void) setDelegate:(jobject)delegate inJNIEnv:(JNIEnv *)jniEnv;
-(void) notify:(id)param methodName:(NSString *)mtdName;
@end /* MacOSXAddrBookContactSourceService */

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_plugin_addrbook_macosx_MacOSXAddrBookContactSourceService_start
    (JNIEnv *jniEnv, jclass clazz)
{
    NSAutoreleasePool *pool;
    MacOSXAddrBookContactSourceService *mabcss;

    pool = [[NSAutoreleasePool alloc] init];

    mabcss = [[MacOSXAddrBookContactSourceService alloc] init];
    if (mabcss)
    {
        NSNotificationCenter *notificationCenter
            = [NSNotificationCenter defaultCenter];

        [notificationCenter
            addObserver:mabcss
            selector:@selector(abDatabaseChangedExternallyNotification:)
            name:kABDatabaseChangedExternallyNotification
            object:nil];
        [notificationCenter
            addObserver:mabcss
            selector:@selector(abDatabaseChangedNotification:)
            name:kABDatabaseChangedNotification
            object:nil];
    }

    [pool release];
    return (jlong) mabcss;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_plugin_addrbook_macosx_MacOSXAddrBookContactSourceService_stop
    (JNIEnv *jniEnv, jclass clazz, jlong ptr)
{
    MacOSXAddrBookContactSourceService *mabcss
        = (MacOSXAddrBookContactSourceService *) ptr;
    NSAutoreleasePool *pool;

    pool = [[NSAutoreleasePool alloc] init];

    [[NSNotificationCenter defaultCenter] removeObserver:mabcss];
    [mabcss clean];
    [mabcss release];

    [pool release];
}

/*
 * Class:     net_java_sip_communicator_plugin_addrbook_macosx_MacOSXAddrBookContactSourceService
 * Method:    setDelegate
 * Signature: (JLnet/java/sip/communicator/plugin/addrbook/macosx/MacOSXAddrBookContactSourceService/NotificationsDelegate;)V
 */
JNIEXPORT void JNICALL Java_net_java_sip_communicator_plugin_addrbook_macosx_MacOSXAddrBookContactSourceService_setDelegate
  (JNIEnv *jniEnv, jclass clazz, jlong ptr, jobject m_delegate)
{
    MacOSXAddrBookContactSourceService *oDelegate;

    if (m_delegate)
    {
        oDelegate = (MacOSXAddrBookContactSourceService *) ptr;
        [oDelegate setDelegate:m_delegate inJNIEnv:jniEnv];
    }
    else
        oDelegate = nil;
}

@implementation MacOSXAddrBookContactSourceService
- (void)clean
{
    [self setDelegate:NULL inJNIEnv:NULL];
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
        }
    }
}

-(void) notify:(id)param methodName:(NSString *)mName
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

        if ([param isKindOfClass:[NSString class]])
        {
            methodid = (*jniEnv)->GetMethodID(jniEnv,
                                              delegateClass,
                                              [mName UTF8String],
                                              "(Ljava/lang/String;)V");

            if(methodid)
                (*jniEnv)->CallVoidMethod(jniEnv,
                                      delegate,
                                      methodid,
                                      (*jniEnv)->NewStringUTF(
                                                    jniEnv,
                                                    [param UTF8String]));

        }
        else
        {
            methodid = (*jniEnv)->GetMethodID(jniEnv,
                                              delegateClass,
                                              [mName UTF8String],
                                              "(J)V");
            if(methodid)
                (*jniEnv)->CallVoidMethod(jniEnv,
                                      delegate,
                                      methodid,
                                      (jlong)param);
        }
    }
    (*jniEnv)->ExceptionClear(jniEnv);
}

- (void)abDatabaseChangedExternallyNotification:(NSNotification *)notification
{
    ABAddressBook *addressBook;
    id inserted =
        [[notification userInfo] objectForKey:kABInsertedRecords];
    id updated =
        [[notification userInfo] objectForKey:kABUpdatedRecords];
    id deleted =
        [[notification userInfo] objectForKey:kABDeletedRecords];

    addressBook = [ABAddressBook sharedAddressBook];

    NSUInteger peopleCount;
    NSUInteger i;
    NSString *personID;

    if (inserted)
    {
        NSArray *people;

        if ([inserted isKindOfClass:[NSArray class]])
        {
            people = inserted;
        } else
        {
            people = [NSArray arrayWithObject:(ABPerson *)[addressBook recordForUniqueId:inserted]];
        }

        peopleCount = [people count];
        for (i = 0; i < peopleCount; i++)
        {
            personID = [people objectAtIndex:i];
            ABPerson *person =
                (ABPerson *)[addressBook recordForUniqueId:personID];
            [self notify:person methodName:@"inserted"];
        }
    }

    if (updated)
    {
        NSArray *people;

        if ([updated isKindOfClass:[NSArray class]])
        {
            people = updated;
        }
        else
        {
            people = [NSArray arrayWithObject:(ABPerson *)[addressBook recordForUniqueId:updated]];
        }

        peopleCount = [people count];
        for (i = 0; i < peopleCount; i++)
        {
            personID = [people objectAtIndex:i];
            ABPerson *person =
                (ABPerson *)[addressBook recordForUniqueId:personID];
            [self notify:person methodName:@"updated"];
        }
    }

    if (deleted)
    {
        NSArray *people;

        if ([deleted isKindOfClass:[NSArray class]])
        {
            people = deleted;
        }
        else
        {
            people = [NSArray arrayWithObject:(ABPerson *)[addressBook recordForUniqueId:deleted]];
        }

        peopleCount = [people count];
        for (i = 0; i < peopleCount; i++)
        {
            personID = [people objectAtIndex:i];

            [self notify:personID methodName:@"deleted"];
        }
    }
}

- (void)abDatabaseChangedNotification:(NSNotification *)notification
{
}
@end /* MacOSXAddrBookContactSourceService */
