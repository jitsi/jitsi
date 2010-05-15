/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#include "common.h"

#import <Foundation/NSAutoreleasePool.h>
#import <Foundation/NSObject.h> /* NSSelectorFromString */
#import <Foundation/NSString.h>

id NSObject_performSelector(id obj, NSString *selectorName)
{
    NSAutoreleasePool *autoreleasePool;
    SEL selector;
    id ret;

    autoreleasePool = [[NSAutoreleasePool alloc] init];

    selector = NSSelectorFromString(selectorName);
    ret = [obj performSelector:selector];

    [autoreleasePool release];
    return ret;
}
