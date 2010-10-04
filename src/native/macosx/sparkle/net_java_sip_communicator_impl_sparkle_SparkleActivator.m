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
 * @author Egidijus Jankauskas
 */

#include <Cocoa/Cocoa.h>
#include <Sparkle/Sparkle.h>
#include "net_java_sip_communicator_impl_sparkle_SparkleActivator.h"

/*
 * Class:     net_java_sip_communicator_impl_sparkle_SparkleActivator
 * Method:    initSparkle
 * Signature: (Ljava/lang/String;ZILjava/lang/String;)V
 */
JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_sparkle_SparkleActivator_initSparkle
  (JNIEnv *env, jclass obj, jstring pathToSparkleFramework, 
   jboolean updateAtStartup, jint checkInterval, jstring downloadLink)
{
    BOOL hasLaunchedBefore = [[NSUserDefaults standardUserDefaults] boolForKey:@"SCHasLaunchedBefore"];

    if(!hasLaunchedBefore)
    {
        [[NSUserDefaults standardUserDefaults] setBool:YES forKey:@"SCHasLaunchedBefore"];
        [[NSUserDefaults standardUserDefaults] synchronize];
    }

    // The below code was used to avoid to link the Sparkle framework
    // at comilation time. 
    //const char *path = (*env)->GetStringUTFChars(env, pathToSparkleFramework, 0); 
    //NSBundle* bundle = [NSBundle bundleWithPath:[NSString stringWithCString: path]];
    //Class suUpdaterClass = [bundle classNamed:@"SUUpdater"];
    //id suUpdater = [[suUpdaterClass alloc] init];
    //(*env)->ReleaseStringUTFChars(env, pathToSparkleFramework, path);

    SUUpdater *suUpdater = [SUUpdater updaterForBundle:[NSBundle mainBundle]];

    if(downloadLink)
    {
        const char* link = (*env)->GetStringUTFChars(env, downloadLink, 0);
        NSString* sLink = [NSString stringWithCString: link length: strlen(link)];
        NSURL* nsLink = [NSURL URLWithString: sLink];

        if(nsLink)
        {
            [suUpdater setFeedURL: nsLink];
        }
    }

    NSMenu* menu = [[NSApplication sharedApplication] mainMenu];
    NSMenu* applicationMenu = [[menu itemAtIndex:0] submenu];
    NSMenuItem* checkForUpdatesMenuItem = [[NSMenuItem alloc]
                                            initWithTitle:@"Check for Updates..."
                                            action:@selector(checkForUpdates:)
                                            keyEquivalent:@""];

    [checkForUpdatesMenuItem setEnabled:YES];
    [checkForUpdatesMenuItem setTarget:suUpdater];

    // 0 => top, 1 => after "About..."
    [applicationMenu insertItem:checkForUpdatesMenuItem atIndex:1];

    // Update is launched only at the second startup
    if (hasLaunchedBefore && updateAtStartup == JNI_TRUE)
    {
        // This method needs to be executed on the main thread because it may result
        // in GUI showing up. Besides, Sparkle uses asynchronous URLConnection which
        // requires to be called from a thread which runs in a default run loop mode.
        [suUpdater performSelectorOnMainThread:@selector(checkForUpdatesInBackground)
                                withObject:nil 
                                waitUntilDone:NO];
    }
}
