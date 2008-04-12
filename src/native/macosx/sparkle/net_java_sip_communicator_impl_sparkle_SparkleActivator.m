/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

/**
 * Init the Sparkle subsystem
 *
 * @author Romain Kuntz
 */
#include <Cocoa/Cocoa.h>
#include "net_java_sip_communicator_impl_sparkle_SparkleActivator.h"

/*
 * Class:     net_java_sip_communicator_impl_sparkle_SparkleActivator
 * Method:    initSparkle
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL 
Java_net_java_sip_communicator_impl_sparkle_SparkleActivator_initSparkle
  (JNIEnv *env, jclass obj, jstring pathToSparkleFramework)
{
    bool haveBundle = ([[NSBundle mainBundle] 
                        objectForInfoDictionaryKey:@"CFBundleName"] != nil);
    const char *path = (*env)->GetStringUTFChars(env, pathToSparkleFramework, 0);  

    NSBundle* bundle = [NSBundle bundleWithPath:[NSString stringWithCString: path]];
    Class suUpdaterClass = [bundle classNamed:@"SUUpdater"];
    id suUpdater = [[suUpdaterClass alloc] init];
   
    (*env)->ReleaseStringUTFChars(env, pathToSparkleFramework, path);  
 
    NSMenu* menu = [[NSApplication sharedApplication] mainMenu];
    NSMenu* applicationMenu = [[menu itemAtIndex:0] submenu];
    NSMenuItem* checkForUpdatesMenuItem = [[NSMenuItem alloc]
                                            initWithTitle:@"Check for Updates"
                                            action:@selector(checkForUpdates:)
                                            keyEquivalent:@""];

    if (haveBundle) {
        [checkForUpdatesMenuItem setEnabled:YES];
        [checkForUpdatesMenuItem setTarget:suUpdater];
    }

    // 0 => top, 1 => after "About..."
    [applicationMenu insertItem:checkForUpdatesMenuItem atIndex:1];
}
