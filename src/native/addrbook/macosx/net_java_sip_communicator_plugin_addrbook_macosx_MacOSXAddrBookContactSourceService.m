/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#include "net_java_sip_communicator_plugin_addrbook_macosx_MacOSXAddrBookContactSourceService.h"

#import <AddressBook/ABGlobals.h>
#import <Foundation/NSAutoreleasePool.h>
#import <Foundation/NSNotification.h>
#import <Foundation/NSObject.h>

@interface MacOSXAddrBookContactSourceService : NSObject
{
}

- (void)abDatabaseChangedExternallyNotification:(NSNotification *)notification;
- (void)abDatabaseChangedNotification:(NSNotification *)notification;
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
    [mabcss release];

    [pool release];
}

@implementation MacOSXAddrBookContactSourceService
- (void)abDatabaseChangedExternallyNotification:(NSNotification *)notification
{
}

- (void)abDatabaseChangedNotification:(NSNotification *)notification
{
}
@end /* MacOSXAddrBookContactSourceService */
