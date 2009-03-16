/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

/**
 * Init the Sparkle subsystem.
 *
 * To generate the .h file, compile SC, go to the 
 * classes/ directory, and execute:
 * javah -jni net.java.sip.communicator.impl.sparkle.SparkleActivator
 *
 * For compilation, this requires the Sparkle.framework 
 * installed in /Library/Frameworks/. This Framework is 
 * available at http://sparkle.andymatuschak.org/
 *
 * @author Romain Kuntz
 */

#include <Cocoa/Cocoa.h>
#include <Sparkle/SUUpdater.h>
#include "net_java_sip_communicator_impl_sparkle_SparkleActivator.h"

/*
 * Class:     net_java_sip_communicator_impl_sparkle_SparkleActivator
 * Method:    initSparkle
 * Signature: (Ljava/lang/String;ZI)V
 */
JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_sparkle_SparkleActivator_initSparkle
  (JNIEnv *env, jclass obj, jstring pathToSparkleFramework, 
   jboolean updateAtStartup, jint checkInterval)
{
    bool haveBundle = ([[NSBundle mainBundle] 
                        objectForInfoDictionaryKey:@"CFBundleName"] != nil);
    const char *path = (*env)->GetStringUTFChars(env, pathToSparkleFramework, 0);  

    // The below code was used to avoid to link the Sparkle framework
    // at comilation time. 
    //NSBundle* bundle = [NSBundle bundleWithPath:[NSString stringWithCString: path]];
    //Class suUpdaterClass = [bundle classNamed:@"SUUpdater"];
    //id suUpdater = [[suUpdaterClass alloc] init];
  
    SUUpdater *suUpdater = [SUUpdater alloc];

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

    // Check at Startup (SUCheckAtStartup and SUScheduledCheckInterval
    // specified in the Info.plist does not seem to work)
    if (updateAtStartup == JNI_TRUE)
        [suUpdater checkForUpdatesInBackground];

    [suUpdater scheduleCheckWithInterval:checkInterval];
}
